# Execution Log

BDD: 跨生产工序的同一 QA 项目任务分别展示 -> Given 同一活跃订单、QA 版本、QA 工序、检验项目和巡检规则在两道冻结生产工序各有一条待检任务，When 一线 PQC 加载工序任务选项，Then 两条任务按各自 routeProcessId 和 processId 精确匹配，不报重复身份。

BDD: 跨日期班次轮次的任务分别展示 -> Given 同一生产工序的同一 QA 项目存在不同业务日期、班次或轮次任务，When 构建覆盖结果，Then 每条任务按完整时序身份匹配。

BDD: 完整身份重复继续阻断 -> Given 两条待检任务的全部正式身份字段完全相同，When 构建覆盖结果，Then 后端明确抛出重复身份异常，不任取第一条。

BDD: 通过正式路线配置生成组合规程测试订单 -> Given 最新路线版本 744 缺少生产工序配置快照且用户授权修复，When 管理员通过工艺路线页面补齐配置并发布有效版本、再从生产组长页面复制测试单，Then 新测试订单冻结产品 QA 与已绑定通用规程套并可在一线 PQC 同时查看。

BDD: E2E 前端可编译 -> Given 文控大版本创建接口要求幂等键，When 执行前端 TypeScript 检查，Then 页面请求参数包含唯一幂等键且全量类型检查通过。

BDD: 历史参数按现行类型契约迁移 -> Given 历史正式 `INTEGER` 参数保留了已不适用的 `decimalScale`，When 管理员显式迁移生产配置，Then 保留参数业务值并清除类型不兼容字段，候选快照通过现行校验。

BDD: 工单按 MDM 产品主档组合通用规程 -> Given 工单保存 MES 物料 ID 且该物料关联已绑定通用规程套的 MDM 产品主档 ID，When 创建或复制活跃订单，Then 后端按 MDM 产品主档 ID 读取绑定并同时冻结产品 QA 与通用规程任务。

BDD: 多 Word 通用规程按套校验 -> Given A 套两份成员 Word 分别提供首检项目和巡检项目，When 创建活跃订单组合通用规程，Then 按套版本整体校验必需检验类型并为两份成员都生成任务；任一必需类型在整套都缺失时仍明确阻断。

BDD: 一线按各规程自身 DCC 回放组合快照 -> Given 产品 QA 与通用规程成员各自属于不同 DCC 项目，且活跃订单已冻结两类任务，When 一线 PQC 加载工序，Then 产品 QA 校验产品 DCC，通用规程校验各自规程 DCC，并同时返回两类来源。

BDD: 检验设备按规程来源查询 -> Given 产品 QA 与通用规程成员分别维护自己的检验项目设备配置，When 一线 PQC 组合显示检验项目，Then 每个来源使用自身 DCC 项目和版本查询设备选项，不用产品 DCC 读取通用版本。

BDD: 通用检验任务按来源 DCC 提交 -> Given 一线 PQC 已打开通用规程成员任务并完成检验结果，When 电子签名提交，Then 后端按任务所属规程 DCC 与版本校验检验项目和设备配置并保存提交，不使用产品 QA DCC。

BDD: 多检验项目签名按任务隔离 -> Given 同一通用工序一次提交包含多个 item-scoped PQC 任务，When 后端依次记录每条任务的电子签名，Then 每个签名主题和幂等键包含各自 `pqcTaskId`，不因同一员工和动作码冲突。

## Evidence

- REPRO: `output/runtime/int_main/logs/yudao-server.log` -> `activeOrderId=1009200075, regulationVersionId=70, qaProcessId=166, qaItemCode=PQC-IDI-001-I001, inspectionRuleKey=PATROL_AM` 被 `MesFrontlinePqcTaskOverlay.fromExpectedTask` 判定重复。
- AUTHORIZATION: 用户明确要求真实 E2E，随后明确授权重启 `int_main` 后端。
- RED: `pnpm ts:check` -> FAIL，`controlled-file/browser/index.vue` 创建大版本请求缺少必填 `idempotencyKey`。
- RED: Playwright 真实页面执行 `工艺流程 -> RT000028-IDI -> 版本 -> 迁移现有生产配置`，自然请求返回 `1040271035`；后端停在 `validateProductionParameterRule`，只读核验定位两条历史 `INTEGER` 参数残留 `decimalScale=1`。
- RED: Playwright 真实页面生成活跃订单 `1009200079`（路线 `V16`）后，一线 PQC 仅显示 6 道产品 QA 工序；只读核验显示工单 `product_id=924008` 对应 MDM `product_master_id=14`，绑定表使用 `14`，但组合逻辑错误地用 `924008` 查询绑定。
- RED: 修复产品主档查询后，Playwright 再次从正式页面复制测试单，返回 `1040506111`：A 套成员版本 `72` 只有巡检项目，却被按单份文档要求首检和巡检都齐全；实际 A 套版本由版本 `71` 的首检和版本 `72` 的巡检共同组成。
- RED: Playwright 成功创建活跃订单 `1009200080` 后，一线 PQC 工序请求返回 `1040760103 lockedQaAggregate`；通用规程版本使用自身 DCC 项目，读取校验却错误要求等于产品 QA 的 DCC 项目。
- RED: 修复组合快照 DCC 校验后，Playwright 复验同一订单继续返回 `itemEquipmentConfig.dccProjectCodeId=129`；检验设备查询仍把产品 DCC 传给通用规程版本。
- RED: Playwright 在 `1009200080` 的“大中包装过程检验规程（通用包装）”完成电子签名提交，自然提交请求返回 `1040506114 itemEquipmentConfig.dccProjectCodeId=129`；显示链路已正确，提交项目解析仍使用产品 DCC。
- RED: 修复提交 DCC 后，Playwright 同一次三项目提交流程中任务 `1009244773` 成功并生成事件 `32629`，下一任务因复用相同电子签名幂等键返回 `1047000004 电子签名幂等键已被不同内容使用`。
- ROOT CAUSE: `ExpectedTaskIdentity` 与匹配过滤器遗漏 `routeProcessId/processId`，且已有 `businessDate/shiftCode/roundNo` 未进入匹配条件。
- RED: 新增跨生产工序、跨日期班次轮次和完整重复场景后，旧匹配身份无法区分合法任务。
- GREEN: `mvn --% -f IntRuoyiBackend/pom.xml -pl yudao-module-mes -am -Dtest=MesFrontlinePqcTaskOverlayTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS，7 tests。
- RED: `node yudao-module-mes/src/test/js/mes-frontline-pqc-overlay-complete-identity-static.spec.cjs --baseline` -> FAIL，基线缺少 `Long routeProcessId`。
- GREEN: `node yudao-module-mes/src/test/js/mes-frontline-pqc-overlay-complete-identity-static.spec.cjs` -> PASS。
- REGRESSION: `mvn --% -f pom.xml -pl yudao-module-mes -Dtest=MesFrontlinePqcTaskOverlayTest,MesFrontlinePqcContextServiceTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS，21 tests。
- GREEN: `mvn.cmd -q -pl yudao-module-mes -am "-Dtest=MesFrontlinePqcTaskOverlayTest,MesFrontlinePqcContextServiceTest,MesFrontlinePqcSubmissionConcurrencyTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，26 tests，0 failures，0 errors。
- GREEN: `node yudao-module-mes\src\test\js\mes-frontline-pqc-overlay-complete-identity-static.spec.cjs` -> PASS。
- GREEN: `pnpm ts:check` -> PASS。
- GREEN: `git diff-files --check -- IntRuoyiBackend IntRuoyiFronted docs doc` -> PASS，仅换行提示。
- GREEN: backend API evidence validator -> PASS。
- GREEN: bug regression evidence validator -> PASS。
- GREEN: 重启前完整 31 模块后端打包 -> PASS。
- GREEN: `restart-int-ruoyi-local.ps1 -Component backend -WorktreeName int_main` -> PASS；新运行 Jar 为 `backend-runtime-control-20260911-160457.jar`，PID 27800，health `UP`。
- GREEN: 运行 Jar 内嵌 MES 模块 SHA-256 与当前 `yudao-module-mes` 构建产物一致。
- GREEN: backend API evidence validator -> PASS。
- GREEN: bug regression evidence validator -> PASS。

## Completed Work

- 覆盖身份增加 `routeProcessId/processId`。
- 匹配过滤增加生产工序、业务日期、班次和轮次精确条件。
- 重复异常补充完整任务身份。
- PQC 上下文构建覆盖身份时透传任务生产工序字段。

## Remaining Blockers

- E2E PARTIAL PASS: 真实前端登录 `芋道源码/admin`，进入一线 PQC，目标活跃订单 `1009200075` 的工序请求业务码为 `0`；原重复覆盖身份系统异常已消失，页面可见 6 个产品 QA 工序及其检验项目、标准和方法。
- E2E BLOCKED: 目标订单创建于产品绑定通用规程套之前，按冻结快照规则不包含通用工序；页面自然请求也只返回 `PRODUCT_QA` 来源。
- E2E FIXTURE BLOCKED: 从生产组长真实页面对正式基线订单点击“复制测试单 -> 确认复制”，自然 POST 返回业务码 `1040271035`：最新工艺路线候选版本快照不完整，`routeVersionId=744`；事务失败且未创建测试订单。
- SHARED LAYOUT ISSUE: 浏览器有 2 条与目标 MES 链路无关的 DCC 待办角标错误，后台首个数据库异常为 `submit_idempotency_key` 缺列；按共享布局归因规则单独记录，不冒充一线 PQC 错误。
- BLOCKER: 要完成“两类流程同时可见”的 E2E，需先修复并发布 routeVersionId=744 的生产工序配置快照，或由用户提供一个绑定后生成且已冻结通用规程的活跃订单。该操作会修改共享路线配置，需新增明确授权。
- AUTHORIZATION: 用户已明确授权通过正式前端修复上述共享路线配置并继续 E2E。
- ROUTE UI REPRO: 真实前端进入 `工艺流程 -> RT000028-IDI -> 版本`，当前 ACTIVE 为 `V14 / #744`；点击“创建候选版本”后页面明确提示 `工艺路线候选版本快照不完整，routeVersionId=744`。
- ROUTE WORKFLOW BLOCKER: V14 是生产配置版本化前的历史快照；候选创建要求来源 ACTIVE 已具有完整 `productionProcessConfigs`，但现有页面只允许在候选中维护生产配置，形成无前端可解除的闭环阻塞。
- DATA SAFETY: 页面默认草稿对每道工序给出 `overagePercent=null`、空损耗、空设备和空参数；直接补 0 或空数组会猜测并覆盖共享生产配置，因此未继续写入或发布。
- REQUIRED CHANGE: 需要新增显式“从现有正式配置迁移到候选版本”的产品入口，结构化保留超量比例、损耗、设备和参数；这属于新的迁移功能，不是覆盖身份修复的隐式 fallback。
- 本轮未执行 Git 提交或推送。

## 2026-09-12 Combination E2E Completion

- GREEN: `node src/test/js/route-production-config-migration-entry-static.spec.cjs` -> PASS。
- GREEN: `MesProRouteVersionWorkflowServiceTest` -> 22 tests, 0 failures, 0 errors。
- GREEN: Playwright 真实页面迁移路线生产配置并发布 `RT000028-IDI / V16 / routeVersionId=746`；历史 `INTEGER` 参数无效小数位字段按现行类型契约规范化，正式业务值保留。
- GREEN: `MesTeamLeaderActiveOrderServiceTest#shouldResolveCommonRegulationBindingByMdmProductMasterId` -> PASS；同时覆盖两份成员分别提供 FIRST/PATROL 和整套缺必需类型 fail-fast。
- GREEN: `MesFrontlinePqcContextServiceTest` -> 15 tests；`MesFrontlinePqcSubmissionConcurrencyTest` -> 5 tests；`MesProBatchRecordExecutionSignatureServiceTest` -> 16 tests，全部 0 failures / 0 errors。
- GREEN: `node src/test/js/mes-common-qa-frontline-pqc-static.spec.cjs` -> PASS。
- GREEN: `pnpm ts:check` -> PASS；文控大版本请求幂等键静态回归 -> PASS。
- GREEN: 标准 `restart-int-ruoyi-local.ps1 -Component backend -WorktreeName int_main` -> 31 模块 `BUILD SUCCESS`；最终 48081 health=`UP`。
- GREEN: Playwright 真实前端生成测试活跃订单 `1009200080`，路线 `V16`；一线 PQC 工序选择器显示 6 道产品 QA 工序与 2 道通用包装工序：`初包装过程检验规程`、`大中包装过程检验规程`。
- GREEN: Playwright 分别打开两道通用工序；初包装显示外观、密封强度、密封泄漏等项目，大中包装显示外观、标签/说明书/合格证、包装标识及正式标准、方法和抽样信息。
- GREEN: Playwright 对大中包装执行真实电子签名提交；任务 `1009244773/1009244849/1009244774/1009244775` 均返回业务码 `0`，生成事件 `32629..32632` 和互异签名 `35..38`。
- GREEN: 清理前只读数据库终核 -> 上述 4 条任务均为 `SUBMITTED` 且分别绑定对应事件；版本 72 / QA 工序 175 总任务 90、待提交 86、已提交 4，进度分母包含通用规程任务。
- GREEN: `git diff --check -- <task scope>` -> PASS，仅 CRLF 归一化提示。
- GREEN: Playwright 通过生产组长页面“清理测试单”依次清理任务自有模拟订单 `1009200080`、`1009200079`；两次请求均由后端完成，前端刷新后活跃订单池不再显示这两个订单。
- GREEN: 只读数据库终核 -> 活跃订单 `1009200079/1009200080` 与工单 `1009205475/1009205477` 的 `deleted=1`，未直接通过 API 或数据库承担清理动作。
- NOTE: 本轮未执行 Git 提交或推送。
- GREEN: task-closeout-cleanup preview/apply -> PASS；首次删除 2 份技能临时证据与任务 Playwright 输出目录，测试订单清理后最终复跑保持 `delete/blocked/warnings=0`，保留 `task.md`、`execution-log.md`、`verification-report.md` 和正式回归测试。
