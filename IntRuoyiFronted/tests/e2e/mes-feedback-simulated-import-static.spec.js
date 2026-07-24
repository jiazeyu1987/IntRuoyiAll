const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const pagePath = path.resolve(process.cwd(), 'src/views/mes/pro/feedback/index.vue')
const dialogPath = path.resolve(process.cwd(), 'src/views/mes/pro/feedback/ImportAttributionDialog.vue')
const apiPath = path.resolve(process.cwd(), 'src/api/mes/pro/feedback/index.ts')

for (const filePath of [pagePath, dialogPath, apiPath]) {
  assert(fs.existsSync(filePath), `生产报工模拟导入相关文件必须存在：${filePath}`)
}

const pageSource = fs.readFileSync(pagePath, 'utf8')
const dialogSource = fs.readFileSync(dialogPath, 'utf8')
const apiSource = fs.readFileSync(apiPath, 'utf8')

for (const fragment of [
  '模拟报工',
  'simulateImportLoading',
  'handleSimulateImport',
  'promptSimulatedProcessCount',
  'SIMULATED_PROCESS_COUNT_MIN',
  'SIMULATED_PROCESS_COUNT_MAX',
  'ProFeedbackApi.simulateThirdPartyXlsxImport(processCount)',
  'handleImportSuccess'
]) {
  assert(pageSource.includes(fragment), `报工页必须包含模拟报工入口片段：${fragment}`)
}

for (const fragment of [
  'simulateThirdPartyXlsxImport',
  '/mes/pro/feedback/simulate-import-third-party-xlsx',
  'processCount: number',
  'feedbackQuantity: number',
  'allocations?: ProFeedbackImportAttributeAllocationReqVO[]'
]) {
  assert(apiSource.includes(fragment), `报工 API 必须包含模拟导入和归属数量合同：${fragment}`)
}

for (const fragment of [
  '导入行工序数量',
  '本次工序完成总量',
  '全部',
  '剩余待分配',
  'EXTERNAL_OTHER_ORDER',
  'CURRENT_ORDER',
  '超出',
  '分配',
  'el-checkbox',
  'el-input-number',
  'feedbackQuantity',
  'selectedProcessIds',
  'allocationMap',
  'allocatedFeedbackQuantity',
  'remainingFeedbackQuantity',
  'importedFeedbackQuantity',
  'currentRecord.value?.feedbackQuantity',
  'plannedQuantity',
  'reportedQuantity',
  'remainingQuantity',
  'resolveDefaultProcessFeedbackQuantity',
  'handleFillRowQuantity',
  'handleCandidateChecked',
  'handleAllocationChange',
  'resolveOverproduceQuantity',
  'selectedCandidates'
]) {
  assert(dialogSource.includes(fragment), `归属弹窗必须包含订单工序完成数量片段：${fragment}`)
}

assert(
  dialogSource.includes(':precision="0"'),
  '归属弹窗本次工序完成数量必须按整数显示。'
)
assert(
  dialogSource.includes('return resolveCurrentOrderFillQuantity(candidate)'),
  '当前订单默认数量和行内全部按钮必须统一复用当前订单取值规则。'
)
assert(
  /candidate\?\.externalOtherOrder[\s\S]*return importedQuantity > 0 \? importedQuantity : 0/.test(dialogSource),
  '其他订单默认数量和行内全部按钮必须使用导入行报工数量。'
)
assert(
  dialogSource.includes('formatCandidatePlannedQuantity(scope.row.plannedQuantity)'),
  '候选订单计划数量必须支持 999999 显示为无限大。'
)

const quantityEditorMatch = dialogSource.match(/<div class="quantity-editor">([\s\S]*?)<\/div>/)
assert(quantityEditorMatch, '归属弹窗必须保留顶部数量展示区。')
assert(!quantityEditorMatch[1].includes('<el-button'), '顶部数量区不得再保留全局全部按钮。')
assert(
  /<el-table-column label="" width="92" align="center">[\s\S]*<el-button[\s\S]*@click="handleFillRowQuantity\(scope\.row\)"[\s\S]*>\s*全部\s*<\/el-button>/.test(
    dialogSource
  ),
  '候选表每行右侧必须提供行内全部按钮，并绑定 handleFillRowQuantity(scope.row)。'
)
assert(
  /:disabled="loading \|\| getCandidateFillQuantity\(scope\.row\) <= 0"/.test(dialogSource),
  '行内全部按钮必须在不可分配时禁用。'
)

assert(
  /attributeImportRecord\(\{[\s\S]*allocations:[\s\S]*targetType[\s\S]*feedbackQuantity/.test(dialogSource),
  '确认归属请求必须携带 allocations 多订单分配明细。'
)
assert(
  dialogSource.includes('多个订单分配数量之和不能大于导入行工序数量与缓存池数量之和'),
  '多订单归属提交前必须校验分配总量不能大于导入行工序数量与缓存池数量之和。'
)

for (const hiddenFragment of [
  '<el-alert',
  'label="排产订单"',
  'label="归属目标"'
]) {
  assert(!dialogSource.includes(hiddenFragment), `归属弹窗不得显示红框中的冗余信息：${hiddenFragment}`)
}

assert(!pageSource.includes('catch {}'), '模拟报工入口不得用空 catch 吞掉后端错误。')
assert(!dialogSource.includes('catch {}'), '归属弹窗不得用空 catch 吞掉后端错误。')

console.log('PASS: MES feedback simulated import static contract')
