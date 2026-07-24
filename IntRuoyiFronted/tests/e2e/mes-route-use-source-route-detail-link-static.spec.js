const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const pagePath = path.resolve(process.cwd(), 'src/views/mes/pro/route/RouteFlowConfigPanel.vue')
const routePagePath = path.resolve(process.cwd(), 'src/views/mes/pro/route/index.vue')

assert(fs.existsSync(pagePath), `required file missing: ${pagePath}`)
assert(fs.existsSync(routePagePath), `required file missing: ${routePagePath}`)

const pageSource = fs.readFileSync(pagePath, 'utf8')
const routePageSource = fs.readFileSync(routePagePath, 'utf8')

assert.match(pageSource, /const router = useRouter\(\)/, '用途路线页必须持有 router，用于路线名称跳转。')
assert.match(pageSource, /<el-button\s+link\s+type="primary"\s+@click="openSourceRoutePage\(row\)">/, '路线名称必须跳转到基础工艺流程/工艺路线页。')
assert.match(pageSource, /const openSourceRoutePage = \(row: ProRouteVO\) => \{[\s\S]*name: 'MesProRoute'[\s\S]*code: row\.code[\s\S]*name: row\.name/, '路线名称点击必须携带路线编码和名称筛选基础工艺路线。')
assert.doesNotMatch(pageSource, /@click="openRouteDetail\(row\)"/, '路线名称不得继续打开只读详情弹窗。')
assert(!pageSource.includes('label="负责人"'), '工艺路线用途列表不得继续显示负责人列。')
assert(!pageSource.includes('label="用途"'), '工艺路线用途列表不得继续显示用途列。')
assert.match(pageSource, /@click="openUseConfig\(row\)"/, '路线编码必须继续打开用途配置。')
assert.match(routePageSource, /queryParams\.code = typeof route\.query\.code === 'string' \? route\.query\.code : undefined/, '基础工艺路线页必须接收 code 查询参数。')
assert.match(routePageSource, /queryParams\.name = typeof route\.query\.name === 'string' \? route\.query\.name : undefined/, '基础工艺路线页必须接收 name 查询参数。')

console.log('PASS: MES route flow source route jump static contract')
