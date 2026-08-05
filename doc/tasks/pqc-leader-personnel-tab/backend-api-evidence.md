# Backend API Evidence

## Endpoint Scope

- 新增 PQC 人员管理 API，全部位于 `/mes/pro/process-pool/team-leader` 下。

## Data Contract

- 创建和维护 `mes_pro_process_pool_team_leader_scope` 记录：
  - `leader_user_id = 当前登录用户`
  - `leader_type = PQC`
  - `scope_type = EMPLOYEE`
  - `employee_user_id = 被关联检验员系统用户ID`
  - `enabled = true/false`

## Auth And Validation

- 查询接口使用 `mes:pro-process-pool-team-leader:query`。
- 候选搜索、关联、状态更新使用 `mes:pro-process-pool-team-leader:maintain`。
- 非当前组长 scope、非 PQC employee scope、重复关联、非下属正式员工均返回业务错误。

## BDD Scenarios

- PQC 关联正式检验员创建正确 scope。
- PQC 列表只返回当前组长的 PQC employee scope。
- 禁用后负责员工范围不包含该检验员。
- 重复关联在 insert 前失败。

## RED

- 已新增目标服务单测；首次 Maven 在到达 Surefire 前超时。

## GREEN

- 实现代码和目标测试已完成，但 Maven 复跑仍在到达 Surefire 前超时，当前不宣称 JUnit PASS。

## Blocker

- 待复跑：`mvn -pl yudao-module-mes -am "-Dtest=MesPqcLeaderPersonnelServiceTest,MesTeamLeaderScopeServiceTest,MesProcessPoolTeamLeaderControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
