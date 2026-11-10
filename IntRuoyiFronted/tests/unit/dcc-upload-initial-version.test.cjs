const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')

const source = fs.readFileSync(path.join(__dirname, '../../src/views/dcc/controlled-file/upload/index.vue'), 'utf8')
const pattern = source.match(/^const WINDCHILL_VERSION_PATTERN = .+$/m)[0]
const validation = source.slice(source.indexOf('const isVersionNoFormatValid = computed'), source.indexOf('const versionFormatPreflightMessage'))
const state = {
  formData: { versionNo: '' },
  isExternalReview: { value: false },
  normalizePreflightVersionNo: value => String(value || '').trim().toUpperCase(),
  isVersionNoTextValid: value => /^[Vv]?\d+(?:\.\d+)*$/.test(value),
  computed: fn => ({ get value() { return fn() } })
}
vm.createContext(state)
vm.runInContext(`${pattern}\n${validation}\nglobalThis.valid = isVersionNoFormatValid`, state)

for (const letter of 'ABCDEFGHJKLMNPQRSTUVWXYZ') {
  for (let number = 1; number <= 9; number++) {
    state.formData.versionNo = `${letter}/${number}`
    assert.equal(state.valid.value, true, state.formData.versionNo)
  }
}
for (const value of ['a/3', ' B/9 ', 'AH/9']) {
  state.formData.versionNo = value
  assert.equal(state.valid.value, true, value)
}
for (const value of ['', 'A/0', 'A/10', 'A/01', 'I/1', 'o/9', 'AI/3', 'AO/2', 'A/2-1', 'V1.0']) {
  state.formData.versionNo = value
  assert.equal(state.valid.value, false, value)
}
state.isExternalReview.value = true
state.formData.versionNo = 'V1.0'
assert.equal(state.valid.value, true, 'external review keeps its numeric version format')
console.log('Upload initial version validation passed')
