const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const ts = require('typescript')

const source = fs.readFileSync('src/views/mes/pro/processpool/QaRegulationPage.vue', 'utf8')
  .match(/<script setup lang="ts">([\s\S]*?)<\/script>/)[1]
const ast = ts.createSourceFile('qa.ts', source, ts.ScriptTarget.Latest, true)
const names = ['createEmptyQaInspectionTypeRules', 'buildQaInspectionTypeRules', 'resolveCommonMemberInspectionRules']
const statements = ast.statements.filter(s => ts.isVariableStatement(s) &&
  s.declarationList.declarations.every(d => names.includes(d.name.getText(ast))))
assert.equal(statements.length, names.length)
const code = ts.transpileModule(statements.map(s => s.getText(ast)).join('\n') +
  '\nreturn {buildQaInspectionTypeRules, resolveCommonMemberInspectionRules}',
  { compilerOptions: { target: ts.ScriptTarget.ES2020 } }).outputText
const { buildQaInspectionTypeRules, resolveCommonMemberInspectionRules } = new Function(code)()
const previousRules = () => [
  { key: 'FIRST', inspectionType: 'FIRST', required: true, fixedQuantity: 13 },
  { key: 'PATROL_AM', inspectionType: 'PATROL', required: true },
  { key: 'PATROL_PM', inspectionType: 'PATROL', required: true },
  { key: 'FINAL', inspectionType: 'FINAL', required: false, notApplicableReason: '过程已覆盖' }
]

test('new drafts publish one patrol definition while retaining explicit final settings and fixed quantities', () => {
  const input = previousRules(), original = structuredClone(input)
  const rules = buildQaInspectionTypeRules(input)
  assert.deepEqual(rules.map(r => [r.key, r.inspectionType]),
    [['FIRST', 'FIRST'], ['PATROL', 'PATROL'], ['FINAL', 'FINAL']])
  assert.equal(rules[0].fixedQuantity, 13)
  assert.equal(rules[1].required, true)
  assert.equal(rules[2].required, false)
  assert.equal(rules[2].notApplicableReason, '过程已覆盖')
  assert.deepEqual(input, original)
})

test('common regulation draft editing uses the same stage definitions and explicit final setting', () => {
  const member = { inspectionTypeRules: previousRules() }
  const rules = resolveCommonMemberInspectionRules(member, true)
  assert.deepEqual(rules.map(r => r.key), ['FIRST', 'PATROL', 'FINAL'])
  assert.equal(rules[2].required, true)
  assert.equal(rules[2].notApplicableReason, undefined)
  assert.equal(member.inspectionTypeRules[3].required, false)
})

test('missing final applicability cannot silently produce a publishable draft', () => {
  assert.throws(() => buildQaInspectionTypeRules([]), /末检适用性/)
  assert.throws(() => buildQaInspectionTypeRules([{ key: 'FINAL' }]), /末检适用性/)
})

test('common regulation numbering excludes retired and draft versions as well as the published version', async () => {
  const versionNames = ['incrementVersionNo', 'resolveCommonMemberNextVersionNo', 'resolvePositiveId']
  const versionStatements = ast.statements.filter(s => ts.isVariableStatement(s) &&
    s.declarationList.declarations.every(d => versionNames.includes(d.name.getText(ast))))
  const versionCode = ts.transpileModule(versionStatements.map(s => s.getText(ast)).join('\n') +
    '\nreturn resolveCommonMemberNextVersionNo',
    { compilerOptions: { target: ts.ScriptTarget.ES2020 } }).outputText
  const calls = []
  const nextVersion = new Function('QcTemplateApi', versionCode)({
    listCommonRegulationVersions: async id => {
      calls.push(id)
      return [
        { regulationId: 65, versionNo: 'A/7', lifecycleStatus: 'RETIRED' },
        { regulationId: 65, versionNo: 'A/8', lifecycleStatus: 'RETIRED' },
        { regulationId: 65, versionNo: 'A/9', lifecycleStatus: 'PUBLISHED' },
        { regulationId: 65, versionNo: 'A/10', lifecycleStatus: 'DRAFT' }
      ]
    }
  })
  assert.equal(await nextVersion({ commonDccProjectCodeId: 44, commonRegulationId: 65,
    versionNo: 'A/7', commonRegulationName: '初包装' }), 'A/11')
  assert.deepEqual(calls, [65])
  await assert.rejects(() => nextVersion({ commonDccProjectCodeId: 44, commonRegulationId: 66,
    versionNo: 'A/1', commonRegulationName: '大中包装' }), /缺少正式版本记录/)
})
