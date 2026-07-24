# 任务：展柜分配按 Excel 恢复为正式 166 条映射

## Goal

以 `D:\ProjectPackage\Int\IntRuoyi\resource\展厅产品与描述清单.xlsx` 为唯一事实源，恢复本地运行库 `ruoyi-vue-pro` 的 `showroom_hall_product`，让后台 `展柜管理 / 展柜分配` 中 8 个展厅的产品数量与 Excel 一致。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\resource\展厅产品与描述清单.xlsx`
- 本地 Docker MySQL 容器 `int-ruoyi-mysql` 的 `ruoyi-vue-pro.showroom_hall_product`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260522-showroom-hall-mapping-match-excel\**`

## Non-Scope

- 不修改 `Website` 前台代码或其 consumer 契约
- 不修改 Java / Vue 业务逻辑、API 契约、数据库 schema
- 不补 preview asset、narration 或其他 live 资源缺口
- 不重建 `showroom_product`、`showroom_product_revision`、`showroom_company`、`showroom_hall`
- 不删除当前库中与本次映射恢复无关的 14 条额外 E2E 产品
- 不引入 fallback、mock 成功、静默降级或替代数据源

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260522-showroom-product-narration-stuck-running-diagnosis\task.md`
- Status before this task: `Completed`
- Impact: 上一条同仓 showroom 任务已完成，不阻塞本次恢复展柜映射。

## Repository Status Check

- Repository: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`
- Current state: 仓库存在大量并行未提交改动。
- Impact: 本任务只允许新增当前任务文档与任务内对账/修复脚本，并只改本地运行库 `showroom_hall_product` 数据；不得覆盖其他在途改动。

## Milestones

1. 创建任务文档与执行日志，记录 BDD 场景和验证命令。
2. 编写可复跑的 Excel 对账/修复脚本，并跑出 RED 证据，证明当前 hall mapping 只有 `8` 条。
3. 按 Excel 重建 `showroom_hall_product`，仅替换 hall-product 关系与 `display_order`。
4. 执行 GREEN 验证，确认 hall 分布恢复为 `26 / 28 / 27 / 17 / 10 / 20 / 11 / 27`。
5. 用 Playwright 验证真实后台页面，再显式探测 `app-config` 风险并记录结果。
6. 回写任务结果并执行 closeout preview。

## Expected Verification

- `python -X utf8 D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260522-showroom-hall-mapping-match-excel\scripts\verify_and_restore_hall_mapping.py --mode verify`
- `python -X utf8 D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260522-showroom-hall-mapping-match-excel\scripts\verify_and_restore_hall_mapping.py --mode apply`
- `python -X utf8 D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260522-showroom-hall-mapping-match-excel\scripts\verify_and_restore_hall_mapping.py --mode verify`
- `npx.cmd --yes --package @playwright/cli playwright-cli --session showroom-hall-mapping-match-excel run-code --filename D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260522-showroom-hall-mapping-match-excel\scripts\verify-showroom-hall-mapping-match-excel.mjs`
- `curl.exe -i http://127.0.0.1:48081/showroom/display/app-config`
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260522-showroom-hall-mapping-match-excel --mode preview`

## Current Status

Completed.

## Completed Work

- 新增了任务内可复跑脚本 `scripts/verify_and_restore_hall_mapping.py`，统一负责 Excel 对账、缺失前置校验和 `showroom_hall_product` 事务性重建。
- 已确认 RED：当前本地运行库的 hall mapping 仍是 8 条 `E2E-PUBLISH-*` 单产品映射，每个展厅只有 1 条。
- 已按 Excel 重建 `showroom_hall_product`，仅替换 hall-product 关系和 `display_order`，未改动 `showroom_product`、`showroom_product_revision`、14 条额外 E2E 产品和其他 showroom 表。
- 已确认 GREEN：hall mapping 恢复为 `166` 条，8 个展厅数量恢复为 `26 / 28 / 27 / 17 / 10 / 20 / 11 / 27`。
- 已新增任务内 Playwright 验证脚本 `scripts/verify-showroom-hall-mapping-match-excel.mjs`，并通过真实后台页面验证列表数量和 `hall_01` 维护产品弹窗。

## Verification Result

- PASS: `python -X utf8 D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260522-showroom-hall-mapping-match-excel\scripts\verify_and_restore_hall_mapping.py --mode verify`
- PASS: `python -X utf8 D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260522-showroom-hall-mapping-match-excel\scripts\verify_and_restore_hall_mapping.py --mode apply`
- PASS: `python -X utf8 D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260522-showroom-hall-mapping-match-excel\scripts\verify_and_restore_hall_mapping.py --mode verify`
- PASS: `docker exec int-ruoyi-mysql mysql --default-character-set=utf8mb4 -uroot -p123456 -D ruoyi-vue-pro -e "SELECT h.hall_code, COUNT(*) AS product_count FROM showroom_hall_product hp JOIN showroom_hall h ON h.id = hp.hall_id WHERE hp.deleted = 0 AND h.deleted = 0 GROUP BY h.hall_code, h.display_order ORDER BY h.display_order;"`
- PASS: `npx.cmd --yes --package @playwright/cli playwright-cli --session showroom-hall-mapping-match-excel open http://127.0.0.1:8081/showroom/hall --headed`
- PASS: `npx.cmd --yes --package @playwright/cli playwright-cli --session showroom-hall-mapping-match-excel run-code --filename D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260522-showroom-hall-mapping-match-excel\scripts\verify-showroom-hall-mapping-match-excel.mjs`
- RISK RECORDED: `curl.exe -i http://127.0.0.1:48081/showroom/display/app-config` -> `HTTP 200`, body `{\"code\":401,\"msg\":\"账号未登录\",\"data\":null}`

## Remaining Blockers

- 当前本地 `GET /showroom/display/app-config` 未返回匿名可用配置，而是返回 `code=401 / 账号未登录`；本次已按要求原样记录，不在当前任务中补前台或安全链路。
- 仓库中仍有大量与本任务无关的并行未提交改动；提交时必须只纳入当前任务新增文档与脚本。

## Assumptions

- 当前本地运行库仍保留 Excel 对应的 `166` 个原始 `product_code`；若脚本核对出缺失编码，任务必须失败并报告。
- 本次完成标准是“后台展柜分配与 Excel 对齐”，不是“同时保持匿名前台 `app-config` 继续 200”。
- 恢复后若 `app-config` 暴露真实 preview/narration 缺口，按 fail-fast 原样记录，不做 fallback 修补。

## Cleanup Keep

- `doc/tasks/20260522-showroom-hall-mapping-match-excel/task.md`
- `doc/tasks/20260522-showroom-hall-mapping-match-excel/execution-log.md`
- `doc/tasks/20260522-showroom-hall-mapping-match-excel/scripts/verify_and_restore_hall_mapping.py`
- `doc/tasks/20260522-showroom-hall-mapping-match-excel/scripts/verify-showroom-hall-mapping-match-excel.mjs`
