# Verification Report

## Summary

本机目标库已执行 `20260803_mes_frontline_pressure_pump_all_process_permission.sql`，并已将 `压力泵全工序切换` 菜单权限 `900450` 分配给当前登录账号 userId=1 在 tenantId=1 下的 27 个启用所属角色。

## Evidence

- DB migration apply: PASS.
- Migration policy gate: PASS, migration id `20260803_mes_frontline_pressure_pump_all_process_permission`。
- Role assignment: PASS, `target_role_count=27` and `active_role_menu_count=27`。
- Cache clear: PASS, cleared exact local Redis permission keys.
- DB verification: PASS, `system_menu.id=900450` exists with permission `mes:pro-feedback:frontline-pressure-pump:all-processes`。
- Login API verification: PASS, `permissionHit=true` for tenantId=1 / userId=1 / username `admin`。
- Final readonly DB check: PASS, `menu_ok=1`, `target_role_count=27`, `active_role_menu_count=27`。

## Result

本机账号 1 已拥有压力泵全工序切换权限；再次进入一线生产填写切换压力泵工序时，应命中标准权限链路，不再落到岗位 14 的工作站绑定报错。
