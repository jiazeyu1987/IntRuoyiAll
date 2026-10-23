const fs = require('node:fs')
const path = require('node:path')
const crypto = require('node:crypto')
const assert = require('node:assert/strict')

const task = __dirname
const root = path.resolve(task, '../../..')
const fe = 'IntRuoyiFronted/src/'
const be = 'IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/'
const expected = [
  ['AGENTS.md', 'int_qms'],
  ['docs/task-closeout-rules.md', 'Current Status'],
  ['docs/frontend-development.md', 'RED -> GREEN -> REGRESSION'],
  ['docs/backend-development.md', 'RED -> GREEN -> REGRESSION'],
  ['doc/tasks/20261014-dcc-gap-fix-coordination/requirements.md', '其他'],
  ['doc/tasks/20261014-dcc-gap-fix-coordination/coordination.md', '### G03'],
  [fe + 'views/dcc/controlled-file/upload/index.vue', 'uploadFileTypeOptions'],
  [fe + 'views/dcc/controlled-file/upload/submitter.ts', 'buildUploadPayload'],
  [fe + 'views/dcc/controlled-file/upload/approval-matrix.ts', 'requireApprovalMatrixSelection'],
  [fe + 'views/dcc/controlled-file/upload/signoff-departments.ts', 'resolvedUploadApprovers'],
  [fe + 'views/dcc/controlled-file/upload/file-type-options.ts', 'buildUploadFileTypePaths'],
  [fe + 'views/dcc/controlled-file/detail/DetailApplicationPanel.vue', 'selectIteration'],
  [fe + 'api/dcc/controlledFile/fileTypeTaxonomies.ts', 'DccFileTypeTaxonomyUploadOption'],
  [fe + 'api/dcc/controlledFile/workflow.ts', 'ControlledFileSubmitIterationReqVO'],
  [fe + 'api/dcc/controlledFile/applicationRead.ts', 'WorkingApplicationSelection'],
  [be + 'service/file/DccControlledFileWorkflowServiceImpl.java', 'readNativeReworkRoute'],
  [be + 'service/file/DccControlledFileApprovalRouteAssigneeResolver.java', 'restoreFrozenRoute'],
  [be + 'service/file/DccApprovalMatrixTokenService.java', 'requireRecordedNativeSupervisor'],
  [be + 'service/file/DccApprovalMatrixSourceService.java', 'readCanonical'],
  [be + 'service/file/DccWorkingApplicationAttributes.java', 'draftApprovalMatrix'],
  [be + 'service/file/DccControlledFileRouteReadinessService.java', 'evaluateFrozenRoute'],
  [be + 'service/file/DccWorkingApplicationDraftInitializer.java', 'ApplicationProvenance'],
  [be + 'service/route/DccPublicMatrixNativeDerivation.java', 'project('],
  [be + 'dal/dataobject/file/DccControlledFileDO.java', 'draftApprovalMatrixJson'],
  [be + 'dal/dataobject/file/DccControlledFileRouteSnapshotDO.java', 'sourceApprovalMatrixJson'],
  [be + 'controller/admin/file/DccControlledFileController.java', 'working-attributes'],
  [be + 'controller/admin/file/vo/DccWorkingApplicationSaveReqVO.java', 'selectedSignoffDepartmentIds'],
  [be + 'controller/admin/file/vo/DccControlledFileSubmitReqVO.java', 'expectedApprovalMatrixDigest'],
  [be + 'controller/admin/file/vo/DccControlledFileSubmitIterationReqVO.java', 'expectedApprovalMatrixDigest'],
  [be + 'controller/admin/file/vo/DccControlledFileRoutePreviewReqVO.java', 'selectedSignoffDepartmentIds'],
  [be + 'controller/admin/file/vo/DccControlledFileRouteReadinessRespVO.java', 'supervisorSourceDigest'],
  [be + 'dal/dataobject/category/DccFileCategoryDO.java', 'class DccFileCategoryDO'],
  [be + 'controller/admin/category/vo/DccFileTypeTaxonomyRespVO.java', 'class DccFileTypeTaxonomyRespVO'],
  [be + 'service/category/DccFileTypeTaxonomyAdminServiceImpl.java', 'validateCategoryMapping'],
  ['IntRuoyiBackend/yudao-module-dcc/src/test/resources/sql/create_tables.sql', 'draft_approval_matrix_json'],
  ['IntRuoyiBackend/sql/mysql/20260526_dcc_other_template_category.sql', 'DCC_OTHER_TEMPLATE_']
]
const tests = [
  'IntRuoyiFronted/tests/unit/dcc-upload-signoff-departments.test.cjs',
  'IntRuoyiFronted/tests/unit/dcc-upload-ic13-matrix.test.cjs',
  'IntRuoyiFronted/tests/unit/dcc-approval-matrix-shared-contract.test.cjs',
  'IntRuoyiFronted/tests/unit/dcc-upload-free-file-type.test.cjs',
  'IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccPublicMatrixNativeDerivationTest.java',
  'IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccNativeUploadSupervisorWorkflowTest.java',
  'IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccWorkflowAttributesIntegrationTest.java',
  'IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccWorkflowRevisionIntegrationTest.java'
]
const read = relative => fs.readFileSync(path.join(root, relative), 'utf8')
const digest = text => crypto.createHash('sha256').update(text.replace(/\r\n?/g, '\n'), 'utf8').digest('hex')
for (const [relative, anchor] of expected) {
  assert.ok(read(relative).includes(anchor), 'Missing existing anchor: ' + relative + ' -> ' + anchor)
}
for (const relative of tests) assert.ok(read(relative).length > 0, 'Missing adjacent test: ' + relative)

if (process.argv.includes('--record-fingerprints')) {
  const lines = [
    '# G03 本轮只读源码指纹', '',
    '基线HEAD：822c31fc0856516cf59ba9cd84bf2bc7f455d17f；实际分支int_qms。', '',
    '本表为SPEC_ONLY观察/相邻测试后的指纹，UTF-8文本统一LF后SHA-256。共享文件有其他owner并行修改，转交后必须重读与重算；不能用本表宣称运行代码版本一致。', '',
    '创建自有任务目录前确认目录不存在，新增记录无改前内容。以下仅被读取，没有G03生产写入。', '',
    '首次原始字节指纹（规范化表与原始字节hash不可直接比较）：', '',
    '- upload/index.vue：F0A8BE6C29DAFD096B97D2179FEA1EAD37F513DD5FBDB546F33E19C29845B652',
    '- upload/submitter.ts：5D80D0061B6A8FBC606872EBC4DB406421F7A78AA838B32261AD878C882D6DE6',
    '- DetailApplicationPanel.vue：06C85044FEB5C0FA726B72D2CD64600AA3939873D4524EB7F395F6654528B73D',
    '- DccControlledFileWorkflowServiceImpl.java：A61B899A3324B9E72C428A711AE96FE40C9E0053445F2038EA112929CCF28060',
    '- DccPublicMatrixNativeDerivation.java：980DA03F93A8FC938BE74FFFEC2B05950F0647515297E66D4F54617AAB54623E', '',
    '首次定位错误已修正：正式workflow.ts位于src/api/dcc/controlledFile/，不是src/api/dcc/；ReadinessService是具体类，没有ReadinessServiceImpl。未建兼容路径。', '',
    '|仓库相对路径|已复核锚点|SHA-256（UTF-8/LF）|', '|---|---|---|'
  ]
  for (const [relative, anchor] of expected) lines.push(`|${relative}|${anchor}|${digest(read(relative))}|`)
  lines.push('', '## 相邻测试源码指纹', '', '|仓库相对路径|本轮证据|SHA-256（UTF-8/LF）|', '|---|---|---|')
  for (const relative of tests) lines.push(`|${relative}|${relative.endsWith('.cjs') ? '实际运行' : '仅阅读，未运行'}|${digest(read(relative))}|`)
  fs.writeFileSync(path.join(task, 'source-fingerprints.md'), lines.join('\n') + '\n', 'utf8')
}

const docs = ['task.md', 'execution-log.md', 'integration-notes.md', 'review-ready.md', 'changed-files.md', 'verification-report.md', 'source-fingerprints.md']
for (const file of docs) {
  const text = fs.readFileSync(path.join(task, file), 'utf8')
  assert.ok(text.startsWith('# '), file + ': missing title')
  assert.ok(!text.includes('\ufffd'), file + ': invalid encoding')
}
const taskText = fs.readFileSync(path.join(task, 'task.md'), 'utf8')
for (const section of ['Goal', 'Authorization', 'Milestones', 'Expected Verification', 'BDD', '设计约束检查', 'Current Status', 'Cleanup Keep']) {
  assert.ok(taskText.includes('## ' + section), 'Missing task section: ' + section)
}
assert.match(taskText, /## Current Status\s+in_progress\b/)
const originalBdd = taskText.slice(taskText.indexOf('## BDD'), taskText.indexOf('## 设计约束检查'))
assert.equal((originalBdd.match(/^- Given /gm) || []).length, 6)
assert.equal((taskText.match(/^- Given /gm) || []).length, 31)
const integration = fs.readFileSync(path.join(task, 'integration-notes.md'), 'utf8')
for (const section of ['现状', '端到端最小合同', '真实 RED / GREEN / REGRESSION 方案', '精确文件转交请求', '依赖与待冻结决定']) assert.ok(integration.includes(section), 'Missing integration section: ' + section)
for (const match of integration.matchAll(/^\|(IntRuoyi[^|]+)\|/gm)) {
  assert.ok(fs.existsSync(path.join(root, match[1])), 'Missing integration request file: ' + match[1])
}
for (const keep of ['changed-files.md', 'integration-notes.md', 'review-ready.md', 'source-fingerprints.md', 'validate-documents.cjs', 'backend-minimal-signatures.md']) {
  assert.ok(taskText.includes('- doc/tasks/20261014-dcc-gap-other-custom-matrix/' + keep), 'Missing Cleanup Keep: ' + keep)
}
console.log(`PASS: G03 records; ${expected.length} existing anchors; ${tests.length} adjacent test sources; 31 original/C01/A08/A10/R03/preview/A20 Given/When/Then scenarios. Documentation validation only.`)
