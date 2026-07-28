# Backend API Evidence

## Scope

- Endpoint: `/mes/pro/route/flow-config/batch-record-attachment-owners`
- Endpoint: `/mes/pro/route/flow-config/batch-record-attachment-owners/init-defaults`
- Endpoint: `/mes/pro/route/flow-config/batch-record-attachment-owners/save`
- Service: `MesProRouteFlowConfigService`

## Contract

- 读取返回 4 个固定附件项：来料检报告、灭菌报告、成品检报告、成品检记录。
- 初始化创建或复用默认角色：来料检报告上传1、灭菌报告上传1、成品检报告上传1、成品检记录上传1。
- 初始化只从当前租户启用用户中分配，每个角色 2-4 人；不足 2 人 fail fast。
- 保存校验 USERS 来源必须全部属于当前租户启用用户，ROLE 来源必须通过角色校验。
- 配置写入候选路线版本 `configSnapshots.batchRecordAttachmentOwners`，不新增 DB 表。

## BDD

- BDD: 当前租户启用用户随机授权 -> Given 当前租户至少 2 个启用用户，When 初始化批记录附件默认角色，Then 每个角色分配 2-4 个当前租户启用用户。
- BDD: 启用用户不足失败 -> Given 当前租户启用用户少于 2 个，When 初始化默认角色，Then 返回明确业务错误。
- BDD: 保存校验 -> Given 用户保存 USERS 来源负责人，When 存在非当前租户启用用户，Then 保存失败且不写入快照。

## Permissions

- Query: `mes:pro-route:batch-record-config:query`
- Update: `mes:pro-route:batch-record-config:update`

## Verification

- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesProRouteBatchRecordAttachmentOwnerServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，批记录附件负责人后端合同未实现。
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProRouteBatchRecordAttachmentOwnerServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，3 tests / 0 failures / 0 errors。
- `mvn -pl yudao-module-mes -am "-Dtest=MesProRouteBatchRecordAttachmentOwnerServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS。

## Validation

- 目标 JUnit 已覆盖角色创建、启用用户数量下限、负责人用户范围校验和快照保存。
- 证据脚本校验范围：backend API contract、BDD、RED/GREEN 和验证命令。

## Blockers

- 无后端目标测试 blocker。
