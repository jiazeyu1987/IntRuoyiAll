const fs = require('fs')
const path = require('path')
const assert = require('assert')

const repoRoot = path.resolve(__dirname, '..', '..')
const pagePath = path.join(repoRoot, 'src/views/infra/runtime-control/index.vue')
const apiPath = path.join(repoRoot, 'src/api/infra/runtimeControl/index.ts')
const sharedPath = path.join(repoRoot, 'src/views/infra/runtime-control/components/shared.ts')

const pageSource = fs.readFileSync(pagePath, 'utf8')
const apiSource = fs.readFileSync(apiPath, 'utf8')
const sharedSource = fs.readFileSync(sharedPath, 'utf8')

for (const fragment of [
  "action: 'build-release'",
  "label: '构建发布包'",
  "action: 'publish-test'",
  "label: '部署发布包到测试服'",
  "action: 'mark-release-tested'",
  "label: '标记测试通过'",
  "action: 'promote-prod'",
  "label: '上线已验证发布包'",
  "action: 'promote-backup'",
  "label: '上线备份服务器'",
  'releaseTag'
]) {
  assert(
    pageSource.includes(fragment) || apiSource.includes(fragment) || sharedSource.includes(fragment),
    `runtime control release package UI must include ${fragment}`
  )
}

assert(!pageSource.includes("label: '提升正式服'"), 'runtime control page must not show old promote wording')
assert(!sharedSource.includes("'promote-prod': '提升正式服'"), 'shared action text must not show old promote wording')
assert(
  apiSource.includes('releaseTag?: string'),
  'runtime control action request must expose releaseTag'
)
assert(
  apiSource.includes('testConclusion?: string'),
  'runtime control action request must expose testConclusion for mark-tested'
)
assert(
  apiSource.includes('includeOnlyOffice?: boolean'),
  'runtime control action request must expose includeOnlyOffice for build-release'
)
assert(
  pageSource.includes("operationDialog.releaseTag"),
  'operation dialog must bind and submit releaseTag'
)
assert(
  pageSource.includes("const DEFAULT_BUILD_RELEASE_REASON = '默认发布'"),
  'build-release dialog must define the default release reason'
)
assert(
  pageSource.includes('const formatDefaultReleaseTag = (date = new Date()) => {') &&
    pageSource.includes('String(date.getFullYear()).slice(-2)') &&
    pageSource.includes('`${year}-${month}-${day} ${hour}:${minute}:${second}`'),
  'build-release dialog must format the default release tag as YY-MM-DD HH:MM:SS'
)
assert(
  pageSource.includes(
    "operationDialog.releaseTag = action.action === 'build-release' ? formatDefaultReleaseTag() : ''"
  ),
  'build-release dialog must default releaseTag only for build-release'
)
assert(
  pageSource.includes('includeOnlyOffice: boolean') &&
    pageSource.includes('includeOnlyOffice: false') &&
    pageSource.includes('operationDialog.includeOnlyOffice = false'),
  'build-release dialog must default OnlyOffice publishing to unchecked'
)
assert(
  pageSource.includes('发布 OnlyOffice') &&
    pageSource.includes('v-model="operationDialog.includeOnlyOffice"'),
  'build-release dialog must render an OnlyOffice publish checkbox'
)
assert(
  pageSource.includes("action.action === 'build-release'") &&
    pageSource.includes('DEFAULT_BUILD_RELEASE_REASON') &&
    pageSource.includes("action.action === 'publish-test'") &&
    pageSource.includes('DEFAULT_PUBLISH_TEST_REASON'),
  'operation dialog must preserve build-release and publish-test default reasons'
)

assert(
  pageSource.includes("const DEFAULT_PROMOTE_PROD_REASON = '默认发布'"),
  'promote-prod dialog must define the default release reason'
)
assert(
  pageSource.includes("return ['publish-test', 'promote-prod', 'promote-backup'].includes(action)"),
  'promote-prod and promote-backup must share the release selector with publish-test'
)
assert(
  pageSource.includes(
    "action.action === 'promote-prod'\n          ? DEFAULT_PROMOTE_PROD_REASON"
  ) ||
    pageSource.includes("action.action === 'promote-prod'") &&
      pageSource.includes('DEFAULT_PROMOTE_PROD_REASON'),
  'promote-prod dialog must default reason to 默认发布'
)
assert(
  pageSource.includes("const testCurrentReleaseTag = computed(() => currentReleaseTagValue('test'))"),
  'release selector must derive the current test release tag'
)
assert(
  pageSource.includes("if (operationUsesCurrentTestReleaseTag(action.action))") &&
    pageSource.includes('await loadOverview()'),
  'opening mark-release-tested must refresh overview before reading the current test release tag'
)
assert(
  pageSource.includes('releasePackages.value') &&
    pageSource.includes('.filter((item) => item.tested)'),
  'production-grade selectors must derive tested packages from ReleasePackage tested metadata'
)
assert(
  pageSource.includes("action.action === 'promote-backup'") &&
    pageSource.includes('DEFAULT_PROMOTE_BACKUP_REASON'),
  'promote-backup dialog must default reason to 默认发布'
)
assert(
  pageSource.includes("if (releaseTag === testCurrentReleaseTag.value) return '当前测试服'") &&
    pageSource.includes("if (testUsedReleaseTags.value.has(releaseTag)) return '曾部署测试服'"),
  'release selector must label current and previously used test releases'
)
assert(
  pageSource.includes('releasePackageUsageText(item.releaseTag)') &&
    pageSource.includes('releasePackageUsageClass(item.releaseTag)'),
  'release selector must render release usage labels in the option list'
)
assert(
  pageSource.includes('releasePackageOnlyOfficeText(item)') &&
    pageSource.includes("'不含 OnlyOffice'") &&
    pageSource.includes("'包含 OnlyOffice'"),
  'release selector must render OnlyOffice inclusion status'
)
assert(
  pageSource.includes("item.status !== 'BLOCKED'") &&
    pageSource.includes('item.checksumPresent !== false'),
  'release selector must hide blocked or checksum-incomplete ReleasePackage candidates'
)
assert(
  apiSource.includes('checksumPresent?: boolean') &&
    apiSource.includes('tested?: boolean') &&
    apiSource.includes('manifestPath?: string') &&
    apiSource.includes('onlyOfficeIncluded?: boolean'),
  'release package API type must expose manifest, checksum and tested metadata'
)
assert(
  pageSource.includes('includeOnlyOffice:') &&
    pageSource.includes("operationDialog.action === 'build-release'") &&
    pageSource.includes('operationDialog.includeOnlyOffice'),
  'build-release submit payload must include the OnlyOffice decision'
)

console.log('PASS: runtime control release package static wiring is present')
