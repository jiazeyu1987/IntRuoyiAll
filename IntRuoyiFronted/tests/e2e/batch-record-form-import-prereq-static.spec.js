const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '..', '..')
const pageSource = fs.readFileSync(
  path.join(repoRoot, 'src/views/mes/pro/batchrecordformlist/index.vue'),
  'utf8'
)

assert.match(
  pageSource,
  /@click="openWordImportDialog"\s*>[\s\S]*?导入[\s\S]*?<\/el-button>/,
  '批记录表单页点击导入必须先打开导入配置弹窗。'
)

assert.ok(
  !pageSource.includes('@click="handleImportFile"'),
  '批记录表单页导入按钮不得直接触发隐藏文件选择器。'
)

assert.match(
  pageSource,
  /<el-form-item label="表单类型" required>[\s\S]*?v-model="wordImportDialog\.selectedFormSlotType"[\s\S]*?formSlotTypeOptions/,
  '导入弹窗必须先提供必选表单类型。'
)

assert.match(
  pageSource,
  /const formSlotTypeOptions[\s\S]*?MAIN[\s\S]*?LOSS_REPORT[\s\S]*?PROCESS_INSPECTION[\s\S]*?PARAMETER_RECORD/,
  '表单类型必须包含批记录、损耗单、过程检验单和参数记录表。'
)

assert.match(
  pageSource,
  /<el-form-item label="产品名称" required>[\s\S]*?<el-select[\s\S]*?v-model="wordImportDialog\.selectedProjectName"[\s\S]*?:remote-method="loadWordImportProjectOptions"/,
  '产品名称必须复用 DCC 项目名称远程下拉选择。'
)

assert.ok(
  pageSource.includes("@/api/dcc/controlledFile/projectCodes") &&
    pageSource.includes('getProjectCodePage'),
  '产品名称候选必须来自 DCC 项目代码接口。'
)

assert.match(
  pageSource,
  /<el-form-item label="Word 文件" required>[\s\S]*?handleWordImportFileSelect[\s\S]*?wordImportDialog\.file/,
  'Word 文件选择必须后置到导入配置弹窗内。'
)

assert.match(
  pageSource,
  /<el-form-item v-if="isMainWordImport" label="导入内容">[\s\S]*?v-model="wordImportDialog\.rebuildBatchRecord"[\s\S]*?批记录表单[\s\S]*?<div class="batch-record-word-import-form__route-title">工艺流程<\/div>[\s\S]*?wordImportDialog\.preflight\.routeProductOptions/,
  '主批记录导入必须提供批记录表单和工艺流程选择。'
)

assert.match(
  pageSource,
  /if \(!wordImportDialog\.selectedFormSlotType\)[\s\S]*?请选择表单类型[\s\S]*?if \(!selectedProjectName\)[\s\S]*?请选择产品名称[\s\S]*?if \(!file\)[\s\S]*?请选择 Word 文件/,
  '确认导入前必须校验表单类型、产品名称和 Word 文件。'
)

assert.match(
  pageSource,
  /isMainWordImport[\s\S]*?BatchRecordReportApi\.recognizeUploadedRoute/,
  '表单类型为 MAIN 时必须继续走主批记录识别导入接口。'
)

assert.match(
  pageSource,
  /BatchRecordReportApi\.uploadExtraFormSlot\(\s*file,\s*selectedProjectName,\s*wordImportDialog\.selectedFormSlotType/,
  '附加表单必须用所选产品名称和表单类型调用 uploadExtraFormSlot。'
)

assert.doesNotMatch(
  pageSource,
  /resolveFormSlotTypeLabel\(wordImportDialog\.selectedFormSlotType\)\}已上传，请先删除后重新上传/,
  '损耗单、过程检验单、参数记录表重复导入必须进入升版流程，不能再要求先删除。'
)

assert.match(
  pageSource,
  /const formSlotLabel = resolveFormSlotTypeLabel\(formSlotType\)[\s\S]*?`确认\$\{formSlotLabel\}升版`[\s\S]*?confirmButtonText:\s*'升版导入'/,
  '损耗单、过程检验单、参数记录表存在历史版本时必须弹出升版确认。'
)

assert.match(
  pageSource,
  /submitImportedVersionApproval\(result,\s*resolveFormSlotTypeLabel\(wordImportDialog\.selectedFormSlotType\)\)/,
  '损耗单、过程检验单、参数记录表导入结果必须复用升版审批状态提示。'
)

console.log('PASS: batch record form import prerequisite static contract')
