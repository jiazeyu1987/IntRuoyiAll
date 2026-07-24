# 任务：Smart Release Deploy Precheck Resource Proof Gate

## Goal

让部署前检查读取并校验发布包内的 `resourceDeltaProofPath`，避免只声明一个路径就通过资源门禁。只有状态为 `completed_verified` 的 resource delta proof 才能作为部署前资源证明；`plan-only`、`passed`、`failed`、`conflict`、缺字段或 JSON 非法都必须 fail fast。

## Scope

- 更新 `DeployPrecheckReport.psm1` 的资源门禁。
- 读取发布包内 `resources.resourceDeltaProofPath` 指向的 proof JSON。
- 校验 proof 状态、summary、错误列表和相对路径。
- 新增 report-only 测试覆盖缺 proof、未验证 proof、failed proof、合法 `completed_verified` proof。
- 保持现有 report-only 入口，不执行远程写操作。

## Non-Scope

- 不执行资源同步、上传、下载、删除或覆盖。
- 不连接测试服、正式服、备份服或 MinIO/NAS。
- 不实现 resource delta executor。
- 不把 `plan-only` proof 当作部署已验证 proof。
- 不修改 `publish-int-ruoyi.ps1` 的真实发布路径。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；proof 缺失、非法、未验证或失败时不降级为通过。
- `是否从根因和长期维护角度解决`：是；部署前资源门禁从“字段存在”升级为“证明内容可校验”。
- `是否存在临时补丁或绕过`：否；本阶段只做本地 proof gate，不接管执行器。

## BDD 场景

- BDD: plan-only proof 不能通过部署资源门禁 -> Given manifest 声明 `resourceDeltaPrepared=true` 且 proof status 为 `passed` / When 运行 deploy-precheck report-only / Then precheck failed，code=`DEPLOY_RESOURCE_DELTA_NOT_VERIFIED`。
- BDD: failed proof 不能通过部署资源门禁 -> Given proof status 为 `failed` 且包含 conflict errors / When 运行 deploy-precheck report-only / Then precheck failed，code=`DEPLOY_RESOURCE_DELTA_PROOF_FAILED`。
- BDD: completed_verified proof 可以通过资源门禁 -> Given proof status 为 `completed_verified` 且 summary 无 conflict / When 运行 deploy-precheck report-only / Then资源检查 passed，不产生资源错误。
- BDD: proof 路径必须在包内且 JSON 可解析 -> Given `resourceDeltaProofPath` 指向缺失或非法 JSON / When 运行 deploy-precheck report-only / Then precheck failed，错误包含 impact 和 nextStep。

## Milestones

- [x] M1：任务文档和 BDD/TDD 验收边界。
- [x] M2：RED 测试。
- [x] M3：资源 proof 内容校验实现。
- [x] M4：GREEN/REGRESSION 验证和证据记录。
- [x] M5：独立验证、清理预览和提交。

## Expected Verification

- `python -X utf8 -m pytest script/tests/test_release_deploy_precheck_report_only.py -q`
- `python -X utf8 -m pytest script/tests/test_release_resource_delta_planner.py script/tests/test_release_deploy_precheck_report_only.py script/tests/test_release_manifest_validator.py -q`

## Current Status

completed

## Completed Work

- `DeployPrecheckReport.psm1` 现在读取并校验包内 `resources.resourceDeltaProofPath`。
- proof 路径必须解析到发布包内部，缺失或非法 JSON 失败。
- proof `status=failed`、存在 `errors` 或 `conflictObjects>0` 失败。
- proof 只有 `status=completed_verified` 才能通过部署前资源门禁；`plan-only` / `passed` 不再被当作可部署资源证明。
- 未新增远程连接、上传、下载、删除、镜像或覆盖操作。

## Final Verification

- `python -X utf8 -m pytest script/tests/test_release_deploy_precheck_report_only.py -q` -> PASS, 22 passed.
- `python -X utf8 -m pytest script/tests/test_release_deploy_precheck_report_only.py script/tests/test_release_resource_delta_planner.py script/tests/test_release_manifest_validator.py script/tests/test_release_intake_report_only.py -q` -> PASS, 53 passed.
- Independent verifier subagent `019e989a-e1f8-7da2-b1a3-eadfa4007e27` -> PASS, no file changes, confirmed completed_verified-only proof gate.
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260606-smart-release-deploy-precheck-resource-proof --mode preview` -> PASS, no delete, blocked, or warnings.
