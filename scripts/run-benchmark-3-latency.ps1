# Kich ban 3: In-process vs Remote inference latency (B=1, B=8).
# YEU CAU: module `api` phai dang chay san (chay scripts\run-api.ps1 o mot cua so khac truoc).
. "$PSScriptRoot\setup-env.ps1"
Set-Location "$PSScriptRoot\.."

Write-Host "=== In-process ==="
mvn -pl benchmark exec:java `
    "-Dexec.mainClass=vn.huit.dl4j.benchmark.InferenceLatencyBenchmark" `
    "-Dexec.args=training/models/fraud_mlp.zip 50 1000"

Write-Host "=== Remote (yeu cau API dang chay tai http://localhost:8080) ==="
mvn -pl benchmark exec:java `
    "-Dexec.mainClass=vn.huit.dl4j.benchmark.RemoteLatencyClient" `
    "-Dexec.args=http://localhost:8080 20 200"
