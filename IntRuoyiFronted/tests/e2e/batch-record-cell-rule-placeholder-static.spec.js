const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const dialog = read('src/views/mes/pro/batchrecordformlist/BatchRecordCellRulesConfirmDialog.vue')
const reportApi = read('src/api/mes/pro/batchrecordreport/index.ts')
const ruleHelper = read('src/views/mes/pro/batchrecord-shared/batchRecordTemplateRules.ts')
const editableForm = read('src/views/mes/pro/edhr/components/EdhrExecutionTemplateEditableForm.vue')
const readonlyForm = read('src/views/mes/pro/edhr/components/EdhrExecutionReadonlyForm.vue')
const executionPage = read('src/views/mes/pro/edhr/ExecutionPage.vue')

const assertIncludes = (content, token, message) => assert.ok(content.includes(token), message)
const assertNotIncludes = (content, token, message) => assert.ok(!content.includes(token), message)

assertNotIncludes(dialog, 'batch-record-cell-rules-editor__rule-list', '右侧红框内的规则清单块不应再渲染。')
assertNotIncludes(dialog, '规则清单', '右侧面板不应再显示“规则清单”标题。')
assertNotIncludes(dialog, 'selectRuleByKey', '隐藏规则清单后不应保留只供清单点击使用的选择函数。')

assertIncludes(dialog, 'label="单元格提示词"', '右侧规则表单必须增加单元格提示词字段。')
assertIncludes(dialog, 'v-model="selectedRule.placeholder"', '单元格提示词必须直接绑定到规则 placeholder 字段。')
assertIncludes(dialog, 'placeholder="请输入单元格空值提示"', '单元格提示词输入框必须说明它是空值提示。')

assertIncludes(reportApi, 'placeholder?: string', '前端规则 VO 必须声明 placeholder 字段。')
assertIncludes(ruleHelper, 'placeholder?: string', '模板规则上下文必须携带 placeholder 字段。')
assertIncludes(ruleHelper, 'placeholder: normalizedRule.placeholder', 'normalize/build 过程必须保留规则 placeholder。')
assertIncludes(editableForm, 'cell.editableContext.placeholder', '模板内填写默认控件必须使用规则 placeholder。')
assertIncludes(readonlyForm, 'cell?.fillForm?.placeholder', '只读模板空值占位仍必须读取 fillForm placeholder。')
assertIncludes(readonlyForm, 'cell?.edhrCellRule?.placeholder', '只读模板应兼容直接来自 edhrCellRule 的 placeholder。')
assertIncludes(executionPage, 'placeholder: field.placeholder', '执行页模板规则必须把快照 placeholder 传给模板内填写组件。')

console.log('PASS: batch record cell rule placeholder static contract')
