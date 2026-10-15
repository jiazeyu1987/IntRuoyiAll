const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')
const ts = require('typescript')
const load = file => {
  const context = { exports: {}, Error, require: id => id.endsWith('approval-matrix') ? require('./dcc-upload-test-loader.cjs').loadUploadMatrix() : id.endsWith('signoff-departments') ? require('./dcc-upload-test-loader.cjs').loadUploadDepartments() : ({ validateAttributes: value => value }) }
  vm.runInNewContext(ts.transpileModule(fs.readFileSync(file, 'utf8'),
    { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText, context)
  return context.exports
}
const helpers = load('src/views/dcc/controlled-file/upload/submitter.ts')
const people = load('src/views/dcc/controlled-file/upload/signoff-departments.ts')
const long = '9223372036854775709'
const actual = { targetMarkets: ['CE', 'OTHER'], otherMarket: '英国', licenseHolder: 'Y', actualManufacturer: 'NA', documentTransfer: 'Y', transferTo: '乙项目' }
const draft = { processType: 'CONTROLLED_FILE', projectAttributes: actual, fileName: '模板显示名', fileNumber: 'SOP-001', versionNo: 'A/1', effectiveMode: 'PRESET_DATE', effectiveDate: '2099-10-01', selectedDistributionDepartmentIds: ['8'], needTraining: true }
const facts = { projectName: '甲项目', folderName: '逻辑文件夹', fileTypeName: '作业指导书', relatedFiles: [{ fileName: '关联文件.pdf', versionNo: 'B/1', pendingEffect: true }], signoffDepartments: [{ id: '8', name: '质量部门' }], distributionDepartments: [{ id: '8', name: '质量部门' }], approvers: [{ id: long, name: '批准甲', deptName: '法规部' }], approvalRule: '全部批准' }
const compiler={id:'71',name:'真实编制甲',deptId:'8',deptName:'质量部门'}
const supervisor={id:'72',name:'正式直属主管乙',deptId:'10',deptName:'研发部门'}
Object.assign(facts,{compiler,supervisor})

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

test('IC31 confirmation shows detached compiler and direct supervisor with their actual official departments',()=>{
  const rows=helpers.buildUploadConfirmationSummary(draft,{fileName:'SOP.PDF'},facts)
  assert.equal(rows.find(row=>row.label==='编制人')?.value,'真实编制甲（质量部门）')
  assert.equal(rows.find(row=>row.label==='直属主管')?.value,'正式直属主管乙（研发部门）')
  const original=rows.map(row=>row.value).join('\n')
  assert.throws(()=>helpers.buildUploadConfirmationSummary(draft,{fileName:'SOP.PDF'},{...facts,compiler:undefined}),/编制/)
  assert.throws(()=>helpers.buildUploadConfirmationSummary(draft,{fileName:'SOP.PDF'},{...facts,supervisor:undefined}),/主管/)
  assert.ok(original.includes('全部批准'))
})
test('IC31 native confirmation rejects absent, unsafe identities or missing official person departments',()=>{
  for(const field of ['compiler','supervisor'])for(const patch of [{id:9007199254740992},{id:'0'},{id:'9223372036854775808'},{name:''},{deptId:undefined},{deptName:undefined},{deptId:'0'},{deptName:'   '}]){
    assert.throws(()=>helpers.buildUploadConfirmationSummary(draft,{fileName:'SOP.PDF'},{...facts,[field]:{...facts[field],...patch}}))
  }
})
test('IC31 compiler and supervisor resolve exact identities from enabled directory rather than cached login nickname or approvers',()=>{
  const accounts=people.approvalUserOptions([{id:'71',nickname:'真实编制甲',deptId:'8',deptName:'质量部门'},
    {id:long,nickname:'正式直属主管乙',deptId:'10',deptName:'研发部门'}])
  const route={ready:true,nodes:[{stageCode:'SUPERVISOR_REVIEW',resolvedUserIds:[long]}],blockers:[]}
  const resolved=people.resolvedUploadApplicationPeople(route,{id:71,deptId:8,nickname:'缓存错误名字'},accounts)
  assert.deepEqual(JSON.parse(JSON.stringify(resolved)),{compiler,supervisor:{...supervisor,id:long}})
  accounts[0].name='后来改名';accounts[1].deptName='后来改部门'
  assert.equal(resolved.compiler.name,'真实编制甲');assert.equal(resolved.supervisor.deptName,'研发部门')
})
test('IC31 people resolver refuses missing, duplicate, lossy supervisor and compiler scope without choosing a leader',()=>{
  const accounts=[compiler,supervisor]
  const node={stageCode:'SUPERVISOR_REVIEW',resolvedUserIds:['72']}
  const route={ready:true,nodes:[node],blockers:[]}
  for(const nodes of [[],[node,node],[{...node,resolvedUserIds:[]}],[{...node,resolvedUserIds:['72','72']}],[{...node,resolvedUserIds:['72','73']}],[{...node,resolvedUserIds:[9007199254740992]}],[{...node,resolvedUserIds:['9223372036854775808']}],[{...node,resolvedUserIds:['73']}]] )
    assert.throws(()=>people.resolvedUploadApplicationPeople({...route,nodes},{id:'71',deptId:'8'},accounts))
  for(const actor of [{id:'73',deptId:'8'},{id:9007199254740992,deptId:'8'},{id:'71',deptId:'10'},{id:'71'}])
    assert.throws(()=>people.resolvedUploadApplicationPeople(route,actor,accounts))
  for(const rows of [[...accounts,compiler],[compiler],accounts.map(row=>row===supervisor?{...row,deptName:undefined}:row)])
    assert.throws(()=>people.resolvedUploadApplicationPeople(route,{id:'71',deptId:'8'},rows))
  assert.throws(()=>people.resolvedUploadApplicationPeople({...route,ready:false},{id:'71',deptId:'8'},accounts))
})
test('IC31 external review confirmation preserves its original flow without native compiler or supervisor',()=>{
  const rows=helpers.buildUploadConfirmationSummary({...draft,processType:'EXTERNAL_REVIEW'}, {fileName:'EXTERNAL.PDF'}, {...facts,compiler:undefined,supervisor:undefined})
  assert.equal(rows.some(row=>row.label==='编制人'||row.label==='直属主管'),false)
  assert.ok(rows.some(row=>row.label==='批准人'))
})
