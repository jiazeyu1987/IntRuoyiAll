const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')
const ts = require('typescript')
const { createRouter, createMemoryHistory } = require('vue-router')

const base = 'src/views/dcc/controlled-file/basic-data/components/'
function evaluate(source, globals = {}) {
  const context = { exports: {}, ...globals }
  vm.runInNewContext(ts.transpileModule(source, {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
  }).outputText, context)
  return context.exports
}
function componentFunctions(names, globals, component = 'ProductCatalogTabPanel.vue') {
  const source = fs.readFileSync(base + component, 'utf8').split('<script lang="ts" setup>')[1].split('</script>')[0]
  const ast = ts.createSourceFile('component.ts', source, ts.ScriptTarget.ES2022, true)
  const selected = ast.statements.filter(statement => ts.isVariableStatement(statement)
    && statement.declarationList.declarations.some(d => names.includes(d.name.getText(ast))))
  assert.equal(selected.length, names.length)
  return evaluate(selected.map(s => s.getText(ast)).join('\n') + `\nexport { ${names.join(', ')} }`, globals)
}

test('cross-module links require both query authority and an actual tenant menu route', () => {
  let permissions = new Set()
  const { canOpenProjectLinkedPage: canOpen } = evaluate(fs.readFileSync(base + 'project-linked-navigation.ts', 'utf8'), {
    require: () => ({ checkPermi: list => list.some(p => permissions.has(p)) })
  })
  const router = createRouter({ history: createMemoryHistory(), routes: [
    { path: '/:path(.*)*', name: '404Page', component: {} }
  ] })
  const path = '/mdm/registration-certificate'
  permissions.add('dcc:registration-certificate:query-current')
  assert.equal(canOpen(router, path, 'dcc:registration-certificate:query-current'), false)
  router.addRoute({ path, name: 'CertificateList', component: {} })
  assert.equal(canOpen(router, path, 'dcc:registration-certificate:query-current'), true)
  permissions.clear()
  assert.equal(canOpen(router, path, 'dcc:registration-certificate:query-current'), false)
})

for (const mayQueryCertificates of [false, true]) {
  test(`viewing an existing relation never queries certificate candidates (query=${mayQueryCertificates})`, async () => {
    let candidateCalls = 0
    const row = { id: 3, projectCodeId: 42, registrationCertificateId: 91 }
    const globals = {
      bindingRow: { value: null }, bindingRelations: { value: [] }, bindingVisible: { value: false },
      bindingFormData: {}, resetBindingForm() {},
      getDccDataRelationsByProductCatalogId: async () => [{ id: 7 }],
      canMaintainBindings: { value: true }, canQueryBindingCertificates: { value: mayQueryCertificates },
      handleBindingProjectCodeChange: async () => { candidateCalls++ },
      searchBindingProjectCodes: async () => { throw Error('unexpected project query') }
    }
    const { openBinding } = componentFunctions(['openBinding'], globals)
    await openBinding(row)
    assert.equal(candidateCalls, 0)
    assert.equal(globals.bindingRelations.value.length, 1)
    assert.equal(globals.bindingFormData.registrationCertificateId, 91)
  })
}

test('binding a project without certificate query authority makes no forbidden request', async () => {
  let requests = 0
  const globals = {
    bindingFormData: { registrationCertificateId: 91 }, bindingCertificateOptions: { value: [1] },
    canMaintainBindings: { value: true }, canQueryBindingCertificates: { value: false },
    bindingCertificateLoading: { value: false },
    getRegistrationCertificatePage: async () => { requests++; throw Error('forbidden') }
  }
  const { handleBindingProjectCodeChange } = componentFunctions(['handleBindingProjectCodeChange'], globals)
  await handleBindingProjectCodeChange(42)
  assert.equal(requests, 0)
  assert.equal(globals.bindingFormData.registrationCertificateId, undefined)
  assert.equal(globals.bindingCertificateLoading.value, false)
})

test('certificate candidate requests preserve the selected project and propagate failures', async () => {
  const failure = Error('certificate service unavailable')
  const globals = {
    bindingFormData: {}, bindingCertificateOptions: { value: [] },
    canMaintainBindings: { value: true }, canQueryBindingCertificates: { value: true },
    bindingCertificateLoading: { value: false },
    getRegistrationCertificatePage: async params => { assert.equal(params.projectCodeId, 42); throw failure }
  }
  const { handleBindingProjectCodeChange } = componentFunctions(['handleBindingProjectCodeChange'], globals)
  await assert.rejects(handleBindingProjectCodeChange(42), error => error === failure)
  assert.equal(globals.bindingCertificateLoading.value, false)
})

test('project-only readers do not issue document or taxonomy requests or remain loading', async () => {
  let resetCalls = 0
  const globals = {
    detailRequestSequence: 1, selectedProjectCode: { value: { id: 42 } },
    canQueryAssociatedFiles: { value: false }, associatedFilesLoading: { value: true },
    resetAssociatedFilesState() { resetCalls++ },
    getProjectCodeFileTaxonomies() { throw Error('unexpected taxonomy request') },
    getProjectCodeControlledFilesPage() { throw Error('unexpected file request') }
  }
  const { getAssociatedFiles } = componentFunctions(['getAssociatedFiles'], globals, 'ProjectCodeTabPanel.vue')
  await getAssociatedFiles(42, 1)
  assert.equal(resetCalls, 1)
  assert.equal(globals.associatedFilesLoading.value, false)
})

test('document read errors remain explicit and propagate to the caller', async () => {
  const failure = Error('taxonomy query failed')
  const globals = {
    detailRequestSequence: 1, selectedProjectCode: { value: { id: 42 } },
    canQueryAssociatedFiles: { value: true }, associatedFilesLoading: { value: false },
    associatedFilesError: { value: '' },
    getProjectCodeFileTaxonomies: async id => { assert.equal(id, 42); throw failure },
    getProjectCodeControlledFilesPage() { throw Error('must not fetch files after taxonomy failure') },
    resolveAiCategoryErrorMessage: error => error.message
  }
  const { getAssociatedFiles } = componentFunctions(['getAssociatedFiles'], globals, 'ProjectCodeTabPanel.vue')
  await assert.rejects(getAssociatedFiles(42, 1), error => error === failure)
  assert.match(globals.associatedFilesError.value, /taxonomy query failed/)
  assert.equal(globals.associatedFilesLoading.value, false)
})

test('authorized document readers use project taxonomy and retain every file page', async () => {
  const calls = []
  const globals = {
    detailRequestSequence: 1, selectedProjectCode: { value: { id: 42 } },
    canQueryAssociatedFiles: { value: true }, associatedFilesLoading: { value: false },
    associatedFilesError: { value: '' }, fileTypeTaxonomies: { value: [] },
    associatedNavigationFiles: { value: [] }, associatedFilesTotal: { value: 0 },
    DCC_PROJECT_CODE_ASSOCIATED_NAVIGATION_PAGE_SIZE: 2,
    getProjectCodeFileTaxonomies: async id => { calls.push(['taxonomy', id]); return [{ id: 9 }] },
    getProjectCodeControlledFilesPage: async (id, query) => {
      calls.push(['files', id, query.pageNo])
      return { total: 3, list: query.pageNo === 1 ? [{ id: 1 }, { id: 2 }] : [{ id: 3 }] }
    },
    resetAssociatedFilePage() {}, applyAssociatedRouteFocus: () => false, ensureAssociatedSelection() {}
  }
  const { getAssociatedFiles } = componentFunctions(['getAssociatedFiles'], globals, 'ProjectCodeTabPanel.vue')
  await getAssociatedFiles(42, 1)
  assert.deepEqual(calls, [['taxonomy', 42], ['files', 42, 1], ['files', 42, 2]])
  assert.equal(globals.associatedNavigationFiles.value.length, 3)
  assert.equal(globals.associatedFilesTotal.value, 3)
  assert.equal(globals.fileTypeTaxonomies.value[0].id, 9)
  assert.equal(globals.associatedFilesLoading.value, false)
})
