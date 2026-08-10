# Execution Log

## User Intent

用户反馈一线 PQC 选择“按压式球囊扩张压力泵”仍报错：设备账号上下文不完整或不一致：routeProjectItems routeId=980091，missingItemIds=[14]。用户要求通过代码分析原因，并去除该限制，使其像截图中能看到工艺路线和 QA 检验规程一样可以找到并继续。

后续真实进入路线 `922119` 时再次报错：`routeProjectCode productId=902101，routeProductMasterIds=[924005, 902149, 902101, 901965]，matchedProjectIds=[]`。用户要求继续按既定业务路径修复。

## BDD / TDD

- BDD: 压力泵 PQC 按 DCC 项目代码加载工序 -> Given 订单 productId 已绑定当前有效工艺路线、路线绑定物料代码命中唯一已启用 DCC 项目代码且该项目 productMasterId 有当前路线版本的已发布 QA 规程, When 一线 PQC 选择对应订单并加载工序, Then 返回 QA 规程绑定的路线工序且不要求路线物料 ID 等于 DCC productMasterId。
- BDD: DCC 或 QA 配置缺失时失败 -> Given 路线未唯一命中已启用 DCC 项目代码或该 DCC productMasterId 没有当前路线版本的已发布 QA 规程, When 加载一线 PQC 工序, Then 后端继续明确失败而不返回空成功或默认工序。
- BDD: 路线物料代码解析 DCC 项目 -> Given 订单 productId=902101 已绑定路线 922119、路线绑定项包含 924005 且该 MES 物料代码为 ID、启用 DCC 项目 projectCode=ID 的 productMasterId 对应当前路线版本已发布 QA 规程, When 一线 PQC 加载所选订单工序, Then 通过项目代码命中 DCC 项目并只按其 productMasterId 返回 QA 规程工序，不要求路线物料 ID 等于 DCC productMasterId。
- RED: 一线 PQC 进入路线 922119 -> FAIL, 运行时错误为 `routeProjectCode productId=902101，routeProductMasterIds=[924005, 902149, 902101, 901965]，matchedProjectIds=[]`，证明当前实现错误地用路线物料 ID 直接匹配 DCC productMasterId。
- RED: `java @junit-console-current.args` -> FAIL，40/41 通过；新增路线项目物料 `924005 / ID` 与 DCC `projectCode=ID、productMasterId=3301` 分离场景稳定复现 `matchedProjectIds=[]`。
- RED: 一线 PQC 选择按压式球囊扩张压力泵订单 -> FAIL, 运行时原始错误为 routeProjectItems routeId=980091，missingItemIds=[14]，证明旧逻辑错误依赖 MES MDM 物料解析。
- RED: 隔离 JUnit 首轮执行 MesFrontlinePqcContextServiceTest -> FAIL, 40/41 通过，新增场景暴露旧 productId mock 未参数化；调整测试 helper 后进入 GREEN。
- GREEN: javac @javac-current.args 加当前 MesQaInspectionRegulationItemDO 和 MesFrontlinePqcInspectionItem -> PASS。
- GREEN: java @junit-console-current.args -> PASS，MesFrontlinePqcContextServiceTest 41/41。
- GREEN: `mvn -q -pl yudao-module-mes -Dtest=MesFrontlinePqcContextServiceTest test` -> PASS，exit 0；Surefire 41 tests，0 failures，0 errors，0 skipped。
- GREEN: bug-regression evidence validator -> PASS。
- GREEN: backend API evidence validator -> PASS。

## Progress

- 已读取 bug-regression-fix-loop、backend-api-delivery 技能及项目后端/任务/编码规则。
- 已创建任务目录和任务记录。
- 已确认二次根因：第一阶段把路线物料 ID 当成 DCC productMasterId，路线 `922119` 的 `924005 / ID` 因身份类型不同无法命中 DCC 项目。
- 已改为从路线绑定中读取可解析 MES 物料代码，以物料代码精确匹配唯一启用 DCC `projectCode`，再仅按该项目 `productMasterId` 查询 QA 规程。
- 已覆盖真实路线 ID 集合 `[924005, 902149, 902101, 901965]`；测试证明只有项目代码物料可解析时仍能命中 DCC，不恢复“所有路线项必须存在 MDM”的旧限制。
- 已保留 DCC 项目唯一性、PUBLISHED、路线/版本、工序身份和检验项目完整性校验。
- 已完成隔离编译和 41 条 JUnit 回归。
- 已完成标准 Maven 41 条定向回归，旧的无关编译阻塞已解除。
- 已按 project-experience-consolidation 将“路线物料代码、DCC projectCode、DCC productMasterId 三种身份不可混用”的门禁合并到 `docs/backend-development.md`。
- task-closeout-cleanup preview -> PASS，keep/delete/blocked/warnings 边界符合预期。
- task-closeout-cleanup apply -> PASS，仅删除本任务临时 evidence、编译参数、隔离 class 和 RED 源副本；保留三份正式任务记录及生产回归测试。
- COMMIT BLOCKED: 提交到 int_main 前，`git add` 首先遇到并发 `index.lock`；锁自然消失后确认 index 中仍有 3 个本任务外 `UU` 冲突文件，Git 无法创建任何新提交。
- 未处理、暂存、回滚或覆盖这些并发冲突文件；本任务实现与测试仍保持在 int_main 工作区。
- COMMIT GREEN: 并发任务自行解决 3 个 `UU` 冲突后，复跑 branch runtime port guard -> PASS；使用 `git commit --only` 避开并保留另一个任务已暂存的 3 份记录，只提交本任务服务与测试。
- int_main 实现提交：`c81c8fb2d fix: resolve PQC QA by route project code`。
- 当前状态为 completed。
