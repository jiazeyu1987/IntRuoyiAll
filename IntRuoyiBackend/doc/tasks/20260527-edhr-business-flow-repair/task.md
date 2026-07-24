# 任务：eDHR 非发布功能后端修复

## 任务目标

- 在后端独立 worktree 中修复测试租户可验证的 eDHR 非发布功能问题。
- 聚焦字段审计权限、字段审计链、签名、审批关闭、追踪与归档闸门，不涉及正式发布。

## 里程碑

- [x] M1：创建后端任务文档。
- [x] M2：补充失败测试复现权限或业务闭环缺陷。
- [x] M3：最小化修复后端 SQL、权限或服务逻辑。
- [x] M4：运行目标 SQL/服务回归测试。

## 预期验证

- `python -X utf8 -m pytest script/tests/test_edhr_approval_archive_schema_contract_sql.py script/tests/test_edhr_field_audit_sql.py -q`
- 相关 `mvn -pl yudao-module-mes ... test`

## 当前状态

completed

## 结果

- 修复字段审计权限菜单只创建但未进入已有 eDHR 租户套餐与 tenant_admin 角色的问题。
- 修复脚本对非法 `system_tenant_package.menu_ids` JSON、缺失权限菜单采用失败退出，不做静默降级。
- 后端 SQL 合同、eDHR Java 目标测试与测试服只读 E2E 均已通过。
