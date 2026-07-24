const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const root = process.cwd()
const routeFlowConfigPanel = fs.readFileSync(
  path.join(root, 'src/views/mes/pro/route/RouteFlowConfigPanel.vue'),
  'utf8'
)
const routeForm = fs.readFileSync(path.join(root, 'src/views/mes/pro/route/RouteFormContent.vue'), 'utf8')

assert.match(
  routeFlowConfigPanel,
  /const updatePermission = computed\(\(\) =>[\s\S]*'mes:pro-route:schedule-config:update'[\s\S]*'mes:pro-route:batch-record-config:update'/,
  '工艺流程配置启停和保存必须按当前配置页签使用 update 权限。'
)

assert.match(
  routeFlowConfigPanel,
  /const canUpdate = computed\(\(\) => checkPermi\(\[updatePermission\.value\]\)\)/,
  '工艺流程配置页签必须统一使用 canUpdate 判断更新权限。'
)

assert.match(
  routeFlowConfigPanel,
  /@click="saveConfig"/,
  '保存工艺流程配置必须走新 saveConfig 入口。'
)

assert.match(
  routeFlowConfigPanel,
  /:disabled="readonly \|\| !canUpdate"[\s\S]*@click="submitProcessFormPermission"/,
  '填写设置保存必须保持 update 权限，不随 query 权限放宽。'
)

assert.match(
  routeForm,
  /config-type="SCHEDULE"/,
  '工艺流程详情必须保留排产配置页签。'
)

assert.match(
  routeForm,
  /config-type="BATCH"/,
  '工艺流程详情必须保留批记录配置页签。'
)
