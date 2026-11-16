import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { createRequire } from 'node:module'
import { parse } from 'vue/compiler-sfc'
import * as vue from 'vue'

const require = createRequire(import.meta.url)
const ts = require('typescript')
const source = readFileSync(new URL('../src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue', import.meta.url), 'utf8')
const script = parse(source).descriptor.scriptSetup.content
const ast = ts.createSourceFile('panel.ts', script, ts.ScriptTarget.Latest, true)
const withoutImports = ast.statements.filter(statement => !ts.isImportDeclaration(statement))
  .map(statement => statement.getText(ast)).join('\n')
const code = ts.transpileModule(withoutImports, { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText

for (const mode of ['production', 'pqc']) {
  test(`${mode} setup and immediate watchers initialize without errors`, async () => {
    const errors = [], scope = vue.effectScope()
    const deps = {
      ...vue,
      defineProps: () => ({ mode }), withDefaults: (props, defaults) => ({ ...defaults, ...props }),
      onMounted() {}, onUnmounted() {}, onBeforeUnmount() {}, onDeactivated() {},
      watch: (source, callback, options) => vue.watch(source, (...args) => {
        try { return Promise.resolve(callback(...args)).catch(error => errors.push(error)) }
        catch (error) { errors.push(error) }
      }, options),
      useRoute: () => ({ query: {}, fullPath: '/mes/frontline/' + mode }),
      useRouter: () => ({}), useMessage: () => ({}),
      useUserStore: () => ({ getUser: { id: 1 } }),
      useWorkflowInteractionLog: () => ({ saveError: vue.ref(''), saving: vue.ref(false) }),
      useApprovalCenterInvalidationStore: () => ({ syncSession: () => ({ key: 'test-session' }) }),
      createFormalActionGuard: () => ({ invalidate() {} }),
      createFormalWriteReceiptState: () => ({ has: () => false, isWriting: () => false }),
      FRONTLINE_TEMPLATE_CODES: { PRODUCTION_SIMPLIFIED: 'production', PQC_SIMPLIFIED: 'pqc' },
      FRONTLINE_FIELD_CODES: {}, FRONTLINE_PQC_RESULTS: {},
      createFrontlineDefaultValues: () => ({}),
      createFrontlineDeviceEmployeeState: () => ({
        activeOrderOptions: [], processOptions: [], productionProcessOptions: [], employeeOptions: [],
        loadingActiveOrders: false, loadingProcesses: false, loadingEmployees: false, loadingTemplate: false,
        productionRuntimeCache: { runtimeConfigByProcessKey: {}, employeeSwitchByKey: {} },
        pqcProcessOptionsCache: new Map(), pqcProcessOptionsRequests: new Map()
      }),
      resolveFrontlineContextKey: () => '',
      document: { visibilityState: 'visible' }, window: { innerWidth: 1920, innerHeight: 1080 },
      getPqcReviewLabel: () => '未选择检验任务', getPqcReviewCounts: () => ({ pending: 0, confirmed: 0, returned: 0 })
    }
    try {
      scope.run(() => new Function('deps', `with (deps) { ${code} }`)(deps))
      await vue.nextTick()
      await Promise.resolve()
      assert.deepEqual(errors, [])
    } finally { scope.stop() }
  })
}
