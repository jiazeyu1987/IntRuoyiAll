# Backend API Evidence

## Scope

生产组长生产人员档案管理后端接口，范围包括当前组长关联员工列表、正式工受限搜索、临时工新增、正式工关联、显示名修改、启禁用、临时工签名密码重置、审计记录查询，以及生产填写运行态员工候选显示名快照。

## Contract

- 列表范围：`GET /mes/pro/process-pool/team-leader/employee-profile/list` 只返回当前登录生产组长关联的生产人员档案。
- 正式工搜索：`GET /employee-profile/formal-candidates?keyword=` 由后端按当前组长允许范围返回候选，不暴露全系统用户列表。
- 临时工：`POST /employee-profile/temporary/create` 只创建生产人员档案，不创建系统登录账号，保存电子签名密码哈希。
- 写操作：正式工关联、显示名修改、启禁用、临时工签名密码重置均要求当前组长上下文与维护权限。
- 运行态：生产填写员工候选只返回当前组长关联且未禁用人员，并携带 `displayName` 供前端员工卡片显示。

## Validation

- 权限：Controller 使用 `mes:pro-process-pool-team-leader:query` 和 `mes:pro-process-pool-team-leader:maintain`。
- 重名：同一生产组长有效员工显示名唯一，失败时返回业务错误并提示加后缀。
- 正式工重复关联：同一生产组长重复关联同一系统用户时，服务层在写库前返回业务错误，避免数据库唯一键冲突暴露为 500。
- 正式工密码：正式工不在本接口设置或重置签名密码，沿用原系统用户电子签名密码。
- 临时工密码：仅临时工允许重置生产人员档案签名密码。
- 审计：新增、关联、禁用、启用、改名、重置密码等动作写入维护审计。

## BDD

- BDD: 当前组长查询关联员工 -> Given 登录用户是生产组长；When 查询员工档案列表；Then 只返回关联当前组长的员工。
- BDD: 正式工受限搜索 -> Given 组长输入姓名关键字；When 调用正式工候选接口；Then 后端只返回允许范围候选。
- BDD: 临时工新增 -> Given 组长录入显示名和签名密码；When 提交；Then 创建生产人员档案且不创建登录账号。
- BDD: 生产填写候选 -> Given 员工已禁用；When 进入新报工选择；Then 禁用员工不再返回，历史快照仍保留。

## RED / GREEN

- RED: `node tests/e2e/production-personnel-management-static.spec.cjs` -> FAIL, 页面缺少生产人员档案标准列表和 scoped API wrapper。
- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderRuntimeConfigServiceTest#shouldRejectDuplicateFormalUserBeforeDatabaseInsert" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, 缺少重复正式工业务错误码。
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderRuntimeConfigServiceTest#shouldRejectDuplicateFormalUserBeforeDatabaseInsert" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, Surefire `Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`.
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolTeamLeaderControllerTest,MesProcessPoolTeamLeaderSchemaTest,MesTeamLeaderRuntimeConfigServiceTest,MesFrontlineRuntimeConfigServiceTest,MesFrontlineRuntimeConfigControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, Tests run: 30, Failures: 0, Errors: 0, Skipped: 0.

## Verification

- Controller、Service、schema、运行态候选目标 JUnit 已通过。
- 前端静态合同确认未调用 `/system/user/page`。
- 真实页面 E2E 已通过：正式工搜索关联、临时工新增、重复名拒绝、绑定、密码重置、禁用和审计均走真实页面路径。

## Blockers

- 无当前 blocker。
