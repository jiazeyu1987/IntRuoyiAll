const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const categoryPagePath = path.resolve(
  process.cwd(),
  'src/views/dcc/controlled-file/categories/index.vue'
)
const categoryFormPath = path.resolve(
  process.cwd(),
  'src/views/dcc/controlled-file/categories/components/CategoryForm.vue'
)

assert(fs.existsSync(categoryPagePath), 'DCC category page must exist.')
assert(fs.existsSync(categoryFormPath), 'DCC category form must exist.')

const pageSource = fs.readFileSync(categoryPagePath, 'utf8')
const formSource = fs.readFileSync(categoryFormPath, 'utf8')

const openFormMatch = pageSource.match(
  /const\s+openForm\s*=\s*async\s*\([^)]*\)\s*=>\s*\{([\s\S]*?)\n\}/
)
assert(openFormMatch, 'DCC category openForm must be async so it can reload latest directory tree.')

const openFormBody = openFormMatch[1]
assert(
  openFormBody.includes('await getDirectoryTree()'),
  'DCC category openForm must fetch latest directory tree before opening the binding dialog.'
)
assert(
  /directories\.value\s*=\s*latestDirectoryTree/.test(openFormBody),
  'DCC category openForm must replace cached directories with the latest directory tree.'
)
assert(
  /directories:\s*latestDirectoryTree/.test(openFormBody),
  'DCC category form must receive the freshly loaded directory tree, not a stale page cache.'
)
assert(
  !/catch\s*\([^)]*\)\s*\{[\s\S]*formRef\.value\.open/.test(openFormBody) &&
    !/catch\s*\{[\s\S]*formRef\.value\.open/.test(openFormBody),
  'DCC category openForm must not open the dialog with stale directories when getDirectoryTree fails.'
)
assert(
  formSource.includes(':data="directoryOptions"'),
  'DCC category form must render directoryOptions in the binding directory tree select.'
)

console.log('PASS: DCC category directory binding refresh static contract')
