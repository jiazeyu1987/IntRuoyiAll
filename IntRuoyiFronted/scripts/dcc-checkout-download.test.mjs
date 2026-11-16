import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { parse, compileScript, compileTemplate } from 'vue/compiler-sfc'

const detail = readFileSync(new URL('../src/views/dcc/controlled-file/detail/index.vue', import.meta.url), 'utf8')
const expression = detail.match(/const checkoutDownloadAllowed = computed\(\(\) => ([\s\S]*?)\)\r?\nconst controlledDownloadLoading/)[1]
const allowed = (changes = {}) => {
  const context = { viewerMode: { value: false }, showDetailManagementActions: { value: true },
    route: { query: { management: '1' } }, isWorkingBrowserDetailCurrent: { value: true },
    fileDetail: { value: { canDownloadCheckedOut: true } }, ...changes }
  return new Function(...Object.keys(context), `return (${expression})`)(...Object.values(context))
}
test('only the current maintenance detail with server checkout authority exposes download', () => {
  assert.equal(allowed(), true)
  assert.equal(allowed({ route: { query: {} } }), false)
  assert.equal(allowed({ viewerMode: { value: true } }), false)
  assert.equal(allowed({ showDetailManagementActions: { value: false } }), false)
  assert.equal(allowed({ isWorkingBrowserDetailCurrent: { value: false } }), false)
  assert.equal(allowed({ fileDetail: { value: { canDownloadCheckedOut: false } } }), false)
})
for (const name of ['index.vue', 'ControlledFileAttachments.vue', '../browser/index.vue']) {
  test(`${name} compiles after wiring checkout Word and attachment downloads`, () => {
    const filename = new URL(`../src/views/dcc/controlled-file/detail/${name}`, import.meta.url).pathname
    const { descriptor, errors } = parse(readFileSync(new URL(`../src/views/dcc/controlled-file/detail/${name}`, import.meta.url), 'utf8'), { filename })
    assert.deepEqual(errors, [])
    const script = compileScript(descriptor, { id: 'checkout-download' })
    const template = compileTemplate({ source: descriptor.template.content, filename, id: 'checkout-download', compilerOptions: { bindingMetadata: script.bindings } })
    assert.deepEqual(template.errors, [])
  })
}

const downloadHandler = detail.match(/const handleDownloadControlledBody = async \(\) => \{([\s\S]*?)\n\}\r?\n\r?\nconst handleDetailMoreCommand/)[1]
const invokeDownload = async ({ checkout, ordinary, busy = false }) => {
  const calls = []
  const context = {
    controlledDownloadLoading: { value: busy }, checkoutDownloadAllowed: { value: checkout },
    controlledDownloadAllowed: { value: ordinary }, controlledFileId: { value: '20' },
    handleDownloadCheckoutWord: async () => { calls.push('word') },
    triggerControlledFileDownload: async () => { calls.push('pdf') },
    ensureDetailActionAllowed: () => { calls.push('ordinary-permission'); return true },
    message: { warning: () => calls.push('denied'), error: () => calls.push('error') }
  }
  await new Function(...Object.keys(context), `return (async () => {${downloadHandler}})()`)(...Object.values(context))
  return calls
}
test('existing download-body action chooses Word for own checkout even without ordinary download permission', async () => {
  assert.deepEqual(await invokeDownload({ checkout: true, ordinary: false }), ['word'])
  assert.deepEqual(await invokeDownload({ checkout: true, ordinary: true }), ['word'])
  assert.deepEqual(await invokeDownload({ checkout: false, ordinary: false }), ['denied'])
  assert.deepEqual(await invokeDownload({ checkout: false, ordinary: true }), ['ordinary-permission', 'pdf'])
  assert.deepEqual(await invokeDownload({ checkout: true, ordinary: true, busy: true }), [])
})
test('More menu reuses Download body without a separate checkout Word item', () => {
  assert.match(detail, /v-if="checkoutDownloadAllowed \|\| controlledDownloadAllowed"[^>]+handleDetailMoreCommand\('download-body'\)[^>]*>下载正文/)
  assert.doesNotMatch(detail, />下载检出 Word<\//)
})

const browser = readFileSync(new URL('../src/views/dcc/controlled-file/browser/index.vue', import.meta.url), 'utf8')
const maintenanceRule = browser.match(/const canOpenCheckoutMaintenance = \(row: ControlledFileBrowserRow\) =>([\s\S]*?)\r?\n\r?\nconst openCheckoutMaintenance/)[1]
const maintenanceAllowed = ({ mode = 'working', selected = { id: '20', status: 'WORKING' }, owner = '99' } = {}) => {
  const context = { browserMode: { value: mode }, isValidBrowserOptionId: id => /^[1-9]\d*$/.test(id),
    getSelectedVersion: () => selected, getVersionOptions: () => [{ id: '21', checkedOutBy: owner }],
    isCheckedOutByCurrentUser: file => String(file.checkedOutBy) === '99', row: {} }
  return new Function(...Object.keys(context), `return (${maintenanceRule})`)(...Object.values(context))
}
test('checkout owner can enter selected draft and active maintenance from working browser only', () => {
  assert.equal(maintenanceAllowed(), true)
  assert.equal(maintenanceAllowed({ selected: { id: '20', status: 'ACTIVE' } }), true)
  assert.equal(maintenanceAllowed({ owner: '88' }), false)
  assert.equal(maintenanceAllowed({ owner: null }), false)
  assert.equal(maintenanceAllowed({ mode: 'controlled' }), false)
  assert.equal(maintenanceAllowed({ selected: { id: '20', status: 'OBSOLETE' } }), false)
  assert.equal(maintenanceAllowed({ selected: { id: '20', status: 'SUPERSEDED' } }), false)
})
test('maintenance opens the selected iteration instead of the old working route identity', () => {
  const handler = browser.match(/const openCheckoutMaintenance = \(row: ControlledFileBrowserRow\) => \{([\s\S]*?)\r?\n\}/)[1]
  const opened = []
  new Function('row', 'canOpenCheckoutMaintenance', 'openManagement', 'getSelectedVersion', handler)(
    {}, () => true, id => opened.push(id), () => ({ id: '2054545668044084629' }))
  assert.deepEqual(opened, ['2054545668044084629'])
  new Function('row', 'canOpenCheckoutMaintenance', 'openManagement', 'getSelectedVersion', handler)(
    {}, () => false, id => opened.push(id), () => ({ id: 'other' }))
  assert.deepEqual(opened, ['2054545668044084629'])
})

const checkoutLockExpression = detail.match(/const fileChangesLocked = computed\(\(\) => (.*?)\)\r?\n/)[1]
const prepareExpression = detail.match(/const canPrepareFileApplication = computed\(\(\) => ([\s\S]*?)\)\r?\nconst applicationRestartLoading/)[1]
test('checkout locks application preparation for every selected version while retaining Word downloads', () => {
  for (const status of ['WORKING', 'ACTIVE', 'CONTROLLED_PENDING_EFFECTIVE']) {
    for (const checkedOut of [true, false]) {
      const fileDetail = { value: { status, checkedOut, checkedOutBy: null, canEditFile: true } }
      const fileChangesLocked = { value: new Function('fileDetail', `return (${checkoutLockExpression})`)(fileDetail) }
      const canPrepare = new Function('fileDetail', 'fileChangesLocked', `return (${prepareExpression})`)(fileDetail, fileChangesLocked)
      assert.equal(canPrepare, !checkedOut, `${status} checkout=${checkedOut}`)
    }
  }
  assert.equal(allowed(), true)
  assert.match(detail, /:allow-edit="showDetailManagementActions && !fileChangesLocked && !isExternalReviewProcess"/)
  assert.match(detail, /const canSubmitObsoleteAction = computed\([\s\S]*?!fileChangesLocked.value && dccObsoleteActionProjection.value.allowed/)
  assert.match(detail, /const canSubmitPublishAction = computed\([\s\S]*?!fileChangesLocked.value && dccPublishActionProjection.value.allowed/)
})
