const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')

const readSource = (relativePath) => {
  const absolutePath = path.join(repoRoot, relativePath)
  assert.equal(fs.existsSync(absolutePath), true, `missing required file: ${relativePath}`)
  return fs.readFileSync(absolutePath, 'utf8')
}

const source = readSource('src/views/bpm/model/index.vue')

assert.match(
  source,
  /const\s+getList\s*=\s*async\s*\(\)\s*=>/,
  'BPM model page must keep a single getList data loader'
)
assert.match(
  source,
  /ModelApi\.getModelList\(queryParams\.name\)/,
  'BPM model page loader must request model list data'
)
assert.match(
  source,
  /CategoryApi\.getCategorySimpleList\(\)/,
  'BPM model page loader must request category data'
)
assert.match(
  source,
  /onMounted\s*\(\s*\(\)\s*=>\s*\{[\s\S]*?getList\(\)[\s\S]*?\}\s*\)/,
  'BPM model page must load data on initial mount; onActivated alone does not run for noCache routes'
)

console.log('PASS: BPM model initial load static contract')
