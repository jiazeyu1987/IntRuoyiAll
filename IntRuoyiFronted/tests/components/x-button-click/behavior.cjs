const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')
const crypto = require('node:crypto')
const { createRequire } = require('node:module')
const { chromium } = require('playwright')
const { expect } = require('playwright/test')

const phase = process.argv[2]
const offline = process.argv.includes('--offline')
assert.ok(['red', 'green'].includes(phase), 'Pass red or green as the evidence phase; assertions stay identical')
const output = path.resolve(__dirname, '../../../../doc/tasks/20261016-dcc-e2e-06/runs/20261017-acceptance')
fs.mkdirSync(output, { recursive: true })
async function compileOffline() {
  const frontend = path.resolve(__dirname, '../../..')
  const req = createRequire(path.join(frontend, 'package.json'))
  const compiler = createRequire(req.resolve('vue/package.json'))('@vue/compiler-sfc')
  const esbuild = createRequire(req.resolve('vite/package.json'))('esbuild')
  const compiled = []
  const bundle = await esbuild.build({
    stdin: { contents: `import {createApp} from 'vue';import {ElButton} from 'element-plus';import Harness from './Harness.vue';createApp(Harness).component('ElButton',ElButton).mount('#app')`, resolveDir: __dirname },
    bundle: true, write: false, platform: 'browser', format: 'iife', alias: { '@': path.join(frontend, 'src') },
    define: { __VUE_OPTIONS_API__: 'true', __VUE_PROD_DEVTOOLS__: 'false', __VUE_PROD_HYDRATION_MISMATCH_DETAILS__: 'false', 'process.env.NODE_ENV': '"development"' },
    plugins: [{ name: 'actual-vue-sfc', setup(build) {
      build.onLoad({ filter: /\.vue$/ }, async args => {
        const source = fs.readFileSync(args.path, 'utf8')
        const { descriptor, errors } = compiler.parse(source, { filename: args.path })
        assert.equal(errors.length, 0)
        const component = compiler.compileScript(descriptor, { id: 'x-button-behavior', inlineTemplate: true })
        // These are the same real Vue hooks supplied by the repository auto-import plugin in Vite.
        const hooks = args.path.endsWith(path.join('XButton', 'src', 'XButton.vue')) ? `import {computed,useAttrs} from 'vue';\n` : ''
        compiled.push({ path: args.path, sha256: crypto.createHash('sha256').update(source).digest('hex') })
        return { contents: hooks + component.content, loader: 'ts', resolveDir: path.dirname(args.path) }
      })
    } }]
  })
  return { html: `<div id="app"></div><script>${bundle.outputFiles[0].text.replace(/<\/script/gi, '<\\/script')}</script>`, compiled }
}
async function main() {
  const browser = await chromium.launch({ headless: true })
  const context = await browser.newContext({ viewport: { width: 1440, height: 900 } })
  const page = await context.newPage()
  const result = { phase, kind: offline ? 'real-compiler-sfc-esbuild-Vue-mounted-with-real-ElButton-offline' : 'real-Vite-compiled-Vue-mounted-with-real-ElButton', cases: [], moduleLoads: [], pageErrors: [], businessRequests: 0 }
  page.on('response', response => {
    const pathname = new URL(response.url()).pathname
    if (pathname.endsWith('/XButton.vue') || pathname.endsWith('/Harness.vue')) result.moduleLoads.push({ path: pathname, status: response.status() })
  })
  page.on('request', request => { if (new URL(request.url()).pathname.startsWith('/admin-api/')) result.businessRequests++ })
  page.on('pageerror', error => result.pageErrors.push(error.message))
  await context.tracing.start({ screenshots: true, snapshots: false, sources: false })
  const check = (name, action) => { try { action(); result.cases.push({ name, status: 'PASS' }) } catch (error) { result.cases.push({ name, status: 'FAIL', error: error.message }) } }
  try {
    if (offline) {
      const fixture = await compileOffline()
      result.compiledSources = fixture.compiled
      await page.setContent(fixture.html, { waitUntil: 'domcontentloaded' })
    } else await page.goto(`http://127.0.0.1:8061/tests/components/x-button-click/index.html?phase=${phase}`, { waitUntil: 'domcontentloaded' })
    await expect(page.getByTestId('x-button-behavior-harness')).toBeVisible({ timeout: 30000 })
    for (const id of ['listener', 'prop', 'disabled', 'loading']) await expect(page.getByTestId(id)).toHaveClass(/el-button/)
    await page.getByTestId('listener').click()
    await expect(page.getByTestId('state')).toContainText('"listenerCount":')
    await page.getByTestId('prop').click()
    for (const id of ['disabled', 'loading']) {
      await expect(page.getByTestId(id)).toBeDisabled()
      const box = await page.getByTestId(id).boundingBox()
      assert.ok(box, `${id} must be visibly mounted`)
      await page.mouse.click(box.x + box.width / 2, box.y + box.height / 2)
    }
    const state = JSON.parse(await page.getByTestId('state').textContent())
    result.state = state
    check('@click invokes external handler once', () => assert.equal(state.listenerCount, 1))
    check(':onClick compatibility invokes external handler once', () => assert.equal(state.propCount, 1))
    check('the exact native MouseEvent reaches the handler', () => assert.equal(state.eventSame, true))
    check('disabled and loading remain constrained by real ElButton', () => { assert.equal(state.disabledCount, 0); assert.equal(state.loadingCount, 0) })
    check('real SFC modules loaded without page errors or business requests', () => {
      assert.ok(offline ? result.compiledSources.some(x => x.path.endsWith(path.join('XButton', 'src', 'XButton.vue'))) : result.moduleLoads.some(x => x.path.endsWith('/XButton.vue') && x.status === 200))
      assert.equal(result.pageErrors.length, 0); assert.equal(result.businessRequests, 0)
    })
    await page.screenshot({ path: path.join(output, `x-button-${offline ? 'offline-' : ''}${phase}.png`), fullPage: true })
  } catch (error) { result.infrastructureError = error.message }
  finally {
    result.status = result.infrastructureError || result.cases.some(x => x.status === 'FAIL') ? 'FAIL' : 'PASS'
    await context.tracing.stop({ path: path.join(output, `x-button-${offline ? 'offline-' : ''}${phase}.trace.zip`) })
    await browser.close()
    fs.writeFileSync(path.join(output, `x-button-${offline ? 'offline-' : ''}${phase}.json`), JSON.stringify(result, null, 2) + '\n')
    console.log(JSON.stringify(result))
    if (result.status !== 'PASS') process.exitCode = 1
  }
}
main().catch(error => { console.error(error.message); process.exitCode = 1 })
