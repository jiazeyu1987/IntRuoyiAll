const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const pagePath = path.resolve(process.cwd(), 'src/views/mes/pro/edhr/ExecutionPage.vue')
const source = fs.readFileSync(pagePath, 'utf8')

const dialogStart = source.indexOf('class="edhr-fill-workspace__save-signature-dialog"')
assert.notEqual(dialogStart, -1, '保存草稿前置签名弹框必须使用少字大字的新样式。')
const templateStart = source.lastIndexOf('<el-dialog', dialogStart)
const templateEnd = source.indexOf('</el-dialog>', dialogStart)
assert.ok(templateStart > -1 && templateEnd > templateStart, '保存草稿前置签名弹框模板必须完整。')
const dialogTemplate = source.slice(templateStart, templateEnd)

for (const text of ['保存草稿', '订单', '工序', '变更', '原因分类', '原因说明', '签名密码', '确认保存']) {
  assert.ok(dialogTemplate.includes(text), `保存草稿签名弹框必须保留必要短文案：${text}`)
}

for (const token of [
  '>FIELD_CHANGE<',
  '签名动作',
  '签名含义',
  '不可篡改审计链',
  '执行编号',
  '单元格值哈希',
  '字段审计版本',
  '字段审计头哈希',
  '待保存附件',
  'fileUrl',
  'attachmentGroupKey'
]) {
  assert.ok(!dialogTemplate.includes(token), `保存草稿签名弹框不得展示技术内容：${token}`)
}

assert.ok(
  /<el-dialog[\s\S]*class="edhr-fill-workspace__save-signature-dialog"[\s\S]*width="720px"[\s\S]*:show-close="false"[\s\S]*:close-on-click-modal="false"[\s\S]*:close-on-press-escape="false"/.test(dialogTemplate),
  '保存草稿签名弹框必须是大弹框，并禁用右上角关闭、遮罩关闭和 ESC 关闭。'
)

assert.ok(
  /pendingFieldChanges\.length\s*\+\s*pendingAttachmentChanges\.length/.test(dialogTemplate),
  '保存草稿签名弹框的变更数量必须同时包含字段变更和附件变更。'
)

assert.ok(
  /\.edhr-fill-workspace__save-signature-title\s*\{[\s\S]*font-size:\s*34px/.test(source),
  '保存草稿签名弹框标题必须使用 34px 大字。'
)

assert.ok(
  /\.edhr-fill-workspace__save-signature-confirm\s*\{[\s\S]*height:\s*64px[\s\S]*font-size:\s*24px/.test(source),
  '保存草稿签名弹框确认按钮必须高 64px 且 24px 大字。'
)

console.log('PASS: EDHR fill workspace save signature dialog static contract')
