const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '../..')
const backendRoot = path.resolve(frontendRoot, '..', 'IntRuoyiBackend')

const readFrontend = (relativePath) =>
  fs.readFileSync(path.join(frontendRoot, relativePath), 'utf8')
const readBackend = (relativePath) =>
  fs.readFileSync(path.join(backendRoot, relativePath), 'utf8')

const userApiSource = readFrontend('src/api/system/user/index.ts')
const userPageSource = readFrontend('src/views/system/user/index.vue')
const userControllerSource = readBackend(
  'yudao-module-system/src/main/java/cn/iocoder/yudao/module/system/controller/admin/user/UserController.java'
)

assert.ok(
  userApiSource.includes('exportGenericAccountUsers') &&
    userApiSource.includes("/system/user/export-generic-account-excel"),
  'System user API must expose the generic account non-compliance export route.'
)

assert.ok(
  userPageSource.includes('command="exportGenericAccounts"') &&
    userPageSource.includes('导出通用账户清单'),
  'System user advanced actions must include a generic account export entry.'
)

assert.ok(
  userPageSource.includes('handleExportGenericAccounts') &&
    userPageSource.includes("download.excel(data, '通用账户不合规清单.xls')"),
  'System user page must download the generic account non-compliance workbook.'
)

assert.ok(
  userControllerSource.includes('@GetMapping("/export-generic-account-excel")') &&
    userControllerSource.includes('getGenericAccountUserList()'),
  'System user backend controller must provide the generic account export endpoint.'
)

console.log('PASS: system user generic account governance static contract')
