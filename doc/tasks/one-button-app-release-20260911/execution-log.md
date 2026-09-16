# Execution Log

- Task ID: `one-button-app-release-20260911`
- Phase: P1 only
- App worktree: `D:\IntRuoyiWorktree\r260911-release-button\a`
- App baseline: `51d02916fcc8c5d188af4e0e72f777835b1fa20f`
- Verified app reference: `713c64112d7b626a2ba71788fd36cc32d686addc`

## BDD

BDD: 标准程序包固定无数据 -> Given 操作者创建标准发布 workflow / When 服务端解析业务意图 / Then `publishScope` 固定为 `app-release`，workflow 请求不接受 `with-data` 或任何基础设施参数。

BDD: 冻结批准源码 -> Given 维护仓与 IntRuoyi 根仓各有批准提交 / When 进入构建前或打包前门禁 / Then 两个 Git root 的 HEAD 和 dirty 均匹配冻结证据，maintenance/backend/frontend 三个 source role 均可追溯。

BDD: 测试失败阻断昂贵构建 -> Given 后端、前端或脚本测试任一失败 / When 执行 P1 构建合同 / Then Maven package、前端 build 和 Docker build 均不启动。

BDD: 三语言摘要一致且非法路径拒绝 -> Given 固定 artifact/manifest bytes、反斜杠路径或大小写冲突路径 / When Java、Python、PowerShell 计算双摘要 / Then合法向量四个摘要完全一致，非法路径以 `PACKAGE_PATH_INVALID` 阻断。

## Baseline And Verified-Line Review

- `713c641` 不是当前 `51d02916f` 的祖先。
- 三个验证线文件与当前主线发生真实差异：当前主线把用户名唯一约束恢复为全部历史行，验证线仅约束 `deleted=0` 的活跃用户。
- P1 将只移植 `active_canonical_username` 生成列、活跃行重复检查和对应测试断言，保留当前主线其它后续 schema/test 内容。

## TDD Evidence

- RED: `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-infra -am -Dtest=ReleaseWorkflowContractTest,ReleaseSourceFreezeTest,ReleaseWorkflowGateOrderTest,ReleaseDigestVectorTest -Dsurefire.failIfNoSpecifiedTests=false test` -> FAIL，8 个合同测试均在运行期明确报告 P1 contract class 缺失；测试与全部依赖模块编译成功，不是编译前置失败。
- RED: `node IntRuoyiFronted/tests/e2e/runtime-control-one-button-static.spec.cjs` -> FAIL，客户端缺少仅含 `reason/sourceSelectionId` 的 workflow intent 合同。
- RED: `python -X utf8 -m pytest -q IntRuoyiBackend/script/tests/test_system_signature_password_t1_contract.py` -> FAIL，其中 2 个 P1 验证线断言证明当前主线缺 `active_canonical_username`；另 1 个 DCC T4 失败属于当前主线既有相邻问题，不计作 P1 RED。
- GREEN: Maven P1 合同命令 -> PASS，8 tests，0 failures/errors/skips。
- GREEN: 前端静态合同 -> PASS；客户端 workflow intent 仅含 reason/sourceSelectionId。
- GREEN: active username 验证线聚焦合同 -> PASS，2 passed、9 deselected；迁移 SQL 与 `713c641` 目标实现逐文件等价，当前主线其它测试/schema 内容保留。
- GREEN: `powershell.exe -File scripts/preflight/branch-runtime-port-guard.ps1` -> PASS，slot 41，frontend 8256，backend 48256；未启动端口。
- REGRESSION: C07 全量 infra reactor -> FAIL，首个非 P1 失败为既有 `CodegenEngineVue2Test` / `CodegenEngineVue3Test` 模板快照差异，共 12 failures；P1 定向类全部通过，未扩大修复。
- BLOCKER: `corepack pnpm --dir IntRuoyiFronted ts:check` -> app worktree 缺 `node_modules/cross-env`；C05 属 P4 门禁，不作为 P1 静态合同假绿，未临时安装依赖兜底。
- COMMIT: app P1 implementation -> `e9fed5ea86dfd8b8b63c779c37d523bc5d92df55`，仅包含 P1 Java/TypeScript/SQL/测试文件。

## Authorization Boundary

- 允许：本机 P1 代码、测试、任务分支提交。
- 禁止：P2-P5、真实 E2E、服务启动、NAS、数据库、MinIO、测试服、正式服和审查服访问或写入。

## P3 App Preflight Scope Continuation

- BDD: app-release 迁移预检合同 -> Given 标准程序包固定为 `PublishScope=app-release` / When 应用仓生成迁移 preflight plan / Then CLI 与内部 allow-list 均接受 `app-release`，并继续执行版本化 data/schema/menu/config/permission/seed 迁移计划；without-data 只禁止全量 MySQL dump、MinIO/DCC/runtime 数据包，不跳过版本化应用迁移。
- RED: `python -X utf8 -m pytest D:\IntRuoyiWorktree\r260911-release-button\a\IntRuoyiBackend\script\tests\test_release_preflight_plan.py -q -k "app_release" --basetemp D:\IntRuoyiWorktree\r260911-release-button\a\.tmp-pytest-one-button-app-release-red` -> FAIL，2 failed，旧 allow-list 拒绝 `app-release`。
- GREEN: `python -X utf8 -m pytest D:\IntRuoyiWorktree\r260911-release-button\a\IntRuoyiBackend\script\tests\test_release_preflight_plan.py -q --basetemp D:\IntRuoyiWorktree\r260911-release-button\a\.tmp-pytest-one-button-app-release-green` -> PASS，19 passed。
- GREEN: `git -C D:\IntRuoyiWorktree\r260911-release-button\a diff --check -- IntRuoyiBackend/script/release/release_preflight_plan.py IntRuoyiBackend/script/tests/test_release_preflight_plan.py` -> PASS。

## P3 Route Menu Contract Continuation

- BDD: 活跃路线菜单兼容旧库和统一后库 -> Given `20260709_mes_route_flow_config_unification` 会把旧排产路线菜单 `900121/900122` 迁移为工艺路线下的 `5726/5727` / When `20260629_mes_smart_scheduling_role_scope` 在目标库执行或执行目标只读预检 / Then SQL 必须选择当前活跃的 query/update 菜单，不因旧菜单已删除而阻断，也不得跳过版本化权限迁移。
- RED: `python -X utf8 -m pytest -q IntRuoyiBackend\script\tests\test_mes_smart_scheduling_role_scope_sql.py IntRuoyiBackend\script\tests\test_release_target_preflight_files.py --basetemp .tmp-pytest-current-dirty-tests` -> FAIL，4 failed；旧迁移硬编码 `900121/900122`，目标只读预检没有验证旧/新路线菜单组合。
- GREEN: `python -X utf8 -m pytest -q IntRuoyiBackend\script\tests\test_mes_smart_scheduling_role_scope_sql.py IntRuoyiBackend\script\tests\test_release_target_preflight_files.py --basetemp .tmp-pytest-route-menu-green` -> PASS，29 passed。
- REGRESSION: `python -X utf8 -m pytest -q script\tests\test_mes_smart_scheduling_role_scope_sql.py script\tests\test_release_target_preflight_files.py script\tests\test_mes_route_flow_config_migration_sql.py script\tests\test_release_preflight_plan.py --basetemp ..\.tmp-pytest-route-menu-regression-2` from `IntRuoyiBackend` -> PASS，54 passed。
- GREEN: `git diff --check -- IntRuoyiBackend/sql/mysql/20260629_mes_smart_scheduling_role_scope.sql IntRuoyiBackend/sql/mysql/target-preflight/20260629_mes_smart_scheduling_role_scope.preflight.sql IntRuoyiBackend/script/tests/test_mes_smart_scheduling_role_scope_sql.py IntRuoyiBackend/script/tests/test_release_target_preflight_files.py` -> PASS。
- IMPLEMENTATION: `20260629_mes_smart_scheduling_role_scope.sql` 新增 `v_process_route_query_menu_id` / `v_process_route_update_menu_id` 解析，优先使用活跃 `5726/5727`，否则兼容尚未统一的 `900121/900122`；目标只读预检改为验证核心 18 个菜单和旧/新路线菜单任一完整组合。

## P3 Puhui Schedule Admin Target Preflight Continuation

- BDD: 璞慧排产管理员迁移兼容当前菜单层级 -> Given 测试服 `智能排产(900120)` 可能位于根级或 MES 根菜单下 / When `20260718_mes_puhui_schedule_admin_role_visibility` 随 app-release 执行或目标只读预检 / Then 迁移必须接受 `parent_id IN (0,5100)` 的受控状态，并在构建前用 target preflight 暴露不支持的菜单漂移。
- RED: `python -X utf8 -c "from pathlib import Path; p=Path(r'E:\Int\CacheData\IntRuoyi\publish-int-ruoyi\release-20260914-one-button-app-r31\required-sql\20260718_mes_puhui_schedule_admin_role_visibility.sql'); text=p.read_text(encoding='utf-8'); assert 'requiresTargetPreflight=true' in text and 'v_smart_scheduling_parent_id' in text and 'NOT IN (0, 5100)' in text, 'r31 package lacks Puhui schedule target-preflight-compatible SQL'"` -> FAIL，R31 旧包缺少该迁移的 target preflight 元数据和父级兼容逻辑，不得发布测试服。
- GREEN: `python -X utf8 -m pytest -q IntRuoyiBackend\script\tests\test_mes_puhui_schedule_admin_role_visibility_sql.py IntRuoyiBackend\script\tests\test_release_target_preflight_files.py --basetemp .tmp-r31-puhui-green` -> PASS，10 tests。
- REGRESSION: `python -X utf8 -m pytest -q script\tests\test_mes_puhui_schedule_admin_role_visibility_sql.py script\tests\test_release_target_preflight_files.py script\tests\test_release_preflight_plan.py --basetemp ..\.tmp-r31-puhui-regression-2` from `IntRuoyiBackend` -> PASS，29 tests。
- GREEN: `git diff --check -- IntRuoyiBackend\script\tests\test_mes_puhui_schedule_admin_role_visibility_sql.py IntRuoyiBackend\script\tests\test_release_target_preflight_files.py IntRuoyiBackend\sql\mysql\20260718_mes_puhui_schedule_admin_role_visibility.sql IntRuoyiBackend\sql\mysql\target-preflight\20260718_mes_puhui_schedule_admin_role_visibility.preflight.sql` -> PASS。
- IMPLEMENTATION: `20260718_mes_puhui_schedule_admin_role_visibility.sql` 增加 `requiresTargetPreflight=true`，读取 `900120` 实际 `parent_id` 并仅接受 `0/5100`；新增目标只读 preflight，验证 MES 根、智能排产、璞慧排产菜单与权限字段完整。

## P3 Cleaning Process Parameter Empty Baseline Continuation

- BDD: 清洗工序参数迁移兼容空规则基线 -> Given 测试服 `mes_pro_process_pool_device_parameter_rule` 当前没有活跃规则 / When `20260811_mes_process_pool_cleaning_process_parameter_data.sql` 随 app-release 执行或目标只读预检 / Then 迁移必须 no-op 通过，只有存在候选规则时才校验五个正式参数、重复归一化冲突和不完整规则组。
- RED: `python -X utf8 -m pytest -q D:\IntRuoyiWorktree\r260911-release-button\a\IntRuoyiBackend\script\tests\test_mes_process_pool_cleaning_process_parameter_data_sql.py --basetemp D:\IntRuoyiWorktree\r260911-release-button\a\IntRuoyiBackend\.tmp-r38-cleaning-process-red` -> FAIL，2 failed；旧迁移把 “No active cleaning process ultrasonic cleaner parameter rules found” 作为硬错误，target preflight 只检查清洗工序存在，没有覆盖空规则基线。
- GREEN: `python -X utf8 -m pytest -q D:\IntRuoyiWorktree\r260911-release-button\a\IntRuoyiBackend\script\tests\test_mes_process_pool_cleaning_process_parameter_data_sql.py --basetemp D:\IntRuoyiWorktree\r260911-release-button\a\IntRuoyiBackend\.tmp-r38-cleaning-process-green` -> PASS，2 passed。
- REGRESSION: `python -X utf8 -m pytest -q script\tests\test_mes_process_pool_cleaning_process_parameter_data_sql.py script\tests\test_release_target_preflight_files.py script\tests\test_release_preflight_plan.py --basetemp .tmp-r38-cleaning-process-regression` from `IntRuoyiBackend` -> PASS，26 passed。
- GREEN: test-server-readonly-preflight -> PASS，测试服只读执行新版 `target-preflight/20260811_mes_process_pool_cleaning_process_parameter_data.preflight.sql` 返回 `TARGET_PREFLIGHT_PASS:20260811_mes_process_pool_cleaning_process_parameter_data`；诊断查询显示 `rules_total=0`、`rules_cleaning_process_join=0`。
- IMPLEMENTATION: `20260811_mes_process_pool_cleaning_process_parameter_data.sql` 新增 `v_cleaning_rule_candidate_count`，仅在候选规则数量大于 0 时执行重复/不完整硬阻断；target preflight 改为 CTE 分组校验，接受空候选基线并保留有候选时的严格校验。
- RESULT: R38 已在测试服版本切换前失败并释放 lock，失败点为旧包内该 SQL 的空规则硬阻断；R38 判废不复用。下一轮必须使用新 releaseTag `release-20260914-one-button-app-r39`，source freeze 固定维护仓当前 clean HEAD 与本应用修复提交后的 clean HEAD。

## P3 UV curing I/II empty source baseline continuation

- BDD: 光固设备参数迁移兼容空来源基线 -> Given 测试服存在光固Ⅰ/Ⅱ工序但没有启用的 A05075/A05059 来源设备配置 / When `20260811_mes_process_pool_uv1_metering_valid_parameter.sql` 或 `20260811_mes_process_pool_uv2_two_device_runtime_config.sql` 随 without-data/app-release 执行 / Then 迁移应 no-op 通过；只有存在候选来源时才严格校验目标设备、绑定、参数克隆和计量有效期冲突。
- RED: `python -X utf8 -m pytest -q script\tests\test_mes_process_pool_uv1_metering_valid_parameter_sql.py --basetemp .tmp-r40-uv1-red` -> FAIL，2 failed；旧 UV1 迁移把缺少 A05075 来源配置当硬错误，target preflight 只检查表存在。
- GREEN: `python -X utf8 -m pytest -q script\tests\test_mes_process_pool_uv1_metering_valid_parameter_sql.py script\tests\test_mes_process_pool_uv2_two_device_runtime_config_sql.py script\tests\test_release_target_preflight_files.py --basetemp .tmp-r40-uv-green` -> PASS，9 passed。
- REGRESSION: `python -X utf8 -m pytest -q script\tests\test_release_preflight_plan.py script\tests\test_release_target_preflight_files.py script\tests\test_mes_process_pool_uv1_metering_valid_parameter_sql.py script\tests\test_mes_process_pool_uv2_two_device_runtime_config_sql.py script\tests\test_mes_process_pool_cleaning_process_parameter_data_sql.py --basetemp .tmp-r40-uv-regression` -> PASS，30 passed。
- GREEN: test-server-readonly-preflight -> PASS，新版 UV1/UV2 target preflight 在测试服均返回 `TARGET_PREFLIGHT_PASS`；诊断查询显示 UV1 `uv1_source_rule_scopes=0`、UV2 `uv2_a05075_binding_count=0`。
- IMPLEMENTATION: UV1/UV2 迁移移除“来源配置为 0 时直接 SIGNAL”的硬阻断，保留存在候选来源时的冲突校验；两个 target preflight 改为 CTE 读取候选数量、目标设备/绑定/规则冲突并允许空来源基线 no-op。
- RESULT: R40 已在测试服版本切换前失败并释放 lock，失败点为旧包内 UV1 SQL；R40 判废不复用。下一轮必须使用新 releaseTag `release-20260914-one-button-app-r41`，source freeze 固定维护仓当前 clean HEAD 与本应用修复提交后的 clean HEAD。

## P3 C00 legacy collation continuation

- BDD: C00 回填兼容旧表排序规则 -> Given 测试服旧业务表 `mes_pqc_inspection_task` 的文本列使用 `utf8mb4_unicode_ci` 且会与 MySQL 8 默认 `utf8mb4_0900_ai_ci` 派生文本比较 / When `20260812_mes_pqc_dcc_qa_c00_backfill.sql` 随 without-data/app-release 执行 / Then 所有规则键、hash、JSON 文本比较必须显式 `COLLATE utf8mb4_unicode_ci`，不得在版本切换前因隐式 collation 冲突失败。
- RED: `python -X utf8 -m pytest -q script\tests\test_mes_pqc_dcc_qa_c00_backfill_sql.py --basetemp .tmp-r43-c00-collation-red` -> FAIL，新增合同发现 `SHA2(manifest.canonical_payload_json, 256) <> manifest.submitted_content_hash` 等隐式文本比较仍存在。
- GREEN: `python -X utf8 -m pytest -q script\tests\test_mes_pqc_dcc_qa_c00_backfill_sql.py --basetemp .tmp-r43-c00-collation-green` -> PASS，3 passed。
- REGRESSION: `python -X utf8 -m pytest -q script\tests\test_mes_pqc_dcc_qa_c00_backfill_sql.py script\tests\test_release_target_preflight_files.py script\tests\test_release_preflight_plan.py --basetemp .tmp-r43-c00-collation-regression` -> PASS，27 passed。
- IMPLEMENTATION: C00 回填 SQL 对 `SHA2(...)`、`inspection_rule_key`、`submitted_content_hash`、`piece_detail_sha256`、`JSON_UNQUOTE(...inspectionRuleKey)`、`scrapQuantity` 与 `nonconformanceDescription` 文本比较显式固定到 `utf8mb4_unicode_ci`；无 fallback、无跳过迁移、无目标数据修改脚本。
- RESULT: R43 已在测试服版本切换前失败并释放 lock，失败点仍为 C00 回填旧表/派生文本排序规则冲突；R43 判废不复用。下一轮必须使用新 releaseTag `release-20260915-one-button-app-r44`，source freeze 固定维护仓记录本次失败后的 clean HEAD 与本应用修复提交后的 clean HEAD。

## P3 pressure pump same-name item nullable master continuation

- BDD: 压力泵同名物料收敛兼容已绑定空产品主数据 -> Given 测试服 `RT000028` 已存在 `AW.107.02.01.1009` 路线产品绑定且该物料/项目的 `product_master_id` 为 NULL / When `20260818_mes_pressure_pump_same_name_item_convergence.sql` 随 without-data/app-release 执行 / Then 迁移应 no-op 通过，只有目标物料、路线、重复绑定或插入前 DCC 产品主数据约束真实漂移时才阻断。
- RED: deploy-release-r51-pressure-pump-target-item-drift -> FAIL，测试服部署在版本切换前执行 `20260818_mes_pressure_pump_same_name_item_convergence.sql` 失败：`Pressure pump convergence failed: target item identity drifted`。只读诊断显示目标物料 `902101/AW.107.02.01.1009` 存在但 `product_master_id=NULL`，目标路线 `922119/RT000028` 存在，且 `mes_pro_route_product` 已有 `route_id=922119,item_id=902101` 的有效绑定。
- RED: `python -X utf8 -m pytest -q script\tests\test_mes_pressure_pump_same_name_item_convergence_sql.py --basetemp .tmp-r51-pressure-pump-red` -> FAIL，2 failed；旧迁移要求 `product_master_id=11`，旧 target preflight 仅检查表存在，未覆盖已绑定 no-op 和目标数据约束。
- GREEN: pressure-pump-nullable-master-fix -> PASS，迁移允许目标物料 `product_master_id` 为 NULL，并把已存在唯一路线产品绑定作为幂等 no-op；只有需要插入新绑定时才检查 DCC 项目产品主数据与路线产品漂移。target preflight 改为 CTE 校验目标物料、路线、已有绑定、插入前 DCC 约束与漂移条件。
- GREEN: `python -X utf8 -m pytest -q script\tests\test_mes_pressure_pump_same_name_item_convergence_sql.py script\tests\test_release_target_preflight_files.py script\tests\test_release_preflight_plan.py --basetemp .tmp-r51-pressure-pump-green2` -> PASS，30 passed。
- GREEN: test-server-readonly-preflight-pressure-pump -> PASS，使用新版 target preflight 在测试服只读事务中返回 `TARGET_PREFLIGHT_PASS:20260818_mes_pressure_pump_same_name_item_convergence`；未修改测试服业务数据。
- RESULT: R51 已在测试服版本切换前失败并释放 lock，旧包判废不复用。下一轮必须使用新 releaseTag `release-20260915-one-button-app-r52`，source freeze 固定维护仓记录本次失败后的 clean HEAD 与本应用修复提交后的 clean HEAD。

## P3 old form-template binding switch regression

- BDD: 旧表单模板 Jimu 布局缺失时前置阻断 -> Given 测试服存在待迁移的 `form_template_id/last_published_template_version_id` 路线绑定，且旧模板版本的 `jimu_schema_json` 只有 `assistRows/fillAssignments`、没有 `sheetLayoutJson` / When app-release 执行 `20260829_mes_old_form_template_binding_switch.sql` / Then 迁移必须在版本切换前明确阻断，或使用正式识别字段转换出可审计 Jimu 布局；不得写入空布局、猜测模板内容或假绿。
- RED: deploy-release-r53-old-form-template-jimu-schema -> FAIL，测试服 `20260829_mes_old_form_template_binding_switch.sql` 在版本切换前返回 `ERROR 1644 (45000): Form template Jimu schema is invalid`；只读诊断冻结 6 条待迁移绑定，模板版本 27/32 的 `jimu_schema_json` 合法但根键仅 `assistRows,fillAssignments`，`sheetLayoutJson` 缺失，未发生版本切换。
- RED: 旧 target preflight 只检查 8 张依赖表，不检查待迁移模板布局，不能在部署前暴露同一数据缺口。
- GREEN: `python -X utf8 -m pytest -q script/tests/test_mes_old_form_template_binding_switch_sql.py script/tests/test_release_target_preflight_files.py -k "old_form_template or target_preflight" --basetemp .tmp-r53-old-form-green2` -> PASS，11 tests；target preflight 增加旧绑定、模板版本、Jimu JSON/识别字段和布局合同检查。
- GREEN: 测试服只读执行新版 `20260829_mes_old_form_template_binding_switch.preflight.sql` 返回 `TARGET_PREFLIGHT_PASS`，因为现有正式 `recognized_schema_json` 字段数组可由迁移侧构建布局；未修改测试服业务数据。
- IMPLEMENTATION: `20260829_mes_old_form_template_binding_switch.sql` 新增基于 `recognized_schema_json` 的临时字段/行/视觉 schema 构建，并让 Jimu 报表阶段读取该同源 schema；非法 JSON、无可识别字段、缺少已发布版本和冲突仍 fail-fast。target preflight 同步覆盖完整字段数组与空/非法形态。
- BLOCKER: 迁移 SQL 尚未在隔离 MySQL 或新的测试服 release 包中验证；当前 R53 包仍包含修复前版本，不能继续发布。需先提交应用修复并生成新的 releaseTag。

## P3 batch-record version schema contract regression

- BDD: 迁移只能写入当前正式表结构 -> Given 目标库 `mes_pro_batch_record_version` 由 `20260708_mes_batch_record_version_phase_one` 创建且不存在 `child_form_member_count/child_form_member_hash` / When `20260829_mes_old_form_template_binding_switch.sql` 创建批记录版本 / Then SQL 只能使用真实存在的 21 个正式列，target preflight 必须在 DML 前检查列集合，不得依赖未发布分支字段。
- RED: deploy-release-r55-batch-version-columns -> FAIL，测试服在版本切换前执行 `20260829_mes_old_form_template_binding_switch.sql` 返回 `ERROR 1054 (42S22) at line 1116: Unknown column 'child_form_member_count' in 'field list'`；operation lock 已释放为 FAILED，测试服 `.env` 与实际镜像未切换。
- RED: `python -X utf8 -m pytest -q script/tests/test_mes_old_form_template_binding_switch_sql.py -k declares_contract --basetemp .tmp-r55-child-column-red` -> FAIL，新增合同证明迁移引用目标真实表不存在的 `child_form_member_count/child_form_member_hash`。
- GREEN: `python -X utf8 -m pytest -q script/tests/test_mes_old_form_template_binding_switch_sql.py script/tests/test_release_target_preflight_files.py script/tests/test_release_preflight_plan.py --basetemp .tmp-r55-child-column-green` -> PASS，11+19 项目标合同通过；完整 migration policy gate 619 passed。
- ROOT_CAUSE: `20260829` 迁移从未进入当前正式 schema/DO 的子表成员字段分支，却在 `mes_pro_batch_record_version` INSERT 列表中硬编码两个不存在的列；当前真实库 `DESCRIBE mes_pro_batch_record_version` 与 `MesProBatchRecordVersionDO` 均不含这两个字段。
- FIX: 删除未发布字段引用和对应值，保留当前正式 21 列；target preflight 增加 `information_schema.columns` 21 列检查。未修改测试服数据库数据，未增加 fallback 或默认列。
- GREEN: 本机 Docker MySQL 只读执行迁移新增 recognized-schema 临时构建片段返回模板版本 27/32 `schema_valid=1, layout_type=STRING`；测试服 target preflight 继续只读通过。完整 SQL 首次/重复执行仍需随新 release 包在测试服验证。
- COMMIT: app migration schema contract fix -> `3098b3319`，包含迁移、target preflight、回归测试和任务证据。
- BLOCKER: R55 包包含修复前 SQL，按失败 tag 退休且不得复用；必须生成新 releaseTag 并完成测试服真实 publish-test 验证。

## 通用按钮发布机制审查纠偏（2026-09-15）

- REVIEW: independent-code-audit -> FAIL。当前实现保留了按钮、状态存储、CAS/lease、摘要和验包保护，但六项通用机制仍未闭环：来源选择未绑定预期 commits；schema rehearsal 排除 data/target-preflight 迁移；应用迁移测试入口固定单文件；workflow 阶段由一次 operation 成功后连续补写且无后台协调器；取消/heartbeat/recovery 未接到底层进程与 scheduler；发布脚本仍按具体 migrationId 注入参数、排序和钩子。
- CLOSED: 已有 source dirty/head/path drift、manifest/source commit+digest、append-only journal/CAS/lease、timeout/interruption 进程树终止、迁移 metadata/dependency/policy gate、target readonly preflight 保护；这些不等于六项审查项完成。
- OPEN: 需要先以 BDD/RED/GREEN 补齐统一迁移合同与隔离执行、迁移测试自动发现、来源 commit 持久化、后台阶段事件/恢复/取消一致性和版本化 hook/参数声明；新迁移不得推动发布引擎增加业务分支。
- CORRECTION: R55 失败证据不得表述为“所有 DML 前/数据库完全没有业务写入”。20260829 在未知列错误前已有 definition INSERT；后续只可依据事务边界与真实只读状态描述，不能把“版本未切换”当作零写入证明。

## Source approval and migration test-gate slice

- BDD: 来源批准绑定 -> Given 服务端收到受控 sourceSelectionId / When 创建 workflow 并启动 build / Then workflow 持久化 maintenance/application/frontend 预期 commit，执行器将同一 commit 传给发布脚本并在冻结时校验，不再把当前 HEAD 自动视为批准源。
- RED: 旧实现仅持久化 `approved-source` 字符串，未有 expected commits；新增 `ReleaseWorkflowSourceBindingTest` 与编排器参数断言证明该缺口。
- GREEN: 应用提交 `e109b707c` 后，workflow record/store 持久化三个 approved commit，build action 将 expected commit 与 sourceSelectionId 传给脚本；新增 Java 2 tests，既有编排器/存储测试 10 PASS，RuntimeControlServiceImplTest 65 PASS。
- BDD: 迁移测试自动发现 -> Given 冻结提交包含版本化迁移 SQL / When 进入昂贵构建前测试门禁 / Then 从 source diff 自动发现对应测试，缺失映射以 `MIGRATION_TEST_MISSING` 阻断，不能固定单一业务测试文件。
- RED: 旧 `Invoke-StandardReleaseContractTests` 固定调用 `test_test_tenant1_all_role_permission_sync_sql.py`，新增脚本合同先失败。
- GREEN: 维护仓脚本新增 `Assert-ApplicationMigrationTestsDiscovered`，根据冻结应用提交差异查找对应迁移测试并缺失即阻断；`test_release_target_data_preflight.py` 与相邻 schema rehearsal 回归 37 PASS，PowerShell AST PASS。
- BLOCKER: 该 slice 尚未形成完整 publish-test；后台阶段事件、底层取消/恢复 scheduler、统一迁移隔离执行和版本化 migration hook/参数声明仍未实现。当前任务继续保持 blocked。

## Cancel safety slice

- BDD: 运行中取消 -> Given workflow 已绑定仍为 running 的底层 operation / When 用户请求取消 / Then 服务端必须先确认底层 operation 已终止或进入可核实安全状态，不能先释放 lease 或把页面状态写成 CANCELED。
- RED: 旧 `ReleaseWorkflowOrchestrator.cancel` 无条件调用 workflow cancel 并释放 lease，未检查 operation 状态。
- GREEN: 应用工作树新增运行中 operation 取消阻断检查与回归用例；`ReleaseWorkflowOrchestratorTest` 7 PASS。随后补齐 `RuntimeControlService.cancelOperation`、执行器 operation/process/container 注册与终止接线；编排器仅在底层终止可核实后释放 workflow lease，无法确认时 fail-closed。
- REGRESSION: `mvn --% -f IntRuoyiBackend/pom.xml -pl yudao-module-infra -am -Dtest=ReleaseWorkflowOrchestratorTest,RuntimeControlServiceImplTest,RuntimeControlCommandExecutorImplTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS，79 tests，0 failures。

## Unified migration metadata contract slice

- BDD: 新迁移声明参数与批准钩子 -> Given 迁移需要稳定的执行顺序、session preamble、结果断言或受批准的领域 hook / When release manifest 解析 SQL 元数据并生成 preflight / Then 通用引擎只读取声明字段，不按 migrationId 增加业务分支；未声明的参数不得被隐式注入。
- RED: 旧 manifest/preflight contract 不接受 `applyOrder`、`sessionProfile`、`approvedHook`、`resultAssertion`；新增 parser/preflight contract 先失败。
- GREEN: manifest parser 与 preflight plan 统一解析并传递四类元数据；3 个现有迁移仅通过 SQL 头部声明顺序/session profile/hook。`python -X utf8 -m pytest -q script\\tests\\test_release_preflight_plan.py script\\tests\\test_release_target_preflight_files.py --basetemp .tmp-metadata-app-green` -> PASS，25 tests；Maven runtime/cancel regression -> PASS，79 tests。
- DESIGN: 无 fallback、无吞异常、无 migrationId 特例；统一元数据合同仅声明能力，实际隔离 rehearsal、阶段事件、后台 scheduler/recovery 与完整 publish-test 仍未闭环。

## P2 backend stale-operation recovery slice

- BDD: 过期 workflow 回收 -> Given workflow 心跳已超时且已绑定仍为 `running` 的底层 operation / When 后台恢复入口执行 / Then 必须先调用底层取消并重新读取 operation 状态，只有确认不再运行后才允许 workflow 进入 `RECOVERY_REQUIRED`，无法确认时 fail-closed。
- RED: `mvn --% -f IntRuoyiBackend/pom.xml -pl yudao-module-infra -am -Dtest=ReleaseWorkflowOrchestratorTest -Dsurefire.failIfNoSpecifiedTests=false test` -> FAIL，新增回归测试缺少 `ReleaseWorkflowOrchestrator.recoverStaleWorkflows(Instant)` 接线，证明编排层只有 `ReleaseWorkflowService.recoverStaleWorkflows`，不会终止仍运行的底层 operation。
- GREEN: 新增 `ReleaseWorkflowOrchestrator.recoverStaleWorkflows(Instant)`：扫描超时 workflow，调用 `RuntimeControlService.cancelOperation`，重读 operation 并在仍为 `running` 时抛出 `RELEASE_WORKFLOW_OPERATION_TERMINATION_UNCONFIRMED`；确认终止后委托状态服务落 `RECOVERY_REQUIRED`。同一测试命令 -> PASS，8 tests，0 failures。
- CLOSED: P2 本 bounded slice 已关闭“后台恢复绕过底层运行进程”的一致性缺口；取消/恢复仍保持 fail-closed，无远程服务器、数据库或 releaseTag 操作。
- OPEN: scheduler/定时触发、阶段事件自动消费、统一迁移隔离执行及完整按钮 publish-test 仍未闭环，需后续 P2/P3 slice；本 slice 未声称完整发布验收通过。

## R56 migration test discovery fix

- RED: build-release-r56-migration-test-discovery -> FAIL，维护仓 R56 构建在 Maven/前端静态合同后、后端打包/Docker/NAS/测试服写入前返回 `MIGRATION_TEST_MISSING: 20260812_mes_route_version_snapshot_identity_enforce`；R56 未形成包且不得复用。
- BDD: 路线快照身份强制迁移必须有同名合同测试 -> Given 冻结提交修改 `20260812_mes_route_version_snapshot_identity_enforce.sql` / When release workflow 自动发现迁移测试 / Then `script/tests` 必须存在引用该 migrationId 的测试，覆盖 metadata、approvedHook、not-null 前置阻断和只收紧现有列。
- GREEN: 新增 `test_mes_route_version_snapshot_identity_enforce_sql.py` 覆盖该迁移的 release metadata、`route-snapshot-identity` approvedHook、blocker 为零后才 `ALTER ... NOT NULL`、以及禁止插入/更新/删除业务数据。
- GREEN: `python -X utf8 -m pytest -q IntRuoyiBackend\script\tests\test_mes_route_version_snapshot_identity_enforce_sql.py IntRuoyiBackend\script\tests\test_mes_route_version_lifecycle_sql.py --basetemp .tmp-r56-route-snapshot-green` -> PASS，7 passed。
- REGRESSION: `python -X utf8 -m pytest -q script\tests\test_release_target_preflight_files.py script\tests\test_release_preflight_plan.py --basetemp ..\.tmp-r56-preflight-regression2`（从 `IntRuoyiBackend` 根执行）-> PASS，25 passed；此前从应用根执行同一命令因 `ModuleNotFoundError: No module named 'script'` 收集失败，未作为产品缺陷。

## P2/P3 scheduler heartbeat and Jimu target-preflight continuation

- USER_AUTHORIZATION: 用户要求“你来修复”，并明确长期目标是通用、长期可用的按钮发布功能；本轮只修改应用 worktree 的发布 workflow 和 target preflight，不执行正式服、审查服、`mark-tested`、`promote-prod`、`promote-backup`、MinIO 数据同步或全量数据库同步。
- STATUS: R61 本地目录只有 required-sql 与 build-preflight 证据，缺少 `manifest.json` 和镜像包；本机无 `release-20260915-one-button-app-r61` 的发布/Maven/Docker 进程。R61 判定为中断半成品，不复用、不发布。
- BDD: 后台自动推进按钮 workflow -> Given 底层 release operation 已经从 `running` 变为 `succeeded` 或 `failed` / When scheduler 运行且操作者没有手动刷新页面 / Then workflow 必须自动 reconcile 到 READY/FAILED/TEST_DEPLOYED 等真实状态，不能依赖用户轮询触发阶段推进。
- BDD: 长构建日志仍推进不得被 heartbeat 误判 -> Given workflow heartbeat 已超过阈值但底层 operation 仍为 `running` 且 operation log 有新的 mtime / When recovery scheduler 执行 / Then workflow heartbeat 使用日志 mtime 刷新，不取消底层进程；只有日志和 operation 均无可见推进时才 fail-closed recovery。
- RED: `mvn -f IntRuoyiBackend\pom.xml -pl yudao-module-infra "-Dtest=ReleaseWorkflowOrchestratorTest" test` -> FAIL，新增测试要求 `ReleaseWorkflowOrchestrator.reconcileActiveWorkflows(Instant)`，旧实现没有后台自动 reconcile 入口。
- GREEN: scheduler-heartbeat-fix -> PASS。新增 `ReleaseWorkflowRecoveryScheduler`，每 30 秒先 `reconcileActiveWorkflows(now)` 再 `recoverStaleWorkflows(now)`；orchestrator 对终态 operation 自动推进 workflow，对 running operation 用日志最后修改时间刷新 heartbeat，对不可读取日志抛出 `RELEASE_WORKFLOW_OPERATION_LOG_INSPECTION_FAILED`，不吞异常。
- GREEN: `mvn -f IntRuoyiBackend\pom.xml -pl yudao-module-infra "-Dtest=ReleaseWorkflowOrchestratorTest,ReleaseWorkflowRecoverySchedulerTest,ReleaseWorkflowRecoveryTest,ReleaseWorkflowSourceBindingTest,ReleaseWorkflowStoreTest" test` -> PASS，19 tests，0 failures，0 errors。
- BDD: R53 Jimu 目标语义必须前移 -> Given 旧表单模板迁移依赖 `jimu_schema_json` / `recognized_schema_json` 生成 Jimu layout / When target readonly preflight 运行 / Then 必须检查 `sheetLayoutJson` object、rows/cols、待迁移 binding 唯一性、已绑定报表存在性和 release migration 状态，不能只检查表/列存在。
- GREEN: `python -X utf8 -m pytest -q script\tests\test_mes_old_form_template_binding_switch_sql.py script\tests\test_release_target_preflight_files.py --basetemp .tmp-current-jimu-preflight` -> PASS，12 tests。
- NOTE: 本机安全策略拦截了递归删除 `.tmp-r61-jimu-preflight-regression` 的清理命令；提交时仅精确暂存源码、测试和任务记录，临时目录不纳入提交。

## R63 target-preflight syntax regression

- RED: build-release-r63-target-preflight-syntax -> FAIL。维护仓 R63 在 Maven workflow 合同、前端静态合同、`pnpm ts:check`、129 个维护脚本回归、Docker、本地依赖和迁移账本门禁后，执行测试服目标只读 preflight 失败：`TARGET_PREFLIGHT_QUERY_FAILED: read-only target query failed: ERROR 1064 (42000)`；报告 `target-data-preflight.json` 显示 2/17 checks 后停在 `20260829_mes_old_form_template_binding_switch`，R63 仅留下本地 `required-sql/` 半成品，未打包、未上传 NAS、未写测试服。
- ROOT_CAUSE: `20260829_mes_old_form_template_binding_switch.preflight.sql` 第一个 `AND NOT EXISTS (` 少闭合一层括号，后续兄弟级 preflight 子句被错误吞进该子查询，MySQL 在外层 `THEN` 前发现条件表达式未闭合并返回 1064。
- BDD: target-preflight SQL 外层 CASE 结构必须可解析 -> Given 任一 target-preflight 使用 `SELECT CASE WHEN ... THEN ... ELSE ... END` / When 构建前只读门禁扫描 SQL / Then 外层 `THEN` 必须出现在括号深度 0，防止少闭合括号在真实 MySQL 才暴露。
- RED: `python -X utf8 -m pytest -q script/tests/test_release_target_preflight_files.py::test_target_preflights_are_read_only_single_statement_contracts --basetemp .tmp-r63-preflight-parenthesis-red` -> FAIL，新增通用合同捕获 `20260829_mes_old_form_template_binding_switch.preflight.sql must close all condition parentheses before outer THEN`。
- GREEN: target-preflight-parenthesis-fix -> PASS。最小修复仅补齐缺失右括号，并在 `test_release_target_preflight_files.py` 增加通用外层 CASE 括号闭合检查；`python -X utf8 -m pytest -q script/tests/test_release_target_preflight_files.py script/tests/test_mes_old_form_template_binding_switch_sql.py --basetemp .tmp-r63-preflight-parenthesis-green` -> 12 passed。
- GREEN: live-readonly-target-preflight-after-fix -> PASS。测试服只读执行新版单文件 preflight 返回 `TARGET_PREFLIGHT_PASS:20260829_mes_old_form_template_binding_switch`；复用 R63 prospective manifest/build evidence 与新版 checks-root 运行完整 target data preflight 返回 `Target data preflight: passed; 17/17 checks`，输出 `target-data-preflight-after-fix.json`。未修改测试服业务数据。
- RESULT: R63 已判废且不得复用；应用修复提交后，维护仓必须记录新应用 commit 并使用全新 releaseTag 重新执行 build-release -> publish-test。正式服、审查服、`mark-tested`、`promote-prod`、`promote-backup`、MinIO 数据同步和全量数据库复制仍不在授权范围。

## Release button static-review corrective slice（2026-09-16）

- REVIEW: static-code-review -> FAIL。审查根为应用 worktree `8ef8b1a5f0ddbdacd53c2a4bcdb3e94839c98936` 与维护 worktree `a2c73110a90eb7344c6ae5f4eceb1085954426ca`；发现 F1-F8：`app-release` scope 与低层 action 断裂、按钮仍指旧脚本、旧 `/actions` 可绕过 workflow 授权、READY/TEST_DEPLOYED/TESTED 等稳定等待态会被心跳误杀、测试验收后 test lease 泄漏、底层进程先于 workflow 绑定启动、失败阶段显示不真实、前端只能操作排序第一条 workflow。
- STATUS: R80 source freeze 暂停；在按钮链路静态 FAIL 未修复前，不继续测试服 publish-test，不执行正式服、审查服、`mark-tested`、`promote-prod`、`promote-backup`、MinIO 数据同步或全量数据库复制。
- BDD: 统一 app-release 调用合同 -> Given 用户点击“生成程序安装包” / When workflow orchestrator 下发 build-release / Then RuntimeControlService 和 RuntimeControlOperationAction 必须接受且只接受 `app-release`，调用维护仓 `ops/deploy/publish-int-ruoyi.ps1`，传入维护仓根、应用后端根、应用前端根和固定 expected commits，不得转换成 `code-only/with-data` 或调用应用仓旧脚本。
- BDD: 低层发布动作只能来自 workflow 上下文 -> Given 调用方拥有 `infra:runtime-control:operate` / When 直接 POST `/infra/runtime-control/actions` 执行 build/publish/mark/promote 发布动作 / Then 服务端必须因缺少 workflowId/stateVersion/preassignedOperationId 拒绝，不能绕过 productionWriteEnabled、一次性授权和 workflow CAS。
- BDD: 先绑定 workflow 再启动底层进程 -> Given workflow 需要派发 build/test/prod operation / When 底层 RuntimeControlService 开始执行 / Then workflow 必须已持久化 operationId 和执行态；若持久化失败不得启动后台进程，也不得释放可重入 lease。
- BDD: 稳定等待态不参与 heartbeat 超时 -> Given workflow 已处于 READY、TEST_DEPLOYED 或 TESTED 并等待人工下一步 / When 超过默认 15 分钟 / Then recovery scheduler 不得把它标记为 FAILED；heartbeat 只约束有活动底层进程的执行态。
- BDD: 测试验收完成释放 test lease -> Given workflow 已 TEST_DEPLOYED 并执行 mark-release-tested 成功 / When reconcile 推进到 TESTED / Then test 环境 lease 必须释放，后续其它 workflow 可获取 test lease；prod lease 不得覆盖 test lease 引用。
- BDD: 页面显式选择发布工作流 -> Given 存在多个 READY/TESTED/FAILED workflow / When 操作者刷新页面或执行发布/验收/晋级 / Then 前端必须使用用户显式选择的 workflowId，不得固定操作 `releaseWorkflows[0]` 或因 mtime 排序漂移改变目标。
- RED: `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-infra -am "-Dtest=RuntimeControlServiceImplTest,ReleaseWorkflowOrchestratorTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，新增后端合同先暴露缺失字段和断裂合同：`RuntimeControlActionReqVO` 缺少 workflow operation 绑定字段、`RuntimeControlCommand` 缺少 workingDirectory、release workflow 配置缺少维护仓/应用仓根，低层 action 不能绑定 `app-release`。
- RED: `node tests/e2e/runtime-control-one-button-static.spec.cjs` -> FAIL，新增前端静态合同捕获页面仍固定使用 `releaseWorkflows.value[0]`，无法显式选择目标 workflow。
- GREEN: release-button-static-review-fix -> PASS。RuntimeControl 发布动作仅接受 `app-release`，默认脚本和运行时配置统一到维护仓 `ops/deploy/publish-int-ruoyi.ps1`；旧 `/actions` 发布类动作必须携带 `releaseWorkflowId/stateVersion/preassignedOperationId`；orchestrator 在派发底层进程前先持久化 operationId 与状态；READY/TEST_DEPLOYED/TESTED 不参与 heartbeat 超时回收；test/prod lease 按 workflowId+environment 隔离并在 TESTED 后释放；前端新增 workflow selector 并移除旧 `code-only/with-data` scope 文案。
- GREEN: `node tests/e2e/runtime-control-one-button-static.spec.cjs` -> PASS，`runtime-control one-button P1 static contract passed`。
- GREEN: `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-infra -am "-Dtest=RuntimeControlServiceImplTest,ReleaseWorkflowOrchestratorTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，80 tests，0 failures，0 errors。
- GREEN: `corepack pnpm run ts:check` -> PASS，vue-tsc 无错误输出。
- GREEN: `rg -n "script/deploy/publish-int-ruoyi.ps1|code-only|with-data|releaseWorkflows\.value\[0\]" <runtime-control source/test roots>` -> PASS，无命中；旧脚本路径、旧 scope 和排序第一条 workflow 操作已从目标源码清除。
- RESULT: 静态审查 F1/F2/F3/F4/F5/F6/F8 已按通用按钮机制修复并回归；F7 阶段失败显示通过“派发前持久化目标执行态 + 稳定等待态不再误杀”收敛到真实 operation 绑定，但完整发布运行态仍需在新应用提交、新 releaseTag 的 build-release -> publish-test 中继续验证。R80 不复用；下一轮必须使用新应用 commit 和全新 releaseTag。

## Release button latest static-review R1-R5 corrective slice（2026-09-16 17:40）

- USER_REQUEST: 用户提供最新复查结论，当前应用提交 `c3220239de7f06a6c2eabc31c9b840d9a599d768` 仍 FAIL；本轮按复查报告修复 R1-R5，不执行真实发布、服务器写入、数据库写入或 E2E。
- GREEN: experience-preflight -> PASS；命中按钮化 `app-release` workflow 门禁和 deploy action 参数契约，当前两个发布 worktree clean，应用 HEAD 与复查提交一致；稳定状态/租约/恢复/阶段证据缺口必须通过 BDD + RED/GREEN 修复。
- BDD: R1 来源 tuple 去重 -> Given 已存在 READY/TESTED workflow A / When 服务端批准 maintenance/application/frontend commits 或 preset 更新但 sourceSelectionId/reason 相同 / Then “生成程序安装包”必须创建新 workflow B，不能返回旧 releaseTag。
- BDD: R2 超时恢复释放已确认终态 lease -> Given build workflow 心跳超时且底层 operation 已被确认停止 / When recovery 将非写阶段标成 FAILED / Then build lease 必须释放，后续新 build 可获取 build lock；RECOVERY_REQUIRED 仍保留隔离。
- BDD: R3 派发后异常进入隔离 -> Given RuntimeControlService 已启动底层 operation / When 随后 operation 绑定核验或 workflow 状态回读异常 / Then workflow 不得提前 FAILED 或释放 lease，必须保持执行隔离并进入可恢复状态。
- BDD: R4 正式发布只读准入前置 -> Given workflow 已 TESTED 但 productionWriteEnabled=false 或确认文本无效 / When 点击正式发布 / Then 在占用 prod lease、推进 PROD_PREVIEW/PROMOTING_PROD 或消费授权前拒绝，并保留 TESTED。
- BDD: R5 build-release 真实阶段定位 -> Given build-release operation log 已输出 preflight/test/build 阶段标记或失败 / When scheduler/reconcile 运行 / Then workflow 根据真实日志事件推进到 TESTING/BUILDING/READY 或以真实阶段失败，不得在整体 succeeded 后补写虚假阶段。
- RED: `mvn -f D:\IntRuoyiWorktree\r260911-release-button\a\IntRuoyiBackend\pom.xml -pl yudao-module-infra '-Dtest=ReleaseWorkflowOrchestratorTest,ReleaseWorkflowSourceBindingTest' test` -> FAIL，新增/补齐回归暴露 6 个旧行为：同 reason/sourceSelection 复用旧 workflow、stale PREFLIGHTING 失败后 build lease 未释放、派发后 operationId mismatch 被提前 FAILED 并释放 test lease、productionWriteEnabled=false 时 TESTED 包被置 FAILED、运行中阶段标记不推进、失败阶段仍显示 PREFLIGHTING。
- GREEN: `mvn -f D:\IntRuoyiWorktree\r260911-release-button\a\IntRuoyiBackend\pom.xml -pl yudao-module-infra '-Dtest=ReleaseWorkflowOrchestratorTest,ReleaseWorkflowSourceBindingTest' clean test` -> PASS，21 tests，0 failures，0 errors。
- GREEN: `mvn -f D:\IntRuoyiWorktree\r260911-release-button\a\IntRuoyiBackend\pom.xml -pl yudao-module-infra '-Dtest=ReleaseWorkflow*Test,RuntimeControlCanonicalContractTest,RuntimeControlCommandExecutorImplTest,RuntimeControlHighRiskActionContractTest' test` -> PASS，58 tests，0 failures，0 errors。
- GREEN: `corepack pnpm run ts:check` -> PASS，前端当前按钮页类型检查无错误。
- RESULT: R1-R5 已按通用按钮发布机制修复：workflow 去重绑定完整批准来源 tuple/preset/scope；已确认终止的 stale workflow 释放对应 lease；派发后异常进入 `RECOVERY_REQUIRED` 并保留隔离锁；正式发布写开关/PROD 确认在占用 prod lease、消费 grant、推进状态和启动底层进程前只读校验；build-release 阶段由脚本输出的 `RELEASE_WORKFLOW_STAGE=TESTING/BUILDING` 事件驱动，失败阶段取最后真实阶段。未执行真实发布、服务器写入、数据库写入、E2E、正式服或审查服。

## 2026-09-17 Release Button S1-S4 Corrective Validation

- USER_REQUEST: 用户要求继续修复最新静态复查 S1-S4，修完后通过发布按钮执行一次测试服发布并处理发布后问题；本轮授权覆盖本机修复、验证、提交和测试服 `build-release -> publish-test`，不覆盖正式服、审查服、`mark-tested`、`promote-prod`、`promote-backup`、MinIO 数据同步或全量数据库复制。
- WORKTREE: 应用修复继续在 `D:\IntRuoyiWorktree\r260911-release-button\a` / branch `codex/one-button-app-release-20260911`；截至本记录，代码尚未融合到 `E:\IntRuoyi` 的 `int_main`。
- BDD: recovery workflow is read-only -> Given workflow 为 `RECOVERY_REQUIRED` 且 operation log 含 TESTING/BUILDING markers / When 列表或详情 reconcile / Then 直接返回恢复态，不推进阶段、不抛 `RELEASE_WORKFLOW_STAGE_SEQUENCE_INVALID`。
- BDD: canceled write stage retains isolation -> Given `TEST_DEPLOYING` 拥有 test lease / When operation 已确认终止后取消 / Then workflow 进入 `RECOVERY_REQUIRED` 且同环境下一 workflow 获取 test lease 失败。
- BDD: test acceptance branches explicitly -> Given `TEST_DEPLOYED` / When 提交 `PASS` / Then 派发 `mark-release-tested`；When 提交 `FAIL` / Then workflow 进入 `FAILED` 且不派发成功凭证写入。
- BDD: TestedBy is authenticated -> Given `PASS` 验收 / When 服务端派发 `mark-release-tested` / Then `RuntimeControlOperationAction` 显式传入 `-OperatorName <requestedBy>` 和 `-TestResult PASS`，不使用客户端或机器账户兜底。
- GREEN: `mvn.cmd -f D:\IntRuoyiWorktree\r260911-release-button\a\IntRuoyiBackend\pom.xml -pl yudao-module-infra '-Dtest=ReleaseWorkflowOrchestratorTest,ReleaseWorkflowSourceBindingTest,ReleaseWorkflowAuthorizationTest,ReleaseWorkflowRecoveryTest,ReleaseWorkflowStoreTest,RuntimeControlServiceImplTest' '-Dsurefire.failIfNoSpecifiedTests=false' test` -> PASS，102 tests，0 failures/errors/skips。
- GREEN: `node D:\IntRuoyiWorktree\r260911-release-button\a\IntRuoyiFronted\tests\e2e\runtime-control-one-button-static.spec.cjs` -> PASS，确认显式 workflow 选择与 PASS/FAIL 验收 payload 静态合同。
- GREEN: `corepack pnpm run ts:check` from `D:\IntRuoyiWorktree\r260911-release-button\a\IntRuoyiFronted` -> PASS。
- GREEN: `git diff --check` from app worktree -> PASS（仅 CRLF 提示）。
- RESULT: S1-S4 应用侧修复验证通过；R81 判废不复用，下一步提交应用修复并由维护仓用全新 releaseTag 重建后测试服发布。

## 2026-09-17 Direct mark-tested structured result patch

- OBSERVATION: 旧的通用“标记测试通过”操作仍可由授权操作者使用；后端新增显式 `testResult=PASS` 校验后，前端直接操作 payload 也必须携带该字段，不能只修 workflow 验收路径。
- BDD: direct mark-tested operation keeps structured PASS contract -> Given the generic mark-tested operation is available / When the frontend submits or previews it / Then `RuntimeControlActionReqVO` includes `testResult=PASS` and backend forwards authenticated `OperatorName` to the script.
- GREEN: `node D:\IntRuoyiWorktree\r260911-release-button\a\IntRuoyiFronted\tests\e2e\runtime-control-one-button-static.spec.cjs` -> PASS，确认 workflow 验收与直接 mark-tested payload 均有结构化结果。
- GREEN: `corepack pnpm run ts:check` from `D:\IntRuoyiWorktree\r260911-release-button\a\IntRuoyiFronted` -> PASS。
- GREEN: `git diff --check` from app worktree -> PASS（仅 CRLF 提示）。
- RESULT: S1-S4 之外的同源合法入口已补齐，避免后端 PASS 门禁把直接 mark-tested 入口误挡。
