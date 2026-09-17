# Chay toan bo pipeline tu dau: build -> train -> benchmark 1 -> benchmark 2 -> (api + benchmark 3 thu cong)
. "$PSScriptRoot\setup-env.ps1"
Set-Location "$PSScriptRoot\.."

Write-Host "`n=== 1. Build toan bo project ===" -ForegroundColor Cyan
mvn install -q -DskipTests
if ($LASTEXITCODE -ne 0) { throw "Build that bai" }

Write-Host "`n=== 2. Train model ===" -ForegroundColor Cyan
& "$PSScriptRoot\run-training.ps1"

Write-Host "`n=== 3. Kich ban 1: WorkspaceMode ===" -ForegroundColor Cyan
& "$PSScriptRoot\run-benchmark-1-workspace.ps1"

Write-Host "`n=== 4. Kich ban 2: Spark Scaling ===" -ForegroundColor Cyan
& "$PSScriptRoot\run-benchmark-2-spark-scaling.ps1"

Write-Host "`n=== 5. Kich ban 3: In-process vs Remote ===" -ForegroundColor Yellow
Write-Host "Buoc nay can chay 'scripts\run-api.ps1' o MOT CUA SO KHAC truoc, roi chay:"
Write-Host "  scripts\run-benchmark-3-latency.ps1"

Write-Host "`n=== 6. Tong hop ket qua ===" -ForegroundColor Cyan
& "$PSScriptRoot\summarize-results.ps1"

Write-Host "`nHoan tat (tru kich ban 3 can chay API thu cong). Xem results\summary.md" -ForegroundColor Green
