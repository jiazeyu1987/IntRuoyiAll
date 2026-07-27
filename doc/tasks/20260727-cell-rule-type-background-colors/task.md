# 20260727 Cell Rule Type Background Colors

## Task Goal

让批记录单元格规则预览中不同字段类型的可填写单元格使用不同背景色显示，便于用户在规则确认页面快速区分文本、数字、日期、签名、下拉框等类型。

## Milestones

- [x] 建立 BDD 场景和最小 RED 静态合同。
- [x] 在单元格规则弹窗中按规则类型输出稳定样式类。
- [x] 补齐不同字段类型背景色样式，并保留选中、必填、不可填写等状态。
- [x] 运行目标验证并记录结果。
- [x] 完成 closeout 记录、提交与推送。

## Expected Verification

- `node tests/e2e/edhr-cell-rule-type-background-colors-static.spec.js`
- `node tests/e2e/edhr-cell-control-type-switch-static.spec.js`
- `node tests/e2e/batch-record-cell-rule-default-fullscreen-static.spec.js`
- `pnpm ts:check`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260727-cell-rule-type-background-colors/frontend-feature-evidence.md`

## Current Status

completed

## 经验门禁

- 已读取 `docs/task-closeout-rules.md`、`docs/frontend-development.md`、`docs/powershell-encoding.md`、`docs/powershell-memory.md`。
- `docs/experience-index.md` 已存在；本任务命中前端页面、表格、样式相关经验，已读取 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`，采用蓝灰运营台风格与淡色背景，不引入营销化或一次性页面色板。
- 静态合同与真实 E2E 同步门禁：本任务只修改静态样式与类名，不改真实写入路径；使用任务专用静态合同锁定当前行为。
- 前端静态契约隔离门禁：若全量 `pnpm ts:check` 或历史大契约先失败，必须使用本任务最小静态合同证明当前行为 RED/GREEN，并记录全量回归剩余阻塞。
- 工作区基线门禁：本任务开始前出现多轮并行脏改动，已按规则分别 baseline，当前任务只修改单元格规则背景色相关文件。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，通过规则类型映射到稳定 CSS 类与样式令预览可持续扩展。
- `是否存在临时补丁或绕过`：否。

## Cleanup Keep

- doc/tasks/20260727-cell-rule-type-background-colors/frontend-feature-evidence.md
- doc/tasks/20260727-cell-rule-type-background-colors/real-ui-cell-rule-colors.png
