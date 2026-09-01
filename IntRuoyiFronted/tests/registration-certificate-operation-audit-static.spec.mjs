import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

const root = process.cwd()
const read = (relativePath) => readFileSync(join(root, relativePath), 'utf8')

const api = read('IntRuoyiFronted/src/api/dcc/registrationCertificate/index.ts')
const detail = read('IntRuoyiFronted/src/views/dcc/registration-certificate/detail/index.vue')

const detailInterfaceStart = api.indexOf('export interface DccRegistrationCertificateDetailVO')
const detailInterfaceEnd = api.indexOf('\n}', detailInterfaceStart)
assert.ok(detailInterfaceStart >= 0 && detailInterfaceEnd > detailInterfaceStart,
  'detail API interface block must exist')
const detailInterface = api.slice(detailInterfaceStart, detailInterfaceEnd)

for (const field of [
  'uploadOperatorName',
  'uploadedAt',
  'uploadApproverName',
  'uploadApprovedAt'
]) {
  assert.match(detailInterface, new RegExp(`\\b${field}\\??:`), `detail API must expose ${field}`)
}

const historyInterfaceStart = api.indexOf('export interface DccRegistrationCertificateHistoryItemVO')
const historyInterfaceEnd = api.indexOf('\n}', historyInterfaceStart)
assert.ok(historyInterfaceStart >= 0 && historyInterfaceEnd > historyInterfaceStart,
  'history API interface block must exist')
const historyInterface = api.slice(historyInterfaceStart, historyInterfaceEnd)

for (const field of [
  'renewalOperatorName',
  'renewalOperatedAt',
  'renewalApproverName',
  'renewalApprovedAt'
]) {
  assert.match(historyInterface, new RegExp(`\\b${field}\\??:`), `history API must expose ${field}`)
}

for (const [label, expression] of [
  ['上传人', 'detail.uploadOperatorName'],
  ['上传时间', 'formatDateTimeValue(detail.uploadedAt'],
  ['上传审批人', 'detail.uploadApproverName'],
  ['上传审批时间', 'formatDateTimeValue(detail.uploadApprovedAt'],
  ['延续操作人', 'item.renewalOperatorName'],
  ['延续操作时间', 'formatDateTimeValue(item.renewalOperatedAt'],
  ['延续审批人', 'item.renewalApproverName'],
  ['延续审批时间', 'formatDateTimeValue(item.renewalApprovedAt']
]) {
  assert.match(detail, new RegExp(label), `detail page must display ${label}`)
  assert.match(
    detail,
    new RegExp(expression.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')),
    `${label} must use the formal API field`
  )
}

assert.doesNotMatch(
  detail,
  /renewalOperatorName\s*\|\||renewalApproverName\s*\|\||uploadOperatorName\s*\|\||uploadApproverName\s*\|\|/,
  'operation names must not fall back to ids or guessed labels'
)

console.log('registration certificate operation audit frontend contract: PASS')
