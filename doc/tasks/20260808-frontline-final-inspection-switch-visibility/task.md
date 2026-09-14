# 20260808-frontline-final-inspection-switch-visibility

## Task Goal

一线 PQC 填写页的“末检”选择只在对应产品发布态 QA 规程中 `finalInspectionApplicable=true` 时显示；关闭时隐藏且不可选择。

## Milestones

- [x] 建立 BDD/TDD 任务记录并定位一线 PQC 与 QA 规程快照契约。
- [x] 补充 RED 静态合同，证明末检按钮必须受发布态 QA 末检适用性控制。
- [x] 实现最小前后端链路：从正式 QA 发布版本字段读取末检适用性，隐藏并阻止不可用末检。
- [x] 运行目标合同、相邻合同、类型/格式验证，并记录结果。
- [x] 收尾前更新验证报告和任务状态。

## Expected Verification

- `node src/views/mes/pro/feedback/frontline-template-render.spec.cjs`
- 目标新增静态合同命令（RED/GREEN）
- 受影响相邻 PQC 静态合同
- `pnpm ts:check` 或记录明确阻塞
- `git diff --check`

## Applicable Gates

- PQC 末检适用性必须来自发布规程 `finalInspectionApplicable` 与 `finalInspectionNotApplicableReason`，不得用前端默认 false、空规则或任务缺失解释末检不适用。
- PQC 填写页检验项目事实必须来自发布态 QA 规程快照与结构化 `inspectionItems`，不得用固定项目或前端文案替代正式快照。
- 前端静态合同必须先 RED 后 GREEN；若全量类型检查被无关历史问题阻塞，需记录阻塞并保留目标合同通过证据。

## Current Status

completed

## Verification Summary

- `node tests/e2e/frontline-pqc-final-inspection-switch-static.spec.cjs`：PASS。
- `pnpm ts:check`：PASS。
- `mvn -pl yudao-module-mes -am "-DskipTests" compile`：PASS。
- `git diff --check`：PASS，仅存在既有 LF/CRLF 提示。
- `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlinePqcContextServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：BLOCKED，MES 测试源整体 `testCompile` 被既有缺失类阻塞，生产源码编译已单独通过。
- `node src/views/mes/pro/feedback/frontline-template-render.spec.cjs`：BLOCKED，既有 `production UI must have a no-device full-width layout` 断言失败，未进入末检断言。

## Closeout Evidence

- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260808-frontline-final-inspection-switch-visibility --mode preview`：READY，无删除项、无阻塞、无警告。
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260808-frontline-final-inspection-switch-visibility --mode apply`：APPLIED，无删除项、无阻塞、无警告。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，计划读取正式 QA 发布快照字段并在 UI 和选择函数双层约束。
- `是否存在临时补丁或绕过`：否。
