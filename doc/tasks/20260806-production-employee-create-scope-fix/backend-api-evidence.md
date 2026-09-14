# 新建生产人员负责范围后端证据

## Scope

范围限定在 MES 工序池生产组长工作台后端：生产人员档案创建/关联、班组长负责范围校验服务，以及工序绑定和报工复核相邻权限路径。

## Contract

API 合同保持不变：`POST /mes/pro/process-pool/team-leader/employee-profile/temporary/create` 创建临时工，`POST /mes/pro/process-pool/team-leader/employee-profile/formal/link` 关联正式工，`POST /mes/pro/process-pool/team-leader/process-employee-binding/save` 保存工序员工关系。新增人员接口不要求员工已在负责范围；工序绑定接口仍要求工序在班组长负责范围。

## Validation

新增 `PRO_PROCESS_POOL_TEAM_TARGET_SCOPE_DENIED`，错误信息为 `班组长不在该{}的负责范围内`；员工、工序、产线、设备、订单范围拒绝分别返回明确目标。无 schema、迁移或配置变更。

## BDD

BDD: 新建正式工档案 -> Given 班组长选择一个全量系统用户作为正式工；When 提交新增生产人员档案但尚未绑定工序；Then 创建当前班组长名下生产人员档案，不校验该员工是否已在负责员工范围内。

BDD: 新建临时工档案 -> Given 班组长录入临时工姓名和签名密码；When 提交新增临时工档案；Then 创建当前班组长名下临时工档案，不校验该员工是否已在负责员工范围内。

BDD: 工序绑定仍受控 -> Given 班组长将生产人员绑定到某个工序；When 该工序不在班组长负责范围；Then 返回 `班组长不在该工序的负责范围内`。

BDD: 报工复核仍受控 -> Given 班组长查看或复核非负责员工的报工；When 请求列表、详情、复核或确认；Then 继续返回负责范围错误。

## RED

RED: `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderScopeServiceTest,MesTeamLeaderRuntimeConfigServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, 新增测试引用的 `PRO_PROCESS_POOL_TEAM_TARGET_SCOPE_DENIED` 未实现。

## GREEN

GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderScopeServiceTest,MesTeamLeaderRuntimeConfigServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, Tests run: 18, Failures: 0, Errors: 0, Skipped: 0。

## Verification

- REGRESSION: `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderScopeServiceTest,MesTeamLeaderRuntimeConfigServiceTest,MesTeamEmployeeBindingServiceTest,MesTeamLeaderReportConfirmationServiceTest,MesTeamLeaderSubmissionReviewServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, Tests run: 35, Failures: 0, Errors: 0, Skipped: 0。
- Observability: 错误响应仍通过统一 `ServiceException` 返回业务错误码和消息，不吞异常、不返回默认成功。

## Blockers

无代码或测试阻塞；提交/推送阶段受当前仓库大量非本任务脏改动影响，需单独处理任务所有权边界。
