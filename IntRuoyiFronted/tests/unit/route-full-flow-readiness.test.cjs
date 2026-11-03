const assert = require('node:assert/strict')
const { test } = require('node:test')
const fs = require('node:fs')
const path = require('node:path')
const { parse, compileTemplate } = require('vue/compiler-sfc')
const root = path.resolve(__dirname, '../..')
const filename = path.join(root, 'src/views/mes/pro/route/RouteFlowGraphDesigner.vue')
const source = fs.readFileSync(filename, 'utf8')
const api = fs.readFileSync(path.join(root, 'src/api/mes/pro/route/index.ts'), 'utf8')

test('end boundary replaces the removed explanation and relation list with detection', () => {
  const leftPanel = source.slice(source.indexOf('data-flow-panel="selected-boundary-detail"'), source.indexOf('<el-empty v-else-if="!selectedNode"'))
  assert.match(leftPanel, /<template v-else>[\s\S]*data-flow-action="check-full-flow-readiness"[\s\S]*handleCheckFullFlowReadiness/)
  assert.doesNotMatch(leftPanel, /边界节点属性暂为只读|selectedBoundaryRelations|boundary-relation-list/)
  assert.match(leftPanel, /data-flow-boundary-field="releaseOwner"/)
})

test('read-only detection carries the exact viewed version and rejects unsaved changes', () => {
  const handler = source.slice(source.indexOf('const handleCheckFullFlowReadiness = async'), source.indexOf('const hasWorkspaceDraftChanges ='))
  assert.match(handler, /props\.routeVersionEditContext\?\.routeVersionId/)
  assert.match(handler, /hasWorkspaceDraftChanges\(\)[\s\S]*throw new Error/)
  assert.match(handler, /checkRouteFullFlowReadiness\(routeId, routeVersionId\)/)
  assert.match(handler, /requestId !== fullFlowReadinessRequestId/)
  assert.match(handler, /fullFlowReadinessResult\.value = undefined/)
  assert.doesNotMatch(handler, /saveRoute|testReset|resetFixed|releaseApprove|fetch\(/)
  const request = api.slice(api.indexOf('checkRouteFullFlowReadiness:'), api.indexOf('// 查询工艺路线工序流转关系图'))
  assert.match(request, /request\.get<RouteFullFlowReadinessVO>/)
  assert.match(request, /params: \{ routeId, routeVersionId \}/)
})

test('report distinguishes blockers and unverified runtime facts and compiles as Vue', () => {
  assert.match(source, /待验证项完成前不能认定整条链路通过/)
  assert.match(source, /fullFlowReadinessError/)
  assert.match(source, /row\.status === 'PASS' \? '通过' : row\.status === 'BLOCKED' \? '阻断' : '待验证'/)
  const { descriptor, errors } = parse(source, { filename })
  assert.deepEqual(errors, [])
  const result = compileTemplate({ source: descriptor.template.content, filename, id: 'readiness-test', compilerOptions: { expressionPlugins: ['typescript'] } })
  assert.deepEqual(result.errors, [])
})

test('the viewed active version keeps its full long ID and explicit draft context', () => {
  const ts = require('typescript')
  const vm = require('node:vm')
  const queryIds = { exports: {} }
  vm.runInNewContext(ts.transpileModule(fs.readFileSync(path.join(root, 'src/utils/routeQueryId.ts'), 'utf8'), {
    compilerOptions: { module: ts.ModuleKind.CommonJS }
  }).outputText, { exports: queryIds.exports })
  const content = fs.readFileSync(path.join(root, 'src/views/mes/pro/route/RouteFormContent.vue'), 'utf8')
  const contextCode = content.slice(content.indexOf('const routeFlowVersionEditContext ='), content.indexOf('const isEditable ='))
  const code = ts.transpileModule(contextCode, {}).outputText + '\nrouteFlowVersionEditContext.value'
  const evaluate = (activeRouteVersionId, explicit) => vm.runInNewContext(code, {
    computed: fn => ({ get value() { return fn() } }),
    routeVersionEditContext: { value: explicit },
    formData: { value: { activeRouteVersionId, activeRouteVersionNo: 'V19' } },
    parsePositiveRouteQueryId: queryIds.exports.parsePositiveRouteQueryId
  })
  assert.equal(evaluate('2116486795650920448').routeVersionId, '2116486795650920448')
  assert.equal(evaluate(803).routeVersionId, '803')
  assert.equal(evaluate(null), undefined)
  const draft = { routeVersionId: '2116486795650920449', versionNo: 'V20', lifecycleStatus: 'DRAFT' }
  assert.equal(evaluate('2116486795650920448', draft), draft)
})
