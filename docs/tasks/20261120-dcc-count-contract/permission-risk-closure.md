# 套餐权限发布风险闭合复核

日期：2026-11-20（America/New_York）。工作基线：0d0c98f5a。
本轮只修改两个测试类及本记录，不修改业务实现、数据库或服务器；未部署。
本记录更新 verification.md 中“额外回归仍失败”的历史状态。

## 当前复现与受控对照

1. 当前完整代码、原测试重新执行两个类：AuthTemporaryPermissionProjectionTest 6项中2项错误；PermissionServiceTest 49项中13项失败。
2. 同一工作树、相同测试和依赖，仅将 PermissionServiceImpl.java 替换为套餐补丁 cdd37806e 的父提交版本，再执行相同命令：临时授权仍2项错误，通用权限14项失败。结束后以字节级备份恢复当前实现，已验证恢复一致。
3. 这是仅撤除套餐补丁的受控对照，不是把其他代码也回退后的完整历史版本测试。当前与对照差1项是普通角色测试使用 randomPojo 随机status，可能碰巧启用；不作为套餐补丁影响证据。
4. cdd37806e 对该实现的差异仅在 tenant_admin 业务自动鉴权、角色菜单列表读取套餐与交集过滤；临时授权、日志角色分配、部门数据权限分支未修改。对照证明此次列出的失败不依赖该套餐补丁。

原始日志（本机 .runtime）：permission-risk-current.log、permission-risk-without-package-patch.log。
对照命令（IntRuoyiBackend）：

```powershell
mvn -pl yudao-module-system -am '-Dtest=AuthTemporaryPermissionProjectionTest,PermissionServiceTest' '-Dsurefire.failIfNoSpecifiedTests=false' test
```

## 逐类根因、修复与影响

| 用例组 | 根因与影响证据 | 本轮修复 |
| --- | --- | --- |
| 临时授权active/tempAdmin（2项） | 手工装配 PermissionServiceImpl 缺 menuService；ERP显式授权过滤须读取菜单以识别ERP后代；角色菜单查询已使用Set，旧stub却匹配List。补齐依赖后第二层不匹配也实际复现 | 注入菜单服务、提供菜单清单、匹配Set参数；验证只有有效本租户临时角色才读取菜单，不自动授予管理员权限；保留过期/撤销/禁用/跨租户阻断及父页面隐藏断言 |
| 日志限制及升级管理员（11项，含参数化） | 角色fixture未设置status，实际服务会剔除非启用角色，未进入日志权限/管理员判断；该逻辑与套餐分支独立，撤除套餐补丁仍失败 | 明确普通、审核员、项目代码管理员及目标管理员为启用状态；保留拒绝、无业务写入、无审计追加等原断言 |
| 超级管理员菜单、普通角色菜单（2项，普通角色不稳定） | 超管fixture未提供实际角色；普通角色status随机，实际服务只处理有效启用角色 | 补齐有效超级管理员角色，普通角色明确启用；原菜单集合断言保留 |
| 自定义部门（1项） | 旧测试要求自动追加本人部门；现实现明确“自定义范围严格使用角色配置，SELF需显式组合”。原随机集合偶尔已包含本人部门，导致数量差异不稳定 | 固定配置部门10/20、本部门3，精确断言仅10/20且不含3；验证不查询本人部门，未改变实现的权限边界 |

初次修复后实际运行还发现List/Set及自定义部门旧预期问题，日志 permission-risk-final.log；随后修正，不用忽略失败或放宽严格stub取代回归。

## 最终实际回归

日志：.runtime/permission-risk-final-verified.log；BUILD SUCCESS，共113项，0失败、0错误、0跳过。

| 类 | 项数 |
| --- | ---: |
| AuthTemporaryPermissionProjectionTest | 6 |
| PermissionServiceTest | 49 |
| TenantAdminPackageBoundaryTest | 3 |
| TenantAdminBusinessPermissionTest | 3 |
| TenantAdminBusinessPolicyTest | 4 |
| ErpDataRolePermissionTest | 15 |
| PermissionApiEntitlementTest | 2 |
| DccApprovalTaskAdapterTest | 31 |

```powershell
mvn -pl yudao-module-dcc -am '-Dtest=DccApprovalTaskAdapterTest,TenantAdminPackageBoundaryTest,TenantAdminBusinessPermissionTest,TenantAdminBusinessPolicyTest,ErpDataRolePermissionTest,PermissionApiEntitlementTest,AuthTemporaryPermissionProjectionTest,PermissionServiceTest' '-Dsurefire.failIfNoSpecifiedTests=false' test
```

边界：仅能宣布上述8个类的113项回归通过，不代表全仓所有权限测试通过；本轮没有Playwright真实页面或服务器部署验收。业务实现仍与0d0c98f5a/3368249ef一致。原JVM崩溃录制文件保持未跟踪，未纳入提交。
