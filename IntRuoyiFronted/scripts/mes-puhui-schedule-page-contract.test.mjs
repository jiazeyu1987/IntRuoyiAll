import assert from 'node:assert/strict'
import { existsSync, readFileSync } from 'node:fs'
import { resolve } from 'node:path'

const repoRoot = resolve(import.meta.dirname, '..')
const featureDir = resolve(repoRoot, 'src/views/mes/pro/puhui-schedule')
const pagePath = resolve(featureDir, 'index.vue')
const schedulerPath = resolve(featureDir, 'scheduler.ts')
const calendarPath = resolve(featureDir, 'components/PuhuiScheduleCalendar.vue')

assert.ok(existsSync(pagePath), 'missing puhui schedule page')
assert.ok(existsSync(schedulerPath), 'missing feature-local scheduler engine')
assert.ok(existsSync(calendarPath), 'missing feature-local calendar component')

const page = readFileSync(pagePath, 'utf8')
const scheduler = readFileSync(schedulerPath, 'utf8')
const calendar = readFileSync(calendarPath, 'utf8')
const combined = `${page}\n${scheduler}\n${calendar}`

assert.match(page, /defineOptions\(\{\s*name:\s*'MesProPuhuiSchedule'\s*\}\)/, 'Vue component name must match menu')
assert.match(page, /璞慧排产/, 'page title must be present')
for (const label of ['订单录入', '每日排产', '产能调整', '产线管理']) {
  assert.match(page, new RegExp(label), `missing tab label ${label}`)
}
for (const label of ['推进1天', '从今天重排', '保存场景', '读取场景', '重置默认', '导出已排订单']) {
  assert.match(combined, new RegExp(label), `missing action ${label}`)
}
for (const label of ['按数量排产', '按天数排产', '跳过法定节假日', '周末模式']) {
  assert.match(page, new RegExp(label), `missing planning control ${label}`)
}
assert.match(scheduler, /liteScheduler\.scenario\.v1/, 'scenario localStorage key must match AutoProduction')
assert.match(scheduler, /liteScheduler\.scenario\.snapshots\.v1/, 'snapshot localStorage key must match AutoProduction')
assert.match(page, /localStorageError/, 'page must surface localStorage errors explicitly')
assert.doesNotMatch(combined, /@\/config\/axios|@\/api\/|request\.(get|post|put|delete)|axios\./, 'feature must not call backend APIs')

console.log('mes-puhui-schedule page contract passed')

