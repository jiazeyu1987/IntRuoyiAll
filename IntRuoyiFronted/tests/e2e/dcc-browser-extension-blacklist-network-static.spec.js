const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')

const readSource = (relativePath) => {
  const absolutePath = path.join(repoRoot, relativePath)
  assert.equal(fs.existsSync(absolutePath), true, `missing required file: ${relativePath}`)
  return fs.readFileSync(absolutePath, 'utf8')
}

const packageJson = JSON.parse(readSource('package.json'))
const viteConfig = readSource('vite.config.ts')
const envLocal = readSource('.env.local')

assert.equal(
  packageJson.scripts['e2e:dcc:browser-extension-blacklist-network:static'],
  'node tests/e2e/dcc-browser-extension-blacklist-network-static.spec.js',
  'package.json must expose the DCC browser extension blacklist network regression contract'
)

assert.match(
  envLocal,
  /VITE_PROXY_TARGET=http:\/\/127\.0\.0\.1:48081/,
  'local dev must declare a backend proxy target for same-origin admin-api requests'
)

assert.match(
  viteConfig,
  /const useSameOriginApiProxy = !isBuild && !isBatchRecordPreviewMode && !!env\.VITE_PROXY_TARGET/,
  'local non-preview dev mode must detect VITE_PROXY_TARGET as same-origin API proxy mode'
)

assert.match(
  viteConfig,
  /const runtimeBaseUrl = isBatchRecordPreviewMode[\s\S]*\? `http:\/\/127\.0\.0\.1:\$\{env\.VITE_PORT\}`[\s\S]*: useSameOriginApiProxy[\s\S]*\? ''[\s\S]*: env\.VITE_BASE_URL/,
  'local proxy mode must compile VITE_BASE_URL to an empty same-origin base URL'
)

assert.match(
  viteConfig,
  /\['\/admin-api'\]: \{[\s\S]*target: proxyTarget/,
  'admin-api proxy must use the resolved proxy target, not the runtime base URL'
)

assert.doesNotMatch(
  viteConfig,
  /target:\s*isBatchRecordPreviewMode\s*\?\s*proxyTarget\s*:\s*env\.VITE_BASE_URL/,
  'admin-api proxy must not bypass VITE_PROXY_TARGET in regular local mode'
)

console.log('PASS: DCC browser extension blacklist network contract')
