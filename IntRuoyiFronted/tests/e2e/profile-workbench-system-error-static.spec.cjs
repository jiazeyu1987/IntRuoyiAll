const fs = require('fs')
const path = require('path')
const assert = require('assert')

const repoRoot = path.resolve(__dirname, '..', '..')
const read = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const profileIndex = read('src/views/Profile/Index.vue')
const profileWorkbench = read('src/views/Profile/components/ProfileWorkbench.vue')
const badgeStore = read('src/store/modules/profileWorkbenchTodoBadge.ts')

const count = (content, pattern) => (content.match(pattern) || []).length

assert.match(
  profileIndex,
  /refreshUnreadNotifyMessageCount\(\)\s*\.catch\([^)]*reportProfileNotifyMessageError[^)]*\)/,
  '个人中心站内信未读数量必须本地捕获异常，不能在进入个人中心时冒泡成系统异常'
)

assert.ok(
  count(profileWorkbench, /ignoreErrorMessage:\s*true/g) >= 6,
  '个人工作台各待办来源请求必须关闭全局错误 toast，并由页面错误区域承接'
)

assert.ok(
  count(badgeStore, /ignoreErrorMessage:\s*true/g) >= 5,
  '个人工作台徽标统计请求必须关闭全局错误 toast，并由调用方 catch 记录'
)

assert.match(
  profileWorkbench,
  /loadErrorMessages\.value\.push\(`隐藏任务状态：/,
  '隐藏任务状态加载失败必须显示在个人工作台局部错误区域'
)

console.log('profile workbench system-error static contract passed')
