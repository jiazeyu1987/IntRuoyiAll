const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const ts = require('typescript')
const vue = require('vue')
const compiler = require('vue/compiler-sfc')
;(async () => {
for (const kind of ['iqc', 'ipqc', 'oqc', 'rqc']) {
 const file = path.join(__dirname, `../src/views/mes/qc/${kind}/index.vue`)
 const source = fs.readFileSync(file, 'utf8')
 const { descriptor, errors } = compiler.parse(source)
 assert.equal(errors.length, 0)
 compiler.compileScript(descriptor, { id: kind })
 const body = 'let listRequestSerial = 0; const loading = { value: false };\n' + source.slice(source.indexOf('const codeLinkRoute ='), source.lastIndexOf('</script>'))
 const compiled = ts.transpileModule(body, { compilerOptions: { target: ts.ScriptTarget.ES2020 } }).outputText
 const route = vue.reactive({ path: `/mes/qc/${kind}`, query: { code: ' A/B 中文 ' } })
 const params = vue.reactive({ pageNo: 9, pageSize: 10, code: undefined, vendorId: 42 })
 let count = 0
 const scope = vue.effectScope()
 scope.run(() => new Function('useRoute','watch','queryParams','getList',compiled)(() => route, vue.watch, params, () => count++))
 assert.equal(params.code, 'A/B 中文'); assert.equal(params.vendorId, undefined); assert.equal(params.pageNo, 1); assert.equal(count, 1)
 route.query.codeLinkVisit = '2'; await vue.nextTick(); assert.equal(count, 2)
 route.path = '/other'; await vue.nextTick(); assert.equal(count, 2)
 route.path = `/mes/qc/${kind}`; await vue.nextTick(); assert.equal(count, 3)
 route.query.code = 'NEW'; await vue.nextTick(); assert.equal(params.code, 'NEW'); assert.equal(count, 4)
 scope.stop()
}
console.log('Inspection code route behavior: PASS (4 types)')
})().catch(error => { console.error(error); process.exitCode = 1 })
;(async () => {
for (const kind of ['iqc','ipqc','oqc','rqc']) {
 const source=fs.readFileSync(path.join(__dirname,`../src/views/mes/qc/${kind}/index.vue`),'utf8').replace(/\r/g,'')
 const start=source.indexOf('let listRequestSerial'); const end=source.indexOf('/** 搜索按钮',start)
 const body=source.slice(start,end)
 const route=vue.reactive({path:`/mes/qc/${kind}`,query:{code:'A'}}), pending=[]
 const list={value:[]},total={value:0},loading={value:false},params={code:'A'}
 const api={};api[`get${kind[0].toUpperCase()+kind.slice(1)}Page`]=()=>new Promise((resolve,reject)=>pending.push({resolve,reject}))
 const run=new Function('codeLinkRoute','queryParams','list','total','loading',`Qc${kind[0].toUpperCase()+kind.slice(1)}Api`,ts.transpileModule(body+'\nreturn getList',{compilerOptions:{target:ts.ScriptTarget.ES2020}}).outputText)(route,params,list,total,loading,api)
 const a=run();route.query.code='B';params.code='B';const b=run();pending[1].resolve({list:['B'],total:2});await b;pending[0].resolve({list:['A'],total:1});await a;assert.deepEqual(list.value,['B']);assert.equal(total.value,2);assert.equal(loading.value,false)
 const c=run();route.path='/other';pending[2].resolve({list:['old'],total:9});await c;assert.deepEqual(list.value,['B']);assert.equal(total.value,2)
 const d=run();pending[3].reject(new Error('real error'));await assert.rejects(d,/real error/)
}
console.log('inspection delayed reverse/leave/error behavior PASS')
})().catch(e=>{console.error(e);process.exitCode=1})
