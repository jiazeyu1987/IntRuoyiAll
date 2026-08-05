# Execution Log

## User Intent

用户在岗位矩阵修复后要求继续；本轮选择下一项高优先缺口 `AC-M10`：生产员工按 SOP 生产，需要证明未选订单/任务仍可按 SOP 进入工序事实报工，且缺工序/SOP 或越权工序时阻塞。

## Preflight

- SKILL: `bug-regression-fix-loop`、`backend-api-delivery`、`frontend-feature-delivery`、`bdd-tdd-acceptance-planner` -> READ。
- SKILL REFERENCES: `bug-contract.md`、`backend-contract.md`、`frontend-contract.md`、`acceptance-structure.md` -> READ。
- RULE: `docs/task-closeout-rules.md`、`docs/backend-development.md`、`docs/frontend-development.md`、`docs/e2e-rules.md`、`docs/powershell-encoding.md`、`docs/powershell-memory.md` -> READ。
- EXPERIENCE: `docs/experience-index.md` -> READ；命中前端静态契约隔离、PowerShell Maven `-D` 引号、隔离验证 worktree 和一线设备账号权限门禁。

## BDD Scenarios

- BDD: 无订单按 SOP 进入工序事实报工 -> Given 生产员工登录一线页面且拥有某正式工序权限，When 未选择活跃订单或批次任务但选择该工序，Then 系统应返回正式 SOP/生产模板上下文并允许进入事实报工草稿。
- BDD: 缺 SOP 阻塞 -> Given 生产员工选择的正式工序没有可用 SOP/生产模板绑定，When 请求进入生产事实报工，Then 后端必须返回明确缺 SOP/模板错误，前端显示阻塞原因且不得进入默认模板。
- BDD: 越权工序阻塞 -> Given 生产员工没有目标工序的岗位/工作站/权限角色授权，When 请求目标工序 SOP 生产上下文，Then 后端必须拒绝并说明授权来源缺失，不得通过前端隐藏或空列表成功绕过。

## TDD Evidence

- RED: `node tests/e2e/role-matrix-ac-m10-sop-production-static.spec.cjs` -> FAIL，预期失败原因为 `assertFormalPayloadContext` 在生产模式预校验阶段仍强制 `context.workOrderId`，阻塞无订单 SOP 生产事实报工草稿进入。
- GREEN: `node tests/e2e/role-matrix-ac-m10-sop-production-static.spec.cjs` -> PASS。
- GREEN: `node tests/e2e/frontline-formal-submit-static.spec.cjs` -> PASS。
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlineRuntimeConfigServiceTest,MesFrontlineTemplateResolverTest,MesFrontlineSubmitAuthorizationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，Tests run: 6, Failures: 0, Errors: 0。
- CLEANUP: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260805-ac-m10-sop-production-fact-reporting --mode preview` -> PASS，keep 3，delete/blocked/warnings 均为 `<none>`。
- CLEANUP: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260805-ac-m10-sop-production-fact-reporting --mode apply` -> PASS，无删除项。
- REGRESSION: 2026-08-05 收尾前复跑 `node tests/e2e/role-matrix-ac-m10-sop-production-static.spec.cjs` -> PASS。
- REGRESSION: 2026-08-05 收尾前复跑 `node tests/e2e/frontline-formal-submit-static.spec.cjs` -> PASS。
- REGRESSION: 2026-08-05 收尾前复跑 `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlineRuntimeConfigServiceTest,MesFrontlineTemplateResolverTest,MesFrontlineSubmitAuthorizationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，Tests run: 6, Failures: 0, Errors: 0, Skipped: 0。

## Implementation Notes

- 定位结果：生产模式 `FrontlineFixedTemplatePanel.vue` 初始化已调用 `loadFrontlineDeviceProcesses(deviceState)`，不依赖 `loadFrontlinePqcActiveOrders`；PQC 模式仍保留活跃订单口径。
- 修复结果：`assertFormalPayloadContext` 仅在 PQC 模式要求订单上下文，生产 SOP 草稿预校验只要求路线、工序和实际员工。
- 修复结果：前端 `ProFrontlineFeedbackSubmitReqVO` 补齐后端必填 `processPoolSubmissionIdempotencyKey`，`buildFrontlineFormalSubmitPayload` 同步发送稳定工序池幂等键。
- 保留边界：正式一体提交接口仍按后端现有模型要求 `workOrderId/taskId`，没有把无订单草稿入口扩展为无订单正式入库提交，避免用前端字段绕过正式报工领域约束。
- 后端证明：`MesFrontlineTemplateResolverTest` 覆盖缺正式模板绑定 fail fast；`MesFrontlineSubmitAuthorizationTest` 覆盖越权提交阻塞；`MesFrontlineRuntimeConfigServiceTest` 覆盖授权工序运行态配置。
- 经验沉淀：已按 `project-experience-consolidation` 检查长期文档归宿；现有 `docs/backend-development.md#MES 一线设备账号权限门禁`、`docs/inception/project-brief.md` 和 `docs/acceptance/production-execution-main-loop/*` 已覆盖通用原则，本次不新增长期经验文档。
- Git 记录：AC-M10 实现、专用静态契约和初始任务记录已由并发基线提交 `057fba5b9` 捕获；本次仅补齐 AC-M10 completed 收尾记录并保持其它并行脏文件未暂存。

## Blockers

- 非本任务阻塞：`node tests/e2e/edhr-frontline-fill-tabs-static.spec.cjs` 失败于 `EdhrBatchRecordTabs.vue` 缺少“历史批记录”页签，本任务未触碰该文件。
- 非本任务阻塞：`pnpm ts:check` 失败于 `src/views/mes/pro/processpool/QaRegulationPage.vue(1617,3)`，`"PATROL_AM"` 不可赋给 `"FIRST" | "PATROL" | "FINAL"`；该文件已有并行修改，本任务未触碰。
- 无 AC-M10 完成门禁阻塞；共享工作区仍有其它并行任务脏改动，本任务未暂存或回滚。
