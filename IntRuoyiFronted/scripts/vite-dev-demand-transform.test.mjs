import assert from 'node:assert/strict'
import { mkdtemp, writeFile, rm } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { join, resolve } from 'node:path'
import test from 'node:test'
import { createServer, resolveConfig } from 'vite'

test('Windows-safe dev transforms requested modules without recursively warming the import graph', async () => {
  const previousArgv = process.argv
  const previousProfile = process.env.VITE_OPTIMIZE_PROFILE
  let server
  let fixtureRoot
  try {
    process.argv = ['node', 'vite', '--mode', 'env.local']
    process.env.VITE_OPTIMIZE_PROFILE = 'windows-safe'
    const config = await resolveConfig({ configFile: resolve('vite.config.ts'), mode: 'env.local' }, 'serve')
    fixtureRoot = await mkdtemp(join(tmpdir(), 'intruoyi-demand-transform-'))
    const imports = Array.from({ length: 128 }, (_, index) => `import './module-${index}.js'`).join('\n')
    await writeFile(join(fixtureRoot, 'entry.js'), imports)
    for (let index = 0; index < 128; index += 1) {
      await writeFile(join(fixtureRoot, `module-${index}.js`), `export const value = ${index}`)
    }
    server = await createServer({
      configFile: false,
      root: fixtureRoot,
      logLevel: 'silent',
      server: { middlewareMode: true, hmr: false, preTransformRequests: config.server.preTransformRequests },
      optimizeDeps: { noDiscovery: true, include: [] }
    })
    const speculativeRequests = []
    server.warmupRequest = async (url) => { speculativeRequests.push(url) }
    const entry = await server.transformRequest('/entry.js')
    assert.match(entry.code, /module-127/)
    assert.equal(speculativeRequests.length, 0,
      'one entry request must not fan out into unbounded speculative filesystem reads')
    const dependency = await server.transformRequest('/module-127.js')
    assert.match(dependency.code, /value = 127/,
      'dependencies must still transform when actually requested')
    await assert.rejects(server.transformRequest('/missing.js'), /Failed to load url/,
      'real missing-file errors must not be suppressed')
  } finally {
    if (server) await server.close()
    if (fixtureRoot) await rm(fixtureRoot, { recursive: true, force: true })
    process.argv = previousArgv
    if (previousProfile === undefined) delete process.env.VITE_OPTIMIZE_PROFILE
    else process.env.VITE_OPTIMIZE_PROFILE = previousProfile
  }
})
