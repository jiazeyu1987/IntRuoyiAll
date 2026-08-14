# PQC Personnel Company-Wide Candidates

## Task Goal

将 `PQC组长 > 人员管理 > 新增 PQC 检验员` 的候选范围调整为全公司正式系统用户搜索，提交关联校验与候选查询保持同一范围。

## Milestones

- [x] 建立任务记录和 BDD/TDD 验收口径
- [x] 补充 RED 契约覆盖 PQC 全公司候选范围
- [x] 实现后端候选查询与提交校验同范围
- [x] 运行聚焦验证并记录结果
- [ ] 收尾清理并更新任务状态

## Expected Verification

- `node tests/e2e/pqc-leader-personnel-company-wide-candidates-static.spec.js`
- `node tests/e2e/pqc-leader-personnel-tab-static.spec.js`
- `mvn -pl yudao-module-mes -am "-Dtest=MesPqcLeaderPersonnelServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `mvn -pl yudao-server -am "-DskipTests" package`
- `pnpm ts:check`
- `git diff --check -- <task paths>`

## Current Status

blocked

## Blocker

- `pnpm ts:check` 失败于当前工作区已有活跃订单类型不一致：`TeamLeaderWorkbenchPage.vue(3760,7)` 传入 `routeId`，但 `TeamLeaderActiveOrderAddReqVO` 类型中不存在该字段。PQC 全公司候选逻辑、静态契约、MES 目标 Maven 测试和独立 worktree 后端打包均已通过。
- 当前本机 `48081` 运行态仍加载 `E:\IntRuoyi\output\runtime\int_main\backend-runtime-production-formal-users-20260806.jar`，只读内嵌 MES class 检查仍可见旧 `getUserListBySubordinate` / `PRO_PROCESS_POOL_TEAM_SCOPE_DENIED` 信号；因此页面要立即生效还需要获得运行态替换/重启授权。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否
- `是否从根因和长期维护角度解决`：是，候选查询和提交关联校验统一使用全公司系统用户范围
- `是否存在临时补丁或绕过`：否

## Applicable Gates

- MES 生产人员档案正式工重复关联门禁：候选查询与提交关联校验必须同范围，不允许只放开下拉。
- 前端静态契约隔离门禁：使用本任务专用最小静态契约覆盖 PQC 候选范围变化。
