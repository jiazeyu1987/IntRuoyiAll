const {test}=require('node:test')
const assert=require('node:assert/strict')
const fs=require('node:fs')
const ts=require('typescript')
const out={}
new Function('exports',ts.transpileModule(fs.readFileSync('src/utils/businessTime.ts','utf8'),{
 compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}
}).outputText)(out)

test('business timestamps keep seconds and Beijing calendar day in every browser zone',()=>{
 const originalZone=process.env.TZ
 try{
  for(const zone of ['America/New_York','Asia/Shanghai','UTC']){
   process.env.TZ=zone
   for(const value of [1794970712963,'1794970712963',new Date(1794970712963)])
    assert.equal(out.formatBusinessDateTime(value),'2026-11-18 10:58:32')
   assert.equal(out.businessDateTimeFormatter({}, {},1794933475000),'2026-11-18 00:37:55')
  }
 }finally{if(originalZone===undefined)delete process.env.TZ;else process.env.TZ=originalZone}
})

test('missing dates and invalid date-time payloads remain distinguishable',()=>{
 assert.equal(out.formatBusinessDateTime(null),'--')
 assert.equal(out.businessDateTimeFormatter({}, {},null),'')
 for(const value of ['bad','2026-11-18 10:58:32',NaN,Infinity,new Date(NaN)])
  assert.equal(out.formatBusinessDateTime(value),'时间格式异常')
})
