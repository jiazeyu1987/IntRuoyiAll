import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import ts from 'typescript'
import vm from 'node:vm'
import test from 'node:test'

const path = new URL('../src/views/dcc/controlled-file/relations/selector-state.ts', import.meta.url)
const setup = () => {
  const c = { exports: {}, Error }; vm.createContext(c)
  vm.runInContext(ts.transpileModule(readFileSync(fileURLToPath(path), 'utf8'), {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
  }).outputText, c)
  return new c.exports.FileSelectorState({ contextKey: 'tenant1:file1', tenantId: '1', masterId: '10' })
}
const row = (masterId, fileId = masterId, extras = {}) => ({ tenantId: '1', masterId, controlledFileId: fileId,
  projectId: '20', fileNumber: 'N', fileName: '测试.pdf', versionNo: 'A/1', status: 'ACTIVE',
  controlled: true, pendingEffect: false, executable: true, canPreview: false, ...extras })
test('selection persists across projects and pages, deduplicates masters and rejects self/cross tenant', () => {
  const s = setup(); s.select(row('20')); s.select(row('20', '200')); s.select(row('30'))
  assert.equal(s.selected.length, 2)
  assert.throws(() => s.select(row('10')), /SELF/)
  assert.throws(() => s.select(row('40', '40', { tenantId: '2' })), /TENANT/)
  const request = s.begin({ projectId: '99', folderId: '88', keyword: '', pageNo: 2, pageSize: 10 })
  s.resolve(request, { total: 110, list: [row('50')] })
  assert.equal(s.selected.length, 2); assert.equal(s.total, 110); assert.equal(s.rows[0].masterId, '50')
})
test('global search sends server keyword and stale responses cannot replace newer context', () => {
  const s = setup(); const old = s.begin({ keyword: '旧', pageNo: 2, pageSize: 10 })
  const latest = s.begin({ keyword: '新', pageNo: 1, pageSize: 10 })
  assert.equal(latest.query.projectId, undefined); assert.equal(latest.query.keyword, '新')
  s.resolve(latest, { total: 1, list: [row('30')] }); s.resolve(old, { total: 1, list: [row('20')] })
  assert.equal(s.rows[0].masterId, '30')
  s.reset({ contextKey: 'tenant1:file2', tenantId: '1', masterId: '11' }, [])
  s.resolve(latest, { total: 1, list: [row('30')] }); assert.equal(s.rows.length, 0); assert.equal(s.selected.length, 0)
})
test('errors are visible and stale errors cannot contaminate a new request', () => {
  const s = setup(); const old = s.begin({ keyword: '旧', pageNo: 1, pageSize: 10 })
  const latest = s.begin({ keyword: '新', pageNo: 1, pageSize: 10 })
  s.reject(old, new Error('旧错误')); assert.equal(s.error, '')
  s.reject(latest, new Error('查询失败')); assert.equal(s.error, '查询失败'); assert.equal(s.loading, false)
})
test('pending effect and obsoletion are explicit; metadata visibility never grants content', () => {
  const s = setup(); s.select(row('20', '200', { pendingEffect: true, executable: false }))
  assert.equal(s.label(s.selected[0]), '待生效')
  assert.throws(() => s.assertPreview(s.selected[0]), /CONTENT/)
  assert.equal(s.label(row('30', '30', { controlled: false, executable: false, status: 'OBSOLETE' })), '源文件已作废')
  assert.throws(() => s.select(row('30', '30', { controlled: false })), /CONTROLLED/)
})
test('saving is context-bound, cancellation does not save, failures preserve the selection', async () => {
  const s=setup();s.select(row('20'));let writes=0
  assert.equal(await s.save(async()=>{writes++}, false),false);assert.equal(writes,0)
  assert.equal(await s.save(async()=>{throw new Error('保存失败')},true),false)
  assert.equal(s.error,'保存失败');assert.equal(s.selected.length,1)
  let finish;const pending=s.save(()=>new Promise(resolve=>{finish=resolve}),true)
  s.reset({contextKey:'tenant1:file2',tenantId:'1',masterId:'11'},[]);finish();assert.equal(await pending,false)
})
test('obsolete existing choices remain visible for removal and cannot be submitted as controlled', async()=>{
  const s=setup();s.reset({contextKey:'tenant1:file1',tenantId:'1',masterId:'10'},[row('20','200',{controlled:false,status:'OBSOLETE'})])
  assert.equal(s.selected.length,1);let writes=0
  assert.equal(await s.save(async()=>{writes++},true),false);assert.equal(writes,0)
  s.remove('20');assert.equal(await s.save(async()=>{writes++},true),true)
})
test('selection changes during pending save are rejected so saved and emitted selection agree', async()=>{
  const s=setup();s.select(row('20'));let finish
  const pending=s.save(()=>new Promise(resolve=>{finish=resolve}),true)
  assert.throws(()=>s.select(row('30')),/SAVING/);assert.throws(()=>s.remove('20'),/SAVING/)
  finish();assert.equal(await pending,true);assert.equal(s.selected.length,1)
})
test('single file operation mode keeps the clicked file as the only selection across pages',()=>{
  const s=setup();s.reset({contextKey:'tenant1:operation',tenantId:'1',selectionMode:'single'},[])
  s.select(row('20'));s.select(row('30'));assert.equal(s.selected.length,1);assert.equal(s.selected[0].masterId,'30')
  const query=s.begin({keyword:'cross project',pageNo:2,pageSize:20});s.resolve(query,{total:30,list:[row('40')]})
  assert.equal(s.selected[0].masterId,'30')
})
test('operation mode may select its current file while relations still forbid self linkage',()=>{
  const s=setup();s.reset({contextKey:'tenant1:operation',tenantId:'1',masterId:'10',selectionMode:'single',forbidSelfRelation:false},[])
  s.select(row('10'));assert.equal(s.selected[0].masterId,'10')
  s.reset({contextKey:'tenant1:relations',tenantId:'1',masterId:'10'},[])
  assert.throws(()=>s.select(row('10')),/SELF/)
})
test('reference and file operation confirmation require a selected file, empty relation clear remains valid',async()=>{
  const s=setup();let saves=0
  s.reset({contextKey:'operation',tenantId:'1',selectionMode:'single',allowEmptySelection:false},[])
  assert.equal(await s.save(async()=>{saves++},true),false);assert.equal(saves,0)
  s.reset({contextKey:'reference',tenantId:'1',allowEmptySelection:false,forbidSelfRelation:false},[])
  assert.equal(await s.save(async()=>{saves++},true),false);assert.equal(saves,0)
  s.reset({contextKey:'relations',tenantId:'1'},[])
  assert.equal(await s.save(async()=>{saves++},true),true);assert.equal(saves,1)
})
test('selector rejects lossy numeric IDs and malformed decimal identities before selection or response publication',()=>{
  const s=setup();assert.throws(()=>s.select(row(9007199254740992,'20')),/IDENTITY/)
  assert.throws(()=>s.select(row('020','200')),/IDENTITY/)
  const token=s.begin({keyword:'test',pageNo:1,pageSize:20})
  assert.throws(()=>s.resolve(token,{total:1,list:[row('20','200',{projectId:0})]}),/IDENTITY/)
  assert.equal(s.rows.length,0)
})
test('candidate page must contain one exact latest identity per master and cannot mark pending as executable',()=>{
  const s=setup();let token=s.begin({keyword:'test',pageNo:1,pageSize:20})
  assert.throws(()=>s.resolve(token,{total:2,list:[row('20','200'),row('20','201')]}),/DUPLICATE/)
  assert.equal(s.rows.length,0);token=s.begin({keyword:'pending',pageNo:1,pageSize:20})
  assert.throws(()=>s.resolve(token,{total:1,list:[row('20','200',{pendingEffect:true,executable:true})]}),/VERSION/)
  assert.equal(s.rows.length,0)
})
