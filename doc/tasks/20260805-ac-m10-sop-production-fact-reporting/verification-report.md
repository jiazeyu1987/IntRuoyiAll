# AC-M10 SOP 生产事实报工验证报告

## Scope

- 验证岗位矩阵 `AC-M10 | 生产员工 | 按 SOP 生产`。
- 目标：未选择订单/任务时，生产模式可按设备账号授权工序进入 SOP 事实报工草稿；缺 SOP/模板或越权工序由后端正式 fail fast；正式一体提交继续遵守现有订单/任务关联模型。

## Changes Verified

- `FrontlineFixedTemplatePanel.vue` 生产模式初始化继续使用 `/frontline/device-account/processes`，不依赖 PQC 活跃订单。
- 生产模式预校验不再要求 `context.workOrderId`；PQC 模式仍要求订单上下文。
- 正式一体提交前端请求类型与 payload 补齐后端必填 `processPoolSubmissionIdempotencyKey`。
- 后端缺模板和越权阻塞链路保持现有 fail-fast 测试覆盖。

## Verification

- `node tests/e2e/role-matrix-ac-m10-sop-production-static.spec.cjs` -> RED，失败于生产预校验强制订单上下文。
- `node tests/e2e/role-matrix-ac-m10-sop-production-static.spec.cjs` -> GREEN，PASS。
- `node tests/e2e/frontline-formal-submit-static.spec.cjs` -> GREEN，PASS。
- `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlineRuntimeConfigServiceTest,MesFrontlineTemplateResolverTest,MesFrontlineSubmitAuthorizationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> GREEN，BUILD SUCCESS；Tests run: 6, Failures: 0, Errors: 0。
- 2026-08-05 收尾复跑上述两条 Node 静态契约和 Maven 目标命令 -> 全部 PASS；Maven Tests run: 6, Failures: 0, Errors: 0, Skipped: 0。
- `git diff --check -- <task paths>` -> PASS，无 whitespace error；仅出现既有 CRLF 工作区提示。
- `task-closeout-cleanup --mode preview/apply` -> PASS；keep `task.md`、`execution-log.md`、`verification-report.md`，delete/blocked/warnings 均为 `<none>`。

## Non-Task Blockers

- `node tests/e2e/edhr-frontline-fill-tabs-static.spec.cjs` -> FAIL，`EdhrBatchRecordTabs.vue` 缺少“历史批记录”页签；该文件不是本次 AC-M10 修改范围。
- `pnpm ts:check` -> FAIL，`src/views/mes/pro/processpool/QaRegulationPage.vue(1617,3)` 的 `PATROL_AM` 类型不匹配；该文件已有并行修改，本任务未触碰。
- 无 AC-M10 完成门禁阻塞；共享工作区仍存在其它并行任务脏改动，本任务未暂存或回滚。

## Result

AC-M10 定向代码修复已通过并完成收尾。生产 SOP 草稿入口不再被订单上下文预校验阻塞；后端缺 SOP/模板和越权阻塞仍由正式测试覆盖；正式订单关联提交未被放宽或降级。
