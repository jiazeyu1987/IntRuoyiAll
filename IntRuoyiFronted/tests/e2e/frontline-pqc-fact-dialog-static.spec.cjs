const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { stripTypeScriptTypes } = require('node:module')
const { runInNewContext } = require('node:vm')
const { parse, compileScript, compileTemplate } = require('vue/compiler-sfc')

const filename = path.resolve(__dirname, '../../src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue')
const source = fs.readFileSync(filename, 'utf8').replace(/\r\n/g, '\n')
const { descriptor, errors } = parse(source, { filename })
assert.deepEqual(errors, [])
const script = compileScript(descriptor, { id: 'pqc-combined-facts' })
assert.deepEqual(compileTemplate({ source: descriptor.template.content, filename,
  id: 'pqc-combined-facts', compilerOptions: { bindingMetadata: script.bindings } }).errors, [])

const header = source.slice(source.indexOf('<header class="frontline-operator-top is-pqc">'), source.indexOf('</header>'))
assert.ok(header.indexOf('data-pqc-fullscreen-toggle') < header.indexOf('data-pqc-facts-button'))
assert.match(header, /data-pqc-facts-button[\s\S]*:disabled="!activePqcTabItem \|\| isPqcProcessNavigationBlocked"[\s\S]*openPqcFactsDialog\(activePqcTabItem.key\)[\s\S]*方法和标准/)
assert.doesNotMatch(descriptor.template.content, /data-pqc-standard-button|data-pqc-method-button/)
assert.equal((descriptor.template.content.match(/data-pqc-facts-dialog\s/g) || []).length, 1)
const dialog = source.slice(source.indexOf('v-if="activePqcFactsItem"'), source.indexOf('<main class="frontline-operator-main is-pqc">'))
for (const token of ['role="dialog"', 'aria-labelledby="pqc-facts-dialog-title"',
  'activePqcFactsItem.standardText', 'formatPqcMethodSummary(activePqcFactsItem)',
  'activePqcFactsItem.samplingPlanText', 'activePqcFactsItem.inspectionTool',
  '@click.self="closePqcFactsDialog"', 'aria-label="关闭方法和标准弹框"']) {
  assert.ok(dialog.includes(token), `combined facts must retain ${token}`)
}
assert.ok(source.indexOf('data-pqc-facts-dialog') > source.indexOf('data-pqc-fullscreen-root'))

const selected = { key: 'appearance', label: '外观' }
const failures = []
const runtime = { activePqcFactsKey: { value: undefined }, pqcTaskInspectionItemMap: { value: { appearance: selected } },
  requirePqcInspectionDisplayFields: item => { assert.equal(item, selected) }, showFrontlineError: error => failures.push(error) }
const start = source.indexOf('const openPqcFactsDialog = (')
const end = source.indexOf('const applyPqcTaskSnapshotToDraft = (', start)
runInNewContext(stripTypeScriptTypes(source.slice(start, end)) + '\nglobalThis.open = openPqcFactsDialog; globalThis.close = closePqcFactsDialog', runtime)
runtime.open('appearance'); assert.equal(runtime.activePqcFactsKey.value, 'appearance')
runtime.close(); assert.equal(runtime.activePqcFactsKey.value, undefined)
runtime.open('missing'); assert.equal(runtime.activePqcFactsKey.value, undefined); assert.equal(failures.length, 1)
runtime.requirePqcInspectionDisplayFields = () => { throw new Error('missing formal display fields') }
runtime.open('appearance'); assert.equal(runtime.activePqcFactsKey.value, undefined); assert.equal(failures.length, 2)
console.log('PASS: combined PQC facts entry, formal contents, close and error paths')
