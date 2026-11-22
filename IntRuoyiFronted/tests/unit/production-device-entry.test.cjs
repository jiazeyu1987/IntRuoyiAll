const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const { test } = require('node:test')
const { computed, ref } = require('vue')
const { parse } = require('vue/compiler-sfc')

const source = fs.readFileSync(path.resolve(__dirname,
  '../../src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue'), 'utf8')
const { descriptor } = parse(source)
const ast = ts.createSourceFile('page.ts', descriptor.scriptSetup.content, ts.ScriptTarget.Latest, true)
const names = ['showProductionDeviceModule', 'showProductionConfigModule', 'showLegacyDailyCloseDashboardModule']
const declarations = ast.statements.filter(s => ts.isVariableStatement(s) &&
  s.declarationList.declarations.some(d => names.includes(d.name.getText(ast))))
assert.equal(declarations.length, names.length)
const code = ts.transpileModule(declarations.map(s => s.getText(ast)).join('\n') +
  `\nexports.gates = { ${names.join(',')} };`, {
  compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
}).outputText

test('模块化生产班长可进入设备维护，其他生产页签及PQC不显示，不恢复旧配置中心和日结区', () => {
  for (const leader of [true, false]) {
    for (const tab of ['devices', 'report', 'personnel', 'activeOrder', 'lossReasons']) {
      const ctx = { exports: {}, computed, isProductionLeader: ref(leader),
        showProductionModuleTabs: ref(true), activeProductionModuleTab: ref(tab) }
      vm.runInNewContext(code, ctx)
      assert.equal(ctx.exports.gates.showProductionDeviceModule.value, leader && tab === 'devices')
      assert.equal(ctx.exports.gates.showProductionConfigModule.value, false)
      assert.equal(ctx.exports.gates.showLegacyDailyCloseDashboardModule.value, false)
    }
  }
})

test('每个生产模块导航均有设备维护入口，设备区域包含真实新增操作', () => {
  const root = descriptor.template.ast
  const nodes = []
  const visit = node => { nodes.push(node); (node.children || []).forEach(visit) }
  visit(root)
  const hasAttr = (n, name) => n.props?.some(p => p.name === name)
  const navs = nodes.filter(n => n.tag === 'el-tabs' && hasAttr(n, 'data-production-leader-module-tabs'))
  assert.ok(navs.length >= 1)
  for (const nav of navs) {
    assert.ok(nav.children.some(n => n.tag === 'el-tab-pane' &&
      n.props.some(p => p.name === 'name' && p.value?.content === 'devices')))
  }
  const deviceArea = nodes.find(n => n.tag === 'ContentWrap' && hasAttr(n, 'data-team-leader-config-center'))
  assert.ok(deviceArea.props.some(p => p.name === 'if' && p.exp.content.includes('showProductionDeviceModule')))
  assert.ok(source.includes('@click="submitTeamDevice"'))
})
