# 任务：修复运行控制台最近操作状态目录漂移

## 任务目标

修复本机运行控制台“最近操作”接口返回空列表的问题。本机后端重启后必须继续读取稳定的运行控制状态目录 `ruoyi-vue-pro/runtime/runtime-control`，不得把状态目录切换到发布产物目录 `output/runtime/int_main/runtime-control` 导致历史操作记录不可见。

## Previous Task Check

- 上一个后端任务：`doc/tasks/20260603-dcc-category-duplicate-code/task.md`
- 状态：`completed`
- 当前后端仓库已有 unrelated runtime state dirty changes；本任务只触碰本机运行控制状态目录脚本合同、对应测试和本任务文档。

## BDD 场景

- BDD: 本机运行控制状态目录重启后保持稳定 -> Given 运行控制台历史操作记录保存在 `ruoyi-vue-pro/runtime/runtime-control` / When 本机后端通过 `restart-int-ruoyi-local.ps1` 重启 / Then 后端必须继续使用该稳定目录读取 `/infra/runtime-control/operations`，不得切换到 `output/runtime/<worktree>/runtime-control`。
- BDD: 运行产物目录仅保存 jar 和进程日志 -> Given 本机重启脚本需要保存 jar、stdout、stderr 等运行产物 / When 脚本生成本次启动产物 / Then 这些产物仍保存在 `output/runtime/<worktree>`，但运行控制操作状态不随产物目录漂移。

## Milestones

- [x] M1：建立任务文档并确认上一后端任务已完成。
- [x] M2：先补 RED 回归测试，复现本机状态目录漂移。
- [x] M3：实现最小脚本修复，不引入 fallback、降级或吞异常。
- [x] M4：运行目标测试与相关回归，记录 GREEN。
- [x] M5：重启本机后端并用真实页面验证最近操作恢复显示。
- [x] M6：运行任务收尾清理预览并完成文档。

## Expected Verification

- RED：`python -m pytest script/tests/test_runtime_control_scripts.py -q` 先失败，指出脚本将 `state-dir` 指向 `output/runtime/<worktree>/runtime-control`。
- GREEN：同一目标测试通过。
- GREEN：运行控制台后端服务单测通过。
- GREEN：真实页面刷新后 `/operations` 返回历史记录且“最近操作”表格显示行。
- GREEN：bug regression evidence validator 通过。
- GREEN：task-closeout-cleanup 预览通过。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；修正状态目录所有权，使运行控制操作记录使用稳定仓库状态目录，运行产物继续使用 output 目录。
- `是否存在临时补丁或绕过`：否。

## 当前状态

completed

## 已完成工作

- 已确认上一后端任务 `20260603-dcc-category-duplicate-code` 状态为 `completed`。
- 已用真实前端路径确认 `/admin-api/infra/runtime-control/operations` 当前返回 `data: []`。
- 已确认当前后端进程参数为 `--yudao.runtime-control.state-dir=D:\ProjectPackage\Int\IntRuoyi\output\runtime\int_main\runtime-control`，而历史操作记录位于 `ruoyi-vue-pro/runtime/runtime-control`。
- 已修改 `script/deploy/restart-int-ruoyi-local.ps1`：本机运行产物仍使用 `output/runtime/<worktree>`，但运行控制状态目录固定为 `$RepoRoot/runtime/runtime-control`。
- 已重启本机后端，当前 Java 进程参数已变为 `--yudao.runtime-control.state-dir=D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\runtime\runtime-control`。

## 验证结果

- RED：`python -m pytest script/tests/test_runtime_control_scripts.py -q` -> FAIL，脚本仍将 `state-dir` 指向 `$RuntimeDir/runtime-control`。
- GREEN：`python -m pytest script/tests/test_runtime_control_scripts.py -q` -> PASS，12 passed。
- GREEN：`python -m pytest script/tests/test_restart_int_ruoyi_local_schema.py -q` -> PASS，2 passed。
- GREEN：`mvn -pl yudao-module-infra "-Dtest=RuntimeControlServiceImplTest" test` -> PASS，35 tests。
- GREEN：`powershell -NoProfile -ExecutionPolicy Bypass -File script\deploy\restart-int-ruoyi-local.ps1 -Component backend` -> PASS。
- GREEN：当前 Java 进程参数包含 `--yudao.runtime-control.state-dir=D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\runtime\runtime-control`。
- GREEN：Playwright 本机真实页面刷新 -> PASS，`/operations` 返回 34 条，最近操作表格显示 34 行。
- GREEN：bug regression evidence validator -> PASS。
- CLOSEOUT PREVIEW：task-closeout-cleanup 预览通过。

## 剩余阻塞

- 无。

## Cleanup Keep

- `doc/tasks/20260603-runtime-control-recent-operations-visible/bug-regression-evidence.md`
