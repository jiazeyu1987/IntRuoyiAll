const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '../..')
const source = fs.readFileSync(
  path.join(frontendRoot, 'src/views/mes/pro/batchrecordformlist/index.vue'),
  'utf8'
)

const extractBlock = (startMarker, endMarker) => {
  const start = source.indexOf(startMarker)
  const end = source.indexOf(endMarker, start + startMarker.length)
  assert.notEqual(start, -1, `missing block start: ${startMarker}`)
  assert.notEqual(end, -1, `missing block end: ${endMarker}`)
  return source.slice(start, end)
}

const buildConfirmedSelectionBlock = extractBlock(
  'const buildWordImportConfirmedSelection =',
  'const confirmWordImportUpgradeSelections ='
)
const confirmUpgradeBlock = extractBlock(
  'const confirmWordImportUpgradeSelections =',
  'const openWordImportDialog ='
)

assert.match(
  buildConfirmedSelectionBlock,
  /selection\.routeUpgradeRequired[\s\S]*selection\.rebuildBatchRecord\s*\|\|\s*selection\.selectedOptions\.length[\s\S]*selection\.expectedRouteId[\s\S]*selection\.expectedRouteVersionId/,
  '只升版批记录表单且存在当前路线时，也必须明确确认生成正式绑定候选版本。'
)

assert.match(
  buildConfirmedSelectionBlock,
  /routeUpgradeConfirmed:\s*shouldConfirmRouteUpgrade[\s\S]*expectedRouteId:\s*shouldConfirmRouteUpgrade\s*\?\s*selection\.expectedRouteId[\s\S]*expectedRouteVersionId:\s*shouldConfirmRouteUpgrade\s*\?\s*selection\.expectedRouteVersionId/,
  '批记录表单正式绑定候选必须提交当前路线和激活版本标识。'
)

assert.match(
  confirmUpgradeBlock,
  /resolveWordImportRouteUpgradeMessage\([\s\S]*rebuildBatchRecord[\s\S]*\)/,
  '导入确认必须把是否更新批记录表单绑定传入路线候选提示。'
)

assert.match(
  source,
  /确认后将生成路线候选版本\$\{batchRecordBindingMessage\}，待审批\/发布后生效/,
  '批记录单独升版必须向用户说明正式逐工序绑定通过路线候选发布。'
)

console.log('PASS: batch record import formal route binding contract')
