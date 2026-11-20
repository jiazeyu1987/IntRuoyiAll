const test = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const source = fs.readFileSync(path.resolve(__dirname, '../../src/views/dcc/controlled-file/basic-data/components/ProjectCodeTabPanel.vue'), 'utf8')
const declaration = source.match(/const handleSortChange = [\s\S]*?\n\}/)[0]
test('status sorts reset pagination and previous ordering before requesting the server', () => {
  const h = { queryParams: { pageNo: 4 }, calls: 0, getList() { h.calls++ }, projectCodeTableRef: { value: { sort(prop, order) { h.sort({ prop, order }) } } } }
  vm.createContext(h)
  vm.runInContext(ts.transpile(declaration + '\nglobalThis.sort = handleSortChange'), h)
  for (const [prop, key] of [['routeStatus', 'routeStatusSort'], ['qaRegulationStatus', 'qaRegulationStatusSort'], ['associatedFileCount', 'fileCountSort']]) {
    for (const [order, value] of [['ascending','asc'], ['descending','desc'], [null, prop === 'associatedFileCount' ? undefined : 'desc']]) {
      h.queryParams.pageNo = 4
      h.sort({ prop, order })
      assert.equal(h.queryParams.pageNo, 1)
      for (const field of ['routeStatusSort','qaRegulationStatusSort','fileCountSort']) {
        assert.equal(h.queryParams[field], field === key ? value : undefined)
      }
    }
  }
  assert.equal(h.calls, 9)
})

test('repeated active arrows keep status ordering and restore the visual arrow', () => {
  const h = { queryParams: {}, calls: 0, restored: [], getList() { h.calls++ },
    projectCodeTableRef: { value: { sort(prop, order) { h.restored.push([prop, order]); h.sort({prop,order}) } } } }
  vm.createContext(h)
  vm.runInContext(ts.transpile(declaration + '\nglobalThis.sort = handleSortChange'), h)
  for (const [prop, key] of [['routeStatus','routeStatusSort'], ['qaRegulationStatus','qaRegulationStatusSort']]) {
    for (const [order, expected] of [['ascending','asc'], ['descending','desc']]) {
      h.sort({prop,order})
      h.sort({prop,order:null})
      assert.equal(h.queryParams[key], expected)
      assert.deepEqual(h.restored.at(-1), [prop,order])
    }
  }
  assert.equal(h.calls, 8)
})
