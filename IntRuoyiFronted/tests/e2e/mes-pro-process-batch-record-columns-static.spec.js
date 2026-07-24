const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const pageSource = fs.readFileSync(path.join(root, 'src/views/mes/pro/process/index.vue'), 'utf8')
const apiSource = fs.readFileSync(path.join(root, 'src/api/mes/pro/process/index.ts'), 'utf8')

const expectedColumns = [['batchRecordFormNames', '批记录表单']]

for (const [field, label] of expectedColumns) {
  assert.match(pageSource, new RegExp(`key:\\s*'${field}'`), `default column config must include ${field}`)
  assert.match(pageSource, new RegExp(`label="${label}"[\\s\\S]*prop="${field}"`), `table must render ${label}`)
  assert.match(apiSource, new RegExp(`${field}\\?:\\s*string`), `process API model must expose ${field}`)
}

assert.match(pageSource, /scope\.row\.batchRecordForms\?\.length/, 'page must render batch record links from structured batchRecordForms')
assert.match(pageSource, /openBatchRecordForm/, 'batch record form values must stay clickable')
assert.match(pageSource, /未配置/, 'empty batch record cells must show 未配置')
assert.doesNotMatch(pageSource, /mock|fallback/i, 'page must not add mock or fallback logic')

for (const legacyToken of [
  'productionFillerNames',
  'qualityFillerNames',
  'equipmentFillerNames',
  'productionFillers',
  'qualityFillers',
  'equipmentFillers',
  '生产填写人',
  '质量填写人',
  '设备填写人',
  'openFillerTarget'
]) {
  assert.ok(!pageSource.includes(legacyToken), `工序设置列表必须移除旧填写人列或跳转逻辑：${legacyToken}`)
  assert.ok(!apiSource.includes(legacyToken), `工序 API 类型必须移除旧填写人字段：${legacyToken}`)
}

console.log('PASS: mes pro process batch record column-only static contract')
