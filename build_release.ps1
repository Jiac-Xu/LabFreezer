$env:JAVA_HOME='D:\Program Files\AndroidStudio\jbr'
$env:ANDROID_HOME='C:\Users\Administrator\AppData\Local\Android\Sdk'
Set-Location 'D:\AI_DevHub\LabFreezer'
.\gradlew.bat assembleRelease 2>&1
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
