param([ValidateSet('red','green','regression','a20-red','a20-green','a20-regression','a23-body','a24-query','a24-db','a24-revision','m10-query-fixture','a34-red','a34-green')][string]$Phase='red')
$ErrorActionPreference='Stop'
$g06Root=$PSScriptRoot
$g06Repo=(Resolve-Path -LiteralPath (Join-Path $g06Root '../../..')).Path
$g06Module=Join-Path $g06Repo 'IntRuoyiBackend/yudao-module-dcc'
$g06Output=Join-Path $g06Root ($Phase+'/classes')
$null=New-Item -ItemType Directory -Path $g06Output -Force
# A second run must fail before it can overwrite this phase's args/classes/evidence.
$g06RunLock=[IO.File]::Open((Join-Path $g06Root ($Phase+'/run.lock')),[IO.FileMode]::OpenOrCreate,[IO.FileAccess]::ReadWrite,[IO.FileShare]::None)
$g06RunId=[Guid]::NewGuid().ToString('N')
$g06Started=[DateTime]::UtcNow.ToString('o')
$g06Launcher=Join-Path ([Environment]::GetFolderPath('UserProfile')) '.m2/repository/org/junit/platform/junit-platform-launcher/1.12.2/junit-platform-launcher-1.12.2.jar'
if(!(Test-Path -LiteralPath $g06Launcher)){throw 'Missing JUnit launcher dependency'}
$g06Dependency=(Get-Content -LiteralPath (Join-Path $g06Module 'target/dcc-test.classpath') -Raw -Encoding UTF8).Trim()
$g06ReactorClasses=@('IntRuoyiBackend/yudao-module-bpm/target/classes','IntRuoyiBackend/yudao-module-system/target/classes','IntRuoyiBackend/yudao-module-infra/target/classes','IntRuoyiBackend/yudao-framework/yudao-spring-boot-starter-test/target/classes')|ForEach-Object{Join-Path $g06Repo $_}
$g06Classpath=@($g06Output,(Join-Path $g06Module 'src/test/resources'),(Join-Path $g06Module 'src/main/resources'),(Join-Path $g06Module 'target/classes'),(Join-Path $g06Module 'target/test-classes'))+@($g06ReactorClasses)+@($g06Launcher,$g06Dependency)
$g06Classpath=$g06Classpath -join ';'
$g06Production=@('service/file/DccControlledFileUploadServiceImpl.java','service/file/DccControlledFileAttachmentService.java','service/file/DccControlledFileAttachmentServiceImpl.java','controller/admin/file/vo/DccControlledFileCheckinReqVO.java','service/upload/DccUploadTicketServiceImpl.java','service/file/DccControlledFileSourceOwnershipService.java','service/file/DccSourceUploadSession.java','service/file/DccControlledFileVersionSourceRollback.java','dal/mysql/file/DccControlledFileAttachmentMapper.java','dal/mysql/file/DccControlledFileCheckoutMapper.java','dal/mysql/protection/DccControlledFileTemporaryFileMapper.java')
$g06Production+=@('service/file/DccControlledFileVersionPolicy.java','service/file/DccControlledFileVersionPolicyProperties.java','service/file/DccControlledFileRevisionChangeType.java','dal/mysql/file/DccControlledFileSourceOwnershipMapper.java')
if($Phase.StartsWith('a20-') -or $Phase.StartsWith('a24-') -or $Phase.StartsWith('a34-') -or $Phase -in @('a23-body','m10-query-fixture')){
    $g06Production+=@('service/file/DccControlledFileQueryServiceImpl.java','service/file/DccControlledFileDetailAuthorizationGuard.java','service/file/DccControlledFileQueryService.java','service/file/DccControlledFileBodyReadContext.java','service/file/DccOnlyOfficePreviewTokenService.java','service/file/DccVerifiedOfficeReadActor.java','service/file/DccControlledFileRevisionOptions.java','controller/admin/file/vo/DccControlledFilePreviewMetadataRespVO.java','dal/dataobject/file/DccControlledFileCheckoutDO.java')
    $g06Production+=@('service/file/DccControlledFileRevisionServiceImpl.java','controller/admin/file/vo/DccControlledFileRespVO.java','dal/dataobject/file/DccControlledFileDO.java','enums/DccControlledFileStatusEnum.java')
    $g06Production+=@('controller/admin/file/vo/DccControlledFileSubmitIterationReqVO.java')
    $g06Production+=@('service/file/relations/DccRelationAccessPolicyImpl.java')
}
$g06Tests=@('service/upload/DccCheckinAttachmentUploadContextTest.java','service/file/DccRevisionAttachmentsDatabaseTest.java')
if($Phase -eq 'a24-query'){$g06Tests=@('service/file/DccControlledFileQueryServiceTest.java')}
if($Phase -eq 'a24-db'){$g06Tests=@('service/file/DccRevisionDatabaseTest.java','service/file/DccCheckinDraftInitializationDatabaseTest.java','service/file/DccW08PreparedCreationDatabaseTest.java','service/file/DccW08PreparedSourceBoundaryTest.java','service/file/DccWorkflowSelectedIterationDatabaseTest.java')}
if($Phase -eq 'a24-db'){$g06Production+=@('service/file/DccControlledFileWorkflowServiceImpl.java','service/file/DccApplicationExecutionContractService.java')}
if($Phase -eq 'a24-revision'){$g06Tests=@('service/file/DccRevisionDatabaseTest.java')}
if($Phase -eq 'a23-body'){$g06Tests=@('service/file/DccControlledFileBodyReadCurrentSourceTest.java')}
if($Phase -eq 'm10-query-fixture'){$g06Tests=@('service/file/DccRevisionAttachmentsDatabaseTest.java')}
if($Phase.StartsWith('a34-')){
    $g06Tests=@('service/file/DccCustomDraftInheritanceIntegrationTest.java')
    $g06Production+=@('service/file/DccApplicationExecutionContractService.java','service/file/DccApplicationCustomMatrix.java','service/file/DccApplicationCustomMatrixService.java')
}
if($Phase.EndsWith('regression')){$g06Tests+=@('service/file/DccOrdinaryAttachmentFactsTest.java','service/file/DccTrainingAttachmentTransactionTest.java','service/file/DccTrainingAttachmentServiceTest.java','service/file/DccRevisionRollbackSynchronizationTest.java')}
$g06Sources=@($g06Production|ForEach-Object{Join-Path $g06Module ('src/main/java/cn/iocoder/yudao/module/dcc/'+$_)})
$g06Sources+=@($g06Tests|ForEach-Object{Join-Path $g06Module ('src/test/java/cn/iocoder/yudao/module/dcc/'+$_)})
$g06Sources+=Join-Path $g06Root 'G06TestLauncher.java'
if($Phase -in @('a24-db','a24-revision')){
    $g06Sources+=Join-Path $g06Repo 'IntRuoyiBackend/yudao-module-bpm/src/main/java/cn/iocoder/yudao/module/bpm/api/task/dto/BpmProcessInstanceCreateReqDTO.java'
}
if($Phase -eq 'a24-db'){$g06Sources+=Join-Path $g06Module 'src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccRevisionAttachmentsDatabaseTest.java'}
if($Phase -eq 'a24-db'){
    $g06Sources+=Join-Path $g06Module 'src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccPreparedExecutionInheritanceDatabaseTest.java'
}
if($Phase.StartsWith('a34-')){
    $g06Sources+=Join-Path $g06Module 'src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccPreparedExecutionInheritanceDatabaseTest.java'
    $g06Sources+=Join-Path $g06Module 'src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccRevisionAttachmentsDatabaseTest.java'
}
$g06Hashes=@($g06Sources|ForEach-Object{[ordered]@{path=$_.Substring($g06Repo.Length+1).Replace('\','/');sha256=(Get-FileHash -LiteralPath $_ -Algorithm SHA256).Hash.ToLowerInvariant()}})
[IO.File]::WriteAllText((Join-Path $g06Root ($Phase+'/compiled-sources.json')),($g06Hashes|ConvertTo-Json),[Text.UTF8Encoding]::new($false))
[IO.File]::WriteAllText((Join-Path $g06Root ($Phase+'/receipt.json')),([ordered]@{phase=$Phase;run_id=$g06RunId;status='COMPILING';started_at_utc=$g06Started;javac_exit=$null;junit_exit=$null;shared_target_write=$false;drift=@()}|ConvertTo-Json),[Text.UTF8Encoding]::new($false))
$g06Args=@('-J-Dfile.encoding=UTF-8','-encoding','UTF-8','--release','17','-implicit:none','-sourcepath',('"'+$g06Output.Replace('\','/')+'"'),'-cp',('"'+$g06Classpath.Replace('\','/')+'"'),'-d',('"'+$g06Output.Replace('\','/')+'"'))
$g06Args+=@($g06Sources|ForEach-Object{'"'+$_.Replace('\','/')+'"'})
$g06ArgsFile=Join-Path $g06Root ($Phase+'/javac.args');[IO.File]::WriteAllLines($g06ArgsFile,$g06Args,[Text.UTF8Encoding]::new($false))
$g06Compile=& javac ('@'+$g06ArgsFile) 2>&1;$g06CompileExit=$LASTEXITCODE
[IO.File]::WriteAllLines((Join-Path $g06Root ($Phase+'/compile.log')),@($g06Compile|ForEach-Object{"$_"}),[Text.UTF8Encoding]::new($false))
$g06Compile|ForEach-Object{"$_"};if($g06CompileExit -ne 0){
    [IO.File]::WriteAllText((Join-Path $g06Root ($Phase+'/receipt.json')),([ordered]@{phase=$Phase;run_id=$g06RunId;status='COMPILE_FAILED';started_at_utc=$g06Started;javac_exit=$g06CompileExit;junit_exit=$null;shared_target_write=$false;drift=@();finished_at_utc=[DateTime]::UtcNow.ToString('o')}|ConvertTo-Json),[Text.UTF8Encoding]::new($false))
    exit $g06CompileExit
}
$g06Selectors=@($g06Tests|ForEach-Object{'cn.iocoder.yudao.module.dcc.'+$_.Replace('/','.').Replace('.java','')})
if($Phase -eq 'm10-query-fixture'){
    $g06FixtureText=Get-Content -LiteralPath (Join-Path $g06Module 'src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccRevisionAttachmentsDatabaseTest.java') -Raw -Encoding UTF8
    $g06Methods=[regex]::Matches($g06FixtureText,'@Test void (a20_\w+|a23_actual\w+|a23_delayed\w+|a29_\w+)\(')
    if($g06Methods.Count -ne 16){throw 'Expected exactly the 16 QueryFixture scenarios selected by Root M10'}
    $g06Selectors=@($g06Methods|ForEach-Object{'method:cn.iocoder.yudao.module.dcc.service.file.DccRevisionAttachmentsDatabaseTest#'+$_.Groups[1].Value})
}
if($Phase.StartsWith('a34-')){
    $g06A34Text=Get-Content -LiteralPath (Join-Path $g06Module 'src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccCustomDraftInheritanceIntegrationTest.java') -Raw -Encoding UTF8
    $g06A34Methods=[regex]::Matches($g06A34Text,'@Test void (a34_\w+)\(')
    $g06Selectors=@($g06A34Methods|ForEach-Object{'method:cn.iocoder.yudao.module.dcc.service.file.DccCustomDraftInheritanceIntegrationTest#'+$_.Groups[1].Value})
}
$g06RootCheck=if($Phase -eq 'm10-query-fixture'){'-Dg06.verify.root.modes=true'}else{'-Dg06.verify.root.modes=false'}
$g06RunArgs=@('-Dfile.encoding=UTF-8',('-Ddcc.repository.root="'+$g06Repo.Replace('\','/')+'"'),$g06RootCheck,'-cp',('"'+$g06Classpath.Replace('\','/')+'"'),'G06TestLauncher')+@($g06Selectors|ForEach-Object{'"'+$_+'"'})
$g06RunFile=Join-Path $g06Root ($Phase+'/java.args');[IO.File]::WriteAllLines($g06RunFile,$g06RunArgs,[Text.UTF8Encoding]::new($false))
if($Phase -in @('a24-db','a24-revision')){Push-Location $g06Module}
try{$g06Result=& java ('@'+$g06RunFile) 2>&1;$g06Exit=$LASTEXITCODE}
finally{if($Phase -in @('a24-db','a24-revision')){Pop-Location}}
[IO.File]::WriteAllLines((Join-Path $g06Root ($Phase+'/test.log')),@($g06Result|ForEach-Object{"$_"}),[Text.UTF8Encoding]::new($false))
$g06Drift=@($g06Hashes|Where-Object{(Get-FileHash -LiteralPath (Join-Path $g06Repo $_.path) -Algorithm SHA256).Hash.ToLowerInvariant() -ne $_.sha256})
[IO.File]::WriteAllText((Join-Path $g06Root ($Phase+'/receipt.json')),([ordered]@{phase=$Phase;run_id=$g06RunId;status='FINISHED';started_at_utc=$g06Started;javac_exit=$g06CompileExit;junit_exit=$g06Exit;shared_target_write=$false;drift=@($g06Drift);finished_at_utc=[DateTime]::UtcNow.ToString('o')}|ConvertTo-Json -Depth 5),[Text.UTF8Encoding]::new($false))
$g06Result|Select-Object -Last 90|ForEach-Object{"$_"}
if($g06Drift.Count -gt 0){throw 'Compiled source drift: inspect receipt'}
exit $g06Exit
