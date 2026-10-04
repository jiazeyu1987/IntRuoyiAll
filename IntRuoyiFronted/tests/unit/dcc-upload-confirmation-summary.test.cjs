const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')
const ts = require('typescript')
const load = file => {
  const context = { exports: {}, Error, require: () => ({ validateAttributes: value => value }) }
  vm.runInNewContext(ts.transpileModule(fs.readFileSync(file, 'utf8'),
    { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText, context)
  return context.exports
}
const helpers = load('src/views/dcc/controlled-file/upload/submitter.ts')
const people = load('src/views/dcc/controlled-file/upload/signoff-departments.ts')
const long = '9223372036854775709'
const actual = { targetMarkets: ['CE', 'OTHER'], otherMarket: '英国', licenseHolder: 'Y', actualManufacturer: 'NA', documentTransfer: 'Y', transferTo: '乙项目' }
const draft = { processType: 'CONTROLLED_FILE', projectAttributes: actual, fileName: '模板显示名', fileNumber: 'SOP-001', versionNo: 'A/1', effectiveDate: '2099-10-01', needTraining: true }
const facts = { projectName: '甲项目', folderName: '逻辑文件夹', fileTypeName: '作业指导书', relatedFiles: [{ fileName: '关联文件.pdf', versionNo: 'B/1', pendingEffect: true }], signoffDepartments: [{ id: '8', name: '质量部门' }], approvers: [{ id: long, name: '批准甲', deptName: '法规部' }], approvalRule: '全部批准' }

test('confirmation summary contains original full name, every actual attribute and formal selected facts', () => {
  const rows = helpers.buildUploadConfirmationSummary(draft, { fileName: 'SOP.PDF' }, facts)
  const text = rows.map(row => `${row.label}：${row.value}`).join('\n')
  for (const expected of ['甲项目', '逻辑文件夹', '作业指导书', 'SOP.PDF', 'SOP-001', 'A/1', '2099-10-01', 'CE', '英国', '注册人：是', '生产方：不适用', '转移至：乙项目', '关联文件.pdf', 'B/1', '待生效', '质量部门', '批准甲', long, '全部批准', '需要培训']) assert.ok(text.includes(expected), expected)
  assert.equal(text.includes('模板显示名'), false)
  actual.otherMarket = '已改变'; facts.approvers[0].name = '已改变'
  assert.equal(rows.map(row => row.value).join('\n').includes('已改变'), false)
})

test('confirmation refuses missing formal facts instead of inventing defaults', () => {
  for (const missing of ['projectName', 'folderName', 'fileTypeName', 'approvers', 'signoffDepartments']) {
    assert.throws(() => helpers.buildUploadConfirmationSummary(draft, { fileName: 'SOP.PDF' }, { ...facts, [missing]: undefined }))
  }
})

test('matrix approvers resolve exact Long identities only from official enabled accounts', () => {
  const accounts = people.approvalUserOptions([{ id: long, nickname: '批准甲', deptName: '法规部' }])
  const resolved = people.resolvedUploadApprovers({ nodes: [{ stageCode: 'MATRIX_APPROVAL', resolvedUserIds: [long], approveMethod: 'ALL' }] }, accounts)
  assert.equal(resolved.accounts[0].id, long)
  assert.equal(resolved.accounts[0].name, '批准甲')
  assert.equal(resolved.rule, '全部批准')
  assert.throws(() => people.resolvedUploadApprovers({ nodes: [{ stageCode: 'MATRIX_REVIEW', resolvedUserIds: [long] }] }, accounts), /批准/)
  assert.throws(() => people.resolvedUploadApprovers({ nodes: [{ stageCode: 'MATRIX_APPROVAL', resolvedUserIds: ['9'], approveMethod: 'ALL' }] }, accounts), /启用/)
})

test('enabled-user and approver contracts reject unsafe, disabled, duplicate or missing people', () => {
  for (const input of [undefined, [{ id: 9007199254740992, nickname: '甲' }], [{ id: '7', nickname: '' }], [{ id: '7', nickname: '甲', status: 1 }], [{ id: '7', nickname: '甲' }, { id: 7, nickname: '乙' }]]) assert.throws(() => people.approvalUserOptions(input))
  const accounts = [{ id: long, name: '甲' }]
  for (const ids of [[], [long, long], [9007199254740992]]) assert.throws(() => people.resolvedUploadApprovers({ nodes: [{ stageCode: 'MATRIX_APPROVAL', resolvedUserIds: ids, approveMethod: 'ALL' }] }, accounts))
})

test('public upload has explicit effective date input and honest working/formal version wording', () => {
  const page = fs.readFileSync('src/views/dcc/controlled-file/upload/index.vue', 'utf8')
  assert.doesNotMatch(page, /effectiveDate:\s*resolveTodayDate\(\)|formData\.effectiveDate\s*=\s*resolveTodayDate\(\)/)
  assert.doesNotMatch(page, /系统允许补录历史生效日期|当前有效版本|后续大小版本统一通过/)
  assert.match(page, /检入.*小版本/)
  assert.match(page, /局部变更.*换版变更.*审批/)
})
