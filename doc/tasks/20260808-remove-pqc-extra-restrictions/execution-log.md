# Execution Log

## User Intent

用户要求移除一线 PQC 填写链路中的额外限制：不需要系统强制预生成 `FIRST/PATROL_AM/PATROL_PM/FINAL` 任务，不需要终检适用性配置阻塞首检/巡检，不需要设备/选项等非用户要求的提交限制，不需要不合格强制说明，也不需要绑定正式生产提交事件才允许提交。

2026-08-08 追加：用户要求“去除这些限制，只要电子签名签名了，检验数量大于0就可以提交”。本轮验收口径以电子签名密码非空和检验数量大于 0 为唯一提交阻断，提交目标仍来自当前一线 PQC 页面已选任务上下文。

## BDD

- BDD: PQC 按用户选择首检或巡检提交 -> Given 一线 PQC 从生产组长活跃订单池选择订单并选择产品路线中的工序 / When 选择首检或巡检并按 QA 规程填写该工序检验项 / Then 系统按该工序检验项名称展示并按 QA 首检数量或巡检抽样率计算检验数量，不依赖 AM/PM/FINAL 预生成任务。
- BDD: PQC 提交不要求生产提交事件 -> Given 订单、产品、工序和 QA 检验项目均可追溯 / When 一线 PQC 输入电子密码确认提交 / Then 数据写入对应 PQC 组长的 PQC 管理列表，不因缺少正式生产提交事件而阻塞。
- BDD: PQC 非要求字段不阻塞提交 -> Given QA 检验项本身没有正式设备必填或选项必填业务要求 / When 一线 PQC 提交合格或不合格结果 / Then 系统不强制设备、选项或不合格说明，不用默认成功或静默降级掩盖错误。
- BDD: PQC 只按签名和检验数量提交 -> Given 一线 PQC 页面已有当前任务上下文 / When 检验数量大于 0 且输入电子签名密码 / Then 即使缺少生产提交事件、活跃订单强校验、任务身份严格匹配、逐件项目明细、设备选择或不良说明，也允许创建正式 PQC 提交。
- BDD: PQC 检验数量非正仍阻断 -> Given 一线 PQC 页面已有当前任务上下文 / When 检验数量为空、0 或负数 / Then 系统阻止提交并提示检验数量必须大于 0。

## Progress

- 2026-08-08：读取并采用 `backend-api-delivery`、`frontend-feature-delivery`、`bug-regression-fix-loop` 技能。
- 2026-08-08：读取项目触发规则、PQC 相关经验门禁和 UTF-8 写入规则。
- 2026-08-08：创建任务目录 `doc/tasks/20260808-remove-pqc-extra-restrictions/`。
- 2026-08-08：新增 `frontline-pqc-extra-restrictions-removed-static.spec.cjs`，覆盖生产提交事件、FINAL/AM/PM、设备/选项、不合格说明、巡检数量公式和测试表结构。
- 2026-08-08：更新一线 PQC 前端，仅保留 `FIRST`/`PATROL` 切换，按任务快照刷新检验项，检验项 Tab 显示检验项名称，去除生产提交事件选择和无业务要求的设备/不合格说明阻断。
- 2026-08-08：更新后端 PQC 提交流和事件服务，`productionSubmitEventId` 可为空，PQC 事件和记录仍按 PQC 任务、工序、电子签名和幂等键创建。
- 2026-08-08：更新生产组长活跃订单 PQC 任务生成，仅生成一个首检任务和一个巡检任务；巡检数量按 `计划数量 * 抽样率 / 100` 向上取整。
- 2026-08-08：更新测试表结构和迁移脚本，使 `mes_pro_process_pool_pqc_record.production_submit_event_id` 允许为空。
- 2026-08-08：按 `project-experience-consolidation` 规则，将并发 Maven/同模块 `target` 重建导致验证阻塞的经验合并到 `docs/powershell-memory.md`。
- 2026-08-08：运行 `task-closeout-cleanup` preview/apply；仅保留 `task.md`、`execution-log.md`、`verification-report.md`，无删除项、无阻塞。
- 2026-08-08：用户追加更宽松提交口径后重新打开任务；当前需补充 RED/GREEN 覆盖“仅签名 + 检验数量>0”。
- 2026-08-08：前端 PQC 签名前置收敛为 `assertPqcSignatureAndQuantityReady()`，确认提交不再依赖模板预校验、正式生产提交事件、任务快照严格断言或逐件样本完整性断言。
- 2026-08-08：后端 PQC 提交命令收敛为 `pqcTaskId`、`actualInspectionQuantity > 0`、`signaturePassword`，并删除未使用的 `requireProductionSubmitEvent` 旧限制入口。
- 2026-08-08：首次后端定向 Maven 受同模块 `target\classes` 缺失类阻断；运行 `mvn -pl yudao-module-mes -DskipTests compile` 重建主类后，复跑目标 JUnit 到达 Surefire 并通过。
- 2026-08-08：运行 `task-closeout-cleanup` preview/apply，保留三份任务记录，无删除项、无阻塞；任务状态标记 completed。
- 2026-08-08：同步旧 `pqc-requirement-alignment-static.spec.cjs` 为可用任务类型渲染契约，并复跑通过，避免继续要求不可用任务按钮禁用。

- 2026-08-09：按用户要求执行复验；前端静态契约、类型检查、后端目标 JUnit、残留扫描和 whitespace 检查均通过。

## RED/GREEN Evidence

- RED: `node tests/e2e/frontline-pqc-extra-restrictions-removed-static.spec.cjs` -> FAIL，预期原因：前端/后端仍存在 `productionSubmitEventId` 必填、生产提交事件绑定或巡检公式等旧限制。
- RED: `mvn -pl yudao-module-mes "-Dtest=MesTeamLeaderActiveOrderServiceTest,MesProcessPoolEventServiceTest,MesFrontlinePqcContextServiceTest" test` -> FAIL，预期原因：旧后端单元测试仍期待 4 个 PQC 任务（FIRST、PATROL AM、PATROL PM、FINAL），新业务只应生成 FIRST 和 PATROL。
- GREEN: `node tests/e2e/frontline-pqc-extra-restrictions-removed-static.spec.cjs && node tests/e2e/frontline-pqc-final-inspection-switch-static.spec.cjs && node tests/e2e/pqc-item-equipment-standard-method-static.spec.js` -> PASS。
- GREEN: `pnpm ts:check` -> PASS。
- GREEN: 残留扫描 `rg` -> PASS，前端未命中 FINAL/终检、生产提交事件选择、设备强制、不合格说明强制等残留；后端未命中生产提交事件强制、终检阻断、AM/PM/FINAL 任务常量等残留。
- GREEN: `git diff --check` -> PASS，无 whitespace error；仅输出工作区 CRLF 提示。
- GREEN: `mvn -pl yudao-module-mes -DskipTests compile` -> PASS，MES 主代码编译通过。
- GREEN: `mvn -pl yudao-module-mes "-Dtest=MesFrontlinePqcContextServiceTest#shouldSubmitPqcInspectionWithOnlySignatureAndPositiveQuantity" test` -> PASS，1 个用例通过，覆盖仅签名和正数检验数量即可提交。
- GREEN: `node tests/e2e/pqc-requirement-alignment-static.spec.cjs` -> PASS，旧任务类型 tab 契约已同步为只渲染可用正式任务类型。

- GREEN: 2026-08-09 复验 `frontline-pqc-extra-restrictions-removed-static.spec.cjs`、`pqc-requirement-alignment-static.spec.cjs`、`frontline-pqc-final-inspection-switch-static.spec.cjs`、`pqc-item-equipment-standard-method-static.spec.js`、`pqc-inspection-tabs-layout-static.spec.js`、`pnpm ts:check`、目标 Maven JUnit、残留扫描和 `git diff --check` -> PASS。

## Blockers

- 无当前阻塞。
