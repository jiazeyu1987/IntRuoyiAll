# Execution Log: 修复 NAS 管理页签菜单乱码

BDD: NAS菜单中文名保持正确 -> Given 系统管理菜单 SQL 包含中文菜单名 `NAS管理` / When 本地数据库执行该 SQL 并后端返回菜单载荷 / Then 菜单名称应保持为 `NAS管理`，而不是乱码

RED: local permission-info payload -> FAIL, 当前 `GET /admin-api/system/auth/get-permission-info` 中 `component=system/nas/index` 对应的菜单名为乱码 `NASç®¡ç†`

RED: python -m pytest ruoyi-vue-pro\script\tests\test_system_nas_menu_sql.py -q -> FAIL, 菜单 SQL 尚未显式声明 `SET NAMES utf8mb4;`

GREEN: SQL charset guard -> PASS, `20260520_system_nas_management_menu.sql` 现已显式声明 `SET NAMES utf8mb4;`

GREEN: python -m pytest ruoyi-vue-pro\script\tests\test_system_nas_menu_sql.py -q -> PASS, 菜单 SQL 回归检查通过

GREEN: local DB + live menu payload verification -> PASS, `system_menu.id in (5900..5903)` 中文名称已修正，重新登录后 `get-permission-info` 返回 `NAS管理`
