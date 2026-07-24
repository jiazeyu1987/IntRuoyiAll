# 执行日志：工艺路线表单槽位参与批次执行任务生成

## User Intent

用户确认下一步需要继续验证并打通“工艺路线表单槽位 -> 发布冻结快照 -> 批次执行任务生成”链路，重点是填写人/填写规则和工序独立/批次共享信息是否真正参与批次执行。

## Gate Notes

- 2026-07-24：检查 `docs/experience-index.md`，结果为不存在；本任务按低风险后端行为修复继续推进，并记录门禁缺失事实。
- 2026-07-24：适用技能：`bug-regression-fix-loop`、`backend-api-delivery`；已读取技能正文和证据契约。

## BDD Scenarios

- BDD: 冻结表单槽位生成批次执行任务 -> Given 已发布路线快照包含一个工序独立表单槽位和一个批次共享表单槽位，并分别配置填写人/填写规则 / When 创建批次执行任务 / Then 批次执行任务应从冻结快照生成，并完整保留表单模板、填写人/填写规则、`instanceScope`、`sharedFormKey` 和必填策略
- BDD: 发布后修改当前草稿不影响批次执行 -> Given 路线已发布并冻结表单槽位配置 / When 后续修改当前工序配置 / Then 基于该发布版本创建的批次执行仍使用冻结快照中的表单槽位配置

## TDD Evidence

- RED: `mvn -pl yudao-module-mes -Dtest=MesProEdhrBatchExecutionServiceTest#openOrCreate_usesFrozenRouteVersionFormBindingsInsteadOfCurrentDraft test` -> FAIL，预期失败原因：发布快照中为 `FB_<routeProcessId>_1`，当前实现实际生成 `CURRENT_DRAFT_1`，证明新建批次任务仍读取当前草稿配置。
- GREEN: `mvn -pl yudao-module-mes -DskipTests compile` -> PASS，后端主代码编译通过。
- GREEN: `node src\test\js\edhr-route-form-slot-frozen-runtime-static.spec.cjs` -> PASS，静态回归确认新建/重执行批次均调用冻结快照感知的任务构建方法。
- GREEN: `mvn -Dtest=MesProEdhrBatchExecutionServiceTest#openOrCreate_usesFrozenRouteVersionFormBindingsInsteadOfCurrentDraft surefire:test` -> PASS，隔离编译目标测试类后，目标 JUnit 方法通过。

## Verification Evidence

- 2026-07-24：根因定位为 `openOrCreate` 和 `reexecuteRejectedBatch` 在批次对象写入 `routeVersionId/routeSnapshotJson` 之前，先调用 `buildBatchTaskConfigs(route, routeProcesses)`，导致任务配置来自当前路线草稿配置。
- 2026-07-24：修复为先构造带 active route version 快照的批次对象，再调用 `buildBatchTaskConfigs(batch, route, routeProcesses)`，复用现有 `resolveFrozenBatchTaskConfigs` 和 `formBindings` 解析逻辑。
- 2026-07-24：新增 `openOrCreate_usesFrozenRouteVersionFormBindingsInsteadOfCurrentDraft`，构造“发布快照旧表单槽位 + 当前草稿已改动”的回归场景，断言任务保留冻结的 `formBindingKey`、`instanceScope`、`sharedFormKey`、`fillableScopeJson`、`requiredPolicy`、`ownerRoleKey`、`slotConfigSnapshotHash`。
- 2026-07-24：新增 `edhr-route-form-slot-frozen-runtime-static.spec.cjs`，锁定新建批次和质量拒收重执行批次均不得绕过 `routeSnapshotJson` 读取当前草稿配置。
- 2026-07-24：`python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260724-route-form-slot-execution-task-generation\bug-regression-evidence.md` -> PASS。
- 2026-07-24：`python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc\tasks\20260724-route-form-slot-execution-task-generation\backend-api-evidence.md` -> PASS。
- 2026-07-24：`task-closeout-cleanup` preview/apply -> PASS，无删除项、无阻塞、当前为主 worktree。
- 2026-07-24：`project-experience-consolidation` 已执行搜索；当前没有合适已有长期经验文档，按技能规则未擅自新建文档。

## Blockers

- 标准 Maven 测试命令在修复后被无关旧测试源码编译错误阻塞，典型错误包括 `MesProRouteProcessControllerTest`、`MesProRouteProductBindFromWorkOrdersTest`、`MesProRouteScheduleConfigServiceTest` 缺失已迁移类；本任务未修改这些文件，已通过隔离目标测试类和直接 surefire 执行完成验证。
