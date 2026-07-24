BDD: 测试服缺少车间主任历史角色时仍可升级 -> Given tenant 1 已存在排产员目标用户与智能排产菜单基线但缺少车间主任角色 When 执行角色范围迁移 SQL Then SQL 应补齐正式车间主任角色并继续收敛菜单权限，而不是直接因缺角色失败。
BDD: 角色分配迁移复用角色范围迁移结果 -> Given 角色范围迁移已确保排产员/车间主任/班组长角色存在 When 执行账号角色分配 SQL Then zhaojie 绑定排产员，guliya/wuxiaolei/zhangjiayi 绑定车间主任，并保持幂等。
BDD: 发布链路只接受已提交修复 -> Given 测试服 publish-test 真实执行 required SQL When 重新构建并部署同一 releaseTag 闭环 Then required SQL 不再因缺少 mes_workshop_director 历史基线而阻塞。
GREEN: previous-task-check -> PASS，最近相关后端任务 `20260629-mes-smart-scheduling-role-scope-check` 已 COMPLETED，本次以独立发布兼容修复继续处理。
GREEN: experience-preflight -> PASS，已读取 `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`、`D:\ProjectPackage\Int\IntRuoyi\docs\release-backup-restore.md`、`D:\ProjectPackage\Int\IntRuoyi\docs\server-access.md`；确认本轮服务器动作仅限测试服 `172.30.30.58` 的只读 SQL 核验，复杂远端 SQL 采用 UTF-8 感知运行时执行，避免 PowerShell 5.1 here-string/中文转义误伤。
GREEN: publish-blocker-readonly-check -> PASS，维护仓真实发布 `release-20260629-1815-committed-head-v4` 的 `publish-test` 已在测试服 required SQL `20260629_mes_smart_scheduling_role_scope.sql` 失败，错误为 `Missing enabled MES workshop director role in tenant 1`。
GREEN: readonly-test-role-baseline -> PASS，测试服 `172.30.30.58` / `ruoyi-vue-pro` 真实回查确认：
- `tenant_id=1` 当前存在 `910216 / 排产员 / 排产员`
- `tenant_id=1` 当前不存在 `车间主任`，也不存在 `code=mes_workshop_director`
- `tenant_id=1` 当前不存在 `班组长`，也不存在 `code=mes_team_leader`
- 目标账号 `zhaojie / guliya / wuxiaolei / zhangjiayi` 均真实存在且启用
结论：测试服阻塞来自历史角色基线缺口，而不是账号缺失、菜单基线缺失或发布脚本未执行到该 SQL。
RED: `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_mes_smart_scheduling_role_scope_sql.py -q` -> FAIL，新增“缺少车间主任历史角色时自动补齐”契约断言前，现有 SQL 仍保留 `Missing enabled MES workshop director role in tenant 1` fail-fast 语义，无法覆盖测试服真实升级路径。
GREEN: role-scope-release-compat-fix -> PASS，已将 `20260629_mes_smart_scheduling_role_scope.sql` 调整为：在 `tenant_id=1` 缺少 `车间主任 / mes_workshop_director` 时自动插入正式角色 `910238 / mes_workshop_director / 车间主任`，再继续后续角色菜单收敛；保留排产员缺失时的 fail-fast。
GREEN: `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_mes_smart_scheduling_role_scope_sql.py -q` -> PASS，7 passed。
GREEN: `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_mes_smart_scheduling_role_scope_related_tabs_sql.py -q` -> PASS，3 passed。
GREEN: `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_mes_smart_scheduling_role_assignment_sql.py -q` -> PASS，3 passed。
GREEN: publish-test-v9-readonly-followup -> PASS，维护仓真实 `publish-test` operation `op-2026-06-29T125803742596500Z-cf9b40cd-ac0d-47f7-b4d4-6d21a200fcd3` 已继续跨过：
- `20260629_mes_smart_scheduling_role_scope.sql`，并在真实发布日志中登记为 `APPLIED`
- `20260629_srm_admin_role_visibility.sql`
- `20260629_dcc_controlled_file_recognition_claim.sql`

新的真实失败点后移到 `20260629_mes_smart_scheduling_role_assignment.sql`：
- `ERROR 1644 (45000) at line 121: Missing enabled MES workshop director role in tenant 1 for assignment`

GREEN: role-assignment-followup-root-cause -> PASS，基于真实发布日志与当前 SQL 合同核对确认：
- `role_assignment.sql` 明确要求 `tenant_id=1` 下必须存在 `deleted=b'0'` 且 `status=0` 的 `车间主任 / mes_workshop_director`
- 当前 `role_scope.sql` 虽已在“完全不存在角色”时补建，但对历史上已存在同名或同 code、却被禁用或软删的角色没有恢复逻辑
- 因此真实链路可能出现：`role_scope.sql` 已执行完成，但 `role_assignment.sql` 仍拿不到可用车间主任角色

RED: `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_mes_smart_scheduling_role_scope_sql.py -q` -> FAIL，新增“恢复已存在但被禁用/软删的车间主任与班组长角色，并解析单一可用角色 ID 供后续分配复用”的契约断言前，当前 SQL 尚未覆盖该真实发布场景。
GREEN: role-scope-reactivate-disabled-role-release-compat -> PASS，已将 `20260629_mes_smart_scheduling_role_scope.sql` 收口为：
- 先把 `tenant_id=1` 下命中 `车间主任 / mes_workshop_director`、`班组长 / mes_team_leader` 的历史角色统一恢复为 `status=0`、`deleted=b'0'`
- 若仍不存在再按动态主键安全模式补建
- 分别解析 `v_workshop_director_role_id` 与 `v_team_leader_role_id`，后续菜单权限收口与角色分配统一复用单一可用角色 ID
GREEN: `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_mes_smart_scheduling_role_scope_sql.py -q` -> PASS，10 passed。
GREEN: `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_mes_smart_scheduling_role_scope_related_tabs_sql.py -q` -> PASS，3 passed。
GREEN: `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_mes_smart_scheduling_role_assignment_sql.py -q` -> PASS，3 passed。
GREEN: publish-test-v8-readonly-followup -> PASS，维护仓真实 `publish-test` operation `op-2026-06-29T122410252644100Z-9ba2cd36-8610-4350-a4d5-7df446a17a5d` 已真实跨过维护仓构建上下文、镜像加载、`.env IMAGE_TAG`、required SQL 传输与大部分迁移登记，但在同一 SQL `20260629_mes_smart_scheduling_role_scope.sql` 上暴露第三轮阻塞：`ERROR 1062 (23000) at line 244: Duplicate entry '910238' for key 'system_role.PRIMARY'`。
GREEN: readonly-v8-role-id-regression-root-cause -> PASS，测试服 `172.30.30.58` / `ruoyi-vue-pro` 真实回查确认：
- `tenant_id=1` 当前已存在 `910240 / mes_workshop_director / 车间主任`
- `tenant_id=1` 当前仍不存在 `mes_team_leader / 班组长`
- `system_role.id=910238` 已被历史角色 `报工冒烟非审批员 / post_release_mes_smoke_non_approver` 占用
- 当前 SQL 的两段角色创建语句仍采用 `INSERT ... SELECT COALESCE(MAX(id), ...) + 1 FROM system_role WHERE NOT EXISTS (...)`
结论：当 `mes_workshop_director` 已存在时，`WHERE NOT EXISTS (...)` 使聚合源为空，但 MySQL 仍返回 1 行 `NULL` 聚合结果，从而把后续主键再次回退到固定基线 `910238/910239`，这是第三轮真实冲突根因。
RED: `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_mes_smart_scheduling_role_scope_sql.py -q` -> FAIL，新增“角色创建必须使用 \`FROM DUAL + 独立 MAX(id)\` 子查询，避免 \`WHERE NOT EXISTS\` 为假时仍返回聚合默认行”的契约断言前，当前 SQL 仍采用不安全聚合写法。
GREEN: safe-role-id-select-pattern-release-compat -> PASS，已将 `车间主任 / 班组长` 两段角色创建统一收口为：
- `SELECT (SELECT COALESCE(MAX(existing_role.id), ...) + 1 FROM system_role AS existing_role), ... FROM DUAL`
- `WHERE NOT EXISTS (...)`
- 从而把“是否插入”与“如何取下一个主键”解耦，避免已有角色时仍回退到固定基线主键
GREEN: `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_mes_smart_scheduling_role_scope_sql.py -q` -> PASS，8 passed。
GREEN: `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_mes_smart_scheduling_role_scope_related_tabs_sql.py -q` -> PASS，3 passed。
GREEN: `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_mes_smart_scheduling_role_assignment_sql.py -q` -> PASS，3 passed。
GREEN: publish-test-v6-readonly-followup -> PASS，维护仓真实 `publish-test` 已继续跨过 `910238` 冲突与此前缺失车间主任角色阻塞，但在同一 required SQL `20260629_mes_smart_scheduling_role_scope.sql` 上命中新阻塞：`ERROR 1062 (23000) at line 243: Duplicate entry '910239' for key 'system_role.PRIMARY'`。
GREEN: readonly-team-leader-id-conflict -> PASS，测试服 `172.30.30.58` / `ruoyi-vue-pro` 真实回查确认：
- `tenant_id=1` 当前已经新增成功 `910240 / 车间主任 / mes_workshop_director`
- `system_role.id=910239` 已被其他历史角色占用，`name` 显示为乱码、`code=doc_control`、`tenant_id=122`
- `tenant_id=1` 真实仍不存在 `班组长 / mes_team_leader`
结论：第二轮兼容修复证明 `车间主任` 动态主键策略有效，但 `班组长` 仍写死为 `910239`，导致同一 SQL 后续步骤继续与历史主键冲突。
RED: fixed-team-leader-id-release-compat -> FAIL，当前 SQL 仍把 `班组长 / mes_team_leader` 写死为 `id=910239`，无法兼容测试服历史角色主键占用。
GREEN: dynamic-team-leader-id-release-compat -> PASS，已将班组长插入策略收口为：
- 仅在 `tenant_id=1` 不存在 `name=班组长` 或 `code=mes_team_leader` 时插入
- 新角色 `id` 改为 `COALESCE(MAX(id), 910238) + 1`，避免与测试服已有 `910239` 历史角色冲突
- 后续角色菜单收敛与账号分配仍按 `name/code` 解析，不依赖固定主键
GREEN: `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_mes_smart_scheduling_role_scope_sql.py -q` -> PASS，7 passed。
GREEN: `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_mes_smart_scheduling_role_scope_related_tabs_sql.py -q` -> PASS，3 passed。
GREEN: `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_mes_smart_scheduling_role_assignment_sql.py -q` -> PASS，3 passed。
GREEN: publish-test-v5-readonly-followup -> PASS，维护仓真实 `publish-test` 已进一步跨过“缺少车间主任角色”阻塞，但在同一 SQL 上命中新阻塞：`ERROR 1062 (23000) at line 242: Duplicate entry '910238' for key 'system_role.PRIMARY'`。
GREEN: readonly-test-role-id-conflict -> PASS，测试服 `172.30.30.58` / `ruoyi-vue-pro` 真实回查确认：
- `system_role.id=910238` 已被历史角色 `报工冒烟非审批员 / post_release_mes_smoke_non_approver` 占用
- `tenant_id=1` 真实仍不存在 `name=车间主任` 且不存在 `code=mes_workshop_director`
结论：首轮兼容修复已证明必须自动补 `mes_workshop_director`，但固定写死 `id=910238` 与测试服历史角色主键发生冲突，仍不满足真实升级路径。
RED: fixed-role-id-release-compat -> FAIL，当前 SQL 仍把车间主任写死为 `id=910238`，无法兼容测试服历史冒烟角色主键占用。
GREEN: dynamic-role-id-release-compat -> PASS，已将车间主任插入策略收口为：
- 仅在 `tenant_id=1` 不存在 `name=车间主任` 或 `code=mes_workshop_director` 时插入
- 新角色 `id` 改为 `COALESCE(MAX(id), 910237) + 1`，避免与测试服已有 `910238` 历史角色冲突
- 角色菜单与账号分配仍按 `name/code` 解析，不依赖固定主键
GREEN: `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_mes_smart_scheduling_role_scope_sql.py -q` -> PASS，7 passed。
GREEN: `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_mes_smart_scheduling_role_scope_related_tabs_sql.py -q` -> PASS，3 passed。
GREEN: `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_mes_smart_scheduling_role_assignment_sql.py -q` -> PASS，3 passed。
