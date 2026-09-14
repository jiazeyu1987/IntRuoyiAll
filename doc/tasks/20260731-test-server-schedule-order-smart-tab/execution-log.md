# 执行日志：测试服务器排产工单归入智能排产页签

## Intent

用户要求把测试服务器的 `排产工单` 放在 `智能排产` 页签下。

## Rule Reads

- Read: `docs/server-access.md`
- Read: `docs/database-rules.md`
- Read: `docs/powershell-encoding.md`
- Read: `docs/task-closeout-rules.md`
- Read: `docs/release-backup-restore.md`
- Read: `C:\Users\BJB110\.codex\skills\database-schema-delivery\SKILL.md`
- Read: `C:\Users\BJB110\.codex\skills\database-schema-delivery\references\database-contract.md`
- Read: `docs/experience-index.md`

## BDD

- BDD: 测试服排产工单归入智能排产 -> Given 测试服务器动态菜单存在 `智能排产` 与 `排产工单` / When 执行菜单归属修复 / Then `排产工单` 必须成为 `智能排产` 子菜单，排产员通过父链可见。

## Evidence

- RED: test-server-menu-readback -> FAIL, expected reason: 测试服务器 `system_menu.id=5580` 当前为 `name=排产工单池`、`parent_id=5700`、`permission=''`、`path=schedule-order`，未作为 `900120/智能排产` 子菜单暴露正式排产工单入口。
- Current test server health: `curl http://127.0.0.1:48081/actuator/health` -> `{"status":"UP"}`。
- GREEN: test-server-menu-fix-sql -> PASS, `system_menu.id=5580` 已更新为 `name=排产工单`、`HEX(name)=E68E92E4BAA7E5B7A5E58D95`、`parent_id=900120`、`permission=mes:pro-schedule-order:query`、`path=/mes/pro/schedule-order`、`component=mes/pro/scheduleorder/index`、`component_name=MesProScheduleOrder`。
- GREEN: test-server-role-package-readback -> PASS, `tenant_id=1 role_id=910216` 与 `tenant_id=122 role_id=910235` 的 `mes_scheduler` 均有效绑定 `5100/900120/5580/5590`；启用测试租户套餐 `111/114` 均包含 `900120/5580`。
- GREEN: test-server-health-after-fix -> PASS, `curl http://127.0.0.1:48081/actuator/health` -> `{"status":"UP"}`。
- GREEN: database-schema-evidence-validator -> PASS, `validate_database_schema.py --evidence doc\tasks\20260731-test-server-schedule-order-smart-tab\database-schema-evidence.md`。
- CLEANUP PREVIEW: `task_closeout.py --task-id 20260731-test-server-schedule-order-smart-tab --mode preview` -> ready；keep `task.md`、`execution-log.md`、`verification-report.md`；delete temporary `database-schema-evidence.md`；blocked `<none>`。
- CLEANUP APPLY: `task_closeout.py --task-id 20260731-test-server-schedule-order-smart-tab --mode apply` -> applied；deleted temporary `database-schema-evidence.md`；blocked `<none>`。
- Project experience consolidation: existing `docs/database-rules.md` already contains dynamic menu delivery checks and Chinese menu-name verification gates; no new long-term experience document was created.
