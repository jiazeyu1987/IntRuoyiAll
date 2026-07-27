const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const pagePath = path.resolve(process.cwd(), 'src/views/mes/pro/edhr/ExecutionPage.vue')
const source = fs.readFileSync(pagePath, 'utf8')

const menuStart = source.indexOf('data-assist-switch-menu="filler"')
const menuEnd = source.indexOf('<template #footer>', menuStart)
assert.notEqual(menuStart, -1, '切换填写人弹窗必须保留填写人候选菜单。')
assert.notEqual(menuEnd, -1, '切换填写人弹窗必须保留关闭 footer。')

const fillerMenuSource = source.slice(menuStart, menuEnd)
assert.ok(
  fillerMenuSource.includes('<strong>选择当前工序填写人</strong>'),
  '切换填写人弹窗必须保留主标题，避免用户不知道当前操作。'
)
assert.equal(
  fillerMenuSource.includes('<span>批处理表单 + 表单槽位</span>'),
  false,
  '切换填写人弹窗标题右侧不应继续渲染截图红框内的表单类型说明。'
)

for (const removedText of ['批处理表单', '工艺路线表单槽位']) {
  assert.equal(
    source.includes(removedText),
    false,
    `切换填写人候选项不应继续渲染截图红框内的来源标签：${removedText}。`
  )
}

const secondaryLabelStart = source.indexOf('const resolveAssistFillerSwitchItemSecondaryLabel')
const secondaryLabelEnd = source.indexOf('const currentAssistUserId', secondaryLabelStart)
assert.notEqual(secondaryLabelStart, -1, '必须保留填写人候选项副标题函数。')
assert.notEqual(secondaryLabelEnd, -1, '填写人候选项副标题函数必须正常结束。')

const secondaryLabelSource = source.slice(secondaryLabelStart, secondaryLabelEnd)
assert.ok(
  secondaryLabelSource.includes('resolveAssistFillerFormName(item.task)'),
  '删除红框标签后仍必须显示候选人对应的表单名称。'
)
assert.equal(
  secondaryLabelSource.includes('resolveAssistBatchTaskStatusLabel'),
  false,
  '切换填写人候选项不应继续渲染截图红框内的“可填写”状态标签。'
)

console.log('PASS: edhr switch filler redbox cleanup static contract')
