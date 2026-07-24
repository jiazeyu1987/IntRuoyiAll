# 执行日志：DCC OnlyOffice 预览增加缩放按钮与滚轮缩放

## 2026-06-22

- 用户需求：`增加滚轮可以放大缩小，有放大缩小按钮可以放大缩小`
- 任务目录：`doc/tasks/20260622-dcc-onlyoffice-zoom-controls/`
- 执行边界：本轮只改 DCC OnlyOffice 只读预览组件和对应静态测试，不改后端接口，不放开受控权限。
- `BDD: 用户可通过按钮缩放 OnlyOffice 预览 -> Given 用户进入 DCC Office 受控预览 When 点击放大、缩小或重置按钮 Then 预览缩放比例应按预设步进变化并保留在受控只读模式。`
- `BDD: 用户可通过滚轮缩放 OnlyOffice 预览 -> Given 用户正在查看 DCC Office 受控预览 When 在预览区域使用滚轮进行缩放手势 Then 预览缩放比例应随滚轮方向变化且不放开编辑权限。`
- `BDD: 缩放增强不破坏受控边界 -> Given OnlyOffice 预览仍属于受控浏览 When 用户使用缩放交互 Then 编辑、下载、打印、复制禁用状态必须保持不变。`
- 执行命令：读取 `frontend-feature-delivery/SKILL.md`、`references/frontend-contract.md`、`FRONTEND_STYLE.md`、最近前端任务文档、`package.json` 与现有 OnlyOffice 静态测试 -> PASS。
- 执行命令：`apply_patch` -> PASS，创建 `task.md`、`execution-log.md` 与 `scripts/dcc-onlyoffice-zoom-controls.test.mjs`
- `RED: node scripts/dcc-onlyoffice-zoom-controls.test.mjs -> FAIL, 当前 OnlyOffice 组件缺少缩放工具条、缩放状态与滚轮绑定`
- 执行命令：本地 Playwright 小实验验证 iframe 场景 -> PASS，确认父容器默认收不到 iframe 内部滚轮事件，需在组件层做受控手势捕获策略
- 执行命令：`apply_patch` -> PASS，修改 `src/views/dcc/controlled-file/view/OnlyOfficeReadOnlyViewer.vue`，增加缩放工具条、缩放状态、缩放比例样式和 `Ctrl + 滚轮` 手势捕获
- 执行命令：`apply_patch` -> PASS，更新 `scripts/dcc-onlyoffice-readonly-config.test.mjs`，将旧的“不得出现 keydown”收紧为“不得依赖 clipboard/execCommand/navigator.clipboard 做复制保护”
- `GREEN: node scripts/dcc-onlyoffice-zoom-controls.test.mjs -> PASS`
- `GREEN: node scripts/dcc-onlyoffice-readonly-config.test.mjs -> PASS`
- `GREEN: $env:NODE_OPTIONS='--max-old-space-size=8192'; node node_modules/vue-tsc/bin/vue-tsc.js --noEmit -p tsconfig.relaxed.json -> PASS`
