# Task: Showroom Display Company Anonymous Detail

## Goal

为 `D:\ProjectPackage\Website` 根首页的公司详情页提供一个独立的匿名公司详情展示接口，使首页点击公司主图后可以从 `IntRuoyi` 直接读取公司标题、摘要、公司预览图和公开字段，而不再被 `app-config` 对整包 hall/product live 数据的依赖阻塞。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\main\java\**`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\test\java\**`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-showroom-display-company-anonymous-detail\**`

## Non-Scope

- 不修改 `app-config` 的 showrooms / product 聚合逻辑。
- 不放开 `display/home`、`display/hall`、`display/product`、`display/narration` 的匿名访问。
- 不新增本地 fallback 或跳过 live 数据校验。

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-showroom-company-narration-revision-align\task.md`
- Status before this task: `In Progress`
- Impact: 已确认当前根首页公司详情被 `app-config` 的非公司 live 数据阻塞；本次改动通过新增更小的匿名公司详情合同来解除该阻塞，而不是扩大 fallback。

## Milestones

1. 创建任务文档并记录当前 `app-config` 阻塞背景。
2. 先补 RED 契约测试，锁定匿名 `display/company` 需要可读且返回公司预览图与公开字段，同时不开放其他 display 路由。
3. 最小实现匿名公司详情合同与安全配置。
4. 跑通定向后端测试并回写 GREEN 结果。

## Expected Verification

- `mvn -pl yudao-module-showroom "-Dtest=ShowroomDisplayCompanyAnonymousContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-showroom-display-company-anonymous-detail\backend-api-evidence.md`

## Current Status

- Status: Blocked
- Completed work:
  - 已创建任务记录并完成需求收敛。
- Remaining blockers:
  - 本地 `app-config` live 数据修复已经直接恢复了 Website 根首页公司详情读取，本任务提出的“新增匿名 company detail 接口”不再是当前必要修复路径。
- Impact:
  - 除非后续明确要求把根首页公司详情从 `app-config` 解耦，否则本任务保持阻塞，不继续扩大匿名接口面。
