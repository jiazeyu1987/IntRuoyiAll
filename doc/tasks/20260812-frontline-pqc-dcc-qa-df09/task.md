# 20260812-frontline-pqc-dcc-qa-df09

## Task Goal

完善一线 PQC 待检任务 overlay 与生产提交候选的后端准入规则，确保 PQC 任务按正式 active order、规程版本、QA 工序与检验规则键精确归属，并确保生产提交候选只来自 active-order process snapshot。

## Milestones

- [x] 建立任务文档与 BDD/TDD 证据。
- [x] 添加 RED 测试覆盖 PQC overlay 精确匹配、未创建状态、检验类型隔离、稳定业务排序，以及生产提交候选 active-order process snapshot 归属。
- [x] 最小实现后端 mapper / overlay / candidate 逻辑并通过 GREEN。
- [x] 运行 diff、禁止项扫描与 backend-api evidence validator。
- [x] 标记 ready_for_closeout。

## Expected Verification

- `cd D:\IntRuoyiWorktree\20260812-frontline-pqc-dcc-qa-df09\IntRuoyiBackend; mvn -pl yudao-module-mes -am "-DskipITs" "-Dtest=MesFrontlinePqcTaskOverlayTest,MesFrontlineProductionSubmitCandidateTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `git diff --check`
- 禁止项扫描：确认未过滤 QA 工序 / 检验项目，未推算 QA，未校验 QA 工序与 MES 路线工序存在性，未引入 fallback / 兼容 / 默认成功。
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260812-frontline-pqc-dcc-qa-df09/backend-api-evidence.md`

## Current Status

ready_for_closeout

## Design Constraints Check

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，围绕正式 active-order process snapshot 与 PQC 任务正式身份字段补齐归属判定。
- 是否存在临时补丁或绕过：否。

## Applicable Experience Gates

- PQC 待检准入与工序选择必须分离：PQC 任务只附着正式上下文，不用 QA 检验项目重复工序，不用路线额外工序扩大列表。
- MES PQC 项目级检验快照门禁：待检列表应保留具备正式工序身份的 PENDING 任务，缺失正式身份必须暴露而非默认成功。
- PowerShell Maven `-D` 参数门禁：Windows PowerShell 下 Maven `-D` 参数使用引号传递。

## Verification Evidence

- RED: 指定 Maven 命令 FAIL，原因是新增测试引用的 `MesFrontlinePqcTaskOverlay` 尚不存在。
- Supervisor RED: 独立补充稳定排序场景后，指定 Maven 命令 FAIL，原因是 overlay 输出保持输入顺序，实际 [1004, 1003, 1001, 1002]，期望 [1001, 1002, 1003, 1004]。
- GREEN: 指定 Maven 命令 PASS，`Tests run: 6, Failures: 0, Errors: 0, Skipped: 0`。
- `git diff --check`: PASS。
- 禁止项扫描：PASS，未发现越界文件、formBindings、产品/物料/路线推算 QA、QA 工序/检验项目过滤、QA 与 MES 路线工序存在性校验、fallback/兼容/默认成功。
- backend-api evidence validator: PASS，`Backend API evidence is valid.`

## Ready For Closeout

- 实现与要求的验证已完成；cleanup / 合并 / worktree 删除不属于本子任务范围。

## Remaining Blockers

- 暂无。
