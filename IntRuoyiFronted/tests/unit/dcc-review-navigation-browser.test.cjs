const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')
const { parse, compileScript } = require('vue/compiler-sfc')
const ts = require('typescript')

// Component geometry test only; this fixture does not represent a business E2E.
const filename = 'src/views/dcc/controlled-file/detail/ReviewSectionNavigation.vue'
const { descriptor } = parse(fs.readFileSync(filename, 'utf8'), { filename })
const compiled = compileScript(descriptor, { id: 'navigation', inlineTemplate: true })
const script = ts.transpileModule(compiled.content, {
  compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
}).outputText

test('real browser navigation: clicks, wheel scrolling, sticky heading clearance and both boundaries', async () => {
  const browser = await chromium.launch({ headless: true })
  try {
    for (const nested of [true, false]) {
      const page = await browser.newPage({ viewport: { width: 1100, height: 700 } })
      await page.setContent(`<style>
        body { margin: 0; --app-content-bg-color: white; }
        #scroll { ${nested ? 'height: 500px; overflow-y: auto; margin-top: 100px; border: 2px solid;' : ''} }
        #app { padding: 16px; } header { height: 120px; }
        section { height: 450px; } section:last-child { height: 200px; }
        h2 { margin: 0; } ${descriptor.styles[0].content}
      </style><div id="scroll"><div id="app"></div></div>`)
      await page.addScriptTag({ path: require.resolve('vue/dist/vue.global.js') })
      await page.evaluate(code => {
        const exports = {}
        new Function('require', 'exports', code)(() => window.Vue, exports)
        const { createApp, shallowRef } = window.Vue
        createApp({
          components: { Navigation: exports.default },
          setup() {
            const elements = Array.from({ length: 5 }, () => shallowRef())
            const sections = ['文件概览', '正文与附件', '当前任务', '申请与审批', '关联文件']
              .map((label, index) => ({ label, element: () => elements[index].value }))
            return { elements, sections }
          },
          template: `<header>动态文件页头</header><Navigation :sections="sections" />
            <section v-for="(section, index) in sections" :ref="el => elements[index].value = el">
              <h2>{{ section.label }}</h2>
            </section>`
        }).mount('#app')
      }, script)
      const buttons = page.locator('nav button')
      const active = () => page.locator('nav [aria-current="location"]').textContent()
      const settle = () => page.waitForTimeout(650)
      assert.equal(await active(), '文件概览')
      const initialTop = await page.locator('nav').evaluate(el => el.getBoundingClientRect().top)
      assert.ok(initialTop > 100, 'navigation starts below the header')
      if (process.env.DCC_NAV_EVIDENCE_DIR) {
        fs.mkdirSync(process.env.DCC_NAV_EVIDENCE_DIR, { recursive: true })
        await page.screenshot({ path: path.join(process.env.DCC_NAV_EVIDENCE_DIR, `${nested ? 'nested' : 'window'}-top.png`) })
      }
      for (let index = 0; index < 5; index++) {
        await buttons.nth(index).click()
        await settle()
        assert.equal(await active(), await buttons.nth(index).textContent())
        const geometry = await page.evaluate(({ index, nested }) => ({
          navBottom: document.querySelector('nav').getBoundingClientRect().bottom,
          headingTop: document.querySelectorAll('h2')[index].getBoundingClientRect().top,
          headingBottom: document.querySelectorAll('h2')[index].getBoundingClientRect().bottom,
          viewportBottom: nested ? document.querySelector('#scroll').getBoundingClientRect().bottom - 2 : innerHeight
        }), { index, nested })
        assert.ok(geometry.headingTop >= geometry.navBottom, `heading ${index} is not obscured`)
        assert.ok(geometry.headingBottom <= geometry.viewportBottom, `heading ${index} is within the viewport`)
      }
      await page.evaluate(nested => {
        const scroller = nested ? document.querySelector('#scroll') : document.scrollingElement
        scroller.scrollTop = 0
      }, nested)
      await settle()
      assert.equal(await active(), '文件概览')
      await page.mouse.move(600, 350)
      await page.mouse.wheel(0, 1100)
      await settle()
      assert.equal(await active(), '当前任务')
      const stickyTop = await page.locator('nav').evaluate(el => el.getBoundingClientRect().top)
      assert.ok(Math.abs(stickyTop - (nested ? 102 : 0)) < 2, 'navigation sticks to its scroll viewport')
      await page.mouse.wheel(0, 10000)
      await settle()
      assert.equal(await active(), '关联文件')
      if (process.env.DCC_NAV_EVIDENCE_DIR) await page.screenshot({
        path: path.join(process.env.DCC_NAV_EVIDENCE_DIR, `${nested ? 'nested' : 'window'}-bottom.png`)
      })
      await page.close()
    }
  } finally {
    await browser.close()
  }
})
