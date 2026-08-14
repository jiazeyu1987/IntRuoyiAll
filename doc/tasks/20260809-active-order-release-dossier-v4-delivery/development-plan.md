# Development Plan

## Task Graph

### 里程碑 1：申请编排严格 RED（A2-RED）

任务编号：A2-RED
原始标题：申请编排严格 RED
目标：新增聚焦测试证明当前 apply 未调用 A3/A4/A5、PQC SUBMITTED 错计 100、hash/事务契约缺失。
依赖节点：[]
涉及文件：后端 release application 聚焦测试。
写入范围：`IntRuoyiBackend/yudao-module-mes/src/test/**/activeorderrelease/**`、`execution-log.md`。
验收编号：AC-05, AC-11, AC-13, AC-14, AC-15
验证步骤：运行聚焦 Maven 测试并得到预期 FAIL。
交付物：RED 因缺三 writer 正式编排等预期原因失败，不因编译错误或错误 fixture 失败。

### 里程碑 2：批记录 writer（A3）

任务编号：A3
原始标题：批记录 writer
目标：小适配复用现有 backfill，在当前 batch/task 生成正式批记录并返回审计/签名证据。
依赖节点：[A2-RED]
涉及文件：team leader backfill、release batch writer、聚焦 JUnit。
写入范围：A3 明确声明的 batch writer/backfill 类和对应测试、`execution-log.md`。
验收编号：AC-03, AC-07, AC-10, AC-13, AC-14, AC-15
验证步骤：writer RED/GREEN、映射/签名/幂等负测、聚焦 Maven test。
交付物：当前 batch/task execution、field audit、source hash、提交/确认签名完整；禁止 formBindings/MAIN。

### 里程碑 3：过程检验单 writer（A4）

任务编号：A4
原始标题：过程检验单 writer
目标：从 CONFIRMED PQC 汇集与 PUBLISHED QA 版本生成传统正式 PROCESS_INSPECTION execution。
依赖节点：[A2-RED]
涉及文件：release inspection writer、QA/PQC reader、映射、聚焦 JUnit。
写入范围：A4 新增 inspection writer/reader/test 类、`execution-log.md`。
验收编号：AC-02, AC-04, AC-08, AC-10, AC-13, AC-14, AC-15
验证步骤：writer RED/GREEN、QA 项目/设备/超限/签名/映射负测。
交付物：传统正式 execution 与当前 batch/task 关联，逐项 field audit 和签名完整，无 raw payload/状态替代。

### 里程碑 4：损耗单 writer 与完成性（A5）

任务编号：A5
原始标题：损耗单 writer 与完成性
目标：对账正式 feedback 和已签名事件损耗明细，生成传统 LOSS_REPORT execution，并提供三资料完成性检查。
依赖节点：[A2-RED]
涉及文件：release loss writer、completeness、聚焦 JUnit。
写入范围：A5 新增 loss/completeness/test 类、`execution-log.md`。
验收编号：AC-03, AC-09, AC-10, AC-11, AC-13, AC-14, AC-15
验证步骤：writer/完成性 RED/GREEN、正损耗对账、零损耗 unsupported、缺映射/签名负测。
交付物：正式 LOSS_REPORT execution/审计/签名完整；任何资料不完整均阻止待办。

### 里程碑 5：申请编排、hash、事务和幂等集成（A2-INTEGRATE）

任务编号：A2-INTEGRATE
原始标题：申请编排、hash、事务和幂等集成
目标：接入 A3/A4/A5，修双 100%、canonical hash、无副作用 plan、原子生成和独立 blocker 持久化。
依赖节点：[A3, A4, A5]
涉及文件：release application service、DTO/mapper/transaction helper、聚焦测试。
写入范围：A2 声明的 orchestration/shared contract 类、申请 DTO/Service/Mapper、对应测试、`execution-log.md`。
验收编号：AC-05, AC-06, AC-11, AC-13, AC-14, AC-15
验证步骤：A2 RED 转 GREEN、writer 调用次序、回滚、双幂等、负责人缺失、静态合同、compile。
交付物：成功三 writer 证据和签名大于 0，资料完整后才提交待办，失败无部分生成物。

### 里程碑 6：前端入口硬化（A1）

任务编号：A1
原始标题：前端入口硬化
目标：对齐 M0 DTO/定位 blocker/状态，保持写成功刷新失败分层，不在前端伪造资料状态。
依赖节点：[A2-INTEGRATE]
涉及文件：team leader API/types/workbench/static spec。
写入范围：`IntRuoyiFronted/src/api/mes/pro/processpool/teamLeader.ts`、相关 static spec、`TeamLeaderWorkbenchPage.vue`、`execution-log.md`。
验收编号：AC-05, AC-11, AC-13, AC-14
验证步骤：前端 static RED/GREEN、`pnpm ts:check`。
交付物：请求仅三字段；定位 blocker 可展示；写成功刷新失败文案正确；不生成后端 ID/成功状态。

### 里程碑 7：fixture manifest 与真实 E2E（A6）

任务编号：A6
原始标题：fixture manifest 与真实 E2E
目标：建任务自有正式 fixture，验证历史可见、自然双 100%、真实申请、三资料和负责人页面处理。
依赖节点：[A1, A2-INTEGRATE]
涉及文件：正式 test fixture、Playwright spec、manifest/evidence。
写入范围：A6 新增的 `src/test`/`tests/e2e` 文件和任务 E2E 证据、`execution-log.md`。
验收编号：AC-01 至 AC-15
验证步骤：fixture self-check、Playwright 全路径、最终只读 API/DB 证据。
交付物：manifest 完整，真实页面和最终只读断言全绿；缺前置时精确阻塞，不记录假 PASS。

## Conflict Rules

- A2 独占申请 service/DTO/mapper/shared orchestration；A3-A5 不修改该 service。
- A3-A5 默认仅新增各自 package/测试；需要共享现有文件时先报告主 Agent，未授权不得写。
- A1 独占指定前端文件；A6 只新增 fixture/E2E，不修改 A1 产品页面。
- `task-state.json` 仅主 Agent 修改；各 Agent 只追加 `execution-log.md`，不得重写其他监督工件。
