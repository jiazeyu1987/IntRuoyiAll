# Execution Log

## 用户意图

- 一线生产需要支持同一设备由不同人员、不同工序反复正式提交。
- 每次正式提交成功后，页面要恢复到可继续填写的原始状态。
- 追加验收：使用真实页面，在不刷新页面的前提下连续正式提交 4 次。

## BDD

- BDD: 成功提交后开始下一次独立报工 -> Given 设备账号已选择工序、实际员工并完成本次生产数据填写 / When 正式提交接口明确返回成功 / Then 系统保留本次正式事实，页面清空本次数量、损耗和设备参数，恢复“正式提交”状态，并为下一次报工生成新的幂等键，操作员可以切换另一工序和另一实际员工继续提交。
- BDD: 提交失败时保留当前填写会话 -> Given 操作员已完成本次填写并确认电子签名 / When 正式提交接口失败或未明确返回成功 / Then 页面保留当前输入和当前幂等键，不显示成功、不自动开始下一次报工，操作员可在确认错误后以同一幂等语义重试。
- BDD: 同一页面连续提交四次 -> Given 已确认的本机测试租户、设备账号、工序、实际员工和签名数据可用 / When Playwright 在同一页面中填写并正式提交四轮且期间不刷新、不重新导航 / Then 四次正式提交请求均成功、四组回执身份互不重复，并且每次成功后页面清空本轮输入并恢复下一轮提交能力。
- BDD: 同页切换不同员工和不同工序连续提交 -> Given 同一设备账号可从正式运行态选择至少两名有唯一生产组长归属且签名可用的员工和至少两道正式工序 / When Playwright 在同一页面四轮交替选择员工与工序并分别签名提交 / Then 页面不刷新不重新导航，四组正式事实的实际员工、签名主体、路线工序和 MES 工序逐轮匹配页面选择，每轮成功后继续恢复选择与提交能力。

## Command Intent

- 只读检查产品、验收、前端实现、相邻任务和经验门禁，定位现有永久锁定状态及幂等键生命周期。
- 记录需求变更决策后，再进入前端 BDD/TDD 实现。
- 使用 `project-experience-consolidation` 将连续独立提交的会话级幂等规则合并到已有前端写入门禁和一线生产正式提交门禁；未新建长期经验文档。
- 复用本机 `测试租户` 中旧正式提交流程保留的可追踪 fixture，通过真实登录、生产填写、签名确认和正式提交 UI 连续执行四轮；密码只从前端本地环境文件注入，不写入脚本、结果或任务证据。

## Milestone Updates

- M1 完成：确认 `docs/acceptance/production-line-process-pool/bdd-scenarios.md` R17 已要求同一工序支持多人、多次、分片填写；现有页面由 `formalSubmitResult` 永久锁定输入、重填和提交，与该约束冲突。
- M2 完成：新增聚焦静态合同，覆盖成功后输入复位、新幂等键、失败保留草稿、单次写请求和取消永久锁定态。
- M3 完成：移除生产页面持久 `formalSubmitResult/isProductionSubmitted` 状态；明确成功后调用统一草稿复位，失败 `finally` 只释放 loading；提交期间锁定工序/员工选择，成功结束后自动恢复。
- M4 完成：聚焦/相邻回归、类型检查、diff 检查、变更与前端 evidence validator、经验沉淀、cleanup preview/apply 均完成。
- M5 完成：前端 `8081` 返回 200，后端 `48081/actuator/health` 为 `UP`；测试租户 fixture、Chrome 与项目 Playwright 依赖均已确认。
- M5 首轮 RED：真实页面已到达正式提交接口，但后端返回 `1040750206`“当前用户未开通电子签名授权”，该轮未生成报工；只读核对确认 tenant `122` 的 `ffs0807worker` 当前授权记录数为 0。
- M5a 完成：事务为唯一目标测试员工新增 1 条 `ENABLED/1` DCC 授权及 1 条任务原因审计；独立复核摘要 `1|1|0|0|0`，未触及其他用户/租户，临时存储过程无残留。
- M5 第二轮 RED：签名授权通过后正式事务因 `mes_pro_process_pool_event.event_idempotency_key varchar(128)` 超长回滚；根因是客户端将完整上下文标签与新 draft token 拼接，真实键超过 128 字符预算。
- M5 验证脚本校正：幂等键修复后正式提交已成功，脚本随后因强制要求可选的 `recordbookEntryId` 中止；当前 API 契约明确将记录本两个回执定义为可选，无工单运行态合法不生成记录本，因此收紧为必验 `feedbackId + processPoolEventId`，并在记录本回执出现时继续要求四轮一致且唯一。
- M5 最终 GREEN：同一页面不刷新连续正式提交四轮，主 frame 导航次数 `0`；反馈 ID `881..884`、工序池事件 ID `195..198`，四个幂等键均为 45 字符且互不重复。每轮成功后数量和设备参数清空，正式提交、工序选择和员工选择恢复可用。
- M5 独立复核：数据库摘要 `4|4|4|45|4`，四个事件、四个签名、四个唯一键和四组反馈映射全部一致；可选记录本回执在当前无工单正式链路中均为空，符合接口契约。
- M5 视觉复核：第四轮后的页面截图显示数量和设备参数为空，损耗为 0，工序与员工仍保留且可继续操作，复位和正式提交按钮均可见。
- 经验沉淀：将客户端幂等键长度预算、短键构造、请求前 fail fast 和真实 E2E 唯一性校验合并到既有前端写入门禁及经验索引，未新建长期经验文档。

## TDD Evidence

- RED: `node tests/e2e/frontline-production-repeat-submit-static.spec.cjs` -> FAIL, 旧实现缺少明确的单次提交草稿复位函数，并由 `formalSubmitResult/isProductionSubmitted` 永久锁定生产页面。
- GREEN: `node tests/e2e/frontline-production-repeat-submit-static.spec.cjs` -> PASS。
- GREEN: `node tests/e2e/frontline-formal-submit-static.spec.cjs` -> PASS。
- REGRESSION: 13 项其它一线生产相邻静态合同 -> PASS。
- REGRESSION: `pnpm ts:check` -> PASS。
- REGRESSION: `git diff --check -- <本任务文件和经验文档>` -> PASS。
- REGRESSION: `validate_change_request.py --evidence docs/changes/20260809-frontline-repeat-submit-reset.md` -> PASS。
- REGRESSION: `validate_frontend_feature.py --evidence doc/tasks/20260809-frontline-repeat-submit-reset/frontend-feature-evidence.md` -> PASS。
- ISOLATED: `node tests/e2e/edhr-frontline-production-fullscreen-toggle-static.spec.cjs` -> FAIL，既有选择器立即关闭正则与当前基线实现不一致，失败代码区与本任务提交后会话复位无交集。
- TOOLCHAIN: `npx --yes --package @playwright/cli playwright-cli --help` -> FAIL，Windows 进程在打印完整帮助后触发既有 libuv `UV_HANDLE_CLOSING` 断言；按 `docs/e2e-rules.md` 改用项目 Playwright 脚本执行同一真实 UI 路径，不改用 API 或 mock。
- RED: `node doc/tasks/20260809-frontline-repeat-submit-reset/frontline-repeat-submit-real.e2e.cjs` -> FAIL，正式接口明确拒绝缺少电子签名授权的测试员工，失败发生在任何报工写入前。
- RED: 写入前有效授权只读断言 -> FAIL（预期），有效授权数 `0`。
- GREEN: `apply-test-worker-signature-authorization.sql` -> PASS，新增授权 `1`、授权审计 `1`，写后有效授权 `1`。
- GREEN: 独立授权范围复核 -> PASS，`1|1|0|0|0`。
- RED: `node tests/e2e/frontline-production-repeat-submit-static.spec.cjs` -> FAIL，旧幂等键构造缺少 128 字符预算并拼接冗长业务上下文。
- GREEN: `node tests/e2e/frontline-production-repeat-submit-static.spec.cjs` -> PASS，使用固定短前缀加 draft token 并在请求前校验长度。
- GREEN: 14 项相邻一线生产静态合同 -> PASS。
- GREEN: `pnpm ts:check` -> PASS。
- GREEN: `node doc/tasks/20260809-frontline-repeat-submit-reset/frontline-repeat-submit-real.e2e.cjs` -> PASS，`feedbackIds=881,882,883,884`。
- GREEN: 独立数据库正式事实复核 -> PASS，摘要 `4|4|4|45|4`。
- REGRESSION: bug/frontend/database evidence validators 及对应 self-tests -> PASS。
- REGRESSION: `git diff --check -- <本任务实现、测试、任务文档和经验文档>` -> PASS（仅有工作区既有 LF/CRLF 提示，无空白错误）。

## Blockers

- 无。

## Verification Notes

- 本轮只使用任务自有可追踪测试租户数据，不使用 mock、API 代提交或页面刷新；四组正式事实与签名按审计要求保留，不修改或删除。
- `playwright-cli` Windows daemon 在打印帮助后触发 libuv 断言，按项目 E2E 规则使用项目 Playwright 依赖执行同一真实 UI 路径，没有降级为 API-only。
- 结果文件：`output/playwright/20260809-frontline-repeat-submit-reset/frontline-repeat-submit-result.json`；截图：`output/playwright/20260809-frontline-repeat-submit-reset/frontline-repeat-submit-after-fourth.png`。
- `task-closeout-cleanup` preview/apply -> PASS，无 blocked 或 warnings；删除任务 SQL、验证器 evidence 和真实 E2E 临时执行脚本，保留 `task.md`、`execution-log.md`、`verification-report.md` 及正式 Playwright 审计产物。
- 用户要求继续后重新进入 M6：前一轮四连提仅覆盖同一员工 `914535` 和同一工序 `routeProcessId=980677/processId=980014`，本轮补齐原始需求中的跨员工、跨工序组合证据；不得用仅可选入口可用替代实际提交。
- M6 前置 BLOCKED：真实页面候选清单为 `uniqueRouteProcessCount=1/uniqueProcessCount=1/uniqueEmployeeCount=1`，只有员工 `914535` 与工序 `980677/980014`；脚本在任何正式提交前退出。
- BDD: M6 任务自有组合夹具 -> Given 当前正式运行态只有 1 人 1 工序 / When 事务补齐固定任务 ID 的第 2 名正式员工、第 2 道工序、唯一组长归属和签名审计 / Then 运行态必须达到至少 2 人 2 工序，任一断言失败整体回滚。
- RED: M6 数据夹具完整性断言 -> FAIL，预期 `1|1|1|1|1|1|1|1`，实际 `0|0|0|0|0|0|0|0`。
- GREEN: `apply-combination-e2e-fixture.sql` 首次及重复执行 -> PASS，两次摘要均为 `1|1|1|1|1|1|1|1`；独立复核 `1|1|1|1|1|1|1|1|0|0`，无重复、无临时存储过程残留。
- M6 harness RED 1：第一轮正式提交已明确成功并形成 `feedback=885/event=199/signature=3404`，但脚本错误要求成功复位后提交按钮必须 disabled，随后中止；数据库先只读确认该正式事实后，修正为断言按钮文本恢复“正式提交”和输入清空，不删除或重放该事实。
- M6 harness RED 2：修正后四轮正式提交 `feedback=886..889/event=200..203` 已完成，最终诊断发现登录重定向已进入目标页后脚本又二次导航，导致两条初始化 GET 被浏览器取消；修正为仅在路径尚未到达目标页时导航，不把该轮写成 PASS，正式事实按审计保留。
- M6 最终 GREEN：真实 Playwright 在同一 URL、主 frame 导航次数 0 的条件下完成 4 轮组合提交：`980677/980014 + 914535`、`9908090103/9908090101 + 914535`、`980677/980014 + 9908090201`、`9908090103/9908090101 + 9908090201`。
- M6 正式回执：feedback `890/891/892/893`，工序池事件 `204/205/206/207`，签名 `3409/3410/3411/3412`；四个幂等键唯一且长度均为 45。
- M6 独立数据库复核 -> PASS，摘要 `4|4|2|2|2|4|4|45|4|4|4`；四轮实际员工、签名主体、报工人、路线工序和 MES 工序均与页面选择一致，密码验证均通过。
- M6 视觉复核 -> PASS，第四轮后页面保留第 2 工序与第 2 员工选择，完成数量清空、损耗为 0、无设备工序正常显示，重填和正式提交按钮可继续使用。
- M6 收尾回归：组合真实 E2E helper `node --check`、连续提交聚焦合同、正式提交相邻合同、`pnpm ts:check`、database evidence validator、自检及任务范围 `git diff --check` 均 PASS；diff 检查仅报告既有 LF/CRLF 提示，无空白错误。
- M6 经验沉淀：将多员工/多工序写入型 E2E 的运行态候选盘点、正式 fixture 完整性、逐轮页面选择与落库事实匹配，以及成功回执后 harness 失败的先固化回执再只读分类规则，合并到既有 `docs/e2e-rules.md` 和经验索引；未新建长期经验文档。
- M6 cleanup：`task-closeout-cleanup` preview/apply -> PASS，无 blocked 或 warning；删除任务自有临时 fixture SQL、database evidence 和一次性组合 E2E helper，保留三份正式任务记录、生产实现、正式静态回归及 Playwright JSON/截图。任务状态更新为 `completed`；最终 preview 为 `delete=<none>/blocked=<none>/warnings=<none>`。
- M6 artifact final gate -> PASS：结果状态 PASS，4 轮覆盖 2 路线工序、2 MES 工序、2 员工、4 个唯一幂等键，主 frame 导航 0，feedback `890..893`、event `204..207`，截图存在。
