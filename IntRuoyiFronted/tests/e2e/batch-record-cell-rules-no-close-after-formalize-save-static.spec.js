const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const root = path.resolve(__dirname, '../..')
const read = (relativePath) =>
  fs.readFileSync(path.join(root, relativePath), 'utf8').replace(/\r\n/g, '\n')

const dialog = read('src/views/mes/pro/batchrecordformlist/BatchRecordCellRulesConfirmDialog.vue')

const includes = (content, token, message) => assert.ok(content.includes(token), message)
const notIncludes = (content, token, message) => assert.ok(!content.includes(token), message)

includes(dialog, '@click="formalizeDetectedCells"', '正式化按钮必须继续绑定正式化动作。')
includes(dialog, '@click="confirmAllRules"', '保存按钮必须继续绑定保存动作。')
includes(dialog, 'message.success(\'可映射格子已正式化\')', '正式化成功后仍应提示成功。')
includes(dialog, 'message.success(\'填写配置已保存\')', '保存成功后仍应提示成功。')
notIncludes(dialog, 'dialogVisible.value = false', '正式化和保存成功后不得自动关闭弹窗。')

console.log('PASS batch-record-cell-rules-no-close-after-formalize-save-static')
