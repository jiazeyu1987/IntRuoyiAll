# Execution Log：权限角色配置包跨环境菜单标识正式修复

- BDD: 跨环境不同菜单 ID 仍可正确导入 -> Given 导出包记录的是菜单稳定业务标识而非目标环境主键 / When 在菜单 ID 不同但业务标识一致的环境导入角色配置包 / Then 角色最终绑定到目标环境对应菜单。
- BDD: 导入包引用的菜单在目标环境缺失时明确失败 -> Given 导入包中的菜单业务标识在目标环境不存在 / When 导入角色配置包 / Then 导入明确报错并指出缺失菜单标识，不写入部分错误权限。
- BDD: 同环境往返导出导入保持可用 -> Given 当前环境存在角色与菜单 / When 导出后原样导入角色配置包 / Then 角色基础字段与菜单权限仍可完整回放。
- RED: `mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-system "-Dtest=RoleConfigPackageServiceImplTest" test` -> FAIL，新增 `menuKeys` 合同测试无法编译，证明当前角色配置包结构仍只暴露 `menuIds` 数字主键，尚不支持稳定菜单业务标识。
- GREEN: `mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-system "-Dtest=RoleConfigPackageServiceImplTest" test` -> PASS，角色配置包已升级到 `packageVersion=2`，导出/导入统一走 `menuKeys` 稳定菜单标识，缺失菜单时以 `CONFIG_PACKAGE_REFERENCE_MISSING` 明确失败。
- GREEN: `mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes "-Dtest=MesProSchedulerWorkbenchFullConfigPackageServiceTest" test` -> PASS，排产员工作台全量数据包服务 3 条测试全部通过，确认嵌套 `roleConfigPackageService.importPackage(...)` 链路兼容新的角色配置包合同。
- GREEN: `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260701-role-config-package-cross-env-menu-identity\bug-regression-evidence.md` -> PASS
- GREEN: `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260701-role-config-package-cross-env-menu-identity\backend-api-evidence.md` -> PASS
