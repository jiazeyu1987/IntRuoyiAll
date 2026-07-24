const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')

const readPage = (relativePath) =>
  fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

for (const [pageName, relativePath] of [
  ['批记录表单列表', 'src/views/mes/pro/batchrecordformlist/index.vue'],
  ['批记录模板页', 'src/views/mes/pro/batchrecordformlist/index.vue']
]) {
  const source = readPage(relativePath)
  const routeConfirmIndex = source.indexOf('确认工艺路线升版')
  const productLineConfirmIndex = source.indexOf('确认生成路线候选版本')

  assert.notEqual(routeConfirmIndex, -1, `${pageName} 必须保留同名工艺路线升版确认。`)
  assert.notEqual(productLineConfirmIndex, -1, `${pageName} 必须保留用户勾选产线的路线候选版本确认。`)
  assert.ok(
    source.includes('collectWordImportCurrentRouteUpgradeKeys') &&
      source.includes('collectWordImportRouteProductUpgradeKeys') &&
      source.includes('confirmedRouteUpgradeKeys') &&
      source.includes('skippedRouteUpgradeKeys'),
    `${pageName} 必须按稳定路线身份记录已确认或已跳过的产线升版。`
  )
  assert.ok(
    source.includes('confirmedRouteUpgradeKeys.has(routeUpgradeKey)') &&
      source.includes('skippedRouteUpgradeKeys.has(routeUpgradeKey)'),
    `${pageName} 同一产线升版被确认或跳过后不得再次弹出产线升版确认。`
  )
  assert.ok(
    source.includes('collectWordImportCurrentRouteUpgradeKeys(wordImportDialog.preflight).forEach') &&
      routeConfirmIndex < productLineConfirmIndex,
    `${pageName} 同名工艺路线升版确认必须先写入已确认产线集合，再进入勾选产线逐项确认。`
  )
  assert.ok(
    source.includes('const shouldConfirmRouteUpgrade = Boolean(selection.routeUpgradeRequired && selection.selectedOptions.length)') &&
      source.includes('routeUpgradeConfirmed: shouldConfirmRouteUpgrade') &&
      source.includes('expectedRouteId: shouldConfirmRouteUpgrade ? selection.expectedRouteId : undefined') &&
      source.includes('if (shouldConfirmRouteUpgrade) {'),
    `${pageName} 未勾选工艺路线/产线时不得弹出工艺路线升版确认，也不得提交路线升版确认参数。`
  )
}

console.log('PASS: batch-record Word import production upgrade confirmation dedupe static contract')
