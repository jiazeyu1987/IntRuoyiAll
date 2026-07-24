# Task: 电子批记录真实 DOC E2E

## Goal

使用用户指定的真实 Word 文档 `C:\Users\BJB110\Desktop\2\2\RE-PP-ID-01（A 1）球囊扩张压力泵生产记录(1).doc`，对当前 `/mes/pro/batch-record-template` 页面执行一次真实 E2E：登录、切换到 `文件解析导入`、上传真实 `.doc`、等待解析结果、验证候选与只读版式预览、提交选中候选、回列表确认模板创建，再删除测试生成模板完成清理。

## Scope

- 在前端仓库创建本次任务文档、执行日志与可复跑 Playwright 脚本。
- 仅验证真实页面和真实后端链路；不使用 mock 数据，不走接口捷径替代前端上传路径。
- 如当前实现已经满足要求，则不改生产代码，只记录验证证据。
- 如验证暴露缺陷，则在当前仓库范围内修复阻塞并复跑。

## Previous Task Check

- Previous frontend task: `doc/tasks/20260516-sidebar-brand-logo-replace/task.md`
- Status before this task: completed.
- Impact: the sidebar branding task is fully closed and does not block this electronic batch record real-DOC E2E verification.

## BDD Scenarios

- BDD: 真实 DOC 上传后出现解析摘要 -> Given 管理员进入 `/mes/pro/batch-record-template` 并切换到 `文件解析导入`, When 上传用户指定的真实 `.doc`, Then 页面展示导入摘要、候选表列表和右侧只读版式预览。
- BDD: 清空勾选时主提交动作被显式阻止 -> Given 已解析出候选表, When 用户清空所有候选勾选, Then `提交选中` 主按钮保持禁用，页面不允许空提交。
- BDD: 真实 DOC 候选可提交并回到列表 -> Given 至少一个真实候选表被勾选, When 用户点击 `提交选中`, Then 页面提交成功、切回 `模板列表`、并显示新创建模板。
- BDD: 提交生成模板可查看并删除 -> Given 列表中出现本次真实 DOC 生成的模板, When 用户打开 `查看版式` 并随后删除该模板, Then 只读预览可见且删除成功。

## Milestones

1. [x] M1: 创建任务包并记录 BDD 场景。
2. [x] M2: 添加针对用户指定真实 `.doc` 的 Playwright E2E 脚本。
3. [x] M3: 运行真实 E2E 并记录 RED/GREEN 或阻塞。
4. [x] M4: 更新任务文档、执行日志与验证结论。
5. [x] M5: 如验证通过，仅提交当前任务相关文档与脚本。

## Expected Verification

- `npx.cmd --yes --package @playwright/cli playwright-cli run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260516-electronic-batch-record-real-doc-e2e\scripts\verify-electronic-batch-record-real-doc-e2e.mjs`

## Current Status

Completed. 用户指定的真实 `.doc` 文件已在真实页面链路中完成上传、解析、候选展示、只读预览、勾选提交、回列表确认与删除清理，且当前任务相关文档与脚本将单独提交。

## Final Verification Result

- `npx.cmd --yes --package @playwright/cli playwright-cli run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260516-electronic-batch-record-real-doc-e2e\scripts\verify-electronic-batch-record-real-doc-e2e.mjs` -> PASS
