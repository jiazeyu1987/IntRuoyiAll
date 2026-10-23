const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs'),
  path = require('node:path'),
  vm = require('node:vm'),
  ts = require('typescript'),
  vue = require('vue')
const { parse } = require('vue/compiler-sfc')
const root = path.resolve(__dirname, '../..')
function module(file, requireModule) {
  const exports = {}
  vm.runInNewContext(
    ts.transpileModule(fs.readFileSync(path.join(root, file), 'utf8'), {
      compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
    }).outputText,
    { exports, require: requireModule, String, Number, Date, Map, Set, JSON, RegExp, Array }
  )
  return exports
}
const labels = module('src/views/dcc/controlled-file/shared/signature-evidence.ts', () => {
  throw Error('Unexpected import')
})
test('native signature actions read as exact Chinese meaning without hiding unknown codes', () => {
  assert.equal(labels.getDccSignatureTaskActionLabel('ASSIGN'), '会签指派')
  assert.equal(labels.getDccSignatureTaskActionLabel('ASSIGNED'), '会签指派')
  assert.equal(labels.getDccSignatureMeaningLabel('MATRIX_REVIEW_ASSIGN'), '会签指派')
  assert.equal(labels.getDccSignatureTaskActionLabel('APPROVE'), '审批通过')
  assert.equal(labels.getDccSignatureTaskActionLabel('REJECT'), '审批驳回')
  assert.equal(labels.getDccSignatureTaskActionLabel('UNKNOWN'), '未识别签名动作（UNKNOWN）')
})
test('signature display separates business opinion from immutable machine evidence', () => {
  const raw = '正文批准；批准文件负责人事实：[1,"admin","张三"]'
  assert.equal(labels.getDccSignatureOpinionLabel(raw), '正文批准')
  assert.equal(raw, '正文批准；批准文件负责人事实：[1,"admin","张三"]')
  assert.equal(labels.getDccSignatureOpinionLabel('原意；含普通分号'), '原意；含普通分号')
})
test('complete detail evidence and trace summary use the same opinion display helper', () => {
  const source = fs.readFileSync(
    path.join(root, 'src/views/dcc/controlled-file/detail/index.vue'),
    'utf8'
  )
  const { descriptor } = parse(source)
  const ast = ts.createSourceFile(
    'detail.ts',
    descriptor.scriptSetup.content,
    ts.ScriptTarget.Latest,
    true
  )
  const fn = ast.statements.find(
    (s) =>
      ts.isVariableStatement(s) &&
      s.declarationList.declarations.some((d) => d.name.text === 'formatSignatureTraceComment')
  )
  const c = { exports: {}, getDccSignatureOpinionLabel: labels.getDccSignatureOpinionLabel }
  vm.runInNewContext(
    ts.transpileModule(fn.getText(ast) + '\nexports.label=formatSignatureTraceComment', {
      compilerOptions: { target: ts.ScriptTarget.ES2022 }
    }).outputText,
    c
  )
  assert.equal(c.exports.label('批准意见；批准文件负责人事实：[1,"甲"]'), '批准意见')
  const column = source.match(/<el-table-column[^>]+label="签名意见"[\s\S]*?<\/el-table-column>/)[0]
  assert.ok(column.includes('getDccSignatureOpinionLabel(row.comment)'))
  const purpose=source.match(/<el-table-column[^>]+label="签名目的"[\s\S]*?<\/el-table-column>/)[0]
  assert.ok(purpose.includes('getSignatureMeaningLabel(row.signaturePurpose || row.meaningCode)'))
})
const detailSource = fs.readFileSync(
  path.join(root, 'src/views/dcc/controlled-file/detail/presentation.ts'),
  'utf8'
)
const ast = ts.createSourceFile('presentation.ts', detailSource, ts.ScriptTarget.Latest, true)
function timeline(file) {
  const names = ['appendLifecycleTimelineItem', 'buildDetailLifecycleTimelineItems']
  const nodes = names.map((name) =>
    ast.statements.find(
      (s) =>
        ts.isVariableStatement(s) &&
        s.declarationList.declarations.some((d) => d.name.text === name)
    )
  )
  assert.ok(nodes.every(Boolean), 'actual timeline functions')
  const c = {
    exports: {},
    Map,
    String,
    Number,
    Date,
    userNameMapValue: () => '',
    formatControlledFileDateTime: (v) => (v == null ? '-' : String(v)),
    formatControlledFileDate: (v) => (v == null ? '-' : String(v)),
    getDetailDateSortValue: (v) => (v ? new Date(v).getTime() : undefined),
    getDetailStatusLabel: (v) => v,
    getDccVersionDisplayText: module('src/views/dcc/controlled-file/shared/version-display.ts', () => { throw Error('Unexpected display import') }).getDccVersionDisplayText,
    getDistributionMediumLabel: (v) => v,
    toText: (v) => (v == null ? '' : String(v)),
    resolveDetailUserName: () => '',
    getSignatureActionLabel: labels.getDccSignatureTaskActionLabel,
    getSignatureMeaningLabel: labels.getDccSignatureMeaningLabel,
    getDccSignatureOpinionLabel: labels.getDccSignatureOpinionLabel,
    getSignatureActorSummary: () => '甲'
  }
  vm.runInNewContext(
    ts.transpileModule(
      nodes.map((n) => n.getText(ast).replace('export ', '')).join('\n') +
        '\nexports.items=buildDetailLifecycleTimelineItems(__file,{userNameMap:new Map(),deptNameMap:new Map()})',
      { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }
    ).outputText,
    { ...c, __file: file }
  )
  return c.exports.items
}
test('working and pending files never present planned date or control date as activation', () => {
  for (const status of ['WORKING', 'CONTROLLED_PENDING_EFFECTIVE']) {
    const items = timeline({
      id: 1,
      status,
      versionNo: 'A/2',
      effectiveDate: '2026-10-13',
      publishedTime: '2026-10-06',
      controlledTime: '2026-10-06'
    })
    assert.equal(
      items.some((item) => item.title === '版本生效' || item.title === '正式生效'),
      false
    )
  }
})
test('actual activation uses server activatedTime and is distinct from formal control', () => {
  const items = timeline({
    id: 1,
    status: 'ACTIVE',
    versionNo: 'A/2',
    effectiveDate: '2026-10-13',
    publishedTime: '2026-10-06',
    controlledTime: '2026-10-06',
    activatedTime: '2026-10-13 00:00:00'
  })
  assert.ok(
    items.some((item) => item.title === '版本生效' && item.timeText === '2026-10-13 00:00:00')
  )
  assert.ok(items.some((item) => item.title === '文件受控' && item.timeText === '2026-10-06'))
})
test('stamped copy is controlled, and timeline opinions never mix machine evidence', () => {
  const raw = '本人会签；会签指派事实：部门=12，会签人=1，指派载荷=abc'
  const items = timeline({
    id: 1,
    versionNo: 'A/2',
    stampedTime: '2026-10-06',
    signatureSummaries: [
      {
        id: 3,
        signedAt: '2026-10-06',
        taskActionResult: 'ASSIGNED',
        meaningCode: 'MATRIX_REVIEW_ASSIGN',
        comment: raw
      }
    ]
  })
  assert.equal(items.find((item) => item.key === 'file-stamped').categoryLabel, '受控')
  const description = items.find((item) => item.key === 'signature-3').description
  assert.equal(description.includes('指派载荷'), false)
  assert.equal(description.includes('MATRIX_REVIEW_ASSIGN'), false)
  assert.equal(raw.includes('指派载荷'), true)
})
test('DCC summary matcher accepts labeled fields with real newlines and formal A/1 versions', () => {
  const { descriptor } = parse(
    fs.readFileSync(path.join(root, 'src/views/approval-center/index.vue'), 'utf8')
  )
  const ast = ts.createSourceFile(
    'approval.ts',
    descriptor.scriptSetup.content,
    ts.ScriptTarget.Latest,
    true
  )
  const statement = ast.statements.find(
    (s) =>
      ts.isVariableStatement(s) &&
      s.declarationList.declarations.some((d) => d.name.text === 'findDccContextTagValue')
  )
  const c = {
    exports: {},
    EMPTY_APPROVAL_DISPLAY: '-',
    normalizeDccContextTag: (s) => s.replace(/^[^:：]+[:：]\s*/, '').trim(),
    resolveBusinessContextValueLabel: (s) => s
  }
  vm.runInNewContext(
    ts.transpileModule(statement.getText(ast) + '\nexports.get=findDccContextTagValue', {
      compilerOptions: { target: ts.ScriptTarget.ES2022 }
    }).outputText,
    c
  )
  assert.equal(
    c.exports.get(
      { businessContextTags: ['文件编号：SOP-1', '版本：A/2\n', '分类：技术调研报告'] },
      [/版本/]
    ),
    'A/2'
  )
})
test('public pages expose official workbench and permitted taxonomy maintenance components', () => {
  const browser = fs.readFileSync(
    path.join(root, 'src/views/dcc/controlled-file/browser/index.vue'),
    'utf8'
  )
  assert.ok(browser.includes('<ControlledFileWorkbenchEntry'))
  const categories = fs.readFileSync(
    path.join(root, 'src/views/dcc/controlled-file/categories/index.vue'),
    'utf8'
  )
  assert.ok(categories.includes('FileTypeTaxonomyPanel'))
  assert.ok(categories.includes("checkPermi(['dcc:controlled-file:category:manage'])"))
  const controller = fs.readFileSync(
    path.join(
      root,
      '../IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/category/DccFileTypeTaxonomyController.java'
    ),
    'utf8'
  )
  assert.match(
    controller,
    /@GetMapping\s+@Operation\([^\n]+\)\s+@PreAuthorize\("@ss\.hasPermission\('dcc:controlled-file:category:manage'\)/
  )
})
