const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const dialogPath = path.resolve(process.cwd(), 'src/views/mes/pro/feedback/ImportAttributionDialog.vue')
const quantityLogicPath = path.resolve(process.cwd(), 'src/views/mes/pro/feedback/importAttributionQuantity.ts')
assert(fs.existsSync(dialogPath), `归属弹窗文件必须存在：${dialogPath}`)
assert(fs.existsSync(quantityLogicPath), `归属数量规则模块必须存在：${quantityLogicPath}`)

const dialogSource = fs.readFileSync(dialogPath, 'utf8')
const quantityLogicSource = fs.readFileSync(quantityLogicPath, 'utf8')

const quantityEditorMatch = dialogSource.match(/<div class="quantity-editor">([\s\S]*?)<\/div>/)
assert(quantityEditorMatch, '归属弹窗必须保留顶部数量展示区。')
assert(!quantityEditorMatch[1].includes('<el-button'), '顶部数量区不得再保留全局全部按钮。')

assert(
  /<el-table-column label="" width="92" align="center">[\s\S]*<el-button[\s\S]*@click="handleFillRowQuantity\(scope\.row\)"[\s\S]*>\s*全部\s*<\/el-button>/.test(
    dialogSource
  ),
  '候选表每行右侧必须存在行内全部按钮，并绑定 handleFillRowQuantity(scope.row)。'
)

assert(
  /const getCandidateFillQuantity = \(row: ProFeedbackImportCandidateVO\) =>[\s\S]*Math\.max\(0, Math\.floor\(resolveDefaultProcessFeedbackQuantity\(row\)\)\)/.test(
    dialogSource
  ),
  '行内全部按钮必须继续复用当前默认分配数量规则。'
)

assert(
  quantityLogicSource.includes('INFINITE_PLANNED_QUANTITY_SENTINEL = 999999'),
  '999999 计划数量必须被定义为无限大哨兵值。'
)

assert(
  quantityLogicSource.includes("INFINITE_PLANNED_QUANTITY_LABEL = '无限大'"),
  '无限大计划数量必须统一显示为“无限大”。'
)

assert(
  /export const resolveCurrentOrderFillQuantity = \(candidate\?: ProFeedbackImportCandidateVO\) =>[\s\S]*Math\.min\(plannedQuantity, remainingQuantity\)/.test(
    quantityLogicSource
  ),
  '当前订单点击全部必须按计划数量与剩余数量取小值。'
)

assert(
  dialogSource.includes('formatCandidatePlannedQuantity(scope.row.plannedQuantity)'),
  '计划数量展示必须经过无限大格式化逻辑。'
)

assert(
  /const handleFillRowQuantity = \(row: ProFeedbackImportCandidateVO\) =>[\s\S]*selectedProcessIds\.value\.push\(row\.scheduleOrderProcessId\)[\s\S]*allocationMap\[candidateKey\(row\)\] = quantity/.test(
    dialogSource
  ),
  '行内全部按钮点击后必须保持勾选联动，并把该行分配数量填满。'
)

assert(
  /:disabled="loading \|\| getCandidateFillQuantity\(scope\.row\) <= 0"/.test(dialogSource),
  '行内全部按钮必须在不可分配时禁用。'
)

console.log('PASS: MES feedback attribution row fill static contract')
