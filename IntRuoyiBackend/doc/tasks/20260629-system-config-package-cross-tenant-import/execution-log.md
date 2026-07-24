# Execution Log：系统配置包支持跨租户导入

BDD: 配置包允许跨租户预检 -> Given tenant 1 导出的系统配置包 / When tenant 122 执行预检 / Then 不因 sourceTenantId 与 targetTenantId 不同而阻塞。
BDD: preserve_existing_passwords 支持跨租户导入新用户 -> Given 配置包中的用户账号在目标租户不存在 / When tenant 122 以保留目标密码策略导入 / Then 系统按账号映射已存在账号并保留密码，不存在账号则创建新用户且不导入源密码摘要。
BDD: 配置包导入只覆盖目标租户 owned scope -> Given tenant 122 执行导入 / When 覆盖系统配置 / Then 只重建目标租户相关用户、角色、部门、岗位、关系与租户套餐，不误删其他租户数据。
RED: `mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-system "-Dtest=SystemConfigPackageServiceImplTest" test` -> FAIL，现有实现仍强制 `sourceTenantId == targetTenantId`，且 `preserve_existing_passwords` 依赖目标租户已有同 ID 用户，无法承载跨租户导入。
GREEN: `mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-system "-Dtest=SystemConfigPackageServiceImplTest" test` -> PASS，`SystemConfigPackageServiceImpl` 已支持跨租户预检与导入；目标租户私有数据按新 ID 重建并重映射关系，未导入源密码摘要。
