# 工艺路线表单槽位参与批次执行任务生成

## Task Goal

验证并修正工艺路线发布后的表单槽位配置在批次执行任务生成中的落地链路，确保批次执行任务从冻结路线快照读取表单槽位绑定，并完整继承填写人、填写规则、工序独立/批次共享和共享表单标识。

## Milestones

- [x] 建立批次执行任务生成的 BDD 场景和 RED 证据
- [x] 定位路线发布快照、表单槽位绑定和批次任务生成链路
- [x] 补充失败优先的后端回归测试
- [x] 实施最小正式修复，禁止 fallback、降级和吞异常
- [x] 运行目标验证并记录 GREEN 证据
- [x] 完成收尾、验证报告和经验沉淀

## Expected Verification

- 目标测试先证明当前批次任务生成未完整使用冻结表单槽位配置。
- 修复后目标测试通过，证明 `formBindings` 中的填写人、填写规则、`instanceScope`、`sharedFormKey`、必填策略等能进入批次执行任务。
- 运行受影响模块的相关后端回归验证。
- `docs/experience-index.md` 当前不存在；本任务不涉及发布、远程服务、生产数据、数据库结构变更或破坏性操作，按低风险后端行为修复继续推进。

## Current Status

completed

## Verification Summary

- RED：`mvn -pl yudao-module-mes -Dtest=MesProEdhrBatchExecutionServiceTest#openOrCreate_usesFrozenRouteVersionFormBindingsInsteadOfCurrentDraft test` 失败，实际任务使用 `CURRENT_DRAFT_1`，证明新建批次绕过发布快照读取了当前草稿配置。
- GREEN：隔离编译目标测试类后运行 `mvn -Dtest=MesProEdhrBatchExecutionServiceTest#openOrCreate_usesFrozenRouteVersionFormBindingsInsteadOfCurrentDraft surefire:test` 通过，证明新建批次改为使用冻结 `formBindings`。
- GREEN：`node src\test\js\edhr-route-form-slot-frozen-runtime-static.spec.cjs` 通过，锁定新建和重执行批次均走冻结快照任务构建。
- GREEN：`mvn -pl yudao-module-mes -DskipTests compile` 通过。
- 阻塞说明：标准 `mvn -pl yudao-module-mes -Dtest=... test` 在修复后进入全量 `testCompile`，被项目中无关旧测试源码缺失类阻塞；本任务已用隔离编译和直接 surefire 执行完成目标方法验证。
- Closeout：`task-closeout-cleanup` preview/apply 均通过，无删除项、无阻塞、非 linked worktree。
- 经验沉淀：已按 `project-experience-consolidation` 搜索现有长期经验文档；当前没有合适的 `*memory*.md` 或工程长期经验归宿，未在没有用户授权的情况下新建长期经验文档。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否
- `是否从根因和长期维护角度解决`：是
- `是否存在临时补丁或绕过`：否

## Cleanup Keep

- `doc/tasks/20260724-route-form-slot-execution-task-generation/backend-api-evidence.md`
- `doc/tasks/20260724-route-form-slot-execution-task-generation/bug-regression-evidence.md`
