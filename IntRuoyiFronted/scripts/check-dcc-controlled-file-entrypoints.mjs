import { existsSync } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)
const repoRoot = path.resolve(__dirname, '..')

const requiredViewEntrypoints = [
  'src/views/dcc/controlled-file/directories/index.vue',
  'src/views/dcc/controlled-file/access-rules/index.vue',
  'src/views/dcc/controlled-file/categories/index.vue',
  'src/views/dcc/controlled-file/positions/index.vue',
  'src/views/dcc/controlled-file/routes/index.vue',
  'src/views/dcc/controlled-file/upload/index.vue',
  'src/views/dcc/controlled-file/browser/index.vue',
  'src/views/dcc/controlled-file/mine/index.vue',
  'src/views/dcc/controlled-file/approval-tasks/index.vue',
  'src/views/dcc/controlled-file/detail/index.vue'
]

const missing = requiredViewEntrypoints.filter((relativePath) => {
  return !existsSync(path.join(repoRoot, relativePath))
})

if (missing.length > 0) {
  console.error('Missing DCC controlled-file entrypoints:')
  for (const relativePath of missing) {
    console.error(`- ${relativePath}`)
  }
  process.exit(1)
}

console.log(`All ${requiredViewEntrypoints.length} DCC controlled-file entrypoints exist.`)
