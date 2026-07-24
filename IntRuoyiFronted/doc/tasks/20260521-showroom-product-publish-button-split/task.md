# 任务：拆分产品管理独立发布按钮

## Goal

按用户最新要求，将 `展厅 -> 产品管理` 中产品发布入口收敛为列表行内唯一 `发布` 按钮，位置固定在 `删除` 左边；基础信息与详细信息弹窗中的 `保存并发布` 统一改为只保存，不改变非企宣用户的审批提交流程，不引入 fallback、mock、静默降级或后端兼容分支。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\showroom-admin\components\ProductListTable.vue`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\showroom-admin\index.vue`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\showroom-admin\product\ProductDetailDialog.vue`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\showroom-product-publish-entry.spec.js`
- 与本任务直接相关的 `doc/tasks/20260521-showroom-product-publish-button-split/**`

## Non-Scope

- 不修改审批中心、审批签名流程与非企宣角色提交审批逻辑。
- 不新增后端 `/showroom/product/publish` 接口或改动后端契约，除非前端验证证明现有契约无法支撑本需求。
- 不顺带改动产品列表其他列的已存在文案压缩、封面列、指派流程或导入导出逻辑。

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-company-ai-script-error\task.md`
- Status before this task: `Completed on 2026-05-21`
- Additional switched task record: `D:\ProjectPackage\Int\IntRuoyi\doc\tasks\20260521-showroom-product-basic-info-narration-not-found-fix\task.md`
- Impact: 同仓最近任务已完成；用户指定切换的旧根任务已标记为 `Blocked`，不阻塞本次前端实施。

## Repository Status Check

- Repository: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3`
- Current state: 仓库存在与本任务无关的进行中文档与源码改动，包括产品列表既有文案压缩和封面列改动。
- Impact: 本任务只允许叠加修改本次发布入口拆分直接涉及的文件，不能回退或覆盖现有在途改动。

## Milestones

- [x] M1: 创建新任务文档，并补记用户切换的旧任务阻塞状态。
- [ ] M2: 记录 BDD，并先补“发布入口唯一化”的 RED 回归。
- [ ] M3: 完成列表独立发布入口与两个弹窗保存入口收敛的最小实现。
- [ ] M4: 跑通定向源码级回归、TypeScript 检查与真实页面验证。
- [ ] M5: 更新证据、执行 cleanup preview，并准备提交范围。

## Expected Verification

- `node tests/e2e/showroom-product-publish-entry.spec.js`
- `node tests/e2e/showroom-product-basic-info-narration-move.spec.js`
- `node tests/e2e/showroom-product-detail-basic-info.spec.js`
- `node tests/e2e/showroom-product-whole-assignment.spec.js`
- `pnpm ts:check`
- Playwright 真实页面验证 `http://localhost:8081/showroom/product`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-product-publish-button-split\frontend-feature-evidence.md`
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260521-showroom-product-publish-button-split --mode preview`

## Current Status

Completed on 2026-05-21.

已完成产品管理发布入口拆分：列表行新增唯一 `发布` 按钮并固定在 `删除` 左侧；企宣账号基础信息与详细信息弹窗都已收敛为单一 `保存` 按钮；编辑账号真实页面验证保留 `保存草稿 + 提交审批`，且无列表发布入口。

## Blockers And Impact

- Blocker: none.
- Impact:
  - 企宣用户的发布入口已统一到产品列表。
  - 列表发布前会读取当前产品详情与中文讲解稿，仅在讲解稿属于当前 revision 时一并带入发布请求。
  - 非企宣编辑用户真实链路下仍保留原审批提交入口，没有回归成直发路径。

## Final Verification Result

- PASS: `node tests/e2e/showroom-product-publish-entry.spec.js`
- PASS: `node tests/e2e/showroom-product-basic-info-narration-move.spec.js`
- PASS: `node tests/e2e/showroom-product-detail-basic-info.spec.js`
- PASS: `node tests/e2e/showroom-product-whole-assignment.spec.js`
- PASS: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check`
- PASS: `npx.cmd --yes --package @playwright/cli playwright-cli --session showroom-product-publicity-entry run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-product-publish-button-split\scripts\verify-showroom-product-publicity-publish-entry.mjs`
- PASS: `npx.cmd --yes --package @playwright/cli playwright-cli --session showroom-product-editor-entry run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-product-publish-button-split\scripts\verify-showroom-product-editor-submit-entry.mjs`
- PASS: 真实接口复核 `E2E-PUBLISH-1779350997526` 已变为 `PUBLISHED`，`revisionId=1329`，中文讲解稿 `sourceRevisionId=1329` 且脚本文本为 `发布入口讲解稿 1779350997526`。
- PASS: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260521-showroom-product-publish-button-split --mode preview`
- PASS: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260521-showroom-product-publish-button-split --mode apply`

## Cleanup Result

- 已按 preview 结果删除本次任务的一次性前端证据文件、Playwright 脚本与截图。
- 当前任务目录仅保留 `task.md` 与 `execution-log.md` 作为主记录。
