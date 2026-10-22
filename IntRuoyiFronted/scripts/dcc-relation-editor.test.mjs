import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import vm from 'node:vm'
import ts from 'typescript'
import test from 'node:test'

const setup=()=>{
  const c={exports:{},Error,crypto:globalThis.crypto};vm.createContext(c)
  vm.runInContext(ts.transpileModule(readFileSync(new URL('../src/views/dcc/controlled-file/relations/relation-editor-state.ts',import.meta.url),'utf8'),{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}}).outputText,c)
  let count=0;const editor=new c.exports.RelationEditorState(()=>`key-${++count}`)
  editor.setCurrent({sourceControlledFileId:'100',rowVersion:'0',files:[]});editor.reason='关联原因';return editor
}
const row={controlledFileId:'200',masterId:'20'}
test('formal save sends exact revision and stable key; failed retry reuses identity and success publishes server version',async()=>{
  const editor=setup(),commands=[];let fail=true
  const persist=async(source,command)=>{commands.push({...command});assert.equal(source,'100');if(fail)throw new Error('响应丢失');return {sourceControlledFileId:'100',rowVersion:'1',relatedMasterIds:['20']}}
  await assert.rejects(()=>editor.save([row],persist),/响应丢失/);fail=false;await editor.save([row],persist)
  assert.equal(commands[0].idempotencyKey,commands[1].idempotencyKey);assert.equal(commands[0].expectedVersion,'0');assert.equal(editor.current.rowVersion,'1')
})
test('changed selection or reason produces a different command identity and blank reason never calls save',async()=>{
  const editor=setup(),commands=[];const persist=async(source,command)=>{commands.push(command);throw new Error('失败')}
  await assert.rejects(()=>editor.save([row],persist));editor.reason='修改原因';await assert.rejects(()=>editor.save([row],persist))
  assert.notEqual(commands[0].idempotencyKey,commands[1].idempotencyKey);editor.reason=' ';await assert.rejects(()=>editor.save([row],persist),/原因/);assert.equal(commands.length,2)
})
test('context reset invalidates pending formal save and malformed result cannot publish success',async()=>{
  const editor=setup();let finish;const pending=editor.save([row],()=>new Promise(resolve=>{finish=resolve}))
  editor.setCurrent({sourceControlledFileId:'101',rowVersion:'2',files:[]});finish({sourceControlledFileId:'100',rowVersion:'1',relatedMasterIds:['20']})
  await assert.rejects(()=>pending,/上下文/);assert.equal(editor.current.sourceControlledFileId,'101')
  await assert.rejects(()=>editor.save([row],async()=>({sourceControlledFileId:'100',rowVersion:'3',relatedMasterIds:['20']})),/结果/)
})
