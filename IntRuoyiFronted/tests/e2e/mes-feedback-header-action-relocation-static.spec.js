const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const pagePath = path.resolve(process.cwd(), 'src/views/mes/pro/feedback/index.vue')
assert(fs.existsSync(pagePath), `生产报工页面必须存在：${pagePath}`)

const source = fs.readFileSync(pagePath, 'utf8')

const firstContentWrapStart = source.indexOf('<ContentWrap>')
const feedbackListStart = source.indexOf('<ContentWrap v-if="activeTab === \'feedback\'">')
const importRecordStart = source.indexOf('<ContentWrap v-if="activeTab === \'import-record\'">')

assert(feedbackListStart >= 0, '正式报工列表区域必须存在。')
assert(importRecordStart >= 0, '待归属列表区域必须存在。')
assert(
  firstContentWrapStart < 0 || firstContentWrapStart > feedbackListStart,
  '不得保留截图蓝框中的独立顶部 ContentWrap。'
)
assert(!source.includes('class="feedback-tabs"'), '不得继续使用独立顶部页签样式 feedback-tabs。')
assert(!source.includes('<el-tabs'), '不得继续渲染截图蓝框中的独立页签。')

const feedbackListEnd = source.indexOf('</UnifiedListTemplate>', feedbackListStart)
assert(feedbackListEnd > feedbackListStart, '正式报工 UnifiedListTemplate 必须闭合。')
const feedbackListSource = source.slice(feedbackListStart, feedbackListEnd)

assert(
  feedbackListSource.includes('<template #actions>'),
  '正式报工按钮必须位于 UnifiedListTemplate actions 操作区。'
)

assert(
  feedbackListSource.includes('class="feedback-filter-action-relocation"'),
  '黄框按钮必须放入筛选行红框位置的独立容器。'
)
assert(
  feedbackListSource.includes('class="feedback-filter-reset-action"'),
  '重置按钮必须保留在筛选行最右侧的独立容器。'
)

const relocatedStart = feedbackListSource.indexOf('class="feedback-filter-action-relocation"')
const resetStart = feedbackListSource.indexOf('class="feedback-filter-reset-action"')
assert(relocatedStart >= 0, '必须存在红框位置按钮容器。')
assert(resetStart > relocatedStart, '重置按钮必须位于红框位置按钮之后。')
const relocatedActionsSource = feedbackListSource.slice(relocatedStart, resetStart)

for (const actionText of ['第三方导入', '导出']) {
  assert(relocatedActionsSource.includes(actionText), `红框位置必须包含按钮：${actionText}`)
}
for (const removedActionText of ['新增', '模拟报工']) {
  assert(!relocatedActionsSource.includes(removedActionText), `红框位置不得继续包含绿框按钮：${removedActionText}`)
  assert(!feedbackListSource.includes(removedActionText), `正式报工筛选行不得继续显示绿框按钮：${removedActionText}`)
}
assert(!relocatedActionsSource.includes('重置'), '红框位置不得包含重置按钮。')

for (const actionText of ['第三方导入', '导出', '重置']) {
  const actionIndex = feedbackListSource.indexOf(actionText)
  const actionsSlotIndex = feedbackListSource.indexOf('<template #actions>')
  assert(actionIndex > actionsSlotIndex, `正式报工操作区必须包含按钮：${actionText}`)
}

const importRecordSource = source.slice(importRecordStart, feedbackListStart)
assert(
  importRecordSource.includes('class="feedback-import-toolbar"'),
  '待归属筛选区必须保留紧凑操作工具栏。'
)
for (const actionText of ['第三方导入', '模拟报工']) {
  assert(importRecordSource.includes(actionText), `待归属工具栏必须保留按钮：${actionText}`)
}

assert(!source.includes('catch {}'), '生产报工页不得吞掉异常。')
assert(!source.includes('catch{}'), '生产报工页不得吞掉异常。')

console.log('PASS: MES feedback header action relocation static contract')
