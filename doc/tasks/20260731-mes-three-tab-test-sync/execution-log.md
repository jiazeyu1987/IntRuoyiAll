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

## 2026-07-31 Deterministic Remap Dependency Sync

- USER INTENT: 用户明确授权“确定性 ID 重映射”，给冲突用户/生产工单生成新 ID，并同步更新三页签包内所有引用。
- POLICY: 仅对授权依赖执行确定性重映射；不覆盖、不删除、不复用其它租户冲突行，不把未授权表单版本、权限范围、日历、工作站或 schema 变更纳入本轮写入。
- BDD: 确定性重映射 -> Given 源依赖 ID 在测试服全局或同租户业务身份冲突 / When 执行授权依赖同步 / Then 工具必须按目标当前最大 ID 后的稳定空闲区间生成新 ID，并保留映射证据。
- BDD: 三页签引用映射预检 -> Given 源三页签包仍引用旧用户/生产工单 ID / When 重跑 preflight / Then 依赖校验必须按 `dependency-remap-plan.json` 映射到目标新 ID 后比对业务身份。
- GREEN: `python -X utf8 -m py_compile doc\tasks\20260731-mes-three-tab-test-sync\tools\sync_authorized_missing_dependencies.py` -> PASS。
- RED: `python -X utf8 doc\tasks\20260731-mes-three-tab-test-sync\tools\sync_authorized_missing_dependencies.py` -> FAIL，expected reason: 业务键扫描使用字符串 `IN` 触发目标列排序规则混用 `ERROR 1271 Illegal mix of collations`；失败发生在写入前，未生成计划且未插入数据。
- GREEN: collation-safe business-key scan -> PASS，已改为 `HEX(username/code) IN (...)`，避免临时字符串排序规则参与比较。
- GREEN: `python -X utf8 doc\tasks\20260731-mes-three-tab-test-sync\tools\sync_authorized_missing_dependencies.py` -> PASS，生成 `artifacts/dependency-remap-plan.json`、`artifacts/dependency-remap-summary.md`、`artifacts/authorized-dependency-sync-result.json`。
- Remap result: `system_users.910269 -> 910293`；`mes_pro_work_order` 共 `18` 个重映射：`925473->925781`、`925477->925782`、`925483->925783`、`925553->925784`、`925671->925785`、`925675->925786`、`925685->925787`、`925689->925788`、`925693->925789`、`925698->925790`、`925704->925791`、`925710->925792`、`925711->925793`、`925716->925794`、`925721->925795`、`925724->925796`、`925729->925797`、`925732->925798`。
- Dependency insert result: `mes_md_item` 保留源 ID 插入 `1` 条；`system_users` 重映射插入 `1` 条；`mes_pro_work_order` 重映射插入 `18` 条、保留源 ID 插入 `20` 条，另有 `2` 条目标已精确一致。
- Backup evidence: 创建 `mes_three_tab_dep_remap_backup_20260731012048_mes_md_item`、`mes_three_tab_dep_remap_backup_20260731012048_system_users`、`mes_three_tab_dep_remap_backup_20260731012048_mes_pro_work_order`。
- GREEN: dependency postcheck -> PASS，授权依赖在测试服 `tenant_id=1` 按源业务身份校验无 missing、无 mismatched。
- GREEN: dependency sync idempotency -> PASS，复跑 `sync_authorized_missing_dependencies.py` 加载既有 `dependency-remap-plan.json`，`pending_insert_total=0`，未重新分配 ID、未重复插入，并在结果文件保留历史备份表名。
- GREEN: `python -X utf8 -m py_compile doc\tasks\20260731-mes-three-tab-test-sync\tools\three_tab_sync_preflight.py` -> PASS。
- GREEN: remap-aware preflight dependency section -> PASS，`item_id/user_id/work_order_id` 缺失与不一致均为 `0`，其中用户映射 `1` 个、生产工单映射 `18` 个。
- RED: `python -X utf8 doc\tasks\20260731-mes-three-tab-test-sync\tools\three_tab_sync_preflight.py` -> FAIL by design，expected reason: 剩余 `10` 项阻塞仍未解决，主三页签白名单替换未执行。
- Remaining blockers: schema `5` 项、缺失 `bpm_form_template_version` `2` 个、缺失 `mes_pro_edhr_permission_scope` `14` 个、`calendar_rule_id` 不一致、`workstation_id` 不一致、白名单外活动引用仍存在。
- STATUS: blocked，授权依赖已同步完成；主三页签同步仍未执行。

## 2026-07-31 Continued Authorization And Final Sync

- USER INTENT: 用户回复“继续授权”，按当前上下文授权解决剩余阻塞，包括 schema 对齐、表单模板版本、权限范围、日历/工作站不一致、白名单外引用处理，以及后续页面验证发现的最小正式依赖。
- POLICY: 扩展同步仍限定为三页签运行必需依赖；禁止同步无关业务数据、覆盖其它租户冲突行、关闭约束或使用默认值掩盖缺失来源。
- BDD: 剩余阻塞授权闭环 -> Given 用户授权继续处理剩余三页签阻塞 / When schema、依赖、外部引用和索引差异被处理 / Then 主白名单同步前后必须有备份、显式结果和 blocker=0 预检。
- BDD: 真实页面运行依赖 -> Given 白名单 hash 已对齐但页面列表接口仍因正式报表元数据缺失失败 / When 同步最小批记录报表元数据依赖 / Then 工序设置列表必须通过正式页面和接口验证，不得用 API-only 或空值绕过。
- GREEN: `python -X utf8 doc\tasks\20260731-mes-three-tab-test-sync\tools\sync_remaining_authorized_blockers.py` -> PASS，插入表单模板版本 `27/32`、14 条权限范围，更新日历规则 `1`，工作站 `900131 -> 922057`。
- GREEN: `python -X utf8 doc\tasks\20260731-mes-three-tab-test-sync\tools\apply_whitelist_schema_delta.py` -> PASS，补齐白名单承载所需 schema 列并保留 `artifacts/whitelist-schema-delta-result.json`。
- GREEN: `python -X utf8 doc\tasks\20260731-mes-three-tab-test-sync\tools\cleanup_authorized_external_references.py` -> PASS，19 组白名单外活动引用已备份并软删除；复验 active references = 0，备份表 `m3extbk_20260731023513_*`。
- GREEN: `python -X utf8 doc\tasks\20260731-mes-three-tab-test-sync\tools\sync_three_tab_whitelist.py` -> PASS，20 张白名单表替换完成，插入源端 `2,989` 行，备份表 `m3syncbk_20260731025926_*`，逐表 hash 对齐。
- GREEN: `python -X utf8 doc\tasks\20260731-mes-three-tab-test-sync\tools\apply_whitelist_index_delta.py` -> PASS，补齐源端唯一索引差异，结果见 `artifacts/whitelist-index-delta-result.json`。
- GREEN: `python -X utf8 doc\tasks\20260731-mes-three-tab-test-sync\tools\three_tab_sync_preflight.py` -> PASS，`blocker_count=0`，source whitelist total `2,989`，target whitelist total `2,989`。
- RED: `node doc\tasks\20260731-mes-three-tab-test-sync\tools\verify_test_server_three_tabs.mjs --timeout 90000` -> FAIL，expected reason: 真实测试服页面登录和滑块验证码通过，但 `/admin-api/mes/pro/process/page` 返回 `系统异常`；后端日志为 `Missing batch record report: routeProcessId=928609, reportId=1d05410f1d3140c5b8aa6786887ae69c`。
- GREEN: `python -X utf8 doc\tasks\20260731-mes-three-tab-test-sync\tools\sync_missing_batch_record_reports.py` -> PASS，插入最小正式依赖：`mes_pro_batch_record_definition=1`、`mes_pro_batch_record_version=1`、`mes_pro_batch_record_report=14`；后置缺失报表数 `0`，备份表 `m3brepbk_20260731115458_*`。
- GREEN: `python -X utf8 doc\tasks\20260731-mes-three-tab-test-sync\tools\three_tab_sync_preflight.py` -> PASS，最终 `blocker_count=0`，source/target whitelist rows 均为 `2,989`。
- GREEN: `node doc\tasks\20260731-mes-three-tab-test-sync\tools\verify_test_server_three_tabs.mjs --timeout 90000` -> PASS，测试服 `http://172.30.30.58:8081/`、`芋道源码/admin` 真实登录并完成滑块验证码；工序设置 total `65`、工艺流程 total `3`、排产工单 total `10`，截图写入 `artifacts/test-server-e2e/*.png`。
- STATUS: ready_for_closeout，数据同步和必要验证已完成；剩余为 cleanup preview/apply、经验沉淀、提交和推送门禁。

## 2026-07-31 Closeout Evidence

- GREEN: project-experience-consolidation -> PASS，已将批记录报表元数据依赖核对补入 `docs/database-rules.md#mes-三页签跨环境同步完整性门禁`，并在 `docs/experience-index.md` 增加 `Missing batch record report` 等关键词路由。
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260731-mes-three-tab-test-sync --mode preview` -> PASS，keep 包含 task/execution-log/verification-report/tools/artifacts，delete/blocked/warnings 均为 none。
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260731-mes-three-tab-test-sync --mode apply` -> PASS，无删除、无阻塞、无 warnings。
- STATUS: completed，数据同步、真实页面验证、经验沉淀和 cleanup apply 均完成；Git 提交/推送状态单独按仓库状态记录。
