# P0 生产执行主闭环 E2E 计划

## Purpose and Scope

本文档定义 P0 主闭环真实 Playwright E2E。E2E 必须从真实前端页面操作，不得用 API-only、mock、静态合同、历史截图或直接 SQL 写入替代用户路径。

## Evidence Reviewed

- `docs/acceptance/production-execution-main-loop/bdd-scenarios.md`
- `docs/acceptance/production-execution-main-loop/tdd-plan.md`
- `docs/e2e-rules.md`
- `docs/login-access.md` 在真实登录前必须再读取。
- `docs/local-runtime.md` 在启动或复用本机运行态前必须再读取。
- `doc/tasks/20260731-team-leader-workbench-prd-plan/p6-real-e2e-evidence.md`

## User Paths

- 路径 1：生产组长配置活跃生产工单、工序员工、设备、设备参数和异常原因。
- 路径 2：生产员工进入一线报工入口，选择工序、实际员工、设备，填写数量和参数，完成电子签名并提交。
- 路径 3：PQC 员工进入 PQC 填写入口，选择活跃生产工单、工序和实际 PQC 员工，填写逐件检验，完成电子签名并提交。
- 路径 4：生产组长查看员工提交详情，完成带电子签名的复核和 FIFO 确认。
- 路径 5：PQC 组长查看 PQC 提交详情，完成带电子签名的复核。
- 路径 6：系统累计生产工单当前工序确认数量，达到目标后回填正式批记录。
- 路径 7：用户打开 P0 生产执行闭环 trace，按 `processPoolEventId` 或生产工单 + 工序查看完整追溯。

## M0 Preflight

- 当前 `IntRuoyiFronted/package.json` 尚未提供 `e2e:p0-production-execution-loop:static` 或 `e2e:p0-production-execution-loop:real` 脚本；P0 实现任务必须先把缺脚本记录为 RED，再新增对应脚本和 spec。
- 真实 E2E 前必须确认 `tests/e2e/p0-production-execution-loop-static.spec.*`、`tests/e2e/p0-production-execution-loop-real.e2e.*` 或等价正式文件存在；缺 spec 时只能记录 E2E 前置 blocker。
- 真实 E2E 前必须确认生产员工入口、PQC 填写入口、生产组长工作台、PQC 组长看板、FIFO 确认入口和统一 trace 入口均存在真实页面 route、菜单权限和按钮。
- 若任一路径只能通过 API wrapper、直接后端接口、SQL 或测试 helper 完成，则真实 E2E 必须 `BLOCKED`，不得记为 PASS。

## Browser or Client Steps

1. 使用 Playwright 打开本机前端入口，确认前端 URL 与后端 URL 成对。
2. 登录生产组长账号，进入工序池班组长工作台。
3. 加入任务自有活跃生产工单，绑定目标工序员工、设备、设备参数上下限、不良原因和正式批记录字段映射。
4. 登录或切换到生产员工真实填报入口。
5. 选择目标工序和实际员工，选择设备，填写完成数量、损耗数量、设备参数和不良原因。
6. 完成生产员工电子签名并提交，记录返回的 `processPoolEventId`。
7. 登录或切换到 PQC 员工真实填报入口。
8. 选择同一活跃生产工单、路线工序、PQC 任务和实际 PQC 员工。
9. 填写 QA 规程驱动的逐件检验明细，完成 PQC 员工电子签名并提交。
10. 打开生产组长提交看板，确认生产员工提交详情包含员工、设备、工序、数量、签名和原始 payload 摘要。
11. 生产组长完成复核电子签名，点击 FIFO 自动分配，必要时手工调整并确认。
12. 打开 PQC 组长看板，确认 PQC 提交详情包含 PQC 任务、规程、逐件明细、质量结果和签名。
13. PQC 组长完成复核电子签名。
14. 只读核验生产工单当前工序累计确认数量达到目标，状态为完成。
15. 打开批记录 trace 入口，确认正式批记录执行和字段审计投影存在。
16. 打开 P0 统一闭环 trace，断言页面完整展示提交、PQC、复核、FIFO 分配、工序完成和批记录字段审计。
17. 对缺正式批记录绑定、缺复核签名、PQC 失败或非活跃订单样本分别执行负向路径，断言页面显示明确阻塞原因。
18. finally 清理任务自有数据或记录无法清理的正式原因。

## API Verification

API 只允许用于最终只读核验、数据准备证据和清理证据。必须核验：

- 生产提交：报工、记录本条目、记录本事件和工序池事件存在且互相关联。
- PQC 提交：PQC 任务、逐件明细、质量结果和工序池 PQC 事件存在且互相关联。
- 电子签名：生产提交、PQC 提交、生产组长复核、PQC 组长复核均保存签名 ID、签名员工和签名快照。
- FIFO 分配：分配明细只指向活跃生产工单，分配总数等于确认数量。
- 工序完成：订单工序完成记录包含目标数量、确认数量、最后事件、最后复核和完成时间。
- 批记录回填：正式批记录执行、字段审计 batch、字段审计 item 和来源幂等键存在。
- 统一 trace：按事件返回的每个节点都能落到正式 ID，不依赖文案拼接。

## Console and Log Checks

- Playwright 捕获 `pageerror`、console error、目标接口 4xx/5xx，未解释错误必须导致失败或阻塞。
- 记录关键写请求：生产提交、PQC 提交、复核签名、FIFO 确认、批记录回填。
- trace、时间轴和看板详情查询不得产生写请求。
- 真实 E2E 证据必须分开记录目标写请求数量、只读核验请求、外部资源异常和目标链路异常；目标链路异常不得被外部资源异常掩盖。
- 日志和证据不得包含密码、token、cookie、Authorization、电子签名密码或私钥。

## Test Blockers

- 前后端运行态未启动或 URL 不成对。
- 缺少登录凭据、测试租户、生产组长、PQC 组长、生产员工、PQC 员工或电子签名测试能力。
- 缺少活跃生产工单、目标工序、设备、工作站、PQC 任务、QA 规程快照、正式批记录绑定或字段映射。
- 缺少 P0 E2E 脚本、spec、路由、页面入口、权限按钮或 trace 页面。
- 任一写路径只能通过 API 或数据库直接调用完成时，真实 E2E 必须 BLOCKED。
