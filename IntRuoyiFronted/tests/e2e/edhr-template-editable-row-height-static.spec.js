import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const currentDir = path.dirname(fileURLToPath(import.meta.url))
const frontendRoot = path.resolve(currentDir, '../..')
const editableForm = fs.readFileSync(
  path.resolve(frontendRoot, 'src/views/mes/pro/edhr/components/EdhrExecutionTemplateEditableForm.vue'),
  'utf8'
)

assert.match(
  editableForm,
  /const MIN_TEXT_EDITABLE_ROW_HEIGHT = 48/,
  '普通“请填写”输入行必须至少 48px，避免输入框与上下行重叠'
)

assert.match(
  editableForm,
  /const MIN_TALL_EDITABLE_ROW_HEIGHT = 72/,
  'textarea、附件、签名等高控件行必须有更高的行高下限'
)

assert.match(
  editableForm,
  /const editableRowHeightFloorMap = computed<Map<number, number>>/,
  '必须在渲染前按可编辑单元格计算每行行高下限'
)

assert.match(
  editableForm,
  /const resolveRenderedRowHeight = \(rawHeight: unknown, editableHeightFloor = 0\)/,
  '必须集中计算最终渲染行高'
)

assert.match(
  editableForm,
  /const rawHeightNumber = Number\(rawHeight\)[\s\S]*Math\.max\([\s\S]*editableHeightFloor[\s\S]*24[\s\S]*\)/,
  '最终行高必须同时尊重模板原始行高、可编辑控件行高下限和基础下限'
)

assert.doesNotMatch(
  editableForm,
  /height:\s*Number\.isFinite\(rawHeight\) && rawHeight > 0 \? Math\.max\(rawHeight,\s*24\) : DEFAULT_ROW_HEIGHT/,
  '不得继续只用 24px 下限渲染可填写输入行'
)

console.log('PASS: eDHR editable template row height contract')
