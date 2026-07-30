import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'

const root = process.cwd()
const readText = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')
const mySignaturePanePath = 'src/views/signature-governance/components/SignatureGovernanceMySignaturePane.vue'
const governanceListPanePaths = [
  'src/views/signature-governance/components/SignatureGovernanceRecordsPane.vue',
  'src/views/signature-governance/components/RetentionGovernanceListPane.vue',
  'src/views/signature-governance/components/PeriodicReviewGovernanceListPane.vue',
  'src/views/signature-governance/components/CsvPackageGovernanceListPane.vue',
  'src/views/signature-governance/components/PolicyGovernanceListPane.vue'
]
const readGovernanceListPaneBundle = () => governanceListPanePaths.map(readText).join('\n')

test('signature governance workbench renders child route content directly', () => {
  const pagePath = 'src/views/signature-governance/index.vue'
  assert.equal(fs.existsSync(path.join(root, pagePath)), true, `${pagePath} must exist`)
  const source = readText(pagePath)
  const governanceSource = `${source}\n${readGovernanceListPaneBundle()}\n${readText('src/api/signature-governance/shared.ts')}`

  assert.match(source, /defineOptions\(\{\s*name:\s*'SignatureGovernanceWorkbench'/)
  assert.equal(fs.existsSync(path.join(root, mySignaturePanePath)), true, `${mySignaturePanePath} must exist`)
  assert.match(source, /activeTab/)
  assert.match(source, /signatureTabRoutes/)
  assert.match(source, /route\.path/)
  assert.match(source, /SIGNATURE_MANAGE_PERMISSION\s*=\s*'dcc:controlled-file:signature:manage'/)
  assert.match(source, /SIGNATURE_ADMIN_ROLE\s*=\s*'electronic_signature_admin'/)
  assert.match(source, /canViewAuthorizations/)
  assert.match(source, /userStore\.getPermissions\.has\(SIGNATURE_MANAGE_PERMISSION\)/)
  assert.match(source, /userStore\.getRoles\.includes\(SIGNATURE_ADMIN_ROLE\)/)
  assert.match(source, /当前账号没有电子签名管理员权限/)
  assert.match(source, /activeTab\s*===\s*'authorizations'\s*&&\s*canViewAuthorizations/)
  assert.doesNotMatch(source, /route\.query\.tab/)
  assert.doesNotMatch(source, /<el-tabs/)
  assert.doesNotMatch(source, /<el-tab-pane/)
  assert.doesNotMatch(source, /@tab-change/)
  assert.doesNotMatch(source, /handleTabChange/)
  assert.doesNotMatch(source, /signature-governance__toolbar/)
  assert.doesNotMatch(source, /signature-governance__tabs/)
  assert.doesNotMatch(source, /统一入口 \| 文件签名、批记录签名、授权与治理策略/)
  assert.doesNotMatch(source, /getSignatureGovernancePortalOverview/)
  assert.doesNotMatch(source, /portalResult/)
  assert.doesNotMatch(source, /activeTab\s*===\s*'overview'/)
  assert.doesNotMatch(source, /\/signature-governance\/overview/)
  assert.doesNotMatch(source, /统一电子签名入口/)
  assert.doesNotMatch(source, /刷新电子签名/)
  for (const tabPath of [
    '/signature-governance/signature-records',
    '/signature-governance/my-signature',
    '/signature-governance/authorizations',
    '/signature-governance/retention',
    '/signature-governance/periodic-review',
    '/signature-governance/csv-package',
    '/signature-governance/policy'
  ]) {
    assert.match(source, new RegExp(tabPath.replace(/[/-]/g, '\\$&')), `${tabPath} must be wired`)
  }
  for (const embeddedComponent of [
    'SignatureGovernanceRecordsPane',
    'SignatureGovernanceMySignaturePane',
    'DccSignatureAuthorizationsPane',
    'RetentionGovernanceListPane',
    'PeriodicReviewGovernanceListPane',
    'CsvPackageGovernanceListPane',
    'PolicyGovernanceListPane'
  ]) {
    assert.match(source, new RegExp(embeddedComponent), `${embeddedComponent} must be embedded`)
  }
  for (const tabName of [
    'signature-records',
    'my-signature',
    'authorizations',
    'retention',
    'periodic-review',
    'csv-package',
    'policy'
  ]) {
    assert.match(source, new RegExp(`activeTab\\s*===\\s*'${tabName}'`), `${tabName} content must render from route`)
  }
  assert.doesNotMatch(source, /signature-governance__overview-actions/)
  assert.doesNotMatch(source, /DCC\s*电子签名/)
  assert.doesNotMatch(source, /eDHR\s*电子签名/)
  assert.doesNotMatch(source, /goModuleRoute\(module\.routes\.primaryPath\)/)
  for (const apiName of [
    'precheckSignatureRetention',
    'createDccSignatureRetentionReceipt',
    'createEdhrArchiveRetentionReceipt',
    'runSignatureRecoveryRehearsal',
    'createSignaturePeriodicReviewBatch',
    'evaluateSignatureCsvReleaseGate',
    'getCurrentSignatureGovernancePolicy'
  ]) {
    assert.match(governanceSource, new RegExp(apiName))
  }
  for (const blockerCode of [
    'OBJECT_LOCK_MISSING',
    'REVIEW_OWNER_MISSING',
    'QA_APPROVAL_MISSING',
    'POLICY_SOURCE_MISSING'
  ]) {
    assert.match(governanceSource, new RegExp(blockerCode))
  }

  assert.match(governanceSource, /v-hasPermi/)
  assert.doesNotMatch(governanceSource, /mock|fallback|TODO/i)
})

test('signature governance route is reachable without adding a fake menu', () => {
  const routeSource = readText('src/router/modules/remaining.ts')
  assert.match(routeSource, /path:\s*'\/signature-governance'/)
  assert.match(routeSource, /redirect:\s*'\/signature-governance\/signature-records'/)
  assert.doesNotMatch(routeSource, /redirect:\s*'\/signature-governance\/overview'/)
  assert.doesNotMatch(routeSource, /path:\s*'overview'/)
  assert.doesNotMatch(routeSource, /name:\s*'SignatureGovernanceOverview'/)
  assert.doesNotMatch(routeSource, /title:\s*'总览'/)
  assert.doesNotMatch(routeSource, /activeMenu:\s*'\/signature-governance\/overview'/)
  assert.match(routeSource, /@\/views\/signature-governance\/index.vue/)
  assert.match(routeSource, /title:\s*'电子签名'/)
  assert.match(routeSource, /signature-governance:policy:query/)
  for (const [routeName, routePath, title] of [
    ['SignatureGovernanceSignatureRecords', 'signature-records', '签名记录'],
    ['SignatureGovernanceMySignature', 'my-signature', '我的签名'],
    ['SignatureGovernanceAuthorizations', 'authorizations', '用户授权'],
    ['SignatureGovernanceRetention', 'retention', '长期留存'],
    ['SignatureGovernancePeriodicReview', 'periodic-review', '周期复核'],
    ['SignatureGovernanceCsvPackage', 'csv-package', 'CSV质量包'],
    ['SignatureGovernancePolicy', 'policy', '统一策略']
  ]) {
    assert.match(routeSource, new RegExp(`path:\\s*'${routePath}'`), `${routePath} child route must exist`)
    assert.match(routeSource, new RegExp(`name:\\s*'${routeName}'`), `${routeName} route name must exist`)
    assert.match(routeSource, new RegExp(`title:\\s*'${title}'`), `${title} route title must exist`)
  }
  const authorizationsRoute = routeSource.match(
    /path:\s*'authorizations'[\s\S]*?name:\s*'SignatureGovernanceAuthorizations'[\s\S]*?meta:\s*\{[\s\S]*?\n\s*\}/
  )?.[0] || ''
  assert.match(
    authorizationsRoute,
    /permission:\s*\[\s*'dcc:controlled-file:signature:manage'\s*\]/,
    'user authorization route must require signature management permission'
  )
  assert.doesNotMatch(
    authorizationsRoute,
    /permission:\s*\[\s*'signature-governance:policy:query'\s*\]/,
    'ordinary signature governance query permission must not expose all user authorization states'
  )
  const mySignatureRoute = routeSource.match(
    /path:\s*'my-signature'[\s\S]*?name:\s*'SignatureGovernanceMySignature'[\s\S]*?meta:\s*\{[\s\S]*?\n\s*\}/
  )?.[0] || ''
  assert.match(
    mySignatureRoute,
    /permission:\s*\[\s*'signature-governance:policy:query'\s*\]/,
    'my signature route must stay available to ordinary signature governance users'
  )
  assert.doesNotMatch(
    mySignatureRoute,
    /permission:\s*\[\s*'dcc:controlled-file:signature:manage'\s*\]/,
    'my signature route must not require all-user authorization management permission'
  )
  for (const [routeName, routePath, title] of [
    ['SignatureGovernanceFileSignatures', 'file-signatures', '文件签名记录'],
    ['SignatureGovernanceBatchSignatures', 'batch-signatures', '批记录签名记录']
  ]) {
    assert.match(routeSource, new RegExp(`path:\\s*'${routePath}'[\\s\\S]*redirect:\\s*'\\/signature-governance\\/signature-records'`))
    assert.match(routeSource, new RegExp(`name:\\s*'${routeName}'`), `${routeName} legacy redirect route must exist`)
    assert.match(routeSource, new RegExp(`title:\\s*'${title}'`), `${title} legacy title must exist`)
  }
})

test('hidden static route merge keeps dynamic sidebar visibility', () => {
  const permissionSource = readText('src/store/modules/permission.ts')

  assert.match(permissionSource, /mergeHiddenStaticChildWithDynamicChild/)
  assert.match(permissionSource, /hidden:\s*dynamicChild\.meta\?\.hidden\s*\?\?\s*staticChild\.meta\?\.hidden/)
  assert.doesNotMatch(
    permissionSource,
    /meta:\s*\{\s*\.\.\.dynamicChild\.meta,\s*\.\.\.staticChild\.meta\s*\}/s
  )
})

test('signature governance merge removes stale dynamic overview menu child', () => {
  const permissionSource = readText('src/store/modules/permission.ts')
  const hiddenShellSource = permissionSource.slice(
    permissionSource.indexOf('const mergeHiddenStaticShellRoute'),
    permissionSource.indexOf('const mergeStaticRoutesWithDynamicRoutes')
  )

  assert.match(permissionSource, /SIGNATURE_GOVERNANCE_ROUTE_PATH\s*=\s*'\/signature-governance'/)
  assert.match(permissionSource, /LEGACY_SIGNATURE_GOVERNANCE_OVERVIEW_CHILD_PATH\s*=\s*'overview'/)
  assert.match(permissionSource, /isSignatureGovernanceShellRoute/)
  assert.match(permissionSource, /isLegacySignatureGovernanceOverviewChild/)
  assert.match(permissionSource, /filterSignatureGovernanceDynamicChildren/)
  assert.match(
    permissionSource,
    /filterSignatureGovernanceDynamicChildren\(\s*staticRoute,\s*dynamicRoute\.children\s*\|\|\s*\[\]\s*\)/s
  )
  assert.match(permissionSource, /SIGNATURE_MY_SIGNATURE_ROUTE_PATH\s*=\s*'my-signature'/)
  assert.match(permissionSource, /resolveSignatureGovernanceRedirect/)
  assert.match(permissionSource, /resolveHiddenShellRedirect\(\s*staticRoute,\s*dynamicRoute,\s*mergedRoute\.redirect,\s*dynamicChildren\s*\)/)
  assert.match(
    hiddenShellSource,
    /const appendUncoveredHiddenStaticChildren = !isSignatureGovernanceShellRoute\(staticRoute\)/
  )
  assert.doesNotMatch(hiddenShellSource, /const dynamicChildren\s*=\s*dynamicRoute\.children\s*\|\|\s*\[\]/)
})

test('signature records remains a leaf menu even when cached dynamic routes contain old children', () => {
  const permissionSource = readText('src/store/modules/permission.ts')

  assert.match(permissionSource, /SIGNATURE_RECORDS_ROUTE_PATH\s*=\s*'signature-records'/)
  assert.match(permissionSource, /SIGNATURE_RECORDS_ROUTE_NAME\s*=\s*'SignatureGovernanceSignatureRecords'/)
  assert.match(permissionSource, /isSignatureRecordsRoute/)
  assert.match(permissionSource, /normalizeSignatureGovernanceLeafChildren/)
  assert.match(permissionSource, /children:\s*undefined/)
  assert.match(permissionSource, /alwaysShow:\s*false/)
})

test('signature governance portal api contract is wired', () => {
  const apiPath = 'src/api/signature-governance/portal.ts'
  assert.equal(fs.existsSync(path.join(root, apiPath)), true, `${apiPath} must exist`)
  const source = readText(apiPath)

  assert.match(source, /getSignatureGovernancePortalOverview/)
  assert.match(source, /\/signature-governance\/portal\/overview/)
  assert.match(source, /SignatureGovernancePortalModuleRespVO/)
  assert.match(source, /primaryPath/)
  assert.match(source, /secondaryPath/)
  assert.doesNotMatch(source, /mock|placeholder|fallback|TODO/i)
})

test('signature governance owns unified signature entry paths in profile and E2E helper', () => {
  const profileSource = readText('src/views/Profile/components/ProfileWorkbench.vue')
  const helperSource = readText('tests/e2e/signature-governance-real-flow-helper.js')

  for (const expectedPath of [
    '/signature-governance/signature-records',
    '/signature-governance/my-signature',
    '/signature-governance/authorizations'
  ]) {
    assert.match(`${profileSource}\n${helperSource}`, new RegExp(expectedPath.replace(/[/?-]/g, '\\$&')))
  }
  for (const legacyPath of [
    '/signature-governance/file-signatures',
    '/signature-governance/batch-signatures',
    '/dcc/controlled-file/signatures',
    '/mes/pro/feedback/edhr-signatures',
    '/signature-governance?tab='
  ]) {
    assert.doesNotMatch(`${profileSource}\n${helperSource}`, new RegExp(legacyPath.replace(/[/?-]/g, '\\$&')))
  }
})

test('signature governance embedded panes do not render nested content cards', () => {
  const dccSignatureSource = readText('src/views/dcc/controlled-file/signatures/index.vue')
  const edhrSignatureSource = readText('src/views/mes/pro/edhr/SignaturePage.vue')

  for (const [label, source, embeddedClass] of [
    ['DCC signature', dccSignatureSource, 'dcc-signature-page--embedded'],
    ['eDHR signature', edhrSignatureSource, 'edhr-signature-page--embedded']
  ]) {
    assert.match(source, /<component\s+:is="signaturePageShell"/, `${label} must use dynamic shell`)
    assert.match(source, /const signaturePageShell\s*=\s*computed/, `${label} shell must be computed from embedded mode`)
    assert.match(source, /\(?isEmbedded\.value\s*\?\s*'div'\s*:\s*ContentWrap\)?/, `${label} embedded shell must be a div`)
    assert.doesNotMatch(source, new RegExp(`<ContentWrap[^>]*${embeddedClass}`), `${label} must not bind embedded class to ContentWrap`)
  }
})

test('signature authorization state labels are localized', () => {
  const signatureEvidenceSource = readText('src/views/dcc/controlled-file/shared/signature-evidence.ts')

  for (const [value, label] of [
    ['UNAUTHORIZED', '未授权'],
    ['ENABLED', '已启用'],
    ['DISABLED', '已停用'],
    ['LOCKED', '已锁定']
  ]) {
    assert.match(signatureEvidenceSource, new RegExp(`label:\\s*'${label}'[\\s\\S]*value:\\s*'${value}'`))
  }

  assert.match(signatureEvidenceSource, /getDccSignatureAuthorizationStateLabel/)
  assert.doesNotMatch(signatureEvidenceSource, /label:\s*'已授权'[\s\S]*value:\s*'ENABLED'/)
})

test('signature governance workbench submits real review and CSV gate inputs', () => {
  const source = readGovernanceListPaneBundle()

  for (const label of [
    '统一策略',
    '创建',
    '复核计划',
    '审阅投影',
    '材料追溯',
    '培训变更',
    'QA批准',
    'DCC回执',
    'eDHR回执',
    '恢复演练',
    '操作'
  ]) {
    assert.match(source, new RegExp(label))
  }

  assert.match(source, /reviewForm\.projections/)
  assert.match(source, /buildReviewProjections/)
  assert.match(source, /validateReviewForm/)
  assert.doesNotMatch(source, /projections:\s*\[\]/)

  for (const field of [
    'materials',
    'traceRelations',
    'trainingRecords',
    'changeControls',
    'qaApproval'
  ]) {
    assert.match(source, new RegExp(`csvForm\\.${field}|${field}:\\s*build`, 's'))
  }
  assert.match(source, /buildCsvMaterials/)
  assert.match(source, /buildCsvTraceRelations/)
  assert.match(source, /buildCsvTrainingRecords/)
  assert.match(source, /buildCsvChangeControls/)
  assert.match(source, /buildCsvQaApproval/)
  assert.match(source, /validateCsvForm/)
  assert.doesNotMatch(source, /materials:\s*\[\]/)
  assert.doesNotMatch(source, /traceRelations:\s*\[\]/)
  assert.doesNotMatch(source, /trainingRecords:\s*\[\]/)
  assert.doesNotMatch(source, /changeControls:\s*\[\]/)
  assert.doesNotMatch(source, /qaApproval:\s*undefined/)
})

test('signature governance workbench auto-fills evidence fields from real signature records', () => {
  const pageSource = readGovernanceListPaneBundle()
  const apiSource = readText('src/api/dcc/controlledFile/signatures.ts')

  assert.match(pageSource, /getDccElectronicSignaturePage/)
  assert.match(pageSource, /DccElectronicSignatureVO/)
  assert.match(pageSource, /loadDccSignatureCandidates/)
  assert.match(pageSource, /applyDccSignatureCandidate/)
  assert.match(pageSource, /真实文件签名样本/)
  assert.match(pageSource, />\s*应用\s*</)
  assert.match(pageSource, /dcc_controlled_file_signature/)
  assert.match(pageSource, /sourceObjectKey/)
  assert.match(pageSource, /sourceVersionId/)
  assert.match(pageSource, /controlledCopyObjectKey/)
  assert.match(pageSource, /controlledCopyVersionId/)
  assert.match(pageSource, /当前签名记录缺少可回填的/)
  assert.match(pageSource, /存储对象Key/)
  assert.match(apiSource, /sourceObjectKey\?:\s*string/)
  assert.match(apiSource, /sourceVersionId\?:\s*string/)
  assert.match(apiSource, /controlledCopyObjectKey\?:\s*string/)
  assert.match(apiSource, /controlledCopyVersionId\?:\s*string/)
})

test('signature governance governance tabs render one standard list each', () => {
  const source = readText('src/views/signature-governance/index.vue')
  const governanceSource = readGovernanceListPaneBundle()

  assert.match(governanceSource, /displayValue/)
  assert.match(governanceSource, /等待来源/)
  assert.doesNotMatch(governanceSource, /\sdisabled(\s|>|\/|$)|:disabled="true"/)
  assert.doesNotMatch(source, /signature-governance__source-strip/)
  assert.doesNotMatch(source, /signature-governance__generated-grid/)
  assert.doesNotMatch(source, /signature-governance__readonly-grid/)
  assert.doesNotMatch(source, /signature-governance__readonly-item/)
  assert.doesNotMatch(source, /SignatureGovernanceBlockerList/)
  assert.doesNotMatch(source, /SignatureGovernancePolicyModuleList/)

  for (const [componentName, fileName] of [
    ['RetentionGovernanceListPane', 'RetentionGovernanceListPane.vue'],
    ['PeriodicReviewGovernanceListPane', 'PeriodicReviewGovernanceListPane.vue'],
    ['CsvPackageGovernanceListPane', 'CsvPackageGovernanceListPane.vue'],
    ['PolicyGovernanceListPane', 'PolicyGovernanceListPane.vue']
  ]) {
    const componentPath = `src/views/signature-governance/components/${fileName}`
    assert.equal(fs.existsSync(path.join(root, componentPath)), true, `${componentPath} must exist`)
    const componentSource = readText(componentPath)
    assert.match(source, new RegExp(`<${componentName}\\s+v-if=`), `${componentName} must be route mounted`)
    assert.equal((componentSource.match(/<UnifiedListTemplate\b/g) || []).length, 1, `${fileName} must render exactly one standard list`)
    assert.match(componentSource, /<template\s+#actions>/, `${fileName} must place global actions in list toolbar`)
    assert.match(componentSource, /label="操作"/, `${fileName} must keep row actions in action column`)
    assert.match(componentSource, /阻断项/, `${fileName} must merge blockers into the same list`)
    assert.doesNotMatch(componentSource, /signature-governance__source-strip/)
    assert.doesNotMatch(componentSource, /signature-governance__generated-grid/)
    assert.doesNotMatch(componentSource, /SignatureGovernanceBlockerList/)
  }

  for (const buttonText of [
    '刷新',
    '应用',
    '归档',
    '来源',
    '评估',
    '创建',
    '预检',
    '记录',
    '演练'
  ]) {
    assert.match(
      governanceSource,
      new RegExp(`>\\s*${buttonText}\\s*<|label:\\s*'${buttonText}'`),
      `${buttonText} button/action must be present`
    )
  }

  for (const loader of [
    'getFileConfigPage',
    'loadRetentionStorageConfig',
    'applyPolicyGeneratedDefaults',
    'loadEdhrArchiveCandidate',
    'loadCsvReleaseCandidate',
    'loadCsvValidationCandidate',
    'loadCsvTrainingCandidate',
    'loadCsvChangeCandidate',
    'loadCsvSourceCandidates',
    'loadCurrentPolicy'
  ]) {
    assert.match(governanceSource, new RegExp(loader), `${loader} must provide real source auto-fill`)
  }
})
