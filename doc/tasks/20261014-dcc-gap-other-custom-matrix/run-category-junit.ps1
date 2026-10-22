param([Parameter(Mandatory=$true)][ValidateSet('RED','GREEN','REGRESSION')][string]$Phase,[ValidatePattern('^[a-zA-Z0-9-]+$')][string]$RunId='1')
$ErrorActionPreference='Stop'
$g03Task=[System.IO.Path]::GetFullPath($PSScriptRoot)
$g03Repo=[System.IO.Path]::GetFullPath((Join-Path $g03Task '../../..'))
$g03Run=Join-Path $g03Task ('category-junit/'+$Phase.ToLowerInvariant()+'-'+$RunId)
if(Test-Path -LiteralPath $g03Run){throw 'Do not reuse old category classes'}
New-Item -ItemType Directory -Path ($g03Run+'/classes') -Force | Out-Null
& python -X utf8 (Join-Path $g03Task 'category-test-support.py') ($g03Run+'/support')
if($LASTEXITCODE -ne 0){throw 'Support generation failed'}
$g03RepoDeps=Join-Path ([Environment]::GetFolderPath('UserProfile')) '.m2/repository'
$g03Artifacts=@(
'org/projectlombok/lombok/1.18.46/lombok-1.18.46.jar','cn/hutool/hutool-all/5.8.44/hutool-all-5.8.44.jar',
'com/baomidou/mybatis-plus-annotation/3.5.16/mybatis-plus-annotation-3.5.16.jar',
'jakarta/annotation/jakarta.annotation-api/2.1.1/jakarta.annotation-api-2.1.1.jar','jakarta/validation/jakarta.validation-api/3.0.2/jakarta.validation-api-3.0.2.jar',
'io/swagger/core/v3/swagger-annotations/2.2.25/swagger-annotations-2.2.25.jar',
'org/springframework/spring-context/6.2.9/spring-context-6.2.9.jar','org/springframework/spring-core/6.2.5/spring-core-6.2.5.jar','org/springframework/spring-tx/6.2.9/spring-tx-6.2.9.jar',
'com/h2database/h2/2.3.232/h2-2.3.232.jar',
'com/fasterxml/jackson/core/jackson-annotations/2.17.2/jackson-annotations-2.17.2.jar',
'com/fasterxml/jackson/core/jackson-databind/2.17.2/jackson-databind-2.17.2.jar','com/fasterxml/jackson/core/jackson-core/2.17.2/jackson-core-2.17.2.jar',
'org/springframework/spring-web/6.2.9/spring-web-6.2.9.jar','org/springframework/security/spring-security-core/6.5.10/spring-security-core-6.5.10.jar',
'org/junit/jupiter/junit-jupiter-api/5.12.2/junit-jupiter-api-5.12.2.jar','org/junit/jupiter/junit-jupiter-engine/5.12.2/junit-jupiter-engine-5.12.2.jar',
'org/junit/platform/junit-platform-launcher/1.12.2/junit-platform-launcher-1.12.2.jar','org/junit/platform/junit-platform-engine/1.12.2/junit-platform-engine-1.12.2.jar','org/junit/platform/junit-platform-commons/1.12.2/junit-platform-commons-1.12.2.jar',
'org/opentest4j/opentest4j/1.3.0/opentest4j-1.3.0.jar','org/apiguardian/apiguardian-api/1.1.2/apiguardian-api-1.1.2.jar')
$g03Jars=foreach($g03Artifact in $g03Artifacts){$g03Jar=Join-Path $g03RepoDeps $g03Artifact;if(-not(Test-Path -LiteralPath $g03Jar)){throw ('Missing dependency '+$g03Jar)};$g03Jar}
$g03SourceBase=Join-Path $g03Repo 'IntRuoyiBackend/yudao-module-dcc/src'
$g03Sources=@(
'main/java/cn/iocoder/yudao/module/dcc/dal/dataobject/category/DccFileCategoryDO.java',
'main/java/cn/iocoder/yudao/module/dcc/dal/dataobject/category/DccCategoryDirectoryBindingDO.java',
'main/java/cn/iocoder/yudao/module/dcc/controller/admin/category/vo/DccFileCategorySaveReqVO.java',
'main/java/cn/iocoder/yudao/module/dcc/controller/admin/category/vo/DccFileCategoryRespVO.java',
'main/java/cn/iocoder/yudao/module/dcc/controller/admin/category/vo/DccCategoryDirectoryBindingSaveReqVO.java',
'main/java/cn/iocoder/yudao/module/dcc/service/category/DccFileCategoryAdminService.java',
'main/java/cn/iocoder/yudao/module/dcc/service/category/DccFileCategoryAdminServiceImpl.java',
'main/java/cn/iocoder/yudao/module/dcc/service/category/DccFileCategoryImportResult.java',
'main/java/cn/iocoder/yudao/module/dcc/service/category/DccFileCategoryAdminErrors.java',
'main/java/cn/iocoder/yudao/module/dcc/service/category/DccFileTypeTaxonomyPath.java',
'main/java/cn/iocoder/yudao/module/dcc/service/category/DccFileTypeTaxonomyAdminService.java',
'main/java/cn/iocoder/yudao/module/dcc/service/category/DccFileTypeTaxonomyAdminServiceImpl.java',
'main/java/cn/iocoder/yudao/module/dcc/service/category/DccFileTypeTaxonomyErrors.java',
'main/java/cn/iocoder/yudao/module/dcc/controller/admin/category/vo/DccFileTypeTaxonomyRespVO.java',
'main/java/cn/iocoder/yudao/module/dcc/controller/admin/category/vo/DccFileTypeTaxonomySaveReqVO.java',
'main/java/cn/iocoder/yudao/module/dcc/controller/admin/category/DccFileTypeTaxonomyController.java',
'main/java/cn/iocoder/yudao/module/dcc/dal/dataobject/category/DccFileTypeTaxonomyDO.java',
'main/java/cn/iocoder/yudao/module/dcc/enums/DccFileCategoryLifecycleStageEnum.java',
'test/java/cn/iocoder/yudao/module/dcc/service/category/DccCategoryApprovalSourceModeTest.java',
'test/java/cn/iocoder/yudao/module/dcc/service/category/DccCategoryApprovalSourceProjectionTest.java')|ForEach-Object{Join-Path $g03SourceBase $_}
$g03InputSources=$g03Sources
$g03Sources+= (Join-Path $g03Task 'CategoryJUnitRunner.java')
$g03Sources+= @(Get-ChildItem -LiteralPath ($g03Run+'/support') -Recurse -File -Filter '*.java'|ForEach-Object{$_.FullName})
$g03Before=@($g03InputSources|ForEach-Object{(Get-FileHash -LiteralPath $_ -Algorithm SHA256).Hash})
[System.IO.File]::WriteAllLines($g03Run+'/sources.txt',@($g03Sources|ForEach-Object{$_+' '+(Get-FileHash -LiteralPath $_ -Algorithm SHA256).Hash}),[System.Text.UTF8Encoding]::new($false))
$g03Classpath=$g03Jars -join [System.IO.Path]::PathSeparator
& javac --release 17 -encoding UTF-8 -classpath $g03Classpath -processorpath $g03Jars[0] -d ($g03Run+'/classes') @g03Sources 2>&1|Tee-Object -FilePath ($g03Run+'/compile.txt')
if($LASTEXITCODE -ne 0){throw 'Compile failure is not behavioral RED'}
& java '-Dfile.encoding=UTF-8' -classpath (($g03Run+'/classes')+[System.IO.Path]::PathSeparator+$g03Classpath) CategoryJUnitRunner 2>&1|Tee-Object -FilePath ($g03Run+'/result.txt')
$g03Exit=$LASTEXITCODE
for($g03Index=0;$g03Index -lt $g03InputSources.Length;$g03Index++){if((Get-FileHash -LiteralPath $g03InputSources[$g03Index] -Algorithm SHA256).Hash -ne $g03Before[$g03Index]){throw 'Current category source drift'}}
[System.IO.File]::WriteAllText($g03Run+'/exit.txt',$g03Exit.ToString(),[System.Text.UTF8Encoding]::new($false))
Write-Output ('G03 category '+$Phase+' compile=0 JUnit='+$g03Exit+' SOURCE_STABLE; real admin/DO/VO + explicit H2/isolated ports, not reactor')
exit $g03Exit
