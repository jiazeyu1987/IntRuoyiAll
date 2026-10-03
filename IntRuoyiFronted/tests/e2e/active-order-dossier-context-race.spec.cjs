const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const { test } = require('node:test')
const { parse } = require('vue/compiler-sfc')

const panelPath = path.resolve(
  __dirname,
  '../../src/views/mes/pro/processpool/components/ActiveOrderSubmissionDetailPanel.vue'
)
const source = fs.readFileSync(panelPath, 'utf8')
const ast = ts.createSourceFile('actual-dossier-panel.ts', parse(source).descriptor.scriptSetup.content, ts.ScriptTarget.Latest, true)

const compile = (value) => ts.transpileModule(value, {
  compilerOptions: { target: ts.ScriptTarget.ES2020 }
}).outputText

const createHarness = ({ mutations = false } = {}) => {
  const pendingLoads = []
  const pendingUploads = []
  const pendingDeletes = []
  const messages = { success: [], error: [] }
  const lifecycle = []
  const dossierFiles = { value: undefined }
  const dossierFileLoading = { value: false }
  const dossierFileError = { value: '' }
  const dossierFileUploadingKey = { value: '' }
  const dossierPreviewDialogVisible = { value: false }
  const selectedDossierPreviewSource = { value: { fileId: 'A' } }
  const selectedDossierPreviewTitle = { value: 'A.pdf' }
  const props = {
    detail: { activeOrderId: 'A' },
    pqcReleaseApplicationId: 'APP-A'
  }
  const context = {
    props,
    computed: (fn) => ({ get value() { return fn() } }),
    useRouter: () => ({}),
    onBeforeUnmount: (callback) => lifecycle.push(callback),
    dossierFiles,
    dossierFileLoading,
    dossierFileError,
    dossierFileUploadingKey,
    dossierPreviewDialogVisible,
    selectedDossierPreviewSource,
    selectedDossierPreviewTitle,
    getActiveOrderDossierFiles: (request) => new Promise((resolve, reject) => {
      pendingLoads.push({ request, resolve, reject })
    }),
    uploadActiveOrderDossierFile: (request) => new Promise((resolve, reject) => {
      pendingUploads.push({ request, resolve, reject })
    }),
    deleteActiveOrderDossierFile: (request) => new Promise((resolve, reject) => {
      pendingDeletes.push({ request, resolve, reject })
    }),
    ElMessage: {
      success: (message) => messages.success.push(message),
      error: (message) => messages.error.push(message)
    },
    ElMessageBox: { confirm: () => Promise.resolve() }
  }
  vm.createContext(context)
  const names = ['createDossierRequestContext', 'dossierRequestContext', 'recordScope', 'auditScopeTypeValue', 'auditScopeIdValue', 'resolveDossierReadContext', 'loadDossierFiles',
    ...(mutations ? ['activeOrderDossierMutationLockReason', 'activeOrderDossierMutationLocked', 'uploadDossierFile', 'deleteDossierFile'] : [])]
  const statements = ast.statements.filter(statement => ts.isVariableStatement(statement) && statement.declarationList.declarations.some(declaration => names.includes(declaration.name.getText(ast))))
  assert.equal(statements.length, names.length, '必须提取实际资料上下文和读写函数，不能复制实现替代测试')
  const unmount = ast.statements.find(statement => ts.isExpressionStatement(statement) && statement.getText(ast).startsWith('onBeforeUnmount(') && statement.getText(ast).includes('dossierRequestContext.invalidate()'))
  assert.ok(unmount, '必须提取实际资料卸载回调')
  const exposed = [
    'globalThis.loadDossierFiles = loadDossierFiles;',
    ...(mutations ? [
      'globalThis.uploadDossierFile = uploadDossierFile;',
      'globalThis.deleteDossierFile = deleteDossierFile;'
    ] : []),
    'globalThis.invalidateDossierContext = () => dossierRequestContext.invalidate();'
  ].join('\n')
  vm.runInContext(compile(`${statements.map(statement => statement.getText(ast)).join('\n')}\n${exposed}`), context)
  vm.runInContext(compile(unmount.getText(ast)), context)
  return {
    ...context,
    pendingLoads,
    pendingUploads,
    pendingDeletes,
    messages,
    lifecycle,
    refs: {
      dossierFiles,
      dossierFileLoading,
      dossierFileError,
      dossierFileUploadingKey,
      dossierPreviewDialogVisible,
      selectedDossierPreviewSource,
      selectedDossierPreviewTitle
    }
  }
}

test('旧请求的成功、异常和 finally 都不得覆盖当前订单上下文', async () => {
  const harness = createHarness()
  const first = harness.loadDossierFiles()
  await Promise.resolve()
  harness.refs.dossierPreviewDialogVisible.value = true
  harness.refs.selectedDossierPreviewSource.value = { fileId: 'A' }
  harness.refs.selectedDossierPreviewTitle.value = 'A.pdf'

  harness.props.detail = { activeOrderId: 'B' }
  harness.props.pqcReleaseApplicationId = 'APP-B'
  const second = harness.loadDossierFiles()
  await Promise.resolve()

  harness.pendingLoads[1].resolve({ context: 'B' })
  await second
  harness.pendingLoads[0].reject(new Error('A 请求失败'))
  await first

  assert.deepEqual(harness.refs.dossierFiles.value, { context: 'B' })
  assert.equal(harness.refs.dossierFileLoading.value, false)
  assert.equal(harness.refs.dossierFileError.value, '')
})

test('旧请求成功晚返回时也不得覆盖当前订单失败状态', async () => {
  const harness = createHarness()
  const first = harness.loadDossierFiles()
  await Promise.resolve()

  harness.props.detail = { activeOrderId: 'B' }
  harness.props.pqcReleaseApplicationId = 'APP-B'
  const second = harness.loadDossierFiles()
  await Promise.resolve()
  assert.equal(harness.refs.dossierFiles.value, undefined)
  assert.equal(harness.refs.dossierPreviewDialogVisible.value, false)
  assert.equal(harness.refs.selectedDossierPreviewSource.value, null)
  assert.equal(harness.refs.selectedDossierPreviewTitle.value, '')

  harness.pendingLoads[1].reject(new Error('B 请求失败'))
  await second
  harness.pendingLoads[0].resolve({ context: 'A' })
  await first

  assert.equal(harness.refs.dossierFiles.value, undefined)
  assert.equal(harness.refs.dossierFileError.value, 'Error: B 请求失败')
  assert.equal(harness.refs.dossierFileLoading.value, false)
})

test('无身份会清空资料、错误和 loading，且旧请求 finally 不能回写', async () => {
  const harness = createHarness()
  const first = harness.loadDossierFiles()
  await Promise.resolve()

  harness.refs.dossierPreviewDialogVisible.value = true
  harness.refs.selectedDossierPreviewSource.value = { fileId: 'A' }
  harness.refs.selectedDossierPreviewTitle.value = 'A.pdf'
  harness.props.detail = undefined
  await harness.loadDossierFiles()
  harness.pendingLoads[0].resolve({ context: 'A' })
  await first

  assert.equal(harness.refs.dossierFiles.value, undefined)
  assert.equal(harness.refs.dossierFileLoading.value, false)
  assert.equal(harness.refs.dossierFileError.value, '')
  assert.equal(harness.refs.dossierPreviewDialogVisible.value, false)
  assert.equal(harness.refs.selectedDossierPreviewSource.value, null)
  assert.equal(harness.refs.selectedDossierPreviewTitle.value, '')
})

test('A 到 B 再回 A 时只接受最后一轮 A 响应', async () => {
  const harness = createHarness()
  const first = harness.loadDossierFiles()
  await Promise.resolve()
  harness.props.detail = { activeOrderId: 'B' }
  harness.props.pqcReleaseApplicationId = 'APP-B'
  const second = harness.loadDossierFiles()
  await Promise.resolve()
  harness.props.detail = { activeOrderId: 'A' }
  harness.props.pqcReleaseApplicationId = 'APP-A'
  const third = harness.loadDossierFiles()
  await Promise.resolve()

  assert.equal(harness.pendingLoads.length, 3)
  harness.pendingLoads[2].resolve({ context: 'A-latest' })
  await third
  harness.pendingLoads[1].resolve({ context: 'B' })
  await second
  harness.pendingLoads[0].resolve({ context: 'A-old' })
  await first

  assert.deepEqual(harness.refs.dossierFiles.value, { context: 'A-latest' })
  assert.equal(harness.refs.dossierFileLoading.value, false)
})

test('同订单切换放行申请时也按申请上下文隔离资料', async () => {
  const harness = createHarness()
  const first = harness.loadDossierFiles()
  await Promise.resolve()
  harness.props.pqcReleaseApplicationId = 'APP-B'
  const second = harness.loadDossierFiles()
  await Promise.resolve()

  assert.equal(harness.pendingLoads.length, 2)
  assert.equal(harness.refs.dossierFiles.value, undefined)
  harness.pendingLoads[1].resolve({ context: 'APP-B' })
  await second
  harness.pendingLoads[0].resolve({ context: 'APP-A' })
  await first

  assert.deepEqual(harness.refs.dossierFiles.value, { context: 'APP-B' })
  assert.equal(harness.refs.dossierFileLoading.value, false)
})

test('组件卸载会失效请求身份，卸载后的响应不能重新填充资料', async () => {
  const harness = createHarness()
  const request = harness.loadDossierFiles()
  await Promise.resolve()
  assert.equal(harness.lifecycle.length, 1)
  harness.lifecycle[0]()
  harness.pendingLoads[0].resolve({ context: 'unmounted' })
  await request

  assert.equal(harness.refs.dossierFiles.value, undefined)
  assert.equal(harness.refs.dossierFileLoading.value, false)
  assert.equal(harness.refs.dossierFileError.value, '')
  assert.equal(harness.refs.dossierPreviewDialogVisible.value, false)
  assert.equal(harness.refs.selectedDossierPreviewSource.value, null)
  assert.equal(harness.refs.selectedDossierPreviewTitle.value, '')
})

test('同订单上传成功后可以刷新资料并清理上传 loading', async () => {
  const harness = createHarness({ mutations: true })
  const initial = harness.loadDossierFiles()
  await Promise.resolve()
  harness.pendingLoads[0].resolve({ context: 'A-before-upload' })
  await initial

  let successCount = 0
  const upload = harness.uploadDossierFile('OTHER_FILE', {
    file: { name: 'a.pdf' },
    onSuccess: () => { successCount += 1 },
    onError: () => {}
  })
  await Promise.resolve()
  harness.pendingUploads[0].resolve()
  await new Promise((resolve) => setImmediate(resolve))
  assert.equal(harness.pendingLoads.length, 2)
  harness.pendingLoads[1].resolve({ context: 'A-after-upload' })
  await upload

  assert.equal(successCount, 1)
  assert.deepEqual(harness.refs.dossierFiles.value, { context: 'A-after-upload' })
  assert.equal(harness.refs.dossierFileUploadingKey.value, '')
  assert.deepEqual(harness.messages.success, ['资料文件已上传'])
})

test('同订单并发刷新不会让上传回调被误判为旧上下文', async () => {
  const harness = createHarness({ mutations: true })
  const initial = harness.loadDossierFiles()
  await Promise.resolve()
  harness.pendingLoads[0].resolve({ context: 'A-before-upload' })
  await initial

  const upload = harness.uploadDossierFile('OTHER_FILE', {
    file: { name: 'a.pdf' },
    onSuccess: () => {},
    onError: () => {}
  })
  await Promise.resolve()
  const externalRefresh = harness.loadDossierFiles()
  await Promise.resolve()
  harness.pendingUploads[0].resolve()
  await new Promise((resolve) => setImmediate(resolve))
  assert.equal(harness.pendingLoads.length, 3)
  harness.pendingLoads[1].resolve({ context: 'A-external-refresh' })
  harness.pendingLoads[2].resolve({ context: 'A-upload-refresh' })
  await externalRefresh
  await upload

  assert.deepEqual(harness.refs.dossierFiles.value, { context: 'A-upload-refresh' })
  assert.equal(harness.refs.dossierFileUploadingKey.value, '')
})

test('上传回调切换到新订单后不得写错误、成功提示或旧 loading', async () => {
  const harness = createHarness({ mutations: true })
  const initial = harness.loadDossierFiles()
  await Promise.resolve()
  harness.pendingLoads[0].resolve({ context: 'A' })
  await initial

  const upload = harness.uploadDossierFile('OTHER_FILE', {
    file: { name: 'a.pdf' },
    onSuccess: () => {},
    onError: () => {}
  })
  await Promise.resolve()
  harness.props.detail = { activeOrderId: 'B' }
  harness.props.pqcReleaseApplicationId = 'APP-B'
  const current = harness.loadDossierFiles()
  await Promise.resolve()
  harness.pendingUploads[0].resolve()
  await upload

  assert.equal(harness.refs.dossierFileUploadingKey.value, '')
  assert.equal(harness.refs.dossierFileError.value, '')
  assert.deepEqual(harness.messages.success, [])
  harness.pendingLoads[1].resolve({ context: 'B' })
  await current
})

test('删除回调切换到新订单后不得写错误或成功提示', async () => {
  const harness = createHarness({ mutations: true })
  const initial = harness.loadDossierFiles()
  await Promise.resolve()
  harness.pendingLoads[0].resolve({ context: 'A' })
  await initial

  const deletion = harness.deleteDossierFile('OTHER_FILE', {
    attachmentId: 'ATT-A',
    fileName: 'a.pdf'
  })
  await new Promise((resolve) => setImmediate(resolve))
  harness.props.detail = { activeOrderId: 'B' }
  harness.props.pqcReleaseApplicationId = 'APP-B'
  const current = harness.loadDossierFiles()
  await Promise.resolve()
  assert.equal(harness.pendingDeletes.length, 1)
  harness.pendingDeletes[0].reject(new Error('A 删除失败'))
  await deletion

  assert.equal(harness.refs.dossierFileError.value, '')
  assert.deepEqual(harness.messages.success, [])
  harness.pendingLoads[1].resolve({ context: 'B' })
  await current
})
