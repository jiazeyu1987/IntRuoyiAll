# Execution Log: 20260521-showroom-product-publish-button-split

BDD: publicity 列表唯一发布入口 -> Given 企宣用户进入 `http://localhost:8081/showroom/product` 且产品行为 `DRAFT` 或 `REJECTED` / When 查看产品管理列表并打开基础信息或详细信息弹窗 / Then 列表行内必须出现唯一 `发布` 按钮且位于 `删除` 左边，两个弹窗内都不能再出现 `保存并发布`。

BDD: 保存后列表发布保留当前讲解稿 -> Given 企宣用户在基础信息弹窗保存了当前 revision 的中文讲解稿 / When 返回产品列表点击同一产品的 `发布` / Then 前端必须先读取当前产品详情与中文讲解稿，并仅在讲解稿 `sourceRevisionId` 等于当前产品 `revisionId` 时把该讲解稿一并传给 `publishProduct`，避免回退到旧 live 讲解稿。

BDD: 非 publicity 提交审批不回归 -> Given 非企宣用户打开基础信息或详细信息弹窗 / When 需要流转产品审批 / Then 页面仍保留现有 `保存草稿 + 提交审批` 路径，且列表中不存在独立 `发布` 入口。

RED: `node tests/e2e/showroom-product-publish-entry.spec.js` -> FAIL, `ProductListTable.vue` 尚未声明 `publish` emit，也没有列表独立 `发布` 按钮与 `index.vue` 绑定。

GREEN: `node tests/e2e/showroom-product-publish-entry.spec.js` -> PASS，列表组件已新增 `publish` emit 与 `发布` 按钮，`index.vue` 已绑定列表发布处理器，两个弹窗源码不再包含 `保存并发布`。

GREEN: `node tests/e2e/showroom-product-basic-info-narration-move.spec.js` -> PASS，基础信息弹窗继续持有讲解稿区域，详细信息弹窗未回退讲解稿编辑能力。

GREEN: `node tests/e2e/showroom-product-detail-basic-info.spec.js` -> PASS，详细信息弹窗仍不包含讲解稿 UI，发布入口拆分未破坏此前职责分离。

GREEN: `node tests/e2e/showroom-product-whole-assignment.spec.js` -> PASS，产品整单指派入口与绑定在当前列表压缩文案下仍有效。

GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS，在当前工作区需要提升 Node 堆内存后，TypeScript 检查通过，无新增类型错误。

GREEN: `npx.cmd --yes --package @playwright/cli playwright-cli --session showroom-product-publicity-entry run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-product-publish-button-split\scripts\verify-showroom-product-publicity-publish-entry.mjs` -> PASS，真实企宣链路创建产品 `E2E-PUBLISH-1779350997526` 后，列表行按钮顺序为 `语音 / 指派 / 基础 / 详细 / 发布 / 删除`，详细信息页脚为 `关闭 / 保存`，基础信息页脚为 `取消 / 保存`，保存讲解稿后列表发布成功且真实页面查询到状态变为 `PUBLISHED`。

GREEN: 真实接口复核 -> PASS，`E2E-PUBLISH-1779350997526` 的产品状态为 `PUBLISHED`、`revisionId=1329`，中文讲解稿 `sourceRevisionId=1329`，脚本文本为 `发布入口讲解稿 1779350997526`，确认列表发布绑定的是当前 revision 讲解稿而不是旧 live 讲解稿。

GREEN: `npx.cmd --yes --package @playwright/cli playwright-cli --session showroom-product-editor-entry run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-product-publish-button-split\scripts\verify-showroom-product-editor-submit-entry.mjs` -> PASS，真实企宣账号创建并指派产品 `E2E-EDITOR-1779351173143` 给 `showroomeditor` 后，编辑账号进入同一路径无列表 `发布` 按钮，基础信息页脚为 `取消 / 保存草稿 / 提交审批`，详细信息页脚为 `关闭 / 保存草稿 / 提交审批`。

GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260521-showroom-product-publish-button-split --mode preview` -> PASS，预览仅包含本次任务的临时证据文件、Playwright 脚本与截图。

GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260521-showroom-product-publish-button-split --mode apply` -> PASS，已删除临时证据与截图，仅保留 `task.md / execution-log.md`。
