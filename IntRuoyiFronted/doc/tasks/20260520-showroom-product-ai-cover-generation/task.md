# 任务：展厅产品 AI 封面生成入口

## 目标

在 `展厅 / 产品管理` 的产品基础信息弹窗中，于 `封面` 上传控件旁新增 `AI生成` 按钮。按钮需要基于当前已填写的产品基础信息触发 AI 封面生成，并回填生成后的封面地址；但只有已经通过基础信息审核的产品才允许真正发起生成。未通过审核时，点击后必须明确提示“需要产品基础信息经过审核之后才可以 AI 生成封面”，不得静默降级或走 fallback。

## 范围

- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\api\showroom-admin\index.ts`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\showroom-admin\approval\contracts.ts`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\showroom-admin\index.vue`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\scripts\showroom-admin-product-cover-field.test.mjs`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\scripts\showroom-admin-workflow-workbenches.test.mjs`
- 需要时补充的 showroom 产品定向前端测试
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260520-showroom-product-ai-cover-generation\**`

## 非范围

- 不重做展厅后台整体布局。
- 不新增测试专用前端入口、假按钮、mock 数据或 fallback 文案。
- 不修改与产品封面生成无关的审批流规则。

## 前置任务检查

- 上一个前端同仓库任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260520-showroom-product-codex-bilingual-narration\task.md`
- 启动前状态：`Completed on 2026-05-20`
- 影响：前端上一任务已完成，本次可以继续扩展产品基础信息弹窗中的 AI 资产入口。

## 里程碑

- [x] M1：记录本次前端任务文档、BDD 场景与验证口径。
- [x] M2：先补 RED 测试，锁定 `封面` 旁 `AI生成` 入口与审核前提示行为。
- [x] M3：接入前端按钮、审批状态判断、加载态和生成成功后的封面回填。
- [x] M4：完成前端验证、证据更新与 cleanup preview。

## 预期验证

- `node --test scripts/showroom-admin-product-cover-field.test.mjs`
- 需要时补充的 showroom 产品前端定向测试
- `npx.cmd eslint src/api/showroom-admin/index.ts src/views/showroom-admin/index.vue`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260520-showroom-product-ai-cover-generation\frontend-feature-evidence.md`
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260520-showroom-product-ai-cover-generation --mode preview`

## 当前状态

Completed on 2026-05-20. 产品基础信息弹窗已在 `封面` 上传区域旁接入 `AI生成` 按钮；未审核产品点击后会提示“需要产品基础信息经过审核之后才可以AI生成封面”，审核通过的产品会调用真实后端接口并把返回的封面地址回填到当前表单。

## 最终验证结果

- PASS：`node --test scripts/showroom-admin-product-cover-field.test.mjs`
- PASS：`node --test scripts/showroom-admin-workflow-workbenches.test.mjs scripts/showroom-admin-product-cover-field.test.mjs`
- PASS：`npx.cmd eslint src/api/showroom-admin/index.ts src/views/showroom-admin/index.vue scripts/showroom-admin-product-cover-field.test.mjs`
- PASS：`npx.cmd eslint src/views/showroom-admin/approval/contracts.ts scripts/showroom-admin-workflow-workbenches.test.mjs`
- PASS：`npx.cmd --yes --package @playwright/cli playwright-cli --session showroom-product-ai-cover run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260520-showroom-product-ai-cover-generation\scripts\verify-product-ai-cover-warning.mjs`
- PASS：`npx.cmd --yes --package @playwright/cli playwright-cli --session showroom-product-ai-cover run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260520-showroom-product-ai-cover-generation\scripts\debug-product-page.mjs`
- PASS：`npx.cmd --yes --package @playwright/cli playwright-cli --session showroom-product-ai-cover run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260520-showroom-product-ai-cover-generation\scripts\debug-approval-page.mjs`
- PASS：`python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260520-showroom-product-ai-cover-generation\frontend-feature-evidence.md`
- PASS：`python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260520-showroom-product-ai-cover-generation --mode preview`
- PASS：后端定向 integration tests 已验证审核通过产品的真实接口成功路径。

## 阻塞

- 已解除：测试租户缺少 `showroom_publicity` 角色绑定。已在本地运行库为 `tenant_id=122` 的 `aoteman` 补齐 `showroom_publicity` 角色绑定，使真实产品提交审批和企宣审批页面可继续验证。
- 已解除：live MySQL `showroom_change_request.submitter_dept_id` 漂移为 `NOT NULL`。已按源码 SQL 基线修回 `DEFAULT NULL`，使无部门用户的真实审批链路可继续运行。
- 已解除：审批页前端把 `submitterDeptId` 当作必填 number，导致真实 no-dept 审批记录整页报错。现已改为允许 `null` 并通过定向测试。
- 当前最终外部 blocker：真实点击 `AI生成` 已经打到后端与外部图片服务，但 SiliconFlow 返回 `403 {"code":30001,"message":"Sorry, your account balance is insufficient"}`。这说明当前图片模型链路受上游账户余额限制阻塞，无法在本地真实环境产出封面图。
- 当前前端目标文件 `src/api/showroom-admin/index.ts` 与 `src/views/showroom-admin/index.vue` 在任务开始前已存在未提交改动。按“不得混提无关改动”规则，本次未自动执行 Git commit，避免把当前线程之外的修改一起提交。
