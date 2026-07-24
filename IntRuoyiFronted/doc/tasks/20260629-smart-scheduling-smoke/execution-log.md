# Execution Log：排产冒烟测试与阻塞项盘点

BDD: 排产静态契约可通过 -> Given 现有排产冒烟脚本与页面契约存在 / When 运行静态校验 / Then 先确认脚本依赖和关键页面片段未被破坏。
BDD: 排产真实链路暴露真实阻塞 -> Given 测试租户可登录本机前端 / When 执行真实排产冒烟脚本 / Then 输出脚本成功证据或首个真实阻塞点。
GREEN: experience-preflight -> PASS
RED: `node tests/e2e/smart-scheduling-smoke-real-flow-static.spec.js` -> FAIL, 初始静态门禁有多处按源码单行文本匹配的断言，真实逻辑存在但因换行格式变化被误判。
RED: `node ..\scripts\preflight\login-preflight.mjs --base-url http://localhost:8081 --tenant 测试租户 --username aoteman --password 111111 --target-path /mes/pro/task --target-text 生产排产` -> FAIL, 首次执行因后端未启动导致 `/system/auth/login` 响应超时。
GREEN: `D:\ProjectPackage\Int\IntRuoyi\restart-ruoyi-backend.bat` -> PASS，修复 `ShowroomProductCoverImageService` 注入构造器后本机后端恢复。
GREEN: `node ..\scripts\preflight\login-preflight.mjs --base-url http://localhost:8081 --tenant 测试租户 --username aoteman --password 111111 --target-path /mes/pro/task --target-text 生产排产` -> PASS。
GREEN: `node tests/e2e/smart-scheduling-smoke-real-flow-static.spec.js` -> PASS，静态门禁已调整为稳定的行为匹配。
BLOCKER: missing-mes-smoke-env -> 当前完整真实冒烟所需 `MES_SMOKE_*` 参数缺失，脚本不能继续执行。
GREEN: `MES_SMOKE_*=... node tests/e2e/smart-scheduling-smoke-real-flow.e2e.js` -> PASS/FAIL，已用真实测试租户账号、真实产品 `AW.106.03.08.1007`、真实单位 `PCS` 和真实路线 `ROUTE-XLSX-00001` 启动完整排产冒烟，链路成功走到 `create ERP work order -> admit schedule order -> open /mes/pro/task`。
RED: 同命令 -> FAIL，`showroomsupervisor` 在 `生产排产` 页找不到可见“自动排产”按钮，脚本卡在 `open auto schedule drawer`。
GREEN: root-cause-confirmed -> PASS，真实截图、权限快照和数据库核验一致表明：测试租户套餐与角色菜单都包含 `5542/900180/900181/900182`，但当前页面把“自动排产”入口错误绑定到 `mes:pro-task:create`；真实排产账号仅具备 `mes:pro-auto-schedule:preview/apply/replan`，因此按钮被误隐藏。
GREEN: local-ui-auto-schedule-entry-fix -> PASS，已把 `生产排产` 页自动排产入口改绑到 `mes:pro-auto-schedule:preview`，静态门禁同步覆盖。
GREEN: smoke-positive-boundary-scope-fix -> PASS，已把正向烟测收口到首个真实串行边界工序，避免一次性导入整条工艺链带来的时序误判。
GREEN: feedback-permission-gap-confirmed -> PASS，测试租户真实缺失 `mes:pro-feedback:create/update/delete/export/approve` 时，“第三方导入”不可见；补齐套餐后真实恢复。
GREEN: user-query-permission-gap-confirmed -> PASS，测试租户真实缺失 `system:user:query` 时，待归属页人员选择弹窗无法查询 `aoteman / eDHR矩阵-审批人`；补齐套餐 `100/1001` 后真实恢复。
GREEN: smoke-batch-context-fix -> PASS，已删除归属/字段补录阶段会清空导入批次上下文的页面重载，恢复“确认报工”可用前置。
GREEN: `node tests/e2e/smart-scheduling-smoke-real-flow-static.spec.js` -> PASS，最新静态门禁通过。
GREEN: `MES_SMOKE_*=... SMART-SCHED-20260629-LOCAL-FULL-13 node tests/e2e/smart-scheduling-smoke-real-flow.e2e.js` -> PASS，真实前端链路已完整通过 `ERP建单 -> MES同步 -> 排产入池 -> 自动排产 -> 日历 -> 第三方导入 -> 归属 -> 确认报工 -> 非审批人校验 -> 审批 -> 进度回写`。
GREEN: `node ..\scripts\preflight\login-preflight.mjs --base-url http://localhost:8081 --tenant 测试租户 --username smokeplan1 --password 111111 --target-path /mes/pro/scheduler-workbench --target-text 生产排产` -> PASS，当前排产员真实登录前置可用。
GREEN: `POST /admin-api/mes/pro/auto-schedule/preview scheduleOrderIds=[78]` -> PASS，当前唯一真实阻塞为 `MATERIAL_DEMAND/BLOCKING -> 工单缺少生产用料清单`。
BLOCKER: `GET /admin-api/erp/production-material-list/page?pageNo=1&pageSize=20&productionOrderNo=SMART-SCHED-20260630-RERUN2-MO` -> 返回 `total=0`，说明这张新 ERP 工单在前端可见链路里仍无生产用料清单可供排产使用。
RED: `node tests/e2e/smart-scheduling-smoke-real-flow-static.spec.js` -> FAIL，新增门禁要求脚本在排产前触发并等待生产用料清单同步，但原脚本只等待 `kingdeeProductionOrderSyncJob`。
GREEN: `node tests/e2e/smart-scheduling-smoke-real-flow-static.spec.js` -> PASS，已补 `triggerProductionMaterialListSync`、`waitForProductionMaterialListSync` 与 `/erp/production-material-list/page` 查询门禁。
GREEN: `tenant122-material-master-backfill-20260630` -> PASS，根仓侧已按用户授权仅向测试租户补齐 12 条本地物料并回填当前工单生产用料清单关联；前端后续无需再把该类“子项未映射本地物料”误判为页面阻塞。
GREEN: `POST /admin-api/mes/pro/auto-schedule/preview scheduleOrderIds=[79]` -> PASS，`SMART-SCHED-20260630-RERUN4` 已验证 `blockingIssueCount=0`、`generatedTaskCount=24`、`shortageCount=12`，前端 smoke 当前可以把物料短缺视为 warning 而非 blocking。
RED: `MES_SMOKE_*=... SMART-SCHED-20260630-RERUN5 node tests/e2e/smart-scheduling-smoke-real-flow.e2e.js` -> FAIL，新的首个真实失败点前移到 ERP 建单：`/admin-api/erp/kingdee-sync/production-order/create` 返回 `明细第1行的单位值为:【100】不存在`。
GREEN: `output/smart-scheduling-smoke/**/config.json erpUnitNumber check` -> PASS，已核对历史成功配置均使用 `erpUnitNumber=PCS`；因此下一轮前端 smoke 应恢复 `MES_SMOKE_ERP_UNIT_NUMBER=PCS` 后再继续追踪新的真实阻塞。
RED: `MES_SMOKE_*=... SMART-SCHED-20260630-RERUN5/RERUN6/RERUN7/RERUN8/RERUN9 node tests/e2e/smart-scheduling-smoke-real-flow.e2e.js` -> FAIL，修正 ERP 单位编码后，真实阻塞前移为月历接口会被测试租户历史坏排程单污染；先后暴露“缺少生产用料清单”和“生产用料清单子项未映射本地物料”的旧单。
GREEN: `tenant122-july-bad-schedule-orders-cleanup-20260630` -> PASS，根仓侧已按用户授权仅清理测试租户历史坏排程单状态，不改 admin。
GREEN: `node tests/e2e/smart-scheduling-smoke-real-flow-static.spec.js` -> PASS，前端 smoke 日历校验已切到“本次发布任务所在月份”。
GREEN: `login-preflight smoke-role-audit-20260630` -> PASS，当前测试租户真实可用账号应为 `aoteman / smokeplan1 / smokeappr1 / smokeread1`；旧脚本账号 `smokesup1 / smokenon1` 已不再适配当前租户。
GREEN: `MES_SMOKE_*=... SMART-SCHED-20260630-RERUN11 node tests/e2e/smart-scheduling-smoke-real-flow.e2e.js` -> PASS，前端真实链路再次完整通过并产出 `D:\ProjectPackage\Int\IntRuoyi\output\smart-scheduling-smoke\SMART-SCHED-20260630-RERUN11`。
