# Execution Log

## User Intent

用户反馈点击 QA 规程“发布规程”时报错：`QA 检验规程发布失败，缺少必要检验规则：FIRST`。截图显示当前规程处于 DRAFT，发布动作在总览页触发。

## BDD Scenarios

BDD: publish regulation with configured first inspection -> Given QA 规程存在需要发布的检验项目且页面已包含首检 FIRST 配置 When 用户点击发布规程 Then 发布载荷必须包含 FIRST 必要检验规则并不得误报缺失。

BDD: publish regulation without configured first inspection -> Given QA 规程确实缺少首检 FIRST 配置 When 用户点击发布规程 Then 发布校验必须 fail fast 并提示缺少 FIRST。

## Evidence

- 2026-08-10: 已读取 bug-regression-fix-loop 技能、bug evidence contract、任务/PowerShell/前端/后端触发规则。

## RED

- Pending.

## GREEN

- Pending.

## Blockers

- Pending root-cause inspection.
