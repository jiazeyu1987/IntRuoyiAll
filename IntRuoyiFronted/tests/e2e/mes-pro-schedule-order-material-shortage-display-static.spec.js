const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const pagePath = path.resolve(process.cwd(), 'src/views/mes/pro/scheduleorder/index.vue')
const apiPath = path.resolve(process.cwd(), 'src/api/mes/pro/task/autoSchedule/index.ts')

assert(fs.existsSync(pagePath), 'Schedule order page must exist.')
assert(fs.existsSync(apiPath), 'Auto schedule API module must exist.')

const pageSource = fs.readFileSync(pagePath, 'utf8')
const apiSource = fs.readFileSync(apiPath, 'utf8')

for (const field of ['materialCode', 'materialName', 'shortageQty']) {
  assert(apiSource.includes(field), `Auto schedule issue API type must expose ${field}.`)
}

assert(
  pageSource.includes('replanIssueRows'),
  'Replan preview must derive a dedicated three-column issue row list.'
)
assert(
  pageSource.includes("issue.issueType === 'MATERIAL'"),
  'Material shortage issues must be identified by issueType MATERIAL, not by parsing message text.'
)
assert(
  pageSource.includes('materialShortageDialogVisible'),
  'Material shortage details must be hidden behind a dialog by default.'
)
assert(
  pageSource.includes('openMaterialShortageDialog'),
  'Material shortage remark must expose a button that opens the shortage dialog.'
)
assert(pageSource.includes('title="物料缺料明细"'), 'Material shortage dialog title must be explicit.')
assert(
  pageSource.includes(`v-if="row.issueType === 'MATERIAL'"`) ||
    pageSource.includes(`v-if="row.issueType === 'MATERIAL' &&`),
  'Material shortage row remark must render the detail button only for MATERIAL issues.'
)

const replanSummaryStart = pageSource.indexOf('<div v-if="replanPreview" class="schedule-order-pool__replan-summary">')
const replanDrawerEnd = pageSource.indexOf('</el-drawer>', replanSummaryStart)
assert(replanSummaryStart >= 0 && replanDrawerEnd > replanSummaryStart, 'Replan preview summary block must exist.')
const replanSummarySource = pageSource.slice(replanSummaryStart, replanDrawerEnd)

assert(replanSummarySource.includes('label="严重度"'), 'Replan issue table must show severity column.')
assert(replanSummarySource.includes('label="问题"'), 'Replan issue table must show problem column.')
assert(replanSummarySource.includes('label="备注"'), 'Replan issue table must show remark column.')
assert(!replanSummarySource.includes('label="日期"'), 'Replan issue table must not show date column.')
assert(!replanSummarySource.includes('label="班次"'), 'Replan issue table must not show shift column.')
assert(!replanSummarySource.includes('label="操作"'), 'Replan issue table must not show operation column.')
assert(
  !replanSummarySource.includes('label="物料"') &&
    !replanSummarySource.includes('label="编码"') &&
    !replanSummarySource.includes('label="缺少数量"'),
  'Material shortage detail columns must not be visible in the replan preview table.'
)

const materialDialogStart = pageSource.indexOf('title="物料缺料明细"')
const materialDialogEnd = pageSource.indexOf('</Dialog>', materialDialogStart)
assert(materialDialogStart >= 0 && materialDialogEnd > materialDialogStart, 'Material shortage dialog must exist.')
const materialDialogSource = pageSource.slice(materialDialogStart, materialDialogEnd)
assert(materialDialogSource.includes('label="物料"'), 'Material shortage dialog must show material name column.')
assert(materialDialogSource.includes('label="编码"'), 'Material shortage dialog must show material code column.')
assert(materialDialogSource.includes('label="缺少数量"'), 'Material shortage dialog must show shortage quantity column.')
assert(
  materialDialogSource.includes('formatQuantity(row.shortageQty)'),
  'Material shortage dialog must format shortage quantity directly from shortageQty.'
)

console.log('PASS: MES schedule order replan issue remark and material dialog static contract')
