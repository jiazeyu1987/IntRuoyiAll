const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')

const readSource = (relativePath) => {
  const absolutePath = path.join(repoRoot, relativePath)
  assert.equal(fs.existsSync(absolutePath), true, `missing required file: ${relativePath}`)
  return fs.readFileSync(absolutePath, 'utf8')
}

const viteConfig = readSource('vite.config.ts')
const biPage = readSource('src/views/report/jmreport/bi.vue')

assert.match(
  biPage,
  /\/drag\/list\?token=/,
  'JimuBI page must still load the vendor BI designer route'
)

assert.match(
  viteConfig,
  /\['\/drag'\]: \{[\s\S]*target: proxyTarget/,
  'local same-origin proxy must forward /drag to the backend; otherwise JimuBI iframe renders the admin SPA 404 page'
)

console.log('PASS: JimuBI local proxy contract')
