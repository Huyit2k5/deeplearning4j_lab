# Gop ket qua nhieu lan chay WorkspaceBenchmark (epoch_detail_run*.csv + os_samples_run*.csv
# trong results\workspace_runs\) thanh bang mean +/- std:
#   - Thoi gian moi Epoch (ms)
#   - VmRSS Epoch 1 / Epoch cuoi (MB)  [Windows: Working Set tuong duong VmRSS]
#   - Dinh bo nho VmHWM (MB)           [Windows: Peak Working Set]
#   - Toc do phinh VmRSS (MB/epoch)
#   - Dung luong JVM Heap (MB)
param(
    [int]$Runs = 3,
    [int]$Epochs = 15
)

Set-Location "$PSScriptRoot\.."

function Get-MeanStd {
    param([double[]]$Values)
    if ($Values.Count -eq 0) { return @{ Mean = 0; Std = 0 } }
    $mean = ($Values | Measure-Object -Average).Average
    if ($Values.Count -lt 2) { return @{ Mean = $mean; Std = 0 } }
    $sumSq = 0.0
    foreach ($v in $Values) { $sumSq += [Math]::Pow($v - $mean, 2) }
    $std = [Math]::Sqrt($sumSq / ($Values.Count - 1))
    return @{ Mean = $mean; Std = $std }
}

function Find-NearestSample {
    param($Samples, [long]$TargetMs)
    $best = $null
    $bestDiff = [double]::MaxValue
    foreach ($s in $Samples) {
        $diff = [Math]::Abs([double]$s.TimestampMs - $TargetMs)
        if ($diff -lt $bestDiff) {
            $bestDiff = $diff
            $best = $s
        }
    }
    return $best
}

$modes = @("NONE", "ENABLED")
$pooledEpochMs = @{ NONE = @(); ENABLED = @() }
$pooledHeapMB  = @{ NONE = @(); ENABLED = @() }
$epoch1MB      = @{ NONE = @(); ENABLED = @() }
$epochLastMB   = @{ NONE = @(); ENABLED = @() }
$peakMB        = @{ NONE = @(); ENABLED = @() }
$growthRate    = @{ NONE = @(); ENABLED = @() }

for ($run = 1; $run -le $Runs; $run++) {
    $epochFile = "results\workspace_runs\epoch_detail_run$run.csv"
    $osFile = "results\workspace_runs\os_samples_run$run.csv"
    if (-not (Test-Path $epochFile) -or -not (Test-Path $osFile)) {
        Write-Host "Bo qua run $run (thieu file $epochFile hoac $osFile)"
        continue
    }
    $epochRows = Import-Csv $epochFile
    $osSamples = Import-Csv $osFile

    foreach ($mode in $modes) {
        $rows = $epochRows | Where-Object { $_.mode -eq $mode } | Sort-Object { [int]$_.epoch }
        if ($rows.Count -eq 0) { continue }

        foreach ($r in $rows) {
            $pooledEpochMs[$mode] += [double]$r.elapsedMs
            $pooledHeapMB[$mode] += ([double]$r.heapUsedBytes / 1MB)
        }

        $first = $rows[0]
        $last = $rows[$rows.Count - 1]

        $sampleAtFirst = Find-NearestSample -Samples $osSamples -TargetMs ([long]$first.timestampMs)
        $sampleAtLast = Find-NearestSample -Samples $osSamples -TargetMs ([long]$last.timestampMs)

        if ($sampleAtFirst) { $epoch1MB[$mode] += ([double]$sampleAtFirst.WorkingSet64 / 1MB) }
        if ($sampleAtLast) { $epochLastMB[$mode] += ([double]$sampleAtLast.WorkingSet64 / 1MB) }

        $modeStartMs = [long]$first.timestampMs - [long]$first.elapsedMs
        $modeEndMs = [long]$last.timestampMs
        $inRange = $osSamples | Where-Object {
            [long]$_.TimestampMs -ge $modeStartMs -and [long]$_.TimestampMs -le $modeEndMs
        }
        if ($inRange.Count -gt 0) {
            $peakBytes = ($inRange | ForEach-Object { [double]$_.WorkingSet64 } | Measure-Object -Maximum).Maximum
            $peakMB[$mode] += ($peakBytes / 1MB)
        }

        if ($sampleAtFirst -and $sampleAtLast -and $rows.Count -gt 1) {
            $deltaMB = ([double]$sampleAtLast.WorkingSet64 - [double]$sampleAtFirst.WorkingSet64) / 1MB
            $growthRate[$mode] += ($deltaMB / ($rows.Count - 1))
        }
    }
}

$table = @()
foreach ($mode in $modes) {
    $t = Get-MeanStd -Values $pooledEpochMs[$mode]
    $h1 = Get-MeanStd -Values $epoch1MB[$mode]
    $hN = Get-MeanStd -Values $epochLastMB[$mode]
    $pk = Get-MeanStd -Values $peakMB[$mode]
    $gr = Get-MeanStd -Values $growthRate[$mode]
    $hp = Get-MeanStd -Values $pooledHeapMB[$mode]

    $table += [PSCustomObject]@{
        Mode                  = $mode
        "ThoiGianMoiEpoch_ms" = "{0:N1} ± {1:N1}" -f $t.Mean, $t.Std
        "VmRSS_Epoch1_MB"     = "{0:N1} ± {1:N1}" -f $h1.Mean, $h1.Std
        "VmRSS_EpochN_MB"     = "{0:N1} ± {1:N1}" -f $hN.Mean, $hN.Std
        "VmHWM_MB"            = "{0:N1} ± {1:N1}" -f $pk.Mean, $pk.Std
        "TocDoPhinh_MBperEpoch" = "{0:+0.00;-0.00;0.00} ± {1:N2}" -f $gr.Mean, $gr.Std
        "JVMHeap_MB"          = "{0:N1} ± {1:N1}" -f $hp.Mean, $hp.Std
    }
}

$outFile = "results\workspace_report_table.md"
$md = "# Ket qua WorkspaceMode NONE vs ENABLED ($Runs lan chay x $Epochs epoch)`n`n"
$md += "| Thong so do luong | Che do NONE | Che do ENABLED |`n"
$md += "| --- | --- | --- |`n"
$md += "| Thoi gian moi Epoch (ms) | $($table[0].ThoiGianMoiEpoch_ms) | $($table[1].ThoiGianMoiEpoch_ms) |`n"
$md += "| VmRSS Epoch 1 (MB) | $($table[0].VmRSS_Epoch1_MB) | $($table[1].VmRSS_Epoch1_MB) |`n"
$md += "| VmRSS Epoch $Epochs (MB) | $($table[0].VmRSS_EpochN_MB) | $($table[1].VmRSS_EpochN_MB) |`n"
$md += "| Dinh bo nho VmHWM (MB) | $($table[0].VmHWM_MB) | $($table[1].VmHWM_MB) |`n"
$md += "| Toc do phinh VmRSS (MB/epoch) | $($table[0].TocDoPhinh_MBperEpoch) | $($table[1].TocDoPhinh_MBperEpoch) |`n"
$md += "| Dung luong JVM Heap (MB) | $($table[0].JVMHeap_MB) | $($table[1].JVMHeap_MB) |`n"

$md | Out-File -FilePath $outFile -Encoding utf8
Write-Host "`n$md"
Write-Host "Da luu bang tong hop vao $outFile" -ForegroundColor Green
