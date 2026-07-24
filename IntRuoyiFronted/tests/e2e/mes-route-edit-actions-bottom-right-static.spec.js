const fs = require('fs')
const path = require('path')
const assert = require('assert')

const root = path.resolve(__dirname, '../..')
const source = fs.readFileSync(
  path.join(root, 'src/views/mes/pro/route/RouteEditPage.vue'),
  'utf8'
)

assert.ok(
  source.includes('v-if="activeRouteTab !== \'flow\'" class="route-edit-page__actions"'),
  '流转关系图页签不得继续显示底部页面级保存操作区。'
)

assert.match(
  source,
  /<div v-if="activeRouteTab !== 'flow'" class="route-edit-page__actions">[\s\S]*?@click="contentRef\?\.submitForm\(\)"[\s\S]*?保 存[\s\S]*?<\/div>/,
  '非流转关系图页签仍必须保留页面级保存行为。'
)

assert.ok(!source.includes('返回列表'), '编辑页底部不得继续渲染“返回列表”。')
assert.ok(!source.includes('handleBack'), '编辑页删除返回按钮后不得保留返回处理函数。')

assert.match(
  source,
  /\.route-edit-page__actions\s*\{[\s\S]*?justify-content:\s*flex-end;[\s\S]*?padding:\s*16px 10px 0;[\s\S]*?\}/,
  '非流转关系图页签的页面级操作区必须右对齐，并避免保留大块底部空白。'
)

console.log('mes-route-edit-actions-bottom-right-static PASS')
