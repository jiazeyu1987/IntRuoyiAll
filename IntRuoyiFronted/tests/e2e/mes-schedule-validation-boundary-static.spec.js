const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const packageJsonPath = path.join(repoRoot, 'package.json')
const tsconfigPath = path.join(repoRoot, 'tsconfig.schedule-relaxed.json')

function readUtf8(filePath) {
  assert.ok(fs.existsSync(filePath), `missing required file: ${filePath}`)
  return fs.readFileSync(filePath, 'utf8')
}

const packageJson = JSON.parse(readUtf8(packageJsonPath))
const axiosServicePath = path.join(repoRoot, 'src/config/axios/service.ts')
const axiosService = readUtf8(axiosServicePath)
assert.equal(
  packageJson.scripts?.['ts:check:schedule'],
  'node node_modules/vue-tsc/bin/vue-tsc.js --noEmit -p tsconfig.schedule-relaxed.json',
  'package.json must expose a schedule-only ts:check entry'
)

const tsconfig = JSON.parse(readUtf8(tsconfigPath))
const include = tsconfig.include || []
const exclude = tsconfig.exclude || []

for (const requiredPattern of [
  'src/views/mes/pro/task/**/*.vue',
  'src/views/mes/pro/task/**/*.ts',
  'src/views/mes/pro/scheduleorder/**/*.vue',
  'src/views/mes/pro/scheduleorder/**/*.ts',
  'src/views/mes/pro/scheduler-workbench/**/*.vue',
  'src/views/mes/pro/scheduler-workbench/**/*.ts',
  'src/views/mes/pro/route/**/*.vue',
  'src/views/mes/pro/route/**/*.ts',
  'src/views/mes/pro/workorder/**/*.vue',
  'src/views/mes/pro/workorder/**/*.ts',
  'src/api/mes/pro/route/**/*.ts',
  'src/api/mes/pro/task/**/*.ts',
  'src/api/mes/pro/scheduleorder/**/*.ts',
  'src/api/mes/pro/scheduleCalendar/**/*.ts',
  'src/api/mes/pro/schedulerWorkbench/**/*.ts',
  'src/api/mes/pro/workorder/**/*.ts',
  'src/api/erp/production/material-list/**/*.ts'
]) {
  assert.ok(include.includes(requiredPattern), `schedule tsconfig must include ${requiredPattern}`)
}

for (const forbiddenInclude of ['src/views/mes/pro/feedback/**/*.vue', 'src/views/mes/pro/feedback/**/*.ts']) {
  assert.ok(
    !include.includes(forbiddenInclude),
    `schedule tsconfig must not include ${forbiddenInclude}`
  )
}

for (const retiredPattern of [
  ['src/views/mes/pro', `schedule${'-'}route/**/*.vue`].join('/'),
  ['src/views/mes/pro', `schedule${'-'}route/**/*.ts`].join('/')
]) {
  assert.ok(!include.includes(retiredPattern), `schedule tsconfig must not include retired entry ${retiredPattern}`)
}

for (const forbiddenPattern of [
  'src/views/mes/pro/edhr/**/*.vue',
  'src/views/mes/pro/edhr-batch/**/*.vue',
  ['src/views/mes/pro', `edhr${'-'}batch${'-'}route/**/*.vue`].join('/'),
  'src/views/mes/pro/edhr-work-task/**/*.vue'
]) {
  assert.ok(exclude.includes(forbiddenPattern), `schedule tsconfig must exclude ${forbiddenPattern}`)
}

assert.ok(
  !include.some((item) => item === 'src' || item.startsWith('src/views/mes/pro/edhr')),
  'schedule tsconfig must not fall back to global src include or pull edhr pages back in'
)

assert.ok(
  !axiosService.includes("from '@/router'") && !axiosService.includes("from './router'"),
  'shared axios service must not statically import the application router during schedule-only type checks'
)

const edhrPage = readUtf8(
  path.join(repoRoot, 'src/views/mes/pro/edhr-batch/BatchExecutionTemplateSimulatePage.vue')
)
assert.ok(
  edhrPage.includes("recordCategory: 'TEMPLATE'"),
  'static contract assumes the current unrelated eDHR type error still exists and must stay outside schedule-only checks'
)

console.log('PASS: MES schedule validation boundary static contract')
