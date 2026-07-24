import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'

const root = path.resolve(process.cwd())
const read = (file) => fs.readFileSync(path.join(root, file), 'utf8')

const api = read('src/api/approval-center/index.ts')
const approvalCenter = read('src/views/approval-center/index.vue')
const srmReview = read('src/views/srm/supplier-portal/review/index.vue')

assert.match(api, /'SRM'/, 'approval center API module union must expose SRM')
assert.match(approvalCenter, /supportedModuleCodes:[\s\S]*'SRM'/, 'approval center route query guard must accept SRM')
assert.match(srmReview, /route\.query\.applicationId/, 'SRM formal review page must read applicationId from unified center')
assert.match(
  srmReview,
  /openDetail\([\s\S]*applicationId|applicationId[\s\S]*openDetail/,
  'SRM formal review page must open or locate the unified-center application record'
)
assert.doesNotMatch(
  approvalCenter,
  /SrmSupplierPortalApi|srm:supplier-portal:audit|approve\(|reject\(/,
  'approval center must not execute SRM supplier portal approve or reject actions'
)

process.stdout.write('approval-center phase6 SRM static contract passed\n')
