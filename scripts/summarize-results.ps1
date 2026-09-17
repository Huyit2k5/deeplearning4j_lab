# Gop cac file CSV benchmark thanh 1 bang markdown de de copy vao slide thuyet trinh.
Set-Location "$PSScriptRoot\.."

$out = New-Object System.Text.StringBuilder
[void]$out.AppendLine("# Tong hop ket qua benchmark DL4J (Windows)")
[void]$out.AppendLine("")

function Add-Table($title, $csvPath) {
    if (-not (Test-Path $csvPath)) {
        [void]$out.AppendLine("## $title`n(chua co ket qua: $csvPath)`n")
        return
    }
    $rows = Import-Csv $csvPath
    if ($rows.Count -eq 0) { return }
    [void]$out.AppendLine("## $title")
    $headers = $rows[0].PSObject.Properties.Name
    [void]$out.AppendLine("| " + ($headers -join " | ") + " |")
    [void]$out.AppendLine("| " + (($headers | ForEach-Object { "---" }) -join " | ") + " |")
    foreach ($row in $rows) {
        $values = $headers | ForEach-Object { $row.$_ }
        [void]$out.AppendLine("| " + ($values -join " | ") + " |")
    }
    [void]$out.AppendLine("")
}

Add-Table "Kich ban 1: WorkspaceMode NONE vs ENABLED" "results\workspace_benchmark.csv"
Add-Table "Kich ban 2: Spark Local Scaling" "results\spark_scaling_benchmark.csv"
Add-Table "Kich ban 3a: Inference Latency (In-process)" "results\inference_latency_inprocess.csv"
Add-Table "Kich ban 3b: Inference Latency (Remote)" "results\inference_latency_remote.csv"

$out.ToString() | Out-File -FilePath "results\summary.md" -Encoding utf8
Write-Host "Da xuat results\summary.md"
