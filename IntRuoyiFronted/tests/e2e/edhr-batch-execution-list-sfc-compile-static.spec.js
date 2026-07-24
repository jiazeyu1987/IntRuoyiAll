const assert = require('node:assert/strict')
const fs = require('node:fs')
const { createRequire } = require('node:module')
const path = require('node:path')

const viteVueRequire = createRequire(require.resolve('@vitejs/plugin-vue'))
const { compileScript, parse } = viteVueRequire('@vue/compiler-sfc')

const root = path.resolve(__dirname, '..', '..')
const pageFiles = [
  'src/views/mes/pro/edhr-batch/BatchExecutionListPage.vue',
  'src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue'
]

for (const file of pageFiles) {
  const pagePath = path.join(root, file)

  assert.ok(fs.existsSync(pagePath), `${file} must exist.`)

  const source = fs.readFileSync(pagePath, 'utf8')
  const parsed = parse(source, { filename: pagePath })

  assert.equal(parsed.errors.length, 0, `${file} must parse cleanly.`)
  compileScript(parsed.descriptor, { id: file.replace(/[^a-z0-9]/gi, '-') })
}

console.log('edhr batch execution SFC compile static contract passed')
