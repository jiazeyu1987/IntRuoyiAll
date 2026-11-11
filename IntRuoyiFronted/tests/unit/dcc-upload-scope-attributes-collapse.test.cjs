const test = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const ts = require('typescript')
const vue = require('vue')
const sfc = require('vue/compiler-sfc')
const base = 'src/views/dcc/controlled-file/'
const source = fs.readFileSync(base + 'project-attributes/ProjectApplicationAttributes.vue', 'utf8')
function setup() {
  const props = { action: 'UPLOAD', collapsible: true }
  const stateExports = {}
  new Function('exports', ts.transpileModule(fs.readFileSync(base + 'project-attributes/state.ts', 'utf8'), { compilerOptions: { module: ts.ModuleKind.CommonJS } }).outputText)(stateExports)
  const defaults = { targetMarkets: ['CE'], licenseHolder: 'Y', actualManufacturer: 'N', documentTransfer: 'N' }
  const script = sfc.parse(source).descriptor.scriptSetup.content.replace(/^import .*$/gm, '')
  let confirms = 0
  const env = { ...vue, ...stateExports, defineProps: () => props, defineEmits: () => () => {}, defineExpose: () => {}, getProjectDefaults: async () => defaults, ElMessageBox: { confirm: async () => { confirms++ } } }
  const code = ts.transpileModule(script + '\nreturn { expanded, summary, state, edit, selectProject, restore }', { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText
  const panel = new Function(...Object.keys(env), code)(...Object.values(env))
  return { panel, defaults, get confirms() { return confirms } }
}
test('attributes start collapsed, summarize current choices and keep user edits after collapsing', async () => {
  const { panel, defaults } = setup()
  assert.equal(panel.expanded.value, false)
  await panel.selectProject('5')
  assert.equal(panel.summary.value, '目标市场：CE 欧盟｜注册人：是｜生产方：否｜文件转移：否')
  panel.expanded.value = true
  panel.edit({ ...defaults, targetMarkets: ['MADSAP', 'OTHER'], otherMarket: '日本', documentTransfer: 'Y', transferTo: '工厂' })
  panel.expanded.value = false
  assert.match(panel.summary.value, /MDSAP、其他（日本）/)
  assert.match(panel.summary.value, /文件转移：是（工厂）/)
  assert.equal(panel.state.dirty, true)
  assert.match(source, /\{\{ expanded \? '收起' : '修改' \}\}/)
})
test('parent-confirmed project changes prompt once and restore still reads project defaults', async () => {
  const result = setup(), { panel, defaults } = result
  await panel.selectProject('5')
  panel.edit({ ...defaults, licenseHolder: 'N' })
  await panel.selectProject('6', true)
  assert.equal(result.confirms, 0)
  panel.edit({ ...defaults, licenseHolder: 'N' })
  await panel.restore()
  assert.equal(result.confirms, 1)
  assert.equal(panel.state.actual.licenseHolder, 'Y')
})
test('affected Vue components compile their templates and scripts', () => {
  for (const file of ['upload/index.vue', 'project-attributes/ProjectApplicationAttributes.vue', 'project-attributes/ProjectAttributesFields.vue']) {
    const filename = base + file, { descriptor, errors } = sfc.parse(fs.readFileSync(filename, 'utf8'), { filename })
    assert.deepEqual(errors, [])
    const script = sfc.compileScript(descriptor, { id: file })
    const template = sfc.compileTemplate({ source: descriptor.template.content, filename, id: file, compilerOptions: { bindingMetadata: script.bindings } })
    assert.deepEqual(template.errors, [])
  }
})

test('only input that project switching discards triggers confirmation; automatic departments are clean', () => {
  const upload = fs.readFileSync(base + 'upload/index.vue', 'utf8')
  const code = upload.slice(upload.indexOf('const uploadProjectAttributesEdited ='), upload.indexOf('const handleProjectCodeChange ='))
  const formData = { versionNo: 'A/1', remark: '保留备注', needTraining: true, effectiveMode: 'DELAYED' }
  const env = { ref: vue.ref, formData, createInitialUploadDraft: () => ({ versionNo: 'A/1' }), fileList: vue.ref([]), attachmentFileList: vue.ref([]) }
  const state = new Function(...Object.keys(env), ts.transpileModule(code + '\nreturn { hasUploadApplicationInput, autoUploadDepartmentSelections }', { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText)(...Object.values(env))
  assert.equal(state.hasUploadApplicationInput(), false)
  state.autoUploadDepartmentSelections.value = { signoff: ['7'], distribution: ['8'] }
  formData.selectedSignoffDepartmentIds = ['7']; formData.selectedDistributionDepartmentIds = ['8']
  assert.equal(state.hasUploadApplicationInput(), false)
  formData.selectedDistributionDepartmentIds = []
  assert.equal(state.hasUploadApplicationInput(), true)
  formData.selectedDistributionDepartmentIds = ['8']; formData.fileTypeTaxonomyId = '9'
  assert.equal(state.hasUploadApplicationInput(), true)
  delete formData.fileTypeTaxonomyId; formData.versionNo = 'B/1'
  assert.equal(state.hasUploadApplicationInput(), true)
})
