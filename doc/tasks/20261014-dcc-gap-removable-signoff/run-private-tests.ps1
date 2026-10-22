param([ValidateSet('red','default-red','green','regression')][string]$Phase='regression')
$ErrorActionPreference='Stop'
$taskRoot=$PSScriptRoot
$repoRoot=(Resolve-Path -LiteralPath (Join-Path $taskRoot '../../..')).Path
$moduleRoot=Join-Path $repoRoot 'IntRuoyiBackend/yudao-module-dcc'
$outputRoot=Join-Path $taskRoot ($Phase+'/private-classes')
$null=New-Item -ItemType Directory -Path $outputRoot -Force
$launcherJar=Join-Path ([Environment]::GetFolderPath('UserProfile')) '.m2/repository/org/junit/platform/junit-platform-launcher/1.12.2/junit-platform-launcher-1.12.2.jar'
if(!(Test-Path -LiteralPath $launcherJar)){throw 'JUnit launcher dependency missing'}
$dependencyClasspath=(Get-Content -Raw -Encoding UTF8 -LiteralPath (Join-Path $moduleRoot 'target/dcc-test.classpath')).Trim()
$classpath=@($outputRoot,(Join-Path $moduleRoot 'target/classes'),(Join-Path $moduleRoot 'target/test-classes'),$launcherJar,$dependencyClasspath)-join ';'
$production=@('service/route/DccPublicMatrixNativeDerivation.java','service/route/DccApprovalMatrixCapabilityException.java','service/file/DccControlledFileApprovalRouteAssigneeResolver.java','service/file/DccApprovalMatrixTokenService.java','service/file/DccControlledFileRouteReadinessService.java','service/file/DccFrozenApprovalSignatures.java','service/file/DccControlledFileWorkflowServiceImpl.java')
$tests=@('DccPublicMatrixNativeDerivationTest','DccControlledFileApprovalRouteAssigneeResolverTest','DccRemovableSignoffContractTest')
if($Phase -eq 'regression'){
    $tests+=@('DccFrozenApprovalSignaturesTest','DccNativeSupervisorSourceTest','DccControlledFileRouteReadinessServiceTest')
}
$sources=@($production|ForEach-Object {Join-Path $moduleRoot ('src/main/java/cn/iocoder/yudao/module/dcc/'+$_)})
$sources+=Join-Path $repoRoot 'IntRuoyiBackend/yudao-module-bpm/src/main/java/cn/iocoder/yudao/module/bpm/api/task/dto/BpmProcessInstanceCreateReqDTO.java'
$sources+=@($tests|ForEach-Object {Join-Path $moduleRoot ('src/test/java/cn/iocoder/yudao/module/dcc/service/file/'+$_+'.java')})
$sources+=Join-Path $taskRoot 'G04TestLauncher.java'
$sourceHashes=@($sources|ForEach-Object {[ordered]@{path=$_.Substring($repoRoot.Length+1).Replace('\','/');sha256=(Get-FileHash -Algorithm SHA256 -LiteralPath $_).Hash.ToLowerInvariant()}})
[System.IO.File]::WriteAllText((Join-Path $taskRoot ($Phase+'/compiled-sources.json')),($sourceHashes|ConvertTo-Json -Depth 3),[System.Text.UTF8Encoding]::new($false))
$compileArguments=@('-J-Dfile.encoding=UTF-8','-J-Dsun.stdout.encoding=UTF-8','-J-Dsun.stderr.encoding=UTF-8','-encoding','UTF-8','--release','17','-implicit:none','-sourcepath',('"'+$outputRoot.Replace('\','/')+'"'),'-cp',('"'+$classpath.Replace('\','/')+'"'),'-d',('"'+$outputRoot.Replace('\','/')+'"'))
$compileArguments+=@($sources|ForEach-Object {'"'+$_.Replace('\','/')+'"'})
$compileArgsPath=Join-Path $taskRoot ($Phase+'/javac.args')
[System.IO.File]::WriteAllLines($compileArgsPath,$compileArguments,[System.Text.UTF8Encoding]::new($false))
$compileOutput=& javac ('@'+$compileArgsPath) 2>&1
$compileExit=$LASTEXITCODE
[System.IO.File]::WriteAllLines((Join-Path $taskRoot ($Phase+'/compile.log')),@($compileOutput|ForEach-Object {"$_"}),[System.Text.UTF8Encoding]::new($false))
$compileOutput|ForEach-Object {"$_"}
if($compileExit -ne 0){exit $compileExit}
$runtimeClasspath=@($outputRoot,(Join-Path $moduleRoot 'src/test/resources'),(Join-Path $moduleRoot 'src/main/resources'),$classpath)-join ';'
$runArguments=@('-Dfile.encoding=UTF-8','-cp',('"'+$runtimeClasspath.Replace('\','/')+'"'),'G04TestLauncher')+@($tests|ForEach-Object {'cn.iocoder.yudao.module.dcc.service.file.'+$_})
$runArgsPath=Join-Path $taskRoot ($Phase+'/java.args')
[System.IO.File]::WriteAllLines($runArgsPath,$runArguments,[System.Text.UTF8Encoding]::new($false))
Push-Location $moduleRoot
try{$runOutput=& java ('@'+$runArgsPath) 2>&1;$runExit=$LASTEXITCODE}
finally{Pop-Location}
[System.IO.File]::WriteAllLines((Join-Path $taskRoot ($Phase+'/test.log')),@($runOutput|ForEach-Object {"$_"}),[System.Text.UTF8Encoding]::new($false))
$runOutput|Select-Object -Last 95|ForEach-Object {"$_"}
$stable=$true
foreach($entry in $sourceHashes){
    if((Get-FileHash -Algorithm SHA256 -LiteralPath (Join-Path $repoRoot $entry.path)).Hash.ToLowerInvariant() -ne $entry.sha256){
        $stable=$false
        Write-Output ('SOURCE_DRIFT: '+$entry.path)
    }
}
if(!$stable){exit 2}
Write-Output 'SOURCE_STABILITY: PASS (all explicitly compiled sources)'
exit $runExit
