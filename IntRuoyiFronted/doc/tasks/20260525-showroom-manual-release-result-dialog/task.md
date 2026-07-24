# 任务：展厅手动发布结果改为弹框提示

## 任务目标

- 点击“手动发布展厅”后：
  - 发布成功时弹框显示发布成功信息
  - 发布失败时弹框显示失败原因
- 保留现有确认弹窗、loading 和真实接口调用，不改后端契约。

## 非目标

- 不修改按钮位置与权限可见性。
- 不修改后端发布逻辑。
- 不把失败原因改成静默 toast 或吞错。

## 前序任务检查

- 已检查上一同仓任务：`D:\\ProjectPackage\\Int\\IntRuoyi\\yudao-ui-admin-vue3\\doc\\tasks\\20260525-showroom-manual-release-run-check\\task.md`
- 上一任务状态：`已完成`
- 影响：上一任务已完成，不阻塞本次前端提示方式调整。

## 里程碑

- [ ] M1：建立任务记录并补 RED 测试，锁定成功/失败都必须弹框。
- [ ] M2：将公司工作台发布结果从 toast 改为弹框。
- [ ] M3：运行定向测试和静态检查。
- [ ] M4：更新任务文档和执行日志。

## 预期验证

- `node --test scripts/showroom-admin-manual-release-button.test.mjs`
- `node node_modules\\eslint\\bin\\eslint.js src\\views\\showroom-admin\\company\\CompanyWorkbench.vue scripts\\showroom-admin-manual-release-button.test.mjs`

## 当前状态

状态：已完成

## Current Status

Completed

## Completed Work

- 已将公司工作台“手动发布展厅”的结果提示从 toast 改为弹框。
- 发布成功时现在使用 `message.alertSuccess(...)` 显示成功信息与 releaseId。
- 发布失败时现在使用 `message.alertError(...)` 显示真实失败原因。
- 已保留原有确认弹窗、loading 和真实接口调用链路。

## Final Verification

- RED: `node --test scripts/showroom-admin-manual-release-button.test.mjs` -> FAIL，处理器仍使用 `message.success(...)` / `message.error(...)`。
- GREEN: `node --test scripts/showroom-admin-manual-release-button.test.mjs` -> PASS。
- GREEN: `node node_modules\\eslint\\bin\\eslint.js src\\views\\showroom-admin\\company\\CompanyWorkbench.vue scripts\\showroom-admin-manual-release-button.test.mjs` -> PASS。

## Note

- 本次只调整结果提示方式；未额外执行真实发布，以避免再次触发实际 release 变更。
