const fs = require('fs')
const path = require('path')
const assert = require('assert')

const root = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const routePage = read('src/views/mes/pro/route/index.vue')
const routeApi = read('src/api/mes/pro/route/index.ts')

assert.match(routeApi, /flowGraphConfigured\?: boolean/, 'ProRouteVO 必须声明 flowGraphConfigured 字段。')
assert.match(routePage, /<el-table-column[\s\S]*label="关系图"[\s\S]*prop="flowGraphConfigured"[\s\S]*:?width="getRouteColumnWidthString\('flowGraphConfigured', 100\)"/, '工艺路线列表必须新增“关系图”列。')
assert.match(routePage, /scope\.row\.flowGraphConfigured\s*\?\s*'已设'\s*:\s*'未设'/, '关系图列必须按 flowGraphConfigured 显示已设/未设。')

const statusColumnIndex = routePage.indexOf('label="状态"')
const graphColumnIndex = routePage.indexOf('label="关系图"')
const productColumnIndex = routePage.indexOf('label="关联产品"')

assert(statusColumnIndex >= 0, '列表必须保留状态列。')
assert(graphColumnIndex > statusColumnIndex, '关系图列必须位于状态列之后。')
assert(productColumnIndex > graphColumnIndex, '关系图列必须位于关联产品列之前。')

console.log('mes-route-flow-graph-status-column-static PASS')
