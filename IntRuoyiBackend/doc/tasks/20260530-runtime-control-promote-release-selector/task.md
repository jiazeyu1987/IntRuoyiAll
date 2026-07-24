# 任务：运行控制台上线发布包选择器（后端校验）

## 任务目标

为运行控制台“上线已验证发布包”发布包选择器补齐跨仓静态校验，确保前端页面继续满足：

- `promote-prod` 使用 NAS 发布包下拉选择器；
- 默认原因固定为“默认发布”；
- 列表能够标识“当前测试服”和“曾部署测试服”。

本任务仅改后端仓库中的跨仓 Python 校验测试和任务文档，不修改后端生产逻辑。

## 前置任务检查

- `20260529-runtime-control-all-buttons-int-main-merge` 状态为 `completed`。
- `20260529-runtime-load-latest-code` 状态为 `completed`。

## BDD 场景

- BDD: 跨仓校验锁定 promote-prod 选择器合同 -> Given admin 前端页面继续承载运行控制台发布包交互 / When 后端仓库运行跨仓 tooling 校验 / Then 校验必须要求 `promote-prod` 使用与 `publish-test` 相同的发布包选择器与默认原因。
- BDD: 跨仓校验锁定测试服使用状态标识 -> Given admin 前端页面展示发布包列表 / When 后端仓库运行跨仓 tooling 校验 / Then 校验必须要求页面存在“当前测试服”和“曾部署测试服”的状态标识逻辑。

## 里程碑

- [x] M1：建立任务记录并确认前置任务完成。
- [x] M2：补充跨仓 Python 校验测试并记录 RED。
- [x] M3：运行目标测试并记录 GREEN。
- [x] M4：更新执行记录并完成提交。

## 预期验证

- `python -m pytest script/tests/test_runtime_control_release_selector_tooling.py -q`

## Current Status

Completed.

## 最终验证

- `python -X utf8 -m pytest script\tests\test_runtime_control_release_selector_tooling.py -q` -> PASS，5 passed。
- `python -X utf8 -m pytest script\tests\test_runtime_control_scripts.py script\tests\test_runtime_control_release_selector_tooling.py -q` -> PASS，12 passed。
