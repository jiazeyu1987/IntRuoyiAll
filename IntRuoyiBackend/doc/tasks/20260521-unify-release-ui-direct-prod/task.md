# 任务：统一发布 UI 并增加直发正式入口

## Goal

- 在一个统一运维 UI 中同时提供以下三种发布入口：
  - 发布当前本地代码到测试服务器 `172.30.30.58`
  - 将当前测试服务器版本提升到正式服务器 `172.30.30.57`
  - 将当前本地代码直接发布到正式服务器 `172.30.30.57`
- 保持现有测试发布与测试提升正式逻辑可用，不引入 fallback、mock 或静默降级。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\运维工具.bat`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\deploy\publish-int-ruoyi-to-test.bat`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\deploy\publish-int-ruoyi-to-prod.bat`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\deploy\publish-int-ruoyi-direct-to-prod.bat`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\deploy\publish-int-ruoyi-to-test.ps1`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_publish_int_ruoyi_to_test_tooling.py`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-unify-release-ui-direct-prod\**`

## Non-Scope

- 不执行真实测试发布、真实正式发布或真实测试提升正式。
- 不修改业务前后端功能代码。
- 不改动远端服务器配置、账号或密钥。

## Previous Task Check

- Previous same-repo task record:
  `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-test-server-full-publish-overwrite-data\task.md`
- Status before this task: `Blocked on 2026-05-21`
- Recorded blocker: 用户切换到更高优先级问题，导致测试服务器全量覆盖发布未执行。
- Impact on this task: 上一任务未实际执行发布，仅停留在预检阶段；本任务只调整本地运维入口与包装脚本，可以在不影响上一任务状态的前提下独立推进。

## Milestones

- [x] M1：检查上一同仓任务状态并创建本任务文档。
- [x] M2：记录 BDD 场景并补充 RED 测试。
- [x] M3：实现统一 UI 的三种发布入口与正式安全确认。
- [x] M4：运行脚本验证、更新帮助文案与任务记录。
- [x] M5：执行 cleanup 预览并评估可交付状态。

## Expected Verification

- `python -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_publish_int_ruoyi_to_test_tooling.py -q`
- `cmd /c D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\deploy\publish-int-ruoyi-direct-to-prod.bat cancel`
- `cmd /c D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\deploy\publish-int-ruoyi-to-prod.bat cancel`
- `cmd /c D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\运维工具.bat help`
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260521-unify-release-ui-direct-prod --mode preview`

## Current Status

Completed on 2026-05-21.

## Current Findings

- 当前统一入口 `运维工具.bat` 已支持“发测试”和“测试提升正式”，但发布菜单只有两个发布选项。
- 现有 `publish-int-ruoyi-to-test.ps1` 已参数化 `ServerHost`、`RemoteAppDir`、端口与同步选项，具备作为“直发正式”底层逻辑复用的条件。
- 当前缺少面向操作员的“本地当前代码直发正式”独立包装器，因此统一 UI 里也没有第三个发布入口。
- 已新增 `script\deploy\publish-int-ruoyi-direct-to-prod.bat`，将当前本地发布逻辑固定指向正式服务器 `172.30.30.57`，并要求显式输入 `PROD`。
- 已将统一入口 `运维工具.bat` 的发布菜单扩展为三项：发测试、测试提升正式、本地直发正式。
- 已同步更新 `OPS.md` 与 `README.md`，使操作说明与当前统一入口保持一致。

## Final Verification Result

- PASS：`python -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_publish_int_ruoyi_to_test_tooling.py -q`
- PASS：`cmd /c D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\deploy\publish-int-ruoyi-direct-to-prod.bat cancel`
- PASS：`cmd /c D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\deploy\publish-int-ruoyi-to-prod.bat cancel`
- PASS：`cmd /c D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\运维工具.bat help`
- PASS：`python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260521-unify-release-ui-direct-prod --mode preview`

## Cleanup Keep

- `doc/tasks/20260521-unify-release-ui-direct-prod/task.md`
- `doc/tasks/20260521-unify-release-ui-direct-prod/execution-log.md`
