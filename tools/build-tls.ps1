param([Parameter(Mandatory=$true)][string]$NdkRoot)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$builder = Join-Path $NdkRoot 'ndk-build.cmd'
if (!(Test-Path -LiteralPath $builder)) { throw "找不到 NDK 构建入口：$builder" }
$nativeRoot = Join-Path $projectRoot 'native/tls'
$outRoot = Join-Path $projectRoot '.work/tls'
& $builder 'HOST_OS=windows' 'HOST_ARCH=x86_64' "NDK_PROJECT_PATH=$nativeRoot" "APP_BUILD_SCRIPT=$nativeRoot/Android.mk" "NDK_APPLICATION_MK=$nativeRoot/Application.mk" 'APP_ABI=armeabi-v7a' 'APP_PLATFORM=android-19' "NDK_OUT=$outRoot/obj" "NDK_LIBS_OUT=$outRoot/libs"
if ($LASTEXITCODE -ne 0) { throw "TLS 编译失败：$LASTEXITCODE" }
Copy-Item -LiteralPath "$outRoot/libs/armeabi-v7a/libntvtls.so" -Destination (Join-Path $projectRoot 'app/src/main/jniLibs/armeabi-v7a/libntvtls.so') -Force

