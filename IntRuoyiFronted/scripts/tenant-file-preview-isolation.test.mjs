import assert from 'node:assert/strict'
import fs from 'node:fs'
import vm from 'node:vm'
import test from 'node:test'
import ts from 'typescript'
import { parse } from 'vue/compiler-sfc'
import { ref, reactive, computed, watch, unref } from 'vue'

const page = new URL('../src/views/infra/file/index.vue', import.meta.url)
const compile = (source) => ts.transpileModule(source, {
  compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
}).outputText

function loadPage(api, message = {}) {
  const revision = ref(0)
  const urls = []
  const revoked = []
  const stops = []
  const { descriptor, errors } = parse(fs.readFileSync(page, 'utf8'))
  assert.deepEqual(errors, [])
  const context = vm.createContext({
    exports: {}, Blob, ref, reactive, computed, unref,
    watch: (...args) => { const stop = watch(...args); stops.push(stop); return stop },
    defineOptions: () => {}, useMessage: () => message, useI18n: () => ({ t: (key) => key }),
    onMounted: () => {}, onBeforeUnmount: (fn) => stops.push(fn),
    URL: { createObjectURL: () => { const url = `blob:test-${urls.length}`; urls.push(url); return url },
      revokeObjectURL: (url) => revoked.push(url) },
    require: (name) => {
      if (name === '@/api/infra/file') return api
      if (name === '@/utils/auth') return { authContextRevision: revision }
      return {}
    }
  })
  vm.runInContext(compile(descriptor.scriptSetup.content + `
globalThis.exposed = { openFile, getList, previewUrl, previewVisible, list, openingFileId, handleDelete, handleDeleteBatch, checkedIds };`), context)
  return { ...context.exposed, revision, urls, revoked, close: () => stops.forEach((stop) => stop()) }
}

test('switching tenant removes displayed content and ignores in-flight previews', async () => {
  let resolve
  const api = { readFileContent: () => Promise.resolve(new Blob(['A'])) }
  const state = loadPage(api)
  try {
    const row = { id: 1, configId: 10, path: 'tenants/11/a.png', name: 'A', type: 'image/png' }
    await state.openFile(row)
    assert.equal(state.previewVisible.value, true)
    api.readFileContent = () => new Promise((done) => { resolve = done })
    const pending = state.openFile(row)
    state.revision.value += 1
    assert.equal(state.previewVisible.value, false)
    assert.equal(state.previewUrl.value, '')
    assert.equal(state.revoked.length, 1)
    resolve(new Blob(['old tenant']))
    await pending
    assert.equal(state.urls.length, 1)
    assert.equal(state.openingFileId.value, undefined)
  } finally { state.close() }
})

test('late list responses from an old session cannot repopulate files', async () => {
  let resolve
  const state = loadPage({ getFilePage: () => new Promise((done) => { resolve = done }) })
  try {
    const pending = state.getList()
    state.revision.value += 1
    resolve({ list: [{ id: 1, name: 'other tenant' }], total: 1 })
    await pending
    assert.equal(state.list.value.length, 0)
  } finally { state.close() }
})

test('file download addresses use authenticated API paths, never stored external URLs', async () => {
  let options
  const context = vm.createContext({ exports: {}, encodeURIComponent,
    require: () => ({ default: { download: (value) => { options = value; return Promise.resolve(new Blob(['data'])) } } }) })
  vm.runInContext(compile(fs.readFileSync(new URL('../src/api/infra/file/index.ts', import.meta.url), 'utf8')), context)
  await context.exports.readFileContent({ configId: 10, path: 'tenants/11/报告 #1.pdf', url: 'https://outside.invalid' })
  assert.equal(options.url, '/infra/file/10/get/tenants/11/%E6%8A%A5%E5%91%8A%20%231.pdf')
})

for (const batch of [false, true]) {
  test(`tenant switch while confirming ${batch ? 'batch' : 'single'} deletion rejects the old action`, async () => {
    let confirm
    let deletes = 0
    const state = loadPage({ deleteFile: () => { deletes++ }, deleteFileList: () => { deletes++ } },
      { delConfirm: () => new Promise((resolve) => { confirm = resolve }) })
    try {
      state.checkedIds.value = [1]
      const pending = batch ? state.handleDeleteBatch() : state.handleDelete({ id: 1, path: 'tenants/11/a.pdf' })
      state.revision.value++
      confirm()
      await assert.rejects(pending, /租户已切换/)
      assert.equal(deletes, 0)
    } finally { state.close() }
  })
}

test('batch deletion uses the selection that was confirmed and does not hide storage failures', async () => {
  let confirm
  let requestedIds
  const state = loadPage({ deleteFileList: async (ids, guard) => {
    requestedIds = ids
    assert.equal(guard(), true)
    throw new Error('storage failure')
  } }, { delConfirm: () => new Promise((resolve) => { confirm = resolve }) })
  try {
    state.checkedIds.value = [1]
    const pending = state.handleDeleteBatch()
    state.checkedIds.value = [2]
    confirm()
    await assert.rejects(pending, /storage failure/)
    assert.deepEqual(Array.from(requestedIds), [1])
  } finally { state.close() }
})
