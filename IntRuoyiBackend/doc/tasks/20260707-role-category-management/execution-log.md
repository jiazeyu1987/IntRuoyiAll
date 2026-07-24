# 权限角色分类管理执行日志

BDD: 分类文件夹展示 -> Given 管理员进入权限角色页 When 页面加载完成 Then 左侧显示展厅、批记录、排产、文控、SRM、菜单六个分类文件夹，右侧显示当前分类角色。

BDD: 按分类筛选角色 -> Given 权限角色分别属于展厅和批记录 When 管理员点击展厅分类 Then 列表只返回展厅分类角色且分页总数按该分类计算。

BDD: 角色分类必选 -> Given 管理员新增或编辑权限角色 When 未选择分类提交 Then 后端返回校验失败，前端表单阻止提交并提示分类不能为空。

BDD: 停用分类不可新选 -> Given 某角色分类已停用 When 管理员新建角色选择该分类 Then 后端拒绝保存并提示分类不可用。

BDD: 引用分类不可删除 -> Given 分类下仍存在角色 When 管理员删除该分类 Then 后端拒绝删除并提示分类已被角色引用。

BDD: 配置包分类契约 -> Given 角色配置包包含分类与角色 categoryCode When 导入目标环境 Then 先按 categoryCode 解析分类，缺失分类时 fail fast。

BDD: 历史角色自动归类 -> Given 迁移脚本处理已有 system_role When 角色 code/name/remark 无法命中归类规则 Then 迁移失败并输出未匹配角色清单。

RED: 待执行 -> FAIL, 现有代码缺少 RoleCategoryService、system_role.category_id、配置包 categoryCode 和前端分类树。

RED: .\mvnw.cmd -pl yudao-module-system '-Dtest=RoleCategoryServiceImplTest,RoleServiceImplTest,RoleConfigPackageServiceImplTest' test -> FAIL, 初始实现缺少角色分类服务、角色分类字段、配置包分类契约。

GREEN: .\mvnw.cmd -pl yudao-module-system '-Dtest=RoleCategoryServiceImplTest,RoleServiceImplTest,RoleConfigPackageServiceImplTest' test -> PASS, Tests run: 36, Failures: 0, Errors: 0, Skipped: 0。

GREEN: mvn.cmd -pl yudao-module-system "-Dtest=RoleCategoryServiceImplTest,RoleServiceImplTest,RoleConfigPackageServiceImplTest" test -> PASS, Tests run: 36, Failures: 0, Errors: 0, Skipped: 0。

GREEN: python -X utf8 -m pytest script/tests/test_system_role_category_management_sql.py -q -> PASS, 6 passed in 0.15s。

GREEN: node tests/e2e/system-role-category-static.spec.js -> PASS, system role category static contract。

GREEN: node tests/e2e/system-role-category-static.spec.js -> PASS, 已覆盖分类查询权限种子和迁移未匹配角色结果集输出。

GREEN: NODE_OPTIONS=--max-old-space-size=8192 pnpm.cmd ts:check -> PASS, 首次角色分类前端类型检查通过。

REGRESSION: NODE_OPTIONS=--max-old-space-size=8192 pnpm.cmd ts:check -> FAIL, 阻塞于既有文件 `src/views/mes/pro/edhr-batch/BatchRecordHistoryPage.vue(642,34)` 的 `closedAt` 类型缺失；该文件非本次角色分类改动范围。

GREEN: experience-preflight -> PASS, 本轮未执行服务器写入、真实 E2E、发布、数据库实库写入或 worktree 合并清理；仅进行本机源码、SQL、单元与静态验证。

GREEN: local-backend-runtime -> PASS, 重建并重启本机 `48081` 后端后，`/admin-api/system/role-category/list` 与前端 `8081` 代理均返回 `401 账号未登录`，不再返回“请求地址不存在”。

BLOCKER: local-role-category-migration -> 本机 `ruoyi-vue-pro` 数据库尚无 `system_role_category` 表和 `system_role.category_id` 字段；迁移前置预检发现 17 个历史角色未命中既定分类规则，按需求必须阻塞并由用户确认分类映射：tenant=1 id=1 name=超级管理员 code=super_admin；tenant=1 id=2 name=普通角色 code=common；tenant=1 id=3 name=CRM 管理员 code=crm_admin；tenant=1 id=155 name=测试数据权限1 code=test-dp；tenant=1 id=910207 name=体系工程师 code=体系工程师；tenant=1 id=910295 name=审批中心入口 code=approval_center_entry；tenant=1 id=910296 name=审批管理员 code=approval_admin；tenant=1 id=910297 name=压力泵生产填写员 code=pressure_pump_production_filler；tenant=1 id=910298 name=压力泵设备填写员 code=pressure_pump_equipment_filler；tenant=1 id=910299 name=压力泵质量填写员 code=pressure_pump_quality_filler；tenant=121 id=109 name=租户管理员 code=tenant_admin；tenant=122 id=910268 name=超级管理员 code=super_admin；tenant=122 id=910269 name=普通角色 code=common；tenant=122 id=910270 name=CRM 管理员 code=crm_admin；tenant=122 id=910271 name=测试数据权限1 code=test-dp；tenant=122 id=910277 name=体系工程师 code=体系工程师；tenant=162 id=910205 name=租户管理员 code=tenant_admin。

GREEN: python -X utf8 -m pytest script/tests/test_system_role_category_management_sql.py -q -> PASS, 6 passed in 0.17s，补齐本机历史角色正式归类规则：pressure_pump -> 批记录，体系工程师 -> 文控，super_admin/common/tenant_admin/crm_admin/test-dp/approval_center_entry/approval_admin -> 菜单。

GREEN: local-role-category-migration -> PASS, 已通过 UTF-8 Python stdin 将 `sql/mysql/20260707_system_role_category_management.sql` 应用到本机 Docker MySQL `int-ruoyi-mysql/ruoyi-vue-pro`，MySQL 仅返回命令行密码 warning，无业务错误。

GREEN: local-role-category-db-assertions -> PASS, `system_role_category` 表存在，`system_role.category_id` 字段存在，`system:role-category:*` 权限点 4 个存在，未删除角色 `category_id IS NULL` 数量为 0，4 个租户均有 6 个默认分类，共 24 条分类。

GREEN: experience-preflight -> PASS, 使用系统 Chrome 执行官方登录预检进入本机 `http://localhost:8081/system/role`，租户 `测试租户`，账号 `aoteman`，路径验证通过。

GREEN: role-category-list-runtime -> PASS, 真实登录后监听 `/admin-api/system/role-category/list` 返回 HTTP 200、业务 `code=0`，数据包含展厅、批记录、排产、文控、SRM、菜单 6 个分类，未捕获 `系统异常`、`role-category` 或 `Uncaught` 控制台错误。

GREEN: task-closeout-cleanup-preview -> PASS, `task_closeout.py --task-id 20260707-role-category-management --mode preview` 无 delete、blocked、warnings，保留 `task.md` 与 `execution-log.md`。

