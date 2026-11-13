// Isolated component fixtures; not real business E2E.
const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { createRequire } = require('node:module')
const ts = require('typescript')
const sourceDir = path.resolve('src/views/mes/pro/feedback')
function load(name) {
  const code = ts.transpileModule(fs.readFileSync(path.join(sourceDir, `${name}.ts`), 'utf8'), {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2020 }
  }).outputText
  const module = { exports: {} }
  new Function('module', 'exports', 'require', code)(module, module.exports, dep => load(dep.replace('./', '')))
  return module.exports
}
const { getPqcInspectionProgressSummary: summary } = load('frontlinePqcCompletion')
const task = (inspectionRuleKey, taskStatus, roundNo = 1) => ({ inspectionRuleKey, taskStatus, roundNo })
const fixture = () => Array.from({ length: 8 }, (_, i) => ({
  pqcTaskOptions: [task('FIRST', 'SUBMITTED'), task('FIRST', 'CONFIRMED'),
    task('PATROL', i < 3 ? 'SUBMITTED' : 'PENDING', 2), task('PATROL', 'CONFIRMED', 1),
    task('PATROL', 'CANCELLED', 3), task('FINAL', 'PENDING')],
  inspectionTypeRules: [{ key: 'PATROL', required: true }],
  inspectionItems: [{ applicableInspectionTypes: ['PATROL'] }]
}))
test('first 8/8 is complete independently of patrol 3/8; rounds and final remain isolated', () => {
  assert.deepEqual(summary(fixture()).map(({ key, submittedCount, totalCount, completed }) =>
    [key, submittedCount, totalCount, completed]), [
      ['FIRST', 8, 8, true], ['PATROL-1', 8, 8, true], ['PATROL-2', 3, 8, false], ['FINAL', 0, 8, false]
    ])
  assert.deepEqual(summary([]), [])
})
test('pending and returned item tasks remove completion; confirmation restores it', () => {
  const processes = fixture()
  for (const status of ['PENDING', 'RETURNED', 'CONFIRMED']) {
    processes[0].pqcTaskOptions[1].taskStatus = status
    assert.equal(summary(processes)[0].submittedCount, status === 'CONFIRMED' ? 8 : 7)
    assert.equal(summary(processes)[0].completed, status === 'CONFIRMED')
  }
  processes.forEach(process => process.pqcTaskOptions.at(-1).taskStatus = 'SUBMITTED')
  assert.equal(summary(processes).at(-1).completed, true)
})
test('component renders yellow at 3/8 and green only at 8/8, updating reactively', { timeout: 60000 }, async () => {
  const compiler = createRequire(require.resolve('vue/package.json'))('@vue/compiler-sfc')
  const esbuild = createRequire(require.resolve('vite'))('esbuild')
  const filename = path.join(sourceDir, 'FrontlinePqcCompletionSummary.vue')
  const parsed = compiler.parse(fs.readFileSync(filename, 'utf8'), { filename })
  assert.deepEqual(parsed.errors, [])
  const script = compiler.compileScript(parsed.descriptor, { id: 'summary-test', inlineTemplate: true })
  const css = require('sass').compileString(parsed.descriptor.styles[0].content).css
  const bundle = await esbuild.build({
    stdin: { contents: `import {createApp,h,reactive} from 'vue';import Summary from './FrontlinePqcCompletionSummary.vue';
      window.processes=reactive(${JSON.stringify(fixture())});
      createApp({setup:()=>()=>h(Summary,{processes:window.processes})}).mount('#app');`, resolveDir: sourceDir, loader: 'ts' },
    bundle: true, write: false, format: 'iife', platform: 'browser',
    define: { 'process.env.NODE_ENV': '"test"', __VUE_OPTIONS_API__: 'true', __VUE_PROD_DEVTOOLS__: 'false', __VUE_PROD_HYDRATION_MISMATCH_DETAILS__: 'false' },
    plugins: [{ name: 'vue', setup(build) {
      build.onLoad({ filter: /\.vue$/ }, () => ({ contents: script.content, loader: 'ts', resolveDir: sourceDir }))
    } }]
  })
  const parentFile = path.join(sourceDir, 'FrontlineFixedTemplatePanel.vue')
  const parent = compiler.parse(fs.readFileSync(parentFile, 'utf8'), { filename: parentFile })
  assert.match(parent.descriptor.template.content, /:processes="allSwitchablePqcProcessOptions"/)
  const bindings = compiler.compileScript(parent.descriptor, { id: 'parent' }).bindings
  assert.deepEqual(compiler.compileTemplate({ source: parent.descriptor.template.content, filename: parentFile, id: 'parent', compilerOptions: { bindingMetadata: bindings } }).errors, [])
  const browser = await require('playwright').chromium.launch({ headless: true })
  try {
    const page = await browser.newPage({ viewport: { width: 500, height: 300 } })
    const errors = []
    page.on('pageerror', error => errors.push(error.message))
    await page.setContent(`<style>${css}</style><div id="app"></div>`)
    await page.addScriptTag({ content: bundle.outputFiles[0].text })
    const first = page.locator('.pqc-completion-summary__badge').filter({ hasText: '首检' })
    const patrol = page.locator('.pqc-completion-summary__badge').filter({ hasText: '巡检 2' })
    const background = locator => locator.evaluate(el => getComputedStyle(el).backgroundColor)
    assert.match(await first.innerText(), /8\/8/)
    assert.equal(await background(first), 'rgb(225, 242, 233)')
    assert.match(await patrol.innerText(), /3\/8/)
    assert.equal(await background(patrol), 'rgb(255, 243, 191)')
    await page.evaluate(() => window.processes.forEach(process => {
      process.pqcTaskOptions.find(task => task.inspectionRuleKey === 'PATROL' && task.roundNo === 2).taskStatus = 'SUBMITTED'
    }))
    assert.match(await patrol.innerText(), /8\/8/)
    assert.equal(await background(patrol), 'rgb(225, 242, 233)')
    await page.evaluate(() => { window.processes[0].pqcTaskOptions[0].taskStatus = 'PENDING' })
    assert.equal(await background(first), 'rgb(255, 243, 191)')
    assert.equal(await background(patrol), 'rgb(225, 242, 233)')
    assert.deepEqual(errors, [])
  } finally { await browser.close() }
})
