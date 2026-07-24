# 任务：提交运行控制台状态

## 任务目标

按用户确认的当前 `int_main` 分支内容，提交后端仓库当前运行控制台状态文件改动。提交范围仅限 `runtime/runtime-control` 状态文件和本任务记录。

## BDD 场景

- BDD: 后端当前状态可被独立提交 -> Given 用户确认当前分支内容正确 / When 提交后端 `int_main` 当前改动 / Then 提交必须只包含后端当前运行控制台状态文件和本任务记录。
- BDD: 提交必须满足 TDD gate -> Given 仓库提交钩子要求 `TDD_TASK_DIR` / When 未设置该变量提交 / Then 提交必须失败并提示缺少任务目录；设置后才能继续。

## Milestones

- [x] M1：确认后端处于 `int_main` 且存在待提交 runtime-control 改动。
- [x] M2：记录提交钩子 RED 证据。
- [x] M3：补充任务记录并准备设置 `TDD_TASK_DIR` 重新提交。
- [x] M4：完成提交并复核后端工作区状态。

## Expected Verification

- RED：未设置 `TDD_TASK_DIR` 的 `git commit` 必须失败。
- GREEN：设置 `TDD_TASK_DIR` 后 `git commit` 成功。
- 提交后 `git status --short` 无未提交内容。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。提交钩子缺少前置条件时直接失败。
- `是否从根因和长期维护角度解决`：是。按仓库要求补齐任务目录并设置明确提交前置条件。
- `是否存在临时补丁或绕过`：否。不绕过提交钩子。

## 当前状态

completed

## 验证结果

- RED：未设置 `TDD_TASK_DIR` 的 `git commit` -> FAIL，提交钩子明确拒绝。
- GREEN：设置 `TDD_TASK_DIR` 后 `git commit` -> PASS，提交钩子返回 `TDD compliance passed`。
- 提交范围：`runtime/runtime-control` 状态文件与本任务记录。

## Blockers

- none.
