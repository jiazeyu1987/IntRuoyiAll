# 任务：Smart Release Resource Delta Local Executor

## Goal

实现本地 resource delta 执行与回读验证工具：读取 plan-only resource delta proof，在本地 source/target object root 之间只复制 `copyObjects`，回读校验 `copyObjects` 和 `verifyOnlyObjects` 的 size/sha256，输出 `status=completed_verified` 的 proof。用于证明 10000 多文件场景下只复制新增对象，不全量镜像、不覆盖冲突、不物理删除 tombstone。

## Scope

- 新增本地执行入口和模块。
- 输入 plan-only proof、source object root、target object root、output path。
- 只对本地临时目录复制文件，不连接远程服务器或 MinIO/NAS。
- 对 `copyObjects` 执行 copy 后 sha256/size readback。
- 对 `verifyOnlyObjects` 只读校验目标文件 sha256/size。
- 对 `tombstoneObjects` 只记录保留，不执行物理删除。
- 缺源文件、目标已存在、sha256/size 不匹配、proof 含 conflict/errors 时 fail fast。

## Non-Scope

- 不实现 MinIO/NAS 远程同步。
- 不接入 `publish-int-ruoyi.ps1` 或 deploy executor。
- 不修改数据库、运行控制台 UI 或服务器配置。
- 不删除、覆盖、镜像或自动修复任何对象。
- 不把测试服、正式服、备份服 IP 写入代码或配置。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；执行前置条件不满足直接 failed proof + exit 2。
- `是否从根因和长期维护角度解决`：是；把“计划只复制 3 个”升级为“执行后读回验证确实只复制 3 个并可作为 deploy-precheck proof”。
- `是否存在临时补丁或绕过`：否；本地执行器是后续 MinIO/NAS adapter 的可测试核心，不作为远程同步临时绕过。

## BDD 场景

- BDD: 10000 目标对象只复制 3 个新增对象 -> Given plan proof 有 10000 个 verify-only 和 3 个 copy objects / When 本地执行器运行 / Then 只新增 3 个目标文件，输出 completed_verified proof，summary.copyObjects=3、summary.verifyOnlyObjects=10000。
- BDD: copy 目标已存在时失败 -> Given plan 要 copy 的 objectKey 在 target root 已存在 / When 本地执行器运行 / Then failed，code=`RESOURCE_DELTA_TARGET_ALREADY_EXISTS`，不覆盖文件。
- BDD: verify-only 目标 sha256 不匹配时失败 -> Given verify-only object 在 target root 内容被篡改 / When 本地执行器运行 / Then failed，code=`RESOURCE_DELTA_READBACK_MISMATCH`。
- BDD: verify-only 目标 size 不匹配时失败 -> Given verify-only object 在 target root 内容 sha256 正确但计划 size 不一致 / When 本地执行器运行 / Then failed，code=`RESOURCE_DELTA_READBACK_MISMATCH`。
- BDD: tombstone 不物理删除 -> Given target root 存在 tombstone object / When 本地执行器运行 / Then object 仍存在，proof 只记录 tombstoneObjects。

## Milestones

- [x] M1：任务文档和 BDD/TDD 验收边界。
- [x] M2：RED 测试。
- [x] M3：本地 resource delta executor 实现。
- [x] M4：GREEN/REGRESSION 验证和证据记录。
- [x] M5：独立验证、清理预览和提交。

## Expected Verification

- `python -X utf8 -m pytest script/tests/test_release_resource_delta_local_executor.py -q`
- `python -X utf8 -m pytest script/tests/test_release_resource_delta_local_executor.py script/tests/test_release_resource_delta_planner.py script/tests/test_release_deploy_precheck_report_only.py -q`

## Current Status

completed

## Completed Work

- 新增本地 resource delta executor 入口和模块。
- 从 plan-only proof 读取 `copyObjects`、`verifyOnlyObjects`、`tombstoneObjects`。
- 对 `copyObjects` 只在目标不存在时复制，复制后读回校验 size/sha256。
- 对 `verifyOnlyObjects` 只读校验目标文件 size/sha256。
- 对 tombstone 只记录、不删除。
- 输出 `status=completed_verified` proof，供 deploy-precheck 的 proof gate 后续消费。

## Final Verification

- `python -X utf8 -m pytest script/tests/test_release_resource_delta_local_executor.py -q` -> PASS, 5 passed.
- `python -X utf8 -m pytest script/tests/test_release_resource_delta_local_executor.py script/tests/test_release_resource_delta_planner.py script/tests/test_release_deploy_precheck_report_only.py -q` -> PASS, 31 passed.
- Independent verifier subagent `019e98ac-1733-7652-ac38-fd75ad2ed6db` -> PASS, no file changes, confirmed local-only behavior and coverage.
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260606-smart-release-resource-delta-local-executor --mode preview` -> PASS, no delete, blocked, or warnings.
