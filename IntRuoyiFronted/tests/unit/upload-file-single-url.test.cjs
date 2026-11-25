const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const { ref, reactive, computed, watch, nextTick } = require('vue')

const source = fs.readFileSync(path.resolve(__dirname,
  '../../src/components/UploadFile/src/UploadFile.vue'), 'utf8').replace(/\r\n/g, '\n')
const watchStart = source.indexOf('watch(', source.indexOf('// 监听模型绑定值变动'))
const modelWatch = source.slice(watchStart, source.indexOf('watch(', watchStart + 6))
const busyState = source.slice(source.indexOf('const isUploading ='), source.indexOf('const uploadNumber ='))
const remove = source.slice(source.indexOf('const handleRemove ='), source.indexOf('const handlePreview:'))
const emitModel = source.slice(source.indexOf('const emitFileNames ='), source.indexOf('defineExpose('))
const compiled = ts.transpileModule(busyState + modelWatch + remove + emitModel, {
  compilerOptions: { target: ts.ScriptTarget.ES2022 }
}).outputText

function setup(modelValue, limit) {
  const events = []
  const state = {
    props: reactive({ modelValue, limit }), fileList: ref([]), fileNameByUrl: new Map(),
    computed, watch, isString: value => typeof value === 'string',
    syncProvidedFileNames() {}, resolveModelFileName: url => url,
    emit: (...args) => events.push(args)
  }
  vm.createContext(state)
  vm.runInContext(compiled + '\nthis.busy = isUploading; this.remove = handleRemove;', state)
  return { ...state, events }
}

test('single SOP URL keeps commas in paths and query parameters and clears as one file', async () => {
  for (const url of ['https://files.example/a,b.pdf', 'https://files.example/sop.pdf?parts=1,2']) {
    const state = setup(url, 1)
    assert.equal(state.fileList.value.length, 1)
    assert.equal(state.fileList.value[0].url, url)
    state.remove(state.fileList.value[0])
    assert.deepEqual(state.events.find(([event]) => event === 'update:modelValue'), ['update:modelValue', ''])
    state.props.modelValue = ''
    await nextTick()
    assert.equal(state.fileList.value.length, 0)
  }
})

test('multiple-file strings and arrays retain their existing parsing', () => {
  const first = 'https://files.example/one.pdf', second = 'https://files.example/two.pdf'
  for (const input of [`${first},${second}`, [first, second]]) {
    const state = setup(input, 5)
    assert.deepEqual(Array.from(state.fileList.value, file => file.url), [first, second])
  }
})

test('upload readiness and progress block saving, completed or failed removal releases it', () => {
  const state = setup('', 1)
  assert.equal(state.busy.value, false)
  state.fileList.value.push({ name: 'sop.pdf', status: 'ready' })
  assert.equal(state.busy.value, true)
  state.fileList.value[0].status = 'uploading'
  assert.equal(state.busy.value, true)
  state.fileList.value[0].status = 'success'
  assert.equal(state.busy.value, false)
  state.fileList.value[0].status = 'uploading'
  state.fileList.value = []
  assert.equal(state.busy.value, false)
})
