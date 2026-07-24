const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const feedbackFormPath = path.resolve(process.cwd(), 'src/views/mes/pro/feedback/FeedbackForm.vue')

assert(fs.existsSync(feedbackFormPath), `生产报工表单必须存在：${feedbackFormPath}`)

const source = fs.readFileSync(feedbackFormPath, 'utf8')

assert(
  source.includes('const buildFeedbackSuccessMessage ='),
  '报工表单必须集中构造提交成功提示，避免继续显示笼统的“报工单已提交”。'
)

for (const token of [
  'workOrderCode',
  'workOrderName',
  'processCode',
  'processName',
  'feedbackQuantity',
  '完成数量'
]) {
  assert(source.includes(token), `报工成功提示必须包含字段或文案：${token}`)
}

assert(
  /message\.success\(buildFeedbackSuccessMessage\(formData\.value\)\)/.test(source),
  '提交报工成功后必须显示包含生产工单、工序和完成数量的业务摘要。'
)

assert(
  !source.includes("message.success('报工单已提交')"),
  '提交报工成功后不得继续显示笼统的“报工单已提交”。'
)

console.log('PASS: MES feedback success detail message static contract')

