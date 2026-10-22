const fs = require('node:fs'), path = require('node:path'), cp = require('node:child_process'), crypto = require('node:crypto')
const root = path.resolve(__dirname, '../../..'), work = path.join(__dirname, 'private-java-current-source')
fs.mkdirSync(work, { recursive: true })
const classpath = fs.readFileSync(path.join(__dirname, 'private-classpath.txt'), 'utf8').trim()
  + ';C:/Users/D01020/.m2/repository/org/junit/platform/junit-platform-launcher/1.12.2/junit-platform-launcher-1.12.2.jar'
const prefix = 'IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/'
let sources = [prefix + 'dal/dataobject/file/DccControlledFileDO.java', prefix + 'dal/dataobject/file/DccControlledFileSignatureDO.java',
  prefix + 'service/file/DccDelayedEffectiveConfirmationEvidence.java', prefix + 'service/file/DccWorkflowDefinitionCatalog.java',
  prefix + 'service/file/DccBoundWorkflowContractResolver.java', prefix + 'service/file/DccLifecycleEffectiveContract.java',
  prefix + 'service/file/DccLifecycleEffectiveContractResolver.java',
  prefix + 'service/file/DccControlledFileSignatureEvidenceCreateReq.java', prefix + 'service/file/DccControlledFileSignatureEvidence.java',
  prefix + 'service/file/DccControlledFileSignatureEvidenceServiceImpl.java',
  prefix + 'service/file/DccControlledFileSignatureEvidenceService.java',
  'IntRuoyiBackend/yudao-module-signature/src/main/java/cn/iocoder/yudao/module/signature/api/ElectronicSignatureSubjectAdapter.java',
  'IntRuoyiBackend/yudao-module-signature/src/main/java/cn/iocoder/yudao/module/signature/service/ElectronicSignatureServiceImpl.java',
  'IntRuoyiBackend/yudao-module-signature/src/main/java/cn/iocoder/yudao/module/signature/service/ElectronicSignatureQueryServiceImpl.java',
  prefix + 'service/file/DccApplicationEffectiveSelection.java', prefix + 'service/file/DccWorkflowDatePolicy.java',
  prefix + 'service/file/DccSignatureVerificationService.java', prefix + 'service/file/DccSignatureVerificationServiceImpl.java',
  prefix + 'service/file/DccElectronicSignatureManagementServiceImpl.java', prefix + 'service/file/DccControlledFileSignatureSubjectAdapter.java',
  prefix + 'service/file/DccElectronicSignatureAuthorizationAuditService.java',
  prefix + 'service/file/DccElectronicSignatureAuthorizationServiceImpl.java',
  prefix + 'dal/dataobject/file/DccElectronicSignatureAuthorizationAuditDO.java',
  prefix + 'service/file/DccDelayedEffectiveConfirmationService.java',
  prefix + 'service/file/DccApplicationExecutionContractService.java', prefix + 'service/file/DccFrozenApprovalSignatures.java',
  prefix + 'service/file/DccControlledFileAttachmentService.java',
  prefix + 'service/file/DccWorkflowFileStateAudit.java',
  prefix + 'service/file/DccControlledFileWorkflowServiceImpl.java', prefix + 'service/file/DccControlledFileRevisionServiceImpl.java',
  prefix + 'service/file/DccControlledFileApprovalRouteAssigneeResolver.java', prefix + 'service/file/DccControlledContentAdapter.java',
  prefix + 'enums/DccControlledFileStageCodeEnum.java', prefix + 'enums/DccControlledFileStatusEnum.java',
  prefix + 'controller/admin/file/DccControlledFileWorkflowController.java', prefix + 'controller/admin/file/vo/DccControlledFileRespVO.java',
  prefix + 'controller/admin/file/vo/DccControlledFileDelayedEffectiveConfirmReqVO.java',
  prefix + 'controller/admin/file/vo/DccControlledFileDelayedEffectiveConfirmRespVO.java',
  'IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccDelayedEffectiveConfirmationEvidenceTest.java',
  'IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/DccWorkflowV6BpmnTest.java',
  'IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccDelayedEffectiveHmacTest.java',
  'IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccDelayedEffectiveConfirmationServiceTest.java',
  'IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccDelayedSignatureRoundtripTest.java',
  'doc/tasks/20261014-dcc-gap-delayed-effective/PrivateJUnit.java'].filter(file => fs.existsSync(path.join(root, file)))
if (!process.argv.includes('--main-checkpoint')) {
  const returned = new Set(JSON.parse(fs.readFileSync(path.join(__dirname, 'main-handback.json'), 'utf8')).files.map(row => row.path))
  sources = sources.filter(file => !returned.has(file))
  process.stdout.write('PRIVATE_SCOPE=OWN_CURRENT_SOURCE_WITH_ROOT_M05_FRESH_RETURNED_MAIN_AND_EXTERNAL_DEPENDENCIES\n')
}
const quote = value => '"' + String(value).replaceAll('\\', '/').replaceAll('"', '\\"') + '"'
const label = process.argv.find(arg => /^--(?:suite|model|roundtrip|hmac|command)$/.test(arg))?.slice(2) || 'evidence'
const fingerprint = () => sources.map(file => ({ path: file, sha256: crypto.createHash('sha256').update(fs.readFileSync(path.join(root,file))).digest('hex') }))
const before = fingerprint(), startedAt = new Date().toISOString()
const compileArgs = path.join(work, 'compile.args'), runArgs = path.join(work, 'run.args')
fs.writeFileSync(compileArgs, ['-encoding', 'UTF-8', '-cp', work + ';' + classpath, '-d', work, ...sources].map(quote).join('\n') + '\n', 'utf8')
const compiled = cp.spawnSync('javac', ['@' + compileArgs], { cwd: root, encoding: 'utf8' })
fs.writeFileSync(path.join(work,label+'-compile.log'), (compiled.stdout || '')+(compiled.stderr || ''), 'utf8')
process.stdout.write(compiled.stdout || ''); process.stderr.write(compiled.stderr || '')
if (compiled.error) throw compiled.error
if (compiled.status !== 0) { process.stdout.write('JAVAC_FAILED_BEFORE_JUNIT\n'); process.exit(compiled.status || 1) }
fs.writeFileSync(runArgs, ['-Ddcc.repository.root='+root, '-cp', work + ';' + classpath, 'PrivateJUnit',
  ...(process.argv.includes('--suite') ? ['cn.iocoder.yudao.module.dcc.service.file.DccDelayedEffectiveConfirmationEvidenceTest',
    'cn.iocoder.yudao.module.dcc.service.file.DccDelayedEffectiveHmacTest','cn.iocoder.yudao.module.dcc.service.file.DccDelayedSignatureRoundtripTest',
    'cn.iocoder.yudao.module.dcc.service.file.DccDelayedEffectiveConfirmationServiceTest','cn.iocoder.yudao.module.dcc.DccWorkflowV6BpmnTest']
    : process.argv.includes('--model') ? ['cn.iocoder.yudao.module.dcc.DccWorkflowV6BpmnTest'] : process.argv.includes('--roundtrip') ? ['cn.iocoder.yudao.module.dcc.service.file.DccDelayedSignatureRoundtripTest'] : process.argv.includes('--hmac') ? ['cn.iocoder.yudao.module.dcc.service.file.DccDelayedEffectiveHmacTest'] : process.argv.includes('--command') ? ['cn.iocoder.yudao.module.dcc.service.file.DccDelayedEffectiveConfirmationServiceTest'] : ['cn.iocoder.yudao.module.dcc.service.file.DccDelayedEffectiveConfirmationEvidenceTest'])].map(quote).join('\n') + '\n', 'utf8')
const tested = cp.spawnSync('java', ['@' + runArgs], { cwd: root, encoding: 'utf8' })
fs.writeFileSync(path.join(work,label+'-junit.log'), (tested.stdout || '')+(tested.stderr || ''), 'utf8')
const after = fingerprint()
fs.writeFileSync(path.join(work,label+'-receipt.json'),JSON.stringify({label,startedAt,endedAt:new Date().toISOString(),compileExit:compiled.status,junitExit:tested.status,
  scope:process.argv.includes('--main-checkpoint')?'EXPLICIT_CURRENT_CHECKPOINT_SOURCES':'OWN_CURRENT_SOURCE_WITH_ROOT_M05_FRESH_RETURNED_MAIN_AND_EXTERNAL_DEPENDENCIES',
  sourceStable:JSON.stringify(before)===JSON.stringify(after),before,after},null,2)+'\n','utf8')
process.stdout.write(tested.stdout || ''); process.stderr.write(tested.stderr || '')
if (tested.error) throw tested.error
process.stdout.write('PRIVATE_JUNIT_EXIT=' + tested.status + '\n')
process.exit(tested.status || 0)
