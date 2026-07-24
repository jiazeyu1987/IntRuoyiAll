const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const apiPath = path.join(root, 'src/api/mes/pro/task/autoSchedule/index.ts')
const pagePath = path.join(root, 'src/views/mes/pro/scheduleorder/index.vue')

const apiSource = fs.readFileSync(apiPath, 'utf8')
const pageSource = fs.readFileSync(pagePath, 'utf8')

assert.match(
  apiSource,
  /REPLAN_REQUEST_TIMEOUT\s*=\s*(?:1[2-9]\d{4}|[2-9]\d{5,})/,
  '手动重排确认链路必须声明大于全局 30 秒的统一专用请求超时'
)

const replanApplyApiStart = apiSource.indexOf('replanApply: async')
assert.notEqual(replanApplyApiStart, -1, '应用重排 API 方法必须存在')
const replanApplyApiEnd = apiSource.indexOf('  getIssues:', replanApplyApiStart)
assert.ok(replanApplyApiEnd > replanApplyApiStart, '应用重排 API 方法范围必须可解析')
const replanApplyApiSource = apiSource.slice(replanApplyApiStart, replanApplyApiEnd)

assert.match(
  replanApplyApiSource,
  /url:\s*'\/mes\/pro\/auto-schedule\/replan\/apply'[\s\S]*timeout:\s*REPLAN_REQUEST_TIMEOUT/,
  '应用重排写入接口必须使用统一长超时，不能继承全局 30000ms'
)

const applyStart = pageSource.indexOf('const applyReplan = async () => {')
assert.notEqual(applyStart, -1, '应用重排按钮处理函数必须存在')
const applyEnd = pageSource.indexOf('\nconst openDailyCompareDialog', applyStart)
assert.ok(applyEnd > applyStart, '应用重排按钮处理函数范围必须可解析')
const applySource = pageSource.slice(applyStart, applyEnd)

assert.match(applySource, /catch\s*\(\s*error\s*\)/, '应用重排必须显式处理请求错误，避免 AxiosError 冒泡为未处理组件事件异常')
assert.match(applySource, /message\.error/, '应用重排失败必须给用户明确错误提示')
assert.match(applySource, /console\.error/, '应用重排失败必须记录真实错误，不能静默吞异常')
assert.doesNotMatch(applySource, /throw\s+error/, '应用重排失败已在当前事件内处理，不能继续抛出导致 Vue 记录未处理组件事件异常')
assert.doesNotMatch(applySource, /catch\s*\(\s*\)\s*\{\s*\}/, '应用重排不能使用空 catch 吞异常')
assert.doesNotMatch(applySource, /fallback|mock|placeholder/i, '应用重排超时修复不能引入 fallback、mock 或 placeholder 逻辑')

console.log('PASS: MES schedule order replan apply timeout and error handling contract')
