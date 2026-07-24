# 执行日志：DCC OnlyOffice 纯滚轮缩放可行性与实现

## 2026-06-22

- 用户需求：`无 Ctrl 的纯滚轮也缩放`
- 任务目录：`doc/tasks/20260622-dcc-onlyoffice-pure-wheel-zoom/`
- 执行边界：本轮先验证纯滚轮在 OnlyOffice iframe 场景的真实事件边界，再决定是否修改组件。
- `BDD: 纯滚轮可直接缩放 OnlyOffice 预览 -> Given 用户在 DCC OnlyOffice 受控预览中阅读文档 When 在预览区域直接滚动滚轮 Then 预览应执行缩放而不是依赖 Ctrl 修饰键。`
- `BDD: 纯滚轮缩放不破坏受控阅读边界 -> Given 预览仍是受控只读文档 When 用户使用纯滚轮缩放 Then 编辑、下载、打印、复制限制必须保持。`
- 执行命令：读取 `frontend-feature-delivery/SKILL.md`、上一任务 `20260622-dcc-onlyoffice-zoom-controls` 文档与日志 -> PASS。
- 执行命令：`node -` + Playwright 实验（iframe 默认指针事件）-> PASS，结果 `{"parentWheel":0}`，确认鼠标位于 iframe 内时纯滚轮不会传到父容器。
- 执行命令：`node -` + Playwright 实验（iframe `pointer-events:none`）-> PASS，结果 `{"parentWheel":1}`，确认只有让外层接管 iframe 区域事件时，父容器才能收到纯滚轮。
- 当前结论：OnlyOffice 预览若要支持“无 Ctrl 纯滚轮缩放”，必须牺牲部分原生阅读交互，把 iframe 区域的滚轮长期交给外层；这是显式交互取舍，不应在未确认风险前直接交付。
- 用户确认：`还是加ctrl来控制放大缩小，防止牺牲上下滚动，同时有放大缩小按钮也可以`
- 最终处理：保留上一任务已交付的 `Ctrl + 滚轮` 缩放和放大/缩小/重置按钮，不继续实施“纯滚轮直接缩放”。
