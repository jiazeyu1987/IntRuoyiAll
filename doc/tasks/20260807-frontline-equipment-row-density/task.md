# 一线生产填设备行密度调整

## Task Goal

根据截图反馈，缩小一线生产“填设备”参数区域每一行的高度和文字大小，保持设备切换、参数输入、单位显示、异常状态和正式提交链路不变。

## Milestones

- [x] 创建任务文档并读取前端、E2E、编码和经验门禁。
- [x] 新增任务专用静态合同并跑出 RED。
- [x] 最小范围调整“填设备”参数行高度、间距和字号。
- [x] 跑目标静态合同、相邻静态合同、格式检查和技能证据校验。

## Expected Verification

- `node tests/e2e/frontline-production-device-row-density-static.spec.cjs`
- `node tests/e2e/edhr-frontline-fill-tabs-static.spec.cjs`
- `git diff --check`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260807-frontline-equipment-row-density/frontend-feature-evidence.md`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --self-test`

## Current Status

completed

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，直接收敛目标设备参数区域的 CSS 密度并用静态合同锁定。
- `是否存在临时补丁或绕过`：否。

## Applicable Gates

- `前端截图字号调整静态契约门禁`：本任务必须先新增目标选择器静态合同并跑 RED，再改最小 CSS；禁止全局字号覆盖或扩大成整页重设计。
- `Windows 换行与脚本行为同步`：新增静态合同读取 Vue SFC 时归一化 CRLF/LF，并将断言范围收窄到目标设备参数样式块。

## Closeout

- `frontend-feature-delivery` validator：PASS，关键 RED/GREEN 已归档到 `execution-log.md` 和 `verification-report.md`。
- `task-closeout-cleanup preview -> ready`：仅计划删除临时 `frontend-feature-evidence.md`，blocked/warnings 均为 none。
- `task-closeout-cleanup apply -> applied`：已删除临时 evidence 文件，保留 `task.md`、`execution-log.md` 和 `verification-report.md`。
- `project-experience-consolidation`：检索命中既有 `docs/frontend-development.md#前端截图字号调整静态契约门禁`，本任务无新增通用经验，无需更新长期经验文档。
