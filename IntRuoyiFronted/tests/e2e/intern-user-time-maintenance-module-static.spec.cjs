const assert = require('assert')
const fs = require('fs')
const path = require('path')

const root = path.resolve(__dirname, '../..')
const repoRoot = path.resolve(root, '..')

function read(relativePath) {
  return fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')
}

const filePage = read('IntRuoyiFronted/src/views/infra/file/index.vue')
const form = read('IntRuoyiFronted/src/views/infra/file/FileUploadTimeForm.vue')
const auditDialog = read('IntRuoyiFronted/src/views/intern-user/time-maintenance/InternUserTimeAuditDialog.vue')
const dccTimeForm = read('IntRuoyiFronted/src/views/intern-user/time-maintenance/InternUserDccTimeForm.vue')
const dccBrowserPage = read('IntRuoyiFronted/src/views/dcc/controlled-file/browser/index.vue')
const infraFileApi = read('IntRuoyiFronted/src/api/infra/file/index.ts')
const internUserApi = read('IntRuoyiFronted/src/api/intern-user/timeMaintenance/index.ts')
const moduleSql = read('IntRuoyiBackend/sql/mysql/20260922_intern_user_time_maintenance_module.sql')

assert(
  filePage.includes("intern-user:time-maintenance:file-upload-time:update"),
  'file page must expose upload-time edit entry through intern-user module permission'
)
assert(
  !filePage.includes("infra:file:update-upload-time"),
  'file page must not use the old generic infra upload-time permission'
)
assert(
  form.includes("@/api/intern-user/timeMaintenance"),
  'upload-time form must call the intern-user module API wrapper'
)
assert(
  !form.includes("@/api/infra/file"),
  'upload-time form must not call the generic infra file API wrapper'
)
assert(
  internUserApi.includes("'/intern-user/time-maintenance/file/upload-time'"),
  'intern-user API wrapper must call the module endpoint'
)
assert(
  internUserApi.includes("'/intern-user/time-maintenance/file/upload-time/audits'"),
  'intern-user API wrapper must expose upload-time audit query endpoint'
)
assert(
  internUserApi.includes("'/intern-user/time-maintenance/dcc/published-time'"),
  'intern-user API wrapper must expose DCC published-time update endpoint'
)
assert(
  internUserApi.includes("'/intern-user/time-maintenance/dcc/published-time/audits'"),
  'intern-user API wrapper must expose DCC published-time audit endpoint'
)
assert(
  internUserApi.includes("'/intern-user/time-maintenance/dcc/obsoleted-time'"),
  'intern-user API wrapper must expose DCC obsoleted-time update endpoint'
)
assert(
  internUserApi.includes("'/intern-user/time-maintenance/dcc/obsoleted-time/audits'"),
  'intern-user API wrapper must expose DCC obsoleted-time audit endpoint'
)
assert(
  !infraFileApi.includes('/infra/file/update-upload-time'),
  'generic infra file API wrapper must not expose the upload-time update endpoint'
)
assert(
  filePage.includes("intern-user:time-maintenance:file-upload-time-audit:query"),
  'file page must expose upload-time audit entry through intern-user permission'
)
assert(
  auditDialog.includes('getFileUploadTimeAudits') &&
    auditDialog.includes('getDccPublishedTimeAudits') &&
    auditDialog.includes('getDccObsoletedTimeAudits'),
  'audit dialog must load all intern-user time maintenance audit types'
)
assert(
  dccTimeForm.includes('updateDccPublishedTime') && dccTimeForm.includes('updateDccObsoletedTime'),
  'DCC time form must call intern-user DCC time update APIs'
)
for (const permission of [
  'intern-user:time-maintenance:dcc-published-time:update',
  'intern-user:time-maintenance:dcc-published-time-audit:query',
  'intern-user:time-maintenance:dcc-obsoleted-time:update',
  'intern-user:time-maintenance:dcc-obsoleted-time-audit:query'
]) {
  assert(
    dccBrowserPage.includes(permission),
    `DCC browser page must expose ${permission} through intern-user module permission`
  )
}
assert(
  moduleSql.includes("'实习用户', 'intern_user'"),
  'module SQL must seed the customer-required role name and code'
)
assert(
  moduleSql.includes("'intern-user:time-maintenance:file-upload-time:update'"),
  'module SQL must seed the intern-user module permission'
)
for (const permission of [
  'intern-user:time-maintenance:file-upload-time-audit:query',
  'intern-user:time-maintenance:dcc-published-time:update',
  'intern-user:time-maintenance:dcc-published-time-audit:query',
  'intern-user:time-maintenance:dcc-obsoleted-time:update',
  'intern-user:time-maintenance:dcc-obsoleted-time-audit:query'
]) {
  assert(moduleSql.includes(`'${permission}'`), `module SQL must seed ${permission}`)
}
assert(
  moduleSql.includes('CREATE TABLE IF NOT EXISTS `intern_user_time_maintenance_audit`'),
  'module SQL must create the intern-user time maintenance audit table'
)
assert(
  !moduleSql.includes("'infra:file:update-upload-time'"),
  'module SQL must not seed the old generic infra permission'
)

console.log('intern-user time maintenance module static contract passed')
