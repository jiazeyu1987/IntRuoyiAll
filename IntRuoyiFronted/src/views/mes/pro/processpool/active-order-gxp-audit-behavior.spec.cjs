const assert = require('node:assert/strict')
const { test } = require('node:test')
const fs = require('node:fs')
const path = require('node:path')
const { parse, compileScript, compileTemplate } = require('vue/compiler-sfc')

test('shared active-order detail compiles without GxP UI or requests for every entry', () => {
  const filename = path.join(__dirname, 'components/ActiveOrderSubmissionDetailPanel.vue')
  const source = fs.readFileSync(filename, 'utf8')
  assert.doesNotMatch(source, /gxp/i)
  const { descriptor, errors } = parse(source, { filename })
  assert.deepEqual(errors, [])
  const script = compileScript(descriptor, { id: 'active-order-detail' })
  const template = compileTemplate({ source: descriptor.template.content, filename,
    id: 'active-order-detail', compilerOptions: { bindingMetadata: script.bindings } })
  assert.deepEqual(template.errors, [])
  assert.match(source, /title="电子签名详情"/)
})
