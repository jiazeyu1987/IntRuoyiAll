const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')

const frontendRoot = path.resolve(__dirname, '../..')
const controlledFileRoot = path.join(frontendRoot, 'src/views/dcc/controlled-file')
const cache = new Map()
const loadProductionModule = (relativePath) => {
  const filename = path.resolve(controlledFileRoot, relativePath)
  if (cache.has(filename)) return cache.get(filename)
  const compiled = ts.transpileModule(fs.readFileSync(filename, 'utf8'), {
    fileName: filename,
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
  })
  const exports = {}
  const context = {
    exports,
    Error,
    URLSearchParams,
    require: (id) => {
      assert.ok(id.startsWith('.'), `Unexpected production dependency: ${id}`)
      return loadProductionModule(path.relative(controlledFileRoot, path.resolve(path.dirname(filename), id + '.ts')))
    }
  }
  vm.runInNewContext(compiled.outputText, context, { filename })
  cache.set(filename, exports)
  return exports
}

const { getDccVersionDisplayText } = loadProductionModule('shared/version-display.ts')
const { buildRevisionCommand } = loadProductionModule('revision/revision-model.ts')
const { buildControlledFileViewerPath, buildControlledFileTraceabilityPath } = loadProductionModule('view/presentation.ts')

test('checked-in working iterations show only 草稿 without the formal or minor number', () => {
  for (const versionNo of ['A/1-1', 'A/1-2', 'B/3-19', 'AA/2-2147483647']) {
    assert.equal(getDccVersionDisplayText(versionNo), '草稿', versionNo)
  }
})

test('working iteration presentation follows version identity across approval and historical states', () => {
  for (const status of ['WORKING', 'PENDING_MATRIX_REVIEW', 'REJECTED', 'WITHDRAWN', 'SUPERSEDED', 'OBSOLETE']) {
    const version = Object.freeze({ id: '9223372036854775701', versionNo: 'A/1-2', status })
    assert.equal(getDccVersionDisplayText(version.versionNo), '草稿', status)
    assert.equal(version.versionNo, 'A/1-2')
  }
})

test('configured multi-segment working identities and policy-normalized inputs do not leak counters', () => {
  for (const versionNo of ['A/1/2-3', 'AA/2/3/4-5', 'a/1-2', ' A/1-2 ']) {
    assert.equal(getDccVersionDisplayText(versionNo), '草稿', versionNo)
  }
})

test('formal versions and stored historical labels retain their original presentation', () => {
  for (const versionNo of ['A/1', 'A/2', 'AA/9', 'A/1/2', 'AA/2/3/4', 'V1.0', ' A/1 ']) {
    assert.equal(getDccVersionDisplayText(versionNo), versionNo)
  }
})

test('absent version facts are explicitly 未记录 rather than an invented draft or formal version', () => {
  for (const versionNo of [undefined, null, '', '   ']) {
    assert.equal(getDccVersionDisplayText(versionNo), '未记录')
  }
})

test('non-string response facts fail explicitly instead of coercing identity or hiding a contract error', () => {
  for (const versionNo of [1, false, {}, ['A/1-2']]) {
    assert.throws(() => getDccVersionDisplayText(versionNo), /版本显示值必须为字符串/)
  }
})

test('identically labelled drafts keep the selected exact Long ID in real revision commands', () => {
  const versions = Object.freeze([
    Object.freeze({ id: '9223372036854775701', versionNo: 'A/1-1' }),
    Object.freeze({ id: '9223372036854775702', versionNo: 'A/1-2' })
  ])
  const facts = {
    projectAttributes: { targetMarkets: ['NA'], licenseHolder: 'NA', actualManufacturer: 'NA', documentTransfer: 'N' },
    selectedSignoffDepartmentIds: ['100'], selectedDistributionDepartmentIds: [], needTraining: false,
    effectiveMode: 'AFTER_STANDARDIZATION_SIGNATURE', effectiveDate: null
  }
  for (const version of versions) {
    const label = getDccVersionDisplayText(version.versionNo)
    assert.equal(label, '草稿')
    const command = buildRevisionCommand('9223372036854775700', version.id, 'PARTIAL', '明确变更说明', 'g08-specific-key', facts)
    assert.equal(command.selectedIterationId, version.id)
    assert.equal(command.controlledBaselineId, '9223372036854775700')
    assert.equal(Object.hasOwn(command, 'versionNo'), false)
    assert.equal(Object.hasOwn(command, 'versionDisplayText'), false)
    assert.throws(() => buildRevisionCommand('9223372036854775700', label, 'PARTIAL', '明确变更说明', 'g08-specific-key', facts), /文件版本身份无效/)
  }
  assert.equal(versions[0].versionNo, 'A/1-1')
  assert.equal(versions[1].versionNo, 'A/1-2')
})

test('real viewer and traceability paths preserve each chosen approval or history ID after display', () => {
  for (const id of ['9223372036854775701', '9223372036854775702']) {
    const selected = Object.freeze({ id, versionNo: 'A/1-2' })
    assert.equal(getDccVersionDisplayText(selected.versionNo), '草稿')
    const viewer = buildControlledFileViewerPath(selected.id, 'approval', '/dcc/controlled-file/browser')
    const history = buildControlledFileTraceabilityPath(selected.id, 'history', undefined, 'signature')
    assert.equal(viewer.split('?')[0], `/dcc/controlled-file/detail/${id}`)
    assert.equal(history.split('?')[0], `/dcc/controlled-file/detail/${id}`)
    assert.equal(new URLSearchParams(viewer.split('?')[1]).get('from'), 'approval')
    assert.equal(new URLSearchParams(history.split('?')[1]).get('traceScope'), 'signature')
    assert.equal(selected.versionNo, 'A/1-2')
  }
})
