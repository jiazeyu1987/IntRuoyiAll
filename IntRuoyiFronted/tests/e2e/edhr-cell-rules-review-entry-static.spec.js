const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const page = read('src/views/mes/pro/batchrecordformlist/index.vue')
const dialog = read('src/views/mes/pro/batchrecordformlist/BatchRecordCellRulesConfirmDialog.vue')

const assertIncludes = (content, token, message) => assert.ok(content.includes(token), message)

assertIncludes(page, "action !== 'cellRules'", 'reportId + action=cellRules must be parsed as a first-class route action.')
assertIncludes(page, 'handleTemplateActionQuery', 'batch record form list must handle action query after loading real rows.')
assertIncludes(page, 'openCellRulesDialog(row)', 'route action must open a real cell rule dialog for the matched report row.')
assertIncludes(page, 'BatchRecordCellRulesConfirmDialog', 'cell rule review must mount a real dialog component.')
assertIncludes(page, 'cellRulesDialog.visible', 'cell rule review must use visible page state.')
assertIncludes(page, "openTemplateAction(selectedReport, 'cellRules')", 'preview toolbar must expose the cell rule entry.')
assert.ok(
  !page.includes('@click.stop="openCellRulesDialog(row)"'),
  'row-level cell rule entry must stay hidden because the preview toolbar owns the rule action.'
)

assertIncludes(dialog, 'loadCellRules', 'cell rule review must load real backend cell rules before save.')
assertIncludes(dialog, 'confirmAllRules', 'cell rule review must provide a page action to confirm all current rules.')
assertIncludes(dialog, 'BatchRecordReportApi.saveCellRules', 'cell rule review must save through the real cell-rules API.')
assertIncludes(dialog, 'reviewed: true', 'confirm action must mark rules reviewed before saving.')
assertIncludes(dialog, '单元格规则已保存', 'successful save must give the operator visible feedback.')
assertIncludes(dialog, 'errorMessage.value', 'cell rule review API failures must be visible on the page.')
assertIncludes(dialog, 'resolveErrorMessage(error', 'cell rule review must surface backend errors instead of swallowing them.')
assertIncludes(dialog, '@click="confirmAllRules"', 'cell rule review must expose a real user-clickable confirm action.')
assertIncludes(dialog, '...(data.rules || [])', 'save preparation must preserve already reviewed rules.')
assertIncludes(dialog, '...(data.suggestions || [])', 'save preparation must include pending rule suggestions.')

console.log('PASS: eDHR cell rule review entry static contract')
