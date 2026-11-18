const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { test } = require('node:test')
const ts = require('typescript')
const { parse, compileTemplate } = require('vue/compiler-sfc')
const filename = path.resolve(__dirname, '../../src/views/mes/pro/processpool/components/ActiveOrderSubmissionDetailPanel.vue')
const { descriptor } = parse(fs.readFileSync(filename, 'utf8'))

test('detail tabs follow the requested business order and optional tabs depend on records', () => {
  const template = descriptor.template.content.replace(/\r\n/g, '\n')
  const start = template.indexOf('        <el-tab-pane')
  const end = template.indexOf('\n      </el-tabs>', start)
  const blocks = template.slice(start, end).split(/(?=^        <el-tab-pane\b)/m).filter(Boolean)
  const openings = blocks.map(block => block.slice(0, block.indexOf('>\n') + 1))
  assert.deepEqual(openings.map(opening => /\slabel="([^"]+)"/.exec(opening)?.[1] || '文件分类'), [
    '总表', '生产表单', '过程检表单', '领料单', '补料单', '生产用料清单', '生产工单',
    '文件分类', '偏差', '返工', '生产过程损耗报告单', '补正历史', '操作记录'
  ])
  for (const [label, condition] of [
    ['补料单', "displayMode === 'full' && replenishmentMaterials.length > 0"],
    ['偏差', 'showSummaryTab && hasDeviations'], ['返工', 'detail.reworkSource'],
    ['生产过程损耗报告单', 'showSummaryTab && pqcLossReportRows.length > 0'],
    ['补正历史', 'hasCorrections']
  ]) {
    assert.ok(openings.find(opening => opening.includes(`label="${label}"`)).includes(`v-if="${condition}"`), label)
  }
  assert.deepEqual(compileTemplate({ source: template, filename, id: 'tabs' }).errors, [])
})

function harness(deviations, corrections) {
  const ast = ts.createSourceFile(filename, descriptor.scriptSetup.content, ts.ScriptTarget.Latest, true)
  const declaration = ast.statements.filter(ts.isVariableStatement)
    .flatMap(statement => [...statement.declarationList.declarations])
    .find(declaration => declaration.name.getText(ast) === 'loadConditionalTabs')
  const expression = ts.transpileModule(`const load = ${declaration.initializer.getText(ast)}`, {
    compilerOptions: { target: ts.ScriptTarget.ES2022 }
  }).outputText
  const state = { props: { detail: { activeOrderId: '100' } }, hasDeviations: { value: false },
    hasCorrections: { value: false }, conditionalTabError: { value: '' }, showSummaryTab: { value: true } }
  const load = Function(...Object.keys(state), 'getDeviationPage', 'getActiveOrderCorrections',
    `let conditionalTabGeneration = 0; ${expression}; return load`)(...Object.values(state), deviations, corrections)
  return { ...state, load }
}

test('empty results hide tabs and actual records show tabs', async () => {
  for (const present of [false, true]) {
    const state = harness(async () => ({ total: present ? 1 : 0, list: present ? [{}] : [] }),
      async () => ({ activeOrderId: '100', corrections: present ? [{}] : [] }))
    await state.load()
    assert.equal(state.hasDeviations.value, present)
    assert.equal(state.hasCorrections.value, present)
    assert.equal(state.conditionalTabError.value, '')
  }
})

test('one failed lookup reports its error while the other tab remains available', async () => {
  const state = harness(async () => { throw new Error('无偏差查询权限') },
    async () => ({ activeOrderId: '100', corrections: [{}] }))
  await state.load()
  assert.equal(state.hasDeviations.value, false)
  assert.equal(state.hasCorrections.value, true)
  assert.match(state.conditionalTabError.value, /偏差查询失败：无偏差查询权限/)
})

test('switching orders discards stale lookup responses', async () => {
  let resolveOld
  const state = harness(({ activeOrderId }) => activeOrderId === '100'
    ? new Promise(resolve => { resolveOld = resolve }) : Promise.resolve({ total: 0, list: [] }),
  async (_, id) => ({ activeOrderId: id, corrections: [] }))
  const old = state.load()
  state.props.detail = { activeOrderId: '200' }
  await state.load()
  resolveOld({ total: 1, list: [{}] })
  await old
  assert.equal(state.hasDeviations.value, false)
  assert.equal(state.hasCorrections.value, false)
})
