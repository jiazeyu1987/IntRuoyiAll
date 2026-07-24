const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')

function readUtf8(relativePath) {
  const absolutePath = path.join(repoRoot, relativePath)
  assert.ok(fs.existsSync(absolutePath), `missing required file: ${relativePath}`)
  return fs.readFileSync(absolutePath, 'utf8')
}

function assertContains(source, expected, label) {
  assert.ok(source.includes(expected), `missing ${label}: ${expected}`)
}

const packageJson = JSON.parse(readUtf8('package.json'))
const axiosService = readUtf8('src/config/axios/service.ts')
const rolePage = readUtf8('src/views/system/role/index.vue')
const dccAdminPage = readUtf8('src/views/dcc/controlled-file/admin/index.vue')

assert.equal(
  packageJson.scripts?.['e2e:dcc:json-attachment-download:static'],
  'node tests/e2e/dcc-json-attachment-download-static.spec.js',
  'package.json must expose the JSON attachment download static gate'
)

for (const fragment of [
  'content-disposition',
  'attachment',
  'isBinaryAttachmentResponse',
  'contentType.includes(\'application/json\')'
]) {
  assertContains(axiosService, fragment, 'axios JSON attachment download guard')
}

assertContains(
  rolePage,
  "download.json(data, '权限角色配置包.json')",
  'permission role package export still goes through the shared download path'
)
assertContains(
  dccAdminPage,
  "download.json(downloadBlob, '文控管理员全量配置包.json')",
  'DCC admin full config export still goes through the shared download path'
)

console.log('PASS: JSON attachment downloads are preserved as Blob responses')
