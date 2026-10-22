param([string]$Phase = 'red', [string]$TestClass = 'DccControlledFileQueryServiceTest', [string]$MethodPrefix = 'g07_')
$ErrorActionPreference = 'Stop'
$taskRoot = $PSScriptRoot
$repo = [IO.Path]::GetFullPath((Join-Path $taskRoot '../../..'))
$module = Join-Path $repo 'IntRuoyiBackend/yudao-module-dcc'
$output = Join-Path $taskRoot "verification/private-$Phase"
New-Item -ItemType Directory -Force -Path $output | Out-Null
$dependencyFile = Join-Path $module 'target/dcc-test.classpath'
$dependencies = (Get-Content -LiteralPath $dependencyFile -Raw -Encoding UTF8).Trim()
$platformEngine = $dependencies.Split(';') | Where-Object { $_ -match 'junit-platform-engine-[0-9.]+\.jar$' }
if ($platformEngine.Count -ne 1) { throw 'matching JUnit platform engine must be unique' }
$launcher = $platformEngine.Replace('junit-platform-engine', 'junit-platform-launcher')
if (!(Test-Path -LiteralPath $launcher)) { throw "matching JUnit launcher missing: $launcher" }
# Existing compiled neighbors and Maven dependency jars are read-only development dependencies.
# This is deliberately not a reactor/Maven/runtime acceptance claim.
$classpath = "$module/target/classes;$module/target/test-classes;$launcher;$dependencies"
$productionNames = @('DccControlledFileQueryServiceImpl','DccControlledFileDetailAuthorizationGuard',
    'DccControlledFileQueryService','DccControlledFileBodyReadContext','DccOnlyOfficePreviewTokenService','DccVerifiedOfficeReadActor',
    'DccControlledFileRevisionOptions')
$sources = @("$taskRoot/G07DevelopmentTestRunner.java")
foreach ($name in $productionNames) {
    $file = "$module/src/main/java/cn/iocoder/yudao/module/dcc/service/file/$name.java"
    if (Test-Path -LiteralPath $file) { $sources += $file }
}
$sources += "$module/src/test/java/cn/iocoder/yudao/module/dcc/service/file/$TestClass.java"
$sources += "$module/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/file/DccControlledFileController.java"
$sources += "$module/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/file/vo/DccControlledFilePreviewMetadataRespVO.java"
$sources += "$module/src/main/java/cn/iocoder/yudao/module/dcc/service/file/access/DccBusinessFileAccessProvider.java"
$before = $sources | Get-FileHash -Algorithm SHA256
$quote = { param($value) '"' + $value.Replace('\','/') + '"' }
$compileArgs = @('-encoding','UTF-8','-cp',(& $quote $classpath),'-d',(& $quote $output))
$compileArgs += $sources | ForEach-Object { & $quote $_ }
$compileArgsFile = Join-Path $output 'javac.args'
[IO.File]::WriteAllLines($compileArgsFile, $compileArgs, [Text.UTF8Encoding]::new($false))
& javac "@$compileArgsFile"
if ($LASTEXITCODE -ne 0) { throw "private source compile exit $LASTEXITCODE" }
$runArgs = @('-Dfile.encoding=UTF-8','-cp',(& $quote "$output;$classpath"),'G07DevelopmentTestRunner',
    "cn.iocoder.yudao.module.dcc.service.file.$TestClass")
if ($MethodPrefix) { $runArgs += $MethodPrefix }
$runArgsFile = Join-Path $output 'java.args'
[IO.File]::WriteAllLines($runArgsFile, $runArgs, [Text.UTF8Encoding]::new($false))
& java "@$runArgsFile" 2>&1 | Tee-Object -FilePath (Join-Path $output 'result.txt')
$testExit = $LASTEXITCODE
$after = $sources | Get-FileHash -Algorithm SHA256
for ($index=0; $index -lt $before.Count; $index++) {
    if ($before[$index].Hash -ne $after[$index].Hash) { throw "source changed during private verification: $($before[$index].Path)" }
}
$before | ForEach-Object { "$($_.Hash) $($_.Path)" } | Set-Content -LiteralPath (Join-Path $output 'source-hashes.txt') -Encoding UTF8
"G07_PRIVATE_DEVELOPMENT_EXIT=$testExit; source stable; no target writes"
exit $testExit
