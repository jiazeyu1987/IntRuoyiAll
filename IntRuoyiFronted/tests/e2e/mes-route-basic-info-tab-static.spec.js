const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const root = path.resolve(__dirname, '../..')
const read = file => fs.readFileSync(path.join(root, file), 'utf8')
const content = read('src/views/mes/pro/route/RouteFormContent.vue')
const graph = read('src/views/mes/pro/route/RouteFlowGraphDesigner.vue')
const index = read('src/views/mes/pro/route/index.vue')

assert.ok(!content.includes('<el-tabs'), 'route editor must open the graph without an inner tab bar')
assert.ok(index.includes(`@click="openEditPage(scope.row.id, 'flow')"`), 'list route code opens the graph page')
assert.ok(content.includes('<template #start-setting="{ setting }">'), 'route settings render within the selected START detail')
const basic = content.slice(content.indexOf('data-route-start-setting="basic"'), content.indexOf('data-route-start-setting="product"'))
for (const label of ['编码', '名称']) {
  assert.ok(basic.includes(`label="${label}"`), `START basic settings preserve ${label}`)
}
for (const label of ['负责人', '备注']) {
  assert.ok(!basic.includes(`label="${label}"`), `START basic settings omit ${label}`)
}
for (const [key, label] of [['basic', '基础信息'], ['product', '关联产品'], ['dcc', '关联项目']]) {
  assert.ok(graph.includes(`{ key: '${key}', label: '${label}' }`), `START exposes ${label}`)
  assert.ok(content.includes(`data-route-start-setting="${key}"`), `${label} detail remains connected`)
}
for (const key of ['batchRecordAttachment', 'productionLeader', 'releaseOwner']) {
  assert.ok(graph.includes(`handleSelectBoundaryDetailField('${key}')`), `existing ${key} remains available`)
}
assert.ok(content.includes('ProRouteApi.updateRoute(data)'), 'basic settings preserve the existing persistence API')
assert.ok(content.includes(':route-version-edit-context="routeVersionEditContext"'), 'product settings retain candidate version context')
console.log('mes-route-basic-info-tab-static PASS')
