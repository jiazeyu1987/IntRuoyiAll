# DCC 流程路线删除 API

## Task Goal

为 DCC 审批路线提供删除当前路线版本的正式后端 API，供前端行级删除操作使用；删除范围仅限当前路线版本及其节点，不清空类别、不恢复旧版本。

## Milestones

- [x] M1 记录 BDD/TDD 证据与后端 API 门禁。
- [x] M2 增加后端删除路线服务测试，先证明能力缺失。
- [x] M3 实现 Controller、Service、事务删除逻辑。
- [x] M4 运行后端单测与契约验证。
- [x] M5 完成任务证据和收尾记录。

## Expected Verification

- `mvn.cmd -pl yudao-module-dcc "-Dtest=DccApprovalRouteAdminServiceImplTest" test`
- 后端 API evidence validator

## 经验门禁

- PowerShell/Windows shell：设置 UTF-8 后读取中文文件；Maven `-D` 参数必须加引号；不得使用 `&&`。
- DCC 审核矩阵/审批路线：不得切换、覆盖或改写 live 审核矩阵版本；本任务只新增当前路线版本删除 API。
- No fallback：删除不存在路线必须抛 `APPROVAL_ROUTE_NOT_EXISTS`；删除后不自动启用旧版本。
- 数据库 schema：不新增表字段、不改菜单 SQL、不改权限标识。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，补正式后端 API 与事务删除，并覆盖服务测试。
- 是否存在临时补丁或绕过：否。

## Verification Evidence

- RED：`mvn.cmd -pl yudao-module-dcc "-Dtest=DccApprovalRouteAdminServiceImplTest" test` 初跑失败，缺少 `deleteRoute(Long)`。
- GREEN：同命令复跑通过，`Tests run: 10, Failures: 0, Errors: 0, Skipped: 0`。

## Current Status

completed
