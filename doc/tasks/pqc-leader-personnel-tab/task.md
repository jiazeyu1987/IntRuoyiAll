# PQC组长人员管理 Tab

## Task Goal

为 `PQC组长` 工作台增加 `人员管理` 功能 tab，支持当前 PQC 组长关联正式 PQC 检验员，并在标准列表中展示、启用和禁用关联人员。

## Milestones

- [x] 建立 BDD / TDD 任务证据和 RED 契约
- [x] 实现后端 PQC 人员 scope 管理 API
- [x] 实现前端 PQC 人员管理 tab、列表和新增弹窗
- [ ] 运行聚焦验证并记录结果
- [ ] 收尾清理并更新任务状态

## Expected Verification

- 前端静态合同：`node tests/e2e/pqc-leader-personnel-tab-static.spec.js`
- 既有相邻前端合同：`node tests/e2e/pqc-leader-module-tabs-static.spec.js`、`node tests/e2e/production-leader-function-tabs-static.spec.js`
- 后端目标单测：`mvn -pl yudao-module-mes -am "-Dtest=MesPqcLeaderPersonnelServiceTest,MesTeamLeaderScopeServiceTest,MesProcessPoolTeamLeaderControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- 前端类型检查：`pnpm ts:check`
- 差异检查：`git diff --check -- <task paths>`

## Current Status

blocked

前端聚焦合同和类型检查已通过；后端目标 Maven 连续两次未在超时窗口内到达 Surefire，尚不能确认后端 JUnit GREEN。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否
- `是否从根因和长期维护角度解决`：是，PQC 组员关系写入正式 `team_leader_scope`，不复用生产人员档案表
- `是否存在临时补丁或绕过`：否

## Applicable Gates

- 前端静态契约隔离门禁：新增任务专用合同，不用无关历史失败作为通过或阻塞依据。
- 统一列表复合工具栏布局门禁：PQC 人员列表使用 `UnifiedListTemplate`，新增按钮和启用状态筛选必须在真实 actions 区。
- Vue Scoped Slot 静态合同门禁：静态合同允许具名 slot 合法属性，不为测试破坏模板结构。
- MES 生产人员档案正式工重复关联门禁：PQC 正式检验员也必须先业务拒绝重复关联，不依赖数据库异常。
- MES PQC 项目级检验快照门禁：PQC 组长复核可见范围使用正式 PQC scope，不用前端文案或 raw payload 替代正式责任范围。
