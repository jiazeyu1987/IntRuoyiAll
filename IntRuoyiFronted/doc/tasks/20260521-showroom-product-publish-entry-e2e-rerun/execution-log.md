# Execution Log: 20260521-showroom-product-publish-entry-e2e-rerun

BDD: publicity list publish entry should remain the only publish path -> Given 真实企宣账号进入 `http://localhost:8081/showroom/product` / When 在列表中创建、编辑并发布真实产品 / Then 行内按钮顺序必须包含 `发布` 且位于 `删除` 左侧，基础信息与详细信息弹窗页脚都不能再出现 `保存草稿 / 提交审批 / 保存并发布`。

BDD: publicity list publish should bind the current revision narration -> Given 企宣账号在基础信息弹窗中为当前 revision 保存中文讲解稿 / When 回到列表点击 `发布` / Then 真实产品状态必须变为 `PUBLISHED`，且中文讲解稿 `sourceRevisionId` 必须等于新发布 revision。

BDD: editor user should keep submit flow and no publish button -> Given 企宣账号已把真实产品整单指派给真实编辑账号 / When 编辑账号进入同一路径查看该产品 / Then 列表中不应出现 `发布` 按钮，基础信息与详细信息弹窗页脚必须保留 `保存草稿 + 提交审批`。

GREEN: publicity 真实 Playwright 回放 -> PASS，产品 `E2E-PUBLISH-1779353074651` 列表行按钮顺序为 `语音 / 指派 / 基础 / 详细 / 发布 / 删除`，基础信息页脚为 `取消 / 保存`，详细信息页脚为 `关闭 / 保存`，保存讲解稿后可直接从列表发布并进入 `PUBLISHED`。

GREEN: editor 真实 Playwright 回放 -> PASS，产品 `E2E-EDITOR-1779353074705` 经真实整单指派后，编辑账号列表无 `发布` 按钮，基础信息页脚为 `取消 / 保存草稿 / 提交审批`，详细信息页脚为 `关闭 / 保存草稿 / 提交审批`。

GREEN: 真实接口复核 -> PASS，`E2E-PUBLISH-1779353074651` 当前状态为 `PUBLISHED`，`revisionId=1335`，中文讲解稿 `sourceRevisionId=1335`，脚本文本为 `发布入口讲解稿 1779353074651`；`E2E-EDITOR-1779353074705` 当前状态为 `IN_FILLING`，`activeAssignment.status=OPEN`。

GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260521-showroom-product-publish-entry-e2e-rerun --mode preview` -> PASS，仅预览删除本次 E2E 复验脚本与截图。

GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260521-showroom-product-publish-entry-e2e-rerun --mode apply` -> PASS，已删除临时 Playwright 脚本与截图，仅保留主记录。
