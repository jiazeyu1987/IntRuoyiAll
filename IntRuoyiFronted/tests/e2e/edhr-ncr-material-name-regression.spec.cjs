const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const { nextTick, reactive, watch } = require('vue')
const { test } = require('node:test')
const exactModule = { exports: {} }
vm.runInNewContext(ts.transpileModule(fs.readFileSync(path.resolve(__dirname, '../../src/utils/exactIntegerJson.ts'), 'utf8'), { compilerOptions: { target: ts.ScriptTarget.ES2020, module: ts.ModuleKind.CommonJS } }).outputText, { exports: exactModule.exports })
const { parseExactIntegerJson } = exactModule.exports

const root = path.resolve(__dirname, '..', '..', '..')
const pagePath = path.join(root, 'IntRuoyiFronted/src/views/mes/pro/edhr-nonconformance/NonconformanceReviewPage.vue')
const uploadPath = path.join(root, 'IntRuoyiFronted/src/components/UploadFile/src/UploadFile.vue')
const detailPath = path.join(root, 'IntRuoyiFronted/src/views/mes/pro/processpool/components/ActiveOrderSubmissionDetailPanel.vue')
const utilityPath = path.join(root, 'IntRuoyiFronted/src/utils/fileName.ts')

const execute = (source, startMarker, endMarker, exports, context = {}) => {
  const start = source.indexOf(startMarker)
  const end = source.indexOf(endMarker, start)
  assert(start >= 0 && end > start, `cannot extract ${startMarker}`)
  const sandbox = { parseExactIntegerJson, URL, window: { location: { origin: 'http://localhost' } }, ...context }
  vm.createContext(sandbox)
  const exposed = `${source.slice(start, end)}\n${exports}`
  vm.runInContext(
    ts.transpileModule(exposed, { compilerOptions: { target: ts.ScriptTarget.ES2020 } }).outputText,
    sandbox
  )
  return sandbox
}

const pageSource = fs.readFileSync(pagePath, 'utf8').replace(/\r\n/g, '\n')
const uploadSource = fs.readFileSync(uploadPath, 'utf8')
const detailSource = fs.readFileSync(detailPath, 'utf8')
const utilitySource = fs.readFileSync(utilityPath, 'utf8').replace(/export const /g, 'const ')
const utility = { URL, window: { location: { origin: 'http://localhost' } } }
vm.createContext(utility)
vm.runInContext(
  ts.transpileModule(`${utilitySource}\nglobalThis.resolveUrlPathFileName = resolveUrlPathFileName;`, {
    compilerOptions: { target: ts.ScriptTarget.ES2020 }
  }).outputText,
  utility
)

const page = execute(
  pageSource,
  'const resolveFileName =',
  'const resolveReviewMaterialDisplay =',
  'globalThis.resolveReviewMaterialName = resolveReviewMaterialName;',
  { resolveUrlPathFileName: utility.resolveUrlPathFileName }
)
const upload = execute(
  uploadSource,
  'const resolveUploadFileName =',
  '// 文件上传之前判断',
  'globalThis.resolveUploadFileName = resolveUploadFileName;',
  { resolveUrlPathFileName: utility.resolveUrlPathFileName }
)
const detail = execute(
  detailSource,
  'const resolveNonconformanceReviewMaterials =',
  'const deleteDossierFile =',
  'globalThis.resolveNonconformanceReviewMaterials = resolveNonconformanceReviewMaterials;',
  { resolveUrlPathFileName: utility.resolveUrlPathFileName }
)
const uploadEvents = []
const uploadBehavior = execute(
  uploadSource,
  'const handleFileSuccess:',
  '</script>',
  'globalThis.handleFileSuccess = handleFileSuccess; globalThis.handleRemove = handleRemove; globalThis.fileList = fileList; globalThis.uploadList = uploadList; globalThis.uploadNumber = uploadNumber;',
  {
    message: { success: () => {}, error: () => {} },
    emit: (event, value) => uploadEvents.push({ event, value }),
    fileList: { value: [] },
    uploadList: { value: [] },
    uploadNumber: { value: 0 },
    fileNameByUrl: new Map(),
    props: { limit: 5, modelValue: [], fileNames: {} },
    watch: () => {},
    isString: (value) => typeof value === 'string'
  }
)
const parentEvents = []
const parentDisposeForm = reactive({
  reviewMaterialUrls: [],
  reviewMaterialNames: {},
  reviewMaterialIds: {},
  reviewMaterialEvents: []
})
const parentWatcherStart = pageSource.indexOf('watch(\n  () => [...disposeForm.reviewMaterialUrls]')
const parentWatcherEnd = pageSource.indexOf('\n\nwatch(', parentWatcherStart + 10)
assert(parentWatcherStart >= 0 && parentWatcherEnd > parentWatcherStart, 'cannot extract NCR material watcher')
const parentWatcher = {
  disposeForm: parentDisposeForm,
  materialTrackingEnabled: true,
  materialEventSequence: 0,
  watch,
  parentEvents
}
vm.createContext(parentWatcher)
vm.runInContext(
  ts.transpileModule(
    `${pageSource.slice(parentWatcherStart, parentWatcherEnd)}\nglobalThis.disposeForm = disposeForm;`,
    { compilerOptions: { target: ts.ScriptTarget.ES2020 } }
  ).outputText,
  parentWatcher
)

const urls = {
  literalPercent: '/files/100%.pdf',
  encodedPercent: '/files/100%25.pdf',
  plus: '/files/100+%E5%A4%8D%E6%A0%B8.pdf',
  encodedChinese: '/files/%E4%B8%8D%E5%90%88%E6%A0%BC%20%E8%AF%84%E5%AE%A1.xlsx',
  doubleEncodedChinese: '/files/%25E4%25B8%258D%25E5%2590%2588%25E6%25A0%25BC%2520%25E8%25AF%2584%25E5%25AE%25A1.xlsx'
}

test('NCR URL fallback uses one pathname decode and preserves legal percent/plus', () => {
  assert.equal(page.resolveReviewMaterialName({ url: urls.literalPercent }), '100%.pdf')
  assert.equal(page.resolveReviewMaterialName({ url: urls.encodedPercent }), '100%.pdf')
  assert.equal(page.resolveReviewMaterialName({ url: urls.plus }), '100+复核.pdf')
  assert.equal(page.resolveReviewMaterialName({ url: urls.encodedChinese }), '不合格 评审.xlsx')
  assert.equal(page.resolveReviewMaterialName({ url: urls.doubleEncodedChinese }), '%E4%B8%8D%E5%90%88%E6%A0%BC%20%E8%AF%84%E5%AE%A1.xlsx')
})

test('NCR persisted FileDO names are displayed verbatim', () => {
  for (const fileName of [
    '100%.pdf',
    '100%25.pdf',
    '%AB.pdf',
    '%E6.pdf',
    '100+复核.pdf',
    '%E4%B8%8D%E5%90%88%E6%A0%BC.pdf'
  ]) {
    assert.equal(page.resolveReviewMaterialName({ url: '/files/other.pdf', fileName }), fileName)
  }
})

test('shared UploadFile URL fallback has the same explicit boundary', () => {
  assert.equal(upload.resolveUploadFileName(urls.literalPercent), '100%.pdf')
  assert.equal(upload.resolveUploadFileName(urls.encodedPercent), '100%.pdf')
  assert.equal(upload.resolveUploadFileName(urls.plus), '100+复核.pdf')
  assert.equal(upload.resolveUploadFileName(urls.encodedChinese), '不合格 评审.xlsx')
  assert.equal(upload.resolveUploadFileName(urls.doubleEncodedChinese), '%E4%B8%8D%E5%90%88%E6%A0%BC%20%E8%AF%84%E5%AE%A1.xlsx')
})

test('active order detail keeps persisted names and uses URL fallback once', () => {
  const resolve = (fileName, url) => detail.resolveNonconformanceReviewMaterials({
    reviewMaterialsJson: JSON.stringify({ activeMaterials: [{ fileId: 11, fileName, url }] })
  })[0].fileName
  assert.equal(resolve('100%.pdf', '/files/other.pdf'), '100%.pdf')
  assert.equal(resolve('100+复核.pdf', '/files/other.pdf'), '100+复核.pdf')
  assert.equal(resolve(undefined, urls.encodedPercent), '100%.pdf')
  assert.equal(resolve(undefined, urls.plus), '100+复核.pdf')
})

test('UploadFile preserves upload originals and removes same-name files by URL identity', () => {
  const normalize = (value) => JSON.parse(JSON.stringify(value))
  uploadEvents.length = 0
  uploadBehavior.fileList.value = [{ uid: 1, name: 'pending.pdf' }]
  uploadBehavior.uploadNumber.value = 1
  uploadBehavior.handleFileSuccess(
    { data: '/files/100%25.pdf' },
    { uid: 1, name: '100%25.pdf' }
  )
  assert.deepEqual(normalize(uploadEvents[0]), {
    event: 'update:fileNames',
    value: { '/files/100%25.pdf': '100%25.pdf' }
  })
  assert.deepEqual(normalize(uploadEvents[1]), {
    event: 'update:modelValue',
    value: ['/files/100%25.pdf']
  })

  uploadEvents.length = 0
  uploadBehavior.fileList.value = [
    { uid: 11, name: '相同.pdf' },
    { uid: 12, name: '相同.pdf' }
  ]
  uploadBehavior.uploadList.value = []
  uploadBehavior.uploadNumber.value = 2
  uploadBehavior.handleFileSuccess({ data: '/files/one.pdf' }, { uid: 11, name: '相同.pdf' })
  uploadBehavior.handleFileSuccess({ data: '/files/two.pdf' }, { uid: 12, name: '相同.pdf' })
  uploadEvents.length = 0
  uploadBehavior.handleRemove({ uid: 12, name: '相同.pdf', url: '/files/two.pdf' })
  assert.deepEqual(normalize(uploadEvents[0]), {
    event: 'update:modelValue',
    value: ['/files/one.pdf']
  })
  assert.deepEqual(normalize(uploadEvents[1]), {
    event: 'update:fileNames',
    value: { '/files/one.pdf': '相同.pdf' }
  })
  assert.equal(uploadBehavior.fileList.value[0].url, '/files/one.pdf')
})

test('NCR Vue watcher records DELETE with the old authoritative name before cleanup', async () => {
  parentDisposeForm.reviewMaterialEvents = []
  parentDisposeForm.reviewMaterialNames = { '/files/one.pdf': '相同.pdf', '/files/two.pdf': '相同.pdf' }
  parentDisposeForm.reviewMaterialIds = { '/files/one.pdf': '9007199254740993', '/files/two.pdf': '9007199254740994' }
  parentDisposeForm.reviewMaterialUrls = ['/files/one.pdf', '/files/two.pdf']
  assert.deepEqual(
    parentDisposeForm.reviewMaterialEvents.map(({ action, url, fileName }) => ({ action, url, fileName })),
    [
      { action: 'UPLOAD', url: '/files/one.pdf', fileName: '相同.pdf' },
      { action: 'UPLOAD', url: '/files/two.pdf', fileName: '相同.pdf' }
    ]
  )

  parentDisposeForm.reviewMaterialEvents = []
  parentDisposeForm.reviewMaterialUrls = ['/files/one.pdf']
  assert.deepEqual(
    parentDisposeForm.reviewMaterialEvents.map(({ action, url, fileName }) => ({ action, url, fileName })),
    [{ action: 'DELETE', url: '/files/two.pdf', fileName: '相同.pdf' }]
  )
  parentDisposeForm.reviewMaterialNames = { '/files/one.pdf': '相同.pdf' }
  await nextTick()
})

console.log('PASS: EDHR NCR material name regression contract')
