const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const { test } = require('node:test')

const source = fs.readFileSync(path.resolve(__dirname, '../../src/views/mes/pro/processpool/components/ActiveOrderSubmissionDetailPanel.vue'), 'utf8')
const start = source.indexOf('type NonconformanceReviewMaterialDisplay =')
const end = source.indexOf('const deleteDossierFile =', start)
assert(start >= 0 && end > start, '评审事实必须解析完整材料清单并逐份保留名称与文件 ID')
const preview = { value: null }
const title = { value: '' }
const visible = { value: false }
const errors = []
const context = {
  URL, window: { location: { origin: 'http://localhost' } },
  selectedDossierPreviewSource: preview, selectedDossierPreviewTitle: title,
  dossierPreviewDialogVisible: visible, ElMessage: { error: (value) => errors.push(value) },
  buildMesEdhrNonconformanceReviewMaterialPreviewSource: (fileId) => ({ fileId })
}
vm.createContext(context)
vm.runInContext(ts.transpileModule(source.slice(start, end) + '\nglobalThis.resolveMaterials = resolveNonconformanceReviewMaterials; globalThis.previewMaterial = previewNonconformanceReviewMaterial;', { compilerOptions: { target: ts.ScriptTarget.ES2020 } }).outputText, context)
const resolve = (materials) => context.resolveMaterials({ reviewMaterialsJson: JSON.stringify({ activeMaterials: materials }) })

test('单份原始中文名称及字面百分号、加号保持不变', () => {
  for (const fileName of ['不合格评审材料.xlsx', '100%合格+复核报告.pdf']) {
    assert.equal(resolve([{ fileId: 11, fileName, url: '/files/internal.pdf' }])[0].fileName, fileName)
  }
})
test('多份材料完整保留顺序并逐份打开对应正式文件', () => {
  const files = resolve([{ fileId: 11, fileName: '评审意见.pdf' }, { fileId: 22, fileName: '返工方案.docx' }])
  assert.equal(files.length, 2)
  for (const file of files) {
    context.previewMaterial(file)
    assert.equal(preview.value.fileId, file.fileId)
    assert.equal(title.value, file.fileName)
    assert.equal(visible.value, true)
  }
})
test('编码及重复编码中文名称正常显示', () => {
  const name = '不合格 评审.xlsx'
  for (const fileName of [encodeURIComponent(name), encodeURIComponent(encodeURIComponent(name))]) {
    assert.equal(resolve([{ fileId: 11, fileName }])[0].fileName, name)
  }
})
test('URL 名称只解码路径，排除查询和片段，处理上传路径加号', () => {
  const url = '/files/%25E4%25B8%258D%25E5%2590%2588%25E6%25A0%25BC%252B%25E8%25AF%2584%25E5%25AE%25A1.xlsx?download=1#preview'
  assert.equal(resolve([{ fileId: 11, url }])[0].fileName, '不合格 评审.xlsx')
  assert.equal(context.resolveMaterials({ reviewMaterialUrl: '/files/中文.pdf', reviewMaterialFileId: 11 })[0].fileName, '中文.pdf')
})
test('正式空清单不被标量补齐，坏清单明确失败', () => {
  assert.equal(context.resolveMaterials({}).length, 0)
  assert.equal(context.resolveMaterials({ reviewMaterialsJson: '{"activeMaterials":[]}', reviewMaterialUrl: '/old.pdf', reviewMaterialFileId: 1 }).length, 0)
  assert.throws(() => context.resolveMaterials({ reviewMaterialsJson: '{bad' }))
  assert.throws(() => context.resolveMaterials({ reviewMaterialsJson: '{}' }), /评审材料/)
})
test('缺少正式文件编号时明确报错且不打开预览', () => {
  visible.value = false
  context.previewMaterial(resolve([{ fileName: '未关联材料.pdf' }])[0])
  assert.equal(visible.value, false)
  assert.match(errors.at(-1), /缺少文件编号/)
})
test('模板按材料逐份渲染名称、绑定对应预览并保留创建事实边界', () => {
  const block = source.slice(source.indexOf('data-active-order-operation-ncr-evidence'), source.indexOf('</tr>', source.indexOf('data-active-order-operation-ncr-evidence')))
  assert.match(block, /v-for="\(material, materialIndex\) in resolveNonconformanceReviewMaterials\(fact\)"/)
  assert.match(block, /\{\{ material\.fileName \}\}/)
  assert.match(block, /@click="previewNonconformanceReviewMaterial\(material\)"/)
  assert.match(block, /fact\.operationType !== 'NONCONFORMANCE_REVIEW_CREATE'/)
  assert.doesNotMatch(block, /查看评审材料/)
})
