# 不合格审批完整链路 E2E 执行日志

## Authorization

- 用户明确要求在新 worktree 中执行不合格审批链路全 E2E。
- 用户明确授权使用本机 `芋道源码/admin` 测试身份。
- 用户明确授权对缺失测试数据进行模拟，并允许用 admin 分配缺失权限。
- 用户要求问题先记录、后集中解决，直到完成一条完整链路。
- 日志禁止记录密码、令牌、Cookie、数据库连接密钥或签名密钥。

## Preflight

- 规则已读取：`AGENTS.md`、`docs/e2e-rules.md`、`docs/login-access.md`、`docs/local-runtime.md`、`docs/worktree-restrictions.md`、`docs/worktree-memory.md`、`docs/branch-runtime-ports.md`、`docs/database-rules.md`、`docs/task-closeout-rules.md`。
- 技能已读取：`playwright`、`bug-regression-fix-loop`、`backend-api-delivery`、`database-schema-delivery` 及对应 evidence contract。
- Playwright 前置：`npx` 可用，来源为本机已安装 Node 工具链。
- 源分支：`int_main`。
- 源 HEAD：`c9466a902d939e1d2eb3331c0c02af6ac2a5eb5d`。
- worktree：`D:\IntRuoyiWorktree\20260902-nonconformance-review-full-e2e`。
- 分支：`codex/20260902-nonconformance-review-full-e2e`。
- 端口登记：`int_main slot 7`，前端 `8088`，后端 `48088`。
- 主工作区存在其它任务 dirty 改动，本任务不读取、覆盖、提交或清理这些改动。

## BDD Scenarios

BDD: 不合格触发后冻结活跃工单 -> Given 一个任务自有的可执行活跃工单且 PQC 在提交或放行节点判定不合格 / When 用户通过真实页面提交不合格结果 / Then 系统创建同一套不合格评审单并把活跃工单状态冻结，QA 待评审数量增加。

BDD: 冻结状态禁止三个生产质量动作 -> Given 活跃工单因待处理不合格评审处于冻结状态 / When 用户分别尝试生产报工、PQC 提交和 PQC 放行 / Then 三项操作均被明确禁止且不产生成功业务写入。

BDD: QA 完成让步放行 -> Given 冻结批次存在待评审单 / When QA 上传评审材料、填写意见、电子签名并选择让步放行 / Then 评审单关闭、处置结论为让步放行、工单恢复主流程、QA 冻结列表不再显示该项，追溯信息完整。

BDD: QA 完成返工 -> Given 独立冻结批次存在待评审单 / When QA 上传评审材料、填写意见、电子签名并选择返工 / Then 评审单关闭、处置结论为返工、工单直接回到主流程，不出现生产人员返工完成确认步骤，追溯信息完整。

BDD: QA 完成作废 -> Given 独立冻结批次存在待评审单 / When QA 上传评审材料、填写意见、电子签名并选择作废 / Then 评审单关闭、处置结论为作废、该批次执行进入只读追溯并禁止继续生产、检验、放行和生成合格指令。

## Execution Evidence

### M0 Worktree Bootstrap

- `git worktree add -b codex/20260902-nonconformance-review-full-e2e D:\IntRuoyiWorktree\20260902-nonconformance-review-full-e2e int_main` -> PASS。
- `reserve-worktree-slot.ps1` -> PASS，登记 `slot=7`、`frontendPort=8088`、`backendPort=48088`。

### M1 Requirement And Existing E2E Audit

- 权威需求：`nonconformance-review-mvp-prd.md` 与 `nonconformance-review-mvp-user-flows.md`，范围为统一入口、冻结、三项操作禁止、QA 必填材料/意见/签名、让步放行/返工/作废和差异追溯。
- 当前源码包含统一后端 Controller/Service/Mapper、统一前端页面与两个来源入口。
- GAP-001：既有真实 E2E 只断言页面提示“禁止报工、PQC提交、PQC放行”，没有通过真实页面分别尝试三个操作；本轮补充真实页面三动作验证。
- 账号与租户：本轮固定使用 `芋道源码/admin`，不切换到测试租户。

### M2 Runtime Preparation

- `branch-runtime-port-guard.ps1` -> PASS，工作区绑定前端 `8088`、后端 `48088`。
- `pnpm install --frozen-lockfile` -> PASS，锁文件未变化。
- `mvn.cmd -pl yudao-server -am -DskipTests package` -> PASS，30 个模块全部 SUCCESS，生成当前 worktree 完整运行 Jar。
- worktree 后端 `48088/actuator/health` -> `UP`；前端 `8088` -> HTTP `200`。
- MinIO `9000/minio/health/ready` -> HTTP `200`，满足评审材料上传前置。
- 电脑重启后复核：`int-ruoyi-mysql`、`int-ruoyi-redis`、`docker-minio-1` 已恢复；后端 PID `40744` 监听 `48088`，前端 PID `15252` 监听 `8088`；`48088/actuator/health` -> `UP`，`8088` -> HTTP `200`。

## Issues And Fixes

### ISSUE-001 Vite 首次依赖优化重载中断页面

- 复现：Playwright run `20260902-yudao-01`，真实登录 `芋道源码/admin` 后从菜单进入 QA 列表。
- 现象：QA 待评审接口已真实返回业务成功，但 Vite 首次依赖优化触发整页 reload，页面回到 `/index`。
- 归因：worktree 新安装依赖后的开发服务器预热问题，不是业务功能失败；没有不合格评审写入。
- 处理：保留脱敏 `result.json` 和失败截图；删除可能包含登录态的 `failure-trace.zip`，使用新 run ID 重跑同一真实页面路径。

### ISSUE-002 路线包含筛选被误当成唯一精确结果

- 复现：Playwright run `20260902-yudao-02`，真实页面在路线编码条件中输入 `RT000028` 并查询。
- 现象：正式“包含”筛选合法返回 `RT000028` 与 `RT000028-IDI` 两行，旧脚本直接断言响应总行数为 1。
- 根因：E2E 脚本把包含筛选结果数量当成精确业务编码唯一性，且 DOM 行定位也使用模糊文本。
- RED: `node tests/e2e/edhr-nonconformance-review-mvp-static.spec.js` -> FAIL，expected reason: 脚本没有从响应中按 `route.code === SOURCE_ROUTE_CODE` 精确选择目标路线。
- GREEN: `node tests/e2e/edhr-nonconformance-review-mvp-static.spec.js` -> PASS。
- GREEN: `node --check tests/e2e/edhr-nonconformance-review-mvp-real.e2e.js` -> PASS。

### ISSUE-003 共享路线被正式规则阻止创建批次

- 复现：Playwright run `20260902-yudao-03`。
- 现象：真实“打开/创建批次”被正式业务错误 `1040750243` 拒绝，原因是共享路线当前批记录模板存在 98 个未确认填写规则单元格。
- 处理：不修改共享模板，改为创建事务保护的任务自有 eDHR 批次夹具。
- RED: Playwright run `20260902-yudao-03` -> FAIL，expected reason: 共享路线前置数据不满足创建批次。
- GREEN: `node doc\tasks\20260902-nonconformance-review-full-e2e\fixture-contract.spec.cjs` -> PASS。

### ISSUE-004 当前一线生产/PQC提交未复用统一冻结门禁

- 根因：`MesProcessPoolEventServiceImpl.createEvent` 与 `createPqcInspectionEvent` 没有调用不合格评审统一冻结服务。
- 修复：两个当前一线入口均调用 `nonconformanceReviewService.ensureWorkOrderNotFrozen(...)`，冻结时 fail fast。
- RED: `mvn.cmd -pl yudao-module-mes "-Dtest=MesProcessPoolEventFreezeGateTest" test` -> FAIL，expected reason: 生产报工/PQC提交旧入口未调用统一冻结服务。
- GREEN: `mvn.cmd -pl yudao-module-mes "-Dtest=MesProEdhrNonconformanceReviewApplicationScopeTest,MesProcessPoolEventFreezeGateTest" test` -> PASS，Tests run: 8。

### ISSUE-005 批次来源评审未完整冻结关联工单

- 根因：批次来源评审创建时只可靠记录批次前状态，未捕获并冻结关联工单；作废处置后无法证明工单继续禁止执行。
- 修复：不合格评审创建时统一锁定关联工单并记录原临时冻结状态；让步放行/返工恢复原状态，作废保持冻结。
- RED: `mvn.cmd -pl yudao-module-mes "-Dtest=MesProEdhrNonconformanceReviewApplicationScopeTest" test` -> FAIL，expected reason: 批次来源评审未冻结关联工单，作废后无法保证工单继续禁止执行。
- GREEN: `mvn.cmd -pl yudao-module-mes "-Dtest=MesProEdhrNonconformanceReviewApplicationScopeTest,MesProcessPoolEventFreezeGateTest" test` -> PASS，Tests run: 8。

## Final E2E Runs

- GREEN: Playwright `20260902-yudao-16` -> PASS，冻结三操作真实页面验证完成：生产报工返回 `1040750474`，PQC提交返回 `1040750474`，PQC放行不可执行且写请求数 `0`。
- GREEN: Playwright `20260902-yudao-17` -> PASS，最新代码完整链路完成：批次 `900000001024`，评审单 `19/20/21`，处置结论 `concession_release/rework/void`，页面错误和目标 console 错误均为 `0`。
- GREEN: 数据库复核 -> PASS，批次 `900000001024` 状态 `60`，关联工单保持 `temporary_frozen=1`；评审 `19/20/21` 全部 `closed` 且材料、意见、签名、追溯快照均有值；目标 pending 数 `0`。
- GREEN: 冻结动作夹具恢复复核 -> PASS，临时员工有效数 `0`，临时路线版本 `740` 有效数 `0`。

## Regression Verification

- GREEN: `node doc\tasks\20260902-nonconformance-review-full-e2e\fixture-contract.spec.cjs` -> PASS。
- GREEN: `node --check tests\e2e\edhr-nonconformance-review-mvp-real.e2e.js` -> PASS。
- GREEN: `node tests\e2e\edhr-nonconformance-review-mvp-static.spec.js` -> PASS。
- GREEN: `mvn.cmd -pl yudao-module-mes "-Dtest=MesProEdhrNonconformanceReviewApplicationScopeTest,MesProcessPoolEventFreezeGateTest" test` -> PASS，Tests run: 8, Failures: 0, Errors: 0, Skipped: 0。
- GREEN: `mvn.cmd -pl yudao-module-mes "-Dtest=MesProcessPoolEventFreezeGateTest,MesProcessPoolEventServiceTest,MesProcessPoolPqcEventTest,MesP0FrontlineSubmitIdempotencyTest" test` -> PASS，Tests run: 15, Failures: 0, Errors: 0, Skipped: 0。

## Evidence Validators

- GREEN: `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc\tasks\20260902-nonconformance-review-full-e2e\backend-api-evidence.md` -> PASS。
- GREEN: `python C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence doc\tasks\20260902-nonconformance-review-full-e2e\database-schema-evidence.md` -> PASS。
- GREEN: `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260902-nonconformance-review-full-e2e\bug-regression-evidence.md` -> PASS。
- GREEN: `git diff --check` -> PASS，仅报告 Windows CRLF 提示，无空白错误。
- GREEN: `pwsh -NoProfile -File scripts\preflight\branch-runtime-port-guard.ps1` -> PASS，`frontend 8088`，`backend 48088`。

## Project Experience Consolidation

- 已按收尾基线读取 `project-experience-consolidation` 技能。
- 已合入既有长期经验文档 `docs\e2e-rules.md`，新增“冻结/禁用类流程真实动作验证门禁”。
- 已在 `docs\e2e-rules.md` 补充“逐步截图验收必须保存 `trace.zip` 并从通过 trace 导出步骤图”的通用规则。
- 未新建长期经验文档。

## Closeout Status

实现和验证已完成。任务文档已标记 `ready_for_closeout`。

- GREEN: `pwsh -NoProfile -File doc\tasks\20260902-nonconformance-review-full-e2e\export-trace-step-screenshots.ps1` -> PASS，已从通过的 Playwright trace 导出逐步截图索引；`20260902-yudao-17` 导出 73 张，`20260902-yudao-16` 导出 39 张，`20260902-yudao-20-entry-both-source-trace` 导出 24 张，共 136 张，未重新执行写库操作。
- BLOCKED: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260902-nonconformance-review-full-e2e --mode preview` -> BLOCKED，expected reason: 当前实现改动尚未提交；当前分支 `codex/20260902-nonconformance-review-full-e2e` 不能 fast-forward 合入 `int_main`；主 worktree `E:\IntRuoyi` 存在非本任务脏改，不能接收 ff-only merge。
- cleanup apply 以及 Git 提交、推送、融合等待当前项目收尾门禁和当轮 Git 授权。

## 2026-09-02 双入口专门 E2E 补充

BDD: PQC双入口只读可达验证 -> Given 已存在一个可从PQC管理与PQC生产放行发起评审的批次 / When 只点击两个入口进入评审页但不提交评审单 / Then `PQC生产放行` 待放行按钮与 `PQC组长 > PQC管理` 行按钮都进入同一个不合格评审页，且评审创建写请求数为 0。

- GREEN: `node --check tests\e2e\edhr-nonconformance-review-mvp-real.e2e.js` -> PASS。
- GREEN: `node tests\e2e\edhr-nonconformance-review-mvp-static.spec.js` -> PASS，静态合同已锁定 entry-only 模式必须同时调用 `openReleaseReviewEntry` 与 `openPqcReviewEntry`，并输出 `entry-pqc-release.png`、`entry-pqc-management.png`、`trace.zip`。
- GREEN: worktree runtime -> 后端 `http://127.0.0.1:48088/actuator/health` 为 `UP`，前端 `http://127.0.0.1:8088/` 返回 HTTP `200`。
- GREEN: `git diff --check` -> PASS，仅报告 Windows CRLF 提示，无空白错误。
- GREEN: Playwright `20260902-yudao-20-entry-both-source-trace` -> PASS，使用既有源批次 `900000000926 / EDHRB-1785810846141` 做只读入口验证，未提交评审单。
- GREEN: `PQC生产放行` 入口 -> 进入同一不合格评审页，URL source 为 `PQC_RELEASE`，sourceId 为 `104`，batchExecutionId 为 `900000000926`。
- GREEN: `PQC组长 > PQC管理` 入口 -> 进入同一不合格评审页，URL source 为 `PQC_SUBMISSION`，sourceId/eventId 为 `160`，batchExecutionId 为 `900000000926`。
- GREEN: 双入口写请求监控 -> 不合格评审 create/dispose 写请求数 `0`，pageErrors `0`，targetConsoleErrors `0`；仅记录到一个非目标静态资源 `502` console 错误。
- GREEN: `pwsh -NoProfile -File doc\tasks\20260902-nonconformance-review-full-e2e\export-trace-step-screenshots.ps1` -> PASS，双入口 run 导出 24 张逐步截图，索引为 `IntRuoyiFronted\output\playwright\nonconformance-review-mvp\step-screenshots\step-screenshots-index.md`。

## 2026-09-02 Closeout Preview After Double Entry Verification

- BLOCKED: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260902-nonconformance-review-full-e2e --mode preview` -> BLOCKED，expected reason: 当前项目规则要求 Git 提交/推送/融合必须当轮明确授权，本轮未获得新的 Git 授权；主工作区 `E:\IntRuoyi` 存在非本任务脏改且 `int_main` ahead 6，不能接收 ff-only merge；当前任务分支 `codex/20260902-nonconformance-review-full-e2e` 落后 `int_main`，当前实现改动尚未提交。
- READONLY: `git status --short --branch --untracked-files=all` in `E:\IntRuoyi` -> `int_main...origin/int_main [ahead 6]`，并存在 DCC、route、docs 等非本任务脏改。
- READONLY: `git merge-base --is-ancestor int_main codex/20260902-nonconformance-review-full-e2e` -> no；`git merge-base --is-ancestor codex/20260902-nonconformance-review-full-e2e int_main` -> yes，说明任务 worktree 基线早于当前 `int_main`。
