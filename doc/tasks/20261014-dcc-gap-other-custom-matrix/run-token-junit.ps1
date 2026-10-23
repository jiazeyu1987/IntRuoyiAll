param([Parameter(Mandatory=$true)][ValidateSet('BASELINE','RED','GREEN','REGRESSION')][string]$Phase,[ValidatePattern('^[a-zA-Z0-9-]+$')][string]$RunId='1')
$ErrorActionPreference='Stop'
$g03Task=[System.IO.Path]::GetFullPath($PSScriptRoot)
$g03Root=[System.IO.Path]::GetFullPath((Join-Path $g03Task '../../..'))
$g03Run=Join-Path $g03Task ('token-junit/'+$Phase.ToLowerInvariant()+'-'+$RunId)
if(Test-Path -LiteralPath $g03Run){throw 'Never reuse prior token class output'}
New-Item -ItemType Directory -Path ($g03Run+'/classes') -Force | Out-Null
& python -X utf8 (Join-Path $g03Task 'token-test-support.py') ($g03Run+'/support')
if($LASTEXITCODE -ne 0){throw 'Token support generation failed'}
$g03Deps=Join-Path ([Environment]::GetFolderPath('UserProfile')) '.m2/repository'
$g03Artifacts=@(
'org/projectlombok/lombok/1.18.46/lombok-1.18.46.jar','cn/hutool/hutool-all/5.8.44/hutool-all-5.8.44.jar',
'com/fasterxml/jackson/core/jackson-databind/2.17.2/jackson-databind-2.17.2.jar','com/fasterxml/jackson/core/jackson-core/2.17.2/jackson-core-2.17.2.jar','com/fasterxml/jackson/core/jackson-annotations/2.17.2/jackson-annotations-2.17.2.jar','com/fasterxml/jackson/datatype/jackson-datatype-jsr310/2.17.2/jackson-datatype-jsr310-2.17.2.jar',
'org/slf4j/slf4j-api/2.0.17/slf4j-api-2.0.17.jar','jakarta/annotation/jakarta.annotation-api/2.1.1/jakarta.annotation-api-2.1.1.jar','org/springframework/spring-context/6.2.9/spring-context-6.2.9.jar','org/springframework/boot/spring-boot/3.5.14/spring-boot-3.5.14.jar',
'org/junit/jupiter/junit-jupiter-api/5.12.2/junit-jupiter-api-5.12.2.jar','org/junit/jupiter/junit-jupiter-engine/5.12.2/junit-jupiter-engine-5.12.2.jar','org/junit/platform/junit-platform-launcher/1.12.2/junit-platform-launcher-1.12.2.jar','org/junit/platform/junit-platform-engine/1.12.2/junit-platform-engine-1.12.2.jar','org/junit/platform/junit-platform-commons/1.12.2/junit-platform-commons-1.12.2.jar','org/opentest4j/opentest4j/1.3.0/opentest4j-1.3.0.jar','org/apiguardian/apiguardian-api/1.1.2/apiguardian-api-1.1.2.jar')
$g03Jars=foreach($g03Artifact in $g03Artifacts){$g03Jar=Join-Path $g03Deps $g03Artifact;if(-not(Test-Path -LiteralPath $g03Jar)){throw ('Missing '+$g03Jar)};$g03Jar}
$g03Dcc=Join-Path $g03Root 'IntRuoyiBackend/yudao-module-dcc/src'
$g03Sources=@('main/java/cn/iocoder/yudao/module/dcc/service/file/DccApplicationCustomMatrix.java','main/java/cn/iocoder/yudao/module/dcc/service/file/DccApplicationCustomMatrixService.java','main/java/cn/iocoder/yudao/module/dcc/service/file/DccApprovalMatrixTokenService.java','main/java/cn/iocoder/yudao/module/dcc/service/file/DccApprovalMatrixTokenProperties.java','main/java/cn/iocoder/yudao/module/dcc/service/file/DccApprovalMatrixCanonicalizer.java','main/java/cn/iocoder/yudao/module/dcc/service/file/DccDirectSupervisorSource.java','test/java/cn/iocoder/yudao/module/dcc/service/file/DccApplicationCustomMatrixTokenTest.java')|ForEach-Object{Join-Path $g03Dcc $_}
$g03Common=Join-Path $g03Root 'IntRuoyiBackend/yudao-framework/yudao-common/src/main/java/cn/iocoder/yudao/framework/common/util/json'
$g03Sources+=@((Join-Path $g03Common 'JsonUtils.java'),(Join-Path $g03Common 'databind/TimestampLocalDateTimeSerializer.java'),(Join-Path $g03Common 'databind/TimestampLocalDateTimeDeserializer.java'),(Join-Path $g03Task 'TokenJUnitRunner.java'))
$g03Inputs=$g03Sources
$g03Sources+=@(Get-ChildItem -LiteralPath ($g03Run+'/support') -Recurse -File -Filter '*.java'|ForEach-Object{$_.FullName})
$g03Before=@($g03Inputs|ForEach-Object{(Get-FileHash -LiteralPath $_ -Algorithm SHA256).Hash})
[System.IO.File]::WriteAllLines($g03Run+'/sources.txt',@($g03Sources+$g03Jars|ForEach-Object{$_+' '+(Get-FileHash -LiteralPath $_ -Algorithm SHA256).Hash}),[System.Text.UTF8Encoding]::new($false))
$g03Cp=$g03Jars -join [System.IO.Path]::PathSeparator
& javac --release 17 -encoding UTF-8 -classpath $g03Cp -processorpath $g03Jars[0] -d ($g03Run+'/classes') @g03Sources 2>&1|Tee-Object -FilePath ($g03Run+'/compile.txt')
if($LASTEXITCODE -ne 0){throw 'Compilation error is not behavioral RED'}
$g03Capture=if($Phase -eq 'BASELINE'){'true'}else{'false'}
& java '-Dfile.encoding=UTF-8' ('-Dg03.root='+$g03Root) ('-Dg03.task='+$g03Task) ('-Dg03.token.capturePublic='+$g03Capture) -classpath (($g03Run+'/classes')+[System.IO.Path]::PathSeparator+$g03Cp) TokenJUnitRunner 2>&1|Tee-Object -FilePath ($g03Run+'/result.txt')
$g03Exit=$LASTEXITCODE
for($g03Index=0;$g03Index -lt $g03Inputs.Length;$g03Index++){if((Get-FileHash -LiteralPath $g03Inputs[$g03Index] -Algorithm SHA256).Hash -ne $g03Before[$g03Index]){throw 'Actual token source drift'}}
[System.IO.File]::WriteAllText($g03Run+'/exit.txt',$g03Exit.ToString(),[System.Text.UTF8Encoding]::new($false))
Write-Output ('G03 token '+$Phase+' compile=0 JUnit='+$g03Exit+' SOURCE_STABLE; actual JsonUtils/key/mac/token/current source, isolated unused legacy DTOs; not full reactor')
exit $g03Exit
