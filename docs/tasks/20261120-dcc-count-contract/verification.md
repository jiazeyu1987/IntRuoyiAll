# DCC计数测试契约修复与套餐边界回归

日期：2026-11-20。验证基线：3368249ef92ce90b7e401a2f916cf57db67c0c74。
本次只修改应用测试契约，不修改业务实现、数据库或服务器，不执行发布。

## 复现与根因

在上述当前提交重新执行，DccApprovalTaskAdapterTest共31项中1项报PotentialStubbingProblem；不是沿用历史报告。
countIncludesNativeAndFlowableTasksWithoutLoadingFileSummaries仍stub二参数todo，而DccApprovalTaskAdapter.pageTodo实际调用带DccApprovalListFilterSql.flowable(false)字段映射的三参数todo。
修正该处后再次执行，暴露第二处旧契约：原生项目/培训来源已经由list切换为page，旧stub导致总数319而非321。

## 修复与验收

- 精确stub及verify三参数todo，比较实际完整筛选字段映射；保留MockitoExtension严格stub，不添加lenient或忽略失败。
- 原生项目与培训以page返回空行、总数各1，模拟纯计数；验证两个page与Flowable使用相同零limit上下文，验证不调用旧list。
- 保留321总数、空结果集及不读取流程实例/文件摘要/类别断言。

## 本轮实际验证

RED日志：.runtime/delegation-20261120-current-tests.log（原错误），.runtime/delegation-20261120-targeted-green.log（进一步发现旧list契约）。
最终GREEN日志：.runtime/delegation-20261120-final-green.log，BUILD SUCCESS，共58项，0失败、0错误、0跳过：

| 测试类 | 项数 |
| --- | ---: |
| DccApprovalTaskAdapterTest | 31 |
| TenantAdminPackageBoundaryTest | 3 |
| TenantAdminBusinessPermissionTest | 3 |
| TenantAdminBusinessPolicyTest | 4 |
| ErpDataRolePermissionTest | 15 |
| PermissionApiEntitlementTest | 2 |

命令（IntRuoyiBackend目录）：

```powershell
mvn -pl yudao-module-dcc -am '-Dtest=DccApprovalTaskAdapterTest,TenantAdminPackageBoundaryTest,TenantAdminBusinessPermissionTest,TenantAdminBusinessPolicyTest,ErpDataRolePermissionTest,PermissionApiEntitlementTest' '-Dsurefire.failIfNoSpecifiedTests=false' test
```

套餐边界3项分别覆盖SRM/CRM/AI：实际组合TenantServiceImpl、MenuServiceImpl、PermissionServiceImpl及AuthController，在存在旧角色菜单授权时执行套餐排除→纳入→排除；检查菜单响应、权限响应、直接鉴权和按角色鉴权，并确认MES菜单/权限仍保留。它是服务组合测试，不是HTTP/Playwright真实页面验证；本轮未做服务器验收。

## 额外回归仍失败，不能宣称全权限套件通过

扩大检查日志：.runtime/delegation-20261120-green-tests.log。
AuthTemporaryPermissionProjectionTest共6项中2项NullPointerException，手工组装PermissionServiceImpl未注入menuService；PermissionServiceTest共49项中14项失败，包含角色分配限制、角色菜单及部门数据权限断言。此次不通过放宽stub、改业务权限或删测试来消除这些失败；保留为后续独立修复项。上述58项通过不代表这两个额外测试类通过。
