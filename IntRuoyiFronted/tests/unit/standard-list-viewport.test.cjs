const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const ts = require('typescript')
const { chromium } = require('playwright')

test('standard lists outside eDHR keep their header and pagination visible while the body scrolls', async () => {
  const hook = ts.transpileModule(fs.readFileSync('src/hooks/web/useEdhrListViewport.ts', 'utf8'), {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
  }).outputText
  const css = fs.readFileSync('src/styles/edhr-list-viewport.css', 'utf8')
  const browser = await chromium.launch({ headless: true })
  try {
    const page = await browser.newPage({ viewport: { width: 1440, height: 900 } })
    await page.setContent(`<style>
      body { margin: 0; }
      main { padding: 100px 20px 0; }
      .unified-list-template { display: flex; flex-direction: column; }
      .toolbar { height: 48px; }
      .el-pagination { height: 32px; margin: 15px 0; }
      .el-table__inner-wrapper { display: flex; flex-direction: column; }
      header { height: 46px; flex-shrink: 0; }
      .el-scrollbar__wrap { overflow: auto; }
      .rows { height: 1600px; }
      .excluded { position: fixed; top: 0; right: 0; }
      ${css}
      </style>
      <main id="root"><section class="unified-list-template">
        <div class="toolbar"></div><div class="unified-list-template__table-shell">
          <div id="standard" class="el-table"><div class="el-table__inner-wrapper">
            <header>表头</header><div class="el-table__body-wrapper"><div class="el-scrollbar">
              <div class="el-scrollbar__wrap"><div class="rows"></div></div>
            </div></div>
          </div></div>
        </div><footer class="el-pagination">分页</footer>
      </section>
      <div class="excluded">
        <div id="ordinary" class="el-table"></div>
        <div class="unified-list-template__table-shell"><div id="owned" class="el-table" style="height:120px"></div></div>
        <div class="el-dialog"><div class="unified-list-template__table-shell"><div id="dialog" class="el-table"></div></div></div>
        <div class="el-drawer"><div class="unified-list-template__table-shell"><div id="drawer" class="el-table"></div></div></div>
        <div data-list-viewport="embedded"><div class="unified-list-template__table-shell"><div id="embedded" class="el-table"></div></div></div>
        <div class="unified-list-template__table-shell" style="display:none"><div id="hidden" class="el-table"></div></div>
      </div></main>`)
    await page.evaluate(code => {
      const mounted = []
      const exports = {}
      const require = id => {
        if (id === 'vue') return { onMounted: fn => mounted.push(fn), onBeforeUnmount() {}, watch() {} }
        if (id.endsWith('.css')) return {}
        throw new Error(`Unexpected dependency: ${id}`)
      }
      new Function('require', 'exports', code)(require, exports)
      exports.useEdhrListViewport({ value: document.querySelector('#root') }, { value: false })
      mounted.forEach(fn => fn())
    }, hook)
    await page.waitForFunction(() => document.querySelector('#standard').classList.contains('edhr-viewport-table'))
    const measure = () => page.evaluate(() => {
      const table = document.querySelector('#standard')
      const wrap = table.querySelector('.el-scrollbar__wrap')
      return { height: table.clientHeight, header: table.querySelector('header').getBoundingClientRect().top,
        footer: document.querySelector('footer').getBoundingClientRect().bottom,
        body: wrap.clientHeight, content: wrap.scrollHeight, scroll: wrap.scrollTop }
    })
    const before = await measure()
    assert.equal(before.height, 690)
    assert.equal(before.footer, 885)
    assert.ok(before.content > before.body)
    await page.locator('#standard .el-scrollbar__wrap').evaluate(el => { el.scrollTop = 300 })
    const after = await measure()
    assert.equal(after.scroll, 300)
    assert.equal(after.header, before.header)
    assert.equal(after.footer, before.footer)
    for (const id of ['ordinary', 'owned', 'dialog', 'drawer', 'embedded', 'hidden']) {
      assert.equal(await page.locator(`#${id}`).evaluate(el => el.classList.contains('edhr-viewport-table')), false, id)
    }
    assert.equal(await page.locator('#owned').evaluate(el => el.clientHeight), 120)
    await page.setViewportSize({ width: 1100, height: 720 })
    await page.waitForFunction(() => document.querySelector('#standard').clientHeight === 510)
    assert.equal((await measure()).footer, 705)
    await page.locator('#standard .rows').evaluate(el => { el.style.height = '0px' })
    assert.equal((await measure()).height, 510, 'empty results still fill the available viewport')
    await page.locator('#hidden').evaluate(el => { el.parentElement.style.display = 'block' })
    await page.waitForFunction(() => document.querySelector('#hidden').classList.contains('edhr-viewport-table'))
    await page.locator('#standard').evaluate(el => { el.style.height = '200px' })
    // A subsequent parent resize also releases an explicit layout acquired after mounting.
    await page.setViewportSize({ width: 1100, height: 800 })
    await page.waitForFunction(() => !document.querySelector('#standard').classList.contains('edhr-viewport-table'))
    assert.equal(await page.locator('#standard').evaluate(el => el.clientHeight), 200)
  } finally {
    await browser.close()
  }
})
