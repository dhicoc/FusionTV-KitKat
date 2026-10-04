param(
    [Parameter(Mandatory=$true)][string]$AndroidSdkRoot,
    [Parameter(Mandatory=$true)][string]$Serial,
    [string]$AdbPath,
    [int]$AdbPort = 5037,
    [string]$TargetApk,
    [string]$MinifiedNetworkClass,
    [string]$DebugKeystore = (Join-Path $env:USERPROFILE '.android/debug.keystore')
)
$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$outRoot = Join-Path $repoRoot '.work/review-regression'
$toolsRoot = Join-Path $AndroidSdkRoot 'build-tools/36.0.0'
$androidJar = Join-Path $AndroidSdkRoot 'platforms/android-35/android.jar'
$ijkClasses = Join-Path $repoRoot 'ijkplayer/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes'
if (!$TargetApk) { $TargetApk = Join-Path $repoRoot 'app/build/outputs/apk/leanback/debug/leanback.apk' }
if (!$AdbPath) { $AdbPath = Join-Path $AndroidSdkRoot 'platform-tools/adb.exe' }
if (!$env:JAVA_HOME) { throw '请先配置 JAVA_HOME，并完成 :app:assembleLeanbackDebug。' }
foreach ($path in @($TargetApk, $ijkClasses, $DebugKeystore, $androidJar, $AdbPath)) {
    if (!(Test-Path -LiteralPath $path)) { throw "缺少测试前置文件：$path" }
}
New-Item -ItemType Directory -Force "$outRoot/classes", "$outRoot/dex" | Out-Null
& "$env:JAVA_HOME/bin/javac.exe" -source 8 -target 8 -encoding UTF-8 -classpath "$androidJar;$ijkClasses" -d "$outRoot/classes" "$PSScriptRoot/ReviewInstrumentation.java"
if ($LASTEXITCODE -ne 0) { throw '测试 Java 编译失败' }
& "$env:JAVA_HOME/bin/jar.exe" cf "$outRoot/tests.jar" -C "$outRoot/classes" .
if ($LASTEXITCODE -ne 0) { throw '测试 JAR 打包失败' }
& "$toolsRoot/d8.bat" --min-api 19 --lib $androidJar --classpath $ijkClasses --output "$outRoot/dex" "$outRoot/tests.jar"
if ($LASTEXITCODE -ne 0) { throw '测试 DEX 编译失败' }
@'
<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="local.fusion.review">
    <uses-sdk android:minSdkVersion="19" android:targetSdkVersion="28" />
    <application android:label="Fusion Review Checks" />
    <instrumentation android:name="local.fusion.review.ReviewInstrumentation" android:targetPackage="com.fongmi.android.tv" />
</manifest>
'@ | Set-Content "$outRoot/AndroidManifest.xml" -Encoding utf8
& "$toolsRoot/aapt.exe" package -f -M "$outRoot/AndroidManifest.xml" -I $androidJar -F "$outRoot/tests-unsigned.apk"
if ($LASTEXITCODE -ne 0) { throw '测试 APK 打包失败' }
Copy-Item "$outRoot/dex/classes.dex" "$outRoot/classes.dex" -Force
Push-Location $outRoot
try { & "$toolsRoot/aapt.exe" add -f tests-unsigned.apk classes.dex } finally { Pop-Location }
if ($LASTEXITCODE -ne 0) { throw '测试 DEX 装入失败' }
& "$toolsRoot/zipalign.exe" -f 4 "$outRoot/tests-unsigned.apk" "$outRoot/tests-aligned.apk"
if ($LASTEXITCODE -ne 0) { throw '测试 APK 对齐失败' }
& "$toolsRoot/apksigner.bat" sign --ks $DebugKeystore --ks-key-alias androiddebugkey --ks-pass pass:android --key-pass pass:android --out "$outRoot/tests.apk" "$outRoot/tests-aligned.apk"
if ($LASTEXITCODE -ne 0) { throw '测试 APK 签名失败' }

function Invoke-TestAdb([string[]]$Arguments, [int]$TimeoutMs = 60000) {
    $info = [Diagnostics.ProcessStartInfo]::new()
    $info.FileName = $AdbPath
    $info.UseShellExecute = $false
    $info.CreateNoWindow = $true
    $info.RedirectStandardOutput = $true
    $info.RedirectStandardError = $true
    foreach ($argument in (@('-P', "$AdbPort", '-s', $Serial) + $Arguments)) { $info.ArgumentList.Add($argument) }
    $process = [Diagnostics.Process]::Start($info)
    try {
        $stdout = $process.StandardOutput.ReadToEndAsync()
        $stderr = $process.StandardError.ReadToEndAsync()
        if (!$process.WaitForExit($TimeoutMs)) { $process.Kill(); throw "ADB 超时：$($Arguments -join ' ')" }
        $output = $stdout.GetAwaiter().GetResult() + $stderr.GetAwaiter().GetResult()
        if ($process.ExitCode -ne 0) { throw $output }
        return $output
    } finally { $process.Dispose() }
}
$sdkLevel = (Invoke-TestAdb @('shell', 'getprop', 'ro.build.version.sdk') 10000).Trim()
if ($sdkLevel -ne '19') { throw "需要独立 API19 测试设备，当前 SDK=$sdkLevel" }
Invoke-TestAdb @('install', '-r', $TargetApk)
Invoke-TestAdb @('install', '-r', "$outRoot/tests.apk")
$instrumentArgs = @('shell', 'am', 'instrument', '-r', '-w')
if ($MinifiedNetworkClass) { $instrumentArgs += @('-e', 'networkClass', $MinifiedNetworkClass) }
$instrumentArgs += 'local.fusion.review/local.fusion.review.ReviewInstrumentation'
$result = Invoke-TestAdb $instrumentArgs
$result | Set-Content "$outRoot/result.log" -Encoding utf8
$result
if ($result -notmatch 'INSTRUMENTATION_RESULT: stream=PASS' -or $result -notmatch 'INSTRUMENTATION_CODE: -1') {
    throw 'Instrumentation 未通过；检查 result.log，不能只看 adb 退出码。'
}
