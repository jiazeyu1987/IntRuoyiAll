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
