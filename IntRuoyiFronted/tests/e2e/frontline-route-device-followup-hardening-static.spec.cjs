const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const panel = fs.readFileSync(
  path.join(root, 'src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue'),
  'utf8'
).replace(/\r\n/g, '\n')
const routeDesigner = fs.readFileSync(
  path.join(root, 'src/views/mes/pro/route/RouteFlowGraphDesigner.vue'),
  'utf8'
).replace(/\r\n/g, '\n')

function extractBlock(source, startNeedle, endNeedle) {
  const start = source.indexOf(startNeedle)
  assert.ok(start >= 0, `${startNeedle} must exist.`)
  const end = source.indexOf(endNeedle, start)
  assert.ok(end > start, `${startNeedle} block must close before ${endNeedle}.`)
  return source.slice(start, end)
}

assert.match(
  panel,
  /const pqcTaskInspectionItemMap = computed[\s\S]*pqcTaskInspectionItems\.value[\s\S]*const activePqcTabItem = computed\(\(\) =>[\s\S]*pqcTaskInspectionItemMap\.value/,
  'PQC 当前填写面板必须读取当前任务的冻结检验项，工序级列表只能用于页签目录。'
)
assert.match(
  panel,
  /const getPqcItemSelectionKey = \([\s\S]*taskOption\.pqcTaskId[\s\S]*itemKey/,
  'PQC 设备选择草稿键必须包含 pqcTaskId 和检验项。'
)
assert.match(
  panel,
  /assertPqcItemEquipmentSelection\(item, taskOption\)/,
  '构造每个任务提交载荷时必须读取该任务自己的设备选择。'
)
const pieceDefaultsBlock = extractBlock(
  panel,
  'const ensurePqcDefaultPieceValuesForTask = (',
  '\n}\n\nconst resizePqcPieceValuesForCurrentTask ='
)
assert.match(
  pieceDefaultsBlock,
  /const ensurePqcDefaultPieceValuesForTask = \([\s\S]*taskOption\.inspectionItems\.map\(mapPqcInspectionItem\)[\s\S]*\.find\(\(candidate\) => candidate\.key === itemKey\)/,
  '逐件默认值必须读取目标 PQC 任务自己的冻结检验项。'
)
assert.doesNotMatch(pieceDefaultsBlock, /pqcInspectionItemMap\.value/)

const resultBlock = extractBlock(
  panel,
  'const resolvePqcResultForTask = (',
  '\n}\n\nconst resolvePqcResult ='
)
assert.match(
  resultBlock,
  /const resolvePqcResultForTask = \([\s\S]*for \(const item of taskOption\.inspectionItems\.map\(mapPqcInspectionItem\)\)[\s\S]*item\.type === 'number'/,
  '任务结果判定必须直接使用目标任务检验项的类型和上下限。'
)
assert.doesNotMatch(resultBlock, /pqcInspectionItemMap\.value/)
assert.match(
  panel,
  /const openPqcMethodDialog = \(itemKey[\s\S]*pqcTaskInspectionItemMap\.value\[itemKey\]/,
  '检验方法弹框必须显示当前任务冻结的检验项。'
)

assert.match(
  routeDesigner,
  /let writeSucceeded = false[\s\S]*writeSucceeded = true[\s\S]*设备参数规则已保存，但刷新最新配置失败/,
  '设备参数写成功后刷新失败必须与写入失败分开提示。'
)
assert.match(
  routeDesigner,
  /isRouteProcessDeviceParameterMutationCurrent[\s\S]*await ProRouteFlowConfigApi\.saveRouteProcessDeviceParameterRule[\s\S]*if \(!isRouteProcessDeviceParameterMutationCurrent/,
  '设备参数保存响应写回前必须确认仍是原路线版本和原工序。'
)
assert.match(
  routeDesigner,
  /refreshRouteProcessDeviceParameterStateAfterStaleMutation[\s\S]*refreshProductionProcessConfigSnapshot[\s\S]*loadSelectedRouteProcessDeviceParameterConfig[\s\S]*if \(!isRouteProcessDeviceParameterMutationCurrent[\s\S]*await refreshRouteProcessDeviceParameterStateAfterStaleMutation/,
  '旧工序写成功后必须刷新同版本当前工序，避免继续使用变化前的候选快照哈希。'
)
assert.match(
  routeDesigner,
  /watch\(selectedRouteProcessId[\s\S]*routeProcessDeviceParameterDialogVisible\.value = false[\s\S]*resetSelectedRouteProcessDeviceParameterConfig\(\)/,
  '切换工序时必须关闭旧工序设备参数弹框并使旧请求失效。'
)

assert.match(
  panel,
  /const getDefaultPqcTaskOption[\s\S]*selectedItemOptions[\s\S]*getPqcTaskOptions\(process\)/,
  '当前项目没有待检任务时必须自动选择同工序下一条待检任务。'
)
assert.match(
  panel,
  /const invalidCompletionMaterials = materialDetails\.filter[\s\S]*outputQuantity <= 0/,
  '填写为零的输出物料必须在前端阻止正式报工。'
)

assert.match(
  panel,
  /const buildProductionMaterialDeviceMemoryKey = \([\s\S]*routeVersionId[\s\S]*routeProcessId[\s\S]*processId[\s\S]*materialKey/,
  '生产设备记忆必须按路线版本、路线工序、基础工序和物料隔离。'
)
assert.match(
  panel,
  /localStorage\.getItem\(storageKey\)[\s\S]*localStorage\.setItem\(storageKey/,
  '生产设备记忆必须在同一生产终端跨页面刷新持久化。'
)
assert.match(
  panel,
  /报工已成功，但默认设备和参数记忆保存失败/,
  '正式报工成功后偏好保存失败必须准确分层提示。'
)

console.log('PASS: frontline route device follow-up frontend contract')
