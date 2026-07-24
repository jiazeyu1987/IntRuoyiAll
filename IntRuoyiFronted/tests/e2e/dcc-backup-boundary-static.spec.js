const fs = require('fs')
const path = require('path')
const assert = require('assert')

const repoRoot = path.resolve(__dirname, '..', '..')
const scripts = [
  'tests/e2e/dcc-incremental-backup-preflight.e2e.js',
  'tests/e2e/dcc-upload-test-file.e2e.js',
  'tests/e2e/dcc-withdraw-delete-file.e2e.js',
  'tests/e2e/dcc-restore-verify.e2e.js'
]

for (const relativePath of scripts) {
  const source = fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')
  assert(
    source.includes('172.30.30.57'),
    `${relativePath} must explicitly reject the protected production server`
  )
  assert(
    source.includes("TENANT !== '测试租户'"),
    `${relativePath} must fail fast outside the test tenant`
  )
  assert(
    source.includes("USERNAME !== 'aoteman'"),
    `${relativePath} must require the dedicated test user aoteman`
  )
  assert(
    !source.includes('http://localhost:8081'),
    `${relativePath} must not silently default to localhost:8081`
  )
  assert(
    source.includes('assertSafeDccBackupBoundary()'),
    `${relativePath} must run the DCC backup boundary assertion before opening the browser`
  )
}

console.log('PASS: DCC backup E2E scripts hard-assert protected boundary')
