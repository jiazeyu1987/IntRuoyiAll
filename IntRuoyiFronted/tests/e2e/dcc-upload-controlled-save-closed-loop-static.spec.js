const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = process.cwd()

function read(relativePath) {
  const file = path.join(root, relativePath)
  assert.ok(fs.existsSync(file), `Missing expected file: ${relativePath}`)
  return fs.readFileSync(file, 'utf8')
}

const uploadPage = read('src/views/dcc/controlled-file/upload/index.vue')
const workflowApi = read('src/api/dcc/controlledFile/workflow.ts')
const controller = fs.readFileSync(
  path.join(
    root,
    '..',
    'IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/file/DccControlledFileController.java'
  ),
  'utf8'
)
const workflowServiceImpl = fs.readFileSync(
  path.join(
    root,
    '..',
    'IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileWorkflowServiceImpl.java'
  ),
  'utf8'
)
const queryServiceImpl = fs.readFileSync(
  path.join(
    root,
    '..',
    'IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileQueryServiceImpl.java'
  ),
  'utf8'
)

assert.match(
  workflowApi,
  /export const submitControlledFile[\s\S]*url:\s*'\/dcc\/controlled-files\/submit'/,
  'DCC workflow API must expose the create-and-submit endpoint for ordinary uploads'
)

assert.match(
  controller,
  /@PostMapping\("\/submit"\)[\s\S]*workflowService\.submitControlledFile\(getLoginUserId\(\),\s*reqVO\)/,
  'DCC backend controller must expose the create-and-submit endpoint for ordinary uploads'
)

assert.match(
  workflowServiceImpl,
  /@Override\s*@Transactional\(rollbackFor = Exception\.class\)\s*public Long submitControlledFile\(Long userId,\s*DccControlledFileSubmitReqVO reqVO\)/,
  'DCC ordinary create-and-submit service must run in one rollback-capable transaction'
)

assert.match(
  workflowServiceImpl,
  /String processInstanceId = createApprovalProcess\(userId,\s*file,\s*resolvedRoute,\s*processDefinitionKey\);\s*if \(controlledFileMapper\.updateById\(DccControlledFileDO\.builder\(\)\s*\.id\(file\.getId\(\)\)\s*\.processInstanceId\(processInstanceId\)\s*\.build\(\)\) != 1\) \{[\s\S]*?throw exception\(CONTROLLED_FILE_ITERATION_SUBMIT_NOT_ALLOWED\);[\s\S]*?\}/,
  'DCC create-and-submit must fail fast if approval process identity cannot be persisted'
)

assert.match(
  uploadPage,
  /submit:\s*submitControlledFile/,
  'DCC ordinary upload must submit the initial version into approval in the same upload flow'
)

assert.doesNotMatch(
  uploadPage,
  /submit:\s*createWorkingControlledFile/,
  'DCC ordinary upload must not stop at a WORKING-only version'
)

assert.doesNotMatch(
  uploadPage,
  /受控文件已创建，请在文件浏览中提交审批/,
  'DCC ordinary upload must not tell users to submit approval from controlled browsing'
)

assert.doesNotMatch(
  workflowServiceImpl,
  /resolveUnclassifiedUploadDirectory\(directoryMapper\.selectEnabledList\(\)\)/,
  'DCC ordinary upload must fail fast when the formal default directory binding is missing'
)

assert.doesNotMatch(
  queryServiceImpl,
  /defaultUnclassified\s*=\s*binding\s*==\s*null|resolveUnclassifiedUploadDirectory/,
  'DCC upload directory tree must not synthesize a fallback unclassified default directory'
)

assert.doesNotMatch(
  uploadPage,
  /按规则发布到“未分类”|自动提交到未分类目录|defaultUnclassified/,
  'DCC upload page must not present missing default directory configuration as an automatic unclassified landing'
)

console.log('dcc upload controlled-save closed-loop static contract passed')
