const assert = require('node:assert/strict')
const path = require('node:path')

const root = path.resolve(__dirname, '..', '..')
const viteConfigPath = path.join(root, 'vite.config.ts')

async function resolveServeConfig(port) {
  const { resolveConfig } = await import('vite')
  const originalArgv = process.argv
  process.argv = ['node', 'vite', '--mode', 'env.local', '--host', '127.0.0.1', '--port', String(port)]
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
  const port8081Config = await resolveServeConfig(8081)
  const port19081Config = await resolveServeConfig(19081)

  assert.match(
    path.normalize(port8081Config.cacheDir),
    /node_modules[\\/]\.vite-env-local-8081$/,
    'Vite cacheDir must be isolated by mode and effective dev-server port'
  )
  assert.match(
    path.normalize(port19081Config.cacheDir),
    /node_modules[\\/]\.vite-env-local-19081$/,
    'A second dev-server port must not share the 8081 optimized deps cache'
  )
  assert.notEqual(
    path.normalize(port8081Config.cacheDir),
    path.normalize(port19081Config.cacheDir),
    'Concurrent Vite dev servers must use different optimized deps cache directories'
  )

  console.log('PASS: Vite dev optimized deps cache is isolated by mode and port')
})().catch((error) => {
  console.error(error)
  process.exit(1)
})
