const {test}=require('node:test')
const assert=require('node:assert/strict')
const fs=require('node:fs')
const vm=require('node:vm')
const source=fs.readFileSync(__dirname+'/ChangeDialog.vue','utf8')

test('remote search retains already selected enterprise options',async()=>{
 const context={enterprises:{value:[{id:12,name:'Selected enterprise'},{id:20,name:'Old result'}]},enterpriseIds:{value:[12]},getUploadEntrustedEnterprises:async()=>[{id:30,name:'New result'}]}
 vm.createContext(context)
 vm.runInContext(source.match(/const searchEnterprises=async[^\n]+/)[0]+';globalThis.search=searchEnterprises',context)
 await context.search('new')
 assert.deepEqual(JSON.parse(JSON.stringify(context.enterprises.value)),[{id:12,name:'Selected enterprise'},{id:30,name:'New result'}])
})
