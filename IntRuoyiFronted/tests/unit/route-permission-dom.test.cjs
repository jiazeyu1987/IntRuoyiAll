const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { test } = require('node:test')
const ts = require('typescript')
const { chromium } = require('playwright')

const root = path.resolve(__dirname, '../..')
const read = relative => fs.readFileSync(path.join(root, relative), 'utf8')

test('directory switching keeps real Vue/Element Plus DOM consistent for every button permission combination', async () => {
  const source = read('src/views/mes/pro/route/RouteTreePanel.vue')
  const template = '<div>' + source.match(/<div class="route-tree__sidebar-actions"[\s\S]*?<\/div>/)[0] +
    source.match(/<div class="route-tree__actions">[\s\S]*?<\/div>/)[0] + '</div>'
  const directiveAst = ts.createSourceFile('hasPermi.ts', read('src/directives/permission/hasPermi.ts'), ts.ScriptTarget.Latest, true)
  const directive = directiveAst.statements.find(statement => ts.isFunctionDeclaration(statement) && statement.name.text === 'hasPermi')
  const directiveCode = ts.transpileModule(directive.getText(directiveAst).replace('export ', ''), {}).outputText
  const browser = await chromium.launch({ headless: true })
  try {
    const page = await browser.newPage()
    const pageErrors = []
    page.on('pageerror', error => pageErrors.push(error.message))
    await page.setContent('<div id="fixtures"></div>')
    await page.addScriptTag({ path: require.resolve('vue/dist/vue.global.js') })
    await page.addScriptTag({ path: require.resolve('element-plus/dist/index.full.js') })
    const result = await page.evaluate(async ({ template, directiveCode }) => {
      const failures = [], errors = []
      const permissions = ['create', 'update', 'delete', 'version-create', 'version-query', 'version-submit']
        .map(action => `mes:pro-route:${action}`)
      for (let mask = 0; mask < 64; mask++) {
        const grants = permissions.filter((_, index) => mask & (1 << index))
        const hasPermission = values => values.some(value => grants.includes(value))
        const selected = Vue.ref({ nodeType: 'FLOW', id: 1 }), publishableDraft = Vue.ref()
        const noop = () => {}
        const app = Vue.createApp({ template, setup: () => ({
          selected, publishableDraft, hasPermission, loading: false, saving: false, publishing: false,
          productBusy: false, quickConfigBusy: false, deleting: false, openGroup: noop, renameGroup: noop,
          openMove: noop, deleteSelectedNode: noop, openDifference: noop, publishDraft: noop, emit: noop, openInherited: noop
        }) })
        app.use(ElementPlus)
        app.component('Icon', { template: '<i />' })
        // Use the actual directive if it is ever reintroduced: direct DOM deletion must fail this regression.
        Function('hasPermission', 't', directiveCode + ';return hasPermi')(hasPermission, key => key)(app)
        app.config.errorHandler = error => errors.push({ mask, message: error.message })
        const container = document.createElement('div')
        document.querySelector('#fixtures').append(container)
        app.mount(container)
        for (let i = 0; i < 8; i++) {
          const group = i % 2 === 1, active = i % 3 !== 0, different = i % 2 === 0
          selected.value = { id: i + 2, nodeType: group ? 'GROUP' : 'FLOW', children: [],
            activeRouteVersionId: active ? 2 : undefined, different }
          publishableDraft.value = i % 3 === 1 ? { id: 3 } : undefined
          await Vue.nextTick()
          const expected = []
          const add = (permission, text) => { if (hasPermission([`mes:pro-route:${permission}`])) expected.push(text) }
          add('create', '新增顶级目录')
          if (group) { add('create', '新增下级目录'); add('update', '重命名目录'); add('update', '移动'); add('delete', '删除目录') }
          else { add('delete', '删除工艺流程'); add('update', '移动') }
          if (different) expected.push('差异分析')
          if (publishableDraft.value) add('version-submit', '发布')
          if (active) { add('version-create', '修改'); expected.push('查看'); add('version-query', '版本') }
          if (group && active) add('create', '新增继承流程')
          const actual = [...container.querySelectorAll('button')].map(button => button.textContent.trim())
          if (JSON.stringify(actual) !== JSON.stringify(expected)) failures.push({ mask, i, actual, expected })
        }
        try { app.unmount() } catch (error) { errors.push({ mask, message: error.message }) }
        container.remove()
      }
      return { failures, errors }
    }, { template, directiveCode })
    assert.deepEqual(pageErrors, [])
    assert.deepEqual(result.errors, [])
    assert.deepEqual(result.failures, [])

    const designer = read('src/views/mes/pro/route/RouteFlowGraphDesigner.vue')
    const linksTemplate = designer.match(/<div\s+v-if="selectedProcessDetailField.links\?\.length"[\s\S]*?<\/div>/)[0]
    const linksResult = await page.evaluate(async template => {
      const allowed = Vue.ref(false), clicked = []
      const fields = { key: 'batchRecordFormNames', links: [
        { key: 'report-1', label: '批记录甲', permission: 'mes:pro-batch-record-template:query' },
        { key: 'report-2', label: '批记录乙', permission: 'mes:pro-batch-record-template:query' }
      ] }
      const app = Vue.createApp({ template, setup: () => ({ selectedProcessDetailField: fields,
        checkPermi: () => allowed.value, handleProcessDetailLinkClick: link => clicked.push(link.key) }) })
      app.use(ElementPlus)
      const container = document.createElement('div'); document.body.append(container); app.mount(container)
      const denied = { buttons: container.querySelectorAll('button').length, text: container.textContent.trim() }
      allowed.value = true; await Vue.nextTick()
      const permitted = { buttons: container.querySelectorAll('button').length }
      container.querySelector('button').click()
      allowed.value = false; await Vue.nextTick()
      const revoked = { buttons: container.querySelectorAll('button').length, text: container.textContent.trim() }
      app.unmount(); container.remove()
      return { denied, permitted, revoked, clicked }
    }, linksTemplate)
    assert.equal(linksResult.denied.buttons, 0)
    assert.match(linksResult.denied.text, /批记录甲.*批记录乙/)
    assert.equal(linksResult.permitted.buttons, 2)
    assert.deepEqual(linksResult.revoked, linksResult.denied)
    assert.deepEqual(linksResult.clicked, ['report-1'])
  } finally {
    await browser.close()
  }
})
