# 任务：调整展厅手动发布按钮位置

## 任务目标

- 将“手动发布展厅”按钮从 `showroom-admin/index.vue` 的全局顶部工具条，调整到公司工作台头部动作区。
- 按钮必须显示在“编辑公司”的右侧，保持现有发布接口、确认交互、loading 和错误提示不变。

## 非目标

- 不修改 `POST /showroom/release/publish` 后端契约。
- 不改变版本中心入口、公司编辑弹框或其他工作台操作。
- 不重做公司页整体布局，仅调整发布按钮归属和位置。

## 前序任务检查

- 已检查上一同仓任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260524-showroom-manual-release-button\task.md`
- 上一任务状态：`已完成`
- 影响：上一任务已完成，当前仅做按钮位置微调，不阻塞继续开发。

## 里程碑

- [ ] M1：建立任务记录并补 RED 测试，锁定按钮必须位于公司页“编辑公司”右侧。
- [ ] M2：移除首页全局工具条上的按钮，实现按钮归属到 `CompanyWorkbench.vue`。
- [ ] M3：保持确认、loading、成功/失败提示语义不变，并跑定向验证。
- [ ] M4：更新任务文档、执行日志并提交本任务改动。

## 预期验证

- `node --test scripts/showroom-admin-manual-release-button.test.mjs`
- `node node_modules\\eslint\\bin\\eslint.js src\\views\\showroom-admin\\company\\CompanyWorkbench.vue src\\views\\showroom-admin\\index.vue scripts\\showroom-admin-manual-release-button.test.mjs`
- Playwright 真实路径：`http://127.0.0.1:8081/showroom/company`

## 当前状态

状态：已完成

## Current Status

Completed

## Completed Work

- 已将“手动发布展厅”按钮从 `showroom-admin/index.vue` 的全局顶部工具条移除。
- 已把按钮与发布交互迁移到 `CompanyWorkbench.vue` 头部动作区。
- 已确认动作顺序为：状态标签 -> `进入版本中心` -> `编辑公司` -> `手动发布展厅`。
- 已保持原有确认框、loading、成功消息与失败直出语义不变。

## Final Verification

- RED: `node --test scripts/showroom-admin-manual-release-button.test.mjs` -> FAIL，按钮仍在首页全局工具条，`CompanyWorkbench.vue` 不含发布入口。
- GREEN: `node --test scripts/showroom-admin-manual-release-button.test.mjs` -> PASS。
- GREEN: `node node_modules\\eslint\\bin\\eslint.js src\\views\\showroom-admin\\company\\CompanyWorkbench.vue src\\views\\showroom-admin\\index.vue scripts\\showroom-admin-manual-release-button.test.mjs` -> PASS。
- GREEN: Playwright 真实前端验证使用当前源码临时端口 `http://127.0.0.1:18082/showroom/company` -> PASS，`手动发布展厅` 按钮可见，且按钮横坐标位于 `编辑公司` 右侧。
- GREEN: Playwright 真实确认发布 -> PASS，页面实际发出 `POST /admin-api/showroom/release/publish`，当前测试租户仍返回后端真实 blocker `SHOWROOM_TARGET_NOT_FOUND: live product revision not found`，前端 toast 原样展示该错误。

## Note

- 本地默认 `8081` 当时由另一棵 `IntRuoyi` worktree 的前端占用；为避免干扰现有联调，本次真实页面验证使用当前源码临时启动的 `18082` 端口。
