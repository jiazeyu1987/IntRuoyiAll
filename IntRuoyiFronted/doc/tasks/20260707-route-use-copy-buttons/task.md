# 工艺用途路线列表复制按钮

## 任务目标

- 在工艺用途路线列表的“路线编码”和“路线名称”两列增加和生产工单一致的图标复制按钮。
- 点击复制按钮后，将当前行对应的路线编码或路线名称复制到剪切板。
- 保留原有点击路线编码打开用途配置、点击路线名称打开源工艺路线详情的行为。

## 里程碑

- [x] M1：读取经验门禁并定位路线用途列表组件。
- [x] M2：添加 RED 静态契约，锁定两列复制按钮与剪切板行为。
- [x] M3：实现路线编码和路线名称复制按钮。
- [x] M4：运行目标静态测试、相关回归静态测试和目标文件 ESLint。
- [x] M5：完成任务记录并准备提交本任务前端改动。

## 预期验证

- `node tests/e2e/mes-route-use-copy-buttons-static.spec.js` 先失败后通过。
- `NODE_OPTIONS=--max-old-space-size=8192 pnpm.cmd ts:check`：当前被未触碰文件 `src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue(130,59)` 的既有 `closedAt` 类型错误阻塞。
- 静态契约确认 `RouteUsePage.vue` 复用 `useClipboard({ legacy: true })`，并为 `row.code`、`row.name` 分别提供复制按钮。

## 经验门禁

- PowerShell / Windows shell：已读取 `docs/powershell-memory.md`；中文输出、Markdown 读取和命令日志必须显式 UTF-8。
- 前端页面 / 表格 / 样式：已读取 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`；复制按钮采用紧凑行内文本按钮，不引入重型操作按钮或一页式改版。
- 前端功能交付：已读取 `frontend-feature-delivery` 与 `frontend-contract.md`；本次先记录 BDD 与 RED，再实施最小 UI 行为。
- 混合脏工作区：当前前端仓已有大量既有脏改；本任务只修改路线用途复制按钮相关文件和本任务文档，不回滚、不提交无关改动。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。复制能力复用项目既有 `useClipboard`，复制失败由依赖抛出，不静默降级为其它通道。
- 是否从根因和长期维护角度解决：是。直接在目标两列增加清晰、可复用的行内复制方法，并保留原详情/配置入口。
- 是否存在临时补丁或绕过：否。

## 当前状态

- 状态：completed
- 已完成：前置规则读取、目标组件定位、RED 静态契约、两列图标复制按钮实现、目标静态测试、相关静态回归测试和目标文件 ESLint。
- 验证：`node tests/e2e/mes-route-use-copy-buttons-static.spec.js`、`node tests/e2e/mes-route-use-source-route-detail-link-static.spec.js`、`pnpm.cmd exec eslint src/views/mes/pro/route-use/RouteUsePage.vue tests/e2e/mes-route-use-copy-buttons-static.spec.js` 均通过。
- 说明：全量类型检查此前被未触碰的 `BatchExecutionDetailPage.vue` 既有 `closedAt` 类型错误阻塞；按无关脏改隔离原则，本任务不修改该文件。
