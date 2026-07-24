# 任务：修复 NAS 转移权限快照未就绪错误

## 任务目标

定位并修复测试服务器 NAS 管理点击转移时提示 `权限快照或恢复接口返回错误`、后端返回 `DCC NAS permission snapshot is not ready for task: 4` 的问题，确保转移流程不会在权限快照尚未采集完成时错误触发恢复流程，同时保持缺失真实快照时 fail-fast。

## 前序任务检查

- 上一任务 `doc/tasks/20260601-release-package-nas-disconnect-2250/task.md` 当前为 `completed`。
- 当前仓库已有未提交改动，本任务提交时只纳入本任务直接产生的文档、测试与修复文件。

## BDD 场景

BDD: NAS 转移完成但权限快照未就绪时不应误报转移失败 -> Given 管理员在 NAS 管理中提交转移任务 / When 文件转移任务已创建但权限快照采集仍在进行或尚未生成 / Then 转移接口和轮询接口应返回可展示的快照状态，不应把恢复接口的未就绪异常作为转移失败。

BDD: 缺少真实权限快照时恢复必须 fail-fast -> Given 管理员显式执行权限恢复 / When 对应转移任务不存在已采集完成的权限快照 / Then 后端必须返回明确的未就绪错误，不能生成空计划、默认成功或静默跳过。

## 里程碑

- [x] M1：建立任务文档并记录用户提供的测试服错误。
- [x] M2：定位本地后端 NAS 转移、权限快照和恢复接口实现。
- [x] M3：新增失败回归测试并记录 RED。
- [x] M4：最小修复转移流程与权限恢复调用边界。
- [x] M5：运行 targeted 回归验证，记录 GREEN 与风险。
- [x] M6：执行收尾清理预览并按验证结果单独提交本任务改动。

## 预期验证

- RED：新增或更新后端测试，复现 NAS 转移状态查询在快照未就绪时触发 `DCC NAS permission snapshot is not ready`。
- GREEN：对应测试通过，确认转移创建/状态查询不会误触发恢复，显式恢复仍在快照未就绪时失败。
- REGRESSION：运行受影响模块测试，确认 NAS 转移、权限快照查询和恢复预览边界不被放宽。

## 当前状态

status: completed

## Current Status

completed

## 当前结果

- 根因：NAS 转移结果区挂载的权限恢复面板会在 `taskId` 出现后自动请求权限快照摘要；后端摘要查询在转移任务存在但 ACL 快照尚未生成时直接抛 `DCC_NAS_PERMISSION_SNAPSHOT_NOT_READY`，导致前端显示“权限快照或恢复接口返回错误”。
- 修复：权限快照摘要接口在转移任务存在但快照尚未生成时返回 `NOT_COLLECTED` 与 0 计数；只有转移任务和快照都不存在时继续 fail-fast。前端先加载摘要，只有 `CAPTURED`/`FAILED` 等真实快照状态后才请求明细和未映射主体，恢复预览只允许 `CAPTURED` 后触发。
- 测试服发布：本任务按默认本机修改策略完成本地修复和验证，尚未操作测试服务器；测试服需后续明确授权后发布。

## 验证记录

- RED：`mvn --% -pl yudao-module-dcc -am -Dtest=DccNasPermissionSnapshotQueryServiceImplTest#getSummary_returnsNotCollectedStatusWhenTransferTaskHasNoSnapshotYet -Dsurefire.failIfNoSpecifiedTests=false test` -> FAIL，摘要接口仍抛 `DCC_NAS_PERMISSION_SNAPSHOT_NOT_READY`。
- GREEN：同一后端单测 -> PASS。
- GREEN：`mvn --% -pl yudao-module-dcc -am -Dtest=DccNasPermissionSnapshotQueryServiceImplTest,DccNasPermissionRestoreServiceTest,DccControlledFileNasTransferServiceTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS，36 tests。
- RED：`node tests/e2e/dcc-nas-permission-restore-static.spec.js` -> FAIL，前端不识别 `NOT_COLLECTED` 且缺少摘要优先门禁。
- GREEN：`node tests/e2e/dcc-nas-permission-restore-static.spec.js` -> PASS。
- GREEN：`$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS。默认 `pnpm ts:check` 先因 Node 约 4GB heap OOM 退出，增加 heap 后同一类型检查通过。
- GREEN：bug regression evidence validator -> PASS。
- GREEN：task-closeout-cleanup preview -> PASS，后端 delete `bug-regression-evidence.md`、blocked `<none>`；前端 delete `<none>`、blocked `<none>`。
- RED：task-closeout-cleanup apply -> BLOCKED，脚本未识别中文状态段落中的完成状态，已补充 `Current Status: completed`。
- GREEN：task-closeout-cleanup apply -> PASS，后端删除 `bug-regression-evidence.md`，前端无删除项。

## 阻塞

None.
