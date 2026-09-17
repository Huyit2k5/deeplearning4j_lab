# Kich ban 1: WorkspaceMode NONE vs ENABLED.
# Dong thoi lay mau Working Set/Private Bytes qua Get-Process de doi chieu voi so lieu JVM-internal
# (Windows khong co VmRSS nhu Linux).
. "$PSScriptRoot\setup-env.ps1"
Set-Location "$PSScriptRoot\.."

$job = Start-Job -ScriptBlock {
    param($root)
    Set-Location $root
    mvn -pl benchmark exec:java `
        "-Dexec.mainClass=vn.huit.dl4j.benchmark.WorkspaceBenchmark" `
        "-Dexec.args=data/creditcard.csv 3 32"
} -ArgumentList (Get-Location).Path

Write-Host "Dang chay benchmark + lay mau Get-Process moi 500ms (Ctrl+C de dung som)..."
$samples = @()
while ($job.State -eq "Running") {
    $procs = Get-Process -Name java -ErrorAction SilentlyContinue
    foreach ($p in $procs) {
        $samples += [PSCustomObject]@{
            Timestamp = Get-Date -Format "o"
            Pid = $p.Id
            WorkingSet64 = $p.WorkingSet64
            PrivateMemorySize64 = $p.PrivateMemorySize64
        }
    }
    Start-Sleep -Milliseconds 500
}
Receive-Job $job
Remove-Job $job

$samples | Export-Csv -Path "results\workspace_benchmark_os_samples.csv" -NoTypeInformation
Write-Host "Da luu mau OS-level vao results\workspace_benchmark_os_samples.csv"
