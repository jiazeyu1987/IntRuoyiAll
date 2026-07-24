# 执行日志

BDD: 同工序生成全部辅助表单任务 -> Given 工序配置主表、损耗单、过程检验单和参数记录表 / When 创建批次执行 / Then 同一工序生成 4 个独立任务并保留槽位类型。

BDD: 辅助表单阻止下一工序 -> Given 主表已完成但任一辅助表单未完成 / When 用户打开下一工序 / Then 系统拒绝并提示上一工序批记录未全部填写完成。

BDD: 同工序全部完成后流转 -> Given 同一工序全部必填表单已完成 / When 用户打开下一工序 / Then 下一工序可正常打开。

BDD: 并行模式开放同工序表单 -> Given 工序执行模式为 PARALLEL / When 批次执行创建完成 / Then 同工序主表和辅助表单均可打开，但下一工序仍等待全部完成。

BDD: 不完整槽位配置直接失败 -> Given 辅助表单缺少正式槽位元数据 / When 创建批次执行 / Then 创建失败并明确暴露配置缺失。

GREEN: previous-task-check -> PASS，上一后端任务已完成并提交。

RED: `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProEdhrBatchExecutionServiceTest#openOrCreate_rejectsCompanionFormWithoutSlotMetadata" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，当前服务未拒绝缺少 `formSlotType/slotConfigSnapshotHash` 的辅助表单绑定。

GREEN: 同一定向命令 -> PASS，缺少槽位元数据的辅助表单绑定在批次创建时直接失败。

GREEN: `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProEdhrBatchExecutionServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，74 tests，覆盖四槽位任务生成、SEQUENTIAL/PARALLEL、下一工序门禁、完整配置和权限范围缺失失败。

GREEN: no-fallback-permission-scope -> PASS，删除运行时记录表权限范围回退；路线表单绑定缺少 `permissionScopeId` 时批次创建失败。

REGRESSION: `MesProEdhrBatchExecutionServiceTest` 全量回归 -> PASS，74 tests，0 failures，0 errors。

GREEN: task-closeout-cleanup -> PASS，仅保留 `task.md` 与 `execution-log.md`，后端一次性证据文件已清理。
