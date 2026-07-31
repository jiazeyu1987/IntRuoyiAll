const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const root = path.resolve(__dirname, '../..')
const read = (relativePath) =>
  fs.readFileSync(path.join(root, relativePath), 'utf8').replace(/\r\n/g, '\n')

const dialog = read('src/views/mes/pro/batchrecordformlist/BatchRecordCellRulesConfirmDialog.vue')

const includes = (content, token, message) => assert.ok(content.includes(token), message)
const notIncludes = (content, token, message) => assert.ok(!content.includes(token), message)

includes(dialog, 'assistGridRowCount', '辅助表单映射必须由右侧控制栏配置表格行数。')
includes(dialog, 'assistGridColumnCount', '辅助表单映射必须由右侧控制栏配置表格列数。')
includes(dialog, 'assistResponsibilitySubjects', '辅助表单映射必须维护当前表单需要的责任主体集合。')
includes(dialog, 'selectedAssistSubjectKey', '辅助表单映射必须支持切换当前责任主体。')
includes(dialog, 'pendingAssistSubjectType', '辅助表单映射必须支持选择个人或角色责任主体。')
includes(dialog, 'selectedAssistGridCellKey', '辅助表单映射必须先选中辅助表格单元格。')
includes(dialog, 'data-assist-grid-cell', '辅助表单预览必须渲染可点击的 M*N 表格单元格。')
includes(dialog, 'handleAssistGridCellClick', '点击辅助表格单元格必须设置当前映射目标。')
includes(dialog, '@dblclick.stop="handleAssistGridCellDoubleClick(gridCell)"', '双击已映射辅助格必须取消映射。')
includes(dialog, 'handleAssistGridCellDoubleClick', '辅助表格必须有双击取消映射处理函数。')
includes(dialog, 'mapSourceCellToSelectedAssistGridCell', '点击原表单元格必须映射到当前辅助表格单元格。')
includes(dialog, 'removeAssistGridCellMapping', '辅助表格单元格必须提供取消映射动作。')
includes(dialog, 'sourceCellGridAssignmentMap', '组件必须维护原表单元格到辅助格子的全局分配索引。')
includes(dialog, 'isSourceCellDisabledForAssistMapping', '已分配原表单元格必须在辅助映射模式灰化禁点。')
includes(dialog, ':disabled="isSourceCellDisabledForAssistMapping(cell)"', '原表按钮必须用 disabled 阻止已分配单元格被再次点击。')
includes(dialog, "'is-assist-mapped': isSourceCellMappedToAssistGrid(cell)", '已分配原表单元格必须有灰化样式类。')
includes(dialog, 'sourceCellGridAssignmentMap.value.has(cell.identity)', '同一原表单元格必须全局只能归属一个辅助格。')
includes(dialog, 'ASSIST_GRID_ROW_KEY_PREFIX', '保存时必须用稳定 rowKey 表达用户和辅助格位置。')
includes(dialog, 'parseAssistGridRowKey', '读取已有 assistRows 时必须还原用户和辅助格位置。')
includes(dialog, 'candidateSourceType: selectedAssistSubject.value.candidateSourceType', '每个辅助格保存时必须明确使用当前责任主体类型。')
includes(dialog, 'candidateSourceIds: [...selectedAssistSubject.value.candidateSourceIds]', '辅助格保存的 fillAssignment 必须指向当前责任主体。')
assert.match(
  dialog,
  /\.batch-record-cell-rules-editor__assist-grid-cell span \{[\s\S]*?text-overflow: ellipsis;[\s\S]*?white-space: nowrap;/,
  '已映射辅助格字段名必须单行显示并用省略号截断。'
)
notIncludes(dialog, 'batch-record-cell-rules-editor__assist-grid-unmap', '辅助表格不应继续显示独立取消映射按钮。')
notIncludes(dialog, '<em v-if="gridCell.valueTypeLabel">', '辅助格内不应继续显示字段类型圆标。')
notIncludes(dialog, '辅助行配置', '辅助表单映射模式不应继续暴露旧辅助行配置交互。')

console.log('PASS: assist grid per-user mapping static contract')
