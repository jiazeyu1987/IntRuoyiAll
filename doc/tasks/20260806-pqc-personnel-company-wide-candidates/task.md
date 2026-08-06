# PQC Personnel Company-Wide Candidates

## Task Goal

将 `PQC组长 > 人员管理 > 新增 PQC 检验员` 的候选范围调整为全公司正式系统用户搜索，提交关联校验与候选查询保持同一范围。

## Milestones

- [x] 建立任务记录和 BDD/TDD 验收口径
- [ ] 补充 RED 契约覆盖 PQC 全公司候选范围
- [ ] 实现后端候选查询与提交校验同范围
- [ ] 运行聚焦验证并记录结果
- [ ] 收尾清理并更新任务状态

## Expected Verification

- `node tests/e2e/pqc-leader-personnel-company-wide-candidates-static.spec.js`
- `mvn -pl yudao-module-mes -am "-Dtest=MesPqcLeaderPersonnelServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `git diff --check -- <task paths>`

## Current Status

in_progress

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否
- `是否从根因和长期维护角度解决`：是，候选查询和提交关联校验统一使用全公司系统用户范围
- `是否存在临时补丁或绕过`：否

## Applicable Gates

- MES 生产人员档案正式工重复关联门禁：候选查询与提交关联校验必须同范围，不允许只放开下拉。
- 前端静态契约隔离门禁：使用本任务专用最小静态契约覆盖 PQC 候选范围变化。
