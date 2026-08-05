# Execution Log

## User Intent

- 用户需要一个“ERP表格自动同步”功能，放在个人工作台的配置页签下。
- 功能需要支持选择每天几点自动同步，以及选择哪些 ERP 表格进行数据同步。
- 用户要求先修改，并提醒当前有其他 Codex CLI 正在修改错误，需避免冲突。

## Environment And Isolation

- 主工作区：`E:\IntRuoyi`，当前存在并行脏改动，本任务不直接修改该工作区。
- 隔离 worktree：原路径 `D:\IntRuoyiWorktree\profile-erp-table-auto-sync` 在验证期间两次被外部进程删除，当前验证路径为 `D:\IntRuoyiWorktree\profile-erp-table-auto-sync-verify`。
- 分支：`codex/profile-erp-table-auto-sync`。
- 运行槽位：原登记 `slot=5`，前端端口 `8086`，后端端口 `48086`；当前未启动服务，仅做代码和静态/合同验证。
- 备注：首次 worktree 在验证后被外部进程移除，已在同一路径重新挂载同一分支并继续。

## BDD Scenarios

BDD: 配置 ERP 表格自动同步 -> Given 用户有权限进入个人工作台配置页签，When 打开“ERP表格自动同步”并选择执行时间和 ERP 表格后保存，Then 系统保存启用状态、执行时间、同步类型列表，并在重新打开页面时按正式配置回显。

BDD: 只同步选中的 ERP 表格 -> Given 自动同步配置已启用且选择了部分 ERP 同步类型，When 调度任务到达配置时间执行，Then 系统只触发被选中的正式 JobHandler，并记录每类同步的运行结果。

BDD: 配置页权限边界 -> Given 用户不能查看个人工作台配置页签，When 尝试访问“ERP表格自动同步”配置接口或页面入口，Then 系统按既有权限模型拒绝访问，不返回默认成功配置。

BDD: 同步类型正式来源 -> Given 系统存在正式 ERP/Kingdee 同步类型枚举，When 前端加载可选 ERP 表格，Then 只能展示后端正式支持的同步类型，不能混用 NAS 导出类型或前端硬编码兜底。

## TDD Evidence

- RED: node IntRuoyiFronted\tests\e2e\profile-erp-table-auto-sync-static.spec.js -> FAIL, expected reason: `ProfileErpTableAutoSyncSetting.vue` 尚未实现。
- RED: python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_erp_kingdee_table_auto_sync_sql.py -> FAIL, expected reason: `20260805_erp_kingdee_table_auto_sync.sql` 尚未实现。
- RED: mvn -pl yudao-module-erp "-Dtest=cn.iocoder.yudao.module.erp.kingdeeautosync.ErpKingdeeTableAutoSyncContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL, non-target reason: 未加 `-am` 时 ERP 模块拿到旧 infra reactor 依赖，`NasBrowserService.writeFile(...)` 编译符号缺失。
- RED: mvn -pl yudao-module-erp -am "-Dtest=cn.iocoder.yudao.module.erp.kingdeeautosync.ErpKingdeeTableAutoSyncContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL, expected reason: Surefire 到达目标测试，新 controller/service/job/type enum 文件尚未实现。
- GREEN: pending -> 实现后运行定向测试并记录 PASS。

## Milestone Updates

- M1 completed：确认正式同步类型为 PRODUCT、STOCK、PURCHASE_ORDER、SALE_ORDER、PRODUCTION_ORDER、PRODUCTION_MATERIAL_LIST、BOM；NAS 自动同步是导出到 NAS，不作为 ERP 拉取同步来源。
- M2 completed：已补充 BDD/TDD/E2E/test-data 设计文档，并完成 RED 证据采集。
- M4 correction：修正自动调度失败重试语义，`lastAutoRunDate` 只在全部所选 ERP JobHandler 成功后写入，避免失败当天被误判为“已自动执行”。

## Verification Notes

- RED: mvn -pl yudao-module-erp -am "-Dtest=cn.iocoder.yudao.module.erp.kingdeeautosync.ErpKingdeeTableAutoSyncContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL, expected reason: 新增 `markAutoRunDateAfterSuccess` 合同后，旧实现缺少成功后写入方法。
- RED: mvn -pl yudao-module-erp -am "-Dtest=cn.iocoder.yudao.module.erp.kingdeeautosync.ErpKingdeeTableAutoSyncContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL, expected reason: 首次断言过宽，误拦截成功后写入 `lastAutoRunDate`，已收窄为顺序断言。
- BLOCKER: 原 worktree `D:\IntRuoyiWorktree\profile-erp-table-auto-sync` 在 Maven 复验后被外部进程删除；随后切换到 `D:\IntRuoyiWorktree\profile-erp-table-auto-sync-verify` 继续。
