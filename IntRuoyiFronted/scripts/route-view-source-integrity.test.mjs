import assert from 'node:assert/strict'
import { existsSync, readFileSync } from 'node:fs'
import { execFileSync } from 'node:child_process'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import test from 'node:test'

const root = fileURLToPath(new URL('../', import.meta.url))

test('static route Vue imports exist in the checkout', () => {
  const source = readFileSync(path.join(root, 'src/router/modules/remaining.ts'), 'utf8')
  const imports = [...source.matchAll(/import\(['"]@\/([^'"]+\.vue)['"]\)/g)]
  assert.ok(imports.length > 0)
  const missing = imports.map((match) => match[1]).filter((view) => !existsSync(path.join(root, 'src', view)))
  assert.deepEqual(missing, [])
})

test('logs view source is included while runtime logs remain ignored', () => {
  const paths = ['src/views/dcc/controlled-file/logs/index.vue', 'logs/runtime.log']
  const ignored = execFileSync('git', ['check-ignore', '--no-index', '--stdin'], {
    cwd: root,
    input: paths.join('\n') + '\n',
    encoding: 'utf8'
  }).trim().split(/\r?\n/)
  assert.deepEqual(ignored, ['logs/runtime.log'])
})
