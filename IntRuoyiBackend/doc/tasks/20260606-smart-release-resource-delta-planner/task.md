# 任务：Smart Release Resource Delta Planner

## Goal

实现 Smart Release 本地资源增量计划工具，为 DCC、展厅和通用 `infra_file` 的大文件、多文件场景提供可验证的 resource delta proof。目标是在 10000 个既有对象中只计划新增或变化对象，不做全量复制、不物理删除、不自动覆盖冲突对象，也不连接外部服务器。

## Scope

- 新增本地 resource delta planner 命令和模块。
- 输入 source resource reference manifest 与 target resource index fixture。
- 输出 resource delta proof JSON，包含 copy、verify-only、conflict、tombstone、orphaned 统计和对象明细。
- 支持 10000 对象规模的本地算法测试。
- 为后续 deploy-precheck blocking 消费 `resourceDeltaProofPath` 做准备。

## Non-Scope

- 不上传、下载、删除或覆盖 MinIO/NAS 对象。
- 不修改远程测试服、正式服或备份服。
- 不改数据库数据。
- 不接管 deploy executor。
- 不实现 artifact cache 或 smart build 复用。
- 不把测试服、正式服、备份服 IP 写入代码或配置。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；缺输入、JSON 非法、sha256/size 缺失、同 key 不同 sha256 冲突均 fail fast 或输出 failed proof。
- `是否从根因和长期维护角度解决`：是；把资源引用和目标索引变成可验证 delta proof，后续部署只消费证明，不靠全桶 mirror 或人工判断。
- `是否存在临时补丁或绕过`：否；本阶段只新增本地工具与测试，不改远程发布行为。

## BDD 场景

- BDD: 10000 对象只新增 3 个 -> Given target resource index 已有 10000 个对象且 source reference manifest 只新增 3 个 / When 运行 resource delta planner / Then proof 中 `copyObjects=3`、`verifyOnlyObjects=10000`，且没有 full mirror、delete 或 overwrite 操作。
- BDD: 同 objectKey 不同 sha256 进入冲突 -> Given source 与 target 有相同 `storageProfileId/bucket/objectKey` 但 sha256 不同 / When 运行 planner / Then proof 状态为 `failed`，对象进入 `conflictObjects`，不进入 copy 或 verify-only。
- BDD: 目标存在但 source 不再引用只产生 tombstone -> Given target index 有对象但 source reference manifest 不再引用 / When 运行 planner / Then 对象进入 `tombstoneObjects`，proof 不包含物理删除动作。
- BDD: 资源引用缺关键字段失败 -> Given source reference 缺 `bucket`、`objectKey`、`sha256` 或 `size` / When 运行 planner / Then 命令失败并输出明确 errorCode、impact、nextStep。

## Milestones

- [x] M1：任务文档和 BDD/TDD 验收边界。
- [x] M2：RED 测试和 fixtures。
- [x] M3：resource delta planner 命令和模块。
- [x] M4：GREEN/REGRESSION 验证和证据记录。
- [x] M5：独立验证、清理预览和提交。

## Expected Verification

- `python -X utf8 -m pytest script/tests/test_release_resource_delta_planner.py -q`
- `python -X utf8 -m pytest script/tests/test_release_manifest_validator.py script/tests/test_release_deploy_precheck_report_only.py script/tests/test_release_intake_report_only.py -q`

## Current Status

completed

## Completed Work

- 新增 `script/release/run-resource-delta-plan.ps1` plan-only 入口。
- 新增 `script/release/lib/ResourceDeltaPlanner.psm1`，本地读取 source references 与 target objects，按 `storageProfileId + bucket + objectKey` 计算 copy、verify-only、conflict、tombstone。
- 缺必填字段与同 key 不同 `sha256`/`size` 均输出 failed proof 并返回 exit 2。
- plan-only proof 不包含 `deleteObjects`、`overwriteObjects`，不执行任何远程命令。

## Final Verification

- `python -X utf8 -m pytest script/tests/test_release_resource_delta_planner.py -q` -> PASS, 4 passed.
- `python -X utf8 -m pytest script/tests/test_release_resource_delta_planner.py script/tests/test_release_manifest_validator.py script/tests/test_release_deploy_precheck_report_only.py script/tests/test_release_intake_report_only.py -q` -> PASS, 49 passed.
- Independent verifier subagent `019e9890-a2bc-7b93-9f05-8ea90798194c` -> PASS, no file changes, confirmed local plan-only behavior and required coverage.
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260606-smart-release-resource-delta-planner --mode preview` -> PASS, no delete, blocked, or warnings.
