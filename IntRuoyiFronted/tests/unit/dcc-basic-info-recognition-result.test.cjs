const test = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')

const source = fs.readFileSync(path.join(__dirname, '../../src/views/dcc/controlled-file/detail/index.vue'), 'utf8')
const handler = source.slice(source.indexOf('const handleRecognizeProjectCode ='), source.indexOf('const openDccProjectCode ='))

for (const [status, expected] of [
  ['UNKNOWN_DCC', '识别完成，未知基础数据，请人工确认'],
  ['NAME_MISMATCH', '识别完成，未识别项目名称，请人工确认'],
  ['SUCCESS', '已识别基础信息：项目甲 / P001']
]) {
  test(`recognition result ${status} reports its actual outcome`, async () => {
    const messages = []
    let reloads = 0
    const context = {
      fileDetail: { value: { id: '2054545668044084616' } },
      projectCodeRecognitionLoading: { value: false },
      recognizeControlledFileProjectCode: async () => ({ recognitionStatus: status, projectName: '项目甲', projectCode: 'P001' }),
      message: { success: text => messages.push(text), error: text => assert.fail(text) },
      reloadAll: async () => { reloads++ }
    }
    vm.createContext(context)
    await vm.runInContext(`${handler}\nhandleRecognizeProjectCode()`, context)
    assert.deepEqual(messages, [expected])
    assert.equal(reloads, 1)
    assert.equal(context.projectCodeRecognitionLoading.value, false)
  })
}
