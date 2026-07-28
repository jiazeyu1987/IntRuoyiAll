# 任务：产品主数据页签重命名

## Task Goal

将前端中产品主数据页面的用户可见页签名称从 `产品主数据` 调整为 `展厅主数据`，不改变接口、权限、菜单路由或业务数据契约。

## Milestones

- [x] M1：定位产品主数据页签标题来源，确认仅做文案级重命名。
- [x] M2：补充或更新静态契约，先 RED 锁定旧页签名。
- [x] M3：实施最小前端文案修改。
- [x] M4：运行目标验证和必要回归，记录结果。
- [x] M5：完成任务收尾、经验沉淀和提交推送。

## Expected Verification

- 目标静态契约先因仍存在旧页签标题而失败，再在实现后通过。
- 受影响前端文案扫描或搜索确认页签标题已改为 `展厅主数据`。
- 不引入 fallback、降级、吞异常或兼容分支。

## 经验门禁

- 前端静态契约隔离门禁：本任务使用专用静态契约覆盖页签重命名，较旧产品主数据总契约的无关断言漂移不得作为当前需求通过证据。
- 数据库菜单迁移门禁：新增 SQL 必须带完整 `release-migration` 元数据，`dependsOn` 使用 migrationId 且不带 `.sql` 后缀；目标 SQL 需与依赖迁移一起跑聚焦 policy gate。
- PowerShell/编码门禁：中文任务文档、SQL 和前端文案均通过 `apply_patch` 或 UTF-8 读取写入，不使用默认编码重写。

## Current Status

completed: 页签/页面标题已改为 `展厅主数据`，目标静态契约、菜单设置静态契约、真实路径语法检查、聚焦迁移门禁、cleanup apply 和经验沉淀均已完成。推送受当前工作区并行未提交改动影响，最终 Git 状态单独记录在执行日志中。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。本任务只修改正式用户可见页签文案来源，不做临时替换或运行时兜底。
- `是否存在临时补丁或绕过`：否。

## Verification Summary

- RED: `node tests/e2e/mdm-product-tab-title-static.spec.js` -> FAIL，缺少 `20260728_rename_mdm_product_menu.sql`，旧页签重命名未落地。
- GREEN: `node tests/e2e/mdm-product-tab-title-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/mdm-tenant-package-real-setup-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/mdm-real-data-prerequisite-guards-static.spec.js` -> PASS。
- GREEN: `node --check tests/e2e/mdm-product-real-setup.e2e.js; node --check tests/e2e/mdm-role-menu-real-setup.e2e.js; node --check tests/e2e/mdm-tenant-package-real-setup.e2e.js` -> PASS。
- GREEN: `python -X utf8 script\release\run-release-migration-policy-gate.py --sql-root sql\mysql --sql-file sql\mysql\20260607_product_master_data.sql --sql-file sql\mysql\20260728_rename_mdm_product_menu.sql` -> PASS，`migrationCount=2`。
- NOTE: 全量 `run-release-migration-policy-gate.py --sql-root sql\mysql` 仍被既有 `20260725_mes_edhr_recordbook_global_setting.sql: config-seed` 阻塞，和本次新增 SQL 无关。
- CLEANUP: `task_closeout.py --task-id 20260728-rename-product-master-tab --mode preview/apply` -> PASS，保留 `task.md`、`execution-log.md`、`verification-report.md`，无删除项、无阻塞。
- EXPERIENCE: 已更新 `docs/frontend-development.md#动态菜单页签重命名门禁` 和 `docs/experience-index.md`，并通过 `rg` 验证索引关键词可定位。
