# 报工提交参数明细与班组长报工表展示实现

## Task Goal

在隔离 worktree `D:\IntRuoyiWorktree\production-reporting-submit-implementation-20260806` 中完成报工提交参数明细的前后端实现和验证，并在验证通过后按规则融合回 `int_main`。

核心需求：

- 班组长“报工管理”生产报工表删除 `生产工单`、`PQC`、`提交内容` 三列。
- 生产组长报工表和显示字段设置不展示 `PQC提交内容`、检验类型/轮次、过程检验汇集等 PQC 专属内容；PQC 专属内容仅保留在 PQC 组长表。
- 报工提交 payload 和班组长展示必须包含工序、员工、完成数量、损耗数量、各损耗原因对应数量、选用设备、设备参数读数。
- 损耗原因、设备、设备参数上下限必须按当前工序对应的班组长配置解析，不能跨工序串用同名配置。
- 设备参数超出配置上下限时允许提交，但在班组长表格/详情中标红提示异常。
- 损耗数量必须等于各损耗原因数量之和，不一致时提交失败。

## Milestones

- [x] M1：建立实现任务证据，复用设计任务 BDD/TDD 方案并冻结验收范围。
- [x] M2：先写前端/后端 RED 测试，覆盖表格列、结构化 payload、损耗合计校验、工序配置范围和异常参数标红。
- [x] M3：实现最小正式方案，保持现有报工链路、配置快照和无 fallback 策略。
- [x] M4：运行 GREEN 与回归验证，记录前端/后端技能证据。
- [ ] M5：完成收尾、提交、推送，并融合回 `int_main`。

## Expected Verification

- `node tests/e2e/team-leader-production-report-payload-columns-static.spec.cjs`
- `node tests/e2e/frontline-production-submit-payload-detail-static.spec.cjs`
- `node tests/e2e/team-leader-production-report-abnormal-parameter-static.spec.cjs`
- `node tests/e2e/team-leader-workbench-static.spec.cjs`
- `node tests/e2e/frontline-formal-submit-static.spec.cjs`
- `node tests/e2e/team-leader-report-allocation-static.spec.cjs`
- `pnpm ts:check`
- `mvn -pl yudao-module-mes -am "-Dtest=MesProFrontlineFeedbackSubmitDetailContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlineRuntimeConfigProcessScopeTest,MesProcessPoolTimelineSubmissionPayloadDisplayTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `scripts\preflight\branch-runtime-port-guard.ps1`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260806-production-reporting-submit-implementation/frontend-feature-evidence.md`
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260806-production-reporting-submit-implementation/backend-api-evidence.md`

真实写入型 E2E 仅在本 worktree runtime、测试租户、账号、数据前置完整时执行；缺前置时按项目规则 fail fast 并记录 blocker，不用 API-only 或 mock 替代。

## Applicable Gates

- Worktree：目标路径必须位于 `D:\IntRuoyiWorktree\` 下，已登记 profile `int_main` slot 5，对应前端 `8086`、后端 `48086`。
- PowerShell：不使用 `&&`；中文文档按 UTF-8 读写；Maven `-D` 参数整体加双引号。
- BDD/TDD：生产代码改动前必须记录 BDD，并先运行 RED 失败测试，再实现 GREEN。
- 前端静态契约隔离：如果全量 `pnpm ts:check` 或既有大契约先失败于无关历史问题，必须保留任务专用最小静态契约 RED/GREEN 证据。
- 后端 Maven：若出现页面文件不足、target 损坏或并发 Maven 写入阻塞，必须记录精确 blocker，不能把未到达 Surefire 的结果写成通过。
- Closeout：实现验证完成后先把状态设为 `ready_for_closeout`，运行 cleanup preview/apply 后才能标记 `completed`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，按正式工序配置、提交 payload 和读模型展示链路实现，不用前端文案或默认值掩盖配置缺失。
- `是否存在临时补丁或绕过`：否。

## Current Status

ready_for_closeout

User追加反馈要求生产组长报工表不显示 PQC 内容。已在同一 worktree 中完成前端静态契约、最小实现、回归验证、证据校验、经验沉淀、任务分支推送和远端 `int_main` 快进融合；远端 `int_main` 已包含实现提交 `b8aad69358aee29e2698c07afb81aca6eb4d7ae0` 以及后续收尾记录提交，最终以 `git fetch origin int_main` 后 `HEAD == origin/int_main` 的校验为准。本地 cleanup apply / worktree removal 仍受 `E:\IntRuoyi` 脏工作区阻塞。
