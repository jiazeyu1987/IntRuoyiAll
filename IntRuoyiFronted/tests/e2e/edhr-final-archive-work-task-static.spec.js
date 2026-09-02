const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const readSource = (relativePath) =>
  fs.readFileSync(path.join(repoRoot, relativePath), 'utf8').replace(/\r\n/g, '\n')

const workTaskApi = readSource('src/api/mes/pro/edhr/workTask.ts')
const batchApi = readSource('src/api/mes/pro/edhr/batchExecution.ts')
const workTaskPage = readSource('src/views/mes/pro/edhr-work-task/WorkTaskBoardPage.vue')
const batchListPage = readSource('src/views/mes/pro/edhr-batch/BatchExecutionListPage.vue')
const batchDetailPage = readSource('src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue')
const realFlow = readSource('tests/e2e/edhr-final-archive-work-task-real-flow.e2e.js')
const simulationBootstrap = readSource('tests/e2e/edhr-pdfa-simulation-bootstrap-real-flow.e2e.js')

function extractConstFunction(source, name) {
  const start = source.indexOf(`const ${name} =`)
  assert.notEqual(start, -1, `missing function: ${name}`)
  const next = source.indexOf('\n\nconst ', start + 1)
  return source.slice(start, next === -1 ? source.length : next)
}

assert.match(workTaskApi, /EDHR_WORK_TASK_TYPE_ARCHIVE\s*=\s*'ARCHIVE'/, '工作任务 API 必须导出 ARCHIVE 任务类型。')
assert.match(workTaskApi, /archiveCount:\s*number/, '工作任务统计必须包含待归档数量。')
assert.match(workTaskApi, /EdhrWorkTaskArchiveRuleReqVO/, '工作任务 API 必须声明最终归档责任规则请求类型。')
assert.match(workTaskApi, /EdhrWorkTaskCloseRuleReqVO/, '工作任务 API 必须声明批次关闭责任规则请求类型。')
assert.match(workTaskApi, /EdhrWorkTaskAssignmentRuleRespVO/, '工作任务 API 必须声明最终归档责任规则响应类型。')
assert.match(workTaskApi, /getEdhrRouteArchiveRule/, '工作任务 API 必须提供按路线查询归档责任规则的方法。')
assert.match(workTaskApi, /saveEdhrRouteArchiveRule/, '工作任务 API 必须提供保存归档责任规则的方法。')
assert.match(workTaskApi, /getEdhrRouteCloseRule/, '工作任务 API 必须提供按路线查询关闭责任规则的方法。')
assert.match(workTaskApi, /saveEdhrRouteCloseRule/, '工作任务 API 必须提供保存关闭责任规则的方法。')
assert.match(workTaskApi, /\/mes\/pro\/edhr-work-task\/route-archive-rule/, '归档责任规则 API 必须使用正式后端 route-archive-rule 接口。')
assert.match(workTaskApi, /\/mes\/pro\/edhr-work-task\/route-close-rule/, '关闭责任规则 API 必须使用正式后端 route-close-rule 接口。')
assert.match(workTaskPage, /EDHR_WORK_TASK_TYPE_ARCHIVE/, '工作任务看板必须导入 ARCHIVE 任务类型。')
assert.match(workTaskPage, /待归档/, '工作任务看板必须展示待归档统计或筛选文案。')
assert.match(workTaskPage, /最终归档/, '工作任务看板任务类型筛选必须包含最终归档。')
assert.match(workTaskPage, /archiveCount/, '工作任务看板必须渲染 archiveCount。')
assert.match(workTaskPage, /mes:pro-edhr-work-task-rule:update/, '工作任务看板必须按权限展示归档责任规则维护入口。')
assert.match(workTaskPage, /getEdhrRouteArchiveRule/, '工作任务看板必须能加载路线级归档责任规则。')
assert.match(workTaskPage, /saveEdhrRouteArchiveRule/, '工作任务看板必须能保存路线级归档责任规则。')
assert.match(workTaskPage, /getEdhrRouteCloseRule/, '工作任务看板必须能加载路线级关闭责任规则。')
assert.match(workTaskPage, /saveEdhrRouteCloseRule/, '工作任务看板必须能保存路线级关闭责任规则。')
assert.match(workTaskPage, /ProRouteApi\.getRouteSimpleList/, '归档责任规则维护必须使用正式工艺路线列表。')
assert.match(workTaskPage, /getSimpleUserList/, '归档责任规则维护必须使用正式用户精简列表。')
assert.match(workTaskPage, /关闭规则/, '工作任务看板必须提供关闭规则维护对话框或按钮。')
assert.match(workTaskPage, /归档规则/, '工作任务看板必须提供归档规则维护对话框或按钮。')
assert.doesNotMatch(workTaskPage, /mock|fixture|demo|defaultSuccess/i, '归档规则维护不得使用 mock、fixture、demo 或默认成功路径。')

const managerReleaseTaskContext = extractConstFunction(workTaskPage, 'hasManagerReleaseTaskContext')
assert.doesNotMatch(
  managerReleaseTaskContext,
  /row\.version|Number\.isInteger/,
  '管理者代表最终放行按钮不得依赖列表行 version；正式版本必须从权威放行回执读取。'
)
const jsonLongIdParser = extractConstFunction(workTaskPage, 'requireJsonLongId')
assert.match(
  jsonLongIdParser,
  /typeof value === 'number'[\s\S]*Number\.isSafeInteger\(value\)[\s\S]*String\(value\)/,
  '管理者代表最终放行必须兼容后端 JSON Long 返回的数字型 ID。'
)
assert.match(
  workTaskPage,
  /managerReleaseReceipt\?\.version\s*\?\?\s*managerReleaseTask\.version/,
  '管理者代表最终放行版本展示必须保留 0 版本，不能用 || 渲染成空。'
)
assert.match(
  workTaskPage,
  /const expectedVersion = requireNonNegativeVersion\(currentReceipt\.version,\s*'最终放行权威版本'\)/,
  '管理者代表最终放行提交必须允许权威回执 0 版本作为 expectedVersion。'
)
assert.match(
  workTaskPage,
  /requireNonNegativeVersion\(receipt\.version,\s*'最终放行回执版本'\)/,
  '管理者代表最终放行回执校验必须允许首次事务 0 版本。'
)

assert.match(batchApi, /workTaskId:\s*number/, '批次归档生成 API 类型必须强制 workTaskId。')
assert.match(batchDetailPage, /archiveWorkTaskId/, '批次详情页必须解析归档待办 workTaskId。')
assert.match(batchDetailPage, /route\.query\.workTaskId/, '批次详情页必须从路由查询读取 workTaskId。')
assert.match(batchDetailPage, /assertArchiveWorkTaskId/, '批次详情页必须断言归档待办 workTaskId。')
assert.match(batchDetailPage, /workTaskId:\s*assertArchiveWorkTaskId\(\)/, '生成最终归档请求必须携带归档待办 workTaskId。')
assert.match(batchDetailPage, /canGenerateArchive[\s\S]*archiveWorkTaskId\.value/, '生成最终归档按钮必须受归档待办上下文约束。')
assert.match(
  batchDetailPage,
  /releaseActionLocked[\s\S]*releaseStatus\.value\s*!==\s*'RELEASED'/,
  '已放行批次不能继续按放行审批锁定，否则归档打印入口会被错误屏蔽。'
)
assert.match(
  batchDetailPage,
  /hasBatchLevelWorkTaskRouteContext[\s\S]*selectReleaseProcess\(\)/,
  '批次级 ARCHIVE workTaskId 入口必须默认打开放行/归档阶段，不能落到普通工序卡。'
)
assert.doesNotMatch(
  batchDetailPage,
  /hasBatchLevelWorkTaskRouteContext[\s\S]{0,180}viewedReleaseStageKey\.value\s*=\s*focus\s*===\s*'precheck'\s*\?\s*'precheck'\s*:\s*'release-approval'/,
  '批次级 ARCHIVE workTaskId 入口不能被默认覆盖成 release-approval，应使用实际 RELEASED/ARCHIVE 阶段。'
)
assert.match(batchListPage, /下载打印版 PDF/, '批次列表页下载入口必须明确为打印版 PDF。')
assert.match(batchDetailPage, /下载打印版 PDF/, '批次详情页下载入口必须明确为打印版 PDF。')
assert.match(batchListPage, /打印版 PDF 下载已开始/, '批次列表页下载提示必须明确为打印版 PDF。')
assert.match(batchDetailPage, /打印版 PDF 下载已开始/, '批次详情页下载提示必须明确为打印版 PDF。')
assert.match(batchDetailPage, /打印版 PDF 窗口已打开/, '批次详情页打印提示必须明确为打印版 PDF。')
assert.match(
  batchDetailPage,
  /:disabled="isViewedReleaseStageReadonly \|\| archiveGenerationLoading \|\| !latestBatchArchive\?\.id"[\s\S]*下载打印版 PDF/,
  '没有已生成归档时，下载打印版 PDF 必须禁用。'
)
assert.match(
  batchDetailPage,
  /:disabled="isViewedReleaseStageReadonly \|\| archiveGenerationLoading \|\| !latestBatchArchive\?\.id"[\s\S]*@click="handlePrintArchive"/,
  '没有已生成归档时，打印入口必须禁用。'
)
assert.doesNotMatch(batchListPage, /generateEdhrBatchArchive|handleGenerateArchive|生成归档/, '批次列表页不得保留无归档待办上下文的生成归档入口。')
assert.doesNotMatch(batchDetailPage, /workTaskId:\s*Number\([^)]*\)\s*\|\|\s*undefined/, '批次详情页不得用 undefined 绕过 workTaskId 门禁。')
assert.doesNotMatch(batchDetailPage, /mock|fixture|demo|defaultSuccess/i, '归档待办前端不得使用 mock、fixture、demo 或默认成功路径。')

assert.match(realFlow, /discoverArchiveWorkTaskFromBoard/, '真实 E2E 必须能从工作任务看板发现真实 ARCHIVE/TODO 待办。')
assert.match(realFlow, /hasExplicitArchiveTarget/, '真实 E2E 必须区分显式目标和 UI 自动发现目标。')
assert.match(realFlow, /EDHR_ARCHIVE_TASK_E2E_TASK_ID/, '真实 E2E 必须允许把证据写入当前任务目录。')
assert.match(realFlow, /findVisibleTableRowByTexts/, '真实 E2E 必须按可见表格行定位最终归档待办，避免命中隐藏下拉选项。')
assert.match(realFlow, /selectArchiveTaskTypeFilter\(page,\s*config\.batchCode\)/, '显式目标 E2E 必须先按最终归档和批次号筛选工作任务看板。')
assert.match(realFlow, /测试租户工作任务看板没有可见的真实 ARCHIVE\/TODO 待办/, '缺少真实归档待办时必须明确 BLOCKED。')
assert.doesNotMatch(realFlow, /必须提供测试租户内真实 ARCHIVE\/TODO 工作任务 ID。/, '真实 E2E 不应强制手工注入 workTaskId。')
assert.match(
  realFlow,
  /const generateArchiveButton = archiveDrawer\.getByRole\('button', \{ name: \/\^生成归档\$\/ \}\)\.first\(\)[\s\S]*await generateArchiveButton\.isDisabled\(\)/,
  '真实 E2E 必须先确认抽屉内生成归档按钮可点击。'
)
assert.match(
  realFlow,
  /await generateArchiveButton\.isDisabled\(\)[\s\S]*const archiveResponsePromise = page\.waitForResponse/,
  '真实 E2E 必须在确认生成按钮可点击后才等待归档请求，避免关闭浏览器后留下未处理等待。'
)
assert.match(
  simulationBootstrap,
  /EDHR_ARCHIVE_TASK_E2E_BASE_URL/,
  'PDF\/A 模拟补齐脚本必须允许显式指定独立 worktree 前端入口。'
)
assert.match(
  simulationBootstrap,
  /EDHR_ARCHIVE_TASK_E2E_TENANT/,
  'PDF\/A 模拟补齐脚本必须允许显式指定已授权测试租户。'
)
assert.match(
  simulationBootstrap,
  /EDHR_ARCHIVE_TASK_E2E_ARCHIVER_USERNAME/,
  'PDF\/A 模拟补齐脚本必须允许显式指定归档责任人账号。'
)
assert.match(
  simulationBootstrap,
  /EDHR_ARCHIVE_TASK_E2E_MANAGER_USERNAME/,
  'PDF\/A 模拟补齐脚本必须允许显式指定管理者代表账号。'
)
assert.match(
  simulationBootstrap,
  /EDHR_ARCHIVE_TASK_E2E_SOURCE_BATCH_EXECUTION_ID/,
  'PDF\/A 模拟补齐脚本必须允许显式指定详情页入口批次编号。'
)
assert.match(
  simulationBootstrap,
  /async function switchLoggedInUser/,
  'PDF\/A 模拟补齐脚本必须提供真实账号切换，不得用单账号冒充职责链路。'
)
assert.match(
  simulationBootstrap,
  /await switchLoggedInUser\(page,\s*context,\s*MANAGER_USERNAME,/,
  'Stage5 管理者代表放行前必须切换到管理者代表账号。'
)
assert.match(
  simulationBootstrap,
  /await switchLoggedInUser\(page,\s*context,\s*USERNAME,/,
  '管理者代表放行后必须切回归档责任人查询最终归档待办。'
)
assert.match(
  simulationBootstrap,
  /STAGE5_SIMULATION_SIGNOFF_STORAGE_KEY[\s\S]*preservedLocalStorageItems/,
  '账号切换时必须保留 Stage5 管理者签核证据，不能清掉正式放行所需上下文。'
)
assert.match(
  simulationBootstrap,
  /\.el-form-item:has-text\("签核证据"\) input[\s\S]*signoffEvidenceHash\.trim\(\)/,
  'Stage5 管理者代表放行必须显式填写签核证据，不能只依赖弹窗自动回填。'
)
assert.doesNotMatch(
  realFlow,
  /config\.baseUrl\s*!==\s*REQUIRED_BASE_URL/,
  '最终归档真实 E2E 不得拒绝端口登记表分配的独立 worktree 入口。'
)
assert.doesNotMatch(
  realFlow,
  /config\.tenant\s*!==\s*DEFAULT_TENANT/,
  '最终归档真实 E2E 不得拒绝用户当轮明确授权的本机测试租户。'
)

console.log('PASS: eDHR final archive work task static contract')
