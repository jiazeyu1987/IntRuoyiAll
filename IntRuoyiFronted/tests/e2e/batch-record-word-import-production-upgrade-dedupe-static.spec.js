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
  const routeConfirmIndex = source.indexOf('resolveWordImportRouteUpgradeDialogTitle')
  const productLineConfirmIndex = source.indexOf('确认生成路线候选版本')

  assert.notEqual(routeConfirmIndex, -1, `${pageName} 必须保留同名工艺路线升版确认。`)
  assert.notEqual(productLineConfirmIndex, -1, `${pageName} 必须保留用户勾选产线的路线候选版本确认。`)
  assert.ok(
    source.includes("'确认更新路线草稿'") &&
      source.includes("'确认生成路线候选版本'"),
    `${pageName} 必须分别提供更新现有草稿和新建候选版本的确认语义。`
  )
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
    source.includes('const routeFlowRebuildRequested = selection.selectedOptions.length > 0') &&
      source.includes('const batchRecordBindingCandidateRequested = Boolean(') &&
      source.includes('selection.routeUpgradeRequired && rebuildBatchRecord && !routeFlowRebuildRequested') &&
      /const shouldConfirmRouteUpgrade = Boolean\([\s\S]*selection\.routeUpgradeRequired[\s\S]*routeFlowRebuildRequested \|\| batchRecordBindingCandidateRequested[\s\S]*\)/.test(source) &&
      source.includes('routeUpgradeConfirmed: shouldConfirmRouteUpgrade') &&
      source.includes('expectedRouteId: shouldConfirmRouteUpgrade ? selection.expectedRouteId : undefined') &&
      source.includes('if (shouldConfirmRouteUpgrade) {'),
    `${pageName} 必须区分工艺流程重建候选和仅批记录表单绑定候选；未勾选工艺流程时不得把批记录表单勾选值当作 flowGraph 重建触发条件。`
  )
}

console.log('PASS: batch-record Word import production upgrade confirmation dedupe static contract')
