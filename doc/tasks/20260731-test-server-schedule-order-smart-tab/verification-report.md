# 验证报告：测试服务器排产工单归入智能排产页签

## Result

PASS。测试服务器 `172.30.30.58` 已将 `5580/排产工单` 恢复为 `900120/智能排产` 下的子菜单。

## Before

- `system_menu.id=5580`：`name=排产工单池`
- `parent_id=5700`
- `permission=''`
- `path=schedule-order`

## After

- `system_menu.id=5580`：`name=排产工单`
- `HEX(name)=E68E92E4BAA7E5B7A5E58D95`
- `parent_id=900120`
- `permission=mes:pro-schedule-order:query`
- `path=/mes/pro/schedule-order`
- `component=mes/pro/scheduleorder/index`
- `component_name=MesProScheduleOrder`

## Permission Verification

- `tenant_id=1 role_id=910216 mes_scheduler`：`5100/900120/5580/5590` 均为 ACTIVE。
- `tenant_id=122 role_id=910235 mes_scheduler`：`5100/900120/5580/5590` 均为 ACTIVE。
- 测试服启用套餐 `111/114` 均包含 `900120/5580`。

## Health Verification

- `curl http://127.0.0.1:48081/actuator/health` -> `{"status":"UP"}`。

## Evidence Validator

- `validate_database_schema.py --evidence doc\tasks\20260731-test-server-schedule-order-smart-tab\database-schema-evidence.md` -> PASS。
