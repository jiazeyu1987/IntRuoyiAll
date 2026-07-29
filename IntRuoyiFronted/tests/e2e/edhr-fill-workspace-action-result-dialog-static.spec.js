const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const pagePath = path.resolve(process.cwd(), 'src/views/mes/pro/edhr/ExecutionPage.vue')
const source = fs.readFileSync(pagePath, 'utf8')

const resultDialogClassIndex = source.indexOf('class="edhr-fill-workspace__result-dialog"')
assert.notEqual(resultDialogClassIndex, -1, '填写工作区操作结果大弹框缺少专用 class。')
const resultDialogStart = source.lastIndexOf('<el-dialog', resultDialogClassIndex)
assert.notEqual(resultDialogStart, -1, '填写工作区操作结果大弹框缺少 el-dialog 模板。')
const resultDialogEnd = source.indexOf('</el-dialog>', resultDialogStart)
assert.notEqual(resultDialogEnd, -1, '填写工作区操作结果大弹框模板未闭合。')
const resultDialogTemplate = source.slice(resultDialogStart, resultDialogEnd)

const resultDialogStateStart = source.indexOf('type FillActionResultDialogState')
const resultDialogStateEnd = source.indexOf('type BatchSharedFillScopeRange', resultDialogStateStart)
assert.ok(resultDialogStateStart > -1 && resultDialogStateEnd > resultDialogStateStart, '填写工作区操作结果大弹框缺少状态契约。')
const resultDialogStateSource = source.slice(resultDialogStateStart, resultDialogStateEnd)

const resultDialogLogicStart = source.indexOf('const resolveFillActionResultOrderText')
const resultDialogLogicEnd = source.indexOf('const resolveAssistFieldTypeLabel', resultDialogLogicStart)
assert.ok(resultDialogLogicStart > -1 && resultDialogLogicEnd > resultDialogLogicStart, '填写工作区操作结果大弹框缺少取值和展示逻辑。')
const resultDialogLogicSource = source.slice(resultDialogLogicStart, resultDialogLogicEnd)
const resultDialogContractSource = [resultDialogTemplate, resultDialogStateSource, resultDialogLogicSource].join('\n')

for (const token of [
  'fillActionResultDialogVisible',
  'fillActionResultDialog',
  'showFillActionResultDialog',
  'resolveFillActionResultOrderText',
  'resolveFillActionResultProcessText',
  'resolveFillActionResultFillerText',
  "showFillActionResultDialog('save-success'",
  "showFillActionResultDialog('submit-success'",
  "showFillActionResultDialog('submit-failed', submitErrorMessage",
  'edhr-fill-workspace__result-dialog',
  'edhr-fill-workspace__result-close',
  'edhr-fill-workspace__result-status',
  'edhr-fill-workspace__result-context',
  'edhr-fill-workspace__result-reason',
  'edhr-fill-workspace__result-confirm',
  'closeFillActionResultDialog',
  '@click="closeFillActionResultDialog"',
  'aria-label="关闭结果弹窗"'
]) {
  assert.ok(source.includes(token), `填写工作区操作结果大弹框缺少契约片段：${token}`)
}

for (const text of ['订单', '工序', '已保存', '已提交', '提交失败', '失败原因', '确认']) {
  assert.ok(source.includes(text), `填写工作区操作结果大弹框必须显示用户要求的简短大字文案：${text}`)
}

for (const text of ['保存结果', '提交结果', '操作结果', '填写人', '当前任务', '当前工序', '当前填写人']) {
  assert.ok(!resultDialogContractSource.includes(text), `填写工作区操作结果大弹框不得额外显示未要求文案：${text}`)
}

assert.ok(
  !/\s:?title=/.test(resultDialogTemplate),
  '填写工作区操作结果大弹框不得显示额外标题。'
)

assert.ok(
  /<el-dialog[\s\S]*class="edhr-fill-workspace__result-dialog"[\s\S]*width="720px"/.test(source),
  '操作结果弹框必须是宽 720px 的大弹框。'
)

assert.ok(
  /<el-dialog[\s\S]*class="edhr-fill-workspace__result-dialog"[\s\S]*:show-close="false"[\s\S]*:close-on-click-modal="false"[\s\S]*:close-on-press-escape="false"/.test(source),
  '操作结果弹框必须禁用 Element Plus 默认关闭、点击遮罩关闭和 ESC 关闭，只保留受控关闭按钮和确认按钮。'
)

assert.ok(
  /<button[\s\S]*class="edhr-fill-workspace__result-close"[\s\S]*aria-label="关闭结果弹窗"[\s\S]*@click="closeFillActionResultDialog"[\s\S]*<Icon icon="ep:close" \/>[\s\S]*<\/button>/.test(resultDialogTemplate),
  '操作结果弹框右上角必须提供受控关闭按钮。'
)

assert.ok(
  /class="edhr-fill-workspace__result-status"[\s\S]*fillerText[\s\S]*statusText/.test(resultDialogTemplate),
  '操作结果弹框必须把“谁”和“已保存/已提交/提交失败”合成一行。'
)

assert.ok(
  /class="edhr-fill-workspace__result-reason"[\s\S]*v-if="fillActionResultDialog\.failureReasonText"[\s\S]*失败原因[\s\S]*failureReasonText/.test(
    resultDialogTemplate
  ),
  '提交失败弹框必须在大字状态下方展示真实失败原因。'
)

assert.ok(
  /failureReasonText:\s*string/.test(resultDialogStateSource),
  '操作结果弹框状态必须承载 failureReasonText。'
)

assert.ok(
  /showFillActionResultDialog\s*=\s*\(\s*type:\s*FillActionResultType,\s*failureReasonText\s*=\s*''\s*\)/.test(
    resultDialogLogicSource
  ),
  'showFillActionResultDialog 必须接收失败原因参数。'
)

assert.ok(
  /fillActionResultDialog\.failureReasonText\s*=\s*type\s*===\s*'submit-failed'[\s\S]*failureReasonText[\s\S]*:\s*''/.test(
    resultDialogLogicSource
  ),
  '只有提交失败弹框可以展示失败原因，成功弹框必须清空失败原因。'
)

assert.ok(
  /<template #footer>[\s\S]*edhr-fill-workspace__result-confirm[\s\S]*确认[\s\S]*<\/template>/.test(source),
  '操作结果弹框底部只能提供一个大号“确认”按钮。'
)

assert.ok(
  /\.edhr-fill-workspace__result-status\s*\{[\s\S]*font-size:\s*34px/.test(source),
  '操作结果状态文字必须使用 34px 大字。'
)

assert.ok(
  /\.edhr-fill-workspace__result-context\s*\{[\s\S]*font-size:\s*22px/.test(source),
  '操作结果上下文必须使用 22px 大字。'
)

assert.ok(
  /\.edhr-fill-workspace__result-confirm\s*\{[\s\S]*height:\s*64px[\s\S]*font-size:\s*24px/.test(source),
  '操作结果确认按钮必须高 64px 且 24px 大字。'
)

assert.ok(
  /const closeFillActionResultDialog = \(\) => \{[\s\S]*fillActionResultDialogVisible\.value = false[\s\S]*\}/.test(resultDialogLogicSource),
  '操作结果弹框关闭事件必须只关闭当前结果弹框。'
)

assert.ok(
  /\.edhr-fill-workspace__result-close\s*\{[\s\S]*position:\s*absolute[\s\S]*top:\s*12px[\s\S]*right:\s*12px/.test(source),
  '操作结果弹框关闭按钮必须固定在右上角。'
)

assert.ok(
  source.indexOf("showFillActionResultDialog('save-success'") > source.indexOf('saveEdhrFieldChanges'),
  '保存成功后必须弹出“已保存”大弹框。'
)

assert.ok(
  source.indexOf("showFillActionResultDialog('submit-success'") > source.indexOf('submitEdhrExecution'),
  '提交成功后必须弹出“已提交”大弹框。'
)

assert.ok(
  source.indexOf("showFillActionResultDialog('submit-failed', submitErrorMessage") >
    source.indexOf('catch (error)'),
  '提交失败后必须把真实错误原因传入“提交失败”大弹框。'
)

assert.ok(!/mock|降级|静默跳过/.test(source), '操作结果弹框不得引入 mock、降级或静默跳过。')

console.log('PASS: EDHR fill workspace action result dialog static contract')
