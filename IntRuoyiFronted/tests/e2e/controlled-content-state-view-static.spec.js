const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')

const readSource = (relativePath) => {
  const absolutePath = path.join(repoRoot, relativePath)
  assert.equal(fs.existsSync(absolutePath), true, `missing required file: ${relativePath}`)
  return fs.readFileSync(absolutePath, 'utf8')
}

const stateStrip = readSource('src/components/ControlledContent/ControlledContentStateStrip.vue')
const routeIndex = readSource('src/views/mes/pro/route/index.vue')
const routeForm = readSource('src/views/mes/pro/route/RouteFormContent.vue')
const dccDetail = readSource('src/views/dcc/controlled-file/detail/index.vue')
const packageJson = JSON.parse(readSource('package.json'))
const dccReadonlyE2e = readSource('tests/e2e/controlled-content-state-view-real-readonly.e2e.js')

assert.equal(
  packageJson.scripts['e2e:controlled-content:state-view:real'],
  'node tests/e2e/controlled-content-state-view-real-readonly.e2e.js',
  'package.json 必须暴露受控内容状态真实只读 E2E 脚本入口。'
)

assert.match(
  stateStrip,
  /data-testid="controlled-content-state-strip"|:data-testid="testId/,
  '受控内容状态条必须提供稳定测试标识。'
)
assert.match(stateStrip, /defineProps/, '受控内容状态条必须通过 props 接收状态，不得自取数据。')
assert.match(
  stateStrip,
  /import\s+\{\s*computed\s*\}\s+from\s+'vue'/,
  '受控内容状态条必须显式导入 computed，避免公共组件依赖隐式注入。'
)
assert.match(stateStrip, /candidateCount/, '受控内容状态条必须支持打开候选数量提示。')
assert.match(stateStrip, /blockers/, '受控内容状态条必须支持发布阻断项展示。')
assert.match(
  stateStrip,
  /<slot\s+name="actions"><\/slot>/,
  '受控内容状态条动作插槽必须符合项目 eslint 规则，不得使用 HTML 元素自闭合写法。'
)
assert.doesNotMatch(
  stateStrip,
  /@\/config\/axios|request\.|fetch\(/,
  '受控内容状态条必须是展示组件，不得引入请求或重复状态机。'
)
assert.doesNotMatch(
  stateStrip,
  /is-readonly|is-editable/,
  '受控内容状态条不得保留没有样式或消费方的状态类，状态语义应由标签和 props 承载。'
)

assert.match(
  routeForm,
  /ControlledContentStateStrip/,
  '工艺路线编辑页必须复用受控内容状态条。'
)
assert.match(
  routeForm,
  /test-id="route-version-workflow-status"/,
  '工艺路线编辑页必须保留版本工作流状态测试标识。'
)
assert.match(
  routeForm,
  /isDraftCandidateVersion[\s\S]*lifecycleStatus === 'DRAFT'/,
  '工艺路线编辑页只能允许 DRAFT 候选版本进入编辑态。'
)
assert.match(
  routeForm,
  /data-route-version-action="edit-production-config"/,
  '生效版本查看态必须显示创建候选编辑入口。'
)
assert.match(
  routeForm,
  /data-route-version-action="submit-route-candidate"/,
  '草稿候选版本必须显示提交发布入口。'
)
assert.match(
  routeForm,
  /当前候选版本已离开草稿状态，仅允许查看/,
  '审核中或待发布候选版本必须明确只读。'
)

assert.match(
  routeIndex,
  /ControlledContentStateStrip/,
  '工艺路线版本工作区必须复用受控内容状态条。'
)
assert.match(
  routeIndex,
  /test-id="mes-route-version-workspace-state-strip"/,
  '工艺路线版本工作区必须提供稳定状态条测试标识。'
)
assert.match(
  routeIndex,
  /routeVersionOpenCandidateCount/,
  '工艺路线版本工作区必须统计打开中的候选版本。'
)
assert.match(
  routeIndex,
  /routeVersionWorkspaceHint/,
  '工艺路线版本工作区必须给出单一候选处理提示。'
)
assert.doesNotMatch(
  routeIndex,
  /多个草稿候选版本/,
  '单一候选迁移后，前端文案不得再表达“多个草稿”是允许状态。'
)
assert.match(
  routeIndex,
  /打开中的候选版本/,
  '出现历史冲突时，前端必须提示保留一个打开候选并关闭其余版本。'
)
assert.match(
  routeIndex,
  /canWithdrawRouteVersion[\s\S]*PENDING_APPROVAL/,
  '审核中的工艺路线候选只能通过撤回回到可编辑流程。'
)

assert.match(
  dccDetail,
  /ControlledContentStateStrip/,
  'DCC 文件详情页必须复用受控内容状态条。'
)
assert.match(
  dccDetail,
  /test-id="dcc-detail-controlled-content-state"/,
  'DCC 文件详情页必须提供稳定状态条测试标识。'
)
assert.match(
  dccDetail,
  /detail-controlled-content-state--viewer/,
  'DCC 浏览器 viewer 侧栏也必须复用统一状态条，保证真实浏览入口可见。'
)
assert.match(
  dccDetail,
  /dccControlledContentHint/,
  'DCC 文件详情页必须用统一提示解释当前版本是否可编辑。'
)
assert.match(
  dccDetail,
  /审核\/发布处理中不可编辑|撤回后再编辑/,
  'DCC 审核中版本必须提示先撤回后再编辑。'
)
assert.match(
  dccDetail,
  /FINALIZATION_FAILED|canRetryFinalization/,
  'DCC 发布失败必须保留重试发布的阻断恢复入口。'
)
assert.match(dccDetail, /canWithdraw/, 'DCC 审核中版本必须保留撤回入口。')
assert.match(dccDetail, /canHandleWithdrawnFlow/, 'DCC 已撤回版本必须保留重新提交入口。')

assert.match(
  dccReadonlyE2e,
  /TENANT.*测试租户[\s\S]*USERNAME.*aoteman/,
  '受控内容状态真实 E2E 必须默认使用测试租户/aoteman。'
)
assert.match(
  dccReadonlyE2e,
  /\/dcc\/controlled-file\/browser\?scope=global/,
  '受控内容状态真实 E2E 必须从 DCC 浏览页全域真实列表进入详情。'
)
assert.match(
  dccReadonlyE2e,
  /data-testid="dcc-browser-row-traceability"/,
  '受控内容状态真实 E2E 必须通过受控浏览操作列追溯入口进入详情页。'
)
assert.match(
  dccReadonlyE2e,
  /isExactControlledFileDetailResponse/,
  '受控内容状态真实 E2E 必须精确匹配 /dcc/controlled-files/{id} 主详情接口，不得把 access-explanation 等附属接口当成详情。'
)
assert.match(
  dccReadonlyE2e,
  /data-testid="dcc-detail-controlled-content-state"/,
  '受控内容状态真实 E2E 必须断言 DCC 详情页统一状态条可见。'
)
assert.match(
  dccReadonlyE2e,
  /writeRequests[\s\S]*admin-api\/dcc/,
  '受控内容状态真实 E2E 必须监控并禁止 DCC 写请求。'
)

console.log('PASS: controlled content state view static contract')
