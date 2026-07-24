const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const dialogPath = path.resolve(process.cwd(), 'src/views/mes/pro/feedback/ImportAttributionDialog.vue')
assert(fs.existsSync(dialogPath), `归属弹窗文件必须存在：${dialogPath}`)

const dialogSource = fs.readFileSync(dialogPath, 'utf8')

for (const fragment of [
  '选择订单工序',
  '已选订单工序',
  'handleCandidateChecked',
  'scheduleOrderProcessId',
  '本次工序完成总量',
  'el-checkbox',
  'allocations',
  'selectedQuantity',
  'reattributeImportRecord'
]) {
  assert(dialogSource.includes(fragment), `归属弹窗必须明确选择工单里的工序：${fragment}`)
}

assert(
  /el-checkbox[\s\S]*:model-value="isCandidateSelected\(scope\.row\)"[\s\S]*handleCandidateChecked\(scope\.row, checked\)/.test(
    dialogSource
  ),
  '候选表每一行必须通过复选框显式选择对应订单工序，并绑定 handleCandidateChecked。'
)

assert(
  /const selectedCandidates = computed\(\(\) =>[\s\S]*selectedProcessIds\.value\.includes\(item\.scheduleOrderProcessId\)/.test(
    dialogSource
  ),
  '弹窗必须继续以 selectedProcessIds 驱动已选订单工序集合。'
)

assert(
  /attributeImportRecord\(\{[\s\S]*allocations:[\s\S]*scheduleOrderProcessId:[\s\S]*row\.targetType === attributionTargetType\.currentOrder \? row\.scheduleOrderProcessId : undefined[\s\S]*feedbackQuantity: quantity/.test(
    dialogSource
  ),
  '确认归属必须继续提交所选工序 scheduleOrderProcessId 和对应分配数量。'
)
assert(
  /selectedQuantity/.test(dialogSource),
  '修改归属模式必须能够根据候选 selectedQuantity 回显旧分配。'
)

console.log('PASS: MES feedback attribution process picker static contract')
