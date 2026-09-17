# Chay WorkspaceBenchmark N lan (mac dinh 3 lan, 15 epoch/lan), lay mau Working Set
# (tuong duong VmRSS tren Windows) tuong quan theo dung timestamp cua tung epoch,
# roi goi script tong hop de ra bang mean +/- std.
param(
    [int]$Runs = 3,
    [int]$Epochs = 15,
    [int]$BatchSize = 32,
    [string]$CsvPath = "data/creditcard.csv"
)

. "$PSScriptRoot\setup-env.ps1"
Set-Location "$PSScriptRoot\.."

New-Item -ItemType Directory -Force -Path "logs" | Out-Null
New-Item -ItemType Directory -Force -Path "results\workspace_runs" | Out-Null

Write-Host "Rebuilding benchmark module..." -ForegroundColor Cyan
mvn -pl benchmark install -q -DskipTests
if ($LASTEXITCODE -ne 0) { throw "Build benchmark that bai" }

for ($run = 1; $run -le $Runs; $run++) {
    Write-Host "`n=== Run $run/$Runs ($Epochs epoch/mode) ===" -ForegroundColor Cyan
    $timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
    $logFile = "logs\workspace_run${run}_$timestamp.log"

    Remove-Item -Path "results\workspace_pid.txt" -ErrorAction SilentlyContinue

    $job = Start-Job -ScriptBlock {
        param($root, $csvPath, $epochs, $batchSize)
        Set-Location $root
        mvn -pl benchmark exec:java `
            "-Dexec.mainClass=vn.huit.dl4j.benchmark.WorkspaceBenchmark" `
            "-Dexec.args=$csvPath $epochs $batchSize"
    } -ArgumentList (Get-Location).Path, $CsvPath, $Epochs, $BatchSize

    # Doi Java ghi PID file (toi da 60s)
    $waited = 0.0
    while (-not (Test-Path "results\workspace_pid.txt") -and $waited -lt 60) {
        Start-Sleep -Milliseconds 500
        $waited += 0.5
    }
    Start-Sleep -Milliseconds 300
    $targetPid = $null
    if (Test-Path "results\workspace_pid.txt") {
        $targetPid = [int](Get-Content "results\workspace_pid.txt" -Raw).Trim()
    }
    Write-Host "Theo doi PID: $targetPid"

    $samples = New-Object System.Collections.Generic.List[object]
    while ($job.State -eq "Running") {
        if ($targetPid) {
            $p = Get-Process -Id $targetPid -ErrorAction SilentlyContinue
            if ($p) {
                $samples.Add([PSCustomObject]@{
                    TimestampMs         = [long](([DateTimeOffset]::UtcNow).ToUnixTimeMilliseconds())
                    WorkingSet64        = $p.WorkingSet64
                    PeakWorkingSet64    = $p.PeakWorkingSet64
                    PrivateMemorySize64 = $p.PrivateMemorySize64
                })
            }
        }
        Start-Sleep -Milliseconds 200
    }
    Receive-Job $job *>&1 | Tee-Object -FilePath $logFile
    Remove-Job $job

    $samples | Export-Csv -Path "results\workspace_runs\os_samples_run$run.csv" -NoTypeInformation
    Copy-Item "results\workspace_epoch_detail.csv" "results\workspace_runs\epoch_detail_run$run.csv" -Force
    Write-Host "Run $run xong. Log: $logFile ; mau OS: $($samples.Count)"
}

Write-Host "`nDang tong hop ket qua tu $Runs lan chay..." -ForegroundColor Cyan
& "$PSScriptRoot\aggregate-workspace-runs.ps1" -Runs $Runs -Epochs $Epochs
