# Execution Log：角色管理导出导入联通（后端）

- BDD: 权限角色导出包保留回导字段 -> Given 系统存在权限角色及其菜单/数据权限 / When 调用权限角色导出接口 / Then 返回产物包含角色基础字段与回导所需权限结构。
- BDD: 组织角色导出包保留回导字段 -> Given 系统存在组织角色 / When 调用组织角色导出接口 / Then 返回产物包含岗位基础字段并可再次导入。
- BDD: 审批角色导出包保留分配关系 -> Given 系统存在审批角色及分配人员 / When 调用审批角色导出接口 / Then 返回产物同时包含角色基本信息与分配列表。
- BDD: 角色导入包非法时失败 -> Given 导入包缺少必填字段或结构非法 / When 调用任一角色导入接口 / Then 明确报错且不写入任何部分数据。
- RED: `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\role-config-package-roundtrip-real.e2e.js` -> FAIL，真实权限角色回导时命中 `不能操作类型为系统内置的角色`，组织角色空导出包回导时命中 `Post config package posts cannot be empty`，说明现有后端合同不能满足“导出后原样导入”。
- GREEN: `mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-system,yudao-module-dcc "-Dtest=RoleServiceImplTest,PostServiceImplTest,DccApprovalPositionControllerTest,DccApprovalPositionAdminServiceImplTest" test` -> PASS。
- GREEN: `mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-system "-Dtest=RoleConfigPackageServiceImplTest" test` -> PASS。
- GREEN: `mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-system,yudao-module-dcc "-Dtest=RoleConfigPackageServiceImplTest,PostConfigPackageServiceImplTest,DccApprovalPositionConfigPackageServiceImplTest" test` -> PASS。
