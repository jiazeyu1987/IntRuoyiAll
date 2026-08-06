# QA 路线 checkFlag 加载失败修复

## Task Goal

修复 QA 规程配置页在选择 `ID / 球囊扩张压力泵 / 112` 后，因当前绑定工艺路线未标记唯一 `checkFlag` 而显示“工艺路线范围加载失败”的问题。页面应在正式路线已绑定、路线版本可解析时继续加载适用范围，并用确定性的工序候选作为 QA 规程适用工序，不因缺少 `checkFlag` 阻断规程配置。

## Milestones

- [x] 创建任务记录并读取前端、编码、bug regression、frontend feature 和相关经验门禁。
- [x] 定位 `checkFlag` 报错链路和现有静态契约。
- [x] 增加 RED 静态回归，证明无 `checkFlag` 但存在正式批记录绑定工序时不能直接报错。
- [x] 实施最小正式修复，避免 fallback/吞异常。
- [x] 运行目标静态契约、相邻 QA 回归、结构检查和类型检查。

## Expected Verification

- `node tests/e2e/qa-regulation-route-checkflag-fallback-static.spec.cjs`
- `node tests/e2e/role-matrix-qa-regulation-tab-static.spec.cjs`
- `node tests/e2e/qa-regulation-id-balloon-pressure-pump-pdf-items-static.spec.cjs`
- `node tests/e2e/qa-regulation-pressure-pump-screenshot-pages-static.spec.cjs`
- `node tests/e2e/qa-regulation-pressure-pump-complete-pdf-items-static.spec.cjs`
- `node tests/e2e/qa-regulation-pressure-pump-pdf-field-alignment-static.spec.cjs`
- `pnpm ts:check`
- `git diff --check -- IntRuoyiFronted/src/views/mes/pro/processpool/QaRegulationPage.vue IntRuoyiFronted/tests/e2e/qa-regulation-route-checkflag-fallback-static.spec.cjs IntRuoyiFronted/tests/e2e/role-matrix-qa-regulation-tab-static.spec.cjs doc/tasks/20260806-qa-route-checkflag-load-error`

## Experience Gate Summary

- `docs/backend-development.md#qa-规程配置状态必须来自产品级规程记录`：QA 规程状态和草稿必须以正式产品 ID 为事实源。
- `docs/backend-development.md#qa-规程手动绑定必须允许已发布路线`：QA 页面手动绑定只维护产品路线关系；路线版本、质检工序、SOP、生产系数和批记录绑定仍从已发布路线自动解析。
- `docs/frontend-development.md#前端静态契约隔离门禁`：本任务新增专用静态契约，避免宽泛契约或相邻模板影响当前行为判断。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。若缺 `checkFlag`，只能按正式路线工序列表或唯一启用 BATCH `batchRecordReports` 正式批记录绑定确定可解析工序；多工序冲突仍 fail-fast。
- `是否从根因和长期维护角度解决`：是，修复 QA 路线范围解析规则与已发布路线绑定的正式链路一致。
- `是否存在临时补丁或绕过`：否。

## Current Status

ready_for_closeout

- 已完成实现和目标验证；收尾提交/推送未执行，因为共享 `int_main` 工作区存在大量无关脏改动，不能在未确认基线范围时将其它任务改动一并提交。
