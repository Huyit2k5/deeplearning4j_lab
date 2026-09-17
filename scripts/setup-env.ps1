# Thiet lap bien moi truong cho phien lam viec hien tai (khong set global).
# Dot-source script nay truoc khi chay training/benchmark/api:
#   . .\scripts\setup-env.ps1

$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot"
$env:MAVEN_HOME = "C:\devtools\apache-maven-3.9.16"
$env:Path = "$env:JAVA_HOME\bin;$env:MAVEN_HOME\bin;$env:Path"

# Khoa luong cho OpenMP/OpenBLAS/MKL de tranh oversubscription khi do Spark scaling.
$env:OMP_NUM_THREADS = 1
$env:OPENBLAS_NUM_THREADS = 1
$env:MKL_NUM_THREADS = 1

$env:HADOOP_HOME = "C:\hadoop"
$env:Path = "$env:HADOOP_HOME\bin;$env:Path"

# exec:java (khong fork) chay TRONG cung JVM voi Maven, nen cac co --add-opens/-D
# phai duoc set qua MAVEN_OPTS (JVM cua chinh tien trinh mvn), khong phai truyen
# qua -Dexec.jvmArgs (chi co tac dung khi exec:java fork JVM rieng, o day khong dung).
$env:MAVEN_OPTS = "-Dorg.bytedeco.javacpp.maxbytes=8G " +
    "--add-opens=java.base/java.lang=ALL-UNNAMED " +
    "--add-opens=java.base/java.lang.invoke=ALL-UNNAMED " +
    "--add-opens=java.base/java.lang.reflect=ALL-UNNAMED " +
    "--add-opens=java.base/java.util=ALL-UNNAMED " +
    "--add-opens=java.base/java.util.concurrent.atomic=ALL-UNNAMED " +
    "--add-opens=java.base/java.nio=ALL-UNNAMED " +
    "--add-opens=java.base/java.net=ALL-UNNAMED " +
    "--add-opens=java.base/sun.nio.ch=ALL-UNNAMED " +
    "--add-opens=java.base/sun.security.action=ALL-UNNAMED " +
    "--add-opens=java.base/java.io=ALL-UNNAMED"

Write-Host "Environment ready. JAVA_HOME=$env:JAVA_HOME"
Write-Host "Maven: $(mvn -v | Select-Object -First 1)"
