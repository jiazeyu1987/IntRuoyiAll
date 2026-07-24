# 任务：修复 NAS 发布包挂载盘符作用域

## 任务目标

运行控制台执行发布包部署、标记测试通过或上线正式服时，NAS 发布包脚本必须在挂载后继续访问同一个 PowerShell 盘符，不能因 `New-PSDrive` 作用域随函数返回而失效。

## BDD 场景

- BDD: NAS 发布包盘符在发布动作期间保持可访问 -> Given 运行控制台调用统一发布脚本并挂载 NAS 发布包共享 / When 脚本返回挂载信息后继续解析发布包路径 / Then PowerShell 盘符仍在脚本作用域可访问，缺包时应报发布包不存在，而不是 `DriveNotFound`。

## 里程碑

- [x] M1：确认失败日志和根因。
- [x] M2：补充 RED 回归测试，覆盖 `New-PSDrive` 作用域。
- [x] M3：最小修改发布脚本，保持挂载盘符到 `finally` 卸载。
- [x] M4：运行发布脚本相关回归和 PowerShell 语法校验。
- [x] M5：记录证据、运行 task-closeout-cleanup 预览并提交本任务改动。

## 预期验证

- RED: `python -X utf8 -m pytest script\tests\test_publish_int_ruoyi_to_test_tooling.py -q` 在新增 `New-PSDrive -Scope Script` 合同处失败。
- GREEN: `python -X utf8 -m pytest script\tests\test_publish_int_ruoyi_to_test_tooling.py -q` 通过。
- GREEN: PowerShell 解析 `script\deploy\publish-int-ruoyi.ps1` 无语法错误。

## 验证结果

- RED: `python -X utf8 -m pytest script\tests\test_publish_int_ruoyi_to_test_tooling.py -q` -> FAIL，新增合同发现脚本缺少 `New-PSDrive -Scope Script`。
- GREEN: `python -X utf8 -m pytest script\tests\test_publish_int_ruoyi_to_test_tooling.py -q` -> PASS，22 passed。
- GREEN: `python -X utf8 -m pytest script\tests\test_runtime_control_ops_scripts.py -q` -> PASS，5 passed。
- GREEN: PowerShell parser check for `script\deploy\publish-int-ruoyi.ps1` -> PASS。
- GREEN: local PSDrive scope check -> PASS，函数返回后 `Join-Path` 能访问脚本作用域盘符。

## 当前状态

completed

status: completed

## Current Status

completed
