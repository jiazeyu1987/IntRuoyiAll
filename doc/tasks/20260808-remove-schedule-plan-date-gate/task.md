# 20260808 Remove Schedule Plan Date Gate

## Task Goal

去除生产组长活跃订单/PQC 任务生成中“排产工序缺少计划日期”的阻断限制；排产工序计划日期为空时仍允许候选可加入并生成 PQC 任务。

## Milestones

- [x] 定位计划日期门禁及相邻业务规则
- [x] 补充 BDD 与 RED 回归测试
- [x] 最小化移除门禁并保持 PQC 日期可追溯
- [x] 运行定向验证并记录结果
- [x] 收尾更新任务文档

## Expected Verification

- `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderActiveOrderServiceTest" test`

## Current Status

completed

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否
- `是否从根因和长期维护角度解决`：是，目标是删除计划日期作为阻断条件并明确业务日期来源
- `是否存在临时补丁或绕过`：否

## Experience Gate

- 适用门禁：`docs/backend-development.md#零排产活跃订单必须使用发布态正式路线`。
- 摘要：活跃订单候选/新增必须复用同一路线来源解析契约；正式 PQC 规程缺失、路线/版本不唯一、工序/数量快照不完整仍 fail-fast。已有一条有效排产时优先使用工序 `planDate`；`planDate` 为空时按用户要求不再阻断，PQC 业务日期使用已落库活跃订单 `joinedAt` 日期；不能引入默认路线、默认 QA、空成功或吞异常。
- Maven 门禁：PowerShell 中 Maven `-D` 参数整体加双引号，并使用 `-pl yudao-module-mes -am` 避免 reactor 依赖陈旧。

## Verification Result

- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderActiveOrderServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，29 tests, 0 failures, 0 errors.
- Static check: `rg "排产工序缺少计划日期|requireBusinessDate|resolvePqcBusinessDate" ...` -> 生产服务仅保留 `resolvePqcBusinessDate`，旧错误文案和 `requireBusinessDate` 已不再命中。
- Diff check: `git diff --check -- <task-owned paths>` -> PASS，仅提示 Git 未来可能 CRLF 归一化。
- Cleanup: `task_closeout.py --task-id 20260808-remove-schedule-plan-date-gate --mode preview/apply` -> PASS，无删除项、无阻塞、无告警。
