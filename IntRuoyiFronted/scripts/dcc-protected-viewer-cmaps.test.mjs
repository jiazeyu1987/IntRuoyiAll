import assert from 'node:assert/strict'
import fs from 'node:fs'
import test from 'node:test'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { createServer } from 'vite'

const root = fileURLToPath(new URL('../', import.meta.url))

test('Chinese PDF packed CMaps are served as binary assets under the deployment base', async () => {
  const server = await createServer({ root, configFile: false, base: '/controlled/', server: { host: '127.0.0.1', port: 0 } })
  await server.listen()
  try {
    const port = server.httpServer.address().port
    for (const name of ['UniGB-UCS2-H', 'Adobe-GB1-UCS2']) {
      const response = await fetch(`http://127.0.0.1:${port}/controlled/pdfjs/cmaps/${name}.bcmap`)
      assert.equal(response.status, 200)
      assert.deepEqual(Buffer.from(await response.arrayBuffer()), fs.readFileSync(path.join(root, `public/pdfjs/cmaps/${name}.bcmap`)))
    }
  } finally { await server.close() }
})
