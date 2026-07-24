const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const pagePath = path.resolve(process.cwd(), 'src/views/mes/pro/scheduleorder/index.vue')
const source = fs.readFileSync(pagePath, 'utf8')
const participatingSetStart = source.indexOf('const SCHEDULE_ORDER_PARTICIPATING_STATUSES')
const participatingSetEnd = source.indexOf('])', participatingSetStart)

assert(
  participatingSetStart >= 0 && participatingSetEnd > participatingSetStart,
  '排产工单主表必须保留参与排产状态集合。'
)

const participatingSetSource = source.slice(participatingSetStart, participatingSetEnd)

assert.match(
  source,
  /label="产品编号"[\s\S]*?prop="productCode"[\s\S]*?:class="\[[\s\S]*?getAdmissionProductCodeClass\(row\)[\s\S]*?'schedule-order-pool__admission-cell-text'[\s\S]*?\]"/,
  '同步工单产品编号列必须按行状态绑定参与排产样式。'
)

assert.match(
  source,
  /const getAdmissionProductCodeClass = \(row: MesProScheduleOrderAdmissionDiffRowVO\) => \{[\s\S]*?return isAdmissionRowAdmitted\(row\)[\s\S]*?'schedule-order-pool__product-code schedule-order-pool__product-code--scheduled'[\s\S]*?: 'schedule-order-pool__product-code schedule-order-pool__product-code--unscheduled'[\s\S]*?\}/,
  '产品编号高亮必须复用 isAdmissionRowAdmitted(row) 作为参与排产口径。'
)

assert.match(
  source,
  /\.schedule-order-pool__product-code--scheduled\s*\{[\s\S]*?color:\s*#d46b08;/,
  '已参与排产的产品编号必须使用与工单编码一致的橙色。'
)

assert.match(
  source,
  /label="产品编号"[\s\S]*?prop="productCode"[\s\S]*?<template #default="\{ row \}">[\s\S]*?:class="\[[\s\S]*?getScheduleOrderProductCodeClass\(row\)[\s\S]*?'schedule-order-pool__main-table-text'[\s\S]*?\]"/,
  '排产工单主表产品编号列必须按排产状态绑定高亮样式。'
)

assert.match(
  participatingSetSource,
  /SCHEDULE_ORDER_STATUS_SCHEDULED[\s\S]*?SCHEDULE_ORDER_STATUS_IN_PROGRESS/,
  '排产工单主表参与排产口径必须覆盖已排产、生产中。'
)

assert.doesNotMatch(
  participatingSetSource,
  /SCHEDULE_ORDER_STATUS_FINISHED/,
  '排产工单主表产品编号橙色不得覆盖已完成状态。'
)

assert.match(
  source,
  /const getScheduleOrderProductCodeClass = \(row: MesProScheduleOrderVO\) => \{[\s\S]*?return isScheduleOrderParticipating\(row\)[\s\S]*?'schedule-order-pool__product-code schedule-order-pool__product-code--scheduled'[\s\S]*?: 'schedule-order-pool__product-code schedule-order-pool__product-code--unscheduled'[\s\S]*?\}/,
  '排产工单主表产品编号高亮必须复用排产状态判断。'
)

console.log('PASS: MES schedule admission product code highlight static contract')
