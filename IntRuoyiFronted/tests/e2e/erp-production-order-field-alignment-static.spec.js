const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const workOrderApiPath = path.resolve(process.cwd(), 'src/api/mes/pro/workorder/index.ts')
const workOrderPagePath = path.resolve(process.cwd(), 'src/views/mes/pro/workorder/index.vue')
const workOrderFormPath = path.resolve(process.cwd(), 'src/views/mes/pro/workorder/WorkOrderForm.vue')

const workOrderApiSource = fs.readFileSync(workOrderApiPath, 'utf8')
const workOrderPageSource = fs.readFileSync(workOrderPagePath, 'utf8')
const workOrderFormSource = fs.readFileSync(workOrderFormPath, 'utf8')

for (const token of [
  'workshopName',
  'bomVersion',
  'pickMode',
  'auxiliaryCode',
  'businessStatus',
  'drawingNumber',
  'scheduleStatus'
]) {
  assert(
    workOrderApiSource.includes(token),
    `Production work order API VO must expose ${token}.`
  )
}

for (const token of [
  '生产车间',
  'BOM版本',
  '冲领料',
  '备注1助记码',
  '业务状态',
  '图号',
  '排产状态'
]) {
  assert(
    workOrderPageSource.includes(token),
    `Production work order page must render ${token}.`
  )
}

for (const token of [
  '生产车间',
  'BOM版本',
  '冲领料',
  '备注1助记码',
  '业务状态',
  '图号',
  '排产状态'
]) {
  assert(
    workOrderFormSource.includes(token),
    `Production work order detail form must render ${token}.`
  )
}

assert(
  !/catch\s*\{\s*\}/.test(workOrderPageSource),
  'Production work order page must not silently swallow failures.'
)

console.log('PASS: ERP production order field alignment static contract')
