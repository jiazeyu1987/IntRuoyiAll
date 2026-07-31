# 任务：修复产品主数据菜单运行态旧标题

## Task Goal

修复本地真实页面左侧动态菜单仍显示 `产品主数据` 的问题，使菜单入口显示为 `展厅主数据`，并确保页面标题、菜单数据迁移和权限菜单树保持一致。

## Milestones

- [x] M1：复现截图中的运行态旧菜单名，确认动态菜单数据来源和当前数据库状态。
- [x] M2：补充回归测试，先 RED 锁定运行态菜单必须显示 `展厅主数据`。
- [x] M3：实施根因修复并安全应用本地正式菜单迁移。
- [x] M4：通过静态契约、菜单数据核验和真实 Playwright 页面验证。
- [x] M5：完成 cleanup、经验沉淀、提交和推送。

## Expected Verification

- 回归测试在修复前因当前菜单数据仍为 `产品主数据` 而失败。
- `system_menu` 的目标菜单记录 `id=990201`、`permission=mdm:product:query` 更新为 `展厅主数据`。
- 真实前端页面左侧菜单显示 `展厅主数据`，页面主标题也保持一致。
- 不引入 fallback、默认文案覆盖、吞异常或未授权远端数据修改。

## Current Status

completed: 已用 UTF-8 hex 修复迁移更新本地动态菜单数据，数据库目标值和真实页面可见菜单均已验证通过；cleanup、经验沉淀和实现提交 `1374f802` 已完成。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。修复动态菜单正式数据与迁移应用状态，不用前端硬编码遮盖旧菜单响应。
- `是否存在临时补丁或绕过`：否。

## Verification Summary

- RED: 本地数据库只读查询 `system_menu.id=990201` -> `HEX(name)=E4BAA7E59381E4B8BBE695B0E68DAE`，即旧值 `产品主数据`。
- RED: `node tests/e2e/mdm-product-menu-runtime-encoding-static.spec.js` -> FAIL，缺少 UTF-8 hex 修复迁移文件。
- GREEN: `node tests/e2e/mdm-product-menu-runtime-encoding-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/mdm-product-tab-title-static.spec.js` -> PASS。
- GREEN: `python -X utf8 script\release\run-release-migration-policy-gate.py --sql-root sql\mysql --sql-file sql\mysql\20260607_product_master_data.sql --sql-file sql\mysql\20260728_rename_mdm_product_menu.sql --sql-file sql\mysql\20260728_fix_mdm_product_menu_utf8_name.sql` -> PASS，`migrationCount=3`。
- GREEN: 本地数据库复核 `system_menu.id=990201` -> `HEX(name)=E5B195E58E85E4B8BBE695B0E68DAE`，即 `展厅主数据`。
- GREEN: Playwright 真实页面验证 -> PASS，展开“基础数据”后可见 `展厅主数据`，可见旧文本 `产品主数据` 数量为 0。
- GREEN: task-closeout-cleanup preview/apply -> PASS，删除本任务额外 evidence 文件，仅保留 `task.md`、`execution-log.md`、`verification-report.md`。
- GREEN: project-experience-consolidation -> PASS，新增 `docs/database-rules.md#中文菜单名称-ascii-安全迁移门禁` 并在 `docs/experience-index.md` 增加关键词路由。
- GREEN: final verification rerun -> PASS，目标静态测试、页签静态测试、聚焦 migration policy gate、数据库 HEX 核验和 `git diff --check` 均通过。
