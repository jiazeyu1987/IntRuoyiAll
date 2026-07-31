# 20260725 jiluben 融合后真实 E2E 验证

## Task Goal

在 `jiluben_20260722_clean` 残留融合已推送到 `int_main` 后，使用本机 `int_main` 前端 `8081` 和后端 `48081` 运行真实用户路径 E2E 验证，复现并修复验证过程中发现的问题，确保记录本/eDHR/批记录相关路径可用。

## Milestones

- [x] M1: 读取 E2E、登录、本地运行态、worktree、端口、PowerShell、任务收尾规则和 Playwright/bug fix 技能。
- [x] M2: 确认本机前后端运行态、登录入口和可执行 E2E 脚本前置条件。
- [x] M3: 运行融合后真实路径 E2E，记录 RED 失败证据。
- [x] M4: 对 E2E 失败做最小正式修复，补充或更新回归测试。
- [x] M5: 复跑 E2E/静态/类型/端口守卫，完成 cleanup 前验证。

## Expected Verification

- `http://127.0.0.1:48081/actuator/health` 返回 `status=UP`，且 `48081` 使用本任务临时 JVM 覆盖连接 Docker MySQL `23306` 和 Redis `26379`，未改配置文件。
- `http://127.0.0.1:8081/login?redirect=/index` 可访问，真实 E2E 使用 `http://localhost:8081` 前端入口。
- 覆盖融合范围的真实 Playwright E2E：`edhr-batch-process-companion-forms-real.e2e.js` 管理员只读模式通过，覆盖批次详情、右侧工序表单、历史执行 tracking 和返回批次详情。
- 产品缺陷已按 RED/GREEN 修复：填写页 toolbar 不再只在 tracking 模式显示；回归合同 `edhr-open-process-form-route-static.spec.js` 覆盖。
- `scripts\preflight\branch-runtime-port-guard.ps1` 通过，`git diff --check` 无空白错误，`pnpm ts:check` 通过。

## BDD Scenarios

- BDD: 融合后 eDHR 详情真实页面可访问 -> Given `int_main` 前后端在 8081/48081 运行, When 用户进入 eDHR 批次执行详情, Then 页面应加载真实接口数据且不出现融合后的前端运行时错误。
- BDD: 历史执行只读追踪返回上下文 -> Given 已存在批次执行和执行记录, When 管理员只读账号从批次详情进入执行记录 tracking 路径, Then 页面显示追踪详情并能带 `batchTaskId` 返回批次详情。
- BDD: 填写页 toolbar 不受 tracking 模式限制 -> Given 用户通过正式打开工序逻辑进入填写页, When `isTrackingReadonlyMode=false`, Then 页面仍显示标题和返回按钮，不被 tracking 条件隐藏。
- BDD: 验证失败最小正式修复 -> Given 真实 E2E 或静态合同暴露融合后缺陷, When 修复代码或同步过期测试契约, Then 必须有 RED/GREEN 证据证明缺陷闭合且无 fallback/降级。

## Experience Gates

- 本地后端数据库凭据门禁：若 `48081` 未监听或日志出现 `dynamic-datasource create datasource named [master] error` / `Access denied for user 'root'@'localhost'`，停止后端成功结论，不改端口、不切换数据源，修复前置后再验证 health。
- 任务专用 E2E 环境变量门禁：写入型 E2E 未设置任务专用测试数据时不运行；本任务采用管理员只读模式，不产生 MES 写请求。
- Element Plus 表格/下拉门禁：真实路径选择必须按可见业务唯一文本定位，不用数组下标、坐标点击、API-only 或表头全选绕过。

## Current Status

completed

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；管理员只读 tracking 是历史执行记录的正式只读路径，不是填表失败后的降级。
- `是否从根因和长期维护角度解决`：是，恢复填写页 toolbar 的正式可见性，并同步 E2E/静态合同到当前路由与 resolver 设计。
- `是否存在临时补丁或绕过`：否；未改端口配置、未 mock、未 API-only 替代页面 E2E、未写入业务数据。
## Closeout Evidence

- Cleanup preview/apply: PASS；删除本任务临时后端日志和中间 bug 证据文件，保留 `task.md`、`execution-log.md`、`verification-report.md`。
- Task-owned runtime cleanup: 已停止本任务启动的 `48081` Java 进程 PID `34940` 以释放日志句柄。
- Experience consolidation: 已更新 `docs/e2e-rules.md#eDHR 历史执行只读验证门禁`。
- Final status: completed；待本任务实现/文档提交并推送。