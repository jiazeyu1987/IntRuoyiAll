# Execution Log

## User Intent

- 用户要求活跃订单池“生产进度”改为正确进度：分配满一个工序的全部数量，只增加该产品对应工序总数的百分比；例如产品有 10 个工序，分配 1 个工序为 10%，2 个工序为 20%。
- FIFO 自动分配和手动分配给订单都必须更新对应订单生产进度。
- 分配数量允许为 0 或空，空按 0 处理。
- 如果生产组长减少已经分配的工序数量，活跃订单池红框中的生产进度也要同步更新。
- 用户随后明确要求使用本机 `芋道源码/admin` 登录，并允许基于当前数据执行写入型 E2E。

## BDD Scenarios

- BDD: 分配满一个工序只增加一段工序进度 -> Given 某活跃订单产品共有 N 个正式工序且当前仅 0 个工序完成分配 / When 组长通过 FIFO 或手动分配把其中 1 个工序数量分配满 / Then 活跃订单池生产进度显示为 1/N。
- BDD: 分配满多个工序按工序数量累加 -> Given 某活跃订单产品共有 N 个正式工序 / When 组长把其中 2 个工序数量分配满 / Then 活跃订单池生产进度显示为 2/N。
- BDD: 减少已分配工序数量后进度回退 -> Given 某活跃订单已有一个工序曾经分配满并计入生产进度 / When 组长把该工序已分配数量减少到未满 / Then 活跃订单池生产进度扣除该工序对应的 1/N。
- BDD: 空分配数量按 0 参与校验 -> Given 分配弹窗中某行分配数量为空 / When 组长确认分配 / Then 该行按 0 处理且不阻断其它有效分配。
- BDD: 当前数据写入验证后恢复 -> Given 用户授权使用 `芋道源码/admin` 和当前可编辑分配数据 / When Playwright 依次以 0、空值和 FIFO/手动方式保存分配 / Then 生产进度按当前满额工序变化，且最终分配数量和生产进度恢复到测试前值。

## Evidence

- User-visible RED: 用户截图显示分配满单个工序后活跃订单生产进度错误显示为 100%；正确口径应为已满额正式工序数 / 正式工序总数。
- RED evidence gap: 生产修复和回归测试已在本轮开始前被并行 checkpoint 到 `61ba20294`，未保留对其父提交运行新增测试的原始失败输出；不得把后续夹具失败冒充业务 RED。
- Root Cause: 活跃订单列表曾使用不完整的 QA 工序快照作为总工序数，导致历史订单只有 1 条快照时被算成 1/1；列表生产进度还必须读取当前有效分配事实，不能依赖历史完成投影，否则组长减少数量后无法回退。
- Implementation: `MesTeamLeaderActiveOrderServiceImpl` 从当前有效 `MesProcessPoolReportAllocationDO` 按活跃订单和正式工序汇总分配量，以发布路线正式工序身份作为分母；分配量达到工序目标才计为完成，减少到目标以下后自然退出完成集合。
- Implementation: 新加入活跃订单保存发布路线全部正式工序快照，PQC 任务仍只按 QA 工序生成，避免用 QA 工序数量代替生产工序总数。
- Implementation: 前端 `normalizeAllocationSubmitQuantity` 将空值和 0 归一为 0，提交时剔除 0 行，负数和非整数仍失败。
- Integration: `git merge-base --is-ancestor 61ba20294 origin/int_main` -> PASS；生产代码、回归测试和前端 0/空数量行为已进入 `origin/int_main`。
- MAVEN BLOCKED: 标准命令 `mvn.cmd -pl yudao-module-mes clean "-Dtest=MesTeamLeaderActiveOrderServiceTest,MesTeamLeaderActiveOrderManualSortTest,MesTeamLeaderActiveOrderErpPlannedStartTest" test` -> FAIL at unrelated `MesTeamLeaderActiveOrderReleaseProcessInspectionReaderTest` testCompile because `MesQaInspectionRegulationMapper.selectPublishedListByStableProcess(...)` does not exist；未进入 Surefire。
- GREEN: 从 2026-08-10 21:04:46、晚于修复提交的本机运行 Jar 提取完整 MES 模块，显式 javac 编译三个目标测试，再用 JUnit Platform Console 执行 -> PASS，37 tests / 0 failures / 0 errors。
- GREEN: `MesTeamLeaderActiveOrderServiceTest#shouldCalculateProductionProgressFromFormalRouteWhenActiveOrderSnapshotIsIncomplete` -> PASS，10 个正式工序、1 个满额工序返回 10%。
- GREEN: `MesTeamLeaderActiveOrderServiceTest#shouldRecalculateProductionProgressFromCurrentAllocationAfterQuantityReduction` -> PASS，已满额工序减少到未满后只保留另一个满额工序的 10%。
- GREEN: `node tests/e2e/team-leader-allocation-zero-quantity-static.spec.cjs` -> PASS，空值/0 归一和剔除行为锁定。
- E2E: `node doc/tasks/20260810-active-order-progress-allocation/read-only-active-order-progress-e2e.cjs` -> PASS；真实登录 `http://127.0.0.1:8081`，核对 8 条活跃订单的生产进度列与正式接口一致，页面/控制台无错误；当前 7 条订单为 7.1%，对应正式路线 14 道工序中的 1 道完成。
- E2E BLOCKED: 缺少已确认的测试租户、写入账号和可清理任务自有报工/分配数据，因此未执行手动减少已有分配数量的写入型 Playwright；没有使用 admin 基线数据、API-only、SQL 或 mock 冒充通过。
- Test Fixture Correction: 回归场景补齐数据库约束要求的非空 `erpFixedQuantitySnapshot=200`；生产代码继续对缺失正式数量快照 fail-fast，未引入 work order 当前数量 fallback。
- E2E AUTHORIZATION: 用户随后明确要求使用本机 `芋道源码/admin` 和当前数据执行写入型 E2E；范围固定为提交日期 `2026-08-09` 的报工事件 `192`、活跃订单 `35 / CODX-AO5-20260807-01`，测试前数量 `10`、分配模式 `FIFO`、生产进度 `7.142857%`。
- E2E PRECONDITION: `http://127.0.0.1:8081` -> HTTP 200；`http://127.0.0.1:48081/actuator/health` -> `UP`；登录身份精确断言为 `芋道源码/admin`，凭据只从本机环境文件读取且未写入日志。
- E2E HARNESS RED: 首次运行在租户可搜索下拉未输入关键词处超时，业务写请求数为 0；修正为先输入租户再选择。首次业务运行已验证 `10 -> 0 -> FIFO 10`，但多行手动恢复时下拉重渲染导致 harness 失败；FIFO 已恢复原数量。第二次运行在陈旧派生进度比较处失败，异常分支通过真实页面 FIFO 恢复 PASS。两次失败均未留下数量残留。
- E2E GREEN: `node doc/tasks/20260810-active-order-progress-allocation/current-data-write-progress-e2e.cjs` -> PASS；真实页面依次验证显式 0 使进度 `7.142857 -> 0`、FIFO 满额使进度 `0 -> 7.142857`、空值按 0 使进度 `7.142857 -> 0`、手动满额使进度 `0 -> 7.142857`。
- E2E REQUEST EVIDENCE: 显式 0 和空值两轮确认分配请求均不包含目标活跃订单 `35`，证明两者统一归一为 0；FIFO 请求和手动请求均包含 `activeOrderId=35, allocatedQuantity=10`。所有写入均由真实页面触发，API 仅用于只读候选选择和最终状态核对。
- E2E RESTORE: 最终再次通过页面 FIFO 保存，按活跃订单、数量、分配模式、可编辑/放行状态比较与原始快照一致；全部活跃订单生产进度恢复到首次正式重算后的规范基线，`pageErrors=[]`、目标接口错误为空，`restored=true`。
- EXPERIENCE: 使用 `project-experience-consolidation` 将“当前共享数据写入必须恢复源事实并复核派生状态；正式重算不得强制回写陈旧派生值”合并到 `docs/e2e-rules.md`，并在 `docs/experience-index.md` 增加路由。
- CLOSEOUT: `task_closeout.py --mode preview` -> ready，无 blocked/warnings；`task_closeout.py --mode apply` -> applied。仅删除本任务临时脚本、隔离 JUnit 产物和 Playwright 截图目录，保留 `task.md`、`execution-log.md`、`verification-report.md`。

## Current Status

completed - 写入型真实 E2E 已通过，正式分配状态已恢复，临时产物已清理。
