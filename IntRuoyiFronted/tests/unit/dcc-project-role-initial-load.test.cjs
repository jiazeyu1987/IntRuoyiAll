const test=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path')
const source=fs.readFileSync(path.resolve(__dirname,'../../src/views/dcc/controlled-file/basic-data/components/ProjectCodeTabPanel.vue'),'utf8').replace(/\r\n/g,'\n')
const body=source.match(/const ensureLoaded = async \(\) => \{([\s\S]*?)\n\}\n\nconst handleSortChange/)[1]
function instance({manage=false,loaded=false,taxonomyFailure}={}){
 const calls=[],hasLoaded={value:loaded}
 const ensureLoaded=new Function('hasLoaded','getList','loadFileTypeTaxonomies','checkPermi',`return async()=>{${body}}`)(hasLoaded,
  async()=>{calls.push('projects')},async()=>{calls.push('taxonomy');if(!manage)throw Error('Unauthorized taxonomy endpoint called');if(taxonomyFailure)throw taxonomyFailure},
  ps=>ps.includes('dcc:controlled-file:category:manage')&&manage)
 return {ensureLoaded,calls,hasLoaded}
}
test('Reviewer-only page initializes its permitted project list without taxonomy access',async()=>{
 const x=instance();await x.ensureLoaded();assert.deepEqual(x.calls,['projects']);assert.equal(x.hasLoaded.value,true)
})
test('Category manager still loads the real taxonomy source',async()=>{
 const x=instance({manage:true});await x.ensureLoaded();assert.deepEqual(x.calls,['projects','taxonomy']);assert.equal(x.hasLoaded.value,true)
})
test('Authorized taxonomy failure is propagated and initialization remains incomplete',async()=>{
 const failure=Error('Actual taxonomy source failed'),x=instance({manage:true,taxonomyFailure:failure})
 await assert.rejects(x.ensureLoaded(),e=>e===failure);assert.equal(x.hasLoaded.value,false)
})
test('An already loaded page does not duplicate either request',async()=>{
 const x=instance({loaded:true});await x.ensureLoaded();assert.deepEqual(x.calls,[])
})
