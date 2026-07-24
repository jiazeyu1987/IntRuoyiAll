const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const pagePath = path.resolve(process.cwd(), 'src/views/mes/pro/edhr-batch/BatchExecutionListPage.vue')
const source = fs.readFileSync(pagePath, 'utf8')

assert(
  source.includes("import * as UserApi from '@/api/system/user'"),
  'Readiness panel must import the formal system user API instead of hard-coding rehearsal users.'
)

assert(
  source.includes('readinessUserOptions') &&
    source.includes('readinessUserLoading') &&
    source.includes('loadReadinessUsers'),
  'Readiness panel must own user selector options, loading state, and a user-list loader.'
)

assert(
  source.includes('UserApi.getSimpleUserList()'),
  'Readiness panel must load users through the formal getSimpleUserList API.'
)

assert(
  source.includes('resolveReadinessUserLabel') &&
    source.includes('username') &&
    source.includes('nickname') &&
    source.includes('ID'),
  'Readiness user options must expose username/nickname and numeric ID to prevent ID guessing.'
)

for (const field of ['executorUserId', 'approverUserId', 'archiverUserId']) {
  assert(
    source.includes(`v-model="readinessForm.${field}"`) &&
      source.includes(':loading="readinessUserLoading"') &&
      source.includes(':filterable="true"'),
    `Readiness ${field} must be a searchable user selector with loading state.`
  )
}

assert(
  source.includes("resolveErrorMessage(error, '演练用户列表加载失败。')"),
  'User-list loading failures must be visible in readinessError.'
)

console.log('PASS: eDHR rehearsal role selector static contract')
