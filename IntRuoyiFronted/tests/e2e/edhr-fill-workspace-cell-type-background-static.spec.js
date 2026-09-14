import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const currentDir = path.dirname(fileURLToPath(import.meta.url))
const frontendRoot = path.resolve(currentDir, '../..')
const read = (relativePath) => fs.readFileSync(path.resolve(frontendRoot, relativePath), 'utf8')

const executionPage = read('src/views/mes/pro/edhr/ExecutionPage.vue')
const editableForm = read(
  'src/views/mes/pro/edhr/components/EdhrExecutionTemplateEditableForm.vue'
)

assert.match(
  executionPage,
  /<EdhrExecutionTemplateEditableForm[\s\S]*?cell-type-display="background"/,
  'eDHR 执行填写页原表模式必须使用类型背景色展示。'
)

assert.equal(
  (editableForm.match(/v-if="cell\.ruleBadge && props\.cellTypeDisplay === 'badge'"/g) || [])
    .length,
  2,
  '适应视口和普通视口都必须在背景色模式下隐藏单元格类型 item。'
)

for (const token of [
  "cellTypeDisplay?: 'badge' | 'background'",
  "cellTypeDisplay: 'badge'",
  "'is-cell-type-background'",
  'is-cell-type-text',
  'is-cell-type-number',
  'is-cell-type-date',
  'is-cell-type-datetime',
  'is-cell-type-boolean',
  'is-cell-type-signature',
  'is-cell-type-attachment'
]) {
  assert.ok(editableForm.includes(token), `模板组件缺少类型背景色契约：${token}`)
}

for (const colorRule of [
  '.edhr-template-editable-form__cell.is-cell-type-background.is-cell-type-text',
  '.edhr-template-editable-form__cell.is-cell-type-background.is-cell-type-number',
  '.edhr-template-editable-form__cell.is-cell-type-background.is-cell-type-date',
  '.edhr-template-editable-form__cell.is-cell-type-background.is-cell-type-datetime',
  '.edhr-template-editable-form__cell.is-cell-type-background.is-cell-type-boolean',
  '.edhr-template-editable-form__cell.is-cell-type-background.is-cell-type-signature',
  '.edhr-template-editable-form__cell.is-cell-type-background.is-cell-type-attachment'
]) {
  assert.ok(editableForm.includes(colorRule), `模板组件必须为类型提供背景色：${colorRule}`)
}

assert.ok(
  editableForm.includes('modelValue[cell.editableContext.fieldIdentity]'),
  '隐藏类型 item 后仍必须保留原表字段控件。'
)
assert.ok(
  editableForm.includes('showRuleLegend: true'),
  '共享模板组件默认小标识/图例能力必须保持，避免影响其他页面。'
)

console.log('PASS: eDHR execution cells use type background colors without type items')
