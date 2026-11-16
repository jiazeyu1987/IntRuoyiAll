const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const ts = require('typescript')
const { chromium } = require('playwright')

test('form center retains its own table height while ordinary lists use viewport sizing', async () => {
  const hook = ts.transpileModule(fs.readFileSync('src/hooks/web/useEdhrListViewport.ts', 'utf8'), {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
  }).outputText
  const source = fs.readFileSync('src/views/mes/pro/batchrecordformlist/index.vue', 'utf8')
  const { parse, compileStyle } = require('vue/compiler-sfc')
  const { descriptor } = parse(source)
  const css = compileStyle({ source: descriptor.styles[0].content, filename: 'index.vue', id: 'data-v-test', scoped: true }).code.replaceAll('[data-v-test]', '')
  const viewportCss = fs.readFileSync('src/styles/edhr-list-viewport.css', 'utf8')
  const browser = await chromium.launch({ headless: true })
  try {
    const page = await browser.newPage({ viewport: { width: 1720, height: 900 } })
    await page.setContent(`<style>
      :root { --top-tool-height: 0px; --tags-view-height: 34px; --app-content-padding: 20px; }
      body { margin: 0; }
      .unified-list-template { display: flex; flex-direction: column; }
      .unified-list-template__query-form, .el-pagination { height: 48px; }
      ${css}
      ${viewportCss}
      </style>
      <main id="root"><div class="batch-record-form-layout">
        <section class="batch-record-form-layout__list"><div class="unified-list-template">
          <div class="unified-list-template__query-form"></div>
          <div class="unified-list-template__table-shell">
            <div id="table" class="el-table edhr-viewport-table" style="height:100%;--edhr-list-height:100px"></div>
          </div><div class="el-pagination"></div>
        </div></section><section class="batch-record-form-preview"></section>
      </div></main>`)
    await page.evaluate((code) => {
      const mounted = []
      const exports = {}
      const require = (id) => {
        if (id === 'vue') return { onMounted: fn => mounted.push(fn), onBeforeUnmount() {}, watch() {} }
        if (id.endsWith('.css')) return {}
        throw new Error(`Unexpected dependency: ${id}`)
      }
      new Function('require', 'exports', code)(require, exports)
      exports.useEdhrListViewport({ value: document.querySelector('#root') }, { value: true })
      mounted.forEach(fn => fn())
    }, hook)
    const measure = () => page.evaluate(() => {
      const table = document.querySelector('#table')
      return { height: table.getBoundingClientRect().height, shell: table.parentElement.getBoundingClientRect().height,
        overridden: table.classList.contains('edhr-viewport-table'), value: table.style.getPropertyValue('--edhr-list-height') }
    })
    await page.waitForFunction(() => !document.querySelector('#table').classList.contains('edhr-viewport-table'))
    let result = await measure()
    assert.ok(result.height > 600, JSON.stringify(result))
    assert.equal(result.height, result.shell)
    assert.equal(result.value, '')
    await page.setViewportSize({ width: 1720, height: 750 })
    result = await measure()
    assert.ok(result.height > 500 && result.height < 600, JSON.stringify(result))
    assert.equal(result.height, result.shell)
    await page.setViewportSize({ width: 1100, height: 800 })
    result = await measure()
    assert.ok(result.height > 600, JSON.stringify(result))
    assert.equal(result.height, result.shell)
    // Removing the owning layout must restore the ordinary-list viewport behavior.
    await page.evaluate(() => document.querySelector('.batch-record-form-layout').classList.remove('batch-record-form-layout'))
    await page.waitForFunction(() => document.querySelector('#table').classList.contains('edhr-viewport-table'))
    result = await measure()
    assert.ok(result.height > 600, JSON.stringify(result))
    assert.match(result.value, /^\d+px$/)

    // QA release checks and PQC preview occupy neighbouring columns. Only each
    // column's own footer consumes its available vertical space.
    await page.evaluate(() => {
      document.querySelector('#root').innerHTML = `<div style="display:grid;grid-template-columns:1fr 1fr;gap:16px">
        <section><div id="checks" class="el-table"></div><footer style="height:40px"></footer></section>
        <section><div id="preview" class="el-table"></div><footer style="height:60px"></footer></section>
      </div>`
    })
    await page.waitForFunction(() => document.querySelector('#checks').style.getPropertyValue('--edhr-list-height') === '760px')
    const heights = await page.evaluate(() => ['checks', 'preview'].map(id => document.getElementById(id).getBoundingClientRect().height))
    assert.deepEqual(heights, [760, 740])
    await page.setViewportSize({ width: 1100, height: 700 })
    await page.waitForFunction(() => document.querySelector('#checks').style.getPropertyValue('--edhr-list-height') === '660px')
    assert.equal(await page.locator('#preview').evaluate(el => el.getBoundingClientRect().height), 640)
  } finally {
    await browser.close()
  }
})
