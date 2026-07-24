# 执行日志：DCC 文控中心子页签改为四字名称

- 2026-06-29：创建后端任务文档，按菜单种子契约文案统一任务执行严格 TDD。
- BDD: 新环境初始化后 DCC 文控中心子页签名称符合四字契约 -> Given 系统执行 DCC 菜单基础种子与补丁 SQL / When 菜单写入 `system_menu` / Then 目标页签名称应为无 `DCC` 前缀且互不重名的 4 字名称。
- RED: `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_dcc_subtab_four_char_menu_sql.py -q` -> FAIL，菜单 SQL 仍包含旧 `DCC` 前缀名称。
- GREEN: `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_dcc_subtab_four_char_menu_sql.py -q` -> PASS，菜单基础种子、补丁 SQL 与运行库迁移脚本已统一新名称。
- GREEN: `docker exec int-ruoyi-mysql mysql --default-character-set=utf8mb4 -uroot -p123456 -N -e "SELECT path, name FROM system_menu WHERE parent_id = 6800 AND deleted = 0 ORDER BY sort;" ruoyi-vue-pro` -> PASS，本机运行库目标子页签已回查为新名称。
