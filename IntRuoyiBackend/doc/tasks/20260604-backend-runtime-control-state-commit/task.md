# 任务：提交后端运行控制台状态

## 任务目标

按用户要求提交后端仓库当前改动。提交范围限定为 `runtime/runtime-control` 运行控制台状态文件与本任务记录，不包含前端或其他无关文件。

## BDD 场景

- BDD: 后端运行状态可被独立提交 -> Given 后端工作区当前改动集中在 `runtime/runtime-control` / When 执行后端提交 / Then 提交必须只包含运行控制台状态文件与本任务记录。
- BDD: 提交门禁必须显式绑定任务目录 -> Given 后端仓库 pre-commit 要求 `TDD_TASK_DIR` / When 未设置该变量提交 / Then 提交必须失败；设置为本任务目录后提交才能通过。

## 里程碑

- [x] M1：确认最近任务已收尾或已明确阻塞，当前待提交范围只在后端运行控制台状态目录。
- [x] M2：完成任务记录并暂存限定范围文件。
- [x] M3：记录提交门禁 RED/GREEN 证据。
- [x] M4：完成后端提交并复核工作区状态。

## 预期验证

- VERIFY：`git status --short --branch` 确认提交前后范围。
- VERIFY：运行状态 JSON 文件必须可被 JSON 解析。
- RED：未设置 `TDD_TASK_DIR` 的 `git commit` 必须失败。
- GREEN：设置 `TDD_TASK_DIR` 后 `git commit` 必须成功。
- CLEANUP：`task_closeout.py --mode preview` 必须无删除项、无阻塞项。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是；本任务仅做用户要求的后端当前状态归档，不引入临时逻辑或绕过路径。
- 是否存在临时补丁或绕过：否。

## 当前状态

completed

## 验证结果

- VERIFY：`node` JSON 解析检查 -> PASS，15 个运行状态 JSON 可解析。
- RED：未设置 `TDD_TASK_DIR` 的 `git commit -m "任务: 提交后端运行控制台状态"` -> FAIL，提交钩子提示必须设置任务目录。
- GREEN：`python .\tool\verify_tdd_compliance.py --task-dir doc/tasks/20260604-backend-runtime-control-state-commit --all-changed` -> PASS，`TDD compliance passed`。
- CLEANUP：`task_closeout.py --mode preview` -> PASS，`ready`，无删除项、无阻塞项。
