# Execution Log：角色配置包导入错误具体化

BDD: 权限角色配置包 JSON 非法时返回明确原因 -> Given 用户上传损坏的权限角色配置包 / When 发起导入 / Then 接口返回明确业务错误码和“JSON 非法”原因，而不是系统异常。
BDD: 权限角色配置包字段缺失时返回具体字段 -> Given 用户上传缺少 role code、role name、menuKeys 等必填字段的配置包 / When 发起导入 / Then 接口返回明确业务错误码并指出缺失字段与对应角色。
BDD: 组织角色与审批角色导入校验失败时也不再落到系统异常 -> Given 用户上传非法的组织角色或审批角色配置包 / When 发起导入 / Then 接口返回具体业务错误，而不是统一 500。
BDD: 目标环境引用缺失仍保留原有精确报错 -> Given 配置包引用了目标环境不存在的菜单标识 / When 发起权限角色导入 / Then 继续返回 CONFIG_PACKAGE_REFERENCE_MISSING 及具体缺失引用。

GREEN: experience-preflight -> PASS，已按门禁读取 docs\experience-index.md 与 docs\powershell-memory.md，允许当前本机最小后端修复与定向测试。
RED: mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-system,yudao-module-dcc -Dtest=RoleConfigPackageServiceImplTest,PostConfigPackageServiceImplTest,DccApprovalPositionConfigPackageServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test -> FAIL，旧实现中 Role/Post 导入分别抛出 RuntimeException / IllegalArgumentException，证明非法配置包仍会落到全局 500/系统异常路径。
GREEN: mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-system,yudao-module-dcc -Dtest=RoleConfigPackageServiceImplTest,PostConfigPackageServiceImplTest,DccApprovalPositionConfigPackageServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test -> PASS。
