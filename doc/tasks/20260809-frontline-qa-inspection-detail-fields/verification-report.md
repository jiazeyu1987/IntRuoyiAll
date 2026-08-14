# 验证报告

## 结论

- **独立验证门禁：BLOCKED**。本轮已在“芋道源码”完成真实 Playwright：QA 来源页显示两个目标工序、两列及首行正式值；一线 PQC 也存在 5 个同产品活跃订单，但抽查 3 个均因 `routeProjectItems routeId=980091，missingItemIds=[14]` 被正式设备账号上下文拒绝，无法渲染并打开详情弹窗。最终并发后端版本另有“历史空字段继续返回”与原 fail-fast 合同互斥的测试，当前目标 JUnit 仍是 39 tests 中 38 pass、1 error，不能判定端到端完成。
- 代码实现完成：红框标题区直接展示 QA 检验项目 `samplingPlanText`，黄框右侧直接展示 `inspectionTool`，旧四卡片已移除。
- 正式数据链路完成：QA 保存请求、数据库字段、发布版响应、一线 PQC 快照与响应均携带两个同名原文字段。
- 未引入 fallback：前端字段为必填并直接映射；保存和历史运行态缺字段时按精确字段路径 fail fast。
- 任务自有静态合同、迁移测试、迁移策略门禁、生产源码编译和 evidence validator 通过。
- 先前 task-closeout-cleanup preview/apply 已通过；用户要求继续真实验收后任务重新打开，并保留核心记录与正式交付物。
- 本次前端类型检查通过，IDI QA 业务工序映射不再阻塞页面显示；当前后端目标 JUnit 存在历史空字段产品口径冲突，剩余门禁详见“未完成验证”。

## 要求与证据清单

- 红框显示 QA 抽样方案：PASS。模板直接读取 `activePqcMethodItem.samplingPlanText`，静态合同覆盖精确 DOM 区域。
- 黄框显示 QA 检验器具及设备：PASS。模板直接读取 `activePqcMethodItem.inspectionTool`，旧四卡片合同明确禁止回归。
- 数据来自 QA 检验项目列表：PASS。QA 保存、DO、发布响应、一线快照、Controller 响应和前端映射均由跨层合同及 49 个目标 JUnit 覆盖。
- 无 fallback：PASS。两个字段为必填，前端直接映射；保存边界与历史运行态缺失时按字段路径 fail fast。
- QA 列表真实用户路径：PASS。已登录“芋道源码”并进入 `MES系统 > eDHR批记录 > QA`，选择 `IDI / 按压式球囊扩充压力泵 / 1`；页面真实显示“组装螺杆八组件”“光固外套四组件”“检验器具及设备”“抽样方案”，`qaWriteRequests=[]`、`consoleErrors=[]`、`pageErrors=[]`。
- 一线 PQC 弹窗真实用户路径：BLOCKED。页面存在 5 个“按压式球囊扩充压力泵”活跃订单；`CODX-AO5-20260807-05/-04/-03` 均在工序加载时返回 `routeProjectItems routeId=980091，missingItemIds=[14]`，因此目标详情入口未渲染，不能断言弹窗内容。

## 变更范围

- 前端：QA 保存载荷、一线 PQC API 类型、检验方法详情弹窗及相邻静态合同。
- 前端行为补充：显式允许 IDI 中尚未识别批记录绑定的业务工序继续显示，并归入页面已解析的正式 QA 质检工序载荷；不猜测组装Ⅰ/Ⅱ、光固Ⅰ/Ⅱ映射，不伪造批记录绑定摘要。
- 后端：QA 保存/发布 VO、DO、服务校验与映射，一线 PQC 项目快照、响应 VO 和 Controller 映射。
- 数据库：新增幂等 migration，为 `mes_qa_inspection_regulation_item` 增加 `inspection_tool`、`sampling_plan_text` nullable 原文字段，不回填历史值。
- 长期经验：更新 `docs/backend-development.md` 与 `docs/experience-index.md`。

## 通过证据

- BDD/RED/GREEN：见 `execution-log.md`，包含三个预期 RED 和对应 GREEN。
- 6 个前端目标/相邻静态合同 PASS。
- 未识别工序静态合同先 RED 后 GREEN；原逐路线工序发布、IDI 完整项目、逐页截图和 QA standalone role-matrix 合同保持 PASS。
- `pnpm ts:check`：PASS。
- migration pytest：2 passed。
- release migration policy gate：PASS，migrationCount=457。
- 先前目标 JUnit：49 tests，0 failures，0 errors，0 skipped，BUILD SUCCESS；该证据早于并发新增历史空字段测试，不能替代当前最终回归结论。
- Maven Reactor compile：24/24 SUCCESS。
- 最终 `mvn -pl yudao-module-mes "-DskipTests" compile`：BUILD SUCCESS。
- `git diff --check`：PASS，仅有 LF/CRLF warning。
- frontend/backend/database evidence validator：全部 PASS。
- 本轮真实 QA 来源页：PASS；目标值 `inspectionTool=目测`、`samplingPlanText=GB/T 2828.1，I，AQL=0.4`，目标 QA 写请求为 0。
- 本轮真实一线 PQC：BLOCKED；3 个同产品订单命中相同正式上下文缺口，目标 HTTP/request failures、`consoleErrors`、`pageErrors` 均为空，业务数据写请求为 0。

## 未完成验证

- 真实 QA 列表验证已经解除原正式工序映射 blocker：未识别批记录绑定的 IDI 业务工序继续显示，已识别工序仍按正式路线工序身份分组。
- 一线 PQC 当前已有可见活跃订单，不再把“完全缺少订单”作为本轮 blocker；正式 blocker 是路线 `980091` 的 `routeProjectItems` 缺少产品项目 `14`，设备账号上下文按正式门禁拒绝加载工序检验项目。
- 在正式绑定缺口修复前，不能用 API/SQL 补上下文、跨产品/路线借用其它订单或以前端直塞数据打开弹窗。
- 本轮 QA 保存/发布 POST 数为 0；未覆盖现有规程，未使用 mock/API-only 替代。
- 相邻回归缺口：`qa-regulation-route-checkflag-fallback-static.spec.cjs` 仍按旧 Promise 解构结构断言，当前正式代码已使用 `currentRouteProcesses + resolveQaVersionRouteProcesses(...)`，因此该旧合同失败；它不覆盖本次未识别工序分支，但未被伪记为 PASS。
- 后端产品口径冲突：最终并发版本新增 `shouldListQaProcessWhenLegacyPublishedItemDisplayFieldsAreBlank`，要求历史空 `inspectionTool/samplingPlanText` 继续返回 `null`；原任务静态合同及长期门禁要求缺正式原文按精确字段路径 fail fast。目标 JUnit 39 tests 中 38 pass、1 error，未伪记为通过，也未增加兼容分支。

## 上线前置

1. 通过正式产品/路线配置入口为 `routeId=980091` 补齐产品项目 `itemId=14` 的 `routeProjectItems` 绑定，并确认设备账号上下文能加载同产品活跃订单工序；不得用 API/SQL 临时补值。
2. 解决历史已发布 QA 项目空 `inspectionTool/samplingPlanText` 应 fail-fast 还是返回 `null` 的产品口径冲突，使目标 JUnit 恢复全绿。
3. 复用 `CODX-AO5-20260807-*` 真实订单重新打开“检验方法”详情弹窗，断言顶部为抽样方案、右侧为检验器具及设备，并继续保持无 PQC 正式提交。

## Git 与环境

- 未执行 Git stage、commit、merge 或 push；用户未授权这些操作。
- 已在本机 Docker MySQL 应用正式幂等迁移，两列为 nullable `varchar(512)`，历史 166 行未回填且非空计数均为 0。
- 曾用任务专用 Jar 启动 `48081` 并完成真实页面前置取证；当前 `8081/48081` 由本机其它已存在运行态提供且健康，本任务未停止或替换这些非任务自有进程。
- 未写入 QA 业务数据，未修改 admin 基线或其它租户现有规程；Playwright 会话和本任务敏感快照/临时输出已清理。
- frontend feature evidence validator 与 self-test 均 PASS；变更请求 evidence validator 与 self-test 均 PASS。
