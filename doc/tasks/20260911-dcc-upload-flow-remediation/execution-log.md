# Execution Log

## BDD Scenarios

BDD: 大版本上传提交使用 Windchill 版本与复合身份 -> Given 当前项目与文件类型下已有 A/1 文件，When 用户从上传页发起大版本，Then 请求携带源文件标识、B/1 版本和幂等键，且不会命中其他项目或类型的同号文件。

BDD: 新文件上传必须通过项目与类别授权 -> Given 用户缺少项目访问或类别 UPLOAD 权限，When 提交新受控文件，Then 后端拒绝创建；Given 权限完整且类别绑定所选文件类型，Then 才允许创建审批实例。

BDD: 会签人员不能覆盖固定矩阵 -> Given 路由配置已解析出固定会签矩阵，When 申请人提交不同的会签人员集合，Then 后端拒绝请求；When 未覆盖或集合与矩阵一致，Then 使用配置矩阵。

BDD: 新建和大版本审批后均独立发布 -> Given 新建或大版本审批全部通过，When 流程完成，Then 文件进入 READY_TO_PUBLISH；When 文控执行一次发布，Then 新版本 ACTIVE 且旧版本 SUPERSEDED，不再次启动四阶段内容审批。

BDD: 表单中心不得绕过正式上传治理 -> Given 用户尝试从旧 UPLOAD 表单策略提交，When 执行效果，Then 系统明确拒绝已退役入口，不创建或激活受控文件。

BDD: 关联文件只能选择当前生效受控文件 -> Given 项目列表包含登记证附件、非生效版本或非当前版本，When 上传页加载关联候选或后端校验关联标识，Then 这些记录不可选且不可提交。

BDD: 重复上传提交保持幂等 -> Given 同一用户以同一幂等键重复提交相同请求，When 请求被重试，Then 只创建一份文件和一条审批链；When 同一键承载不同请求，Then 后端拒绝冲突。

BDD: 替代旧版本不产生不可回滚的存储副作用 -> Given 新版本发布事务的后续快照失败，When 数据库事务回滚，Then 旧版本仍保持原存储位置和可访问状态。

## TDD Evidence

RED: `mvn.cmd --% -pl yudao-module-dcc -am -Dtest=DccControlledFileRouteReadinessServiceTest,DccControlledFileWorkflowServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test` -> FAIL, 首次编译缺少绑定错误码，随后旧的会签覆盖测试暴露固定矩阵约束尚未固化。

GREEN: `mvn.cmd --% -Dtest=DccControlledFileRouteReadinessServiceTest,DccControlledFileWorkflowServiceImplTest,DccControlledFileFinalizationServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS, 139 tests, 0 failures, 0 errors。

GREEN: `mvn.cmd --% -pl yudao-module-dcc -DskipTests compile` -> PASS。

GREEN: `pnpm exec eslint src/api/dcc/controlledFile/workflow.ts src/views/dcc/controlled-file/upload/index.vue src/views/dcc/controlled-file/upload/submitter.ts` -> PASS。

GREEN: `node tests/e2e/dcc-upload-current-version-static.spec.js` -> PASS。

GREEN: `node tests/e2e/dcc-upload-category-taxonomy-binding-static.spec.js` -> PASS。

GREEN: `node doc/tasks/20260911-dcc-upload-flow-remediation/verify-dcc-upload-remediation.mjs` -> PASS。

GREEN: `pnpm exec eslint src/api/dcc/controlledFile/workflow.ts src/views/dcc/controlled-file/upload/index.vue src/views/dcc/controlled-file/upload/submitter.ts src/views/dcc/controlled-file/external-review/index.vue` -> PASS。

BLOCKED: `pnpm exec vue-tsc --noEmit --skipLibCheck` -> Node heap limit exceeded at approximately 4GB; not a TypeScript diagnostic.

INFO: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm exec vue-tsc --noEmit --skipLibCheck` -> task-owned external-review idempotency type error found and fixed; remaining errors are concurrent unused declarations outside this remediation.

INFO: 发布策略源码迁移将 DCC `READY_TO_PUBLISH/PUBLISH` 从 `BPM_REQUIRED` 改为 `DIRECT`，发布前置仍由 `DccControlledFileFinalizationServiceImpl.requirePublishReadyCandidate` 强制校验状态、文控批准权限和待办冲突。

RED: `python -X utf8 script\release\run-release-migration-policy-gate.py --sql-root sql\mysql` -> FAIL，已提交的 `20260911_dcc_publish_direct_policy.sql` 缺少 `release-migration` 元数据。

GREEN: 同一全量 migration policy gate -> PASS，620 migrations；该数据迁移现显式依赖 `20260906_dcc_new_file_lifecycle_p4`。

GREEN: task-closeout-cleanup preview/apply -> PASS，仅删除本任务分散证据文件和一次性合同脚本，保留三份正式任务记录、生产代码、迁移与正式测试。

INFO: project-experience-consolidation -> 已将 DCC 上传复合身份、后端授权、固定矩阵、幂等和独立发布边界合并到 `docs/backend-development.md` 既有 DCC 章节。

BLOCKED: `node tests/e2e/dcc-upload-related-files-static.spec.js` -> test file does not exist in current checkout.

FAIL: `node tests/e2e/dcc-upload-category-permission-static.spec.js` -> existing contract expects a legacy taxonomy endpoint, while the current page correctly loads project template taxonomy options; contract needs alignment in a separate frontend test task.

## Milestone Status

- M1: completed
- M2: completed
- M3: completed
- M4: completed
- M5: completed; targeted verification passed, full type check retains unrelated diagnostics outside this task.

## Final Closeout

- Implementation commit: `e3de87069`，包含 DIRECT 发布策略迁移元数据修复。
- task-closeout-cleanup preview/apply -> PASS；保留三份核心任务记录，无删除项、blocker 或 warning。
- 用户当轮已授权 Git 提交/推送；软件代码范围标记 completed，未宣称真实数据库迁移或 E2E。
- Closeout commit: `390e8283a`；`git push origin int_main` -> PASS，远端更新范围 `51d02916f..390e8283a`。
