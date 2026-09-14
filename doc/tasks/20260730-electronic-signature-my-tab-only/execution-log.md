# Execution Log

## User Intent

用户要求：电子签名里，普通用户只能看到“我的签名”这个页签。截图显示普通用户进入电子签名时仍看到无权限列表区域并提示“没有该操作权限”。

用户追问：为什么用 `zhaojie` 账号仍能看到电子签名下多个左侧菜单页签。截图显示本机 `localhost:8081/signature-governance` 运行态左侧菜单仍包含“签名记录 / 我的签名 / 长期留存 / 周期复核 / CSV质量包 / 统一策略”。

## BDD

- `BDD: 普通用户电子签名页签隔离 -> Given 普通用户进入电子签名页面 When 页面初始化页签 Then 页面只展示“我的签名”页签且不会展示/默认进入无权限的管理页签`
- `BDD: zhaojie 运行态电子签名菜单隔离 -> Given zhaojie 使用普通业务角色登录本机系统 When 后端动态菜单返回电子签名子菜单 Then 只应返回电子签名根入口和“我的签名”，不得返回签名记录、长期留存、周期复核、CSV质量包或统一策略`

## Milestone Log

- 2026-07-30：已读取任务、前端、权限、E2E、编码规则和适用技能说明，创建任务目录。
- 2026-07-30：定位根因：电子签名根入口固定指向 `signature-records`，隐藏静态子路由会补回未授权治理子路由，历史菜单授权也会让普通角色获得签名记录页签。
- 2026-07-30：新增前端静态合同 `IntRuoyiFronted/tests/e2e/electronic-signature-my-tab-only-static.spec.js` 与 SQL 合同 `IntRuoyiBackend/script/tests/test_signature_regular_users_my_signature_only_sql.py`。
- 2026-07-30：前端修复 `signature-governance/index.vue`，普通用户进入治理页签时重定向到“我的签名”，全量签名记录/治理页签仅管理员可 mount。
- 2026-07-30：前端修复 `permission.ts`，电子签名壳路由重定向改为基于已授权动态子路由，并且不再给普通用户补回未授权隐藏静态子路由。
- 2026-07-30：新增迁移 `20260730_signature_regular_users_my_signature_only.sql`，普通角色只保留电子签名根入口和“我的签名”，管理员角色保留治理/签名记录范围。
- 2026-07-30：项目经验已合并到 `docs/frontend-development.md#前端权限页签正向授权门禁` 与 `docs/experience-index.md`。
- 2026-07-30：收到 zhaojie 运行态截图复核；只读查询本机 `int-ruoyi-mysql/ruoyi-vue-pro`，确认 `zhaojie` 在租户 1 绑定 `wenkong`、`mes_scheduler`、`approval_center_entry`，其中 `wenkong` 角色仍有电子签名治理菜单 `900210/900410/900211/900411/900212/900418/900213/900214/900414/900215/900415/900216/900416/900217/900417/900218` 的有效授权。
- 2026-07-30：执行正式迁移 `IntRuoyiBackend/sql/mysql/20260730_signature_regular_users_my_signature_only.sql` 到本机 `int-ruoyi-mysql/ruoyi-vue-pro`；复核后 `zhaojie` 在租户 1 的有效电子签名菜单仅剩 `900218 电子签名` 与 `900418 我的签名`，`wenkong` 角色有 14 条旧治理范围授权被软删除并标记 `signature-regular-my-only`。

## Verification Evidence

- `RED: node tests/e2e/electronic-signature-my-tab-only-static.spec.js -> FAIL, expected reason: 尚未声明管理员页签正向集合，普通用户仍可 mount 签名记录页签`
- `RED: python -m pytest script/tests/test_signature_regular_users_my_signature_only_sql.py -> FAIL, expected reason: 缺少普通角色只保留“我的签名”的正式 SQL 迁移`
- `GREEN: node tests/e2e/electronic-signature-my-tab-only-static.spec.js -> PASS`
- `GREEN: python -m pytest script/tests/test_signature_regular_users_my_signature_only_sql.py -> PASS, 3 passed`
- `GREEN: node tests/e2e/signature-governance-records-static.spec.js -> PASS`
- `GREEN: node tests/e2e/signature-governance-e2e-static.spec.js -> PASS`
- `GREEN: node --test scripts/signature-governance-page-contract.test.mjs -> PASS, 12 passed`
- `GREEN: python -m pytest script/tests/test_signature_my_signature_admin_menu_sql.py script/tests/test_admin_full_scope_role_standardization_sql.py -> PASS, 12 passed`
- `GREEN: pnpm ts:check -> PASS`
- `GREEN: python script/release/run-release-migration-policy-gate.py --sql-root sql/mysql -> PASS, migrationCount=398`
- `NOTE: python script/release/run-release-migration-policy-gate.py --sql-root sql/mysql --sql-file sql/mysql/20260730_signature_regular_users_my_signature_only.sql -> FAIL, expected gate behavior because single-file mode excludes historical dependencies; reran full sql root and passed.`
- `RED: docker exec int-ruoyi-mysql mysql read-only zhaojie signature menu query -> FAIL, expected reason: tenant 1 role wenkong still has active electronic-signature governance menu grants beyond root + my-signature`
- `GREEN: docker exec int-ruoyi-mysql mysql apply IntRuoyiBackend/sql/mysql/20260730_signature_regular_users_my_signature_only.sql -> PASS`
- `GREEN: docker exec int-ruoyi-mysql mysql read-only zhaojie signature menu query -> PASS, effective menu ids: 900218, 900418; deleted old scope count for wenkong: 14`
- `GREEN: python -m pytest script/tests/test_signature_regular_users_my_signature_only_sql.py -> PASS, 3 passed`
- `GREEN: node tests/e2e/electronic-signature-my-tab-only-static.spec.js -> PASS`

## Blockers

- Closeout commit/push is blocked by pre-existing/concurrent repository state: `int_main...origin/int_main [ahead 12, behind 8]` plus unrelated dirty/untracked files across multiple task directories. No unrelated files were reverted or cleaned.
- During this task, external/concurrent baseline commits already included part of the electronic-signature changes (`bf547497`, `cda510bf`); no history rewrite, rollback, or force push was performed.
