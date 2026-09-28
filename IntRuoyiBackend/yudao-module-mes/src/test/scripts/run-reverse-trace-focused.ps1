param(
    [string[]] $Selectors = @('cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrFormalReverseTraceAdapterTest'),
    [string[]] $ProductionSources = @('MesProEdhrFormalReverseTraceAdapter.java', 'MesProEdhrFormalReverseTraceAdapterConfiguration.java', 'MesProEdhrReverseTraceModels.java'),
    [string[]] $TestSources = @('MesProEdhrFormalReverseTraceAdapterTest.java')
)
$ErrorActionPreference = 'Stop'
$moduleRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../../..'))
$backendRoot = Split-Path $moduleRoot
$reportPath = Join-Path $moduleRoot 'target/surefire-reports/TEST-cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrFormalReverseTraceAdapterTest.xml'
[xml] $report = Get-Content -LiteralPath $reportPath -Raw
$dependencyClasspath = ($report.testsuite.properties.property | Where-Object name -eq 'java.class.path').value
if (-not $dependencyClasspath) { throw 'Missing actual Surefire classpath' }
$entries = @($dependencyClasspath -split ';' | Where-Object { $_ -ne '' })
$dependencyClasspath = $entries -join ';'
foreach ($entry in $entries) {
    if (-not (Test-Path -LiteralPath $entry)) { throw "Missing classpath entry: $entry" }
    if ($entry -match '[\\/]target[\\/]' -and -not $entry.StartsWith($backendRoot, [StringComparison]::OrdinalIgnoreCase)) {
        throw "Foreign workspace target: $entry"
    }
}
$console = 'E:/Int/DevCache/maven-repository/org/junit/platform/junit-platform-console-standalone/1.12.2/junit-platform-console-standalone-1.12.2.jar'
if (-not (Test-Path -LiteralPath $console)) { throw 'Existing JUnit console 1.12.2 is missing' }
$outputDir = Join-Path $moduleRoot 'target/reverse-trace-round2'
New-Item -ItemType Directory -Path $outputDir -Force | Out-Null
$classpath = "$outputDir;$dependencyClasspath;$console"
$sourcePackage = 'java/cn/iocoder/yudao/module/mes/service/pro/batchrecord'
$sourcePaths = @($ProductionSources | ForEach-Object { Join-Path $moduleRoot "src/main/$sourcePackage/$_" })
$sourcePaths += @($TestSources | ForEach-Object { Join-Path $moduleRoot "src/test/$sourcePackage/$_" })
foreach ($source in $sourcePaths) { if (-not (Test-Path -LiteralPath $source)) { throw "Missing source: $source" } }
function Quote-JavaArgument([string] $value) { '"' + $value.Replace('\', '/') + '"' }
$compileArgs = @('-encoding', 'UTF-8', '--release', '17', '-classpath', (Quote-JavaArgument $classpath), '-d', (Quote-JavaArgument $outputDir))
$compileArgs += @($sourcePaths | ForEach-Object { Quote-JavaArgument $_ })
$compileArgsPath = Join-Path $outputDir 'javac.args'
[IO.File]::WriteAllLines($compileArgsPath, $compileArgs, [Text.UTF8Encoding]::new($false))
Write-Output ('Compiling current sources: ' + ($ProductionSources + $TestSources -join ', '))
& javac '-J-Duser.language=en' '-J-Dfile.encoding=UTF-8' "@$compileArgsPath"
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
$runArgs = @('-Xmx768m', '-classpath', (Quote-JavaArgument $classpath), 'org.junit.platform.console.ConsoleLauncher', 'execute', '--disable-ansi-colors', '--details=summary', '--fail-if-no-tests')
foreach ($selector in $Selectors) {
    $runArgs += $(if ($selector.Contains('#')) { '--select-method' } else { '--select-class' })
    $runArgs += Quote-JavaArgument $selector
}
$runArgsPath = Join-Path $outputDir 'java.args'
[IO.File]::WriteAllLines($runArgsPath, $runArgs, [Text.UTF8Encoding]::new($false))
Write-Output ('JUnit selection: ' + ($Selectors -join ', '))
& java "@$runArgsPath"
exit $LASTEXITCODE
