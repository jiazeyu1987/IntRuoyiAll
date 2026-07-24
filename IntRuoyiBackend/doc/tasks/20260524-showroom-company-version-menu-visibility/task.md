# 任务：展柜公司版本菜单可见性修复

## Goal

- 修复 `展柜 -> 公司版本` 在真实运行态侧边栏不可见的问题。
- 保持前端 `ShowroomAdminCompanyVersion` 路由与后端菜单数据一致，不通过前端绕过权限过滤强行显示。
- 让本地运行库 `127.0.0.1:23306/ruoyi-vue-pro` 中的 `system_menu` 与仓库 SQL 种子都具备 `公司版本` 菜单项。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\sql\showroom\**`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\sql\mysql\**`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260524-showroom-company-version-menu-visibility\**`

## Non-Scope

- 不改 `yudao-ui-admin-vue3` 的权限合并策略去绕过后端菜单缺失。
- 不改展柜公司版本页面业务逻辑、接口合同或数据库业务表结构。
- 不顺手调整其他展柜菜单、角色授权或历史隐藏页签策略。
- 不引入 fallback、默认展示、mock 菜单或静默降级。

## Previous Task Check

- Previous same-repo task record:
  `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260524-showroom-product-cover-prompt-management\task.md`
- Status before this task: `Blocked on 2026-05-24 due to user priority shift`
- Impact on this task:
  上一同仓任务已在启动当前任务前显式标记为阻塞，因此不再阻塞本次菜单可见性修复。

## Milestones

- [x] M1：完成前序任务阻塞登记，并建立当前任务文档、执行日志、数据库证据文件。
- [ ] M2：补只读探针 / RED，确认本地运行库缺少 `公司版本` 菜单记录。
- [ ] M3：新增幂等 SQL 修复脚本并更新展厅菜单种子。
- [ ] M4：应用本地运行库修复并完成真实前端菜单可见性验证。
- [ ] M5：更新证据、完成 closeout 预览并按仓库边界提交。

## Expected Verification

- 只读探针：查询 `system_menu` 中 `parent_id = 980100` 的展柜子菜单顺序与 `company-version` 缺失现状
- SQL 修复：幂等执行新增菜单修复脚本到 `127.0.0.1:23306/ruoyi-vue-pro`
- 真实验证：本地前端 `http://127.0.0.1:8081` 登录后侧边栏可见 `公司信息 / 公司版本 / 产品管理`

## Current Status

- Completed on 2026-05-24.
- 已确认前端静态路由已存在 `ShowroomAdminCompanyVersion`，但 `permission.ts` 中 `mergeStaticShowroomRoute` 只保留后端菜单树返回的非隐藏子节点。
- 已确认仓库菜单种子 `sql/showroom/20260519_showroom_menu_seed.sql` 原先缺少 `公司版本` 记录，且本地运行库 `system_menu` 也确实缺项。
- 已确认本地运行后端 `127.0.0.1:48081` 正连接 `127.0.0.1:23306/ruoyi-vue-pro`，并已对该真实运行库完成幂等 SQL 修复。
- 已完成：
  - 新增运行库修复脚本 `sql/mysql/20260524_showroom_company_version_menu_visibility.sql`
  - 更新展柜菜单种子 `sql/showroom/20260519_showroom_menu_seed.sql`
  - 新增 SQL 回归测试 `script/tests/test_showroom_company_version_menu_sql.py`
  - 通过运行库探针确认 `system_menu` 新增 `id=980118 / path=company-version / componentName=ShowroomAdminCompanyVersion`
  - 通过角色绑定复制确认 `admin / 芋道源码` 的展柜菜单树已包含 `公司版本`
  - 通过 Playwright 注入会话方式确认真实前端侧边栏已显示 `公司信息 / 公司版本 / 产品管理`

## Risks / Blockers

- 无当前阻塞。若其他环境仍不可见，需按同样方式检查该环境的 `system_menu` 与 `system_role_menu` 是否已同步此修复。

## Final Verification Result

- `pytest -q D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_showroom_company_version_menu_sql.py` -> PASS
- `java -cp "C:\Users\BJB110\.m2\repository\com\mysql\mysql-connector-j\9.7.0\mysql-connector-j-9.7.0.jar" D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260512-erp-schema-repair\scripts\MysqlSchemaRunner.java "jdbc:mysql://127.0.0.1:23306/ruoyi-vue-pro?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&nullCatalogMeansCurrent=true&rewriteBatchedStatements=true" root 123456 D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\sql\mysql\20260524_showroom_company_version_menu_visibility.sql` -> PASS
- `java -cp "C:\Users\BJB110\.m2\repository\com\mysql\mysql-connector-j\9.7.0\mysql-connector-j-9.7.0.jar" D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260524-showroom-company-version-menu-visibility\scripts\ProbeShowroomMenuState.java "jdbc:mysql://127.0.0.1:23306/ruoyi-vue-pro?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&nullCatalogMeansCurrent=true&rewriteBatchedStatements=true" root 123456` -> PASS
- `admin-api /system/auth/get-permission-info`（租户 `芋道源码`，账号 `admin / admin123`）-> PASS，`ShowroomAdminCompanyVersion` 已进入真实权限菜单树
- `npx.cmd --yes --package @playwright/cli playwright-cli --session showroom-admin-account-sidebar run-code --filename D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260524-showroom-company-version-menu-visibility\scripts\verify-showroom-company-version-sidebar.mjs` -> PASS
