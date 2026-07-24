const assert = require('node:assert/strict')
const path = require('node:path')

const root = path.resolve(__dirname, '..', '..')
const viteConfigPath = path.join(root, 'vite.config.ts')

async function resolveServeConfig() {
  const { resolveConfig } = await import('vite')
  const originalArgv = process.argv
  process.argv = ['node', 'vite', '--mode', 'env.local', '--host', '127.0.0.1', '--port', '8081']
  try {
    return await resolveConfig(
      {
        configFile: viteConfigPath,
        mode: 'env.local'
      },
      'serve',
      'development'
    )
  } finally {
    process.argv = originalArgv
  }
}

;(async () => {
  const config = await resolveServeConfig()
  const ignored = config.server.watch?.ignored

  assert.ok(Array.isArray(ignored), 'Vite dev server must declare watch.ignored as an array')

  const ignoredSamples = [
    path.join(root, 'node_modules.corrupt-20260710', 'package.json'),
    path.join(root, 'dist-intruoyi-test', 'pdfjs', 'pdf.worker.mjs.gz'),
    path.join(root, 'doc', 'tasks', 'task.md'),
    path.join(root, 'tests', 'output', 'result.json'),
    path.join(root, 'output', 'playwright', 'error.txt'),
    path.join(root, 'output-frontend-8081-start.err.log'),
    path.join(root, 'test-results', 'trace.zip'),
    path.join(root, '.playwright-cli', 'session.json'),
    path.join(root, '.git', 'index.lock'),
    path.join(root, '.tmp', 'vite-probe.json'),
    path.join(root, '.runtime', 'frontend-state.json'),
    path.join(root, 'runtime', 'restart-state.json'),
    path.join(root, 'node_modules', '.vite-env-local-8081', 'deps', 'chunk.js'),
    path.join(root, 'yudao-ui-admin-vue3', 'node_modules', 'vite', 'package.json'),
    path.join(root, 'showroom-vue-tsc-after-freeze-removal.log')
  ]

  for (const sample of ignoredSamples) {
    assert.ok(
      ignored.some((pattern) => pattern instanceof RegExp && pattern.test(sample)),
      `Vite dev server watch.ignored must exclude Windows path: ${sample}`
    )
  }

  const sourcePath = path.join(root, 'src', 'main.ts')
  assert.equal(
    ignored.some((pattern) => pattern instanceof RegExp && pattern.test(sourcePath)),
    false,
    'Vite dev server must continue watching source files'
  )

  console.log('PASS: Vite dev watcher excludes generated and diagnostic directories')
})().catch((error) => {
  console.error(error)
  process.exit(1)
})
