const fs = require('fs')
const path = require('path')
const assert = require('assert')

const repoRoot = path.resolve(__dirname, '..', '..')
const apiPath = path.join(repoRoot, 'src', 'api', 'mes', 'pro', 'processpool', 'teamLeader.ts')
const pagePath = path.join(
  repoRoot,
  'src',
  'views',
  'mes',
  'pro',
  'processpool',
  'TeamLeaderWorkbenchPage.vue'
)

const apiSource = fs.readFileSync(apiPath, 'utf8')
const pageSource = fs.readFileSync(pagePath, 'utf8')

assert(
  apiSource.includes('TeamLeaderActiveOrderVersionUpgradePreviewRespVO'),
  'frontend API must expose active-order version-upgrade preview response contract'
)
assert(
  apiSource.includes('previewTeamLeaderActiveOrderVersionUpgrade') &&
    apiSource.includes('/mes/pro/process-pool/team-leader/active-order/version-upgrade/preview'),
  'frontend API must call backend preview endpoint for active-order version upgrade'
)
assert(
  apiSource.includes('submitTeamLeaderActiveOrderVersionUpgrade') &&
    apiSource.includes('/mes/pro/process-pool/team-leader/active-order/version-upgrade/submit'),
  'frontend API must call backend submit endpoint for active-order version upgrade'
)
assert(
  pageSource.includes('data-team-leader-active-order-version-upgrade') &&
    pageSource.includes("v-hasPermi=\"['mes:pro-process-pool-team-leader:version-upgrade']\"") &&
    pageSource.includes('handleActiveOrderVersionUpgrade(row)') &&
    pageSource.includes('>升级<'),
  'active-order row operations must expose an Upgrade button wired to the version-upgrade permission and flow'
)
assert(
  pageSource.includes('版本升级重启') &&
    pageSource.includes('全部最新正式版本') &&
    pageSource.includes('不提供逐项版本选择') &&
    pageSource.includes('整单从头执行'),
  'upgrade confirmation must communicate all-latest-version restart semantics'
)
assert(
  !/el-select[\s\S]{0,240}(versionUpgrade|升级目标|targetVersion)/.test(pageSource),
  'upgrade confirmation must not introduce per-version selection controls'
)

console.log('active-order version upgrade frontend static contract PASS')
