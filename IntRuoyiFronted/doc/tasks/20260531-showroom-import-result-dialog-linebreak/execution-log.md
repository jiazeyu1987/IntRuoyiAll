# 执行日志：展厅产品导入结果弹窗换行与跳过提示核查

BDD: 导入结果弹窗按行展示统计 -> Given 导入接口返回包含换行的结果提示 / When 前端展示系统提示弹窗 / Then 用户看到分行统计文本，不看到原始 `<br/>` 标签。

BDD: 图片差异产品不应被旧后端误导 -> Given `产品资料正式版.xlsx` 中 `product_001` 有产品图且与系统封面不同 / When 使用包含封面 hash 修复的新后端导入 / Then `product_001` 不应因固定 URL 复用被归为相同产品。

REPRO: 用户截图显示导入结果弹窗内容为 `总行数：160<br/>成功发布：0<br/>跳过无变化...`，说明普通文本弹窗收到原始 `<br/>` 字符串。

ROOT CAUSE: `ShowroomProductImportForm.vue` 中 `handleImportSuccess(...)` 使用 `textLines.join('<br/>')` 后调用 `message.alert(...)`；`useMessage.alert` 默认不会启用 HTML 渲染，导致 `<br/>` 原样展示。

ROOT CAUSE: 本地 8081 前端的 `.env.local` 指向 `http://127.0.0.1:48081/admin-api`；48081 后端进程 PID 54816 启动时间为 2026-05-31 17:35，运行 `backend-companytype-20260531-173100.jar`，早于后端提交 `20ab7c1f4a` 的封面 hash 修复。48082 进程 PID 51936 也同样是 2026-05-31 17:34 的旧 jar。因此截图中 `product_001` 仍被跳过符合旧后端未重启现象。

RED: node scripts/showroom-admin-product-import-form.test.mjs -> FAIL，新增 `Showroom product import result dialog renders line breaks without raw HTML tags` 捕获 `join('<br/>')`；同时发现既有标题断言未覆盖当前 `产品 Excel 导入` 标题。

GREEN: node scripts/showroom-admin-product-import-form.test.mjs -> PASS，4 tests。导入结果弹窗不再把 `<br/>` 作为普通字符串传给 alert，而是用 VNode 逐行渲染文本。

GREEN: pnpm ts:check -> FAIL，Node 默认约 4GB heap 下 `vue-tsc` OOM，未产生类型错误明细。

GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS。相同 `ts:check` 脚本在足够 Node heap 下完成。

GREEN: python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc/tasks/20260531-showroom-import-result-dialog-linebreak/bug-regression-evidence.md -> PASS，Bug regression evidence is valid。

GREEN: python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260531-showroom-import-result-dialog-linebreak --mode preview --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --worktree-closeout off --json -> PASS，预览保留 `task.md` 与 `execution-log.md`，仅识别临时 `bug-regression-evidence.md` 为可清理，无 blocked/warnings。

GREEN: python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260531-showroom-import-result-dialog-linebreak --mode apply --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --worktree-closeout off --json -> PASS，已清理临时 `bug-regression-evidence.md`，保留正式任务记录。
