# Task: 展厅 app-config 本地 live 数据补齐验证

## Goal

在本地验证环境中补齐 `GET /showroom/display/app-config` 所需的 live 数据前置条件，让匿名请求不再停在 `SHOWROOM_TARGET_NOT_FOUND`，并返回稳定 JSON contract。

## Scope

- 当前本地 `ruoyi-vue-pro` MySQL 运行库中的 showroom live 数据
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260520-showroom-app-config-live-data-local-verification\**`

## Non-Scope

- 不改动 `Website` 前端 consumer 代码
- 不新增或修改 showroom Java 业务逻辑
- 不把本地临时素材当成正式生产素材
- 不静默绕过缺失数据校验

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260520-showroom-display-anonymous-access\task.md`
- Status before this task: `Completed on 2026-05-20`
- Impact: 匿名访问认证阻塞已解除，当前剩余问题已切换为本地 live 数据不完整，可继续处理数据前置条件。

## Milestones

1. 记录当前 `app-config` 匿名请求失败的真实数据缺口。
2. 盘点 company / hall / product 的 live revision、preview asset、双语 narration 现状。
3. 仅在本地验证范围内补齐缺失的 company/product preview 与 product live 依赖。
4. 重新验证匿名 `GET /showroom/display/app-config` 返回 200 与稳定 JSON。
5. 更新任务记录并保留本地验证说明。

## Expected Verification

- `Invoke-WebRequest http://127.0.0.1:48081/showroom/display/app-config`
- 必要时查询：
  - `showroom_preview_asset_version`
  - `showroom_narration_version`
  - `showroom_hall_product`

## Current Status

- Status: Completed
- Completed work:
  - 已确认匿名认证阻塞已解除。
  - 已确认当前第一层数据缺口是 `SHOWROOM_TARGET_NOT_FOUND: live company preview asset is required`。
  - 已确认本地现状：
    - company live revision 已存在
    - company published ZH/EN narration 已存在
    - hall published preview 已存在 8 条
    - company preview asset 缺失
    - product published preview asset 缺失
    - hall mapping 与当前 live product 未对齐
  - 已新增 `local-app-config-backfill.sql`，通过可重复 SQL 回填本地验证所需的 company/product preview、product narration 与 hall mapping。
  - 已确认匿名 `GET /showroom/display/app-config` 返回 `code=0` 与稳定 JSON。
- Remaining blockers:
  - None for this local verification scope.

## Final Verification Result

- PASS: 执行 `doc/tasks/20260520-showroom-app-config-live-data-local-verification/local-app-config-backfill.sql`
- PASS: `GET http://127.0.0.1:48081/showroom/display/app-config` -> HTTP 200 with `code=0`
- PASS: company preview live row exists
- PASS: product preview live row exists
- PASS: `showroom_hall_product` 已对齐到当前 live product `172`

## Local Verification Note

- 本任务仅用于本地联调验证。
- `company` 与 `product` preview 复用了先前已批准的临时预览图 `infra_file.id = 2272`。
- `8` 个 hall 当前临时共用同一个 live product `172`，仅为了把 `app-config` 跑通，不应视为正式生产展示编排。

## Cleanup Keep

- `doc/tasks/20260520-showroom-app-config-live-data-local-verification/development-plan.md`
- `doc/tasks/20260520-showroom-app-config-live-data-local-verification/local-app-config-backfill.sql`
