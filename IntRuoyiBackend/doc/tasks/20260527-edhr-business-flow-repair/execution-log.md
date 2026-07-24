# Execution Log

## BDD

BDD: 字段审计权限进入测试租户角色 -> Given 测试租户角色包含 eDHR 菜单, When 初始化或修复 eDHR 字段审计权限, Then 角色和租户套餐必须包含字段审计查询、保存、校验和导出权限。

BDD: 审批归档闭环保留审计证据 -> Given eDHR 草稿存在字段审计 revision/headHash, When 提交和审批关闭, Then 签名、审批快照、追踪和归档源数据必须保留该证据。

## TDD Evidence

RED: `python -X utf8 -m pytest script\tests\test_edhr_field_audit_sql.py -q` -> FAIL, expected reason: 字段审计 SQL 只创建 900027-900030 权限菜单，未提供 `ensure_edhr_field_audit_tenant_package_menus` 将权限合并进已有 eDHR 租户套餐和 tenant_admin 角色。

GREEN: `python -X utf8 -m pytest script\tests\test_edhr_field_audit_sql.py -q` -> PASS, 5 passed。

GREEN: `python -X utf8 -m pytest script\tests\test_edhr_approval_archive_schema_contract_sql.py script\tests\test_edhr_field_audit_sql.py -q` -> PASS, 22 passed。

GREEN: 测试服 SQL 应用 -> PASS, 租户 `122` 的 `tenant_admin` 角色 `111` 已包含字段审计菜单 `900027,900028,900029,900030`。

GREEN: `mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordExecutionFieldAuditControllerTest,MesProBatchRecordExecutionFieldAuditQueryExportServiceTest,MesProBatchRecordExecutionFieldAuditServiceTest,MesProBatchRecordExecutionFieldAuditHashTest,MesProBatchRecordExecutionServiceImplTest,MesProBatchRecordExecutionArchiveServiceImplTest,MesProBatchRecordExecutionSignatureServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 75 tests, 0 failures。

GREEN: `mvn -f ruoyi-vue-pro\pom.xml -pl yudao-server -am -DskipTests package` -> PASS, worktree 后端 jar 构建成功。

GREEN: 测试服后端镜像更新 -> PASS, `intruoyi-backend:20260527_edhr_business_flow_repair` 已在测试服启动并通过健康检查；未同步数据库、未触碰正式服。
