# Execution Log

## Intent

用户要求“进行修复”，范围来自上一轮静态分析：AC-M12 至 AC-M15 及相关 QA/PQC/放行链路不符合项，包括 QA 规程保存发布未接入、PQC 任务正式生成器缺失、计划/实际数量不一致、多件样本被截断、上午/下午巡检身份隔离不足、末检不适用依据缺失和放行完整性只检查已存在任务。

## Preflight

- 已读取 `bug-regression-fix-loop`、`backend-api-delivery`、`frontend-feature-delivery` 技能及其 evidence contract。
- 已读取 `docs/task-closeout-rules.md`、`docs/backend-development.md`、`docs/frontend-development.md`、`docs/database-rules.md`、`docs/powershell-encoding.md`、`docs/powershell-memory.md`。
- 已读取 `docs/experience-index.md`，命中 PQC/QA 规程相关门禁并摘入 `task.md`。
- 当前 Git 状态：`int_main...origin/int_main [ahead 4]`，存在既有脏改动；本任务实现前需按规则独立基线提交既有脏改动，避免混入当前修复。

## BDD Scenarios

- BDD: PQC task planned quantity is authoritative -> Given a pending PQC task generated from a published QA regulation with planned quantity N / When the inspector submits actual quantity not equal to N or provides more/fewer sample values / Then the backend rejects the submission and does not write piece details or process-pool events.
- BDD: Patrol AM and PM task identities are isolated -> Given a published regulation requiring morning and afternoon patrol tasks for the same active order and process / When a task is generated or submitted / Then business date, shift code, round number and patrol period identity must match, and reusing the morning task for afternoon fails.
- BDD: Final inspection applicability is explicit -> Given a published regulation marks final inspection applicable or not applicable / When release completeness is evaluated / Then applicable final inspection requires a confirmed task, while not applicable must have persisted explicit evidence and must not be treated as a missing task.
- BDD: QA regulation persistence is formal -> Given QA updates inspection rules for a product/route/process / When saving draft or publishing / Then backend persists a draft/published immutable version or fail-fast reports the missing formal API, and the UI must not pretend a preview-only draft is a saved rule.

## RED / GREEN Evidence

- RED: pending.
- GREEN: pending.

## Milestone Updates

- M1 in progress：准备基线隔离和 RED 测试。

## Blockers

- 当前工作区有前序任务脏改动，需要先独立基线提交后再修改生产代码。
