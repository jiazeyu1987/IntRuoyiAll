# Execution Log

## User Intent

- 用户要求修复 `芋道源码/admin` 登录一线生产填写页时，点击工序、员工后候选列表空白的问题。

## Milestone Updates

- M1 完成：截图文件时间为 2026-08-07 14:21:37；运行日志显示当时仅请求模板目录，尚未请求 `frontline/device-account/processes`。源码确认生产初始化先等待模板目录，点击卡片只打开基于现有数组的弹框。

## BDD / TDD Evidence

- BDD: 初始化期间打开工序选择 -> Given 生产填写页刚挂载且模板目录请求尚未完成 / When 用户点击工序 / Then 工序正式请求已经独立启动，弹框显示加载状态并在响应后展示候选。
- BDD: 工序未就绪时打开员工选择 -> Given 工序仍在加载或尚未选定 / When 用户点击员工 / Then 弹框显示工序加载或请先选择工序，不呈现无说明空白列表。
- BDD: 正式请求失败 -> Given 工序或运行配置接口返回错误 / When 选择弹框处于打开状态 / Then 页面展示正式错误信息，异常不被吞掉且不生成默认候选。
- RED: `node tests/e2e/frontline-production-picker-initial-loading-static.spec.cjs` -> FAIL, 旧生产选择弹框缺少 `pickerStatusText` 状态节点，且初始化仍串行等待模板目录。
- GREEN: `node tests/e2e/frontline-production-picker-initial-loading-static.spec.cjs` -> PASS，生产初始化通过 `Promise.all` 并发模板目录与正式工序上下文，picker 状态覆盖 loading/prerequisite/empty/error。

## Command Intent

- 计划使用任务专用 Node 静态合同锁定并行初始化、加载状态、错误状态和正式 API 数据源。
- 已运行 RED 合同，退出码 1，首个预期失败为选择弹框缺少加载/前置/空数据/错误状态。
- 实现 `initializeProductionSelection`，生产模式不再等待模板目录后才加载工序；PQC 初始化链路保持原顺序。
- 新增 `pickerStatusText` 与可访问状态节点，继续使用正式候选数组并直接暴露 `lastError`。

## Blockers

- 非任务阻塞：`node tests/e2e/role-matrix-ac-m10-sop-production-static.spec.cjs` 在读取业务断言前失败，原因是历史合同仍用不存在的 `onBeforeUnmount` 作为结束锚点；当前组件在本任务前已使用 `onUnmounted`。按前端静态契约隔离门禁，不修改该大合同，目标专用合同继续作为本任务 RED/GREEN 证据。
- 非任务阻塞：`node src/views/mes/pro/feedback/frontline-template-render.spec.cjs` 失败于既有 `is-no-device` 布局断言；本任务未修改该布局，未通过删除本次功能或放宽断言绕过。
- 真实登录 E2E 阻塞：本机 Chrome 无 remote debugging，Playwright/IAB 没有可复用登录态；当前后端访问日志会记录登录请求中的明文凭据，因此不发起新的自动登录请求。以目标静态合同、相邻合同、`pnpm ts:check` 和 Vite 实时编译作为本轮安全验证，并明确不宣称真实 E2E PASS。

## Regression Results

- PASS：`node tests/e2e/frontline-production-picker-initial-loading-static.spec.cjs`。
- PASS：`node tests/e2e/edhr-frontline-fill-tabs-static.spec.cjs`。
- PASS：`node tests/e2e/edhr-frontline-production-employee-options-match-leader-personnel-static.spec.cjs`。
- PASS：`node tests/e2e/frontline-team-config-static.spec.cjs`。
- PASS：`node src/views/mes/pro/feedback/frontline-template-switch.spec.cjs`。
- PASS：`node tests/e2e/edhr-frontline-production-prototype-parity-static.spec.cjs`。
- PASS：`node tests/e2e/edhr-frontline-production-fullscreen-toggle-static.spec.cjs`。
- PASS：`pnpm ts:check`。
- PASS：Vite 转换 `FrontlineFixedTemplatePanel.vue` 返回 HTTP 200、`text/javascript`。
- PASS：任务涉及的已跟踪组件 `git diff --check`。
- PASS：任务新文件可按 UTF-8 读取，无尾随空白且以换行结尾。

## Experience Consolidation

- `project-experience-consolidation` 已将“候选请求不得被无关目录请求串行阻塞、picker 必须区分 loading/prerequisite/empty/error/ready”合并到既有 `docs/frontend-development.md#前端选择弹框即时反馈门禁`。
- `docs/experience-index.md` 已补充对应检索关键词；未新建长期经验文档。

## Closeout

- PASS：task-closeout-cleanup preview，keep 为 `task.md`、`execution-log.md`、`verification-report.md`，delete 为两份已归档的临时技能 evidence，blocked/warnings 均为空。
- PASS：task-closeout-cleanup apply，按 preview 删除 `bug-regression-evidence.md` 和 `frontend-feature-evidence.md`，保留三份核心任务记录；当前不是 linked worktree，无合并或 worktree 删除动作。
- Git：用户未要求，本任务未执行 stage、commit、merge 或 push。
