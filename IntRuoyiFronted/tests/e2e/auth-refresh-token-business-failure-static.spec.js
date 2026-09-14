const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '../..')

function readUtf8(relativePath) {
  const absolutePath = path.join(frontendRoot, relativePath)
  assert.ok(fs.existsSync(absolutePath), `missing required file: ${relativePath}`)
  return fs.readFileSync(absolutePath, 'utf8')
}

const packageJson = JSON.parse(readUtf8('package.json'))
const axiosService = readUtf8('src/config/axios/service.ts')

assert.equal(
  packageJson.scripts?.['e2e:auth:refresh-token-business-failure:static'],
  'node tests/e2e/auth-refresh-token-business-failure-static.spec.js',
  'package.json must expose the refresh-token business failure static gate'
)

assert.match(
  axiosService,
  /const\s+refreshTokenPayload\s*=\s*refreshTokenRes\.data/,
  'refresh-token response must be normalized before calling setToken'
)
assert.match(
  axiosService,
  /const\s+refreshCode\s*=\s*refreshTokenPayload\?\.code\s*\?\?\s*result_code/,
  'refresh-token business code must be read from the raw axios response'
)
assert.match(
  axiosService,
  /if\s*\(\s*refreshCode\s*!==\s*0\s*&&\s*refreshCode\s*!==\s*200\s*\)/,
  'refresh-token business failure must be handled before token persistence'
)
assert.match(
  axiosService,
  /throw\s+createApiError\(\s*refreshTokenPayload\?\.msg\s*\|\|\s*refreshTokenPayload\?\.message\s*\|\|\s*t\('sys\.api\.timeoutMessage'\)/,
  'refresh-token business failure must throw a structured API error for handleAuthorized'
)
assert.match(
  axiosService,
  /setToken\(\s*refreshTokenPayload\.data\s*\)/,
  'token persistence must use the validated payload data'
)
assert.match(
  axiosService,
  /if\s*\(\s*isRelogin\.show\s*\)\s*\{[\s\S]*?deleteUserCache\(\)[\s\S]*?removeToken\(\)[\s\S]*?window\.location\.href\s*=\s*['"]\/login\?redirect=['"]\s*\+\s*encodeURIComponent\(window\.location\.pathname\s*\+\s*window\.location\.search\)/,
  'route-guard refresh failures must clear stale tokens and redirect to login even when the relogin flag is already set'
)

const validationIndex = axiosService.indexOf('refreshCode !== 0 && refreshCode !== 200')
const persistenceIndex = axiosService.indexOf('setToken(refreshTokenPayload.data)')
assert.ok(
  validationIndex >= 0 && persistenceIndex > validationIndex,
  'business failure validation must run before setToken'
)

console.log('PASS: refresh-token business failures route to re-login before setToken')
