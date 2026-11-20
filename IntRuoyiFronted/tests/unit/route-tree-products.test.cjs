const assert = require('node:assert/strict')
const fs = require('node:fs'), path = require('node:path'), vm = require('node:vm')
const test = require('node:test'), ts = require('typescript'), vue = require('vue')
const {parse, compileScript, compileTemplate} = require('vue/compiler-sfc')
const filename = path.resolve(__dirname, '../../src/views/mes/pro/route/RouteTreeProducts.vue')
const {descriptor} = parse(fs.readFileSync(filename, 'utf8'))
const candidate = {id: 11, routeId: 1, versionNo: 'V2', lifecycleStatus: 'DRAFT', active: false}
function setup() {
  const state = {versions: [], rows: [{itemId: 1, itemCode: 'P1'}], bound: true, permission: true, creates: [], deletes: [], reads: []}
  const props = {route: {id: 1, activeRouteVersionId: 10}, publishing: false}
  const errors = [], successes = [], events = [], out = {}; let dispose
  const api = {
    getRouteVersionList: async () => state.versions,
    getRouteDccProjectBinding: async () => ({bound: state.bound, dccProjectCodeId: state.bound ? 2 : undefined})
  }
  const productsApi = {
    getRouteProductListByRoute: async (routeId, versionId) => {
      state.reads.push([routeId, versionId]); return state.rows.map(row => ({...row}))
    },
    createRouteProduct: async row => {
      assert.equal(row.routeVersionId, 11)
      if (state.failItem === row.itemId) throw Error('产品绑定冲突')
      state.creates.push(row.itemId); state.rows.push({itemId: row.itemId, itemCode: 'P'+row.itemId})
    },
    deleteCandidateRouteProduct: async (routeId, itemId, versionId) => {
      assert.equal(versionId, 11); state.deletes.push(itemId); state.rows = state.rows.filter(row => row.itemId !== itemId)
    }
  }
  const code = descriptor.scriptSetup.content + '\nObject.assign(exports,{products,draft,loadProducts,addProducts,removeProduct,busy,error,selectorVisible,filteredProducts,filter,locked})'
  vm.runInNewContext(ts.transpileModule(code, {compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}}).outputText,
    {...vue, exports: out, defineProps:()=>props, defineEmits:()=> (...args)=>events.push(args),
      onMounted(){}, onBeforeUnmount: callback => {dispose=callback},
      useMessage:()=>({success: value=>successes.push(value),error: value=>errors.push(value),
        delConfirm: async()=>{if(state.cancel) throw 'cancel'}}),
      require: name => name === '@/api/mes/pro/route' ? {ProRouteApi:api}
        : name.endsWith('/product') ? {ProRouteProductApi:productsApi}
        : name.endsWith('/hasPermi') ? {hasPermission:()=>state.permission}
        : name === './routeCandidateEntry' ? {ensureSameSourceDraftCandidateForProductionConfig:async()=>{
          if(state.candidateError) throw Error(state.candidateError)
          state.versions=[candidate]; return {candidate,created:true}
        }} : name === './routeError' ? {isRouteConfirmCancel: value=>value==='cancel',resolveRouteOperationErrorMessage:value=>value.message} : {}})
  return {...out,state,props,errors,successes,events,dispose:()=>dispose()}
}
test('product component compiles with add, unbind and draft notice',()=>{
  compileScript(descriptor,{id:'products'})
  assert.deepEqual(compileTemplate({source:descriptor.template.content,filename,id:'products'}).errors,[])
})
test('reads explicit published or draft snapshots and supports product code search',async()=>{
  const h=setup(); await h.loadProducts()
  assert.equal(h.state.reads[0][1],10)
  h.state.versions=[candidate]; await h.loadProducts()
  assert.equal(h.state.reads[1][1],11)
  assert.equal(h.draft.value.id,11)
  h.filter.value=' p1 '; assert.equal(h.filteredProducts.value.length,1)
  h.filter.value='P2'; assert.equal(h.filteredProducts.value.length,0)
})
test('adds only new products to the draft, deduplicates selection and refreshes',async()=>{
  const h=setup(); await h.loadProducts()
  await h.addProducts([{id:1},{id:2},{id:2},{id:3}])
  assert.deepEqual(h.state.creates,[2,3])
  assert.equal(h.products.value.length,3)
  assert.match(h.successes[0],/2 个产品到草稿/)
  assert.equal(h.events.at(-1)[0],'changed')
  assert.equal(h.busy.value,false)
})
test('partial batch failure reports actual progress and reloads the saved draft',async()=>{
  const h=setup(); h.state.failItem=3
  await h.addProducts([{id:2},{id:3},{id:4}])
  assert.deepEqual(h.state.creates,[2])
  assert.equal(h.products.value.length,2)
  assert.match(h.errors[0],/已添加 1 个产品；产品绑定冲突/)
  assert.equal(h.successes.length,0)
})
test('unbind confirmation changes only draft; cancel writes nothing',async()=>{
  const h=setup(); h.state.cancel=true
  await h.removeProduct({itemId:1,itemCode:'P1'})
  assert.deepEqual(h.state.deletes,[])
  h.state.cancel=false; h.state.bound=false; await h.removeProduct({itemId:1,itemCode:'P1'})
  assert.deepEqual(h.state.deletes,[1]); assert.equal(h.products.value.length,0)
  assert.match(h.successes[0],/从草稿解绑/)
})
test('missing project, permission, invalid candidate or active publishing prevent writes',async()=>{
  for(const condition of ['project','permission','candidate','publishing','disposed']) {
    const h=setup()
    if(condition==='project') h.state.bound=false
    if(condition==='permission') h.state.permission=false
    if(condition==='candidate') h.state.candidateError='草稿来源已变化'
    if(condition==='publishing') h.props.publishing=true
    if(condition==='disposed') h.dispose()
    await h.addProducts([{id:2}])
    assert.deepEqual(h.state.creates,[],condition)
    assert.equal(h.busy.value,false)
  }
})
test('multiple drafts are explicit errors and pending approval locks editing',async()=>{
  const h=setup(); h.state.versions=[candidate,{...candidate,id:12}]
  await assert.rejects(h.loadProducts(),/多个草稿/)
  assert.match(h.error.value,/多个草稿/)
  h.state.versions=[{...candidate,lifecycleStatus:'PENDING_APPROVAL'}]; await h.loadProducts()
  assert.equal(h.locked.value,true)
})
