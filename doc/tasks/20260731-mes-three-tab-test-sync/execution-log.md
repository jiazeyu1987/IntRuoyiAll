# Execution Log

## 2026-07-31

- USER INTENT: 将本机 `tenant_id=1 / 芋道源码` 的工序设置、工艺流程、排产工单三页签数据同步到测试服务器同租户，其他数据不同步。
- POLICY: 仅允许白名单三页签数据同步；缺失依赖、schema 差异、白名单外活动引用或校验失败必须 fail fast。
- GIT PRECHECK: `git status --short --branch --untracked-files=all` -> 当前分支 `int_main` 干净但领先 `origin/int_main` 1 个提交；最近提交 `6a1390ff` 为并发 Runner 修复基线，不属于本任务实现。
- BDD: 缺失依赖零写入 -> Given 测试服缺少三页签数据运行所需的正式依赖 / When 执行同步 preflight / Then 同步工具必须阻塞并保持测试服三页签和白名单外数据零写入。
- BDD: 外部引用零破坏 -> Given 测试服白名单外业务表仍引用旧工序、路线或排产记录 / When 用户要求只同步三页签 / Then 工具必须阻塞，不得删除或改写仍被引用的记录。
- BDD: 精确替换 -> Given 依赖完整、schema 对齐、外部引用为零且备份成功 / When 执行同步 / Then 只替换白名单表中 `tenant_id=1` 的有效三页签数据，且白名单外数据 hash 不变。
- BDD: 失败回滚 -> Given 数据替换事务中任一行数、主键、业务键或 hash 校验失败 / When 提交前校验执行 / Then 事务必须回滚并保留失败证据和恢复路径。
- GREEN: experience-preflight -> PASS，已读取 `docs/experience-index.md` 并命中测试服发布、远端 MySQL、release migration、工艺路线导入完整性、排产数据包和 Git 并发基线门禁；适用摘要已补入 `task.md`。
- RED: `python -X utf8 doc/tasks/20260731-mes-three-tab-test-sync/tools/three_tab_sync_preflight.py` -> FAIL，expected reason: 测试服仍存在 schema 差异、缺失依赖和白名单外活动引用；脚本生成 `artifacts/preflight-report.json` 与 `artifacts/preflight-summary.md`。
- Preflight scope: source whitelist total `2989` rows; target current whitelist total `1096` rows.
- Source whitelist counts: process `65`, process_content `0`, route `3`, route_version `46`, route_process `63`, flow_edge `60`, flow_layout `63`, boundary_edge `8`, flow_config `4`, flow_process_config `77`, flow_process_batch_record `20`, route_schedule_config `63`, route_product `16`, route_product_bom `49`, release assignment rule `1`, schedule_order `40`, schedule_order_process `976`, schedule_diff `2`, schedule_daily_compare `0`, schedule_operation_log `1433`.
- Blocker: schema -> target `mes_pro_route_version.route_snapshot_json` is `TEXT` not `MEDIUMTEXT`; target `mes_pro_schedule_order.promise_date` is `NOT NULL`; target `mes_pro_batch_record_report` is missing `form_definition_id` and `form_version_id`; source has route snapshots above target `TEXT` capacity.
- Blocker: dependencies -> missing `bpm_form_template_version` IDs `27,32`; missing `mes_pro_edhr_permission_scope` 14 IDs; missing `mes_md_item.id=924005`; work orders required `40`, missing `33`, mismatched `5`; calendar rule `1` mismatched; workstation dependency mismatched; missing `system_users.id=910269`.
- Blocker: external references -> target has `19` non-whitelist active reference groups, including machinery/process `129`, workstation/process `93`, feedback route/process/schedule refs, route legacy config refs, production task refs `663/664`, and task schedule extension refs `663`.
- GREEN: zero-write-safety -> PASS，本轮仅执行本机 Docker MySQL 与测试服 SSH MySQL 只读查询；未执行 DELETE/INSERT/UPDATE、备份恢复、发布、服务重启或 Playwright 写入验证。
- GREEN: project-experience-consolidation -> PASS，已将通用“三页签跨环境同步完整性门禁”合并到 `docs/database-rules.md`，并在 `docs/experience-index.md` 增加关键词路由；未新建长期经验文档。
- STATUS: blocked，保持安全阻塞；解除上述依赖和外部引用前不得同步测试服三页签数据。

## 2026-07-31 Additional Dependency Authorization

- USER INTENT: 用户明确授权把缺失的物料、用户、生产工单也同步到测试服务器芋道源码租户。
- POLICY: 授权仅覆盖缺失 `mes_md_item`、`system_users`、`mes_pro_work_order` 依赖；不覆盖 schema 迁移、动态表单版本、权限范围、日历、工作站、白名单外引用清理、跨租户覆盖、跨租户删除或主键重映射。
- BDD: 授权依赖同步边界 -> Given 用户只追加授权缺失物料、用户、生产工单 / When 预检发现其它依赖或 schema 阻塞 / Then 工具只能识别授权范围并继续阻塞未授权范围。
- BDD: 全局主键冲突阻塞 -> Given 测试服其它租户已占用待插入依赖主键 / When 尝试按源 ID 插入到 `tenant_id=1` / Then 同步必须失败并保持目标租户零插入，不能覆盖其它租户或静默重映射。
- GREEN: `python -X utf8 doc/tasks/20260731-mes-three-tab-test-sync/tools/three_tab_sync_preflight.py` -> PASS for authorization classification，预检识别授权依赖同步范围 `3` 类：`mes_md_item.id=924005`、`system_users.id=910269`、`mes_pro_work_order` 缺失 `33` 个 ID；整体仍 FAIL by design，剩余阻塞 `11` 项。
- RED: `python -X utf8 doc/tasks/20260731-mes-three-tab-test-sync/tools/sync_authorized_missing_dependencies.py` -> FAIL，expected reason: 测试服存在全局主键冲突，按源 ID 插入授权缺失依赖会触发 `system_users.PRIMARY` duplicate key；事务未提交。
- Verification: read-only target tenant postcheck -> PASS，测试服 `tenant_id=1` 中授权依赖仍为零插入：`mes_md_item=0`、`system_users=0`、`mes_pro_work_order=0`。
- Verification: read-only cross-tenant PK scan -> PASS，`system_users.id=910269` 已存在于 `tenant_id=122`；`mes_pro_work_order` 授权缺失 ID 中 `925473/925477/925483/925671/925675/925685/925689/925693/925716/925721/925724/925729/925732` 已存在于 `tenant_id=122/162`。
- Verification: read-only backup-table scan -> PASS，失败尝试创建的备份表 `mes_three_tab_dep_backup_20260731010102_mes_md_item`、`mes_three_tab_dep_backup_20260731010102_mes_pro_work_order`、`mes_three_tab_dep_backup_20260731010102_system_users` 均为 `0` 行。
- STATUS: blocked，授权的缺失物料、用户、生产工单不能按源主键直接同步；主三页签同步仍未执行。
