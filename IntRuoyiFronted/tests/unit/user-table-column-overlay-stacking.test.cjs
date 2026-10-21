const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')
const ts = require('typescript')
const { chromium } = require('playwright')

// Isolated browser layout test: real positioning function and stylesheet, no business API.
const root = path.resolve(__dirname, '../..')
const source = fs.readFileSync(path.join(root, 'src/components/UserTableColumnGlobalEnhancer/index.vue'), 'utf8')
const position = source.slice(source.indexOf('const updatePanelPosition ='), source.indexOf('const updateAllPanelPositions'))
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
    console.log('PASS: page, dialog, drawer and covering preview stack correctly')
  } finally { await browser.close() }
})().catch(error => { console.error(error); process.exitCode = 1 })
