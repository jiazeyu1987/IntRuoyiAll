const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')
const ts = require('typescript')
const { chromium } = require('playwright')

// Isolated browser layout test: real positioning function and stylesheet, no business API.
const root = path.resolve(__dirname, '../..')
const source = fs.readFileSync(path.join(root, 'src/components/UserTableColumnGlobalEnhancer/index.vue'), 'utf8')
const position = source.slice(source.indexOf('const isTabLocalPanel ='), source.indexOf('const updateAllPanelPositions'))
const script = ts.transpileModule(position, { compilerOptions: { target: ts.ScriptTarget.ES2020 } }).outputText
const styles = fs.readFileSync(path.join(root, 'src/styles/index.scss'), 'utf8').match(/\.user-table-column-global-settings\s*\{[^}]+\}/)[0]

;(async () => {
  const browser = await chromium.launch({ headless: true })
  try {
    const page = await browser.newPage({ viewport: { width: 1000, height: 800 } })
    await page.setContent(`<style>${styles}
      .el-table { width:600px; height:100px; position:absolute; top:80px; left:40px }
      .el-overlay { position:fixed; inset:0; background:white }
      </style><div class="el-table" id="base"></div>
      <div class="el-overlay" id="dialog" style="z-index:2004"><div class="el-table" id="inside"></div></div>
      <div class="el-overlay" id="drawer" style="z-index:2007"><div class="el-table" id="nested"></div></div>
      <div class="el-overlay" id="preview" style="z-index:2008"></div>`)
    const hits = await page.evaluate(code => {
      const update = new Function(code + '; return updatePanelPosition')()
      const controls = ['base', 'inside', 'nested'].map(id => {
        const table = { tableEl: document.getElementById(id), style: {} }
        update(table)
        const control = document.createElement('button')
        control.className = 'user-table-column-global-settings'
        control.textContent = id
        Object.assign(control.style, table.style)
        document.body.append(control)
        return control
      })
      const hit = () => document.elementFromPoint(400, 100)?.id || document.elementFromPoint(400, 100)?.textContent
      const hits = [hit()]
      for (const id of ['preview', 'drawer', 'dialog']) {
        document.getElementById(id).remove()
        // Real enhancer prunes controls whose table was destroyed.
        controls.filter((_, i) => !document.getElementById(['base', 'inside', 'nested'][i])).forEach(n => n.remove())
        hits.push(hit())
      }
      return hits
    }, script)
    assert.deepEqual(hits, ['preview', 'nested', 'inside', 'base'], 'each control must remain in its owning modal layer and recover after the covering modal closes')
    await page.setViewportSize({ width: 390, height: 844 })
    const local = await page.evaluate(code => {
      const { update, popoverStyle } = new Function(code + '; return {update: updatePanelPosition, popoverStyle: tabLocalPopoverStyle}')()
      const root = document.createElement('div')
      root.className = 'followup-management'
      root.innerHTML = '<div class="el-table" style="width:328px;height:120px;left:20px;top:700px"></div>'
      document.body.append(root)
      const table = { tableEl: root.firstElementChild, style: {} }
      update(table)
      const popover = document.createElement('div')
      Object.assign(popover.style, popoverStyle(table), { width: '240px', height: '360px' })
      document.body.append(popover)
      const rect = popover.getBoundingClientRect()
      root.style.display = 'none'
      update(table)
      return { right: rect.right, bottom: rect.bottom, left: rect.left, top: rect.top, hidden: table.style.display }
    }, script)
    assert.ok(local.left >= 0 && local.top >= 0 && local.right <= 390 && local.bottom <= 844, 'DCC field menu must remain within the mobile viewport')
    assert.ok(local.bottom <= 700, 'a menu near the viewport bottom must open above its toolbar without covering the toggle')
    assert.equal(local.hidden, 'none', 'closed DCC drawers must not leave a visible field toolbar')
    const modalStyles = fs.readFileSync(path.join(root, 'src/styles/index.scss'), 'utf8')
      .match(/:is\(\.el-dialog, \.el-drawer, \.dcc-detail-surface, \[data-testid='dcc-detail-attachments'\]\) \.user-table-column-global-managed\s*\{[^}]+\}/)[0]
    await page.setContent(`<style>${styles}${modalStyles}
      .el-dialog { width:340px;height:300px;overflow:auto }
      .el-table { position:relative;width:320px }
      .header { height:36px } .rows { height:500px }
      </style><div class="el-overlay" style="z-index:2004"><div class="el-dialog">
      <div class="el-table user-table-column-global-managed"><div class="header">表头</div><div class="rows">规则内容</div></div>
      </div></div>`)
    const modalLayout = await page.evaluate(code => {
      const { update, local } = new Function(code + '; return {update: updatePanelPosition, local: isTabLocalPanel}')()
      const tableEl = document.querySelector('.el-table')
      const table = { tableEl, style: {} }
      update(table)
      const toolbar = document.createElement('div')
      toolbar.className = 'user-table-column-global-settings'
      toolbar.innerHTML = '<button class="user-table-column-global-settings__button">显示字段</button><button>重置</button>'
      Object.assign(toolbar.style, table.style)
      tableEl.append(toolbar)
      const before = toolbar.getBoundingClientRect()
      const header = document.querySelector('.header').getBoundingClientRect()
      document.querySelector('.el-dialog').scrollTop = 80
      update(table)
      Object.assign(toolbar.style, table.style)
      const after = toolbar.getBoundingClientRect()
      document.querySelector('.el-dialog').style.display = 'none'
      update(table)
      return { local: local(table), toolbarBottom: before.bottom, headerTop: header.top,
        scrollDelta: before.top - after.top, hidden: table.style.display }
    }, script)
    assert.equal(modalLayout.local, true, 'any dialog table uses its own toolbar row without page-specific selectors')
    assert.ok(modalLayout.toolbarBottom <= modalLayout.headerTop, 'field/reset controls must not overlap the header or row inputs')
    assert.equal(modalLayout.scrollDelta, 80, 'toolbar must scroll with its owning dialog table')
    assert.equal(modalLayout.hidden, 'none', 'closing a modal hides its toolbar')
    const attachmentLayout = await page.evaluate(code => {
      const update = new Function(code + '; return updatePanelPosition')()
      const root = document.createElement('section')
      root.dataset.testid = 'dcc-detail-attachments'
      root.innerHTML = '<div class="el-table user-table-column-global-managed"><div class="header">附件字段</div></div>'
      document.body.append(root)
      const table = { tableEl: root.firstElementChild, style: {} }
      update(table)
      const toolbar = document.createElement('button')
      toolbar.className = 'user-table-column-global-settings'
      toolbar.textContent = '显示字段 重置'
      Object.assign(toolbar.style, table.style)
      table.tableEl.append(toolbar)
      return { headerTop: table.tableEl.querySelector('.header').getBoundingClientRect().top,
        toolbarBottom: toolbar.getBoundingClientRect().bottom }
    }, script)
    assert.ok(attachmentLayout.toolbarBottom <= attachmentLayout.headerTop, 'ordinary detail attachment table reserves a row for field/reset controls')
    const registrationSource = source.slice(source.indexOf('const registerTable ='), source.indexOf('const pruneDisconnectedTables ='))
    const registration = ts.transpileModule(registrationSource, { compilerOptions: { target: ts.ScriptTarget.ES2020 } }).outputText
    const count = await page.evaluate(code => {
      const tableMap = new Map()
      const register = new Function('tableMap', 'isExplicitlyConfigured', 'readColumns', 'resolveTableIdentity', 'isTabLocalPanel', 'handleManagedHeaderPointerDown', 'attachTableObserver', 'loadPanel', 'updatePanelPosition', code + '; return registerTable')(
        tableMap, () => false, () => [{ key: 'name' }], (_, i) => `table-${i}`, () => false, () => {}, () => {}, () => {}, () => {}
      )
      const el = document.createElement('div')
      el.className = 'el-table'
      register(el, 0)
      register(el, 2)
      return tableMap.size
    }, registration)
    assert.equal(count, 1, 'changing table scan order must not register the same DOM table twice')
    const columnSource = source.slice(source.indexOf('const readColumns ='), source.indexOf('const readColumnWidthSnapshot ='))
    const columnScript = ts.transpileModule(columnSource, { compilerOptions: { target: ts.ScriptTarget.ES2020 } }).outputText
    const labels = await page.evaluate(code => {
      const read = new Function('normalizeText', 'resolveColumnKey', 'normalizeWidth', 'STRUCTURAL_LABELS', code + '; return readColumns')(
        value => (value || '').trim(), (_, label) => label, value => Number(value) || undefined, new Set(['操作'])
      )
      const el = document.createElement('div')
      el.className = 'el-table'
      el.innerHTML = '<div class="el-table__header-wrapper"><table><tr><th>主表字段</th></tr></table></div><div class="el-table"><div class="el-table__header-wrapper"><table><tr><th>子表字段</th></tr></table></div></div>'
      return read(el).map(column => column.label)
    }, columnScript)
    assert.deepEqual(labels, ['主表字段'], 'nested table fields must not leak into the parent configuration')
    console.log('PASS: page, dialog, drawer and covering preview stack correctly')
  } finally { await browser.close() }
})().catch(error => { console.error(error); process.exitCode = 1 })
