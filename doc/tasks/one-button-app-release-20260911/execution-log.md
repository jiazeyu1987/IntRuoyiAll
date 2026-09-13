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
