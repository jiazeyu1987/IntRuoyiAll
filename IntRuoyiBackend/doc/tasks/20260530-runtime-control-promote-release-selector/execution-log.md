# Execution Log

BDD: 跨仓校验锁定 promote-prod 选择器合同 -> Given admin 前端页面继续承载运行控制台发布包交互 / When 后端仓库运行跨仓 tooling 校验 / Then 校验必须要求 `promote-prod` 使用与 `publish-test` 相同的发布包选择器与默认原因。

BDD: 跨仓校验锁定测试服使用状态标识 -> Given admin 前端页面展示发布包列表 / When 后端仓库运行跨仓 tooling 校验 / Then 校验必须要求页面存在“当前测试服”和“曾部署测试服”的状态标识逻辑。

RED: `python -X utf8 -m pytest script\tests\test_runtime_control_release_selector_tooling.py -q` -> FAIL，跨仓校验尚未要求 `promote-prod` 使用发布包下拉、默认原因和测试服使用状态标识。

GREEN: `python -X utf8 -m pytest script\tests\test_runtime_control_release_selector_tooling.py -q` -> PASS，5 passed。

GREEN: `python -X utf8 -m pytest script\tests\test_runtime_control_scripts.py script\tests\test_runtime_control_release_selector_tooling.py -q` -> PASS，12 passed。
