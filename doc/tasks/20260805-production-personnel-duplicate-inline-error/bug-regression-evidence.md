# Bug

新增临时工遇到同名有效员工时，`submitCreateTemporaryEmployee` 直接调用全局 `ElMessage.error`，错误提示脱离“新增人员”弹窗，且没有与弹窗交互一致的清理机制。

# Expected

后端返回的“当前生产组长已有同名有效员工，请修改姓名或增加后缀”应以红字显示在新增人员弹窗标题栏内，不触发全局错误；提示应支持 6 秒自动消失、手动关闭、修改显示名清除和关闭弹窗清理。

# Reproduction

- 页面路径：生产组长 -> 人员管理 -> 新增人员 -> 手动录入临时工。
- 输入与当前生产组长已有有效员工相同的显示名并提交。
- RED command：`node tests\e2e\production-personnel-duplicate-inline-error-static.spec.js`。

# Root Cause

- 弹窗只有 `title="新增人员"`，没有可承载局部错误的 header slot。
- 临时工提交 catch 直接执行 `ElMessage.error(resolveErrorMessage(...))`。
- 页面没有弹窗错误状态、定时器、手动关闭或弹窗关闭清理逻辑。

# Regression Test

- 新增 `tests/e2e/production-personnel-duplicate-inline-error-static.spec.js`。
- 合同覆盖标题栏局部红字、`role="alert"`、非全局错误、6 秒自动清理、手动关闭、修改显示名清除、弹窗关闭清理和组件卸载清理。

# RED

RED: `node tests\e2e\production-personnel-duplicate-inline-error-static.spec.js` -> FAIL，新增人员弹窗没有自定义 header，临时工提交仍使用全局 `ElMessage.error`。

# GREEN

GREEN: `node tests\e2e\production-personnel-duplicate-inline-error-static.spec.js` -> PASS。

# Verification

- PASS: `node tests\e2e\production-personnel-duplicate-inline-error-static.spec.js`
- PASS: `node tests\e2e\production-personnel-add-dialog-static.spec.cjs`
- PASS: `node tests\e2e\production-personnel-management-static.spec.cjs`
- PASS: `node tests\e2e\production-leader-remove-header-content-static.spec.js`
- PASS: `pnpm ts:check`
- PASS: task-path `git diff --check`，仅有 LF/CRLF 归一化 warning

# Risk

- 仅改变临时工新增失败的展示归属；后端请求、错误原文、成功提示、列表刷新和其它人员操作保持不变。
- 其它非同名的临时工新增错误也在当前操作弹窗内展示，不吞异常。
- 未执行真实浏览器 E2E；未启动或修改本地服务。

# Blockers

- 当前 `int_main` 领先 `origin/int_main`，包含非本任务混合基线提交；推送前必须单独复核。

# Follow-up Actions

- 使用 frontend feature validator 和 bug regression validator 校验证据。
- 提交前按共享分支门禁检查目标文件 diff 和 staged 清单。

