import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import vm from 'node:vm'
import test from 'node:test'

const source = readFileSync(fileURLToPath(new URL('../src/views/dcc/controlled-file/directories/components/DirectoryForm.vue', import.meta.url)), 'utf8')
const body = source.match(/const submitForm = async \(\) => \{([\s\S]*?)\n\}/)?.[1]?.replace('pending.shift()!', 'pending.shift()')
assert.ok(body)

for (const parentId of [1, 2]) {
  test(`directory form refuses parent ${parentId} for A before API write`, async () => {
    let writes = 0
    const errors = []
    const context = {
      formRef: { value: { validate: async () => true } },
      formType: { value: 'update' },
      formData: { value: { id: 1, parentId } },
      directoryOptions: { value: [{ id: 1, parentId: null }, { id: 2, parentId: 1 }] },
      formLoading: { value: false }, dialogVisible: { value: true },
      message: { error: e => errors.push(e), success: () => {} },
      updateDirectory: async () => { writes++ }, t: x => x, emit: () => {}
    }
    await vm.runInNewContext(`(async () => {${body}})()`, context)
    assert.equal(writes, 0)
    assert.equal(context.dialogVisible.value, true)
    assert.equal(errors.length, 1)
  })
}
