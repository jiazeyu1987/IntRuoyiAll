const fs=require('node:fs'),path=require('node:path'),cp=require('node:child_process'),crypto=require('node:crypto')
const root=path.resolve(__dirname,'../../..'),run=process.argv[2]
if(!/^[a-z0-9-]+$/.test(run||''))throw Error('Explicit fresh run id required')
const out=path.join(__dirname,'workflow-junit',run);if(fs.existsSync(out))throw Error('Do not reuse workflow classes');fs.mkdirSync(out,{recursive:true})
const base='IntRuoyiBackend/yudao-module-dcc/src/',main=base+'main/java/cn/iocoder/yudao/module/dcc/',test=base+'test/java/cn/iocoder/yudao/module/dcc/'
const before=JSON.parse(fs.readFileSync(path.join(__dirname,'a23-main-before.json'),'utf8'))
const sources=before.map(x=>x.path).filter(x=>x.endsWith('.java')&&!x.includes('service/file/DccApplicationEffectiveSelection')&&!x.includes('DccWorkflowDatePolicy')&&!x.includes('DccWorkflowDefinitionCatalog'))
for(const file of ['service/file/DccApplicationEffectiveSelection.java','service/file/DccWorkflowDatePolicy.java','service/file/DccWorkflowDefinitionCatalog.java','service/file/DccApprovalMatrixTokenService.java','service/file/DccApprovalMatrixTokenProperties.java','service/file/DccApplicationCustomMatrix.java','service/file/DccApplicationCustomMatrixService.java','service/file/DccApprovalMatrixCanonicalizer.java','service/file/DccWorkingApplicationDraftInitializer.java','service/file/DccApplicationRoundService.java','service/projectcode/attributes/DccProjectApplicationSnapshotService.java','service/projectcode/attributes/DccProjectAttributesService.java','dal/dataobject/category/DccFileCategoryDO.java','dal/mysql/file/DccControlledFileMapper.java','dal/mysql/category/DccFileCategoryMapper.java','service/file/DccLifecycleEffectiveContract.java','service/file/DccLifecycleEffectiveContractResolver.java'])sources.push(main+file)
sources.push(test+'service/file/DccPreparedExecutionInheritanceDatabaseTest.java',test+'service/file/DccApplicationCustomMatrixWorkflowTest.java','doc/tasks/20261014-dcc-gap-other-custom-matrix/WorkflowJUnit.java')
for(const file of ['service/file/DccDelayedEffectiveConfirmationEvidence.java','enums/DccControlledFileStageCodeEnum.java','enums/DccControlledFileStatusEnum.java','dal/dataobject/file/DccControlledFileSignatureDO.java'])sources.push(main+file)
for(const file of ['service/route/DccActionApprovalRoutePolicy.java','service/route/DccFixedApprovalRoutePolicy.java','service/route/DccPublicMatrixNativeDerivation.java','service/route/DccApprovalMatrixCapabilityException.java'])sources.push(main+file)
sources.push(main+'service/file/DccSourceUploadSession.java')
if(run.startsWith('rf11'))for(const file of ['service/file/DccBoundExecutionGraphContractTest.java','service/file/DccRoundFlowContractTest.java','service/file/DccPublicRoundNativeSourceGuardTest.java'])sources.push(test+file)
if(run.startsWith('rework')){
 for(const file of ['service/file/DccWorkflowSelectedIterationDatabaseTest.java','service/file/DccCustomRejectedCheckinResubmitTest.java'])sources.push(test+file)
 for(const file of ['service/file/DccControlledFileRevisionServiceImpl.java','service/file/DccControlledFileQueryServiceImpl.java'])sources.push(main+file)
}
for(const file of ['service/file/DccControlledFileAttachmentService.java','service/file/DccControlledFileAttachmentServiceImpl.java','dal/mysql/file/DccControlledFileAttachmentMapper.java','dal/dataobject/file/DccControlledFileAttachmentDO.java'])sources.push(main+file)
const unique=[...new Set(sources)],hash=file=>crypto.createHash('sha256').update(fs.readFileSync(file)).digest('hex')
const manifest=unique.map(file=>({path:file,sha256:hash(path.join(root,file))}));fs.writeFileSync(path.join(out,'sources.json'),JSON.stringify(manifest,null,2))
let classpath=fs.readFileSync(path.join(root,'doc/tasks/20261014-dcc-gap-delayed-effective/private-classpath.txt'),'utf8').trim()
// Current accepted target jars supply external framework and unrelated module dependencies only.
// All tested DCC main contracts and methods listed above are freshly compiled first; final -am is Root-owned.
classpath+=';C:/Users/D01020/.m2/repository/org/junit/platform/junit-platform-launcher/1.12.2/junit-platform-launcher-1.12.2.jar'
const q=s=>'"'+s.replaceAll('\\','/').replaceAll('"','\\"')+'"'
const args=['--release','17','-encoding','UTF-8','-cp',classpath,'-d',out,...unique.map(f=>path.join(root,f))]
fs.writeFileSync(path.join(out,'compile.args'),args.map(q).join('\n'))
let result=cp.spawnSync('javac',['-J-Duser.language=en','-J-Duser.country=US','-J-Dfile.encoding=UTF-8','@'+path.join(out,'compile.args')],{cwd:root,encoding:'utf8',maxBuffer:1024*1024*6});fs.writeFileSync(path.join(out,'compile.txt'),(result.stdout||'')+(result.stderr||''));if(result.status!==0){process.stdout.write(result.stderr||'');throw Error('Compilation failure, not behavioral RED')}
const resource=path.join(out,'sql');fs.mkdirSync(resource,{recursive:true})
const sourceSchema=fs.readFileSync(path.join(root,base+'test/resources/sql/create_tables.sql'),'utf8')
// Exact current shared schema. The test itself owns any explicit production-DDL fixture used in both Maven/private runs.
fs.writeFileSync(path.join(resource,'create_tables.sql'),sourceSchema)
fs.copyFileSync(path.join(root,base+'test/resources/sql/clean.sql'),path.join(resource,'clean.sql'))
fs.copyFileSync(path.join(root,base+'test/resources/application-unit-test.yaml'),path.join(out,'application-unit-test.yaml'))
fs.writeFileSync(path.join(out,'run.args'),['-Dfile.encoding=UTF-8','-Duser.language=en','-Duser.country=US','-Dg03.root='+root,'-cp',out+';'+classpath,'WorkflowJUnit',...(run.startsWith('rf11')?['cn.iocoder.yudao.module.dcc.service.file.DccPublicRoundNativeSourceGuardTest']:run.startsWith('rework')?['cn.iocoder.yudao.module.dcc.service.file.DccCustomRejectedCheckinResubmitTest']:[])].map(q).join('\n'))
result=cp.spawnSync('java',['@'+path.join(out,'run.args')],{cwd:run.startsWith('rework')?path.join(root,'IntRuoyiBackend/yudao-module-dcc'):root,encoding:'utf8',maxBuffer:1024*1024*12});fs.writeFileSync(path.join(out,'result.txt'),(result.stdout||'')+(result.stderr||''));fs.writeFileSync(path.join(out,'exit.txt'),String(result.status));for(const file of manifest)if(hash(path.join(root,file.path))!==file.sha256)throw Error('Source drift '+file.path)
process.stdout.write(((result.stdout||'')+(result.stderr||'')).slice(-15500));process.stdout.write('\nACTUAL_WORKFLOW_JUNIT_EXIT='+result.status+' SOURCE_STABLE\n');process.exit(result.status||0)
