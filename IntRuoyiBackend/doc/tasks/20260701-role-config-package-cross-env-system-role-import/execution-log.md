# Execution Log：权限角色配置包跨环境导入系统异常修复

- BDD: 缺失的系统类型角色可跨环境导入 -> Given 权限角色配置包包含 system type 角色且目标环境不存在同 code 角色 / When 导入配置包 / Then 角色被创建并写入菜单与基础数据范围字段，且不再触发系统内置角色数据权限更新禁令。
- BDD: 已存在的系统类型角色仍可原样回导 -> Given 目标环境已存在同 code 的 system type 角色 / When 导入配置包 / Then 角色基础字段与菜单更新成功，且仍跳过 assignRoleDataScope 禁止路径。
- RED: `mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-system "-Dtest=RoleConfigPackageServiceImplTest" test` -> FAIL，新增回归用例 `importPackage_shouldCreateMissingSystemRoleWithoutCallingDataScopeMutation` 证明当前代码在“目标环境缺少 system type 角色”时仍会调用 `PermissionService.assignRoleDataScope(...)`，失败点位于 `RoleConfigPackageServiceImpl.java:66`。
- GREEN: `mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-system "-Dtest=RoleConfigPackageServiceImplTest" test` -> PASS，已在“新增 system type 角色”路径复用与更新路径一致的保护条件，仅对非系统角色回放 `assignRoleDataScope(...)`。
