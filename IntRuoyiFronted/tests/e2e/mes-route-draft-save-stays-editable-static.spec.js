const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const routeEditPage = read('src/views/mes/pro/route/RouteEditPage.vue')

const handleSavedBlock = routeEditPage.match(
  /const handleSaved = async \(\) => \{[\s\S]*?\n\}/
)?.[0]

assert.ok(handleSavedBlock, '编辑页必须保留保存成功处理函数。')

assert.doesNotMatch(
  handleSavedBlock,
  /confirmSubmitRouteCandidateVersionAfterSave|submitRouteCandidateVersion\(/,
  '普通保存成功后不得隐式触发提交发布确认，否则草稿会进入审批/发布并变成不可继续编辑。'
)

assert.doesNotMatch(
  routeEditPage,
  /草稿已保存，是否立即提交发布/,
  '草稿保存成功后不得弹“立即提交发布”确认，保存与提交发布必须解耦。'
)

assert.match(
  routeEditPage,
  /const handleSubmitRouteCandidateVersion = async \(\) => \{[\s\S]*submitRouteCandidateVersion\(/,
  '显式提交发布入口必须继续调用提交发布流程。'
)

console.log('PASS: route draft save stays editable until explicit submit')
