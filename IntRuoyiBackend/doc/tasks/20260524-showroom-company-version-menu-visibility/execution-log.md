# 执行日志：展柜公司版本菜单可见性修复

BDD: 侧边栏显示公司版本菜单 -> Given 展柜后台存在公司信息和产品管理菜单 When 用户登录真实前端并展开展柜菜单 Then 侧边栏应显示公司版本，且顺序位于公司信息与产品管理之间

BDD: 菜单数据与前端路由一致 -> Given 前端静态路由已定义 `ShowroomAdminCompanyVersion` When 后端返回展柜菜单树 Then 菜单树应包含 path `company-version` 与 component name `ShowroomAdminCompanyVersion`

BDD: 运行库可重复修复 -> Given 本地运行库缺少公司版本菜单记录 When 执行幂等 SQL 修复脚本 Then `system_menu` 应新增或更新 `公司版本` 记录，重复执行不报错且顺序保持正确

RED: `java -cp "C:\Users\BJB110\.m2\repository\com\mysql\mysql-connector-j\9.7.0\mysql-connector-j-9.7.0.jar" D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260524-showroom-company-version-menu-visibility\scripts\ProbeShowroomMenuState.java "jdbc:mysql://127.0.0.1:23306/ruoyi-vue-pro?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&nullCatalogMeansCurrent=true&rewriteBatchedStatements=true" root 123456` -> FAIL，`system_menu` 在 `parent_id=980100` 下仅有 `company / product / hall / approval / history / assignment / discussion / narration-workbench / display/screen/home`，缺少 `path=company-version` + `componentName=ShowroomAdminCompanyVersion`，并抛出 `Missing system_menu row for path=company-version componentName=ShowroomAdminCompanyVersion`。

GREEN: `pytest -q D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_showroom_company_version_menu_sql.py` -> PASS，菜单种子与运行库修复 SQL 均包含 `company-version` 菜单和“复制公司菜单角色绑定”的结构断言。

GREEN: `java -cp "C:\Users\BJB110\.m2\repository\com\mysql\mysql-connector-j\9.7.0\mysql-connector-j-9.7.0.jar" D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260512-erp-schema-repair\scripts\MysqlSchemaRunner.java "jdbc:mysql://127.0.0.1:23306/ruoyi-vue-pro?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&nullCatalogMeansCurrent=true&rewriteBatchedStatements=true" root 123456 D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\sql\mysql\20260524_showroom_company_version_menu_visibility.sql` -> PASS，运行库幂等 SQL 已应用。

GREEN: `java -cp "C:\Users\BJB110\.m2\repository\com\mysql\mysql-connector-j\9.7.0\mysql-connector-j-9.7.0.jar" D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260524-showroom-company-version-menu-visibility\scripts\ProbeShowroomMenuState.java "jdbc:mysql://127.0.0.1:23306/ruoyi-vue-pro?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&nullCatalogMeansCurrent=true&rewriteBatchedStatements=true" root 123456` -> PASS，`system_menu` 已新增 `id=980118 name=公司版本 path=company-version componentName=ShowroomAdminCompanyVersion sort=2`，并从 `company` 菜单复制出同一组角色绑定 `role_id=910206`。

GREEN: `admin-api /system/auth/get-permission-info`（租户 `芋道源码`，账号 `admin / admin123`）-> PASS，返回的 `ShowroomAdmin*` 菜单树已包含 `ShowroomAdminCompanyVersion`。

GREEN: `npx.cmd --yes --package @playwright/cli playwright-cli --session showroom-admin-account-sidebar run-code --filename D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260524-showroom-company-version-menu-visibility\scripts\verify-showroom-company-version-sidebar.mjs` -> PASS，使用 API 登录并注入前端会话后，真实侧边栏文本已包含 `公司信息 / 公司版本 / 产品管理`，顺序正确。
