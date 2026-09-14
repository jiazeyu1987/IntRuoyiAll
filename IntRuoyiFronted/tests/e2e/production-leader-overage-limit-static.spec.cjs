const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '..', '..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const api = read('src/api/mes/pro/processpool/teamLeader.ts')
const page = read('src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue')

assert.match(api, /overagePercent\??: number \| string \| null/, '工序配置 API 必须暴露允许超量比例')
assert.match(api, /saveTeamLeaderProcessOverageLimit/, '前端必须调用生产组长超量比例保存接口')
assert.match(page, /允许超量比例/, '生产组长工序配置页必须展示允许超量比例')
assert.match(page, /saveTeamLeaderProcessOverageLimit/, '生产组长工序配置页必须支持保存允许超量比例')
assert.match(page, /超过允许上限/, '报工分配页必须明确提示超过允许上限')

console.log('PASS: production leader overage limit static contract')
