const fs = require('fs')
const path = require('path')
const assert = require('assert')

const workspaceRoot = path.resolve(__dirname, '../../../../..')
const read = (relativePath) => fs.readFileSync(path.join(workspaceRoot, relativePath), 'utf8').replace(/\r\n/g, '\n')

function sliceBetween(source, startNeedle, endNeedle, label) {
  const start = source.indexOf(startNeedle)
  assert.ok(start >= 0, `${label} missing start marker`)
  const end = source.indexOf(endNeedle, start + startNeedle.length)
  assert.ok(end > start, `${label} missing end marker`)
  return source.slice(start, end)
}

const cellLinkService = read(
  'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecordcelllink/MesProBatchRecordCellLinkService.java'
)
const cellLinkServiceImpl = read(
  'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecordcelllink/MesProBatchRecordCellLinkServiceImpl.java'
)
const batchExecutionService = read(
  'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrBatchExecutionServiceImpl.java'
)

assert.match(
  cellLinkService,
  /buildFormTemplateVersionPrefillData\(Long templateVersionId,\s*Long workOrderId,\s*String executionBatchCode,\s*Map<String, Object> formData\)/,
  '动态表单预填接口必须显式接收 eDHR 执行上下文批号。'
)

const createFormCenterInstanceForTask = sliceBetween(
  batchExecutionService,
  'private Long createFormCenterInstanceForTask(MesProEdhrBatchExecutionDO batch,',
  'private void autoPersistDynamicRouteFormPrefill',
  'createFormCenterInstanceForTask'
)
assert.match(
  createFormCenterInstanceForTask,
  /buildFormTemplateVersionPrefillData\(\s*task\.getFormTemplateVersionId\(\),\s*batch\.getWorkOrderId\(\),\s*batch\.getBatchCode\(\),\s*baseFormData\)/,
  '创建动态表单实例时必须把当前 eDHR 批号传入预填服务。'
)

const autoPersistDynamicRouteFormPrefill = sliceBetween(
  batchExecutionService,
  'private void autoPersistDynamicRouteFormPrefill(MesProEdhrBatchExecutionDO batch,',
  'private Map<String, Object> parseFormCenterFormData',
  'autoPersistDynamicRouteFormPrefill'
)
assert.match(
  autoPersistDynamicRouteFormPrefill,
  /buildFormTemplateVersionPrefillData\(\s*task\.getFormTemplateVersionId\(\),\s*batch\.getWorkOrderId\(\),\s*batch\.getBatchCode\(\),\s*currentFormData\)/,
  '再次打开动态表单时必须继续用当前 eDHR 批号自动落库草稿。'
)

const resolveFormTemplateWorkOrderFieldValue = sliceBetween(
  cellLinkServiceImpl,
  'private Object resolveFormTemplateWorkOrderFieldValue(',
  'private Scope resolveQueryScope',
  'resolveFormTemplateWorkOrderFieldValue'
)
const batchCodeBranch = sliceBetween(
  resolveFormTemplateWorkOrderFieldValue,
  'if (WORK_ORDER_SOURCE_FIELD_BATCH_CODE.equals(field.code())) {',
  '        }\n        return field.valueExtractor().apply(workOrder);',
  'resolveFormTemplateWorkOrderFieldValue batchCode branch'
)
assert.match(
  batchCodeBranch,
  /return StrUtil\.trim\(executionBatchCode\)/,
  'FORM_TEMPLATE_VERSION 的生产批号必须取 eDHR 执行上下文批号，而不是依赖工单表 batchCode。'
)
assert.doesNotMatch(
  batchCodeBranch,
  /field\.valueExtractor\(\)\.apply\(workOrder\)/,
  'FORM_TEMPLATE_VERSION 的生产批号不得回到 workOrder.batchCode。'
)

console.log('mes-edhr-dynamic-form-cell-link-batch-code-static PASS')
