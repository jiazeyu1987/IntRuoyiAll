# eDHR 批次工序状态背景优化

## 任务目标
- 隐藏批次详情左侧普通工序卡片右侧的“0/1 已完成”等状态文字。
- 已完成工序使用淡绿色背景。
- 正在填写工序使用淡黄色背景。
- 尚未开始工序保持当前白色背景和边框样式。
- 不改变工序顺序、名称、点击行为、任务状态或后端接口。

## 上一任务检查
- 上一任务 `doc/tasks/20260710-edhr-batch-process-order-layout/` 状态为 `completed`。
- 当前目标组件存在其他任务的并行未提交改动；本任务仅增加普通工序状态类、背景样式和对应回归测试，不覆盖其他改动。

## 经验门禁
- PowerShell / UTF-8：中文文档和命令输出使用显式 UTF-8，不使用 PowerShell 默认编码写文件。
- 缺陷修复：必须先以静态契约复现状态文字仍显示、状态背景类缺失，再实施最小修复。
- 前端交付：保持现有 API、路由、权限、任务状态和点击行为，仅调整用户明确指定的状态呈现。
- 前端样式：已完成使用淡绿色、填写中使用淡黄色、未开始保持白色；颜色之外保留当前选中边框，避免破坏现有交互识别。

## 设计约束检查
- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；由统一的工序组状态函数生成语义类，模板和样式不重复推断任务状态。
- `是否存在临时补丁或绕过`：否。

## BDD 场景
- BDD: 隐藏完成计数 -> Given 普通工序包含一个或多个表单任务 / When 用户查看左侧工序列表 / Then 卡片不显示“0/1 已完成”等完成计数文字。
- BDD: 已完成背景 -> Given 普通工序的必填任务全部完成或跳过 / When 用户查看该工序 / Then 卡片使用淡绿色背景。
- BDD: 正在填写背景 -> Given 普通工序至少一个必填任务已进入填写或处理状态且尚未全部完成 / When 用户查看该工序 / Then 卡片使用淡黄色背景。
- BDD: 未开始背景 -> Given 普通工序全部必填任务仍为待打开 / When 用户查看该工序 / Then 卡片保持当前白色背景。

## 里程碑
1. [已完成] 建立任务文档、经验门禁和 BDD 场景。
2. [已完成] 新增状态文字隐藏和背景状态 RED 回归测试。
3. [已完成] 实现工序组状态类和淡色背景。
4. [已完成] 运行定向回归、类型检查和真实页面验证。
5. [已完成] 更新证据、提交并完成任务收尾。

## 预期验证
- `node tests/e2e/edhr-batch-process-state-background-static.spec.js`
- `node tests/e2e/edhr-batch-process-card-density-static.spec.js`
- `node tests/e2e/edhr-batch-process-order-layout-static.spec.js`
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm.cmd ts:check`
- 本机真实批次详情只读验证状态文字已隐藏，三种背景状态符合任务数据。

## Cleanup Candidates
- 本任务未生成独立输出目录。
- 已清理 `bug-regression-evidence.md` 和 `frontend-feature-evidence.md`。
- 保留 `task.md`、`execution-log.md` 和 `verification-report.md`。

## 当前状态
- COMPLETED：完成计数已隐藏；已完成、填写中、未开始三种语义背景已实现。真实批次 `900000000480` 验证粗洗工序为淡黄色、其余未开始工序保持原背景，页面无完成计数文字且 MES 写请求为 0；淡绿色完成态由静态状态契约覆盖。实现提交为 `5c53e06b8`，收尾清理已通过。

## Current Status
completed
