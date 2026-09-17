# Train MLP thuan DL4J tren creditcard.csv va luu model + normalizer.
. "$PSScriptRoot\setup-env.ps1"
Set-Location "$PSScriptRoot\.."

mvn -pl training exec:java `
    "-Dexec.mainClass=vn.huit.dl4j.training.LocalTrainingRunner" `
    "-Dexec.args=data/creditcard.csv 3 32"
