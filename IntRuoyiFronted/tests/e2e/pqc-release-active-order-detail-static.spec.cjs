const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const { parse } = require('vue/compiler-sfc')

const attr = (node, name) => node.props.find(prop => prop.type === 6 && prop.name === name)
const directive = (node, name, argument) => node.props.find(prop => prop.type === 7 && prop.name === name && (!argument || prop.arg?.content === argument))
function elements(node, predicate, parents = []) {
  const matches = node.type === 1 && predicate(node) ? [{ node, parents }] : []
  for (const child of node.children || []) matches.push(...elements(child, predicate, [...parents, node]))
  return matches
}
function runDeclaration(page, name, context) {
  const source = parse(page).descriptor.scriptSetup.content
  const ast = ts.createSourceFile('actual.ts', source, ts.ScriptTarget.Latest, true)
  const statement = ast.statements.find(node => ts.isVariableStatement(node) && node.declarationList.declarations.some(d => ts.isIdentifier(d.name) && d.name.text === name))
  assert.ok(statement, `真实页面必须保留 ${name}`)
  vm.runInNewContext(ts.transpileModule(statement.getText(ast) + `\nexports.action=${name}`, { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText, context)
  return context.exports.action
}
const source = fs.readFileSync(path.resolve(__dirname, '../../src/views/mes/pro/production-release/PqcProductionReleasePage.vue'), 'utf8')
const productionLeaderDetail = fs.readFileSync(
  path.resolve(__dirname, '../../src/views/mes/pro/processpool/ActiveOrderSubmissionDetailPage.vue'),
  'utf8'
)
const batchExecutionDetail = fs.readFileSync(
  path.resolve(__dirname, '../../src/views/mes/pro/edhr-batch/BatchExecutionActiveOrderDetailPage.vue'),
  'utf8'
)
const batchVoidedDetail = fs.readFileSync(
  path.resolve(__dirname, '../../src/views/mes/pro/edhr-batch/BatchVoidedPage.vue'),
  'utf8'
)
const nonconformanceDetail = fs.readFileSync(
  path.resolve(__dirname, '../../src/views/mes/pro/edhr-nonconformance/NonconformanceReviewPage.vue'),
  'utf8'
)
const marker = source.indexOf('data-pqc-production-release-detail')
assert.ok(marker >= 0, 'PQC release needs a detail action')
assert.ok(marker < source.indexOf('data-pqc-production-release-approve'), 'Detail precedes release')
assert.match(source, /@click="openActiveOrderDetail\(row\)"/)
assert.match(source, /:disabled="!hasActiveOrderDetail\(row\)"/)
const start = source.indexOf('const openActiveOrderDetail =')
const end = source.indexOf('\nconst ', start + 1)
assert.ok(start >= 0 && end > start)
const handler = source.slice(start, end)
assert.match(handler, /loadActiveOrderDetail\(row.applicationId, row\)/)
assert.match(handler, /navigatePqcReleaseSelection\(row, 'detail'\)/)
const loaderStart = source.indexOf('const loadActiveOrderDetail =')
const loader = source.slice(loaderStart, source.indexOf('\nconst ', loaderStart + 1))
assert.match(loader, /getPqcProductionReleaseOrderDetail\(applicationId, /)
assert.doesNotMatch(loader, /workOrderId|getTeamLeaderActiveOrderDetail|\|\|/)
assert.match(source, /loadActiveOrderDetail\(context.applicationId\)/)
assert.doesNotMatch(handler, /router\.push|router\.replace|getTeamLeaderActiveOrderDetail/)
assert.match(source, /data-pqc-production-release-detail-back/)
assert.match(source, /ActiveOrderSubmissionDetailPanel/)

const detailPages = [
  ['生产组长', productionLeaderDetail, 'data-team-leader-active-order-detail-back', 'summary', 'goBack'],
  ['批次执行/历史追溯', batchExecutionDetail, 'data-edhr-batch-source-detail-back', 'icon', 'goBack'],
  ['PQC生产放行', source, 'data-pqc-production-release-detail-back', 'direct', 'detailVisible = false'],
  ['批次执行作废', batchVoidedDetail, 'data-edhr-batch-voided-detail-back', 'direct', 'detailVisible = false'],
  ['不合格评审', nonconformanceDetail, 'data-edhr-ncr-active-order-detail-back', 'direct', 'closeOrderDetail']
]

for (const [name, page, backMarker, placement, click] of detailPages) {
  const matches = elements(parse(page).descriptor.template.ast, node => node.tag === 'el-button' && attr(node, backMarker))
  assert.equal(matches.length, 1, `${name} 详情必须有唯一返回按钮`)
  const { node: button, parents } = matches[0]
  assert.ok(parents.some(node => node.tag === 'ActiveOrderDetailLayout'), `${name} 返回按钮必须位于统一详情布局内`)
  assert.equal(directive(button, 'on', 'click')?.exp?.content, click, `${name} 返回动作必须绑定正确处理器`)
  if (placement === 'summary') {
    assert.equal(parents.at(-1).tag, 'template')
    assert.equal(directive(parents.at(-1), 'slot')?.arg?.content, 'summary-actions')
    assert.equal(parents.at(-2).tag, 'ActiveOrderSubmissionDetailPanel')
    assert.equal(parents.at(-3).tag, 'ActiveOrderDetailLayout')
  } else if (placement === 'icon') {
    assert.equal(parents.at(-1).tag, 'el-tooltip')
    assert.equal(attr(parents.at(-1), 'content')?.value?.content, '返回')
    assert.equal(attr(parents.at(-2), 'class')?.value?.content, 'edhr-batch-active-order-detail')
    assert.equal(parents.at(-3).tag, 'ActiveOrderDetailLayout')
    assert.equal(attr(button, 'aria-label')?.value?.content, '返回')
    assert.ok(attr(button, 'circle'))
    assert.equal(attr(button, 'class')?.value?.content, 'edhr-batch-active-order-detail__back')
    assert.equal(elements(button, node => node.tag === 'Icon' && attr(node, 'icon')?.value?.content === 'ep:back').length, 1)
  } else {
    assert.equal(parents.at(-1).tag, 'ActiveOrderDetailLayout', `${name} 返回按钮必须直接参与统一Grid布局`)
  }
  if (placement !== 'icon') {
    assert.ok(button.children.some(node => node.type === 2 && node.content.trim() === '返回'))
    assert.equal(elements(button, node => node.tag === 'Icon').length, 0)
    assert.ok(!attr(button, 'icon') && !directive(button, 'bind', 'icon'))
    assert.doesNotMatch(button.loc.source.slice(0, button.loc.source.indexOf('>')), /\s(?:type|size|class|style|link|text|plain|round|circle)(?:\s|=|$)/, `${name} 返回按钮保留默认呈现`)
  }
}

const backCalls = []
const leaderContext = { exports: {}, inProcessReadOnly: { value: false }, router: { back: () => backCalls.push('back'), push: path => backCalls.push(path) } }
const leaderBack = runDeclaration(productionLeaderDetail, 'goBack', leaderContext)
leaderBack()
leaderContext.inProcessReadOnly.value = true
leaderBack()
assert.deepEqual(backCalls, ['back', '/mes/pro/in-process-order'])
assert.match(batchExecutionDetail, /router\.push\(\{ path: resolveReturnPath\(\) \}\)/)
const closeContext = { exports: {}, orderDetailSequence: 7 }
for (const key of ['orderDetailVisible', 'orderDetailLoading', 'orderDetail', 'orderDetailReview', 'orderDetailSummary', 'orderDetailError']) closeContext[key] = { value: true }
runDeclaration(nonconformanceDetail, 'closeOrderDetail', closeContext)()
assert.equal(closeContext.orderDetailSequence, 8, '返回时使正在读取的旧详情失效')
assert.equal(closeContext.orderDetailVisible.value, false)
assert.equal(closeContext.orderDetailLoading.value, false)
for (const key of ['orderDetail', 'orderDetailReview', 'orderDetailSummary']) assert.equal(closeContext[key].value, undefined)
assert.equal(closeContext.orderDetailError.value, '')

const detailPanel = fs.readFileSync(path.resolve(__dirname, '../../src/views/mes/pro/processpool/components/ActiveOrderSubmissionDetailPanel.vue'), 'utf8')
const summarySlots = elements(parse(detailPanel).descriptor.template.ast, node => node.tag === 'slot' && attr(node, 'name')?.value?.content === 'summary-actions')
assert.equal(summarySlots.length, 2, '摘要动作在正式详情与未加载/错误状态均有出口')
for (const { parents } of summarySlots) assert.equal(attr(parents.at(-1), 'class')?.value?.content, 'active-order-detail-actions')
assert.ok(summarySlots.some(({ parents }) => directive(parents.at(-1), 'if')?.exp?.content === "(!detail || error) && $slots['summary-actions']"))
assert.ok(summarySlots.some(({ parents }) => attr(parents.at(-2), 'class')?.value?.content === 'team-leader-workbench__active-order-detail-summary'))
assert.match(detailPanel, /\.team-leader-workbench__active-order-detail-summary\.has-summary-actions\s*\{[^}]*grid-template-columns:[^;]*\bauto\s*;/)
assert.match(detailPanel, /\.team-leader-workbench__active-order-detail-summary > \.active-order-detail-actions\s*\{\s*display:\s*flex/)
assert.match(batchExecutionDetail, /\.edhr-batch-active-order-detail\s*\{[^}]*position:\s*relative/)
assert.match(batchExecutionDetail, /\.edhr-batch-active-order-detail__back\s*\{[^}]*position:\s*absolute;[^}]*right:\s*0;/)
assert.match(batchExecutionDetail, /\.edhr-batch-active-order-detail :deep\(\.team-leader-workbench__active-order-detail-summary\)\s*\{\s*margin-right:\s*44px;/)

const sharedLayout = fs.readFileSync(
  path.resolve(__dirname, '../../src/views/mes/pro/processpool/components/ActiveOrderDetailLayout.vue'),
  'utf8'
)
assert.match(sharedLayout, /class="active-order-detail-layout"/)
assert.match(sharedLayout, /display:\s*grid/)
assert.match(sharedLayout, /gap:\s*16px/)
assert.match(sharedLayout, /<slot\s*(?:\/>|><\/slot>)/)

console.log('PASS: active-order detail return actions preserve their current layout, navigation and cleanup contracts')
