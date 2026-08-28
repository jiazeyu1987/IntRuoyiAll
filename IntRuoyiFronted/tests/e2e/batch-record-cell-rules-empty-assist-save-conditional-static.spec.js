const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const root = path.resolve(__dirname, '../..')
const read = (relativePath) =>
  fs.readFileSync(path.join(root, relativePath), 'utf8').replace(/\r\n/g, '\n')

const dialog = read('src/views/mes/pro/batchrecordformlist/BatchRecordCellRulesConfirmDialog.vue')

const includes = (content, token, message) => assert.ok(content.includes(token), message)
const notIncludes = (content, token, message) => assert.ok(!content.includes(token), message)

includes(dialog, 'const rows = orderAssistGridRows(normalizeAssistRows(assistRows.value))', '保存前必须先规范化辅助行。')
includes(dialog, 'if (rows.length === 0) {', '空辅助行必须走单独分支。')
includes(dialog, 'return []', '空辅助行必须直接返回空辅助映射。')
includes(dialog, 'const assistRowsForSave = normalizedAssistRowsForSave()', '保存入口必须继续复用辅助行归一化结果。')
includes(dialog, 'const hasAssistRowsForSave = assistRowsForSave.length > 0', '保存入口必须显式区分空辅助行和真实辅助行。')
includes(dialog, 'if (hasAssistRowsForSave) {', '只有存在辅助行时才应继续保存辅助层配置。')
includes(dialog, 'EdhrProcessFormPermissionRuleApi.saveByReport({', '辅助层填写人保存仍必须使用真实接口。')
notIncludes(dialog, 'throw new Error(\'请先在辅助表格中完成原表单元格映射。\')', '空辅助行保存不应再阻断映射提示。')

console.log('PASS batch-record-cell-rules-empty-assist-save-conditional-static')
