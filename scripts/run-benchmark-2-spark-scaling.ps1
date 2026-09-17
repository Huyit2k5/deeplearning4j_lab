# Kich ban 2: Spark local scaling (threads 1,2,4,6,8).
# YEU CAU: da cai winutils.exe + HADOOP_HOME (xem README/plan) truoc khi chay script nay.
. "$PSScriptRoot\setup-env.ps1"
Set-Location "$PSScriptRoot\.."

mvn -pl benchmark exec:java `
    "-Dexec.mainClass=vn.huit.dl4j.benchmark.SparkScalingBenchmark" `
    "-Dexec.args=data/creditcard.csv 1 32"
