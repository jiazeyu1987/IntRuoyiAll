const fs = require('fs')
const path = require('path')
const assert = require('assert')

const repoRoot = path.resolve(__dirname, '..', '..')
const read = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const profileIndex = read('src/views/Profile/Index.vue')
const profileWorkbench = read('src/views/Profile/components/ProfileWorkbench.vue')
const badgeStore = read('src/store/modules/profileWorkbenchTodoBadge.ts')

assert.match(
  profileIndex,
  /refreshUnreadNotifyMessageCount\(\)\s*\.catch\([^)]*reportProfileNotifyMessageError[^)]*\)/,
  '个人中心站内信未读数量必须本地捕获异常，不能在进入个人中心时冒泡成系统异常'
)


const queryApi = read('src/api/system/profileWorkbenchTodo/index.ts')
assert.equal((queryApi.match(/ignoreErrorMessage: true/g) || []).length, 4)
assert.match(profileWorkbench, /catch \(error\)[\s\S]*tryCommitPage\(token,[\s\S]*loadErrorMessages\.value =/)
assert.match(profileWorkbench, /total\.value = undefined/)
assert.match(profileWorkbench, /total === undefined \? '待办列表尚未加载成功，请重试'/)
assert.match(badgeStore, /catch \(error\)[\s\S]*this\.tryCommit\(token,[\s\S]*throw error/)
assert.match(profileWorkbench, /任务已恢复，但列表刷新失败|任务已隐藏，但列表刷新失败/)
console.log('PASS: profile workbench local query-error static contract')
