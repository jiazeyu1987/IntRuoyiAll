import assert from 'node:assert/strict'
import fs from 'node:fs'
import fsPromises from 'node:fs/promises'
import path from 'node:path'
import { createRequire } from 'node:module'
import { fileURLToPath, pathToFileURL } from 'node:url'
import test from 'node:test'

const args = process.argv.slice(2)
const option = (name) => {
  const index = args.indexOf(name)
  assert.ok(index >= 0 && args[index + 1], `Required option: ${name}`)
  return path.resolve(args[index + 1])
}
const helper = option('--helper')
const frontendRoot = option('--frontend-root')
const { createReadFileLimiter, installWindowsReadFileLimit } = await import(pathToFileURL(helper))
const nativeReadFile = fsPromises.readFile
const taskDir = path.dirname(fileURLToPath(import.meta.url))
const fixtureDir = await fsPromises.mkdtemp(path.join(taskDir, '.vite-read-limit-r1-'))
const textFile = path.join(fixtureDir, 'payload.txt')
const payload = Buffer.from('真实文件读取\u0000\u0001\ufffd\n'.repeat(8192), 'utf8')
await fsPromises.writeFile(textFile, payload)

try {
  await test('invalid limits and missing readers fail before installation', () => {
    for (const limit of [0, -1, 1.5, NaN, Infinity, '8', null, Number.MAX_SAFE_INTEGER + 1]) {
      assert.throws(() => createReadFileLimiter(nativeReadFile, limit), RangeError)
      assert.throws(() => installWindowsReadFileLimit(limit), RangeError)
    }
    assert.throws(() => createReadFileLimiter(null, 1), TypeError)
    assert.equal(fsPromises.readFile, nativeReadFile)
  })

  await test('real native reads preserve Buffer, string, options identity and bounded concurrency', async () => {
    let active = 0
    let peak = 0
    let calls = 0
    const encodingOptions = { encoding: 'utf8' }
    const observedOptions = []
    const limited = createReadFileLimiter(async function (...readArgs) {
      assert.equal(this, fsPromises)
      observedOptions.push(readArgs[1])
      calls += 1
      active += 1
      peak = Math.max(peak, active)
      try {
        return await Reflect.apply(nativeReadFile, fsPromises, readArgs)
      } finally {
        active -= 1
      }
    }, 4)
    const results = await Promise.all(Array.from({ length: 80 }, (_, index) =>
      limited.call(fsPromises, textFile, index % 3 === 0 ? undefined : index % 3 === 1 ? 'utf8' : encodingOptions)
    ))
    results.forEach((result, index) => assert.deepEqual(result, index % 3 === 0 ? payload : payload.toString('utf8')))
    assert.equal(calls, 80)
    assert.equal(active, 0)
    assert.equal(peak, 4)
    assert.ok(observedOptions.some((value) => value === encodingOptions))
  })

  await test('FIFO and finally release keep original rejection and synchronous error objects', async () => {
    const permissionError = Object.assign(new Error('permission denied'), { code: 'EACCES' })
    const synchronousError = new Error('synchronous read failure')
    const receiver = { marker: 'receiver' }
    const started = []
    let release
    const gate = new Promise((resolve) => { release = resolve })
    const limited = createReadFileLimiter(function (name) {
      assert.equal(this, receiver)
      started.push(name)
      if (name === 'first') return gate.then(() => 'first bytes')
      if (name === 'denied') return Promise.reject(permissionError)
      if (name === 'throws') throw synchronousError
      return Promise.resolve('last bytes')
    }, 1)
    const first = limited.call(receiver, 'first')
    const denied = limited.call(receiver, 'denied')
    const throws = limited.call(receiver, 'throws')
    const last = limited.call(receiver, 'last')
    const deniedCheck = assert.rejects(denied, (error) => error === permissionError)
    const throwsCheck = assert.rejects(throws, (error) => error === synchronousError)
    assert.deepEqual(started, ['first'])
    release()
    assert.equal(await first, 'first bytes')
    await Promise.all([deniedCheck, throwsCheck])
    assert.equal(await last, 'last bytes')
    assert.deepEqual(started, ['first', 'denied', 'throws', 'last'])
  })

  await test('native missing-file, directory and AbortSignal errors survive unchanged and release capacity', async () => {
    const errors = []
    const limited = createReadFileLimiter(async (...readArgs) => {
      try {
        return await Reflect.apply(nativeReadFile, fsPromises, readArgs)
      } catch (error) {
        errors.push(error)
        throw error
      }
    }, 1)
    await assert.rejects(limited(path.join(fixtureDir, 'missing.txt')), (error) => error === errors.at(-1) && error.code === 'ENOENT')
    await assert.rejects(limited(fixtureDir), (error) => error === errors.at(-1) && error.code === 'EISDIR')
    const controller = new AbortController()
    controller.abort()
    await assert.rejects(limited(textFile, { signal: controller.signal }), (error) => error === errors.at(-1) && error.name === 'AbortError')
    assert.deepEqual(await limited(textFile), payload)
  })

  await test('idempotent installation bounds real Vite JS and ElementPlus CSS reads without opening a port', async () => {
    let active = 0
    let peak = 0
    const observedFiles = []
    fsPromises.readFile = async function (...readArgs) {
      active += 1
      peak = Math.max(peak, active)
      observedFiles.push(String(readArgs[0]))
      try {
        return await Reflect.apply(nativeReadFile, this, readArgs)
      } finally {
        active -= 1
      }
    }
    const first = installWindowsReadFileLimit(4)
    const installedRead = fsPromises.readFile
    assert.equal(installWindowsReadFileLimit(4), first)
    assert.equal(fsPromises.readFile, installedRead)
    assert.equal(fs.promises.readFile, installedRead)
    assert.throws(() => installWindowsReadFileLimit(8), /installation has changed/)
    const requireFromFrontend = createRequire(path.join(frontendRoot, 'package.json'))
    const cssFile = requireFromFrontend.resolve('element-plus/theme-chalk/el-popover.css')
    const viteEntry = path.join(frontendRoot, 'node_modules/vite/dist/node/index.js')
    const { createServer } = await import(pathToFileURL(viteEntry))
    const moduleUrls = Array.from({ length: 24 }, (_, index) => `/entry-${index}.js`)
    await Promise.all(moduleUrls.map((url, index) => fsPromises.writeFile(path.join(fixtureDir, url.slice(1)), `export const value = ${index}\n`)))
    const server = await createServer({
      configFile: false,
      envFile: false,
      root: fixtureDir,
      cacheDir: path.join(fixtureDir, 'cache'),
      publicDir: false,
      logLevel: 'silent',
      server: { middlewareMode: true, hmr: false, watch: null, preTransformRequests: false, fs: { allow: [fixtureDir, frontendRoot] } },
      optimizeDeps: { noDiscovery: true, include: [] }
    })
    try {
      const outputs = await Promise.all(moduleUrls.map((url) => server.transformRequest(url)))
      outputs.forEach((output, index) => assert.match(output.code, new RegExp(`value = ${index}(?:\\s|;)`)))
      const cssUrl = `/@fs/${cssFile.replaceAll('\\', '/')}`
      const cssResult = await server.transformRequest(cssUrl)
      assert.match(cssResult.code, /el-popover/)
      await assert.rejects(server.transformRequest('/absent-module.js'), (error) => error.code === 'ERR_LOAD_URL')
      assert.ok(observedFiles.some((file) => path.resolve(file) === path.resolve(cssFile)))
      assert.ok(observedFiles.filter((file) => /entry-\d+\.js$/.test(file)).length >= 24)
      assert.ok(peak <= 4, `Vite native promise reads reached ${peak}`)
      assert.ok(peak > 1, 'Actual parallel Vite/native reads were exercised')
      assert.equal(server.httpServer, null)
      console.log(JSON.stringify({ viteVersion: requireFromFrontend('vite/package.json').version, viteNativeReadPeak: peak, transformedJs: outputs.length, originalFailedCss: cssFile, portOpened: false }))
    } finally {
      await server.close()
    }
    assert.equal(active, 0)
  })
} finally {
  const resolvedFixture = path.resolve(fixtureDir)
  assert.ok(path.dirname(resolvedFixture) === path.resolve(taskDir) && path.basename(resolvedFixture).startsWith('.vite-read-limit-r1-'))
  await fsPromises.rm(resolvedFixture, { recursive: true, force: true })
}
