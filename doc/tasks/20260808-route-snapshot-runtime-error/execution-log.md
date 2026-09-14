# Execution Log

## User Intent

用户反馈一线 PQC 红框内容需要和 QA 检验项目按产品、工序逐项对应；补充说明部分 QA 检验项目没有设备，“按压式压力泵”的清洗工序有 QA 工序。随后运行态报错：`设备账号上下文不完整或不一致：routeVersion.routeSnapshotJson.routeProcessId=980645，processId=922985`，需要判断错误来源并修复正式链路。

## BDD

BDD: 运行态不再触发旧路线快照裁剪错误 -> Given 当前源码已按正式产品路线返回工序候选，When 一线页面加载包含 `routeProcessId=980645 / processId=922985` 的订单或路线版本，Then 后端不应再抛出旧的 `routeVersion.routeSnapshotJson.routeProcessId=...` 上下文错误。

BDD: PQC 无设备检验项目按正式 QA 项配置为非必填 -> Given QA 检验项目没有任何正式设备绑定行，When 一线 PQC 读取或提交该项目，Then `equipmentRequired` 应为 `false` 且允许空设备快照；有设备绑定行的项目继续要求并校验所选设备。

## Evidence

- Initial source search: current `MesFrontlinePqcContextServiceImpl.java` no longer contains `routeVersion.routeSnapshotJson.routeProcessId` / `parseRouteSnapshotProcesses` / `routeVersionMapper` / `processSnapshotMapper`.
- Root cause 1: 本机运行 Jar 曾是旧构建，未携带前序源码修复，导致仍可能抛出旧 `routeVersion.routeSnapshotJson.routeProcessId` 错误。
- Root cause 2: `selectActiveByWorkOrderAndRoute` 面对同一工单/路线多条 ACTIVE active order 时存在非确定性，修复为按 `joinedAt DESC, id DESC LIMIT 1` 取最新 ACTIVE。
- Root cause 3: PQC 工序列表使用当前 `mes_pro_route_process`，但 active order 的正式身份来自冻结快照；`routeProcessId=980645 / processId=922985` 与当前重建路线工序 ID 漂移，修复为读取 `MesProcessPoolActiveOrderProcessSnapshotMapper.selectListByActiveOrderId`。
- Root cause 4: 真实库中 QA 规程项目设备标志存在数据不一致：activeOrderId=48 的 110 个映射检验项中，46 个没有设备绑定行却被标为 `equipment_required=1`。新增迁移 `IntRuoyiBackend/sql/mysql/20260808_mes_qa_optional_equipment_items.sql`，按正式设备绑定行归一化 `equipment_required`，不插入或删除设备行。
- Runtime evidence: 本机后端运行 `E:\IntRuoyi\output\runtime\int_main\backend-latest-20260808-1524-pqc-snapshot-process-hotfix.jar`，PID 66736，健康检查 `UP`。
- API evidence: 真实登录 `芋道源码/admin` 后调用 `/admin-api/mes/pro/feedback/frontline/device-account/pqc/active-order/processes?workOrderId=980019&routeId=922119` 返回 `code=0`、`activeOrderIds=[48]`、`processCount=14`、命中 `routeProcessId=980645 / processId=922985`，且响应不含旧错误文本。
- Cleaning process evidence: 同一接口返回“清洗工序” `routeProcessId=980647 / processId=922987 / pqcTaskId=240`，包含 4 个 `pqcTaskOptions`，证明用户指出的清洗工序已有 QA/PQC 任务上下文。
- Read-only DB evidence: 版本 41 的“清洗工序” QA 规程有 3 个项目，其中 FIRST/FINAL 两项没有设备绑定行但当前 `equipment_required=1`；按新增迁移只读投影，activeOrderId=48 会有 46 个无设备绑定项改为非必填、64 个有设备绑定项保持必填。

## RED

- `python -X utf8 -m pytest E:\IntRuoyi\IntRuoyiBackend\script\tests\test_mes_qa_optional_equipment_items_sql.py` -> FAIL, expected reason: missing migration `E:\IntRuoyi\IntRuoyiBackend\sql\mysql\20260808_mes_qa_optional_equipment_items.sql`.
- Prior runtime RED evidence: stale JDK21 hotfix Jar failed with `UnsupportedClassVersionError` major 65; old/stale runtime did not prove the current JDK17 deployment.

## GREEN

- `java "@E:\IntRuoyi\IntRuoyiBackend\yudao-module-mes\target-pqc-route-snapshot\junit-console.args"` -> PASS, 36 tests successful, 0 failed.
- `python -X utf8 -m pytest E:\IntRuoyi\IntRuoyiBackend\script\tests\test_mes_qa_optional_equipment_items_sql.py` -> PASS, 3 passed.
- `python -X utf8 E:\IntRuoyi\IntRuoyiBackend\script\release\run-release-migration-policy-gate.py --sql-root E:\IntRuoyi\IntRuoyiBackend\sql\mysql --output E:\IntRuoyi\doc\tasks\20260808-route-snapshot-runtime-error\migration-policy-gate.json` -> PASS, 451 migrations recognized;新增 `20260808_mes_qa_optional_equipment_items` type=data/risk=medium/dependsOn=`20260803_mes_pqc_item_equipment_standard_snapshot`。
- Hotfix packaging evidence: JDK17 class major version 61; nested `BOOT-INF/lib/yudao-module-mes-2026.04-SNAPSHOT.jar` stored uncompressed; Jar SHA256 `2C8BB890FE22A6020F89F86A7BA5BD4C663C3E0239F6CE060A51BDAFD20CD20F`。
- Real API GREEN: health `UP`; target endpoint `code=0`、`activeOrderIds=[48]`、`targetRouteProcessId=980645`、`targetProcessId=922985`、`targetPqcTaskId=232`、`targetPqcTaskOptionCount=4`、`hasOldRouteSnapshotError=false`。
- `git -C E:\IntRuoyi diff --check -- IntRuoyiBackend\sql\mysql\20260808_mes_qa_optional_equipment_items.sql IntRuoyiBackend\script\tests\test_mes_qa_optional_equipment_items_sql.py doc\tasks\20260808-route-snapshot-runtime-error\task.md doc\tasks\20260808-route-snapshot-runtime-error\execution-log.md doc\tasks\20260808-route-snapshot-runtime-error\verification-report.md` -> PASS。

## Blockers

- 本地数据库写入未获明确授权，因此未执行 `20260808_mes_qa_optional_equipment_items.sql`。在执行前，当前运行库/API 仍会显示 46 个“有 QA 项但无设备绑定、却标为设备必填”的历史数据不一致项。
