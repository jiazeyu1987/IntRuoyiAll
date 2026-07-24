# Execution Log：展厅产品 AI 封面生成入口

BDD: 已审核产品允许 AI 生成封面 -> Given 产品基础信息已完成审批并处于允许生成封面的状态 / When 管理员在产品基础信息弹窗点击 `AI生成` / Then 前端必须调用真实产品封面生成接口，并把生成结果回填到 `封面` 字段。

BDD: 未审核产品禁止 AI 生成封面 -> Given 产品基础信息尚未通过审核 / When 管理员在产品基础信息弹窗点击 `AI生成` / Then 前端不得调用生成接口，必须提示“需要产品基础信息经过审核之后才可以AI生成封面”。

RED: `node --test scripts/showroom-admin-product-cover-field.test.mjs` -> FAIL，`src/api/showroom-admin/index.ts` 尚未暴露 `generateProductCoverImage`，`src/views/showroom-admin/index.vue` 也没有 `AI生成` 按钮与审核提示文案。

GREEN: `node --test scripts/showroom-admin-product-cover-field.test.mjs` -> PASS。

GREEN: `npx.cmd eslint src/api/showroom-admin/index.ts src/views/showroom-admin/index.vue scripts/showroom-admin-product-cover-field.test.mjs` -> PASS。

GREEN: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260520-showroom-product-ai-cover-generation\frontend-feature-evidence.md` -> PASS。

GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260520-showroom-product-ai-cover-generation --mode preview` -> PASS，preview 状态 `ready`。

GREEN: `npx.cmd --yes --package @playwright/cli playwright-cli --session showroom-product-ai-cover run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260520-showroom-product-ai-cover-generation\scripts\verify-product-ai-cover-warning.mjs` -> PASS，真实页面中未审核产品点击 `AI生成` 后出现提示“需要产品基础信息经过审核之后才可以AI生成封面”，且未发起 `/showroom/product/generate-cover-image` 请求。

GREEN: `node --test scripts/showroom-admin-workflow-workbenches.test.mjs scripts/showroom-admin-product-cover-field.test.mjs` -> PASS。

GREEN: `npx.cmd eslint src/views/showroom-admin/approval/contracts.ts scripts/showroom-admin-workflow-workbenches.test.mjs` -> PASS。

INFO: 只读运行库核对确认测试租户 `tenant_id=122` 原先缺少 `showroom_publicity` 角色绑定；随后已在本地运行库补齐角色与用户绑定，解除真实审批闭环阻塞。

INFO: 只读 schema 核对确认 live MySQL `showroom_change_request.submitter_dept_id` 与源码基线不一致：源码 SQL 为 `DEFAULT NULL`，live 表为 `NOT NULL`；随后已在本地运行库修回允许 `NULL`。

INFO: `src/views/showroom-admin/approval/contracts.ts` 已改为允许 `submitterDeptId = null`，解决真实审批页“审批中心缺少数值字段：submitterDeptId”崩溃。

GREEN: `npx.cmd --yes --package @playwright/cli playwright-cli --session showroom-product-ai-cover run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260520-showroom-product-ai-cover-generation\scripts\debug-product-page.mjs` -> PASS，真实产品页可见搜索框、表格和产品行。

GREEN: `npx.cmd --yes --package @playwright/cli playwright-cli --session showroom-product-ai-cover run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260520-showroom-product-ai-cover-generation\scripts\debug-approval-page.mjs` -> PASS，真实审批页可见待办列表，no-dept 提交记录已正常渲染。

BLOCKER: `npx.cmd --yes --package @playwright/cli playwright-cli --session showroom-product-ai-cover run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260520-showroom-product-ai-cover-generation\scripts\verify-product-ai-cover-published-only.mjs` -> FAIL，真实点击 `AI生成` 后后端返回 `403 {"code":30001,"message":"Sorry, your account balance is insufficient"}`。当前阻塞已收敛为外部 SiliconFlow 图片账户余额不足。
