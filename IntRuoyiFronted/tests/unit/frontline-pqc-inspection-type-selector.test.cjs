// Component interaction with explicit test fixtures, not real business E2E.
const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { createRequire } = require('node:module')
const { pathToFileURL } = require('node:url')
const { chromium } = require('playwright')
const compiler = createRequire(require.resolve('vue/package.json'))('@vue/compiler-sfc')
const esbuild = createRequire(require.resolve('vite'))('esbuild')
const sass = require('sass')

const sourceDir = path.resolve('src/views/mes/pro/feedback')
const selectorPath = path.join(sourceDir, 'FrontlinePqcInspectionTypeSelector.vue')
const artifacts = path.resolve('.runtime/pqc-inspection-ui')
const tabs = [
  { ruleKey: 'FIRST', type: 'FIRST', label: '首检' },
  { ruleKey: 'PATROL', type: 'PATROL', label: '巡检' },
  { ruleKey: 'FINAL', type: 'FINAL', label: '末检' }
]

test('existing PQC page delegates the new control to its original task selection and submit flow', () => {
  const filename = path.join(sourceDir, 'FrontlineFixedTemplatePanel.vue')
  const parsed = compiler.parse(fs.readFileSync(filename, 'utf8'), { filename })
  assert.deepEqual(parsed.errors, [])
  const script = compiler.compileScript(parsed.descriptor, { id: 'original-pqc-page' })
  const template = compiler.compileTemplate({
    source: parsed.descriptor.template.content,
    filename,
    id: 'original-pqc-page',
    compilerOptions: { bindingMetadata: script.bindings }
  })
  assert.deepEqual(template.errors, [])
  assert.match(
    parsed.descriptor.template.content,
    /<FrontlinePqcInspectionTypeSelector[\s\S]*:tabs="pqcInspectionTypeTabs"[\s\S]*:selected-rule-key="selectedPqcInspectionRuleKey"[\s\S]*:current-task="activePqcTaskOption"[\s\S]*@select-rule="selectPqcInspectionRule"/
  )
  const pageEntry = fs.readFileSync('src/views/mes/pro/edhr-batch/BatchPqcFillPage.vue', 'utf8')
  assert.match(pageEntry, /<FrontlineFixedTemplatePanel mode="pqc"\s*\/>/)
  assert.doesNotMatch(pageEntry, /Workspace|InspectionPage|isAssignedTask/)
})

async function preview() {
  const css = []
  const id = 'pqc-type-selector-test'
  const script = await esbuild.build({
    stdin: {
      contents: `import {createApp,h,reactive} from 'vue';
        import Selector from ${JSON.stringify(selectorPath.replaceAll('\\', '/'))};
        const state=reactive({tabs:${JSON.stringify(tabs)},finalCreated:true,selectedRuleKey:'FIRST',selectedPatrolRound:undefined,patrolRounds:[],disabled:false});
        window.selectorState=state;window.selections=[];window.created=0;window.roundActions=[];
        createApp({setup:()=>()=>h(Selector,{...state,
          onSelectRule:key=>{window.selections.push(key);state.selectedRuleKey=key},
          onSelectRound:n=>{window.selections.push(n);state.selectedRuleKey='PATROL';state.selectedPatrolRound=n},
          onCreatePatrol:()=>{window.created++},
          onCancelRound:n=>window.roundActions.push(['cancel',n]),
          onRestoreRound:n=>window.roundActions.push(['restore',n])
        })}).mount('#app');`,
      resolveDir: process.cwd(),
      loader: 'ts'
    },
    bundle: true,
    alias: { '@': path.resolve('src') },
    format: 'iife',
    write: false,
    platform: 'browser',
    define: {
      'process.env.NODE_ENV': '"test"',
      __VUE_OPTIONS_API__: 'true',
      __VUE_PROD_DEVTOOLS__: 'false',
      __VUE_PROD_HYDRATION_MISMATCH_DETAILS__: 'false'
    },
    plugins: [
      {
        name: 'vue',
        setup(build) {
          build.onLoad({ filter: /\.vue$/ }, ({ path: filename }) => {
            const parsed = compiler.parse(fs.readFileSync(filename, 'utf8'), { filename })
            assert.deepEqual(parsed.errors, [])
            const script = compiler.compileScript(parsed.descriptor, {
              id,
              inlineTemplate: true,
              genDefaultAs: '__component'
            })
            const styleSource = sass.compileString(parsed.descriptor.styles[0].content).css
            const style = compiler.compileStyle({
              source: styleSource,
              filename,
              id: `data-v-${id}`,
              scoped: true
            })
            assert.deepEqual(style.errors, [])
            css.push(style.code)
            return {
              contents: `${script.content}\n__component.__scopeId='data-v-${id}'; export default __component;`,
              loader: 'ts',
              resolveDir: path.dirname(filename)
            }
          })
        }
      }
    ]
  })
  fs.mkdirSync(artifacts, { recursive: true })
  const html = path.join(artifacts, 'preview.html')
  fs.writeFileSync(
    html,
    `<!doctype html><html lang="zh-CN"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>原一线PQC检验选择控件 · 测试样例</title><style>body{--frontline-panel:#fff;--frontline-ink:#111a15;--frontline-muted:#5b665f;--frontline-line:#cbd6ce;--frontline-dark:#24322b;font-family:'Microsoft YaHei',sans-serif;background:#eef3ef;margin:0;padding:24px}*{box-sizing:border-box}main{max-width:650px;margin:64px auto}main>p{font-size:14px;color:#5b665f}#app{padding:10px 16px;border:3px solid #f0bf00;border-radius:28px;background:#fff9d7}button{font-family:inherit}${css.join('\n')}</style></head><body><main><p>原一线 PQC 页面中的检验选择区域 · 组件测试样例</p><div id="app"></div></main><script>${script.outputFiles[0].text.replaceAll('</script', '<\\/script')}</script></body></html>`
  )
  return html
}

test('patrol creation and selection use arbitrary server rounds without time slots', { timeout: 120000 }, async () => {
  const html = await preview()
  const browser = await chromium.launch({ headless: true })
  const page = await browser.newPage({ viewport: { width: 1000, height: 700 } })
  const errors = []
  page.on('pageerror', error => errors.push(error.message))
  const openPatrol = () => page.locator('[data-pqc-inspection-type-tab="PATROL"]').click()
  try {
    await page.goto(pathToFileURL(html).href)
    assert.deepEqual(await page.locator('[data-pqc-inspection-type-tab] strong').allTextContents(), ['首检', '巡检', '末检'])
    assert.doesNotMatch(await page.locator('#app').innerText(), /上午|下午/)
    await openPatrol()
    assert.match(await page.getByRole('dialog').innerText(), /尚未创建巡检/)
    await page.locator('[data-pqc-create-patrol]').click()
    assert.equal(await page.evaluate(() => window.created), 1)
    assert.deepEqual(await page.evaluate(() => window.selections), [])
    // Creation is owned by the parent/API. The component never fabricates a round.
    assert.equal(await page.locator('[data-pqc-patrol-round]').count(), 0)
    await page.evaluate(() => {
      window.selectorState.patrolRounds = [
        { roundNo: 1, pendingCount: 0, totalCount: 3, createTime: new Date(2026, 10, 4, 9, 15, 30).getTime() },
        { roundNo: 2, pendingCount: 2, totalCount: 3, createTime: new Date(2026, 10, 4, 11, 20, 40).getTime() },
        { roundNo: 3, pendingCount: 3, totalCount: 3, createTime: new Date(2026, 10, 4, 13, 25, 50).getTime() },
        { roundNo: 4, pendingCount: 3, totalCount: 3, createTime: new Date(2026, 10, 4, 14, 25, 50).getTime() },
        { roundNo: 5, pendingCount: 3, totalCount: 3, createTime: new Date(2026, 10, 4, 15, 25, 50).getTime() },
        { roundNo: 12, pendingCount: 3, totalCount: 3, createTime: new Date(2026, 10, 5, 15, 30, 0).getTime() }
      ]
    })
    await openPatrol()
    assert.match(await page.locator('[data-pqc-patrol-round="1"]').innerText(), /2026-11-04 09:15:30/)
    assert.match(await page.locator('[data-pqc-patrol-round="2"]').innerText(), /2026-11-04 11:20:40/)
    assert.match(await page.locator('[data-pqc-patrol-round="12"]').innerText(), /2026-11-05 15:30:00/)
    assert.equal(await page.locator('[data-pqc-patrol-round="1"]').isDisabled(), true)
    const visibleCards = await page.locator('.pqc-inspection-selector__rules').evaluate(grid => {
      const viewport = grid.getBoundingClientRect()
      return [...grid.children].filter(card => {
        const bounds = card.getBoundingClientRect()
        const time = card.querySelector('time')
        return bounds.top >= viewport.top && bounds.bottom <= viewport.bottom &&
          time.scrollWidth <= time.clientWidth
      }).length
    })
    assert.ok(visibleCards >= 6, `only ${visibleCards} complete cards and timestamps visible`)
    await page.locator('[data-pqc-patrol-round="3"]').click()
    assert.deepEqual(await page.evaluate(() => window.selections), [3])
    assert.match(await page.locator('[data-pqc-inspection-type-tab="PATROL"]').innerText(), /第 3 轮/)
    await openPatrol()
    await page.locator('[data-pqc-patrol-round="12"]').click()
    assert.equal(await page.evaluate(() => window.selections.at(-1)), 12)
    await openPatrol()
    const cancel = page.getByRole('button', { name: '撤销第 3 轮', exact: true })
    const card = cancel.locator('xpath=ancestor::article')
    const cardBounds = await card.boundingBox(), cancelBounds = await cancel.boundingBox()
    assert.ok(cancelBounds.x >= cardBounds.x && cancelBounds.y >= cardBounds.y)
    assert.ok(cancelBounds.x + cancelBounds.width <= cardBounds.x + cardBounds.width)
    assert.ok(cancelBounds.y + cancelBounds.height <= cardBounds.y + cardBounds.height)
    await cancel.click()
    assert.deepEqual(await page.evaluate(() => window.roundActions), [['cancel',3]])
    await page.evaluate(() => { window.selectorState.patrolRounds[2].cancelled = true;window.selectorState.patrolRounds[2].pendingCount = 0 })
    await openPatrol()
    assert.equal(await page.locator('[data-pqc-patrol-round="3"]').isDisabled(), true)
    const restore = page.getByRole('button', { name: '恢复第 3 轮', exact: true })
    assert.equal(await restore.locator('xpath=ancestor::article').count(), 1)
    assert.equal(await restore.innerText(), '恢复')
    const cancelledBackground = await restore.locator('xpath=ancestor::article').evaluate(card =>
      getComputedStyle(card).backgroundColor.match(/\d+/g).map(Number))
    assert.ok(cancelledBackground[0] > cancelledBackground[1] && cancelledBackground[0] > cancelledBackground[2])
    await restore.click()
    assert.deepEqual(await page.evaluate(() => window.roundActions), [['cancel',3],['restore',3]])
    await page.locator('[data-pqc-inspection-type-tab="FINAL"]').click()
    await page.locator('[data-pqc-inspection-type-tab="FIRST"]').click()
    assert.deepEqual(await page.evaluate(() => window.selections.slice(-2)), ['FINAL', 'FIRST'])
    await openPatrol()
    await page.keyboard.press('Escape')
    assert.equal(await page.getByRole('dialog').isVisible(), false)
    await openPatrol()
    await page.evaluate(() => { window.selectorState.disabled = true })
    assert.equal(await page.getByRole('dialog').isVisible(), false)
    assert.equal(await page.locator('[data-pqc-inspection-type-tab="PATROL"]').isDisabled(), true)
    await page.evaluate(() => { window.selectorState.disabled = false })
    await page.screenshot({ path: path.join(artifacts, 'selector-desktop.png') })
    await page.setViewportSize({ width: 390, height: 844 })
    assert.equal(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth), true)
    await page.screenshot({ path: path.join(artifacts, 'selector-mobile.png') })
    await openPatrol()
    await page.screenshot({ path: path.join(artifacts, 'selector-patrol.png') })
    await page.evaluate(() => { window.selectorState.tabs = [] })
    assert.match(await page.locator('#app').innerText(), /当前没有可选择的检验任务/)
    assert.deepEqual(errors, [])
  } finally { await browser.close() }
})
