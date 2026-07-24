# 生产工单工单编号复制按钮

## 任务目标

在 MES 生产工单列表的“工单编号”列中，为每个工单编号增加一个复制按钮。用户点击按钮后，将该行工单编号复制到系统剪切板，并看到明确成功提示。

## 非目标

- 不修改后端 API、数据结构或权限契约。
- 不改变工单编号点击进入详情的既有行为。
- 不引入 fallback、mock 数据或静默吞异常。

## 经验门禁

- PowerShell / Windows shell / 中文编码：已读取 `docs/powershell-memory.md`，中文读写与命令输出显式 UTF-8。
- 前端页面 / 表格 / 样式：已读取 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`，沿用生产工单列表当前表格密度与蓝白中性操作样式。
- 前端特性交付：已读取 `frontend-feature-delivery` 与 `frontend-contract.md`，本次只改前端页面行为与静态合同测试。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，在工单编号列直接提供复制入口，复用浏览器剪切板 API 并显式暴露失败。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

BDD: 复制生产工单编号 -> Given 用户打开生产工单列表并看到某行工单编号 / When 点击该编号旁边的“复制”按钮 / Then 系统将该行 `scope.row.code` 写入剪切板，并提示“工单编号已复制”。

## 里程碑

- [x] M1：新增静态回归测试，先复现工单编号列缺少复制按钮与复制处理函数。
- [x] M2：更新生产工单列表工单编号列，保留详情入口并增加复制按钮。
- [x] M3：运行 RED/GREEN 静态测试、eslint 与必要类型检查。
- [x] M4：更新任务证据、执行收尾预览，并按验证结果提交本任务改动。

## 预期验证

- `node tests/e2e/workorder-code-copy-button-static.spec.js`
- `pnpm.cmd exec eslint src/views/mes/pro/workorder/index.vue tests/e2e/workorder-code-copy-button-static.spec.js --format stylish`
- `pnpm.cmd ts:check`

## 当前状态

completed

## 完成记录

- 实现：生产工单列表“工单编号”列保留原点击详情入口，并在编号旁增加复制按钮。
- 交互：点击复制按钮调用 `navigator.clipboard.writeText(scope.row.code)`，成功后提示“工单编号已复制”；空编号或剪切板异常不静默吞错。
- 验证：静态合同、目标文件 eslint、高堆内存类型检查均通过。

## Cleanup Keep

- `doc/tasks/20260707-workorder-copy-code-button/frontend-feature-evidence.md`
