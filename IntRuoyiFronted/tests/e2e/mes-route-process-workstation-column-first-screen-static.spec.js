const fs = require('fs')
const path = require('path')
const assert = require('assert')

const root = path.resolve(__dirname, '..', '..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const sharedColumns = read('src/views/mes/pro/route/routeProcessSettingsColumns.ts')
const processList = read('src/views/mes/pro/route/RouteProcessList.vue')

function assertAppearsBefore(source, first, second, label) {
  const firstIndex = source.indexOf(first)
  const secondIndex = source.indexOf(second)
  assert.ok(firstIndex >= 0, `${label}: missing first marker ${JSON.stringify(first)}`)
  assert.ok(secondIndex >= 0, `${label}: missing second marker ${JSON.stringify(second)}`)
  assert.ok(
    firstIndex < secondIndex,
    `${label}: ${JSON.stringify(first)} must appear before ${JSON.stringify(second)}`
  )
}

assertAppearsBefore(
  sharedColumns,
  "key: 'processName'",
  "key: 'workstation'",
  '工作站默认列必须紧跟工序基础字段，避免宽表首屏不可见'
)
assertAppearsBefore(
  sharedColumns,
  "key: 'workstation'",
  "key: 'capacitySource'",
  '工作站默认列必须在资源和排产字段之前'
)
assert.ok(
  /key:\s*'workstation'[\s\S]{0,80}hideable:\s*false/.test(sharedColumns),
  '工作站列必须是核心绑定列，不能被历史显示字段配置隐藏'
)
assertAppearsBefore(
  processList,
  `v-if="isRouteProcessSettingColumnVisible('processName')"`,
  `v-if="isRouteProcessSettingColumnVisible('workstation')"`,
  '工作站表格列必须靠近工序名称'
)
assertAppearsBefore(
  processList,
  `v-if="isRouteProcessSettingColumnVisible('workstation')"`,
  `v-if="isRouteProcessSettingColumnVisible('capacitySource')"`,
  '工作站表格列必须在资源字段之前'
)
assert.ok(
  /v-if="isRouteProcessSettingColumnVisible\('workstation'\)"[\s\S]{0,260}fixed="left"/.test(
    processList
  ),
  '工作站列必须固定在左侧，首屏可见'
)

console.log('mes-route-process-workstation-column-first-screen-static PASS')
