const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = process.cwd()
const workspaceRoot = path.resolve(frontendRoot, '..')
const readFrontend = (relativePath) =>
  fs.readFileSync(path.join(frontendRoot, relativePath), 'utf8')
const readWorkspace = (relativePath) =>
  fs.readFileSync(path.join(workspaceRoot, relativePath), 'utf8')

const page = readFrontend('src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue')
const api = readFrontend('src/api/mes/pro/processpool/teamLeader.ts')
const responseVo = readWorkspace(
  'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/team/vo/MesTeamLeaderActiveOrderRespVO.java'
)
const controller = readWorkspace(
  'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/team/MesProcessPoolTeamLeaderController.java'
)

const listStart = page.indexOf('data-team-leader-active-order-list')
assert.notEqual(listStart, -1, 'active order table must exist')
const listEnd = page.indexOf('</el-table>', listStart)
assert.notEqual(listEnd, -1, 'active order table must close')
const activeOrderTable = page.slice(listStart, listEnd)

assert.match(
  api,
  /export interface TeamLeaderActiveOrderRespVO \{[\s\S]*routeName: string[\s\S]*routeVersionNo: string/,
  'frontend active-order contract must expose formal route name and version number'
)
assert.match(api, /productionProgressPercent: number \| string/, 'frontend contract must expose production progress percent')
assert.match(api, /inspectionProgressPercent: number \| string/, 'frontend contract must expose inspection progress percent')
assert.match(responseVo, /private String routeName;/, 'backend response must expose routeName')
assert.match(responseVo, /private String routeVersionNo;/, 'backend response must expose routeVersionNo')
assert.match(responseVo, /private BigDecimal productionProgressPercent;/, 'backend response must expose production progress')
assert.match(responseVo, /private BigDecimal inspectionProgressPercent;/, 'backend response must expose inspection progress')
assert.match(
  controller,
  /setRouteName\(activeOrder\.getRouteName\(\)\)[\s\S]*setRouteVersionNo\(activeOrder\.getRouteVersionNo\(\)\)/,
  'controller must project the formal route display fields'
)
assert.match(
  controller,
  /setProductionProgressPercent\(activeOrder\.getProductionProgressPercent\(\)\)[\s\S]*setInspectionProgressPercent\(activeOrder\.getInspectionProgressPercent\(\)\)/,
  'controller must project active-order progress fields'
)

assert.match(activeOrderTable, /label="路线名称"\s+prop="routeName"/)
assert.match(activeOrderTable, /label="版本号"\s+prop="routeVersionNo"/)
assert.match(activeOrderTable, /label="生产进度"\s+prop="productionProgressPercent"/)
assert.match(activeOrderTable, /label="检验进度"\s+prop="inspectionProgressPercent"/)
assert.match(activeOrderTable, /data-team-leader-active-order-production-progress/)
assert.match(activeOrderTable, /data-team-leader-active-order-inspection-progress/)
assert.doesNotMatch(activeOrderTable, /label="路线ID"|label="路线版本ID"|label="状态"/)
assert.doesNotMatch(activeOrderTable, /prop="routeId"|prop="routeVersionId"|activeStatus/)

const columnsStart = page.indexOf('const activeOrderColumns')
const columnsEnd = page.indexOf('\n]', columnsStart)
assert.notEqual(columnsStart, -1, 'active order column metadata must exist')
assert.notEqual(columnsEnd, -1, 'active order column metadata must close')
const activeOrderColumns = page.slice(columnsStart, columnsEnd)
assert.match(activeOrderColumns, /key: 'routeName', label: '路线名称'/)
assert.match(activeOrderColumns, /key: 'routeVersionNo', label: '版本号'/)
assert.match(activeOrderColumns, /key: 'productionProgressPercent', label: '生产进度'/)
assert.match(activeOrderColumns, /key: 'inspectionProgressPercent', label: '检验进度'/)
assert.doesNotMatch(activeOrderColumns, /key: 'routeId'|key: 'routeVersionId'|key: 'activeStatus'/)

console.log('PASS: production leader active-order route labels and progress contract')
