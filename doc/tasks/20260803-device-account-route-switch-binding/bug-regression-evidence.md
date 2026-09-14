# Bug Regression Evidence

## Bug Summary

- Symptom: `设备账号 1 未绑定启用工艺路线，无法切换工序`。
- Expected behavior: 设备账号切换工序时必须按正式链路解析 `登录用户岗位 -> 工作站人员绑定 -> 启用工作站 -> 工艺路线工序工作站 -> 启用工艺路线`；如果正式配置缺失，必须暴露具体缺失前置条件，不能返回默认路线、空绑定成功、mock 成功或吞异常。

## Reproduction

- Runtime data command: `docker exec int-ruoyi-mysql sh -lc 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -N -D "ruoyi-vue-pro" -e "...read-only selects..."'`
- Runtime evidence:
  - `system_users.id=1`: `status=0`, `deleted=0`。
  - `system_user_post.user_id=1`: `post_id=1`、`post_id=2` 已删除；当前未删除岗位只有 `post_id=14`。
  - `mes_md_workstation_worker.post_id=14 AND deleted=0`: count `0`。
- Deterministic regression test: `MesFrontlineWorkstationPostRouteBindingSourceTest#shouldFailFastWhenFormalPostHasNoWorkstationBinding`。

## Root Cause

- 账号 1 的正式用户岗位链路已正确返回当前岗位 `14`，但岗位 `14` 没有工作站人员绑定。
- 原行为在正式岗位存在但工作站绑定为空时返回空列表，上层只能表现为泛化的“未绑定启用工艺路线”，掩盖了真正缺失的正式配置前置条件。
- 该问题不能用默认工艺路线、`formBindings`、工序开始配置、旧岗位、空绑定成功或前端文案兜底修复。

## Regression Test

- Added/verified test: `IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/frontline/MesFrontlineWorkstationPostRouteBindingSourceTest.java`
- Assertion: 当登录用户有启用正式岗位但 `mes_md_workstation_worker` 无对应工作站绑定时，`listEnabledRouteBindings` 抛出 `ServiceException`，错误信息包含 `post workstation binding`。

## RED

- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlineWorkstationPostRouteBindingSourceTest#shouldFailFastWhenFormalPostHasNoWorkstationBinding" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- Result: FAIL as expected before production fix.
- Expected reason: `Expected ServiceException to be thrown, but nothing was thrown.`

## GREEN

- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlineWorkstationPostRouteBindingSourceTest#shouldFailFastWhenFormalPostHasNoWorkstationBinding" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- Result: PASS, tests run `1`, failures `0`, errors `0`, skipped `0`.

## Verification

- Command: `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlineWorkstationPostRouteBindingSourceTest,MesFrontlineDeviceAccountContextServiceTest,MesFrontlineEmployeeSwitchServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- Result: PASS, tests run `9`, failures `0`, errors `0`, skipped `0`.

## Blockers And Follow-Up

- Risk: 运行态仍会阻止账号 1 切换工序，直到正式配置补齐；这是预期 fail-fast 行为，不是代码兜底缺失。
- Required configuration follow-up: 将 `post_id=14` 绑定到有效工作站，且该工作站必须出现在启用工艺路线的工序工作站配置中；或调整用户 1 的正式岗位到已绑定有效工作站的岗位。
- Closeout blocker: 当前工作区存在无关 DCC 前端改动和 ahead 提交，本任务不能提交/推送无关变更。
