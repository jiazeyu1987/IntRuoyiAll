const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const pagePath = path.resolve(__dirname, '../../src/views/mes/pro/batchrecordformlist/index.vue')
const pageSource = fs.readFileSync(pagePath, 'utf8')

assert(
  pageSource.includes('UnifiedListTemplate'),
  '批记录表单列表必须使用标准列表模板承载列表和工具栏。'
)
assert(
  pageSource.includes('class="batch-record-form-toolbar__import-button"') &&
    pageSource.includes('@click="openWordImportDialog"'),
  '批记录表单列表必须在标准列表工具栏保留导入入口。'
)
assert(
  pageSource.includes('class="batch-record-form-preview__actions"'),
  '右侧表单预览必须保留操作区。'
)

const readStyleBlock = (selector) => {
  const start = pageSource.indexOf(`${selector} {`)
  assert.notEqual(start, -1, `必须存在样式块：${selector}`)
  const end = pageSource.indexOf('}', start)
  assert.notEqual(end, -1, `样式块必须正确闭合：${selector}`)
  return pageSource.slice(start, end)
}

const previewActionStyle = readStyleBlock('.batch-record-form-preview__actions')
for (const declaration of ['justify-content: flex-end', 'flex-wrap: wrap']) {
  assert(
    previewActionStyle.includes(declaration),
    `右侧预览操作按钮必须在窄屏下自然换行：${declaration}`
  )
}

const previewTitleStyle = readStyleBlock('.batch-record-form-preview__title')
for (const declaration of ['overflow: hidden', 'text-overflow: ellipsis', 'white-space: nowrap']) {
  assert(
    previewTitleStyle.includes(declaration),
    `右侧预览标题必须单行省略，避免挤压操作按钮：${declaration}`
  )
}

console.log('PASS: batch-record form list title actions layout static contract')
