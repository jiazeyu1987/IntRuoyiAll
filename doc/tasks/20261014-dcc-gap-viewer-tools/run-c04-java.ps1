param([string]$Phase='red', [switch]$Regression, [switch]$OnlineOnly)
$ErrorActionPreference='Stop'
$repoRoot=(Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '../../..')).Path
$moduleRoot=Join-Path $repoRoot 'IntRuoyiBackend/yudao-module-dcc'
$privateRoot=Join-Path $PSScriptRoot ('c04-'+$Phase)
$classes=Join-Path $privateRoot 'classes'
New-Item -ItemType Directory -Force -Path $classes | Out-Null
$launcher=Join-Path ([Environment]::GetFolderPath('UserProfile')) '.m2/repository/org/junit/platform/junit-platform-launcher/1.12.2/junit-platform-launcher-1.12.2.jar'
if(!(Test-Path -LiteralPath $launcher)){throw 'JUnit launcher is not installed'}
$dependencies=(Get-Content -Raw -Encoding UTF8 (Join-Path $moduleRoot 'target/dcc-test.classpath')).Trim()
$cp=@($classes,(Join-Path $moduleRoot 'target/classes'),(Join-Path $moduleRoot 'target/test-classes'),$launcher,$dependencies)-join ';'
$mainRoot=Join-Path $moduleRoot 'src/main/java/cn/iocoder/yudao/module/dcc'
$production=@('service/preview/DccControlledPreviewAccessService.java','service/preview/DccPreviewAccessRequest.java','service/preview/DccPreviewAccessResult.java',
 'service/audit/DccControlledFileAccessAuditService.java','service/audit/DccAccessEventCreateCommand.java',
 'dal/dataobject/protection/DccControlledFileAccessEventDO.java','dal/mysql/protection/DccControlledFileAccessEventMapper.java',
 'service/token/DccViewerTokenService.java','controller/admin/file/vo/DccControlledFilePreviewMetadataRespVO.java',
 'service/filepreview/DccOnlineFilePreviewServiceImpl.java')
$sources=@($production|ForEach-Object {Join-Path $mainRoot $_})
$sources+=Join-Path $repoRoot 'IntRuoyiBackend/yudao-framework/yudao-spring-boot-starter-test/src/main/java/cn/iocoder/yudao/framework/test/core/ut/BaseDbUnitTest.java'
$tests=@('service.preview.DccG09PreviewRegisteredAtTest','service.filepreview.DccG09OnlinePreviewRegisteredAtTest')
if($Regression){$tests+=@('service.preview.DccControlledPreviewAccessServiceTest','service.preview.DccPreviewIssuedRoundTransactionTest','service.filepreview.DccOnlineFilePreviewServiceTest')}
if($OnlineOnly){$tests=@('service.filepreview.DccG09OnlinePreviewRegisteredAtTest')}
foreach($test in $tests){$sources+=Join-Path $moduleRoot ('src/test/java/cn/iocoder/yudao/module/dcc/'+$test.Replace('.','/')+'.java')}
$sources+=Join-Path $PSScriptRoot 'G09TestLauncher.java'
$before=@($sources|ForEach-Object {[pscustomobject]@{path=$_.Substring($repoRoot.Length+1).Replace('\','/');sha256=(Get-FileHash -LiteralPath $_).Hash}})
$utf8=[System.Text.UTF8Encoding]::new($false)
[System.IO.File]::WriteAllText((Join-Path $privateRoot 'sources.json'),($before|ConvertTo-Json),$utf8)
$compile=@('-encoding','UTF-8','--release','17','-implicit:none','-sourcepath',('"'+$classes.Replace('\','/')+'"'),'-cp',('"'+$cp.Replace('\','/')+'"'),'-d',('"'+$classes.Replace('\','/')+'"'))
$compile+=@($sources|ForEach-Object {'"'+$_.Replace('\','/')+'"'})
$argsPath=Join-Path $privateRoot 'javac.args'
[System.IO.File]::WriteAllLines($argsPath,$compile,$utf8)
$compileOutput=& javac ('@'+$argsPath) 2>&1
$compileExit=$LASTEXITCODE
[System.IO.File]::WriteAllLines((Join-Path $privateRoot 'compile.log'),@($compileOutput|ForEach-Object {"$_"}),$utf8)
if($compileExit -ne 0){$compileOutput;exit $compileExit}
$runtime=@($classes,(Join-Path $moduleRoot 'src/test/resources'),(Join-Path $moduleRoot 'src/main/resources'),$cp)-join ';'
$run=@('-Dfile.encoding=UTF-8','-cp',('"'+$runtime.Replace('\','/')+'"'),'G09TestLauncher')+@($tests|ForEach-Object {'cn.iocoder.yudao.module.dcc.'+$_})
$argsPath=Join-Path $privateRoot 'java.args'
[System.IO.File]::WriteAllLines($argsPath,$run,$utf8)
Push-Location $moduleRoot
try {$output=& java ('@'+$argsPath) 2>&1;$testExit=$LASTEXITCODE} finally {Pop-Location}
[System.IO.File]::WriteAllLines((Join-Path $privateRoot 'test.log'),@($output|ForEach-Object {"$_"}),$utf8)
$output|Select-Object -Last 90
foreach($entry in $before){if((Get-FileHash -LiteralPath (Join-Path $repoRoot $entry.path)).Hash -ne $entry.sha256){throw ('SOURCE_DRIFT '+$entry.path)}}
Write-Output 'SOURCE_STABILITY PASS'
exit $testExit
