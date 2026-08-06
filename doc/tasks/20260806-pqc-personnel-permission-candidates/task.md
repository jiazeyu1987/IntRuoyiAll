# PQC Personnel Permission Candidates

## Task Goal

将 PQC 组长添加人员弹窗的候选范围从全体员工收敛为拥有 PQC 权限的用户；候选查询和提交关联必须使用同一权限口径，不做前端本地过滤或静默降级。

## Milestones

- [x] 建立任务记录和 BDD/TDD 验收口径
- [x] 定位 PQC 人员候选接口和权限来源
- [x] 用 RED 测试固定“只返回 PQC 权限用户”
- [x] 实现后端正式权限过滤并保持关联校验一致
- [x] 运行聚焦后端/前端验证并记录证据
- [x] 空点击自动加载候选，并标记其它 PQC 组长占用候选为红色禁选
- [ ] 收尾清理、提交和推送

## Expected Verification

- `rg` 定位 PQC personnel candidates 调用链和权限码
- 目标后端单测 RED/GREEN
- 前端静态合同验证不做全量用户本地过滤
- `mvn -pl yudao-module-mes -am "-Dtest=MesPqcLeaderPersonnelServiceTest,MesProcessPoolTeamLeaderControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `pnpm ts:check` 或记录明确阻塞
- `git diff --check -- <task paths>`

## Current Status

ready_for_closeout

已完成本轮功能实现和验证：PQC 新增人员下拉空点击会加载空关键字候选；其它 PQC 组长启用员工 scope 占用的候选会在后端标记 disabled/occupied，前端红色显示且不可选择，提交接口也会再次业务拒绝跨组长占用。

原共享工作区 conflict markers 已清零，目标 Maven、`pnpm ts:check`、PQC 静态合同与 `git diff --check` 均已通过。剩余收尾阻塞：当前分支仍有其它任务文档改动、未跟踪任务目录，且 `int_main` 领先 `origin/int_main` 2 个提交；为避免混入非本任务改动，本任务未执行提交和推送。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否
- `是否从根因和长期维护角度解决`：是，候选来源在后端按正式 PQC 角色分配池取数，提交关联复用同一权限校验
- `是否存在临时补丁或绕过`：否

## Applicable Gates

- MES 生产人员档案正式工重复关联门禁：候选查询与提交关联校验必须使用同一范围，不能只放开或只收窄下拉。
- MES 一线设备账号权限门禁：权限判断走系统标准权限解析，不硬编码账号或岗位 ID；角色 ID 通过正式角色编码解析。
- 前端静态契约隔离门禁：本次仅更新 PQC 候选最小静态合同；全量 `pnpm ts:check` 本轮已通过。
