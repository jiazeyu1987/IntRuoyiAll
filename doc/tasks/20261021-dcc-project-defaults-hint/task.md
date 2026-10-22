# DCC 项目默认属性缺失提示
## Current Status
blocked — implementation and targeted verification PASS; no current commit/push authorization; runtime not updated
## Goal
缺省配置为空时，提示准确列出需维护的默认属性和真实页面入口。
## Milestones
BDD -> RED -> 提示修改 -> GREEN -> 清理。
## Expected Verification
Given 项目默认属性未配置 When 选择DCC项目 Then 提示目标市场、是否为注册人、是否为生产方、文件转移，并指向基础数据>DCC项目代码>修改项目默认属性与负责人账号。保持原校验和错误码；不自动填值。
## 设计约束检查
int_qms；仅修改错误文案及定向测试；不写数据库、不启用E2E、不重启服务、不提交推送。JSON整组缺失，不能声称其中某项已经存在。负责人是维护入口内容，并非此报错的缺失判定。
## Cleanup Keep
- doc/tasks/20261021-dcc-project-defaults-hint/task.md
- doc/tasks/20261021-dcc-project-defaults-hint/execution-log.md
- doc/tasks/20261021-dcc-project-defaults-hint/verification-report.md


