const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const root = path.resolve(__dirname, '../..')
const read = (relativePath) =>
  fs.readFileSync(path.join(root, relativePath), 'utf8').replace(/\r\n/g, '\n')

const dialog = read('src/views/mes/pro/batchrecordformlist/BatchRecordCellRulesConfirmDialog.vue')

const includes = (content, token, message) => assert.ok(content.includes(token), message)

const rowsCheck = 'if (rows.length === 0) {'
const subjectCheck = 'if (assistResponsibilitySubjects.value.length === 0) {'

includes(dialog, 'const rows = orderAssistGridRows(normalizeAssistRows(assistRows.value))', '保存前必须先规范化辅助行。')
includes(dialog, rowsCheck, '没有辅助行时不应先要求责任主体。')
includes(dialog, subjectCheck, '存在辅助行时仍应要求至少一个责任主体。')
assert.ok(
  dialog.indexOf(rowsCheck) < dialog.indexOf(subjectCheck),
  '保存前必须先判断是否真的存在辅助行，再要求责任主体。'
)

console.log('PASS batch-record-cell-rules-save-assist-subject-guard-static')
