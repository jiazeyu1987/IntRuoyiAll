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

const buildFormTemplateVersionPrefillData = sliceBetween(
  cellLinkServiceImpl,
  'public Map<String, Object> buildFormTemplateVersionPrefillData(',
  'private String resolveFormTemplateTargetFormDataKey',
  'buildFormTemplateVersionPrefillData'
)
assert.match(
  buildFormTemplateVersionPrefillData,
  /String targetFormDataKey = resolveFormTemplateTargetFormDataKey\(/,
  '动态表单自动预填必须把目标单元格坐标解析为 FormCenter formData 字段 key。'
)
assert.match(
  buildFormTemplateVersionPrefillData,
  /result\.put\(targetFormDataKey, sourceValue\)/,
  '动态表单自动预填必须写入解析后的字段 key，而不是坐标 key。'
)
assert.doesNotMatch(
  buildFormTemplateVersionPrefillData,
  /result\.put\(targetCellKey, sourceValue\)/,
  '动态表单自动预填不得把 5:3 这类坐标 key 写入 FormCenter formData。'
)

const resolveFormTemplateTargetFormDataKey = sliceBetween(
  cellLinkServiceImpl,
  'private String resolveFormTemplateTargetFormDataKey(',
  'private Object resolveFormTemplateWorkOrderFieldValue',
  'resolveFormTemplateTargetFormDataKey'
)
assert.match(
  resolveFormTemplateTargetFormDataKey,
  /resolveRecognizedFieldCode\(templateVersion, targetRowIndex, targetColumnIndex,/,
  '动态表单字段 key 必须来自模板识别字段 fieldCode。'
)
assert.match(
  resolveFormTemplateTargetFormDataKey,
  /throw exception\(MesProBatchRecordCellLinkErrorCodeConstants\.PRO_BATCH_RECORD_CELL_LINK_CELL_MISSING/,
  '动态表单目标格无法映射字段编码时必须 fail fast，不能写坐标 key 兜底。'
)

console.log('mes-edhr-dynamic-form-cell-link-batch-code-static PASS')
