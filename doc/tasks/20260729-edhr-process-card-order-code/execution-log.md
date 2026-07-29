# Execution Log

## User Intent

- 用户要求“切换工序”弹框内工序卡片可以更高一些、字体变大。
- 用户要求每个卡片红框里的明细内容不显示。
- 用户要求截图黄框位置显示订单号。

## Preflight

- 已读取 `frontend-feature-delivery` 技能和 `references/frontend-contract.md`。
- 已读取 `docs/task-closeout-rules.md`、`docs/frontend-development.md`、`docs/powershell-encoding.md`、`docs/powershell-memory.md`。
- 当前分支：`int_main`；remote：`origin https://github.com/jiazeyu1987/IntRuoyiAll.git`。
- `git status --short --branch` 显示分支 ahead 2，且存在并行任务文件 `doc/tasks/20260729-admin-submitted-content-e2e/admin-submitted-content-real.e2e.js` 脏改动；按项目规则将在本任务实现前基线保存，不混入本任务文档和实现。
- 已读取 `docs/experience-index.md`；本任务适用门禁为 eDHR 工序切换正式链路、前端静态合同隔离、脏工作区基线和 PowerShell 测试退出码门禁。

## BDD

- BDD: 工序卡片放大展示 -> Given 用户打开“切换工序”弹框, When 工序卡片渲染, Then 卡片高度更高、工序名和状态字体更大。
- BDD: 隐藏卡片明细说明 -> Given 工序卡片包含状态下方的序号/表单项/直接前置说明, When 弹框显示, Then 这些红框位置明细不再显示。
- BDD: 弹框顶部展示订单号 -> Given 当前填写页有订单号上下文, When 用户打开“切换工序”弹框, Then �