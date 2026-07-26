# Task: 隐藏 eDHR 放行资料限制红框说明

## Task Goal

按用户截图隐藏个人中心配置页中 `eDHR 放行资料限制` 卡片红框内的辅助说明、默认关闭标签、每个开关项说明文案和当前配置 hash；保留标题、四个开关标签、开关交互、确认保存、错误显示和接口契约。

## Milestones

- [x] 识别截图对应组件与现有契约。
- [x] 建立任务专用静态 RED 合同。
- [x] 实现页面级隐藏，不改变保存逻辑。
- [x] 运行聚焦合同、既有合同和类型检查。
- [x] 完成证据、提交与推送。

## Expected Verification

- `node tests/e2e/edhr-release-dossier-requirement-copy-hidden-static.spec.js`
- `node tests/e2e/edhr-release-dossier-requirement-setting-static.spec.js`
- `pnpm ts:check`

## Current Status

completed

## 经验门禁

- 已读取 `docs/experience-index.md`。
- 命中 `docs/frontend-development.md#前端静态契约隔离门禁`：本次只隐藏截图红框说明，用任务专用最小静态合同覆盖，不借机修改无关逻辑。
- 命中 `docs/e2e-rules.md#静态合同与真实-e2e-同步门禁`：同步调整既有静态合同中已废弃的可见文案断言。
- 命中 `docs/powershell-memory.md#脏工作区基线门禁`：当前工作区存在本任务开始前未跟踪并行任务文件，实施前先单独保存基线。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，直接移除不需要渲染的辅助文案，保留正式状态与交互链路。
- `是否存在临时补丁或绕过`：否。

## Cleanup Keep

- doc/tasks/20260727-edhr-release-dossier-hide-helper-copy/frontend-feature-evidence.md
- doc/tasks/20260727-edhr-release-dossier-hide-helper-copy/bug-regression-evidence.md
