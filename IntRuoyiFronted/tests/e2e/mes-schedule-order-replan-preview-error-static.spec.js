const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '..', '..')
const pagePath = path.join(root, 'src', 'views', 'mes', 'pro', 'scheduleorder', 'index.vue')
const source = fs.readFileSync(pagePath, 'utf8')

function assertContains(haystack, text, message) {
  if (!haystack.includes(text)) {
    throw new Error(message)
  }
}

const functionStart = source.indexOf('const previewReplan = async () => {')
const functionEnd = source.indexOf('const buildReplanApplyIdempotencyKey', functionStart)
if (functionStart < 0 || functionEnd < 0) {
  throw new Error('未找到手动重排预览事件处理函数')
}
const previewReplanSource = source.slice(functionStart, functionEnd)

assertContains(previewReplanSource, 'try {', '手动重排预览事件必须捕获请求异常')
assertContains(previewReplanSource, '} catch (error) {', '手动重排预览事件必须处理 Promise 拒绝')
assertContains(previewReplanSource, "console.error('[MES] 重排预览失败', error)", '预览失败必须保留控制台诊断信息')
assertContains(previewReplanSource, 'message.error(', '预览失败必须向用户显示错误原因')
assertContains(previewReplanSource, 'await previewReplanForRequest(request)', '异常处理不得跳过真实重排预览请求')

console.log('MES schedule-order replan preview error contract passed')
