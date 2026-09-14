# Execution Log

## User Intent

用户要求将 DCC 下载、查看、培训、下发等能力尽可能拆成独立权限角色。

## Preflight

- 已读取 `docs/database-rules.md`、`docs/login-access.md`、`docs/powershell-encoding.md`、`docs/task-closeout-rules.md`。
- 已读取 `database-schema-delivery` 技能与 `references/database-contract.md`。
- 已读取 `docs/experience-index.md`，本任务命中 DCC 权限、数据库和 PowerShell 编码门禁。
- 当前只操作本机 `int-ruoyi-mysql` / 租户 `1`，不触碰远端环境。

## BDD

- BDD: DCC view role is independent -> Given 文控用户只需要查看当前受控文件, When 授予查看角色, Then 仅获得受控浏览/预览菜单和 `VIEW` 类别动作，不获得下载、培训或下发。
- BDD: DCC download role is independent -> Given 用户只需要下载能力, When 授予下载角色, Then 仅获得下载菜单和 `DOWNLOAD` 类别动作，仍需另行授予查看角色才能从页面入口浏览。
- BDD: DCC training role is independent -> Given 培训对象需要完成阅读确认, When 授予培训角色, Then 仅获得 `dcc:controlled-file:training:mine`，不获得下载或下发。
- BDD: DCC distribute role is independent -> Given 文控下发人员需要正式下发, When 授予下发角色, Then 仅获得 `DISTRIBUTE` 类别动作，页面访问能力由查看角色单独提供。

## RED/GREEN

- RED: 只读 DB 基线 -> FAIL，当前缺少统一的四个独立角色；`wangsiyu` 仍依赖混合 `wenkong/wenkong_download/dcc_distribute_e2e`，培训对象混用旧 `dcc_training_mine_e2e`。
- RED: `Get-Content ... role-split.sql | docker exec -i int-ruoyi-mysql ... mysql` -> FAIL，首次执行命中 `ERROR 1267 Illegal mix of collations`，按排序规则门禁停止并修复 SQL 会话 collation。
- RED: 修复 collation 后复跑同一 SQL -> FAIL，命中 `ERROR 1295 This command is not supported in the prepared statement protocol yet`，确认 MySQL 不支持 prepared `SIGNAL`，改为临时表 `CHECK` 预检门禁。
- RED: 改为临时表门禁后复跑同一 SQL -> FAIL，预检输出 `DCC_ROLE_SPLIT_BLOCKED: required DCC menu permissions missing`；只读核对发现 `dcc:controlled-file:query` 有两个启用菜单，原脚本按行数计数会误判，并且会把审批任务入口连带给查看角色。
- GREEN: 只绑定 `controlled-file/browser` 查询菜单和预览按钮后复跑 SQL -> PASS，输出角色 ID：view=`910432`、download=`910433`、training=`910434`、distribute=`910435`，targetCategoryId=`906104`。
- GREEN: 只读 DB 核验 -> PASS，四个角色存在；查看角色菜单为 `6807:dcc:controlled-file:query:controlled-file/browser|6810:dcc:controlled-file:preview`；下载角色菜单为 `6811:dcc:controlled-file:download`；培训角色菜单为 `980121:dcc:controlled-file:training:mine`；下发角色菜单数为 `0`。
- GREEN: 类别规则核验 -> PASS，类别 `906104` 下新角色规则为 `VIEW/DOWNLOAD/DISTRIBUTE`，其中培训角色不写类别动作规则。
- GREEN: 账号绑定核验 -> PASS，`wangsiyu` 拥有 `dcc_action_distribute_independent|dcc_action_view_independent`；9 个培训对象均拥有 `dcc_action_training_independent`；培训对象拥有新下载角色数量为 `0`。
- GREEN: Redis 权限缓存刷新 -> PASS，精确删除本次角色、目标用户、目标菜单、目标 permission 相关缓存键，`DEL` 返回 `7`；后端重建后的 `user_role_ids:910250` 包含 `910432/910435`，`menu_role_ids:1:6807` 包含 `910432`。
- GREEN: `node -e "...role-split.sql static contract..."` -> PASS，确认 SQL 包含四个独立角色和 `VIEW/DOWNLOAD/DISTRIBUTE/training:mine`，且不包含更新 DCC 文件状态、master 指针、培训完成状态或确认时间的禁用语句。
- GREEN: `python C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence E:\IntRuoyi\doc\tasks\20260803-dcc-action-role-split\database-schema-evidence.md` -> PASS，`Database schema evidence is valid.`
- GREEN: task file UTF-8/trailing whitespace check -> PASS，`PASS: task files UTF-8 readable and no trailing whitespace`。

## Evidence

- `role-split.sql`：任务自有、幂等、非破坏性 SQL；只新增/启用独立角色和绑定。
- 只读核验摘要：`roleCount=4`，`trainingUsersWithTrainingRole=9`，`trainingUsersWithNewDownloadRole=0`，`distributeMenuCount=0`。
- Redis 缓存证据：源码键名来自 `RedisKeyConstants.java`，启动刷新器覆盖 `role/user_role_ids/menu_role_ids/permission_menu_ids`；本次按精确键刷新。
- 长期经验沉淀判断：已读取 `project-experience-consolidation`；`ERROR 1267` 已由 `docs/database-rules.md#数据修复临时表排序规则门禁` 覆盖，本轮 DCC 菜单拆分细节属于当前任务上下文，暂不改动长期经验文档。

## Blockers

- 功能拆分本身无阻塞。
- 旧混合角色移除属于潜在破坏性变更，本轮不执行；如要彻底收敛，需要单独确认迁移名单和回滚方案。
- Git closeout 未执行：当前工作区存在大量非本任务改动且分支已 ahead 2，本任务未触碰或提交这些并行改动。
