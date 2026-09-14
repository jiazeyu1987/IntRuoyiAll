const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '../..')
const source = fs.readFileSync(
  path.join(frontendRoot, 'src/views/mes/pro/batchrecordformlist/index.vue'),
  'utf8'
)

assert.match(
  source,
  /selection\.routeUpgradeRequired\s*&&\s*\(selection\.selectedOptions\.length\s*\|\|\s*rebuildBatchRecord\)/,
  '只选择批记录表单升版时，也必须确认生成路线候选版本。'
)

assert.match(
  source,
  /expectedRouteId:\s*shouldConfirmRouteUpgrade\s*\?\s*selection\.expectedRouteId\s*:\s*undefined/,
  '批记录表单升版确认后必须提交当前路线 ID，供后端写入逐工序正式批记录表单候选快照。'
)

assert.match(
  source,
  /expectedRouteVersionId:\s*shouldConfirmRouteUpgrade\s*\?\s*selection\.expectedRouteVersionId\s*:\s*undefined/,
  '批记录表单升版确认后必须提交当前路线版本 ID，避免后端把候选写到错误路线。'
)

assert.match(
  source,
  /recognizeUploadedRoute\([\s\S]*selection\.rebuildBatchRecord[\s\S]*Boolean\(selection\.routeUpgradeConfirmed\)[\s\S]*selection\.expectedRouteId[\s\S]*selection\.expectedRouteVersionId/,
  'Word 导入接口调用必须同时提交 rebuildBatchRecord、routeUpgradeConfirmed 和期望路线信息。'
)

assert.doesNotMatch(
  source,
  /formBindings[\s\S]{0,120}routeUpgradeConfirmed|routeUpgradeConfirmed[\s\S]{0,120}formBindings/,
  '批记录表单升版确认不得绑定到表单槽位 formBindings。'
)

console.log('PASS: MES batch record import formal route binding static contract')
