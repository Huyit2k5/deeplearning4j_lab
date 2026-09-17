# Chay Spring Boot API + web demo tai http://localhost:8080
# LUU Y: dung "java -jar" tren fat-jar da package, KHONG dung "mvn spring-boot:run" -
# vi spring-boot:run fork mot tien trinh moi voi classpath day du truyen qua dong lenh,
# va classpath cua DL4J+Spark qua dai vuot gioi han dong lenh cua Windows
# (loi "CreateProcess error=206, The filename or extension is too long").
. "$PSScriptRoot\setup-env.ps1"
Set-Location "$PSScriptRoot\.."

mvn -pl api package -q -DskipTests
if ($LASTEXITCODE -ne 0) { throw "Package api that bai" }

java -jar api\target\api-1.0.0.jar
