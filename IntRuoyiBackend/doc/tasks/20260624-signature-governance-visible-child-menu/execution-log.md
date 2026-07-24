# 执行日志：电子签名左侧菜单显示子页签

- `BDD: 电子签名显示可见子菜单 -> Given 用户拥有电子签名菜单权限 / When 登录后台并展开左侧电子签名 / Then 电子签名下显示 总览、文件签名记录、批记录签名记录、用户授权、长期留存、周期复核、CSV质量包、统一策略。`
- `BDD: 旧签名入口不恢复为独立菜单 -> Given 已执行统一电子签名菜单迁移 / When 查询旧 DCC 与 eDHR 菜单 / Then 6815 和 900026 不作为独立可见菜单，只作为统一菜单下的权限项保留原权限码。`
- `BDD: 子菜单角色授权随统一菜单迁移 -> Given 角色已拥有统一电子签名菜单 / When 迁移执行完成 / Then 角色同时拥有 8 个电子签名子菜单，登录后可在左侧看到子页签。`

## 记录

- `INFO: scope -> 只修复电子签名动态菜单子项可见性，不恢复旧 DCC/eDHR 独立入口，不修改业务 API。`
- `RED: python -m pytest script\tests\test_unified_electronic_signature_menu_sql.py -> FAIL, 当前 SQL 缺少 @unified_signature_child_menu_id、8 个可见子菜单、旧权限项挂到子菜单、角色子菜单绑定迁移。`
- `GREEN: python -m pytest script\tests\test_unified_electronic_signature_menu_sql.py -> PASS, 5 passed，菜单 SQL 契约已要求 8 个可见子菜单和角色绑定迁移。`
- `INFO: mysql-precheck -> PASS, 本机 int-ruoyi-mysql 运行中；真实库 900410-900417 未占用；900218 下当前只有 type=3 权限项，测试租户角色 111 仅有 6815、900026、900218。`
- `GREEN: experience-preflight -> PASS, 已阅读 docs/login-access.md、docs/server-access.md、docs/worktree-memory.md；本次只写本机 MySQL 菜单数据并用本机测试租户真实登录验证，不访问远端环境，不处理非本任务脏改动。`
- `GREEN: local-menu-migration-apply -> PASS, 使用 UTF-8 字节方式将 sql/mysql/20260624_unified_electronic_signature_menu.sql 应用到本机 int-ruoyi-mysql。`
- `RED: mysql-menu-verification -> FAIL, 权限项迁移 UPDATE 未排除 type=2 子菜单，导致部分子菜单 parent_id 被更新为自身。`
- `RED: local-menu-migration-reapply -> FAIL, 二次应用时冲突检查把已存在但 parent_id 错误的目标子菜单当成不可恢复冲突，需允许目标子菜单自修复 parent_id。`
- `GREEN: local-menu-migration-reapply -> PASS, 修复后迁移可二次应用，并将 900410-900417 全部挂回 900218。`
- `GREEN: mysql-menu-verification -> PASS, 真实库 child_count=8、self_parent_count=0；测试租户角色 111 与 910211 均绑定 8 个电子签名子菜单。`
- `GREEN: real-playwright-e2e -> PASS, 使用 http://localhost:8081 和 测试租户/aoteman/111111 登录，打开 /signature-governance/overview 后页面可见 总览、文件签名记录、批记录签名记录、用户授权、长期留存、周期复核、CSV质量包、统一策略；最近电子签名相关接口均返回 200。`
- `GREEN: evidence-validation -> PASS, bug-regression-evidence 与 database-schema-evidence 均通过校验。`
- `GREEN: task-closeout-cleanup -> PASS, cleanup apply 无删除项；临时 Playwright 截图目录已从前端仓库精确路径清理。`
RED: python -m pytest script\tests\test_unified_electronic_signature_menu_sql.py -> FAIL, 当前 SQL 缺少 @unified_signature_child_menu_id、8 个可见子菜单、旧权限项挂到子菜单、角色子菜单绑定迁移。
GREEN: python -m pytest script\tests\test_unified_electronic_signature_menu_sql.py -> PASS, 5 passed，菜单 SQL 契约已要求 8 个可见子菜单和角色绑定迁移。
