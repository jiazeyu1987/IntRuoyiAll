const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const test = require('node:test')
const vue = require('vue')
const compiler = require('vue/compiler-sfc')
const ts = require('typescript')
const filename = path.resolve(
  __dirname,
  '../../src/views/mes/pro/route/components/RouteDeviceGroups.vue'
)
const { descriptor } = compiler.parse(fs.readFileSync(filename, 'utf8'), { filename })
const script = compiler.compileScript(descriptor, { id: 'device-groups-test' })
const template = compiler.compileTemplate({
  source: descriptor.template.content,
  filename,
  id: 'device-groups-test',
  compilerOptions: { bindingMetadata: script.bindings }
})
assert.deepEqual(template.errors, [])
const flush = async () => {
  await new Promise(setImmediate)
  await vue.nextTick()
}
function setup(overrides = {}) {
  const config = {
    routeProcessId: 10,
    processId: 20,
    overagePercent: 0,
    lossReasons: [{ id: 7 }],
    deviceSelectionGroups: [],
    parameterRules: []
  }
  const other = {
    routeProcessId: 11,
    processId: 21,
    deviceSelectionGroups: [{ deviceGroupKey: '原设备组', selectionMode: 'SINGLE', deviceIds: [9] }]
  }
  const props = vue.reactive({
    routeProcessId: 10,
    routeVersionId: 2002,
    disabled: false,
    snapshot: {
      routeVersionId: 2002,
      routeSnapshotSha256: 'hash',
      schemaVersion: 1,
      productionProcessConfigs: [config, other]
    }
  })
  const writes = [],
    emitted = []
  const api = {
    getRouteDeviceOptions: async () => [
      { deviceId: 501, deviceCode: 'D501', deviceName: '模拟设备' }
    ],
    saveRouteProductionProcessConfig: async (body) => {
      writes.push(JSON.parse(JSON.stringify(body)))
    },
    getRouteProductionProcessConfig: async () => ({
      ...props.snapshot,
      routeSnapshotSha256: 'new-hash',
      productionProcessConfigs: writes.at(-1).productionProcessConfigs
    }),
    ...overrides
  }
  const module = { exports: {} }
  vm.runInNewContext(
    ts.transpileModule(script.content, {
      compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
    }).outputText,
    {
      ...vue,
      module,
      exports: module.exports,
      require: (id) => (id === 'vue' ? vue : { ProRouteApi: api, ProRouteFlowConfigApi: api }),
      useMessage: () => ({ success() {} })
    }
  )
  const scope = vue.effectScope()
  const state = vue.proxyRefs(
    scope.run(() =>
      module.exports.default.setup(props, { expose() {}, emit: (...args) => emitted.push(args) })
    )
  )
  return { state, props, writes, emitted, stop: () => scope.stop() }
}
test('空设备组可首次选择设备并保存，保留其他工序与损耗配置及乐观锁', async () => {
  const t = setup()
  await flush()
  assert.equal(t.state.options[0].deviceId, 501)
  t.state.add()
  t.state.groups[0].deviceIds = [501]
  await t.state.save()
  assert.equal(t.writes.length, 1)
  assert.equal(t.writes[0].expectedRouteSnapshotSha256, 'hash')
  assert.deepEqual(t.writes[0].productionProcessConfigs[0].deviceSelectionGroups[0].deviceIds, [
    501
  ])
  assert.deepEqual(t.writes[0].productionProcessConfigs[0].lossReasons, [{ id: 7 }])
  assert.deepEqual(
    t.writes[0].productionProcessConfigs[1],
    JSON.parse(JSON.stringify(t.props.snapshot.productionProcessConfigs[1]))
  )
  assert.equal(t.emitted[0][1].routeSnapshotSha256, 'new-hash')
  t.stop()
})
test('删除参数引用设备时保留关联校验，禁止写入', async () => {
  const t = setup()
  await flush()
  t.props.snapshot.productionProcessConfigs[0].parameterRules = [
    { deviceId: 501, parameterCode: 'SIM', parameterName: '模拟参数' }
  ]
  await t.state.save()
  assert.match(t.state.error, /SIM.*引用的设备未加入本工序设备组/)
  assert.equal(t.writes.length, 0)
  t.stop()
})
test('加载设备失败明确报错且禁止保存', async () => {
  const t = setup({
    getRouteDeviceOptions: async () => {
      throw new Error('无设备查询权限')
    }
  })
  await flush()
  assert.match(t.state.error, /无设备查询权限/)
  t.state.add()
  t.state.groups[0].deviceIds = [501]
  await t.state.save()
  assert.equal(t.writes.length, 0)
  t.stop()
})
test('并发版本冲突不报告保存成功、不覆盖草稿', async () => {
  const t = setup({
    saveRouteProductionProcessConfig: async () => {
      throw new Error('版本已变化')
    }
  })
  await flush()
  t.state.add()
  t.state.groups[0].deviceIds = [501]
  await t.state.save()
  assert.match(t.state.error, /版本已变化/)
  assert.equal(t.emitted.length, 0)
  assert.deepEqual(Array.from(t.state.groups[0].deviceIds), [501])
  t.stop()
})
test('只读状态禁止保存；解除锁定后加载可选设备', async () => {
  const t = setup()
  await flush()
  t.props.disabled = true
  await vue.nextTick()
  t.state.add()
  t.state.groups[0].deviceIds = [501]
  await t.state.save()
  assert.equal(t.writes.length, 0)
  t.props.disabled = false
  await flush()
  assert.equal(t.state.loaded, true)
  t.stop()
})
test('工艺删除最后一项参数预览给出具体提示并阻止请求', async () => {
  const source = fs.readFileSync(
    path.resolve(__dirname, '../../src/views/mes/pro/craft/index.vue'),
    'utf8'
  )
  const fn = source.slice(
    source.indexOf('async function saveOrPreview()'),
    source.indexOf('async function confirmAll()')
  )
  const context = {
    form: { name: '模拟测试', processId: 20, reason: '删除参数', parameters: [], id: 47 },
    editorError: { value: '' },
    saving: { value: false },
    CraftApi: {
      preview() {
        throw new Error('不应请求预览')
      }
    }
  }
  vm.createContext(context)
  vm.runInContext(fn, context)
  await context.saveOrPreview()
  assert.equal(context.editorError.value, '工艺至少保留一项参数')
  assert.equal(context.saving.value, false)
})
