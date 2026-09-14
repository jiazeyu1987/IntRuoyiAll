const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const pagePath = path.resolve(process.cwd(), 'src/views/mes/pro/edhr/ExecutionPage.vue')
const apiPath = path.resolve(process.cwd(), 'src/api/mes/pro/feedback/index.ts')
const source = fs.readFileSync(pagePath, 'utf8').replace(/\r\n/g, '\n')
const api = fs.readFileSync(apiPath, 'utf8')

for (const token of [
  'edhr-fill-workspace__submit-action',
  "v-hasPermi=\"['mes:pro-batch-record-execution:update']\"",
  ':loading="submitLoading"',
  ':disabled="hasSlotContextBlockers || hasPendingFieldChanges"',
  '@click="openSubmitDialog"'
]) {
  assert.ok(source.includes(token), `填写工作区提交入口缺少契约片段：${token}`)
}

assert.ok(/>\s*提交执行\s*<\/el-button>/.test(source), '填写工作区提交入口必须显示“提交执行”。')

assert.ok(
  source.indexOf('edhr-fill-workspace__submit-action') > source.indexOf('edhr-fill-workspace__primary-action'),
  '提交执行入口应跟随保存入口，确保填表工作区可完成保存后提交。'
)

const submitDialogClassIndex = source.indexOf('class="edhr-fill-workspace__submit-sign-dialog"')
assert.notEqual(submitDialogClassIndex, -1, '提交执行电子签名弹框必须使用专用精简弹框。')
const submitDialogStart = source.lastIndexOf('<el-dialog', submitDialogClassIndex)
assert.notEqual(submitDialogStart, -1, '提交执行电子签名弹框必须使用 el-dialog。')
const submitDialogEnd = source.indexOf('</el-dialog>', submitDialogStart)
assert.ok(submitDialogEnd > submitDialogStart, '提交执行电子签名弹框模板必须闭合。')
const submitDialogTemplate = source.slice(submitDialogStart, submitDialogEnd)

for (const token of [
  'edhr-fill-workspace__submit-sign-row',
  'edhr-fill-workspace__submit-sign-label">姓名',
  'submitSignatureUserName',
  'edhr-fill-workspace__submit-sign-label is-required">电子签名',
  'v-model="submitForm.password"',
  '@keyup.enter="handleSubmitExecution"',
  'edhr-fill-workspace__submit-sign-close',
  '@click="closeSubmitDialog"',
  'aria-label="关闭电子签名弹窗"',
  'edhr-fill-workspace__submit-sign-confirm',
  '确认'
]) {
  assert.ok(submitDialogTemplate.includes(token), `提交弹框必须保留用户要求内容：${token}`)
}

for (const forbidden of [
  '提交 eDHR 执行',
  '普通工序提交',
  '金手指测试权限',
  '审核/批准人',
  '提交备注',
  '签名显示时间',
  '签名时间',
  '签名时区',
  '时间原因',
  '取 消',
  '确认提交'
]) {
  assert.ok(!submitDialogTemplate.includes(forbidden), `提交弹框不得显示用户未要求内容：${forbidden}`)
}
assert.ok(
  !submitDialogTemplate.includes('电子签名密码'),
  '提交弹框标签必须是“电子签名”，不得显示“电子签名密码”。'
)

assert.ok(
  /<el-dialog[\s\S]*class="edhr-fill-workspace__submit-sign-dialog"[\s\S]*:show-close="false"[\s\S]*:close-on-click-modal="false"[\s\S]*:close-on-press-escape="false"/.test(submitDialogTemplate),
  '提交弹框必须禁用 Element Plus 默认关闭、点击遮罩关闭和 ESC 关闭，只保留受控关闭按钮。'
)
assert.ok(
  /<button[\s\S]*class="edhr-fill-workspace__submit-sign-close"[\s\S]*aria-label="关闭电子签名弹窗"[\s\S]*@click="closeSubmitDialog"[\s\S]*<Icon icon="ep:close" \/>[\s\S]*<\/button>/.test(submitDialogTemplate),
  '提交弹框右上角必须提供受控关闭按钮。'
)
assert.ok(
  /<template #footer>[\s\S]*edhr-fill-workspace__submit-sign-confirm[\s\S]*确认[\s\S]*<\/template>/.test(submitDialogTemplate),
  '提交弹框底部只能提供一个“确认”按钮。'
)
assert.ok(
  !/\s:?title=/.test(submitDialogTemplate),
  '提交弹框不得显示额外标题。'
)
assert.ok(!source.includes('submitForm.comment'), '提交弹框不得保留提交备注字段。')
assert.ok(!source.includes('submitSignatureTimeForm'), '提交弹框不得保留签名显示时间字段。')
assert.ok(
  !source.includes('buildSignatureTimePayload(submitSignatureTimeForm)'),
  '提交请求不得再发送提交弹框签名显示时间。'
)
assert.ok(
  /\.edhr-fill-workspace__submit-sign-confirm\s*\{[\s\S]*height:\s*56px[\s\S]*font-size:\s*20px/.test(source),
  '提交确认按钮必须是大按钮。'
)
assert.ok(
  /\.edhr-fill-workspace__submit-sign-close\s*\{[\s\S]*position:\s*absolute[\s\S]*top:\s*12px[\s\S]*right:\s*12px/.test(source),
  '提交弹框关闭按钮必须固定在右上角。'
)
assert.ok(
  /\.edhr-fill-workspace__submit-sign-row\s*\{[\s\S]*grid-template-columns:\s*104px minmax\(0,\s*1fr\)[\s\S]*align-items:\s*center/.test(source) &&
    /\.edhr-fill-workspace__submit-sign-label\s*\{[\s\S]*text-align:\s*left[\s\S]*white-space:\s*nowrap/.test(source) &&
    /\.edhr-fill-workspace__submit-sign-name\s*\{[\s\S]*min-height:\s*48px[\s\S]*align-items:\s*center/.test(source),
  '姓名行和电子签名输入行必须使用固定两列网格对齐。'
)

assert.ok(
  api.includes('submitEdhrExecution: async (data: ProFeedbackEdhrSubmitReqVO)') &&
    api.includes("headers: { 'Content-Type': 'application/json' }"),
  '提交 eDHR 执行请求必须使用后端支持的 application/json Content-Type。'
)

assert(!/mock|降级|静默跳过/.test(source), '填写工作区提交入口不得引入 mock、降级或静默跳过。')

console.log('PASS: EDHR execution fill workspace submit static contract')
