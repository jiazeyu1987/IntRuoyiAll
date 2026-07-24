# 执行日志：统一电子签名一级页签后端与菜单迁移

INFO: skill -> 使用 `worktree`、`backend-api-delivery`、`database-schema-delivery`，并读取对应契约。

INFO: experience-index -> matched `docs/worktree-memory.md`, `docs/login-access.md`, `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`。

GREEN: experience-preflight -> PASS，已确认使用独立 worktree `D:\ProjectPackage\Int\IntRuoyiWorktrees\signature-primary`，分支 `codex/unified-signature-primary-tab`，规划端口 `8089/48089`，真实 E2E 使用本机测试租户 `测试租户/aoteman/111111`；禁止 fallback、mock、静默跳过和跨环境切换。

BDD: 电子签名一级菜单承载全部签名入口 -> Given 系统菜单已执行统一菜单迁移 / When 用户拥有电子签名相关权限登录 / Then 侧边菜单只暴露一级电子签名入口，不再暴露 DCC电子签名管理 或 eDHR签名记录。

BDD: 门户入口返回统一页内子页签 -> Given DCC/eDHR adapter 已接入统一门户 / When 请求 portal overview / Then DCC、eDHR 主入口路径分别指向 /signature-governance?tab=file-signatures 与 /signature-governance?tab=batch-signatures。

BDD: 原权限码继续用于后端鉴权 -> Given 原 DCC 签名管理或 eDHR 签名查询权限仍被页面和接口使用 / When 菜单迁移执行 / Then 原权限码作为统一菜单下权限项保留，角色授权不丢失。

RED: pytest script/tests/test_unified_electronic_signature_menu_sql.py -> FAIL，缺少 `sql/mysql/20260624_unified_electronic_signature_menu.sql`。

RED: mvn -pl yudao-module-dcc "-Dtest=SignatureGovernancePortalServiceTest,DccSignatureGovernancePortalAdapterTest,SignatureGovernanceControllerTest" test -> FAIL，DCC adapter 仍返回 `DCC 电子签名` 与旧 `/dcc/controlled-file/signatures` 入口。

RED: mvn -pl yudao-module-mes -am "-Dtest=MesEdhrSignatureGovernancePortalAdapterTest" test -> FAIL，聚合模块先于 MES 执行指定测试时触发 surefire `No tests matching pattern`，需显式设置 `surefire.failIfNoSpecifiedTests=false` 才能到达目标测试。

GREEN: pytest script/tests/test_unified_electronic_signature_menu_sql.py -> PASS，一级菜单 900218、旧菜单权限项转换、signature-governance 权限父级迁移 SQL 契约通过。

GREEN: mvn -pl yudao-module-dcc "-Dtest=SignatureGovernancePortalServiceTest,DccSignatureGovernancePortalAdapterTest,SignatureGovernanceControllerTest" test -> PASS。

GREEN: mvn -pl yudao-module-mes -am "-Dtest=MesEdhrSignatureGovernancePortalAdapterTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS。

RED: post-merge menu preflight -> FAIL，本机测试库执行首版迁移后 `system_menu.id=900218` 被 `signature-governance:policy:query` 权限父级迁移更新为 `parent_id=900218` 自引用。

GREEN: post-merge menu sql self-parent regression -> PASS，SQL 增加 `parent_id=0` 幂等修复并在权限迁移中排除 `@unified_signature_menu_id`。

GREEN: post-merge real Playwright E2E -> PASS，本机测试库已应用 `20260624_unified_electronic_signature_menu.sql` 修正版，`900218` 为一级菜单、旧 `6815/900026` 为 `900218` 下权限项；真实登录 `测试租户/aoteman/111111` 后打开统一电子签名页，portal、策略、文件签名、批记录签名、用户授权接口均 HTTP 200 且业务 `code=0`。
