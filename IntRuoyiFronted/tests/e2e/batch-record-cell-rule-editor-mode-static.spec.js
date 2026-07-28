const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const dialog = read('src/views/mes/pro/batchrecordformlist/BatchRecordCellRulesConfirmDialog.vue')

const assertIncludes = (content, token, message) => assert.ok(content.includes(token), message)
const assertNotIncludes = (content, token, message) => assert.ok(!content.includes(token), message)

assertIncludes(dialog, 'batch-record-cell-rules-editor', '单元格规则弹窗必须切换为规则编辑模式布局。')
assertIncludes(dialog, 'batch-record-cell-rules-editor__preview', '规则编辑模式必须提供左侧只读表单预览区。')
assertIncludes(dialog, 'batch-record-cell-rules-editor__side-panel', '规则编辑模式必须提供右侧规则编辑面板。')
assertIncludes(dialog, 'selectedRuleKey', '规则编辑模式必须维护当前选中的单元格规则。')
assertIncludes(dialog, 'selectedRule', '规则编辑模式必须把右侧表单绑定到当前选中规则。')
assertIncludes(dialog, 'selectRuleCell', '点击预览单元格必须只选择规则目标。')
assertIncludes(dialog, '@click="handleSourceCellClick(cell)"', '预览单元格点击必须通过统一入口按当前模式分流。')
assertIncludes(dialog, 'batch-record-cell-rules-editor__cell-button', '预览单元格必须使用按钮层承接点击，避免真实控件响应。')
assertIncludes(dialog, ':aria-label="activeConfigMode === \'assistMapping\' ? \'映射原表单元格\' : \'选择单元格规则\'"', '预览单元格按钮必须具备明确可访问语义。')

assertIncludes(dialog, 'v-model="selectedRule.required"', '右侧面板必须可编辑必填状态。')
assertIncludes(dialog, 'v-model="selectedRule.valueType"', '右侧面板必须可编辑字段类型。')
assertIncludes(dialog, 'v-model="selectedRule.label"', '右侧面板必须可编辑字段名称。')
assertIncludes(dialog, 'v-model="selectedRule.componentFlag"', '右侧面板必须可编辑控件类型。')
assertIncludes(dialog, 'cellRuleDefaultComponentMap', '类型切换时必须沿用规则默认控件映射。')
assertIncludes(dialog, 'BatchRecordReportApi.saveCellRules', '保存必须继续复用真实 cell-rules 接口。')
assertIncludes(dialog, "source: 'MANUAL'", '保存前必须把规则标记为人工规则。')
assertIncludes(dialog, 'reviewed: true', '保存前必须把规则标记为已确认。')

assertIncludes(dialog, 'parseSheetLayout', '规则编辑模式必须从 sheetLayoutJson 渲染真实表单结构。')
assertIncludes(dialog, 'normalizeTemplateCellMerge', '规则编辑模式必须保留合并单元格结构。')
assertIncludes(dialog, 'stringifyTemplateCell', '规则编辑模式必须按模板原始内容展示单元格文本。')
assertNotIncludes(dialog, 'EdhrExecutionTemplateEditableForm', '规则编辑模式不得复用真实填写控件组件，避免日期/签名/复选框误触。')

console.log('PASS: batch record cell rule editor mode static contract')
