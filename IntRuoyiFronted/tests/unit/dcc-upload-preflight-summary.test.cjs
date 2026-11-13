const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')
const ts = require('typescript')
const { ref, reactive, computed, nextTick } = require('vue')
const { parse, compileScript, compileStyle } = require('vue/compiler-sfc')
const page = fs.readFileSync('src/views/dcc/controlled-file/upload/index.vue', 'utf8')

function setup() {
  const context = {
    ref, reactive, computed, nextTick,
    previewUpload: ref({ fileName: '正文.docx' }), uploadPreviewLoading: ref(false), uploadPreviewError: ref(''),
    formData: reactive({ fileNumber: 'SOP-07', versionNo: 'A/1', dccProjectCodeId: '7', categoryId: '8' }),
    formRef: ref({ fields: [] }),
    routeReadiness: ref({ ready: true, nodes: [], blockers: [] }), uploadApprovers: ref({}),
    approvalUsersLoading: ref(false), approvalUsersError: ref(''), uploadApproverError: ref(''),
    routeReadinessLoading: ref(false), routeReadinessError: ref(''),
    signoffDepartmentsLoading: ref(false), signoffDepartmentsError: ref(''),
    isNormalNewUpload: ref(true), isExternalReview: ref(false), isCustomApprovalSource: ref(false),
    selectedProjectFolder: ref({ name: '正式文件夹', projectCodeId: '7' }),
    projectFoldersLoading: ref(false), projectFoldersError: ref(''), selectedUploadDirectoryPath: ref('项目/目录'),
    isVersionNoFormatValid: ref(true), versionFormatPreflightMessage: ref(''),
    currentVersionLookupLoading: ref(false), currentVersionLookupError: ref(''),
    currentVersionInfo: ref({ matched: false }), existingUploadIdentityBlockReason: ref(''),
    isRequestedVersionDuplicate: ref(false), versionDuplicatePreflightMessage: ref(''), currentVersionProjectionBlockReason: ref(''),
    effectiveSelectionValidation: ref({ valid: true }), effectiveDatePreflightText: ref('按签名生效'),
    selectedCategory: ref({ name: '正式类别' }), approvalChainPreflightText: ref('审批检查说明'),
    controlledBrowserPermissionScopeText: ref('按正式 VIEW 权限控制')
  }
  const start = page.indexOf('const uploadPreflightChecks =')
  const end = page.indexOf('const loadApprovalUsers =', start)
  vm.runInNewContext(ts.transpileModule(page.slice(start, end) + '\nObject.assign(globalThis, { uploadPreflightChecks, uploadPreflightSummary, sortedUploadPreflightChecks, locatePreflightField, preflightDetailsVisible })', {
    compilerOptions: { target: ts.ScriptTarget.ES2022 }
  }).outputText, context)
  return context
}

test('all seven actual checks pass; changes immediately update count and put failures first', () => {
  const c = setup()
  assert.equal(c.uploadPreflightSummary.value.text, '7项校验通过')
  c.effectiveSelectionValidation.value = { valid: false }
  c.existingUploadIdentityBlockReason.value = '文件编号已重复'
  assert.equal(c.uploadPreflightSummary.value.text, '还有2项待完善')
  assert.deepEqual(Array.from(c.sortedUploadPreflightChecks.value.slice(0, 2), x => x.key), ['file-version', 'effective-date'])
  assert.equal(c.sortedUploadPreflightChecks.value[0].description, '文件编号已重复')
  c.effectiveSelectionValidation.value = { valid: true }
  assert.equal(c.uploadPreflightSummary.value.text, '还有1项待完善')
})

test('debounced, running, failed and completed number lookups cannot prematurely pass', () => {
  const c = setup()
  c.currentVersionInfo.value = undefined
  assert.equal(c.uploadPreflightChecks.value[0].status, '待检查')
  assert.equal(c.uploadPreflightSummary.value.passed, false)
  assert.doesNotMatch(c.uploadPreflightChecks.value[0].description, /未发现/)
  c.currentVersionLookupLoading.value = true
  assert.equal(c.uploadPreflightChecks.value[0].status, '检查中')
  c.currentVersionLookupLoading.value = false
  c.currentVersionLookupError.value = '查询服务失败'
  assert.equal(c.uploadPreflightSummary.value.text, '还有1项待完善')
  assert.equal(c.uploadPreflightChecks.value[0].description, '查询服务失败')
  c.currentVersionLookupError.value = ''
  c.currentVersionInfo.value = { matched: false }
  assert.equal(c.uploadPreflightSummary.value.text, '7项校验通过')
})

test('missing, uploading and rejected main files cannot report all checks passed and locate the upload field', async () => {
  const c = setup(), calls = []
  c.previewUpload.value = undefined
  assert.equal(c.uploadPreflightSummary.value.text, '还有1项待完善')
  c.uploadPreviewLoading.value = true
  assert.match(c.uploadPreflightSummary.value.text, /1项待检查或检查中/)
  c.uploadPreviewLoading.value = false
  c.uploadPreviewError.value = '文件名称已被占用'
  assert.equal(c.sortedUploadPreflightChecks.value[0].key, 'main-file')
  assert.equal(c.sortedUploadPreflightChecks.value[0].description, '文件名称已被占用')
  assert.equal(c.uploadPreflightSummary.value.passed, false)
  c.formRef.value.fields = [{ prop: 'file', $el: { scrollIntoView: () => calls.push('file'), querySelector: () => null } }]
  await c.locatePreflightField('main-file')
  assert.deepEqual(calls, ['file'])
  c.uploadPreviewError.value = ''
  c.previewUpload.value = { fileName: '新正文.docx' }
  assert.equal(c.uploadPreflightSummary.value.text, '7项校验通过')
})

test('loading and stale readiness never count as approval success; folder failure remains visible', () => {
  const c = setup()
  for (const name of ['routeReadinessLoading', 'approvalUsersLoading', 'signoffDepartmentsLoading']) {
    c[name].value = true
    assert.equal(c.uploadPreflightChecks.value.find(x => x.key === 'approval-chain').ok, false)
    assert.match(c.uploadPreflightSummary.value.text, /检查中/)
    c[name].value = false
  }
  c.routeReadinessError.value = '审批预检失败'
  assert.equal(c.uploadPreflightSummary.value.passed, false)
  c.projectFoldersError.value = '目录无权访问'
  assert.equal(c.uploadPreflightChecks.value.find(x => x.key === 'controlled-browser-directory').description, '目录无权访问')
})

test('locate closes details, scrolls the actual registered field and focuses its input', async () => {
  const c = setup(), calls = []
  c.versionFormatPreflightMessage.value = '版本格式错误'
  c.formRef.value.fields = [{ prop: 'versionNo', $el: {
    scrollIntoView: () => calls.push('scroll'), querySelector: () => ({ focus: () => calls.push('focus') })
  } }]
  c.preflightDetailsVisible.value = true
  await c.locatePreflightField('file-version')
  assert.equal(c.preflightDetailsVisible.value, false)
  assert.deepEqual(calls, ['scroll', 'focus'])
})

test('duplicate number and approval problems locate their actual editable fields', async () => {
  const c = setup(), calls = []
  c.formRef.value.fields = [{ prop: 'fileNumber', $el: { scrollIntoView: () => calls.push('number'), querySelector: () => null } }]
  await c.locatePreflightField('file-version')
  c.formRef.value.$el = { querySelector: selector => ({ scrollIntoView: () => calls.push(selector), querySelector: () => null }) }
  await c.locatePreflightField('approval-chain')
  c.isCustomApprovalSource.value = true
  await c.locatePreflightField('approval-chain')
  assert.deepEqual(calls, ['number', '[data-testid="dcc-upload-signoff-departments"]', '[data-testid="dcc-upload-custom-matrix"]'])
})

test('updated SFC template/script/style compile; footer keeps original submit and draft handlers', () => {
  const { descriptor, errors } = parse(page)
  assert.deepEqual(errors, [])
  assert.doesNotThrow(() => compileScript(descriptor, { id: 'issue07', inlineTemplate: true }))
  assert.deepEqual(compileStyle({ source: descriptor.styles[0].content, filename: 'index.vue', id: 'issue07', scoped: true }).errors, [])
  assert.match(page, /@click="saveUploadDraft"/)
  assert.match(page, /@click="submitForm"/)
  assert.doesNotMatch(page, /upload-preflight-card/)
  assert.match(page, /<details v-if="check.ok">/)
  assert.match(page, /定位到对应字段/)
})
