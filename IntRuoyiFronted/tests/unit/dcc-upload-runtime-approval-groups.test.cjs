const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs'), path = require('node:path'), vm = require('node:vm'), ts = require('typescript')
const { parse } = require('vue/compiler-sfc')
const h = require('./dcc-upload-test-loader.cjs')
const helpers = h.loadUploadDepartments()
const long = '9223372036854775709'
const accountRows = [{ id: '9', name: '批准甲', deptName: '质量' }, { id: long, name: '批准乙', deptName: '法规' }]
const raw = (id, ids, patch = {}) => ({ ...h.matrixFixture().nodes[0], id, stageNo: 3, stageCode: null,
  stageName: '批准', stageType: 'APPROVAL', subjectType: 'USER', subjectId: ids[0], subjectName: '原主体' + id,
  subjectDepartmentPath: '正式部门' + id, candidateSourceType: 'USER', candidateSourceId: ids[0], candidateSourceIds: ids.join(','),
  approveMethod: 'ALL', approveRatio: 100, requireAllApprovals: true, required: true, ...patch })
const group = node => ({ sourceNodeId: node.id, stageType: node.stageType, marker: node.marker, required: node.required,
  subjectType: node.subjectType, candidateSourceType: node.candidateSourceType, candidateSourceIds: node.candidateSourceIds.split(',').map(x => x.trim()),
  approveMethod: node.approveMethod, approveRatio: node.approveRatio, requireAllApprovals: node.requireAllApprovals, applied: true })
const response = () => {
  const nodes = [raw('41', ['9']), raw('42', [long])]
  return { ready: true, blockers: [], executionContract: 'PUBLIC_MATRIX_NATIVE_V1', matrixAfter: { ...h.matrixFixture(), nodes },
    runtimeRuleGroups: nodes.map(group), runtimeCapabilityLimitations: ['多候选ANY原组需独立执行，当前不支持'],
    nodes: [{ stageCode: 'MATRIX_APPROVAL', candidateSourceType: 'USER', candidateSourceIds: ['9', long], resolvedUserIds: ['9', long], approveMethod: 'ALL', approveRatio: 100, requireAllApprovals: true }] }
}
test('PUBLIC approval projection keeps independent original groups rather than one flattened ALL sentence', () => {
  const result = helpers.resolvedUploadApprovers(response(), accountRows)
  assert.equal(result.groups.length, 2)
  assert.equal(result.groups[0].sourceNodeId, '41'); assert.equal(result.groups[1].sourceNodeId, '42')
  assert.deepEqual(Array.from(result.groups[1].candidateSourceIds), [long])
  const summary = result.groups.map(row => row.summary).join('\n')
  for (const value of ['原主体41', '原主体42', '正式部门41', '组内全部批准', '实际参与', '批准甲', '批准乙', long, '●']) assert.ok(summary.includes(value), value)
  assert.equal(result.capabilityLimitations[0], '多候选ANY原组需独立执行，当前不支持')
})
test('R09 registered PUBLIC V2 uses the same original V1 derivation and full group checks as PUBLIC V1',()=>{
 for(const contract of ['PUBLIC_MATRIX_NATIVE_V1','PUBLIC_MATRIX_NATIVE_V2']) {
  const r=response();r.executionContract=contract;r.derivationVersion='DCC_PUBLIC_MATRIX_NATIVE_V1'
  const projected=helpers.resolvedUploadApprovers(r,accountRows)
  assert.equal(projected.groups.length,2);assert.equal(projected.groups[1].sourceNodeId,'42');assert.equal(r.derivationVersion,'DCC_PUBLIC_MATRIX_NATIVE_V1')
  for(const change of [x=>delete x.runtimeRuleGroups,x=>x.runtimeRuleGroups[0].applied=false,x=>x.runtimeRuleGroups[0].candidateSourceIds=['8'],
    x=>x.nodes[0].resolvedUserIds=['9'],x=>x.nodes[0].approveMethod='ANY']){
   const corrupt=response();corrupt.executionContract=contract;change(corrupt);assert.throws(()=>helpers.resolvedUploadApprovers(corrupt,accountRows))
  }
 }
})
test('R09 unknown execution contracts with public source evidence cannot use the old flattened projection',()=>{
 for(const contract of ['PUBLIC_MATRIX_NATIVE_V3','CUSTOM','LEGACY_V4_ACTION',undefined]){
  const r=response();r.executionContract=contract;r.derivationVersion='DCC_PUBLIC_MATRIX_NATIVE_V1'
  assert.throws(()=>helpers.resolvedUploadApprovers(r,accountRows),/PUBLIC|合同|原组/)
 }
})
test('R09 shared V2 workflow contract keeps explicit CUSTOM source separate from public original groups',()=>{
 const r=require('./dcc-upload-a32-fixture.cjs').response()
 const result=helpers.resolvedUploadApprovers(r,accountRows)
 assert.equal(result.accounts[0].id,long);assert.equal(result.groups.length,0);assert.equal(r.matrixAfter,null)
})
test('PUBLIC group identity and original policy contradictions fail rather than inventing or flattening groups', () => {
  for (const change of [r => { delete r.runtimeRuleGroups }, r => { r.runtimeRuleGroups = [] }, r => { r.runtimeRuleGroups.push(r.runtimeRuleGroups[0]) },
    r => { r.runtimeRuleGroups[0].sourceNodeId = '999' }, r => { r.runtimeRuleGroups[0].sourceNodeId = 9007199254740992 },
    r => { r.runtimeRuleGroups[0].candidateSourceIds = [9007199254740992] }, r => { r.runtimeRuleGroups[0].candidateSourceIds = ['8'] },
    r => { r.runtimeRuleGroups[0].required = false }, r => { r.runtimeRuleGroups[0].applied = false },
    r => { r.runtimeRuleGroups[0].marker = '◎' }, r => { r.runtimeRuleGroups[0].approveMethod = 'ANY' },
    r => { r.nodes[0].resolvedUserIds = ['9'] }, r => { r.nodes[0].approveMethod = 'ANY'; r.nodes[0].requireAllApprovals = false }]) {
    const r = response(); change(r); assert.throws(() => helpers.resolvedUploadApprovers(r, accountRows), /原组|组|候选|主体|规则/)
  }
})
test('blocked multiple ANY capability keeps exact original group policies visible without pretending support', () => {
  const r = response(); r.ready = false
  for (const node of r.matrixAfter.nodes) { node.approveMethod = 'ANY'; node.approveRatio = null; node.requireAllApprovals = false }
  r.runtimeRuleGroups = r.matrixAfter.nodes.map(group)
  r.nodes[0].approveMethod = 'ANY'; r.nodes[0].approveRatio = null; r.nodes[0].requireAllApprovals = false
  const result = helpers.resolvedUploadApprovers(r, accountRows)
  assert.equal(result.groups.length, 2)
  assert.ok(result.groups.every(row => row.summary.includes('组内任一候选批准')))
  assert.match(result.rule, /各原批准组/)
})
test('old external projection without PUBLIC raw groups retains its own existing legal account policy', () => {
  const r = response(); delete r.executionContract; delete r.runtimeRuleGroups; delete r.matrixAfter
  const result = helpers.resolvedUploadApprovers(r, accountRows)
  assert.equal(result.rule, '全部批准')
})

test('department or position candidate IDs remain source identities and are never mislabeled as resolved accounts', () => {
  const r = response()
  for (const raw of r.matrixAfter.nodes) { raw.subjectType = 'POSITION'; raw.candidateSourceType = 'POSITION' }
  r.runtimeRuleGroups = r.matrixAfter.nodes.map(group); r.nodes[0].candidateSourceType = 'POSITION'
  const result = helpers.resolvedUploadApprovers(r, accountRows)
  assert.match(result.groups[0].summary, /候选来源 岗位：身份 9/)
  assert.doesNotMatch(result.groups[0].summary, /候选来源.*批准甲/)
})

test('PUBLIC raw CSV missing or blank fails despite a populated single candidate field, while trim preserves its original order', () => {
  for (const value of [null, '', '  ']) {
    const r = response(); r.matrixAfter.nodes[0].candidateSourceIds = value
    assert.equal(r.matrixAfter.nodes[0].candidateSourceId, '9')
    assert.throws(() => helpers.resolvedUploadApprovers(r, accountRows), /候选|原配置|原组/)
    assert.equal(r.matrixAfter.nodes[0].candidateSourceIds, value)
  }
  const r = response()
  r.matrixAfter.nodes = [raw('41', [long, '9'])]
  r.matrixAfter.nodes[0].candidateSourceIds = ` ${long} , 9 `
  r.runtimeRuleGroups = [group(r.matrixAfter.nodes[0])]
  r.nodes[0].candidateSourceIds = [long, '9']; r.nodes[0].resolvedUserIds = [long, '9']
  const result = helpers.resolvedUploadApprovers(r, accountRows)
  assert.deepEqual(Array.from(result.groups[0].candidateSourceIds), [long, '9'])
  assert.equal(r.matrixAfter.nodes[0].candidateSourceIds, ` ${long} , 9 `)
  r.runtimeRuleGroups[0].candidateSourceIds = ['9', long]
  assert.throws(() => helpers.resolvedUploadApprovers(r, accountRows), /候选.*不一致/)
})

test('PUBLIC capability limitations use explicit empty arrays but missing or malformed explanations fail', () => {
  const r = response(); r.runtimeCapabilityLimitations = []
  assert.deepEqual(Array.from(helpers.resolvedUploadApprovers(r, accountRows).capabilityLimitations), [])
  for (const value of [undefined, null, [undefined], [' ']])
    assert.throws(() => helpers.resolvedUploadApprovers({ ...r, runtimeCapabilityLimitations: value }, accountRows), /能力|事实/)
})

test('a damaged PUBLIC execution marker cannot make raw groups fall back to the old flattened display', () => {
  for (const marker of [undefined, null, 'LEGACY_V4_ACTION', 'OTHER']) {
    const r = response(); r.executionContract = marker
    assert.throws(() => helpers.resolvedUploadApprovers(r, accountRows), /PUBLIC|合同|原组/)
  }
})

test('raw CSV integer parsing matches Java Long values without rewriting raw bytes or sorting group candidates', () => {
  const r = response()
  r.matrixAfter.nodes = [raw('41', [long, '9'])]
  r.matrixAfter.nodes[0].candidateSourceIds = ` ${long} , 0009 `
  r.runtimeRuleGroups = [{ ...group(raw('41', [long, '9'])), sourceNodeId: '41' }]
  r.nodes[0].candidateSourceIds = [long, '9']; r.nodes[0].resolvedUserIds = [long, '9']
  assert.deepEqual(Array.from(helpers.resolvedUploadApprovers(r, accountRows).groups[0].candidateSourceIds), [long, '9'])
  assert.equal(r.matrixAfter.nodes[0].candidateSourceIds, ` ${long} , 0009 `)
  r.matrixAfter.nodes[0].candidateSourceIds = '9,0009'
  assert.throws(() => helpers.resolvedUploadApprovers(r, accountRows), /候选.*不一致|重复/)
  const unicode = response(); unicode.matrixAfter.nodes[0].candidateSourceIds = '\u00a09'
  assert.throws(() => helpers.resolvedUploadApprovers(unicode, accountRows), /CSV|候选/)
})

test('original group helper type-checks against actual public raw and runtime interfaces without a global build', () => {
  const apiFile = path.resolve(__dirname, '../../src/api/dcc/controlledFile/workflow.ts')
  const api = ts.createSourceFile(apiFile, fs.readFileSync(apiFile, 'utf8'), ts.ScriptTarget.Latest, true)
  const names = new Set(['ApprovalMatrixBaselineVO', 'ControlledFileRuntimeRuleGroupVO'])
  const declarations = api.statements.filter(s => s.name && names.has(s.name.text)).map(s => s.getText(api))
  assert.equal(declarations.length, 2)
  const file = path.resolve(__dirname, '../../src/views/dcc/controlled-file/upload/signoff-departments.ts')
  const source = ts.createSourceFile(file, fs.readFileSync(file, 'utf8'), ts.ScriptTarget.Latest, true)
  const code = declarations.join('\n') + '\n' + source.statements.filter(s => !ts.isImportDeclaration(s)).map(s => s.getText(source)).join('\n')
  const virtual = path.resolve(__dirname, 'runtime-groups-virtual.ts'), options = { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.CommonJS, strict: true, noEmit: true, types: [] }
  const host = ts.createCompilerHost(options), getSource = host.getSourceFile, read = host.readFile, exists = host.fileExists
  const same = file => path.resolve(file) === virtual
  host.fileExists = file => same(file) || exists(file); host.readFile = file => same(file) ? code : read(file)
  host.getSourceFile = (file, language, error, fresh) => same(file) ? ts.createSourceFile(file, code, language, true) : getSource(file, language, error, fresh)
  const diagnostics = ts.getPreEmitDiagnostics(ts.createProgram([virtual], options, host))
  assert.deepEqual(diagnostics.map(d => `${d.code}: ${ts.flattenDiagnosticMessageText(d.messageText, '\n')}`), [])
})

test('a late corrupted approval-group preview cannot overwrite the newer actual upload response', async () => {
  const page = parse(fs.readFileSync(path.resolve(__dirname, '../../src/views/dcc/controlled-file/upload/index.vue'), 'utf8')).descriptor.scriptSetup.content
  const ast = ts.createSourceFile('upload.ts', page, ts.ScriptTarget.Latest, true)
  const st = name => ast.statements.find(s => ts.isVariableStatement(s) && s.declarationList.declarations.some(d => d.name.text === name)).getText(ast)
  const ref = value => ({ value })
  const projection = () => {
    const r = response(), matrix = r.matrixAfter
    return { ...h.matrixReadiness({ready:true}), ...r, approvalMatrix: matrix, matrixBefore: matrix, matrixAfter: matrix,
      nodes: [{ stageCode: 'MATRIX_REVIEW', candidateSourceType: 'DEPT', candidateSourceIds: ['7'] }, ...r.nodes] }
  }
  let release, reads = 0
  const c = { exports: {}, Error, JSON, Promise, ...helpers, ...h.loadUploadMatrix(),
    userStore:{getUser:{id:'71',deptId:'7'}},
    approvalMatrixState: ref(), approvalMatrixScopeKey: '', formData: { categoryId: '2', dccProjectCodeId: '3', fileTypeTaxonomyId: '4', processType: 'CONTROLLED_FILE', selectedSignoffDepartmentIds: ['7'], selectedDistributionDepartmentIds: ['8'] },
    isCustomApprovalSource: ref(false), selectedFileTypeTaxonomyPath: ref({approvalSourceMode:'PUBLIC_MATRIX'}),
    isExternalReview: ref(false), routeReadinessRequestSeq: 0, routeReadiness: ref(), routeReadinessLoading: ref(false), routeReadinessError: ref(''), routeReadinessSelectionKey: ref(''), signoffSelectionCategoryKey: helpers.signoffCategoryKey('2', 'CONTROLLED_FILE'),
    referenceIdentity: String, uploadFileTypeIdentity: String, getTenantId: () => '1', getVisitTenantId: () => undefined, createControlledFileUploadSessionId: () => 'scope', resolveUploadErrorMessage: error => error.message,
    checkControlledFileRouteReadiness: () => ++reads === 1 ? new Promise(resolve => { release = resolve }) : Promise.resolve(projection()) }
  vm.runInNewContext(ts.transpileModule(st('ensureUploadApprovalMatrix') + '\n' + st('refreshRouteReadiness') + '\nexports.refresh=refreshRouteReadiness', { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText, c)
  const old = c.exports.refresh(); await c.exports.refresh()
  const corrupt = projection(); corrupt.runtimeRuleGroups[0].sourceNodeId = '999'; release(corrupt); await old
  assert.ok(c.routeReadiness.value, c.routeReadinessError.value)
  const result = helpers.resolvedUploadApprovers(c.routeReadiness.value, accountRows)
  assert.equal(result.groups[0].sourceNodeId, '41'); assert.equal(c.routeReadinessError.value, '')
  assert.deepEqual(Array.from(c.formData.selectedDistributionDepartmentIds), ['8'])
})
