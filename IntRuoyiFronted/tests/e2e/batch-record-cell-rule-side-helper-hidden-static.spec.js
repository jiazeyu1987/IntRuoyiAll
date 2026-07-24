const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const dialog = read('src/views/mes/pro/batchrecordformlist/BatchRecordCellRulesConfirmDialog.vue')

const assertIncludes = (content, token, message) => assert.ok(content.includes(token), message)
const assertNotIncludes = (content, token, message) => assert.ok(!content.includes(token), message)

assertIncludes(dialog, 'batch-record-cell-rules-editor__side-panel', '右侧规则编辑面板必须保留。')
assertIncludes(dialog, 'v-model="isSelectedCellFillable"', '右侧可填写切换开关必须保留。')
assertIncludes(dialog, 'active-text="可填写"', '开关必须保留可填写文字。')
assertIncludes(dialog, 'inactive-text="不可填写"', '开关必须保留不可填写文字。')
assertIncludes(dialog, 'v-model="selectedRule.label"', '字段名称编辑必须保留。')
assertIncludes(dialog, 'v-model="selectedRule.required"', '必填编辑必须保留。')
assertIncludes(dialog, 'v-model="selectedRule.valueType"', '字段类型编辑必须保留。')
assertIncludes(dialog, 'v-model="selectedRule.componentFlag"', '控件类型编辑必须保留。')

assertNotIncludes(dialog, 'batch-record-cell-rules-editor__selected-card', '截图红框中的当前单元格说明卡片不应显示。')
assertNotIncludes(dialog, '<strong>规则设置</strong>', '截图红框中的右侧标题说明不应显示。')
assertNotIncludes(dialog, '字段规则保存后会参与批记录填写提交校验。', '截图红框中的规则设置说明不应显示。')
assertNotIncludes(dialog, '白色为不可填写说明单元格，蓝色为可填写字段并参与提交校验。', '截图红框中的蓝白说明文案不应显示。')
assertNotIncludes(dialog, 'batch-record-cell-rules-editor__save-tip', '截图红框中的底部保存提示不应显示。')
assertNotIncludes(dialog, 'batch-record-cell-rules-editor__static-tip', '不可填写静态说明提示不应显示。')
assertNotIncludes(dialog, '保存后该单元格将作为可填写字段参与执行校验。', '底部保存说明不应显示。')
assertNotIncludes(dialog, '该单元格当前为不可填写静态内容', '静态单元格说明不应显示。')

console.log('PASS: batch record cell rule side helper hidden static contract')
