import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

const repoRoot = resolve(import.meta.dirname, '..', '..')
const nginxConfigPath = resolve(repoRoot, 'script', 'deploy', 'int-ruoyi-test', 'nginx.conf')
const source = readFileSync(nginxConfigPath, 'utf8')

const extractLocationBlock = (locationPattern) => {
  const locationIndex = source.indexOf(locationPattern)
  assert.notEqual(locationIndex, -1, `${locationPattern} must exist in admin frontend nginx config`)

  const openBraceIndex = source.indexOf('{', locationIndex)
  assert.notEqual(openBraceIndex, -1, `${locationPattern} must have an opening brace`)

  let depth = 0
  for (let index = openBraceIndex; index < source.length; index += 1) {
    const char = source[index]
    if (char === '{') depth += 1
    if (char === '}') {
      depth -= 1
      if (depth === 0) {
        return source.slice(openBraceIndex + 1, index)
      }
    }
  }
  throw new Error(`${locationPattern} block is not closed`)
}

const assertNoStoreEntryHeaders = (locationPattern) => {
  const block = extractLocationBlock(locationPattern)
  assert.match(
    block,
    /add_header\s+Cache-Control\s+"no-store,\s*no-cache,\s*must-revalidate,\s*max-age=0"\s+always;/,
    `${locationPattern} must force revalidation so browser does not keep stale app shell after release`
  )
  assert.match(block, /add_header\s+Pragma\s+"no-cache"\s+always;/)
  assert.match(block, /add_header\s+Expires\s+"0"\s+always;/)
  assert.match(block, /try_files\s+\/index\.html\s+=404;/)
}

assertNoStoreEntryHeaders('location = / ')
assertNoStoreEntryHeaders('location = /index.html ')

const assetsBlock = extractLocationBlock('location ^~ /assets/ ')
assert.match(
  assetsBlock,
  /add_header\s+Cache-Control\s+"public,\s*max-age=31536000,\s*immutable"\s+always;/,
  'hashed frontend assets must be immutable while index.html stays no-cache'
)
assert.match(assetsBlock, /try_files\s+\$uri\s+=404;/)

const adminApiIndex = source.indexOf('location /admin-api/ ')
const spaFallbackIndex = source.indexOf('location / {')
assert(adminApiIndex >= 0, 'admin API proxy must exist')
assert(spaFallbackIndex >= 0, 'SPA fallback must exist')
assert(
  adminApiIndex < spaFallbackIndex,
  'admin API proxy must be declared before SPA fallback to avoid serving index.html for API calls'
)

console.log('admin frontend nginx cache headers contract passed')
