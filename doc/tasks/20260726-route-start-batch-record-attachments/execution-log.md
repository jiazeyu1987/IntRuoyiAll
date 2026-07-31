# Execution Log

## User Intent

- 在“工序开始”节点左侧固定增加“批记录附件”页签。
- 点击后右侧显示 4 个流程负责人选择：来料检报告、灭菌报告、成品检报告、成品检记录。
- 默认创建 4 个权限角色：来料检报告上传1、灭菌报告上传1、成品检报告上传1、成品检记录上传1。
- 每个角色随机分配 2-4 个用户，用户来源只能是当前租户启用用户。
- 该能力只作用于“工序开始”节点。

## Environment Preflight

- Branch: `int_main`
- Remote: `origin https://github.com/jiazeyu1987/IntRuoyiAll.git`
- Initial status: `## int_main...origin/int_main [ahead 9]`
- Dirty worktree: no tracked/untracked files shown by initial `git status --short --branch`.

## BDD

- BDD: 工序开始批记录附件入口 -> Given 用户在路线流程图选中“工序开始”节点，When 查看左侧固定页签，Then 只能在该节点看到“批记录附件”入口并可打开右侧配置。
- BDD: 四项附件负责人配置 -> Given 用户打开“批记录附件”，When 查看右侧配置区，Then 系统展示来料检报告、灭菌报告、成品检报告、成品检记录 4 项及对应默认上传角色。
- BDD: 当前租户启用用户随机授权 -> Given 当前租户存在至少 2 个启用用户，When 初始化默认上传角色，Then 每个角色只分配当前租户启用用户且人数为 2-4。
- BDD: 启用用户不足失败 -> Given 当前租户启用用户少于 2 个，When 初始化默认上传角色，Then 初始化失败并返回明确错误，不使用停用用户或其他租户用户。
- BDD: 路线配置持久化 -> Given 用户保存路线配置，When 重新打开候选路线或发布版本，Then 4 项批记录附件负责人配置保持一致。

## RED/GREEN Evidence

- RED: `node tests/e2e/mes-route-flow-start-batch-record-attachments-static.spec.js` -> FAIL，前端 `flowconfig.ts` 缺少批记录附件负责人读取、初始化和保存 API；组件缺少 START 节点“批记录附件”入口与右侧 4 项配置区。
- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesProRouteBatchRecordAttachmentOwnerServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，后端初始测试期望的 VO/API/service 行为未完成。
- GREEN: `node tests/e2e/mes-route-flow-start-batch-record-attachments-static.spec.js` -> PASS，START 节点批记录附件静态合同通过。
- GREEN: `node tests/e2e/mes-route-flow-end-release-owner-static.spec.js` -> PASS，END 节点放行责任人回归通过。
- GREEN: `node tests/e2e/mes-route-flow-release-owner-candidate-static.spec.js` -> PASS，放行责任人候选来源回归通过。
- GREEN: `pnpm ts:check` -> PASS，前端 relaxed TypeScript 检查通过。
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProRouteBatchRecordAttachmentOwnerServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，3 tests / 0 failures / 0 errors。
- GREEN: `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260726-route-start-batch-record-attachments/backend-api-evidence.md` -> PASS。
- GREEN: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260726-route-start-batch-record-attachments/frontend-feature-evidence.md` -> PASS。

## Completed Work

- 后端新增/接通批记录附件负责人接口，使用路线候选版本 `configSnapshots.batchRecordAttachmentOwners` 保存 4 项配置。
- 后端初始化会创建 4 个默认角色，并为每个角色从当前租户启用用户中确定性随机分配 2-4 人；启用用户不足、角色分类缺失、负责人非法时 fail fast。
- 前端只在“工序开始”边界节点左侧展示“批记录附件”，默认打开右侧配置；“工序结束”仍只展示放行责任人。
- 前端右侧配置区展示来料检报告、灭菌报告、成品检报告、成品检记录及默认角色名称，支持个人/权限角色候选、初始化默认角色和保存。
- 项目经验已沉淀到 `docs/powershell-memory.md`，并更新 `docs/experience-index.md` 关键词。

## Verification Notes

- 中途 Maven 聚焦过滤发现本任务后端 `parseCandidateSourceNames` 方法重名，已改为批记录附件专用解析方法并复验通过。
- 中途测试 import 追加曾出现 PowerShell `` `r`n`` 字面量，已用 `apply_patch` 修正并复跑 Maven 通过；该经验已写入 PowerShell memory。

## Blockers

- 收尾阻塞：当前工作区存在非本任务并行脏改动和本地分支 ahead 状态；为避免提交/清理并行任务文件，本任务未执行 cleanup apply、commit 和 push。

## 2026-07-26 Runtime Bug Report

- Bug: 用户复现 `请求地址不存在: admin-api/mes/pro/route/flow-config/batch-record-attachment-owners`。
- Diagnosis: `E:\IntRuoyi\IntRuoyiFronted` 的 `8081` 前端使用 `.env.local` 代理到 `http://127.0.0.1:48081`，但 `48081` 当前监听 PID `53560` 来自 `D:\IntRuoyiWorktree\codex-test-run-monitor-runtime\IntRuoyiBackend\yudao-server\target\yudao-server-exec.jar`。
- Evidence: 当前 `E:\IntRuoyi` 源码包含 `batch-record-attachment-owners` 三个 Controller 映射；PID `53560` 对应 worktree 的 `MesProRouteFlowConfigController.java` 不包含这些映射。
- Impact: 前端命中了未包含本任务接口的运行 Jar，登录态访问会出现“请求地址不存在”；这不是当前前端 URL 拼接错误。
- Blocker: 按本地运行态规则，不能静默强停其他 worktree 进程或切换端口；需要用户确认后停止 PID `53560` 并加载 `E:\IntRuoyi` 后端 Jar 到 `48081`。

## 2026-07-26 Real E2E Retry

- Preflight: 已读取 `docs/e2e-rules.md`、`docs/local-runtime.md`、`docs/login-access.md`、`docs/worktree-restrictions.md`、`docs/branch-runtime-ports.md`、`docs/powershell-encoding.md` 与 `docs/task-closeout-rules.md`。
- Runtime: 使用任务专用 worktree `D:\IntRuoyiWorktree\route-start-batch-record-attachments-e2e`，前端 `http://127.0.0.1:8087`，后端 `http://127.0.0.1:48087`。
- Runtime: 旧后端 PID `32484` 的命令行和 pid 文件均指向任务 worktree Jar 与 `--server.port=48087`，已停止并重启为 PID `39420`；`/actuator/health` 返回 `UP`。
- Runtime: 前端 `8087` 返回 HTTP 200，Playwright 依赖前置 `npx` 可用。
- RED: `node doc/tasks/20260726-route-start-batch-record-attachments/e2e-artifacts/route-start-attachments-real/route-start-batch-record-attachments-real.e2e.js` -> FAIL，真实页面登录阶段返回“登录失败，账号密码不正确”；使用的是 `测试租户/aoteman`，密码由本地环境读取并已脱敏。
- Blocker: 当前进程没有 `MES_ROUTE_START_ATTACHMENT_E2E_PASSWORD`、`EDHR_BATCH_E2E_PASSWORD`、`DCC_BACKUP_E2E_PASSWORD` 等任务专用密码环境变量；`IntRuoyiFronted/.env` 默认账号为 `芋道源码/admin`，不是本 E2E 约束的 `测试租户/aoteman`。
- Impact: E2E 未进入路线页面、未点击“工序开始”、未执行初始化/保存写入；不能声明真实页面 E2E 通过。
