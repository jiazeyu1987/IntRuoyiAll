import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)

const routeIndexPath = path.resolve(__dirname, '../src/views/mes/pro/route/index.vue')
const routeIndexSource = fs.readFileSync(routeIndexPath, 'utf8')

if (routeIndexSource.includes('label="resource"')) {
  throw new Error(`found obsolete resource view toggle in ${routeIndexPath}`)
}

if (routeIndexSource.includes('RouteResourceTable')) {
  throw new Error(`found obsolete RouteResourceTable usage in ${routeIndexPath}`)
}

const routeProcessListPath = path.resolve(__dirname, '../src/views/mes/pro/route/RouteProcessList.vue')
const routeProcessListSource = fs.readFileSync(routeProcessListPath, 'utf8')

for (const blockedText of ['排产用途配置', '批处理用途配置', '资源类型', '标准资源', '标准班次产能', '资源状态', '工作站']) {
  if (routeProcessListSource.includes(blockedText)) {
    throw new Error(`found obsolete "${blockedText}" in ${routeProcessListPath}`)
  }
}

if (routeProcessListSource.includes('RouteUseConfigDialog')) {
  throw new Error(`found obsolete RouteUseConfigDialog reference in ${routeProcessListPath}`)
}

const scheduleRoutePath = path.resolve(__dirname, '../src/views/mes/pro/schedule-route/index.vue')
const scheduleRouteSource = fs.readFileSync(scheduleRoutePath, 'utf8')
if (!scheduleRouteSource.includes('RouteResourceTable')) {
  throw new Error(`missing RouteResourceTable handoff in ${scheduleRoutePath}`)
}

console.log('PASS: route responsibility split static guard matches latest requirements')
