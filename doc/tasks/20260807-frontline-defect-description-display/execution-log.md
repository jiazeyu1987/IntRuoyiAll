# 执行日志

## 用户意图

- 用户要求截图红框中的一线生产“不良明细”显示不良详情，而不是 `RLR...` 编码。

## BDD

- BDD: 一线生产不良明细显示正式详情 -> Given 当前工序运行态返回同一不良原因的 `reasonName` 与内部 `reasonCode`；When 员工打开一线生产填报页；Then 不良明细卡片显示 `reasonName`，且不显示或回退到 `reasonCode`。

## 命令意图与证据

- 已读取 `docs/frontend-development.md`、`docs/task-closeout-rules.md`、`docs/powershell-encoding.md`、`docs/experience-index.md` 与缺陷回归技能契约。
- 已检查 Git 状态；工作区存在其它任务改动，本任务不修改、不清理、不提交这些改动。
- 截图定位：`FrontlineFixedTemplatePanel.vue` 的 `frontline-production-defect-name` 渲染 `defect.label`。
- 正式字段来源：运行态 `FrontlineRuntimeDefectReasonVO` 与后端 `MesFrontlineDefectReasonOption` 均分别提供 `reasonName` 和 `reasonCode`。
- 初步根因：`configuredDefectReasons` 将可见 `label` 定义为 `reason.reasonName || reason.reasonCode || 不良原因编号`，允许内部编码和编号占位文案进入用户可见区域，违反本次展示契约。
- 回归测试：在 `frontline-template-render.spec.cjs` 中抽取 `configuredDefectReasons`，要求可见 `label` 直接取 `reasonName`，并禁止编码或编号占位 fallback。
- RED: `node src/views/mes/pro/feedback/frontline-template-render.spec.cjs` -> FAIL，预期原因：当前 `label` 仍包含 `reasonCode` 与编号占位 fallback。
- 实施：将 `configuredDefectReasons` 的可见 `label` 从 `reasonName || reasonCode || 编号占位` 收敛为正式 `reasonName`；`reasonId`、`reasonCode` 和结构化提交逻辑保持不变。
- 旧大合同非本任务失败：修复后的 `frontline-template-render.spec.cjs` 已通过本任务新增断言，随后失败在既存 `frontline-production-main ... is-no-device` 布局断言；按静态契约隔离门禁，将同一目标断言迁移到独立 `frontline-defect-description-static.spec.cjs`，未修改无关布局。
- GREEN: `node tests/e2e/frontline-defect-description-static.spec.cjs` -> PASS。
- REGRESSION: `node tests/e2e/frontline-production-submit-payload-detail-static.spec.cjs` -> PASS。
- REGRESSION: `node tests/e2e/frontline-team-config-static.spec.cjs` -> PASS。
- REGRESSION: `node tests/e2e/edhr-frontline-fill-tabs-static.spec.cjs` -> PASS。
- REGRESSION: `node --check tests/e2e/frontline-defect-description-static.spec.cjs` -> PASS。
- REGRESSION: `pnpm ts:check` -> PASS。
- REGRESSION: `git diff --check -- <task-owned paths>` -> PASS；仅有 Git 的 LF/CRLF 未来转换提示，无空白错误。
- 经验沉淀：已将“用户可见描述与内部编码隔离门禁”归并到 `docs/frontend-development.md`，并更新 `docs/experience-index.md` 路由；未新建长期经验文档。
- 缺陷证据校验：`validate_bug_regression.py --evidence doc/tasks/20260807-frontline-defect-description-display/bug-regression-evidence.md` -> PASS；RED/GREEN、根因、回归范围和阻塞结论已归档到保留文档。
- CLEANUP PREVIEW: `task_closeout.py --task-id 20260807-frontline-defect-description-display --mode preview` -> PASS；keep 为 `task.md`、`execution-log.md`、`verification-report.md`，delete 仅为临时 `bug-regression-evidence.md`，blocked/warnings 均为空。
- CLEANUP APPLY: `task_closeout.py --task-id 20260807-frontline-defect-description-display --mode apply` -> PASS；仅删除临时 `bug-regression-evidence.md`，核心记录和正式回归合同均保留。
- 最终复核命令修正：首次从仓库根目录运行 `node tests/e2e/frontline-defect-description-static.spec.cjs` 因缺少前端目录前缀而 `MODULE_NOT_FOUND`；改在 `E:\IntRuoyi\IntRuoyiFronted` 运行同一命令 -> PASS，属于命令工作目录错误，不是产品失败。
- 最终状态：`completed`；未执行 Git 提交或推送，符合项目 Git Policy 的默认行为。

## 里程碑状态

- M1：已完成；目标页面与正式字段来源已确认。
- M2：已完成；聚焦回归合同按预期 RED。
- M3：已完成；目标合同与三项相邻合同通过。
- M4：已完成；任务状态已设为 `ready_for_closeout`，等待 cleanup preview/apply。
- 收尾：已完成；cleanup preview/apply 通过，任务最终状态为 `completed`。

## 阻塞项

- 暂无。
