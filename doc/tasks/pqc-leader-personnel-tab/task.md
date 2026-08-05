# PQC组长人员管理 Tab

## Task Goal

为 `PQC组长` 工作台增加 `人员管理` 功能 tab，支持当前 PQC 组长关联正式 PQC 检验员，并在标准列表中展示、启用和禁用关联人员。

## Milestones

- [x] 建立 BDD / TDD 任务证据和 RED 契约
- [x] 实现后端 PQC 人员 scope 管理 API
- [x] 实现前端 PQC 人员管理 tab、列表和新增弹窗
- [ ] 修复 `48081` 旧 Jar 导致的认证态接口不存在
- [x] 运行后端聚焦验证并记录结果
- [ ] 收尾清理并更新任务状态

## Expected Verification

- 前端静态合同：`node tests/e2e/pqc-leader-personnel-tab-static.spec.js`
- 既有相邻前端合同：`node tests/e2e/pqc-leader-module-tabs-static.spec.js`、`node tests/e2e/production-leader-function-tabs-static.spec.js`
- 后端目标单测：`mvn -pl yudao-module-mes -am "-Dtest=MesPqcLeaderPersonnelServiceTest,MesTeamLeaderScopeServiceTest,MesProcessPoolTeamLeaderControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- 前端类型检查：`pnpm ts:check`
- 差异检查：`git diff --check -- <task paths>`

## Current Status

in_progress

前后端聚焦验证已通过。用户真实页面发现 PQC personnel 接口不存在；已确认 `48081` 运行的是缺少新增 PQC class 的旧 Jar，隔离构建的新 Jar 已通过关键 class 合同，正在替换旧运行态并完成认证态请求验证。

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
- 本地后端隔离构建 Jar 加载门禁：运行 Jar 必须包含新增 PQC class，未登录 `401` 不作为路由加载证明。
- 主工作区 Maven Target 冲突时的隔离验证 Worktree 门禁：从已提交 HEAD 创建任务自有 detached worktree，不从并行脏主工作区直接打包。
