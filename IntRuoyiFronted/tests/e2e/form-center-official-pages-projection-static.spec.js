const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '..', '..')
const read = (file) => fs.readFileSync(path.join(root, file), 'utf8')

const assertPageUsesProjection = (file, tokens) => {
  const source = read(file)
  assert.ok(
    source.includes('@/api/form-center/actionProjection'),
    `${file} must import the shared form-center action projection helper.`
  )
  assert.ok(
    source.includes('resolveControlledActionProjection'),
    `${file} must resolve official action state through shared backend projection.`
  )
  for (const token of tokens) {
    assert.ok(source.includes(token), `${file} must expose ${token}.`)
  }
}

assertPageUsesProjection('src/views/dcc/controlled-file/detail/index.vue', [
  'dccObsoleteActionProjection',
  'dccPublishActionProjection'
])

assertPageUsesProjection('src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue', [
  'edhrReleaseActionProjection',
  'edhrVoidActionProjection'
])

assertPageUsesProjection('src/views/mes/pro/scheduleorder/index.vue', [
  'replanProjectionState',
  'scheduleReplanActionProjection'
])

console.log('form-center official pages projection static contract passed')
