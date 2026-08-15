const assert = require('node:assert/strict')
const { spawnSync } = require('node:child_process')
const fs = require('node:fs')
const os = require('node:os')
const path = require('node:path')

const target = path.resolve(
  __dirname,
  'frontline-active-order-submit-allocation-real.e2e.js'
)

assert.equal(
  fs.existsSync(target),
  true,
  '必须提供一线活跃订单自动分配专用真实 E2E 脚本。'
)

const source = fs.readFileSync(target, 'utf8')
const contract = require(target)
const fixtureTarget = path.resolve(
  __dirname,
  '../../../doc/tasks/20260814-frontline-active-order-submit-allocation-docs/fas_fixture_orchestrator.py'
)
const fixtureSource = fs.readFileSync(fixtureTarget, 'utf8')

for (const requiredEnv of [
  'FAS_FRONTEND_URL',
  'FAS_BACKEND_URL',
  'FAS_ALLOWED_TEST_TENANT_IDS',
  'FAS_ALLOWED_TEST_TENANT_NAMES',
  'FAS_RUNTIME_EVIDENCE_FILE',
  'FAS_FIXTURE_MANIFEST',
  'FAS_ORCHESTRATOR_EXECUTABLE',
  'FAS_ORCHESTRATOR_SCRIPT',
  'FAS_FRONTLINE_USERNAME',
  'FAS_FRONTLINE_PASSWORD',
  'FAS_LEADER_USERNAME',
  'FAS_LEADER_PASSWORD',
  'FAS_SIGNATURE_PASSWORD'
]) {
  assert.match(source, new RegExp(requiredEnv), `真实 E2E 必须 fail fast 校验 ${requiredEnv}。`)
}

assert.match(source, /8099[\s\S]*48099/, '真实 E2E 必须允许当前 worktree 的 8099/48099 端口对。')
assert.match(source, /POST_MERGE_INT_MAIN[\s\S]*8081[\s\S]*48081/, '真实 E2E 必须允许融合后 int_main 的 8081/48081 端口对。')
assert.match(source, /ADMIN_TENANT1_INT_MAIN[\s\S]*8081[\s\S]*48081/, 'admin 补充验证必须通过独立运行模式绑定 int_main 8081/48081。')
assert.match(source, /FAS_EVIDENCE_RUN_ID/, 'admin 补充验证必须要求本轮独立证据标识，禁止并发流程共用证据目录。')
assert.match(source, /artifactDirFor/, '真实 E2E 必须通过统一 helper 生成按运行隔离的证据目录。')
assert.match(source, /RUNTIME_PROFILES/, '运行模式必须通过显式运行态 profile 选择，不得混用端口和工作区。')
assert.match(source, /validateRuntimeEvidence/, '真实 E2E 必须校验运行态归属和版本证据。')
assert.match(source, /sourceRevision[\s\S]*sourceFingerprintSha256/, '运行态证据必须绑定源码版本和工作树指纹。')
assert.match(source, /backendArtifactSha256/, '运行态证据必须核验实际后端产物哈希。')
assert.match(source, /Get-NetTCPConnection[\s\S]*Get-CimInstance/, '运行态证据必须核验监听 PID 和进程命令行归属。')
assert.match(source, /allowedTestTenantIds[\s\S]*allowedTestTenantNames/, '真实 E2E 必须使用显式测试租户 ID/名称白名单。')
assert.match(source, /FIXED_TEST_TENANT[\s\S]*id:\s*['"]122['"][\s\S]*name:\s*['"]测试租户['"]/, '真实 E2E 必须固定证明本机测试租户 122/测试租户。')
assert.match(source, /ADMIN_SUPPLEMENT_TENANT[\s\S]*id:\s*['"]1['"][\s\S]*name:\s*['"]芋道源码['"]/, 'admin 补充模式必须固定租户 1/芋道源码。')
assert.match(source, /fixtureMode[\s\S]*ADMIN_TENANT1/, 'admin 补充模式必须由 fixture mode 显式声明，不得从账号名猜测。')
assert.match(fixtureSource, /protectedBaselineFingerprint/, 'admin fixture 必须记录不泄密的受保护基线指纹。')
assert.match(fixtureSource, /protectedBaselineVerified/, 'verify/cleanup 必须证明 admin 受保护基线前后一致。')
assert.match(fixtureSource, /ADMIN_TENANT1/, 'fixture 编排器必须提供独立的 tenant 1 admin 模式。')
assert.match(source, /validateRuntimeEvidence\(config\)[\s\S]{0,500}verifyExternalFixture\(config\)[\s\S]{0,500}runScenario\(config\)/, '真实写入前必须依次完成运行态、fixture 与权限数据前置核验。')
assert.doesNotMatch(source, /FORBIDDEN_TENANTS/, '租户归属不得只靠生产/admin 黑名单证明。')
assert.match(source, /selectFrontlineActiveOrder/, '真实 E2E 必须通过页面明确选择 O1。')
assert.match(source, /loginResponseWait[\s\S]*auth\/login/, '真实登录必须等待并核验正式登录响应，不得只等 URL。')
assert.match(source, /waitForLoginFormShell[\s\S]*login-form[\s\S]*state:\s*['"]visible['"][\s\S]*selectLoginTenant/, '登录页必须先等待真实表单壳层可见，再读取租户下拉框，避免首屏 loading 竞争。')
assert.match(
  source,
  /page\.goto\(`\$\{frontendUrl\}\/login\?redirect=\/index`[\s\S]{0,120}waitUntil:\s*['"]domcontentloaded['"]/,
  '登录页必须等待 DOM 就绪，不得被持续轮询或外部资源阻塞在 networkidle。'
)
assert.match(source, /waitForURL\([\s\S]{0,240}waitUntil:\s*['"]commit['"]/, '登录成功后的 URL 门禁必须使用 commit，避免主应用长资源误报 load 超时。')
assert.match(
  source,
  /const LEADER_ROUTE = ['"]\/mes\/pro\/process-pool\/production-leader['"]/,
  '生产组长真实路径必须使用正式菜单路由，不得进入不存在的 team-leader 兼容路径。'
)
assert.match(source, /processPoolContext\.activeOrderId/, '真实 E2E 必须断言提交载荷精确携带 O1 activeOrderId。')
assert.match(source, /assertInitialAllocation/, '真实 E2E 必须在组长改配前验证 O1 全量初始分配。')
assert.match(
  source,
  /assert\.equal\(snapshot\.lines\[0\]\.allocationMode,\s*['"]FRONTLINE_SELECTED['"]/,
  '初始分配模式必须从版本 1 的正式分配行断言，不得要求接口不存在的顶层兼容字段。'
)
assert.match(source, /data-team-leader-report-overage/, '真实 E2E 必须验证组长列表红色待调整标识。')
assert.match(source, /reallocateToSecondOrder/, '真实 E2E 必须通过组长页面把部分数量改配到 O2。')
assert.match(source, /postSavePageResponse/, '组长改配后必须等待并核验正式报工管理列表刷新响应。')
assert.match(source, /function observeWait[\s\S]*then\([\s\S]*error/, '多个 Playwright 等待必须在创建时立即观测 reject，禁止未处理 promise 跳过 finally cleanup。')
assert.match(source, /postSavePageResponseWait\s*=\s*observeWait/, '改配后列表刷新等待必须使用可安全延后 await 的观测器。')
assert.match(source, /assertAdjustedResponse/, '组长改配后必须从正式列表响应核验 O1、O2 数量和待调整状态。')
assert.match(source, /data-team-leader-report-overage[^\n]*waitFor\(\{\s*state:\s*['"]hidden['"]/, '组长改配后必须等待红色待调整标识真正消失。')
assert.match(source, /submission\/allocation\/audit/, '真实 E2E 必须只读核验初始分配与改配审计。')
assert.match(source, /assertNoTargetErrors/, '真实 E2E 必须检查页面错误、控制台错误和目标接口失败。')
assert.match(source, /page\.on\(['"]requestfailed['"]/, '真实 E2E 必须记录网络失败请求的 URL 和失败原因。')
assert.match(source, /requestFailures/, '真实 E2E 证据必须保留网络失败请求，不能只记录无来源的控制台文本。')
assert.match(source, /targetRequestFailures/, '真实 E2E 必须单独阻断当前 MES 业务路径的网络失败请求。')
assert.match(source, /localStorage\.getItem\(['"]ACCESS_TOKEN['"]\)/, '最终只读审计必须精确读取 ACCESS_TOKEN。')
assert.doesNotMatch(
  source,
  /Object\.keys\(localStorage\)[\s\S]{0,240}includes\(['"]token['"]\)/i,
  '最终只读审计禁止模糊匹配任意 token key。'
)

assert.match(source, /class E2EBlockedError/, '真实 E2E 必须用专用错误类型区分 BLOCKED。')
for (const category of [
  'SERVICE_UNREACHABLE',
  'BROWSER_UNAVAILABLE',
  'LOGIN_PREREQUISITE',
  'PERMISSION_PREREQUISITE',
  'TASK_DATA_PREREQUISITE'
]) {
  assert.match(source, new RegExp(category), `真实 E2E 必须分类 ${category}。`)
}
assert.match(source, /error instanceof E2EBlockedError/, '只有正式前置错误才能被判为 BLOCKED。')
assert.match(source, /function statusForError\(error\)[\s\S]{0,160}instanceof E2EBlockedError[\s\S]{0,100}FAIL/, '业务断言错误必须保持 FAIL，只有 E2EBlockedError 才能是 BLOCKED。')
assert.doesNotMatch(source, /error\.blocked/, '禁止用任意 error.blocked 属性把业务断言误判为 BLOCKED。')
assert.doesNotMatch(source, /blockedPhase\([\s\S]{0,600}selectFrontlineActiveOrder/, '页面选单和普通 UI 断言不得被前置包装器降级为 BLOCKED。')

assert.match(source, /loadFixtureManifest/, '任务数据必须来自外部 fixture manifest 合同。')
assert.match(source, /verifyExternalFixture/, '写入前必须由外部编排验证真实 fixture。')
assert.match(source, /runExternalCleanup/, '异常和成功路径都必须调用外部清理编排。')
assert.match(source, /finally\s*\{[\s\S]{0,500}runExternalCleanup\(config\)/, '外部清理必须位于主执行路径的 finally 中。')
assert.match(source, /cleanupVerified[\s\S]*remainingTaskDataCount/, '清理结果必须包含机器可读核验和残留数量。')
assert.match(source, /status\s*===\s*['"]CLEAN['"]/, '只有外部清理返回 CLEAN 才能满足清理合同。')
assert.match(source, /status:\s*['"]PASS['"][\s\S]{0,500}cleanupResult/, 'PASS 证据必须携带已经核验的清理结果。')
assert.match(source, /isVerifiedCleanCleanup\(cleanupResult\)/, '写入 PASS 前必须再次验证 cleanup=CLEAN 且残留为 0。')
assert.doesNotMatch(source, /仍需执行已确认的数据清理方案/, '不得把尚待清理的数据路径写成 PASS。')
assert.doesNotMatch(source, /FAS_CLEANUP_CONFIRMED/, '声明已确认清理不能代替实际清理。')
assert.match(source, /collectCleanupConfig\(\)[\s\S]{0,600}collectConfig\(\)/, 'collectConfig 之前必须先收集独立清理身份。')
assert.match(source, /collectConfig\(\)[\s\S]{0,1200}attemptEarlyCleanup/, 'collectConfig 失败或缺项时必须进入提前清理路径。')
assert.match(source, /function attemptEarlyCleanup[\s\S]{0,600}runExternalCleanup/, '提前清理路径必须调用外部 cleanup。')

assert.match(source, /assertManifestContainsNoSecrets/, 'fixture manifest 必须递归拒绝敏感键。')
assert.match(source, /function redactEvidence/, '机器可读证据必须使用统一递归脱敏器。')
assert.match(source, /collectSensitiveValues/, '递归脱敏必须同时移除嵌套敏感值在普通字符串中的副本。')
assert.match(source, /writeRequestCount/, '结果必须记录实际目标业务写请求数。')

assert.doesNotMatch(source, /\.at\(-1\)/, 'Node >=16 合同不得依赖 Node 16.6 才提供的 Array.prototype.at。')
assert.doesNotMatch(source, /\bfetch\s*\(/, 'Node >=16 合同不得依赖 Node 18 才默认提供的全局 fetch。')

assert.match(source, /function positiveLong/, 'Java Long 类 ID 必须使用十进制字符串校验。')
assert.match(source, /BigInt\(/, 'Java Long 类 ID 必须通过 BigInt 规范化。')
assert.match(source, /function sameLongId/, 'Java Long 类 ID 比较必须使用字符串或 BigInt-safe helper。')
assert.match(source, /parseJsonPreservingLongIds/, '请求和响应 JSON 必须保留 Long ID 原始十进制值。')
assert.match(source, /parseJsonPreservingLongIds\(request\.postData\(\)/, '提交与改配请求断言必须用 Long-safe JSON 解析。')
assert.match(source, /readPlaywrightJson\(response/, 'Playwright 响应断言必须用 Long-safe JSON 解析。')
assert.doesNotMatch(source, /Number\([^\n)]*(?:Id|ID)[^\n)]*\)/, '禁止把 Java Long 类 ID 转成 Number。')

assert.equal(
  contract.statusForError(new contract.E2EBlockedError('SERVICE_UNREACHABLE', 'service down')),
  'BLOCKED',
  '已分类的真实前置错误必须得到 BLOCKED。'
)
assert.equal(
  contract.statusForError(new assert.AssertionError({ message: 'business assertion failed' })),
  'FAIL',
  '业务断言失败不得降级为 BLOCKED。'
)
assert.equal(
  contract.parseJsonPreservingLongIds('{"activeOrderId":9007199254740993}', 'Long ID 合同').activeOrderId,
  '9007199254740993',
  '超过 Number 安全范围的 Long ID 必须保持精确十进制字符串。'
)
assert.equal(
  contract.sameLongId('9007199254740993', '9007199254740993'),
  true,
  'Long ID 必须进行精确字符串/BigInt-safe 比较。'
)
assert.equal(
  contract.sameLongId('9007199254740993', '9007199254740994'),
  false,
  '相邻超大 Long ID 不得因 Number 精度丢失被判为相等。'
)
assert.deepEqual(
  contract.runtimeForMode('POST_MERGE_INT_MAIN'),
  {
    workspaceRoot: path.resolve(__dirname, '../../..'),
    frontendUrls: ['http://127.0.0.1:8081', 'http://localhost:8081'],
    backendUrl: 'http://127.0.0.1:48081',
    frontendPort: 8081,
    backendPort: 48081
  },
  '融合后模式必须精确绑定 E:\\IntRuoyi 的 8081/48081。'
)
assert.deepEqual(
  contract.runtimeForMode('ADMIN_TENANT1_INT_MAIN'),
  {
    workspaceRoot: path.resolve(__dirname, '../../..'),
    frontendUrls: ['http://127.0.0.1:8081', 'http://localhost:8081'],
    backendUrl: 'http://127.0.0.1:48081',
    frontendPort: 8081,
    backendPort: 48081
  },
  'admin 补充模式必须精确绑定 E:\\IntRuoyi 的 8081/48081。'
)
assert.deepEqual(
  contract.tenantForMode('ADMIN_TENANT1_INT_MAIN'),
  { id: '1', name: '芋道源码', fixtureMode: 'ADMIN_TENANT1' },
  'admin 补充模式必须精确绑定租户 1/芋道源码及 admin fixture 合同。'
)
assert.deepEqual(
  contract.tenantForMode('POST_MERGE_INT_MAIN'),
  { id: '122', name: '测试租户', fixtureMode: 'STANDARD_TENANT122' },
  '原 P6 融合后模式必须仍固定租户 122/测试租户。'
)
assert.equal(
  contract.artifactDirFor('ADMIN_TENANT1_INT_MAIN', 'p7-run-20260815'),
  path.resolve(
    __dirname,
    '../../../doc/tasks/20260814-frontline-active-order-submit-allocation-docs/e2e-artifacts/admin-tenant1-int-main/p7-run-20260815'
  ),
  'admin 补充模式必须把每轮证据写入独立子目录。'
)
assert.throws(
  () => contract.artifactDirFor('ADMIN_TENANT1_INT_MAIN', '../shared'),
  /FAS_EVIDENCE_RUN_ID/,
  '证据运行标识不得包含路径穿越或落入共享目录。'
)
assert.equal(contract.runtimeForMode('UNSUPPORTED'), undefined, '未知运行模式必须 fail fast，不得降级。')

assert.deepEqual(
  contract.classifyConsoleErrors(
    ['Failed to load resource: net::ERR_CONNECTION_TIMED_OUT'],
    [{ method: 'GET', url: 'https://api.unisvg.com/fa.json?icons=slack', errorText: 'net::ERR_CONNECTION_TIMED_OUT' }]
  ),
  {
    targetConsoleErrors: [],
    externalResourceConsoleErrors: ['Failed to load resource: net::ERR_CONNECTION_TIMED_OUT']
  },
  '只有与已记录第三方资源超时精确关联的通用浏览器报错可从目标业务错误中分离。'
)
assert.deepEqual(
  contract.classifyConsoleErrors(['Failed to load resource: net::ERR_CONNECTION_TIMED_OUT'], []),
  {
    targetConsoleErrors: ['Failed to load resource: net::ERR_CONNECTION_TIMED_OUT'],
    externalResourceConsoleErrors: []
  },
  '没有第三方失败证据时不得忽略通用资源超时。'
)
assert.deepEqual(
  contract.classifyConsoleErrors(['业务页面异常'], [{ method: 'GET', url: 'https://example.com/x', errorText: 'net::ERR_CONNECTION_TIMED_OUT' }]),
  { targetConsoleErrors: ['业务页面异常'], externalResourceConsoleErrors: [] },
  '业务控制台错误不得因同时存在第三方超时而被吞掉。'
)
assert.deepEqual(
  contract.classifyConsoleErrors(
    [
      'Failed to load resource: the server responded with a status of 502 (Bad Gateway)',
      'Failed to load resource: the server responded with a status of 502 (Bad Gateway)'
    ],
    [],
    [
      { method: 'GET', url: 'http://test.yudao.iocoder.cn/user/avatar/a.jpg', status: 502, statusText: 'Bad Gateway' },
      { method: 'GET', url: 'http://test.yudao.iocoder.cn/user/avatar/a.jpg', status: 502, statusText: 'Bad Gateway' }
    ]
  ),
  {
    targetConsoleErrors: [],
    externalResourceConsoleErrors: [
      'Failed to load resource: the server responded with a status of 502 (Bad Gateway)',
      'Failed to load resource: the server responded with a status of 502 (Bad Gateway)'
    ]
  },
  '只有与逐条记录的非本机 HTTP 错误响应一一对应时，通用 console 502 才可归类为外部资源错误。'
)
assert.deepEqual(
  contract.classifyConsoleErrors(
    ['Failed to load resource: the server responded with a status of 502 (Bad Gateway)'],
    [],
    [{ method: 'GET', url: 'http://127.0.0.1:8081/admin-api/mes/pro/x', status: 502, statusText: 'Bad Gateway' }]
  ),
  {
    targetConsoleErrors: ['Failed to load resource: the server responded with a status of 502 (Bad Gateway)'],
    externalResourceConsoleErrors: []
  },
  '本机业务响应错误必须保持目标错误，禁止被外部资源分类吞掉。'
)

assert.throws(
  () => contract.assertManifestContainsNoSecrets({ nested: [{ apiTokenValue: 'sensitive' }] }),
  (error) => error instanceof contract.E2EBlockedError,
  'manifest 必须递归拒绝数组内嵌套的敏感键。'
)
const recursivelyRedacted = contract.redactEvidence({
  config: {
    nested: [{ signaturePassword: 'sensitive-value' }],
    connectionCredentials: { value: 'isolated-container-value-927' },
    repeated: 'prefix-sensitive-value-suffix',
    repeatedContainerValue: 'prefix-isolated-container-value-927-suffix'
  }
})
assert.equal(recursivelyRedacted.config.nested[0].signaturePassword, '[REDACTED]')
assert.equal(recursivelyRedacted.config.connectionCredentials, '[REDACTED]')
assert.equal(recursivelyRedacted.config.repeated.includes('sensitive-value'), false, '普通字符串中的敏感值副本也必须脱敏。')
assert.equal(
  recursivelyRedacted.config.repeatedContainerValue.includes('isolated-container-value-927'),
  false,
  '敏感容器内的嵌套值副本也必须从普通字符串中脱敏。'
)
assert.equal(
  contract.isVerifiedCleanCleanup({
    status: 'CLEAN',
    cleanupPerformed: true,
    cleanupVerified: true,
    remainingTaskDataCount: 0
  }),
  true,
  '完整 CLEAN 结果才允许进入 PASS。'
)
assert.equal(
  contract.isVerifiedCleanCleanup({ status: 'CLEAN', cleanupVerified: true, remainingTaskDataCount: 1 }),
  false,
  '存在任务数据残留时不得进入 PASS。'
)

const artifactDir = path.resolve(__dirname, '../../../doc/tasks/20260814-frontline-active-order-submit-allocation-docs/e2e-artifacts/worktree')
const artifactPaths = ['result.json', 'evidence.md', 'scenario-state.json', 'cleanup-result.json']
  .map((name) => path.join(artifactDir, name))
const artifactSnapshots = new Map(artifactPaths.map((filePath) => [
  filePath,
  fs.existsSync(filePath) ? fs.readFileSync(filePath) : undefined
]))
const contractDir = fs.mkdtempSync(path.join(os.tmpdir(), 'fas-cleanup-contract-'))
try {
  const manifestPath = path.join(contractDir, 'fixture.json')
  const orchestratorPath = path.join(contractDir, 'orchestrator.cjs')
  const cleanupMarkerPath = path.join(contractDir, 'cleanup-called.txt')
  const runId = 'FAS-20260814-contract-cleanup'
  fs.writeFileSync(manifestPath, JSON.stringify({
    schemaVersion: 'fas-fixture-v1',
    fixtureMode: 'STANDARD_TENANT122',
    taskId: '20260814-frontline-active-order-submit-allocation-docs',
    runId,
    tenant: { id: '122', name: '测试租户' },
    accounts: { frontlineUsername: 'fasfrontline', leaderUsername: 'fasleader' },
    orders: {
      o1: { activeOrderId: '9007199254740993', workOrderId: '9007199254740994', workOrderCode: `${runId}-O1`, plannedQuantity: 1 },
      o2: { activeOrderId: '9007199254740995', workOrderId: '9007199254740996', workOrderCode: `${runId}-O2`, plannedQuantity: 10 }
    },
    context: {
      routeId: '9007199254740997',
      routeProcessId: '9007199254740998',
      processId: '9007199254740999',
      actualEmployeeId: '9007199254741000',
      submitQuantity: 2,
      feedbackCode: `${runId}-FB`
    },
    cleanupContract: 'fas-cleanup-v1'
  }), 'utf8')
  fs.writeFileSync(orchestratorPath, [
    "const fs = require('node:fs')",
    "const args = process.argv.slice(2)",
    "const action = args[0]",
    "const resultPath = args[args.indexOf('--result') + 1]",
    `const markerPath = ${JSON.stringify(cleanupMarkerPath)}`,
    "if (action !== 'cleanup') process.exit(9)",
    "fs.writeFileSync(markerPath, 'called', 'utf8')",
    `fs.writeFileSync(resultPath, JSON.stringify({ status: 'CLEAN', cleanupPerformed: true, cleanupVerified: true, remainingTaskDataCount: 0, taskId: '20260814-frontline-active-order-submit-allocation-docs', runId: ${JSON.stringify(runId)}, tenantId: '122' }), 'utf8')`
  ].join('\n'), 'utf8')
  const childEnv = {
    ...process.env,
    FAS_FRONTEND_URL: 'http://127.0.0.1:8099',
    FAS_BACKEND_URL: 'http://127.0.0.1:48099',
    FAS_ALLOWED_TEST_TENANT_IDS: 'invalid-long',
    FAS_ALLOWED_TEST_TENANT_NAMES: '测试租户',
    FAS_RUNTIME_EVIDENCE_FILE: path.join(contractDir, 'runtime.json'),
    FAS_FIXTURE_MANIFEST: manifestPath,
    FAS_ORCHESTRATOR_EXECUTABLE: process.execPath,
    FAS_ORCHESTRATOR_SCRIPT: orchestratorPath,
    FAS_FRONTLINE_USERNAME: 'fasfrontline',
    FAS_FRONTLINE_PASSWORD: 'frontline-secret',
    FAS_LEADER_USERNAME: 'fasleader',
    FAS_LEADER_PASSWORD: 'leader-secret',
    FAS_SIGNATURE_PASSWORD: 'signature-secret'
  }
  const child = spawnSync(process.execPath, [target], {
    cwd: path.dirname(path.dirname(__dirname)),
    env: childEnv,
    encoding: 'utf8'
  })
  assert.equal(child.status, 2, `配置失败合同应以 BLOCKED 退出：${child.stderr || child.stdout}`)
  assert.equal(
    fs.existsSync(cleanupMarkerPath),
    true,
    `collectConfig 失败后仍必须调用外部 cleanup：${child.stderr || child.stdout}`
  )
  const resultPath = path.join(artifactDir, 'result.json')
  const childResult = JSON.parse(fs.readFileSync(resultPath, 'utf8'))
  assert.equal(childResult.cleanupResult.status, 'CLEAN')
  assert.equal(childResult.cleanupResult.remainingTaskDataCount, 0)
  assert.equal(childResult.writeRequestCount, 0)
  assert.equal(JSON.stringify(childResult).includes('frontline-secret'), false)
  assert.equal(JSON.stringify(childResult).includes('leader-secret'), false)
  assert.equal(JSON.stringify(childResult).includes('signature-secret'), false)
} finally {
  fs.rmSync(contractDir, { recursive: true, force: true })
  for (const [filePath, snapshot] of artifactSnapshots.entries()) {
    if (snapshot === undefined) {
      fs.rmSync(filePath, { force: true })
    } else {
      fs.writeFileSync(filePath, snapshot)
    }
  }
}

console.log('PASS: active-order submit allocation real E2E contract')
