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
const api = readUtf8('src/api/dcc/controlledFile/workflow.ts')
const page = readUtf8('src/views/dcc/controlled-file/admin/index.vue')

assert.equal(
  packageJson.scripts?.['e2e:dcc:admin-full-config:static'],
  'node tests/e2e/dcc-admin-full-config-static.spec.js',
  'package.json must expose the DCC admin full config package static gate'
)

assert.equal(
  packageJson.scripts?.['e2e:dcc:admin-full-config:route:static'],
  'node tests/e2e/dcc-admin-full-config-route-static.spec.js',
  'package.json must expose the DCC admin full config package route static gate'
)

for (const fragment of [
  'export interface DccAdminFullConfigPackageImportRespVO',
  'export const exportAdminConfigPackage = async (): Promise<Blob>',
  "url: '/dcc/file-categories/admin-config-package/export'",
  'export const exportDmrSheetWorkbook = async (): Promise<Blob>',
  "url: '/dcc/controlled-files/dmr-sheet/export'",
  'timeout: DCC_ADMIN_FULL_CONFIG_IMPORT_REQUEST_TIMEOUT',
  'export const importAdminConfigPackage = async (',
  "url: '/dcc/file-categories/admin-config-package/import'",
  'request.download',
  'request.upload'
]) {
  assertContains(api, fragment, 'dcc admin full config API contract')
}

for (const fragment of [
  "defineOptions({ name: 'DccControlledFileAdmin' })",
  '导出数据包',
  'DMR-sheet',
  '导入数据包',
  "accept=\".json,application/json\"",
  'handleExportAdminConfigPackage',
  'handleExportDmrSheet',
  "download.excel(downloadBlob, 'DMR-sheet.xlsx')",
  'handleAdminConfigFileChange',
  "download.json(downloadBlob, '文控管理员全量配置包.json')",
  'approvalPositionCount',
  'directoryCount',
  'distributionRuleCount',
  'trainingRuleCount'
]) {
  assertContains(page, fragment, 'dcc admin page contract')
}

assertNotContains(page, 'catch {}', 'empty catch in DCC admin page')
assertNotContains(page, 'catch{}', 'empty catch in DCC admin page')
const dmrHandlerStart = page.indexOf('const handleExportDmrSheet')
const dmrHandlerEnd = page.indexOf('const openAdminConfigImport')
assert.ok(dmrHandlerStart >= 0 && dmrHandlerEnd > dmrHandlerStart, 'DMR handler must be present')
assertNotContains(
  page.slice(dmrHandlerStart, dmrHandlerEnd),
  'throw error',
  'DMR export handler must surface message without emitting an unhandled page error'
)

console.log('PASS: dcc admin full config static contract is present')
