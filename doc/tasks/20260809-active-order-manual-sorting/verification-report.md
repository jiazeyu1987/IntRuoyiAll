# Verification Report

## Result

结论为 `PASS`。源码实现、定向自动化、运行库 schema、运行 Jar 路由门禁和真实 Playwright 上移/下移路径均通过；任务专用测试活跃订单已从页面移除，最终顺序恢复。

## Delivered Behavior

- 活跃订单操作列提供上移、下移图标按钮；首行上移、末行下移禁用。
- `PUT /mes/pro/process-pool/team-leader/active-order/move` 从安全上下文取得组长身份，只交换当前组长范围内的相邻活跃订单。
- 列表按 `sort_order, joined_at, id` 返回；新增或重新激活订单进入当前组长序列末尾，刷新后人工顺序保持。
- 正式迁移确定性回填历史顺序，并建立组长范围排序索引。

## Automated Verification

- 前端任务专用及相邻静态合同：3 个脚本 PASS。
- `pnpm ts:check`：PASS。
- SQL 迁移合同：3 项 PASS。
- MES 定向 Maven profile：31 项 PASS，`BUILD SUCCESS`。
- release migration policy gate：PASS，457 个迁移通过，包含本任务迁移。
- 运行库 schema：`sort_order bigint NOT NULL`；`idx_mes_pp_active_order_manual_sort` 存在；14 条未删除记录无空排序值。
- 运行 Jar 门禁：`backend-runtime-control-20260809-202548.jar` PASS，Controller、移动 VO、Service、Mapper 和 `PUT /active-order/move` 均已加载。
- 后端运行态：`48081` health `UP`，运行 Jar SHA256 `87DBA62E274F7601F56E7A37B3AC57E0A56B75910FBF51C37B1C550E6A1E946B`。
- 收尾后共享运行态再次切换为 `backend-report-shared-allocation-20260809.jar`；最终只读复核仍为 health `UP`，SHA256 `392D860CDE71F102A9FFE1AE510F3A7295126440A421274AE47BC236DBA7BEB2`，移动路由字节码和未压缩内嵌 MES Jar 门禁 PASS。

## Real Playwright Verification

- 登录本机测试租户后，从“生产组长 -> 活跃订单池”通过页面新增候选工单 `881MO090935`，生成任务专用活跃订单 ID `50`。
- 点击“上移”：接口 HTTP 200、业务码 `0`，顺序由 `[35,36,37,38,39,48,49,50]` 变为 `[35,36,37,38,39,48,50,49]`。
- 点击“下移”：接口 HTTP 200、业务码 `0`，顺序恢复为 `[35,36,37,38,39,48,49,50]`；页面刷新后仍保持。
- 边界状态：首行上移禁用，末行下移禁用；按钮数量与行数一致。
- 清理：通过页面移除活跃订单 `50`；最终 UI 与登录态 API 都为 `[35,36,37,38,39,48,49]`，任务行不存在。
- 浏览器最终控制台：0 errors、0 warnings。

## Runtime Incident Evidence

- 旧运行 Jar 缺少移动 VO 和 Controller 方法，运行门禁取得预期 RED。
- 首次任务定向运行包因脏主工作区 Controller 混入并发报工分配依赖，启动时报 `NoClassDefFoundError: MesReportAllocationSnapshot` 并 fail fast；该运行包未作为成功版本。
- 完整 `int_main` 运行包随后接管 `48081`，通过结构、health、登录态写接口和真实页面四层验证。

## Residual Risk

- 无定向 profile 的 MES 全量测试仍受并发任务既有测试编译问题影响；本任务 31 项正式定向测试、类型检查、静态合同、schema 和真实运行态验收均通过。
- 本次只验证本机 `int_main`，未获授权操作远端环境。

## Closeout

- Bug regression evidence validator：PASS。
- `task-closeout-cleanup` preview/apply：PASS，无 blocked/warnings；14 个任务专用临时路径已删除。
- 项目经验复核：既有 `docs/local-runtime.md` 门禁已覆盖本轮运行 Jar 依赖闭包问题，无需重复新增长期经验规则。

## M7 Production Order Number Display

- 结论：`PASS`。活跃订单列表表头由“生产订单ID”改为“生产订单号”，单元格直接展示正式 `workOrderCode`；`workOrderId` 继续保留为新增、移动、移除和异常上报等结构化身份参数。
- BDD/TDD：聚焦静态合同先取得预期 RED，最小修复后 GREEN；相邻 4 个活跃订单、工作台和异常上报静态合同均 PASS。
- TypeScript：`pnpm ts:check` PASS；scoped `git diff --check` 无空白错误。
- 技能证据：bug regression 与 frontend feature evidence validator 均 PASS。
- 真实 Playwright：登录本机页面并进入“生产组长 -> 活跃订单池”，表头显示“生产订单号”，行内显示 `CODX-AO5-20260807-01`、`881MO090889` 等正式订单号，对应内部 ID 不在目标列展示；console 0 errors、0 warnings。
- 运行态：前端 `8081` HTTP 200，后端 `48081` health `UP`。
- 经验复核：`docs/frontend-development.md#用户可见描述与内部编码隔离门禁` 及其经验索引已覆盖本轮规则，无需重复更新。
- 收尾：`task-closeout-cleanup` preview/apply PASS，仅删除 4 个本轮 Playwright 临时文件和 2 份临时技能证据；实现、正式回归测试与三份核心任务文档已保留。收尾后 preview 的 delete/blocked/warnings 均为空，任务状态为 `completed`。
