const fs = require('fs')
const path = require('path')
const assert = require('assert')

const repoRoot = path.resolve(__dirname, '..', '..')
const pagePath = path.join(repoRoot, 'src/views/mes/pro/scheduler-workbench/index.vue')
const apiPath = path.join(repoRoot, 'src/api/mes/pro/schedulerWorkbench/index.ts')

const pageSource = fs.readFileSync(pagePath, 'utf8')
const apiSource = fs.readFileSync(apiPath, 'utf8')

assert.match(pageSource, /导出排产工艺路线/, '工作台必须提供“导出排产工艺路线”按钮。')
assert.match(pageSource, /导入排产工艺路线/, '工作台必须提供“导入排产工艺路线”按钮。')
assert.match(pageSource, /导出全部数据包/, '工作台必须提供“导出全部数据包”按钮。')
assert.match(pageSource, /导入全部数据包/, '工作台必须提供“导入全部数据包”按钮。')
assert.match(pageSource, /routeConfigInputRef/, '工作台必须保留隐藏文件选择器引用。')
assert.match(pageSource, /handleRouteConfigFileChange/, '工作台必须处理配置包文件上传事件。')
assert.match(pageSource, /fullConfigInputRef/, '工作台必须保留全量数据包隐藏文件选择器引用。')
assert.match(pageSource, /handleFullConfigFileChange/, '工作台必须处理全量数据包文件上传事件。')
assert.match(pageSource, /accept="\.json,application\/json"/, '工作台导入必须限制为 JSON 配置包。')

assert.match(
  apiSource,
  /url: '\/mes\/pro\/scheduler-workbench\/route-config\/export'/,
  '工作台 API 必须提供排产工艺路线配置包导出接口。'
)
assert.match(
  apiSource,
  /url: '\/mes\/pro\/scheduler-workbench\/route-config\/import'/,
  '工作台 API 必须提供排产工艺路线配置包导入接口。'
)
assert.match(
  apiSource,
  /SchedulerWorkbenchRouteConfigImportRespVO/,
  '工作台 API 必须声明配置包导入返回合同。'
)
assert.match(
  apiSource,
  /url: '\/mes\/pro\/scheduler-workbench\/full-config\/export'/,
  '工作台 API 必须提供全部数据包导出接口。'
)
assert.match(
  apiSource,
  /url: '\/mes\/pro\/scheduler-workbench\/full-config\/import'/,
  '工作台 API 必须提供全部数据包导入接口。'
)
assert.match(
  apiSource,
  /SchedulerWorkbenchFullConfigImportRespVO/,
  '工作台 API 必须声明全部数据包导入返回合同。'
)

console.log('mes-pro-scheduler-workbench-route-import-export-static: PASS')
