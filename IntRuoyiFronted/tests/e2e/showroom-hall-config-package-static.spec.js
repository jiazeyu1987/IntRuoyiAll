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

function assertNotContains(source, forbidden, label) {
  assert.ok(!source.includes(forbidden), `unexpected ${label}: ${forbidden}`)
}

const packageJson = JSON.parse(readUtf8('package.json'))
const api = readUtf8('src/api/showroom-admin/index.ts')
const hallTable = readUtf8('src/views/showroom-admin/components/HallListTable.vue')
const page = readUtf8('src/views/showroom-admin/index.vue')

assert.equal(
  packageJson.scripts?.['e2e:showroom:hall-config-package:static'],
  'node tests/e2e/showroom-hall-config-package-static.spec.js',
  'package.json must expose the showroom hall config package static gate'
)

for (const fragment of [
  'ShowroomHallConfigPackageImportRespVO',
  '/showroom/hall/config-package/export',
  '/showroom/hall/config-package/import',
  'request.download',
  'request.upload'
]) {
  assertContains(api, fragment, 'showroom hall config package API contract')
}

for (const fragment of [
  '导出数据包',
  '导入数据包',
  "accept=\".zip,application/zip\"",
  "emit('export-config-package')",
  "emit('import-config-package', file)"
]) {
  assertContains(hallTable, fragment, 'hall list table config package contract')
}

for (const fragment of [
  ':exporting-config-package="exportingHallConfigPackage"',
  ':importing-config-package="importingHallConfigPackage"',
  ':manage-config-package="canManageHallConfigPackage"',
  "@export-config-package=\"handleExportHallConfigPackage\"",
  "@import-config-package=\"handleImportHallConfigPackage\"",
  "download.zip(data, 'showroom-hall-config-package.zip')",
  "formatShowroomStructuredError(error, '导入展柜数据包')",
  '仅支持导入 zip 数据包'
]) {
  assertContains(page, fragment, 'showroom admin hall config package page contract')
}

assertNotContains(page, 'catch {}', 'empty catch in showroom admin page')
assertNotContains(page, 'catch{}', 'empty catch in showroom admin page')
assertNotContains(hallTable, 'catch {}', 'empty catch in hall list table')
assertNotContains(hallTable, 'catch{}', 'empty catch in hall list table')

console.log('PASS: showroom hall config package static contract is present')
