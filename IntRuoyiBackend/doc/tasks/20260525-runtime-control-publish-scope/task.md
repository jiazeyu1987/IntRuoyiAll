# 任务：运行控制台发布范围选项后端

## 任务目标

- 给运行控制台运维动作 API 增加发布范围参数，覆盖 `发布测试服` 和 `提升正式服`。
- 默认业务语义为只发布前后端与 Website 代码产物；用户明确选择后才同步 MySQL 与 MinIO 数据。
- 后端必须校验 `publishScope`，记录审计参数，并把只发代码映射为发布脚本的跳过数据同步参数。

## 非目标

- 不执行真实测试服发布或正式服提升。
- 不新增只同步数据库或只同步 MinIO 的细分模式。
- 不改变备份、回滚、恢复数据动作的现有语义。

## 前置任务检查

- 当前 worktree 分支：`task/20260525-runtime-control-ops-console`。
- 前一任务 `20260525-runtime-control-ops-console` 状态为 `completed`。

## 里程碑

- [x] M1：建立任务文档和 BDD 场景。
- [x] M2：补齐后端与脚本 RED 契约测试。
- [x] M3：实现 `publishScope` 校验、审计参数和脚本参数映射。
- [x] M4：实现正式服提升脚本 `-SkipDatabaseSync` / `-SkipMinioSync`。
- [x] M5：运行后端单元、脚本契约和 evidence 校验。
- [x] M6：closeout 预览并提交后端改动。

## BDD 场景

- BDD: 发布测试服默认只发代码 -> Given 运维人员提交 `publish-test` 且选择 `code-only`, When 后端派发脚本, Then 调用受控测试发布脚本并附加 `-SkipDatabaseSync -SkipMinioSync`，审计记录 `publishScope=code-only`。
- BDD: 发布测试服可带数据发布 -> Given 运维人员提交 `publish-test` 且选择 `with-data`, When 后端派发脚本, Then 调用受控测试发布脚本且不附加跳过数据参数，审计记录 `publishScope=with-data`。
- BDD: 提升正式服只发代码仍需 PROD -> Given 运维人员提交 `promote-prod` 且选择 `code-only`, When 缺少 `PROD` 确认, Then 请求失败且不执行；When 确认完整, Then 调用正式提升脚本并附加跳过数据参数。
- BDD: 非发布动作不接受发布范围 -> Given 运维人员提交备份、回滚或恢复动作, When 请求携带 `publishScope`, Then 后端失败关闭并提示参数不适用。

## 预期验证

- RED：`mvn -pl yudao-module-infra -Dtest=RuntimeControlServiceImplTest test` 先失败，证明 publishScope 契约未实现。
- RED：`python -m pytest script\tests\test_runtime_control_ops_scripts.py script\tests\test_publish_int_ruoyi_to_test_tooling.py -q` 先失败，证明脚本参数契约未实现。
- GREEN：上述后端单元与脚本契约通过。
- GREEN：`python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc\tasks\20260525-runtime-control-publish-scope\backend-api-evidence.md` 通过。

## 当前状态

- 状态：completed
- 已完成：
  - 已建立任务文档和 BDD 场景。
  - 已补齐后端 Java 单元 RED/GREEN 覆盖。
  - 已补齐脚本契约 RED/GREEN 覆盖。
  - 已实现发布范围校验、审计记录和发布脚本参数映射。
  - 已给正式服提升脚本增加代码发布模式的数据同步跳过分支。
  - 后端单元、脚本契约和 evidence 校验均已通过。
  - 已运行 task-closeout-cleanup 预览；预览因主 worktree 状态阻止 apply，未执行清理应用。
- 阻塞与影响：
  - task-closeout-cleanup apply 阶段未执行：后端主 worktree dirty 且当前 linked worktree 不能快进合并到 `int_main`；这只影响自动清理/合并 worktree，不影响本任务代码交付。
