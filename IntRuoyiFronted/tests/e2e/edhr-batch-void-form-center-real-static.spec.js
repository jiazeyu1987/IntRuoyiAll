const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const readSource = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const listPage = readSource('src/views/mes/pro/edhr-batch/BatchExecutionListPage.vue')
const changeApi = readSource('src/api/mes/pro/edhr/change.ts')

assert.match(
  listPage,
  /resolveVoidBatchExecutionApproval[\s\S]*requestVoidBatchExecution/,
  'eDHR batch void must resolve the BPM approval route from the business approval policy table before submit.'
)

assert.match(
  changeApi,
  /resolveVoidBatchExecutionApproval[\s\S]*\/mes\/pro\/edhr-change\/void-batch-execution\/approval-resolution/,
  'eDHR batch void approval route resolution must use the eDHR business approval endpoint.'
)

assert.match(
  changeApi,
  /requestVoidBatchExecution[\s\S]*\/mes\/pro\/edhr-change\/void-batch-execution\/request/,
  'eDHR batch void submit must still use the controlled batch void request endpoint.'
)

assert.doesNotMatch(
  listPage,
  /createFormInstance|submitFormInstance|resolveBusinessAction\(context\)/,
  'eDHR batch void must not use form-center instance or form policy resolution.'
)

assert.doesNotMatch(
  listPage,
  /\/form-center\/actions\/resolve/,
  'eDHR batch void page must not call the form-center policy resolver.'
)

console.log('PASS: eDHR batch void business approval policy static contract')
