# Execution Log

## User Intent

- 用户反馈：`设备账号 1 未绑定启用工艺路线，无法切换工序`。
- 初始判断：问题命中设备账号切换工序时的正式启用工艺路线绑定解析链路，需要按 bug-regression-fix-loop 执行 RED/GREEN 修复。

## Rule And Experience Reads

- Read: `C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\SKILL.md`
- Read: `C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\references\bug-contract.md`
- Read: `docs\task-closeout-rules.md`

## BDD

- BDD: 设备账号使用正式启用工艺路线切换工序 -> Given 设备账号关联的业务对象存在正式启用工艺路线和当前工序上下文, When 设备账号执行切换工序, Then 后端应按正式启用路线解析可切换工序并允许切换, And 不得错误返回“未绑定启用工艺路线”。
- BDD: 正式启用工艺路线确实缺失时 fail fast -> Given 设备账号没有任何正式启用工艺路线绑定, When 设备账号执行切换工序, Then 后端应返回明确缺失配置错误, And 不得用默认路线、空绑定、mock 成功或吞异常替代。

## Command Log

- Command intent: `git status --short --branch` -> observed workspace already dirty and `int_main` ahead of origin with unrelated task changes; current task will avoid staging or modifying unrelated files.
- Command intent: read-only local MySQL check for user 1 formal post and workstation binding -> PASS; `system_users.id=1` is enabled/not deleted, active `system_user_post` is `post_id=14`, and active `mes_md_workstation_worker` count for `post_id=14` is `0`.
- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlineWorkstationPostRouteBindingSourceTest#shouldFailFastWhenFormalPostHasNoWorkstationBinding" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected reason: `Expected ServiceException to be thrown, but nothing was thrown.`
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlineWorkstationPostRouteBindingSourceTest#shouldFailFastWhenFormalPostHasNoWorkstationBinding" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, tests run `1`, failures `0`, errors `0`, skipped `0`.
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlineWorkstationPostRouteBindingSourceTest,MesFrontlineDeviceAccountContextServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, tests run `4`, failures `0`, errors `0`, skipped `0`.
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlineWorkstationPostRouteBindingSourceTest,MesFrontlineDeviceAccountContextServiceTest,MesFrontlineEmployeeSwitchServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, tests run `9`, failures `0`, errors `0`, skipped `0`.

## Root Cause

- 设备账号 1 的用户接口已能从正式 `system_user_post` 读取当前岗位；当前本地数据中未删除岗位为 `post_id=14`。
- `post_id=14` 没有未删除的 `mes_md_workstation_worker` 工作站绑定，导致后续无法解析到启用工作站、工艺路线工序工作站和启用工艺路线。
- 修复点：`MesFrontlineWorkstationPostRouteBindingSource` 在正式岗位存在但工作站绑定为空时 fail fast，抛出包含 `post workstation binding loginUserId=..., postIds=...` 的正式缺失配置错误，不再返回空列表让上层误报泛化路线缺失。

## Current Verification

- `bug-regression-evidence.md` 已按 bug-regression-fix-loop 契约记录。
- `verification-report.md` 已记录定向验证、相邻回归和运行态数据根因。
- Evidence validation: `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc/tasks/20260803-device-account-route-switch-binding/bug-regression-evidence.md` -> PASS。
- Diff validation: `git diff --check -- doc/tasks/20260803-device-account-route-switch-binding/...` -> PASS。
- Experience consolidation: 已读取 `project-experience-consolidation` 技能并搜索既有经验；可复用经验应归入长期门禁，但 `docs/experience-index.md` 已有无关 DCC 页签缓存 dirty 改动，本任务不混写共享经验索引。
- 当前 closeout 阻塞：工作区存在无关 DCC 前端 dirty 文件且分支已 ahead，不能在本任务中提交/推送无关变更。
