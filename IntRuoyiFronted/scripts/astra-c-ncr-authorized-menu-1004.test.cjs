'use strict'
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const test = require('node:test')
const { createRequire } = require('node:module')
const { pathToFileURL } = require('node:url')
const project = path.resolve(__dirname, '..')
const req = createRequire(path.join(project, 'package.json'))
const ts = req('typescript')
let helper, remaining, menuHelper

// Run the production module. Only Vite's build-time file discovery is supplied by Node.
function compile(source, filename, globals = {}, imports = {}, transformGlob = false) {
  const transform = context => node => {
    function visit(value) {
      if (ts.isCallExpression(value) && ts.isPropertyAccessExpression(value.expression) &&
          ts.isMetaProperty(value.expression.expression) && value.expression.name.text === 'glob') {
        return ts.factory.createIdentifier('__viewModules')
      }
      return ts.visitEachChild(value, visit, context)
    }
    return ts.visitNode(node, visit)
  }
  const built = ts.transpileModule(source, {
    fileName: filename,
    reportDiagnostics: true,
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2020 },
    transformers: transformGlob ? { before: [transform] } : undefined
  })
  assert.deepEqual((built.diagnostics || []).filter(d => d.category === ts.DiagnosticCategory.Error), [], 'Source must compile before a behavior result can count')
  const module = { exports: {} }
  vm.runInNewContext(built.outputText, {
    module, exports: module.exports, ...globals,
    require: name => {
      if (!(name in imports)) throw new Error('Unprovided test module: ' + name)
      return imports[name]
    }
  }, { filename })
  return module.exports
}

test.before(async () => {
  const lodash = await import(pathToFileURL(req.resolve('lodash-es')).href)
  const is = compile(fs.readFileSync(path.join(project, 'src/utils/is.ts'), 'utf8'), 'is.ts')
  const views = {}
  function discover(directory) {
    for (const row of fs.readdirSync(directory, { withFileTypes: true })) {
      const file = path.join(directory, row.name)
      if (row.isDirectory()) discover(file)
      else if (/\.(vue|tsx)$/.test(file)) views['../views/' + path.relative(path.join(project, 'src/views'), file).replace(/\\/g, '/')] = () => file
    }
  }
  discover(path.join(project, 'src/views'))
  helper = compile(fs.readFileSync(path.join(project, 'src/utils/routerHelper.ts'), 'utf8'), 'routerHelper.ts', { __viewModules: views }, {
    'vue-router': req('vue-router'), 'lodash-es': lodash, qs: req('qs'), '@/utils/is': is
  }, true)
  remaining = compile(fs.readFileSync(path.join(project, 'src/router/modules/remaining.ts'), 'utf8'), 'remaining.ts', {
    useI18n: () => ({ t: key => key })
  }, { '@/utils/routerHelper': helper }).default
  menuHelper = compile(fs.readFileSync(path.join(project, 'src/layout/components/Menu/src/helper.ts'), 'utf8'), 'Menu/helper.ts', {
    ref: req('vue').ref, unref: req('vue').unref
  }, { '@/utils/tree': {} })
})

// Exact authorized menu shape observed in QA345's natural permission response.
const qaNcr = () => ({ id: 9008300, parentId: 5700, name: 'eDHR不合格评审',
  path: 'feedback/edhr-nonconformance-review', component: 'mes/pro/edhr-nonconformance/NonconformanceReviewPage',
  componentName: 'MesProFeedbackEdhrNonconformanceReview', icon: 'ep:warning-filled',
  visible: true, keepAlive: true, alwaysShow: true, children: null })
const qaMenus = leaf => [{ id: 5100, parentId: 0, name: 'MES 系统', path: '/mes', component: '', visible: true, keepAlive: true, alwaysShow: true,
  children: [{ id: 5700, parentId: 5100, name: '生产管理', path: 'pro', component: '', visible: true, keepAlive: true, alwaysShow: true, children: leaf ? [leaf] : [] }] }]
function entries(routes, parent = '') {
  return routes.flatMap(route => {
    const fullPath = route.path.startsWith('/') ? route.path : (parent + '/' + route.path).replace(/\/+/g, '/')
    return [{ ...route, fullPath }, ...entries(route.children || [], fullPath)]
  })
}
const ncrPath = '/mes/pro/feedback/edhr-nonconformance-review'

test('QA with an authorized visible NCR menu and no batch menu has a visible sidebar entry', () => {
  const routes = helper.generateRoute(qaMenus(qaNcr()))
  const flat = entries(routes)
  assert.equal(flat.some(row => row.fullPath === '/mes/pro/feedback/edhr-batch-execution'), false)
  const ncr = flat.find(row => row.fullPath === ncrPath)
  assert.ok(ncr && ncr.component, 'Resolve the actual NCR page component')
  assert.equal(ncr.meta.hidden, false, 'A formally visible authorized menu must remain visible')
  const production = routes[0].children[0]
  const showing = menuHelper.hasOneShowingChild(production.children, production)
  assert.equal(showing.onlyOneChild.name, 'MesProFeedbackEdhrNonconformanceReview')
  assert.equal(showing.onlyOneChild.noShowingChildren, undefined, 'QA must not see an empty production group')
})

test('explicitly hidden authorized NCR menu stays hidden with batch activeMenu', () => {
  const ncr = entries(helper.generateRoute(qaMenus({ ...qaNcr(), visible: false }))).find(row => row.fullPath === ncrPath)
  assert.equal(ncr.meta.hidden, true)
  assert.equal(ncr.meta.activeMenu, '/mes/pro/feedback/edhr-batch-execution')
})

test('static NCR deep link stays hidden, query guarded, and highlights its batch entry', () => {
  const ncr = entries(remaining).find(row => row.fullPath === ncrPath)
  assert.ok(ncr)
  assert.equal(ncr.meta.hidden, true)
  assert.equal(ncr.meta.activeMenu, '/mes/pro/feedback/edhr-batch-execution')
  assert.deepEqual(Array.from(ncr.meta.permission), ['mes:pro-edhr-nonconformance-review:query'])
})

test('QA without an authorized NCR menu does not acquire a generated NCR or batch entry', () => {
  const flat = entries(helper.generateRoute(qaMenus(null)))
  assert.equal(flat.some(row => row.fullPath === ncrPath), false)
  assert.equal(flat.some(row => row.fullPath === '/mes/pro/feedback/edhr-batch-execution'), false)
  assert.equal(helper.generateRoute([]).length, 0)
})

test('component identity preserves a visible authorized NCR alias; hidden alias remains hidden', () => {
  for (const visible of [true, false]) {
    const ncr = entries(helper.generateRoute(qaMenus({ ...qaNcr(), path: 'qa-ncr-review', visible }))).find(row => row.name === 'MesProFeedbackEdhrNonconformanceReview')
    assert.equal(ncr.meta.hidden, !visible)
    if (!visible) assert.equal(ncr.meta.activeMenu, '/mes/pro/feedback/edhr-batch-execution')
  }
})

test('unrelated visible and hidden QA regulation menus keep their original visibility', () => {
  for (const visible of [true, false]) {
    const routes = helper.generateRoute(qaMenus({ ...qaNcr(), path: '/mes/pro/process-pool/qa-regulation', component: 'mes/pro/processpool/QaRegulationPage', componentName: 'MesProProcessPoolQaRegulation', visible }))
    const row = entries(routes).find(r => r.name === 'MesProProcessPoolQaRegulation')
    assert.equal(row.meta.hidden, !visible)
    assert.equal(row.meta.activeMenu, undefined)
  }
})
