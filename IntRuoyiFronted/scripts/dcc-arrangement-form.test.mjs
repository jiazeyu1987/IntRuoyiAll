import assert from 'node:assert/strict'
import {readFileSync} from 'node:fs'
import vm from 'node:vm'
import ts from 'typescript'
import test from 'node:test'
const c={exports:{},Error,Date};vm.createContext(c)
vm.runInContext(ts.transpileModule(readFileSync(new URL('../src/views/dcc/controlled-file/relations/arrangement-form.ts',import.meta.url),'utf8'),{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}}).outputText,c)
const validate=c.exports.validateArrangementForm
const relations=[{masterId:'20'},{masterId:'30'}],assignees=[{id:'8',name:'会签整改甲'},{id:'9',name:'会签整改乙'}]
test('unselected relations create no arrangement; selected payload uses explicit assignee and absolute deadline',()=>{
  assert.equal(validate([],relations,assignees).length,0)
  const result=validate([{relatedMasterId:'20',assigneeUserId:'8',dueAt:'2026-10-03 12:00:00'}],relations,assignees)
  assert.equal(result[0].assigneeUserId,'8');assert.equal(result[0].dueAt,'2026-10-03 12:00:00');assert.equal(result.length,1)
})
test('missing person or deadline and duplicate or unbound relation reject without manufacturing defaults',()=>{
  assert.throws(()=>validate([{relatedMasterId:'20'}],relations,assignees),/负责人/)
  assert.throws(()=>validate([{relatedMasterId:'20',assigneeUserId:'8'}],relations,assignees),/期限/)
  const row={relatedMasterId:'20',assigneeUserId:'8',dueAt:'2026-10-03 12:00:00'}
  assert.throws(()=>validate([row,row],relations,assignees),/重复/)
  assert.throws(()=>validate([{...row,relatedMasterId:'99'}],relations,assignees),/关联/)
  assert.throws(()=>validate([{...row,assigneeUserId:'77'}],relations,assignees),/负责人/)
})
test('deadline validates real calendar including leap days and exact seconds without timezone guessing',()=>{
  const row={relatedMasterId:'20',assigneeUserId:'8',dueAt:'2028-02-29 23:59:59'}
  assert.equal(validate([row],relations,assignees)[0].dueAt,row.dueAt)
  for(const dueAt of ['2026-02-29 12:00:00','2026-04-31 12:00:00','2026-10-03 24:00:00','2026-10-03T12:00:00Z','2026-10-03 12:00','2026-10-03 12:00:00.001'])
    assert.throws(()=>validate([{...row,dueAt}],relations,assignees),/期限/)
})
