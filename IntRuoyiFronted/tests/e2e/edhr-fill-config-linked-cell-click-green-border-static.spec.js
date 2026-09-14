const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const root = path.resolve(__dirname, '../..')
const read = (relativePath) =>
  fs.readFileSync(path.join(root, relativePath), 'utf8').replace(/\r\n/g, '\n')

const component = read('src/views/mes/pro/batchrecordformlist/BatchRecordCellRulesConfirmDialog.vue')
const template = component.slice(
  component.indexOf('<template>'),
  component.indexOf('<script setup')
)
const script = component.slice(
  component.indexOf('<script setup'),
  component.indexOf('</script>')
)
const style = component.slice(component.indexOf('<style scoped>'))

const includes = (content, token, message) => assert.ok(content.includes(token), message)
const notIncludes = (content, token, message) => assert.ok(!content.includes(token), message)

notIncludes(
  template,
  ':disabled="isSourceCellDisabledForAssistMapping(cell)"',
  '已链接的原表单元格不能再通过 disabled 禁止点击。'
)
notIncludes(
  script,
  'isSourceCellDisabledForAssistMapping',
  '已链接原表单元格不应保留禁点 helper，避免后续回归成不可点击。'
)
includes(
  script,
  'selectLinkedAssistGridCellForSourceCell',
  '点击已链接原表单元格必须切换到对应辅助表格格子。'
)
includes(
  script,
  'const linkedAssignment = sourceCellGridAssignmentMap.value.get(cell.identity)',
  '映射处理必须先识别当前原表单元格是否已有辅助表格链接。'
)
includes(
  script,
  'selectedAssistSubjectKey.value = linkedAssignment.subjectKey',
  '点击已链接原表单元格必须同步选中辅助表格责任主体。'
)
includes(
  script,
  'selectedAssistGridCellKey.value = linkedAssignment.rowKey',
  '点击已链接原表单元格必须同步选中被链接的辅助表格单元格。'
)
notIncludes(
  script,
  '该原表单元格已分配，请先在辅助表格取消映射后再重新分配。',
  '点击已链接原表单元格不能再弹出先取消映射的阻塞提示。'
)
includes(
  style,
  '.batch-record-cell-rules-editor__workspace--assist-mapping .batch-record-cell-rules-editor__cell.is-selected',
  '辅助映射模式下原表单当前选中单元格必须使用绿色边框。'
)
includes(
  style,
  '.batch-record-cell-rules-editor__assist-grid-cell.is-mapped.is-selected',
  '辅助表单中被链接且当前选中的格子必须使用绿色边框。'
)
includes(
  style,
  'border-color: #16a34a;',
  '联动选中边框必须使用绿色。'
)
includes(
  style,
  'box-shadow: 0 0 0 2px rgba(22, 163, 74, 0.22);',
  '联动选中态必须有绿色外描边，保证截图中的边框可见。'
)

console.log('PASS edhr-fill-config-linked-cell-click-green-border-static')
