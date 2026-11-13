const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')
const ts = require('typescript')
const vue = require('vue')
function load(path) {
  const exports = {}
  vm.runInNewContext(ts.transpileModule(fs.readFileSync(path, 'utf8'), {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
  }).outputText, { exports, Error, require: () => vue })
  return exports
}
const { useListColumnFilter, serializeListFilters } = load('src/hooks/web/useListColumnFilter.ts')
const { useLatestListRequest } = load('src/hooks/web/useServerListFilter.ts')
const plain = value => JSON.parse(JSON.stringify(value))
const deferred = () => { let resolve, reject; const promise = new Promise((a,b) => {resolve=a;reject=b}); return {promise, resolve, reject} }
const condition = (value, operator='contains') => ({ id: 'ui-only', key:'name',operator,value })
function setup(query) {
  const model = { pageNo: 7, pageSize: 20 }
  const filter = useListColumnFilter([{key:'name',label:'Name',type:'text'}], () => [],
    () => () => { throw Error('must not fetch all pages') },
    { enabled: () => true, queryModel: () => model, query })
  return {model, filter}
}
test('server conditions serialize all AND clauses, operators, zero and false without UI IDs', () => {
  const rows = ['contains','eq','in','between','gte','lte'].map(operator => ({...condition(operator==='in' ? [0,false] : 0,operator), valueEnd: operator==='between' ? 5 : undefined}))
  const parsed = JSON.parse(serializeListFilters(rows))
  assert.equal(parsed.length,6)
  assert.equal(parsed[0].id,undefined)
  assert.deepEqual(parsed[2].value,[0,false])
  assert.equal(serializeListFilters([]),undefined)
  assert.throws(() => serializeListFilters([condition(1,'between')]), /范围/)
})
test('draft remains unapplied; exactly one paged server query commits only on success', async () => {
  const pending = deferred(); let calls=0
  const {model,filter}=setup(() => {calls++;return pending.promise})
  filter.state.value.conditions=[condition('A')]
  assert.equal(filter.active.value,false)
  const work=filter.apply()
  assert.equal(calls,1);assert.equal(model.pageNo,1);assert.equal(model.pageSize,20)
  assert.equal(JSON.parse(model.listFilters)[0].value,'A');assert.equal(filter.active.value,false)
  filter.state.value.conditions[0].value='B'
  pending.resolve(true);await work
  assert.equal(filter.state.value.appliedConditions[0].value,'A')
  assert.equal(filter.state.value.conditions[0].value,'B')
})
test('query failure restores successful filters, surfaces error, reset removes server params', async () => {
  let fail=false
  const {model,filter}=setup(async () => {if(fail)throw Error('network failure')})
  filter.state.value.conditions=[condition('A')];await filter.apply()
  fail=true;filter.state.value.conditions=[condition('B')]
  await assert.rejects(filter.apply(),/network failure/)
  assert.equal(JSON.parse(model.listFilters)[0].value,'A')
  assert.equal(filter.state.value.appliedConditions[0].value,'A')
  assert.equal(filter.error.value,'network failure');assert.equal(filter.loading.value,false)
  filter.reset();assert.equal('listFilters' in model,false);assert.equal(filter.active.value,false)
})
test('latest request alone publishes rows and controls loading; errors are never swallowed', async () => {
  const request=useLatestListRequest();const a=deferred();const b=deferred();const commits=[]
  const first=request.run(()=>a.promise,x=>commits.push(x))
  const second=request.run(()=>b.promise,x=>commits.push(x))
  a.resolve('old');assert.equal(await first,false);assert.equal(request.loading.value,true)
  b.resolve('new');assert.equal(await second,true);assert.deepEqual(commits,['new'])
  assert.equal(request.loading.value,false)
  await assert.rejects(request.run(async()=>{throw Error('failed')},()=>{}),/failed/)
  assert.equal(request.loading.value,false)
})
test('reset prevents pending results from reapplying conditions', async () => {
  const pending=deferred();const {model,filter}=setup(()=>pending.promise)
  filter.state.value.conditions=[condition('A')];const work=filter.apply();filter.reset()
  pending.resolve(true);await work
  assert.equal(filter.active.value,false);assert.equal(model.listFilters,undefined)
})
