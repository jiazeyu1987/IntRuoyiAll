import assert from 'node:assert/strict'
import fs from 'node:fs'

const read = (path) => fs.readFileSync(new URL(`../${path}`, import.meta.url), 'utf8')
const api = read('src/api/dcc/registrationCertificate/index.ts')
const list = read('src/views/dcc/registration-certificate/index/index.vue')
const detail = read('src/views/dcc/registration-certificate/detail/index.vue')

assert.match(api, /export interface DccRegistrationCertificatePageItemVO[\s\S]*hasPendingChange:\s*boolean/,
  'current list item must expose the backend pending-change state')
assert.match(list, /v-if="row\.status === 'CURRENT' && row\.hasPendingChange === false && canChangeRegistrationCertificate"[\s\S]*>\s*变更\s*</,
  'current list must block the change entry while an approval is pending and allow authorized roles')
assert.match(detail, /if \(status === 'APPLIED'\) return '已变更'/,
  'applied change history must display 已变更')
assert.match(detail, /if \(status === 'PENDING_APPROVAL'\) return '待审批'/,
  'pending change history must display 待审批')
assert.match(detail, /if \(status === 'REJECTED'\) return '已驳回'/,
  'rejected change history must display 已驳回')
assert.match(detail, /const key = String\(item\.changeId\)/,
  'change history must group by the formal change id')
assert.match(api, /submittedByName\?: string[\s\S]*reviewedByName\?: string/,
  'history API type must expose resolved submitter and reviewer names')

console.log('PASS: registration certificate single-row and pending-change static contract')
