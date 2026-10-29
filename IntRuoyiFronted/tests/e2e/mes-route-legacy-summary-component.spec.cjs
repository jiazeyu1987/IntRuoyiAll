const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const vue = require('vue')
const sfc = require('vue/compiler-sfc')
const { renderToString } = require('vue/server-renderer')
const root = path.resolve(__dirname, '../..')
const dir = 'src/views/mes/pro/route/'
const read = (file) => fs.readFileSync(path.join(root, file), 'utf8')
const evaluate = (source, dependencies = {}, globals = {}) => {
  const exports = {}
  const code = ts.transpileModule(source, {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2020 }
  }).outputText
  vm.runInNewContext(code, {
    exports,
    require: (id) => (Object.hasOwn(dependencies, id) ? dependencies[id] : require(id)),
    ...globals
  })
  return exports
}
const history = evaluate(read(dir + 'routeVersionHistory.ts'))
const legacy = {
  id: 5,
  routeId: 922022,
  versionNo: 'V1',
  active: false,
  lifecycleStatus: 'LEGACY_INCOMPLETE',
  routeSnapshotFormatVersion: 'MES_ROUTE_LEGACY_SUMMARY_V1',
  routeSnapshotJson:
    '{"routeId":922022,"routeCode":"saved-code","routeName":"保存的旧名称<script>","status":0,"description":"旧说明","remark":null}'
}
const complete = {
  id: 6,
  routeId: 922022,
  versionNo: 'V2',
  active: false,
  lifecycleStatus: 'DRAFT'
}
let count = 0
const check = (fn) => {
  fn()
  count++
}

async function main() {
  check(() => assert.equal(history.isLegacyRouteVersion(legacy), true))
  check(() => assert.equal(history.isLegacyRouteVersion(complete), false))
  check(() =>
    assert.equal(history.isLegacyRouteVersion({ ...legacy, lifecycleStatus: 'DRAFT' }), true)
  )
  check(() => assert.equal(history.requireLegacyRouteSummary(legacy), legacy.routeSnapshotJson))
  check(() =>
    assert.deepEqual(
      Object.keys(history.readLegacyRouteSummaryFields(legacy)).sort(),
      ['routeId', 'routeCode', 'routeName', 'status', 'description', 'remark'].sort()
    )
  )
  check(() =>
    assert.throws(
      () => history.requireLegacyRouteSummary({ ...legacy, routeSnapshotJson: undefined }),
      /未返回真实/
    )
  )
  check(() =>
    assert.throws(
      () => history.readLegacyRouteSummaryFields({ ...legacy, routeSnapshotJson: '[]' }),
      /不是 JSON 对象/
    )
  )
  check(() =>
    assert.throws(() =>
      history.readLegacyRouteSummaryFields({ ...legacy, routeSnapshotJson: 'broken' })
    )
  )
  check(() => assert.equal(history.formatLegacyRouteSummaryField({ status: 0 }, 'status'), '0'))
  check(() => assert.equal(history.formatLegacyRouteSummaryField({}, 'name'), '原摘要未保存此字段'))
  check(() =>
    assert.equal(history.formatLegacyRouteSummaryField({ remark: null }, 'remark'), '原值为 null')
  )

  // Compile and render the actual read-only component; no graph/current route API is available.
  const filename = dir + 'LegacyRouteVersionSummary.vue'
  const parsed = sfc.parse(read(filename), { filename })
  assert.deepEqual(parsed.errors, [])
  const compiled = sfc.compileScript(parsed.descriptor, {
    id: 'legacy-summary-test',
    inlineTemplate: true
  })
  const component = evaluate(compiled.content, { './routeVersionHistory': history }).default
  const app = vue.createSSRApp(component, { version: legacy })
  for (const name of ['el-alert', 'el-descriptions', 'el-descriptions-item', 'el-button']) {
    app.component(name, {
      props: ['title', 'description', 'label'],
      setup:
        (props, { slots }) =>
        () =>
          vue.h('div', [props.title, props.description, props.label, slots.default?.()])
    })
  }
  const html = await renderToString(app)
  check(() => assert.match(html, /历史摘要不完整，不可执行/))
  check(() => assert.match(html, /保存的旧名称&lt;script&gt;/))
  check(() => assert.match(html, /旧说明/))
  check(() => assert.doesNotMatch(html, /原摘要未保存此字段/))
  check(() =>
    assert.doesNotMatch(html, /<script>|RouteFlowGraphDesigner|关联产品|保存草稿|提交发布/)
  )

  let createdRequest
  const candidates = evaluate(read(dir + 'routeCandidateEntry.ts'), {
    '@/api/mes/pro/route': {
      ProRouteApi: {
        getRoute: async () => ({ id: 922022 }),
        getRouteVersionList: async () => [legacy],
        createRouteCandidateVersion: async (request) => {
          createdRequest = request
          return complete
        }
      }
    },
    '@/utils/routeQueryId': {
      parsePositiveRouteQueryId: (value) => (Number(value) > 0 ? String(value) : undefined)
    },
    './routeVersionHistory': history
  })
  const result = await candidates.ensureSameSourceDraftCandidateForProductionConfig({
    routeId: 922022,
    actionName: '新建完整版本',
    changeReason: '正常发布'
  })
  check(() => assert.equal(result.candidate, complete))
  check(() => assert.equal(createdRequest.sourceRouteVersionId, undefined))
  for (const activeRouteVersionId of [null, undefined]) {
    for (const sourceRouteVersionId of [null, undefined]) {
      // JSON round trip models explicit null and an omitted property on the actual wire.
      const routeInfo = JSON.parse(JSON.stringify({ id: 922022, activeRouteVersionId }))
      const draft = JSON.parse(JSON.stringify({ ...complete, sourceRouteVersionId }))
      check(() => assert.equal(
        candidates.resolveSameSourceDraftCandidateForProductionConfig(routeInfo, [legacy, draft], '编辑').id,
        complete.id
      ))
    }
  }
  check(() =>
    assert.throws(
      () => candidates.resolveSameSourceDraftCandidateForProductionConfig({ id: 10 }, [], '编辑'),
      /缺少激活版本/
    )
  )
  check(() =>
    assert.equal(
      candidates.resolveSameSourceDraftCandidateForProductionConfig(
        { id: 922022, activeRouteVersionId: 7 },
        [{ ...complete, sourceRouteVersionId: 7 }],
        '编辑'
      ).id,
      complete.id
    )
  )
  check(() =>
    assert.throws(
      () =>
        candidates.resolveSameSourceDraftCandidateForProductionConfig(
          { id: 922022, activeRouteVersionId: 7 },
          [{ ...complete, sourceRouteVersionId: 8 }],
          '编辑'
        ),
      /来源已不是当前/
    )
  )

  // Exercise the real page loader with a forged DRAFT query. The server's legacy state wins;
  // it must return before opening the current graph/products/DCC form.
  const page = sfc.parse(read(dir + 'RouteEditPage.vue')).descriptor
  const route = vue.reactive({
    name: 'MesProRouteEdit',
    params: { id: '922022' },
    query: {
      routeVersionId: '5',
      routeVersionNo: 'V1',
      routeVersionStatus: 'DRAFT',
      routeDraftOrigin: 'list-edit',
      discardOnUnsavedExit: '1'
    }
  })
  let returnedVersion = legacy
  let opens = 0
  const pageModule = evaluate(
    page.scriptSetup.content.replace(/defineOptions\([^\n]+\)/, '') +
      '\nexport { loadRoute, contentRef, legacyRouteVersion, routeVersionEditContext, routeEditBlockingError, shouldPromptUnsavedCandidateDraftBeforeExit };',
    {
      'element-plus': { ElMessageBox: {} },
      '@/api/mes/pro/route': { ProRouteApi: { getRouteVersion: async () => returnedVersion } },
      './RouteFormContent.vue': {},
      './LegacyRouteVersionSummary.vue': {},
      './routeVersionHistory': history,
      './routeCandidateEntry': candidates,
      './routeError': { resolveRouteOperationErrorMessage: (error) => error.message },
      '@/store/modules/tagsView': { useTagsViewStore: () => ({}) },
      '@/utils/routeQueryId': {
        parsePositiveRouteQueryId: (value) => (Number(value) > 0 ? String(value) : undefined)
      }
    },
    {
      ref: vue.ref,
      computed: vue.computed,
      nextTick: vue.nextTick,
      useRoute: () => route,
      useRouter: () => ({}),
      useMessage: () => ({ error() {} }),
      onBeforeRouteLeave() {},
      onMounted() {},
      onActivated() {},
      watch() {}
    }
  )
  pageModule.contentRef.value = {
    open: async () => {
      opens++
    }
  }
  await pageModule.loadRoute()
  check(() => assert.equal(opens, 0))
  check(() => assert.equal(pageModule.legacyRouteVersion.value.id, legacy.id))
  check(() =>
    assert.equal(pageModule.routeVersionEditContext.value.lifecycleStatus, 'LEGACY_INCOMPLETE')
  )
  check(() => assert.equal(pageModule.shouldPromptUnsavedCandidateDraftBeforeExit.value, false))
  route.query.routeVersionId = '6'
  route.query.routeVersionNo = 'V2'
  returnedVersion = complete
  await pageModule.loadRoute()
  check(() => assert.equal(opens, 1))
  check(() => assert.equal(pageModule.legacyRouteVersion.value, undefined))
  check(() => assert.equal(pageModule.routeVersionEditContext.value.lifecycleStatus, 'DRAFT'))
  route.query.routeVersionId = '7'
  returnedVersion = { ...legacy, id: 7, routeId: 999 }
  await pageModule.loadRoute()
  check(() => assert.match(pageModule.routeEditBlockingError.value, /不属于当前路线/))
  check(() => assert.equal(opens, 1))

  for (const file of ['index.vue', 'RouteEditPage.vue', 'LegacyRouteVersionSummary.vue']) {
    const parsedFile = sfc.parse(read(dir + file), { filename: file })
    assert.deepEqual(parsedFile.errors, [])
    const template = sfc.compileTemplate({
      source: parsedFile.descriptor.template.content,
      filename: file,
      id: file
    })
    assert.deepEqual(template.errors, [])
    const script = sfc.compileScript(parsedFile.descriptor, { id: file })
    const transpiled = ts.transpileModule(script.content, {
      reportDiagnostics: true,
      compilerOptions: { module: ts.ModuleKind.ESNext, target: ts.ScriptTarget.ES2020 }
    })
    assert.deepEqual(transpiled.diagnostics, [])
    count++
  }
  console.log(
    `PASS ${count} legacy summary component/loader/candidate checks (local, no server access)`
  )
}
main().catch((error) => {
  console.error(error)
  process.exitCode = 1
})
