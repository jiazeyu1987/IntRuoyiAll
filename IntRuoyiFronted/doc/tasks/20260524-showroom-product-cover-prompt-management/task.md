# 任务：展柜产品封面提示管理页签

## Goal

在 `展柜` 后台新增平级页签 `提示管理`，只管理 `PRODUCT_COVER` 场景的产品封面提示词，支持：

- 展示当前生效版本摘要；
- 保存新提示词版本并即时刷新当前版本；
- 查看历史版本列表与只读详情；
- 不改产品管理原有单图/批量封面用户路径，只让服务端默认套用当前版本。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\router\modules\showroom.ts`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\api\showroom-admin\index.ts`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\showroom-admin\**`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\scripts\**`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260524-showroom-product-cover-prompt-management\**`

## Non-Scope

- 不新增“用旧版本 prompt 直接重新生成”的按钮
- 不扩展到公司图、展柜图或 Website 前台
- 不重做现有展柜后台视觉语言
- 不为了测试新增额外前端隐藏控件或绕过入口

## Previous Task Check

- Previous same-repo task record:
  `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260523-runtime-control-access-path-display\task.md`
- Status before this task: `Completed`
- Impact on this task:
  上一同仓任务已完成，但当前前端仓存在未提交在途改动，尤其 `src/router/modules/showroom.ts`、`src/views/showroom-admin/index.vue`、`company-version` 相关文件已被修改；本任务只做增量兼容，禁止回滚这些现有改动。

## Milestones

- [x] M1：核对同仓前置任务状态并建立本任务文档、执行日志和证据文件。
- [x] M2：先补 RED，锁定路由页签、当前版本摘要、保存表单和历史查看的前端可观察行为。
- [x] M3：实现 `ShowroomAdminPrompt` 路由、工作台页面、API 类型和数据加载。
- [x] M4：完成保存成功后刷新当前版本/历史列表、历史详情只读查看和错误显式展示。
- [x] M5：运行前端定向验证与真实入口验证，更新证据并检查提交边界。

## Expected Verification

- `node --test scripts/showroom-admin-prompt-management.test.mjs`
- `pnpm exec eslint src/router/modules/showroom.ts src/api/showroom-admin/index.ts src/views/showroom-admin/index.vue src/views/showroom-admin/prompt/PromptWorkbench.vue scripts/showroom-admin-prompt-management.test.mjs`
- `npx.cmd --yes --package @playwright/cli playwright-cli --session showroom-prompt run-code --filename <task-script>`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260524-showroom-product-cover-prompt-management\frontend-feature-evidence.md`

## Current Status

- Completed on 2026-05-24.
- 已完成前端实现：
  - 新增 `ShowroomAdminPrompt` 路由与 shell section；
  - 新增 `PromptWorkbench.vue`、API 类型与当前/历史/保存交互；
  - 历史版本支持只读查看完整模板，不提供旧版本直接生成入口；
  - 非企宣角色进入页签时显示明确无权限提示。
- 已完成定向验证：
  - `node --test scripts/showroom-admin-prompt-management.test.mjs`
  - `pnpm exec eslint src/router/modules/showroom.ts src/api/showroom-admin/index.ts src/views/showroom-admin/index.vue src/views/showroom-admin/prompt/PromptWorkbench.vue scripts/showroom-admin-prompt-management.test.mjs`
  - `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check`
  - `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260524-showroom-product-cover-prompt-management --mode preview`
- 已完成真实前端链路的部分验证：
  - 使用测试租户 `测试租户 / aoteman / admin123` 从真实登录页进入 `http://127.0.0.1:8081/showroom/prompt`；
  - 新提示词版本已通过真实前端保存，运行库当前版本为 `V3`，`changeNote=playwright verify 1779559450331`。
- 本次提交边界已收口为本任务相关前端代码、脚本测试与任务文档；`showroom-frontstage-removal`、`showroom-product-pagination-diagnosis` 等同仓在途任务继续留在工作区，不混入本次提交。

## Risks / Blockers

- 残余风险：真实单图 `AI生成` 响应极慢。
- 影响：
  - Playwright 在等待 `/admin-api/showroom/product/generate-cover-image` 响应 120 秒后超时；
  - 但后端访问日志显示真实请求最终完成，耗时约 `764819 ms` 与 `1054806 ms`；
  - API 最终核对显示当前提示词版本 `useCount=5`，说明真实生成链路已命中新版本 prompt 并完成写回。
- closeout 预览结果：当前任务目录仅保留 `task.md` 与 `execution-log.md`，其余证据/脚本文件若后续生成可按预览结果清理。
