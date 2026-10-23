const {test}=require('node:test')
const assert=require('node:assert/strict')
const fs=require('node:fs')
const path=require('node:path')
const vm=require('node:vm')
const ts=require('typescript')
const root=path.resolve(__dirname,'../..')
const helper=path.join(root,'src/views/dcc/controlled-file/workflow/effective-facts.ts')
function load(){assert.ok(fs.existsSync(helper),'effective facts presentation must be formally implemented')
  const context={exports:{},Error};vm.runInNewContext(ts.transpileModule(fs.readFileSync(helper,'utf8'),{
    compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}}).outputText,context);return context.exports}
test('signed effectiveness and daily activation remain independent displayed facts',()=>{
  const view=load().effectiveFacts({effectiveMode:'AFTER_STANDARDIZATION_SIGNATURE',effectiveDate:null,
    effectiveTime:'2026-10-11 10:00:00',activatedTime:null})
  assert.equal(view.modeText,'文控标准化签名后生效');assert.equal(view.effectiveText,'2026-10-11 10:00:00')
  assert.equal(view.activatedText,'尚未日常启用');assert.equal(view.presetDateVisible,false)
})
test('historical mode is unknown even with original effective calendar date',()=>{
  const view=load().effectiveFacts({effectiveDate:'2026-10-12',activatedTime:'2026-10-13 08:00:00'})
  assert.equal(view.modeText,'未记录');assert.equal(view.effectiveText,'未记录')
  assert.equal(view.activatedText,'2026-10-13 08:00:00');assert.equal(view.presetDateVisible,true)
})
test('preset future has no invented effectiveness and rejects unknown enum',()=>{
  const view=load().effectiveFacts({effectiveMode:'PRESET_DATE',effectiveDate:'2026-10-12'})
  assert.equal(view.modeText,'按预设日期生效');assert.equal(view.effectiveText,'尚未生效')
  assert.equal(view.presetDateVisible,true)
  assert.throws(()=>load().effectiveFacts({effectiveMode:'LEGACY',effectiveDate:'2026-10-12'}))
})
test('both W06 views consume separate effective and activated facts',()=>{
  for(const file of ['workflow/ControlledLifecyclePanel.vue','detail/ControlledFileBrowseRecords.vue']){
    const source=fs.readFileSync(path.join(root,'src/views/dcc/controlled-file',file),'utf8')
    assert.match(source,/effectiveFacts/);assert.match(source,/effectiveText/);assert.match(source,/activatedText/)
    assert.ok(source.includes('实际生效时间'));assert.ok(source.includes('日常启用时间'))
  }
})
