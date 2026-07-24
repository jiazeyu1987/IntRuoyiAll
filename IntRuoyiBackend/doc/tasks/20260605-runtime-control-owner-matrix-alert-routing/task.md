# 任务：补齐运行控制台责任人矩阵与告警路由

## 任务目标

根据根任务 `D:\ProjectPackage\Int\IntRuoyi\doc\tasks\20260604-dr-recovery-rollback-gap-audit` 的 P1 缺口，补齐 Runtime Control 默认责任人矩阵，至少覆盖容量告警、立即备份、定时备份、恢复、回滚、演练和发布动作。运行时告警不得因为缺少默认责任人矩阵而被阻塞。

## Previous Task Check

- 上一个同服务仓库任务：`doc/tasks/20260605-backend-runtime-base-local-config/task.md`
- 状态：`completed`
- 处理：上一任务已生成本机内部基础镜像 tar/sha256/image id 并写入用户环境变量；本任务不修改发布包构建产物，只处理 Runtime Control owner matrix 与告警路由。

## BDD 场景

- BDD: 默认容量告警责任人可用 -> Given 本机或任一运行环境产生 `storage-capacity-warning` / When 告警服务查找必填责任人 / Then `local/test/backup/prod` 均有默认 `ops-owner`，站内信不因缺少责任人矩阵而阻塞。
- BDD: 默认备份恢复责任人可用 -> Given `backup-now`、`backup-scheduled`、`restore-data`、`rollback-app` 或 `rehearsal` 产生操作/告警 / When Runtime Control 校验责任人 / Then 目标环境有默认必填责任人和升级路径。
- BDD: 显式配置仍可覆盖默认责任人 -> Given 运维人员配置某环境/动作/角色责任人 / When 查询 owner matrix / Then 显式配置覆盖默认 ownerUserId/ownerName，但保持必填语义。

## Milestones

- [x] M1：确认上一任务 completed，确认当前运行时告警缺少 `local/storage-capacity-warning` 责任人。
- [x] M2：写入 RED 测试，证明默认矩阵缺容量/备份/演练路由。
- [x] M3：补齐默认 owner matrix。
- [x] M4：运行 Java/脚本回归和 diff check。
- [x] M5：cleanup 预览并提交本任务改动。

## Expected Verification

- `mvn -pl yudao-module-infra -Dtest=RuntimeOpsResponsibilityServiceImplTest test`
- `mvn -pl yudao-module-infra -Dtest=RuntimeOpsAlertServiceImplTest test`
- `python -m pytest script/tests/test_release_go_no_go_contract_docs.py::test_g10_alert_routing_runbook_defines_webhook_and_evidence_contract script/tests/test_release_go_no_go_contract_docs.py::test_go_no_go_doc_defines_g11_owner_matrix_contract`
- `python -m pytest script/tests/test_release_readiness_g10_g11_contracts.py`
- `git diff --check -- yudao-module-infra/src/main/java/cn/iocoder/yudao/module/infra/service/runtimecontrol/RuntimeOpsResponsibilityServiceImpl.java yudao-module-infra/src/test/java/cn/iocoder/yudao/module/infra/service/runtimecontrol/RuntimeOpsResponsibilityServiceImplTest.java yudao-module-infra/src/test/java/cn/iocoder/yudao/module/infra/service/runtimecontrol/RuntimeOpsAlertServiceImplTest.java doc/tasks/20260605-runtime-control-owner-matrix-alert-routing`

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。缺责任人仍应阻塞；本任务补齐正式默认责任人矩阵。
- `是否从根因和长期维护角度解决`：是。默认矩阵覆盖运行控制台关键动作，显式配置仍可覆盖默认值。
- `是否存在临时补丁或绕过`：否。不修改运行时 JSON，不把告警标记为假成功。

## 当前状态

completed

## Current Status

completed

## Cleanup Keep

- `doc/tasks/20260605-runtime-control-owner-matrix-alert-routing/task.md`
- `doc/tasks/20260605-runtime-control-owner-matrix-alert-routing/execution-log.md`
