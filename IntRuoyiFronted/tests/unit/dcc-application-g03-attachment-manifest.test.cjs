const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),ts=require('typescript'),vm=require('node:vm')
function load(){const source=fs.readFileSync(path.resolve(__dirname,'../../src/views/dcc/controlled-file/revision/revision-model.ts'),'utf8'),c={exports:{},Error,require:name=>{if(name==='../project-attributes/state')return {validateAttributes:()=>{throw Error('Not an attachment action')}};throw Error(name)}};vm.runInNewContext(ts.transpileModule(source,{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}}).outputText,c);return c.exports}
const file=()=>({id:'20',ordinaryAttachmentManifestHash:'a'.repeat(64),attachments:[{attachmentId:'71',controlledFileId:'20',fileName:'One.pdf',attachmentPurpose:'ATTACHMENT',sortNo:0},{attachmentId:'72',controlledFileId:'20',fileName:'Two.pdf',attachmentPurpose:'ATTACHMENT',sortNo:1},{attachmentId:'73',controlledFileId:'20',fileName:'Training.pdf',attachmentPurpose:'TRAINING_RECORD',sortNo:2}]})
test('actual application helper sends ordered ordinary IDs and exact server hash while retaining current training for confirmation',()=>{
 const result=load().readApplicationOrdinaryAttachmentExpectation(file());assert.deepEqual(JSON.parse(JSON.stringify(result.expectedOrdinaryAttachmentIds)),['71','72']);assert.equal(result.expectedOrdinaryAttachmentManifestHash,'a'.repeat(64))
})
test('unread manifest hash or rows never become empty success; unknown purpose and other version reject',()=>{
 const fn=load().readApplicationOrdinaryAttachmentExpectation
 for(const wrong of [{...file(),ordinaryAttachmentManifestHash:null},{...file(),attachments:undefined},{...file(),attachments:[{...file().attachments[0],attachmentPurpose:'UNKNOWN'}]},{...file(),attachments:[{...file().attachments[0],controlledFileId:'21'}]}])assert.throws(()=>fn(wrong),/附件/)
 const empty={...file(),attachments:[]};assert.deepEqual(JSON.parse(JSON.stringify(fn(empty).expectedOrdinaryAttachmentIds)),[]);assert.equal(fn(empty).expectedOrdinaryAttachmentManifestHash,'a'.repeat(64))
})
