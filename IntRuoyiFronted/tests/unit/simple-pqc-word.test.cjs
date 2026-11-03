const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')
const ts = require('typescript')
const PizZip = require('pizzip')
const { test } = require('node:test')
const context = { exports: {}, require, TextEncoder, TextDecoder, Uint8Array, btoa, atob }
vm.runInNewContext(
  ts.transpileModule(fs.readFileSync('src/views/form-center/parser/simplePqcWord.ts', 'utf8'), {
    compilerOptions: {
      module: ts.ModuleKind.CommonJS,
      target: ts.ScriptTarget.ES2022,
      esModuleInterop: true
    }
  }).outputText,
  context
)
const { createPqcWord, readPqcWord } = context.exports
const draftContext = { exports: {} }
vm.runInNewContext(
  ts.transpileModule(fs.readFileSync('src/views/form-center/parser/simplePqc.ts', 'utf8'), {
    compilerOptions: { module: ts.ModuleKind.CommonJS }
  }).outputText,
  draftContext
)
const draft = {
  dccProjectCodeId: 129,
  expectedDraftRevision: 0,
  ownerModule: 'MES_QA',
  regulationCode: 'PQC-001',
  regulationName: '清洗 & 压力 <检验> 😀',
  versionNo: 'B/2',
  effectiveDate: '2026-11-02',
  finalInspectionApplicable: true,
  finalInspectionNotApplicableReason: '',
  inspectionTypeRules: [
    {
      key: 'FINAL',
      inspectionType: 'FINAL',
      label: '末检',
      required: true,
      fixedQuantity: 3,
      roundLabel: '末轮',
      taskRule: '检验',
      releaseGate: '全合格'
    }
  ],
  processes: [
    {
      processCode: 'P2',
      processName: '组装',
      sort: 1,
      items: [
        {
          itemCode: 'I9',
          itemSort: 1,
          itemName: '压力',
          standardText: '无泄漏\n压力≤10',
          inspectionMethod: '观察\t10s',
          inspectionTool: '压力计',
          samplingPlanText: '全检',
          resultType: 'NUMERIC',
          applicableInspectionTypes: ['FIRST', 'PATROL', 'FINAL'],
          firstInspectionQuantity: 2,
          patrolInspectionRatio: 12.5,
          standardLowerLimit: 0,
          standardUpperLimit: 10,
          standardUnit: 'MPa',
          standardPrecision: 2,
          critical: true,
          failureRule: '隔离',
          sourceOriginalExcerpt: '原文',
          sourceOriginalPage: 3,
          equipmentOptions: [
            {
              equipmentId: 42,
              equipmentCode: 'EQ-42',
              equipmentNumber: '42号',
              equipmentName: '压力计',
              defaultFlag: true,
              sort: 1
            }
          ]
        },
        {
          itemCode: 'I3',
          itemSort: 2,
          itemName: '外观',
          standardText: '',
          inspectionMethod: '',
          inspectionTool: '',
          samplingPlanText: '',
          resultType: 'TEXT',
          applicableInspectionTypes: [],
          equipmentOptions: [],
          equipmentUnboundReason: '秒表台账未登记，执行前补齐绑定',
          critical: false
        }
      ]
    },
    { processCode: 'P1', processName: '空工序', sort: 2, items: [] }
  ]
}
const bytes = createPqcWord(draft, '7', ['I3'])
test('exported DOCX restores all edited fields, order, provenance and equipment exactly', () => {
  const restored = readPqcWord(bytes, '7', 129)
  assert.equal(JSON.stringify(restored.draft), JSON.stringify(draft))
  assert.equal(JSON.stringify(restored.confirmedUnbound), '["I3"]')
  const zip = new PizZip(bytes)
  const xml = zip.file('word/document.xml').asText()
  assert.match(xml, /清洗 &amp; 压力 &lt;检验&gt; 😀/)
  assert.match(xml, /42号 \/ 压力计/)
  assert.match(xml, /系统巡检抽样比例（%）：12.5/)
  assert.match(xml, /器具未绑定原因：秒表台账未登记，执行前补齐绑定/)
  assert.match(xml, /<w:br\/>/)
  assert.ok(zip.file('[Content_Types].xml'))
})
test('ordinary Word is recognized separately; invalid exports and foreign bindings fail explicitly', () => {
  const ordinary = new PizZip()
  ordinary.file('word/document.xml', '<document/>')
  assert.equal(readPqcWord(ordinary.generate({ type: 'uint8array' }), '7', 129), null)
  assert.throws(() => readPqcWord(bytes, '8', 129), /其他租户/)
  assert.throws(() => readPqcWord(bytes, '7', 130), /所属产品/)
  const broken = new PizZip(bytes)
  broken.file('customXml/simplePqc.xml', '<invalid/>')
  assert.throws(() => readPqcWord(broken.generate({ type: 'uint8array' }), '7', 129), /格式无效/)
  broken.remove('customXml/simplePqc.xml')
  assert.throws(() => readPqcWord(broken.generate({ type: 'uint8array' }), '7', 129), /数据缺失/)
})
// Optional artifact for native Word-format validation, outside the repository.
if (process.env.PQC_WORD_TEST_OUTPUT) fs.writeFileSync(process.env.PQC_WORD_TEST_OUTPUT, bytes)

for (const common of [false, true])
  test(`${common ? 'common' : 'PQC'} page restores DOCX exports and sends binary DOC directly for recognition`, async () => {
    const { parse, compileScript } = require('vue/compiler-sfc')
    const vue = require('vue')
    const filename = `src/views/form-center/${common ? 'simple-common-parser' : 'simple-pqc-parser'}/index.vue`
    const expectedDraft = { ...draft, ownerModule: common ? 'MES_QA_COMMON' : 'MES_QA' }
    const exportedBytes = createPqcWord(expectedDraft, '7', common ? [] : ['I3'])
    const descriptor = parse(fs.readFileSync(filename, 'utf8'), { filename }).descriptor
    const imports = {
      '../parser/components/ParserProcessToolbar.vue': {}, '../parser/components/ParserTrialNavigation.vue': {}, '../parser/components/ParserIssueList.vue': {},
      './components/ParserProcessToolbar.vue': {}, './components/ParserTrialNavigation.vue': {}, './components/ParserIssueList.vue': {},
      '../parser/useParserEditing': loadEditing(vue), './useParserEditing': loadEditing(vue),
      vuedraggable: {},
      vue,
      'vue-router': { useRouter: () => ({}) },
      './draft': {},
      '@/api/mes/qc/template/simpleCommonParser': {
        SimpleCommonParserApi: {
          parse: () => {
            throw new Error('Common export must not be recognized again')
          }
        }
      },
      '../parser/components/SimpleParserHeader.vue': {},
      '../parser/components/SimplePqcTrialPreview.vue': {},
      '../parser/components/ParserRegulationResult.vue': {},
      '@/api/mes/qc/template': {
        QcTemplateApi: {
          parseQaInspectionRegulationJson: () => {
            throw new Error('Export must not be recognized again')
          },
          getCurrentQaRegulation: async () => null,
          listQaRegulationVersions: async () => [],
          listCommonRegulationPublishedVersions: async () => []
        }
      },
      '@/api/mes/dv/machinery': {
        DvMachineryApi: { getMachineryPage: async () => ({ list: [], total: 0 }) }
      },
      '@/api/dcc/controlledFile/projectCodes': {
        getProjectCodePage: async () => ({ list: [] }),
        getProjectCode: async () => ({ id: 129, productMasterId: 8 })
      },
      '@/utils/auth': { getTenantId: () => '7' },
      '@/store/modules/user': {},
      '../parser/simplePqc': draftContext.exports,
      '../parser/simplePqcWord': context.exports
    }
    const errors = []
    const pageContext = {
      exports: {},
      require: (name) => {
        if (name in imports) return imports[name]
        throw new Error(name)
      },
      useMessage: () => ({ confirm: async () => {}, error: (message) => errors.push(message) })
    }
    vm.runInNewContext(
      ts.transpileModule(compileScript(descriptor, { id: 'word-import' }).content, {
        compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
      }).outputText,
      pageContext
    )
    const state = pageContext.exports.default.setup({}, { expose() {} })
    state.projectId.value = 129
    state.draft.value = { processes: [] }
    await state.importWord({ raw: { name: '导出规程.docx', arrayBuffer: async () => exportedBytes } })
    assert.deepEqual(errors, [])
    assert.equal(JSON.stringify(state.draft.value), JSON.stringify(expectedDraft))
    assert.equal(state.draft.value.versionNo, 'B/2')
    if (!common) assert.equal(state.confirmedUnbound.value[0], 'I3')
    assert.equal(state.published.value, false)

    let recognized = 0
    const docFile = { name: '源规程.DOC', arrayBuffer: async () => {
      throw new Error('Binary DOC must not be opened as a DOCX snapshot')
    } }
    const parseDoc = async (file, projectId) => {
      assert.equal(file, docFile)
      if (common) assert.equal(projectId, 129)
      recognized++
      return { ...expectedDraft, regulationName: 'DOC 识别规程' }
    }
    imports['@/api/mes/qc/template'].QcTemplateApi.parseQaInspectionRegulationJson = parseDoc
    imports['@/api/mes/qc/template/simpleCommonParser'].SimpleCommonParserApi.parse = parseDoc
    await state.importWord({ raw: docFile })
    assert.deepEqual(errors, [])
    assert.equal(recognized, 1)
    assert.equal(state.draft.value.regulationName, 'DOC 识别规程')
    assert.equal(state.draft.value.processes.length, expectedDraft.processes.length)
    const recognizedDraft = state.draft.value
    await state.importWord({ raw: { name: '规程.pdf' } })
    assert.equal(errors.pop(), '请选择 DOC 或 DOCX 文件')
    assert.equal(recognized, 1)
    assert.equal(state.draft.value, recognizedDraft)
  })

test('common Word uses the correct page and preserves the entire common draft', () => {
  const commonDraft = {
    ...draft,
    ownerModule: 'MES_QA_COMMON',
    regulationId: 123,
    expectedDraftRevision: 4
  }
  const commonBytes = createPqcWord(commonDraft, '7', [])
  assert.equal(
    JSON.stringify(readPqcWord(commonBytes, '7', 129, 'MES_QA_COMMON').draft),
    JSON.stringify(commonDraft)
  )
  assert.throws(() => readPqcWord(commonBytes, '7', 129), /规程类型不一致/)
  assert.throws(() => readPqcWord(bytes, '7', 129, 'MES_QA_COMMON'), /规程类型不一致/)
  assert.throws(() => readPqcWord(commonBytes, '7', 130, 'MES_QA_COMMON'), /所属项目/)
  assert.throws(() => readPqcWord(commonBytes, '8', 129, 'MES_QA_COMMON'), /其他租户/)
  assert.match(new PizZip(commonBytes).file('word/document.xml').asText(), /简易通用解析/)
})

function loadEditing(vue) {
  const target = { exports: {}, require: () => vue }
  vm.runInNewContext(ts.transpileModule(fs.readFileSync('src/views/form-center/parser/useParserEditing.ts', 'utf8'), { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText, target)
  return target.exports
}
