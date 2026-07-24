import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'

const root = process.cwd()

const readText = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

test('protected viewer points PDF.js worker to a static public asset', () => {
  const source = readText('src/views/dcc/controlled-file/view/index.vue')
  const publicWorkerPath = path.join(root, 'public/pdfjs/pdf.worker.mjs')

  assert.match(source, /pdfjs\/pdf\.worker\.mjs/)
  assert.doesNotMatch(source, /new URL\('\.\/vendor\/pdf\.worker\.mjs', import\.meta\.url\)/)
  assert.equal(fs.existsSync(publicWorkerPath), true)
})

test('public PDF.js worker asset is served without Vite HMR client injection', async () => {
  const publicWorkerSource = readText('public/pdfjs/pdf.worker.mjs')

  assert.match(publicWorkerSource, /pdfjsVersion/)
  assert.doesNotMatch(publicWorkerSource, /@vite\/client/)

  const { createServer } = await import('vite')
  const server = await createServer({
    root,
    configFile: false,
    server: {
      host: '127.0.0.1',
      port: 0,
      strictPort: false
    }
  })

  await server.listen()

  try {
    const address = server.httpServer?.address()
    const port = typeof address === 'object' && address ? address.port : null
    assert.notEqual(port, null)

    const response = await fetch(`http://127.0.0.1:${port}/pdfjs/pdf.worker.mjs`)
    assert.equal(response.status, 200)

    const responseText = await response.text()
    assert.match(responseText, /pdfjsVersion/)
    assert.doesNotMatch(responseText, /@vite\/client/)
  } finally {
    await server.close()
  }
})
