const assert = require('node:assert/strict')
const { readFileSync } = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const { test } = require('node:test')
const { ref, shallowRef, watch, nextTick } = require('vue')

const source = readFileSync(
  path.resolve(__dirname, '../../src/views/mes/pro/route/RouteEditPage.vue'),
  'utf8'
).replace(/\r\n/g, '\n')
const loadSource = source.slice(
  source.indexOf('const loadRoute = async'),
  source.indexOf('const submitRouteCandidateVersion =')
)
const watchStart = source.indexOf('watch(\n  contentRef,')
assert.notEqual(watchStart, -1, '表单实例重建必须触发初始化')
const watchSource = source.slice(watchStart, source.indexOf('\n</script>', watchStart))

const createPage = () => {
  const errors = []
  const context = vm.createContext({
    contentRef: shallowRef(),
    loadedRouteRequestKey: ref(),
    isCurrentRouteEditPage: ref(true),
    routeIdInvalidError: ref(''),
    routeLoadError: ref(''),
    routeId: ref(980214),
    initialTab: ref('flow'),
    buildRouteRequestKey: () => '980214:flow:active',
    nextTick,
    watch,
    resolveRouteOperationErrorMessage: (error) => error.message,
    message: { error: (message) => errors.push(message) }
  })
  vm.runInContext(
    `let loadedRouteContent; ${loadSource}; ${watchSource}; globalThis.loadRoute = loadRoute`,
    context
  )
  return { context, errors }
}

const settle = async () => {
  await nextTick()
  await new Promise(setImmediate)
}

test('同一表单与路线重复加载不清空已有状态', async () => {
  const { context } = createPage()
  let calls = 0
  context.contentRef.value = { open: async () => { calls += 1 } }
  await context.loadRoute()
  await context.loadRoute()
  assert.equal(calls, 1)
})

test('热更新更换表单实例后以当前路线和编辑模式重新初始化', async () => {
  const { context } = createPage()
  context.contentRef.value = { open: async () => {} }
  await context.loadRoute()
  context.contentRef.value = undefined
  await nextTick()
  const calls = []
  context.contentRef.value = { open: async (...args) => { calls.push(args) } }
  await settle()
  assert.deepEqual(calls, [['update', 980214, 'flow']])
})

test('首次请求未完成时重建表单仍须初始化新实例', async () => {
  const { context } = createPage()
  let finishOldLoad
  context.contentRef.value = {
    open: () => new Promise((resolve) => { finishOldLoad = resolve })
  }
  const loading = context.loadRoute()
  await settle()
  let calls = 0
  context.contentRef.value = { open: async () => { calls += 1 } }
  await nextTick()
  finishOldLoad()
  await loading
  assert.equal(calls, 1)
})

test('重建后加载失败保留明确错误且不循环重试', async () => {
  const { context, errors } = createPage()
  context.contentRef.value = { open: async () => {} }
  await context.loadRoute()
  let calls = 0
  context.contentRef.value = {
    open: async () => { calls += 1; throw new Error('路线加载失败') }
  }
  await settle()
  context.contentRef.value = undefined
  await settle()
  assert.equal(calls, 1)
  assert.equal(context.routeLoadError.value, '路线加载失败')
  assert.deepEqual(errors, ['路线加载失败'])
})
