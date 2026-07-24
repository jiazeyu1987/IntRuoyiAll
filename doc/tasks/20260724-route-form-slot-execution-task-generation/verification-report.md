# 验证报告：工艺路线表单槽位参与批次执行任务生成

## Summary

- 修复范围：`MesProEdhrBatchExecutionServiceImpl` 新建批次和质量拒收重执行批次的任务配置构建入口。
- 目标行为：批次执行任务必须从 active route version 的冻结 `routeSnapshotJson.formBindings` 解析，不读取发布后修改的当前草稿配置。
- 结论：目标回归测试、静态回归测试和后端主编译通过。

## Commands

- RED：`mvn -pl yudao-module-mes -Dtest=MesProEdhrBatchExecutionServiceTest#openOrCreate_usesFrozenRouteVersionFormBindingsInsteadOfCurrentDraft test` -> FAIL，实际 `CURRENT_DRAFT_1`。
- GREEN：`mvn -pl yudao-module-mes -DskipTests compile` -> PASS。
- GREEN：`node src\test\js\edhr-route-form-slot-frozen-runtime-static.spec.cjs` -> PASS。
- GREEN：`mvn -Dtest=MesProEdhrBatchExecutionServiceTest#openOrCreate_usesFrozenRouteVersionFormBindingsInsteadOfCurrentDraft surefire:test` -> PASS，先隔离编译目标测试类以避开无关测试源码阻塞。

## Blockers

- `mvn -pl yudao-module-mes -Dtest=MesProEdhrBatchExecutionServiceTest#openOrCreate_usesFrozenRouteVersionFormBindingsInsteadOfCurrentDraft test` 修复后进入全量 `testCompile`，被无关旧测试源码缺失类阻塞；本任务未修改这些旧测试。

## Closeout

- `task-closeout-cleanup` preview/apply -> PASS，无删除项、无阻塞、非 linked worktree。
- 技能证据校验 -> PASS：bug regression evidence 和 backend API evidence 均满足脚本要求。
- 经验沉淀 -> 已搜索现有长期经验文档；无合适归宿，未在缺少授权时新建文档。
