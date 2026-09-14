# P0 生产执行主闭环 E2E 计划

## Purpose and Scope

本文档定义 P0 主闭环真实 Playwright E2E。E2E 必须从真实前端页面操作，不得用 API-only、mock、静态合同、历史截图或直接 SQL 写入替代用户路径。

## Evidence Reviewed

- `docs/acceptance/production-execution-main-loop/bdd-scenarios.md`
- `docs/acceptance/production-execution-main-loop/tdd-plan.md`
- `docs/e2e-rules.md`
- `docs/database-rules.md` 在运行态 schema、迁移或只读 DB 核验前必须再读取。
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

- M0 初始缺口是 `IntRuoyiFronted/package.json` 未提供 `e2e:p0-production-execution-loop:static` 或 `e2e:p0-production-execution-loop:real` 脚本；后续若脚本再次缺失，必须先把缺脚本记录为 RED，再新增对应脚本和 spec。
- 真实 E2E 前必须确认 `tests/e2e/p0-production-execution-loop-static.spec.*`、`tests/e2e/p0-production-execution-loop-real.e2e.*` 或等价正式文件存在；缺 spec 时只能记录 E2E 前置 blocker。
- 真实 E2E 前必须确认生产员工入口、PQC 填写入口、生产组长工作台、PQC 组长看板、FIFO 确认入口和统一 trace 入口均存在真实页面 route、菜单权限和按钮。
- 若任一路径只能通过 API wrapper、直接后端接口、SQL 或测试 helper 完成，则真实 E2E 必须 `BLOCKED`，不得记为 PASS。
- 当前 worktree 运行态默认端口是前端 `8092`、后端 `48092`；若改用融合后的 `int_main`，必须显式记录前端 `8081`、后端 `48081`，并证明代码已合入该运行态。端口不成对或 `Frontend` / `Backend` evidence 混配时只能 `BLOCKED`，completion gate 必须返回 `P0_COMPLETION_RUNTIME_URL_PAIR_INVALID`。
- `Browser Preflight` 必须来自同一个 `Frontend` 运行态，允许等于 `Frontend` 或以该 URL 后接 `/`、`?`、`#` 开始；若浏览器实际页面落到其它端口或其它前端实例，completion gate 必须返回 `P0_COMPLETION_BROWSER_PREFLIGHT_URL_MISMATCH`。
- 每个 `Target Request <LABEL>` evidence 必须同时记录 `Hit`、实际 `URL`、实际 `Method`、实际 `HTTP Status` 和实际 `Business Code`；URL 必须指向同一个 `Backend` 运行态下的正式 endpoint，Method 必须匹配正式 POST/GET 边界，HTTP Status 必须为 2xx，Business Code 必须为 `0`。错后端或旧后端必须返回 `P0_COMPLETION_TARGET_REQUEST_URL_MISMATCH`，错方法必须返回 `P0_COMPLETION_TARGET_REQUEST_METHOD_MISMATCH`，非 2xx 必须返回 `P0_COMPLETION_TARGET_REQUEST_HTTP_STATUS_NOT_OK`，业务码非 0 或缺失必须返回 `P0_COMPLETION_TARGET_REQUEST_BUSINESS_CODE_NOT_OK`。
- `result.json.targetRequests` 必须按 `label + endpoint` 输出且只输出五个 canonical 目标请求；重复提交、重复 PQC 和重复 FIFO 确认只能进入对应 duplicate evidence，不得在主目标请求列表产生数量漂移、重复 label、重复 endpoint 或任何非 required 目标请求，否则 completion gate 必须返回 `P0_COMPLETION_REAL_E2E_RESULT_TARGET_REQUEST_COUNT_MISMATCH`、`P0_COMPLETION_REAL_E2E_RESULT_TARGET_REQUEST_DUPLICATE` 或 `P0_COMPLETION_REAL_E2E_RESULT_TARGET_REQUEST_UNEXPECTED`。
- `result.json.targetRequests[*]` 必须逐条是 JSON object；字符串、数组、数字或其它非对象项必须返回 `P0_COMPLETION_REAL_E2E_RESULT_TARGET_REQUEST_OBJECT_MISSING`，不得让非对象项落入泛化 unexpected 后继续倒推目标请求完整性。
- `result.json.targetRequestEvidenceFlushed` 必须为 `true`，证明真实 E2E 已等待目标请求 response body 解析完成后才写入 `targetRequests` 的 Business Code 证据；缺失或不是 `true` 时 completion gate 必须返回 `P0_COMPLETION_REAL_E2E_RESULT_TARGET_REQUEST_EVIDENCE_NOT_FLUSHED`。
- `result.json.targetRequests[*].label` 必须逐条存在且非空；缺失、空值或字符串 `MISSING` 时 completion gate 必须返回 `P0_COMPLETION_REAL_E2E_RESULT_TARGET_REQUEST_LABEL_MISSING`，不得只用 URL endpoint、数组顺序或 Markdown `Target Request <LABEL>` 反推 JSON 请求身份。
- `result.json.targetRequests[*].url` 必须逐条存在且非空；缺失、空值或字符串 `MISSING` 时 completion gate 必须返回 `P0_COMPLETION_REAL_E2E_RESULT_TARGET_REQUEST_URL_MISSING`，不得只用 Markdown `URL=<Backend>/<endpoint>`、label 或 endpoint 反推 JSON 请求 URL。
- `result.json.targetRequests[*].method` 必须逐条存在且非空；缺失、空值或字符串 `MISSING` 时 completion gate 必须返回 `P0_COMPLETION_REAL_E2E_RESULT_TARGET_REQUEST_METHOD_MISSING`，不得只用 Markdown `Method=POST/GET` 或 endpoint 反推 JSON 请求方法。
- `result.json.targetRequests[*].businessCode` 必须逐条存在且可解析为数字；缺失、字符串 `MISSING`、空值或非数字时 completion gate 必须返回 `P0_COMPLETION_REAL_E2E_RESULT_TARGET_REQUEST_BUSINESS_CODE_MISSING`，不得只用 Markdown `Business Code=0` 或 HTTP 2xx 替代 JSON 业务码证据。
- `result.json.targetRequests[*].httpStatus` 必须逐条存在且可解析为数字；缺失、字符串 `MISSING`、空值或非数字时 completion gate 必须返回 `P0_COMPLETION_REAL_E2E_RESULT_TARGET_REQUEST_HTTP_STATUS_MISSING`，不得只用 Markdown `HTTP Status=200` 或业务码 0 替代 JSON HTTP 响应状态证据。
- `result.json.targetResponseIdentities` 必须精确输出五个 required response identity key；每个 identity 必须来自对应目标请求响应，且不得少于五个、多于五个，或混入背景刷新、登录、权限预检、相似 URL、旧 run 的额外 response identity，否则 completion gate 必须返回 `P0_COMPLETION_REAL_E2E_RESULT_TARGET_RESPONSE_COUNT_MISMATCH` 或 `P0_COMPLETION_REAL_E2E_RESULT_TARGET_RESPONSE_UNEXPECTED`。
- `result.json.targetResponseIdentities.<LABEL>` 必须逐项是 JSON object；字符串、数组、数字或其它非对象项必须返回 `P0_COMPLETION_REAL_E2E_RESULT_TARGET_RESPONSE_OBJECT_MISSING`，不得把非对象响应身份泛化为 missing 后继续倒推响应身份完整性。
- `result.json.targetResponseIdentities.<LABEL>.field` 必须逐项存在且非空；缺失、空值或字符串 `MISSING` 时 completion gate 必须返回 `P0_COMPLETION_REAL_E2E_RESULT_TARGET_RESPONSE_FIELD_MISSING`，不得只用 Markdown `Target Response <LABEL> <field>` 反推 JSON 响应字段名。
- `result.json.targetResponseIdentities.<LABEL>.value` 必须逐项存在且可解析为正整数；缺失、字符串 `MISSING`、空值、非数字或非正数时 completion gate 必须返回 `P0_COMPLETION_REAL_E2E_RESULT_TARGET_RESPONSE_VALUE_MISSING`，不得只用 Markdown `Target Response <LABEL> <field>=<id>` 替代 JSON 响应 ID。
- `result.json.targetResponseIdentities.<LABEL>.sourceRequestLabel` 必须逐项存在且非空；缺失、空值或字符串 `MISSING` 时 completion gate 必须返回 `P0_COMPLETION_REAL_E2E_RESULT_TARGET_RESPONSE_SOURCE_REQUEST_LABEL_MISSING`，不得只用响应 identity key 或 Markdown 请求行替代 JSON 来源请求 label。
- `result.json.targetResponseIdentities.<LABEL>.sourceRequestLabel` 必须等于当前 canonical `<LABEL>`；真实 E2E 写入 result artifact 时必须把响应身份绑定回对应目标请求 label，来源 label 串用其它 label 时 completion gate 必须返回 `P0_COMPLETION_REAL_E2E_RESULT_TARGET_RESPONSE_REQUEST_LABEL_MISMATCH`。
- `result.json.targetResponseIdentities` 的 key 集合必须与同一个 `result.json.targetRequests[].label` 观测集合完全一致；若响应身份 key 没有对应观测目标请求，或观测请求 label 没有对应响应身份，completion gate 必须返回 `P0_COMPLETION_REAL_E2E_RESULT_TARGET_RESPONSE_REQUEST_SET_MISMATCH`。
- 真实 E2E 前必须通过 `P0_RUNTIME_DB_HOST`、`P0_RUNTIME_DB_PORT`、`P0_RUNTIME_DB_NAME`、`P0_RUNTIME_DB_USER`、`P0_RUNTIME_DB_PASSWORD` 注入只读运行态 MySQL 连接，并执行 `verify_p0_runtime_migration.py`；缺环境变量、缺字段、缺索引或历史断链 blocker 时只能 `BLOCKED`，不得启动浏览器写入。
- 真实 E2E 前必须确认前后端代码版本来自本任务分支或已合入分支；运行态 schema 缺正式字段、索引或唯一约束时只能 `BLOCKED`。
- 真实 E2E 前必须确认测试账号的租户、角色、菜单和工单范围；若只能用超管绕过权限完成主路径，必须记录为权限前置 blocker，不能作为普通用户闭环 PASS。

## Result Semantics

- `PASS`：Playwright 从真实前端页面完成生产提交、PQC 提交、班组长复核、FIFO 确认、工序完成、活跃订单完成统一回填、批次执行三类文件上传和 trace 完整断言，并完成只读核验与清理/保留证据。
- `BLOCKED`：真实前后端、租户、账号、签名、工单、设备、PQC 任务、正式批记录绑定、字段映射、页面入口或 `P0_RUNTIME_DB_*` 只读核验环境缺失；未执行写入型主路径。
- `FAIL/RED`：真实前置已具备，但页面步骤、目标接口、业务断言、清理或 trace 完整性失败；不得降级为 `BLOCKED` 掩盖实现缺口。
- 只通过脚本存在性、URL 健康检查、登录成功或 trace 只读页加载，均不能记为 P0 主闭环 PASS。
- 使用预置 `P0_PROCESS_POOL_EVENT_ID` 直接打开 trace、或用 API/SQL 预先写好主路径事实后只让页面查看，不能记为 PASS；真实 PASS 必须在同一次 Playwright run 中从页面写入并捕获新的主事件 ID。

## PASS Assertion Checklist

真实 E2E 只有同时满足以下断言才允许记录为 `PASS`：

- 捕获新的生产提交根事件 `processPoolEventId`，且该 ID 来自真实页面生产提交响应或页面可见 trace 链接，不来自预置环境变量；PQC 提交产生的 `pqcEventId` 必须单独记录，不能替代生产提交根事件。
- 证据中必须包含本次 run 的 `runId`、生产提交幂等键、PQC 幂等键、复核/确认幂等键和捕获到的全部正式 ID；这些 ID 必须能互相追溯，且不得由脚本伪造。
- 证据中必须包含本次 run 生成的 `Generated At`，格式为 ISO UTC `Z` 时间戳，且不得早于或晚于 evidence 文件写入时间超过 6 小时；缺失、格式错误、复用旧 evidence 或使用未来时间戳 evidence 时不得 PASS。
- 证据中 `Frontend` / `Backend` 必须是同一正式运行态配对：当前 worktree `8092/48092` 或已合入 `int_main` 的 `8081/48081`；不得用 `8092/48081` 等混配 evidence 证明同一次闭环。
- 证据中 `Browser Preflight` 必须落在同一个 `Frontend` URL 下；不得用 `Frontend=8092` 但浏览器预检 URL 指向 `8081` 或其它前端的 evidence 证明同一次闭环。
- 生产提交、PQC 提交、PQC 组长复核、生产组长 FIFO 确认都必须产生 `POST` 目标请求；统一 trace 必须产生 `GET` 目标请求；看板只读刷新不得计入写请求。
- 每个目标请求 evidence 必须记录实际 URL 和 Method，且 URL 必须使用 evidence `Backend` 对应的正式 endpoint，Method 必须匹配生产/PQC/复核/FIFO 的 `POST` 与 trace 的 `GET`；只写 `Hit=true`、只记录 endpoint 名称、指向其它后端或 Method 错误都不得 PASS。
- 证据必须包含 `verify_p0_runtime_migration.py` 对真实 MySQL 的只读 PASS 结果，证明 PQC 结构化绑定、复核签名、FIFO 来源、批记录字段审计、幂等键、索引和历史断链检查均满足当前代码。
- 证据必须包含相关新增 SQL 的 release migration policy gate 结果，且 migration policy evidence 文件必须明确 `PASS`、不得包含 `BLOCKED/FAIL/FAILED` 标记；运行态只存在测试 schema 字段、未应用正式迁移或迁移靠默认值补历史数据时，不得 PASS。
- trace 顶层 `complete=true` 时，`submitEvent`、`quality`、`review`、`allocation`、`completion`、`batchRecord` 六个分组都存在正式 `sourceIds`，并且 `blockers` 为空。
- PQC 质量分组必须展示结构化生产提交绑定证据、PQC 检验数量、合格数量、可分配数量和已消耗数量；如果只从 rawPayload 解析到生产提交 ID，或合格数量不足以覆盖 FIFO 确认数量，本次 E2E 必须记为 `FAIL/RED` 或 `BLOCKED`，不得 PASS。
- 只读核验能证明所有分组属于同一租户、同一目标生产工单、同一路线工序、同一来源事件链或正式绑定关系。
- FIFO 确认发生在质量状态可分配、PQC 合格数量覆盖确认数量、生产组长强制复核已签名完成、以及所有配置为强制的 PQC 组长复核已签名完成之后。
- 批记录分组能展示正式批记录执行 ID、报表 ID、定义/版本 ID、字段审计 batch/item 和字段来源路径。
- 批记录字段审计必须能展示来源值、旧值、新值、字段路径、单元格位置和来源分配/事件 ID；只有批记录执行 ID 或中文摘要不算追溯完成。
- 负向样本至少覆盖缺复核签名、PQC 失败或未知质量、非活跃订单、缺正式批记录绑定之一；负向路径必须显示明确 blocker，而不是静默失败。
- 重复点击或重复确认样本必须证明第二次请求未产生第二条有效事件、分配、完成或字段审计；若只能靠前端按钮禁用证明，不能 PASS。
- 跨租户或跨工单负向样本必须证明 trace 不会拼接其它租户、工单或工序的 PQC、复核、FIFO 或批记录事实。
- 清理或保留证据已经写入任务证据；无法删除的 append-only 审计数据必须记录 runId 和只读复验方式。

## Closure Evidence Packet

真实 E2E 的最终证据必须生成脱敏闭环证据包，并保存到任务证据或 E2E 输出中。证据包字段不得包含密码、token、cookie、签名密码或未脱敏 raw payload。

| 字段 | 必须证明 |
| --- | --- |
| `runId` | 本次真实页面 run 的任务前缀和运行态 URL。 |
| `processPoolEventId` | 来自本次页面生产提交捕获的新主事件 ID，事件类型必须为 `PRODUCTION_SUBMIT`。 |
| `pqcEventId` | 来自本次页面 PQC 提交捕获的质量子事件 ID，事件类型必须为 `PQC_INSPECTION`，并通过结构化字段绑定到 `processPoolEventId`。 |
| `answers.who` | 实际员工、设备账号和提交签名员工，带正式来源 ID。 |
| `answers.device` | 设备、工作站和设备账号，带正式来源 ID。 |
| `answers.process` | 生产工单、路线工序和 MES 工序，带正式来源 ID。 |
| `answers.quantity` | 提交数量、PQC 检验数量、PQC 合格数量、确认数量、分配数量和订单工序累计数量的勾稽。 |
| `answers.quality` | PQC 任务、逐件摘要、质量结论、可分配状态、合格数量覆盖结果和结构化生产提交绑定。 |
| `answers.signature` | 生产提交、PQC、生产组长复核和 PQC 组长复核签名 ID 与签名员工。 |
| `answers.workOrder` | FIFO 目标活跃订单和生产工单，带来源复核和来源事件。 |
| `answers.review` | 所有强制复核角色的配置状态、实际状态、复核人和签名。 |
| `answers.batchRecord` | 正式批记录执行、字段审计 batch/item、来源值、旧值、新值和来源分配/事件。 |
| `sameSourceChecks` | 租户、生产工单、路线工序、MES 工序和权限边界同源结果。 |
| `blockers` | 若任一答案缺失，必须列出机器可读 blocker；此时不得 PASS。 |

证据包只能由后端 trace 响应和只读核验结果组装；Playwright 不得用页面文案、截图 OCR、历史环境变量或测试脚本常量补齐缺失业务事实。

## Failure Attribution Rules

- 缺少账号、运行态、页面入口、权限、签名能力或任务自有数据时，记录 `BLOCKED`，并列出解除条件。
- 前置齐备但页面按钮不可用、接口返回业务错误、trace 缺分组、`complete` 误判、只读核验不一致或清理失败时，记录 `FAIL/RED`。
- 目标链路 4xx/5xx、pageerror 或目标写请求数量为 0 时，不能被外部头像、字体、地图或第三方资源错误掩盖。
- 使用 API/DB 直接写入主路径事实后跑页面查看，只能算诊断或种子验证，不能算真实 E2E PASS。

## Browser or Client Steps

1. 使用 `P0_RUNTIME_DB_*` 调用 `verify_p0_runtime_migration.py` 只读核验真实 MySQL；只有验证器 PASS 才允许启动浏览器写入。
2. 使用 Playwright 打开本机前端入口，确认前端 URL 与后端 URL 成对。
3. 登录生产组长账号，进入工序池班组长工作台。
4. 加入任务自有活跃生产工单，绑定目标工序员工、设备、设备参数上下限、不良原因和正式批记录字段映射。
5. 登录或切换到生产员工真实填报入口。
6. 选择目标工序和实际员工，选择设备，填写完成数量、损耗数量、设备参数和不良原因。
7. 完成生产员工电子签名并提交，记录返回的生产提交根事件 `processPoolEventId`，并确认事件类型为 `PRODUCTION_SUBMIT`。
8. 登录或切换到 PQC 员工真实填报入口。
9. 选择同一活跃生产工单、路线工序、PQC 任务和实际 PQC 员工。
10. 填写 QA 规程驱动的逐件检验明细，完成 PQC 员工电子签名并提交，记录 `pqcEventId`、检验数量、合格数量和结构化生产提交绑定。
11. 打开生产组长提交看板，确认生产员工提交详情包含员工、设备、工序、数量、签名和原始 payload 摘要。
12. 生产组长完成复核电子签名，但在质量和强制复核门禁未满足前不得确认 FIFO。
13. 打开 PQC 组长看板，确认 PQC 提交详情包含 PQC 任务、规程、逐件明细、质量结果和签名。
14. 若后端规则要求 PQC 组长强制复核，PQC 组长完成复核电子签名；若不要求强制复核，trace 必须返回该角色非强制配置状态。
15. 生产组长在质量状态可分配、PQC 合格数量覆盖确认数量且所有强制复核完成后，点击 FIFO 自动分配，必要时手工调整并确认。
16. 只读核验生产工单当前工序累计确认数量达到目标，状态为完成。
17. 打开批记录 trace 入口，确认正式批记录执行和字段审计投影存在。
18. 打开 P0 统一闭环 trace，断言页面完整展示提交、PQC、复核、FIFO 分配、工序完成和批记录字段审计。
19. 断言 trace 顶层 `complete=true` 时每个分组都有正式 `sourceIds`，并且所有 ID 都能通过只读 API/DB 落到同一租户、同一生产工单和同一路线工序。
20. 对生产提交、PQC 提交或 FIFO 确认执行一次重复提交验证，断言第二次请求返回同一正式结果或明确重复拒绝。
21. 对缺正式批记录绑定、缺复核签名、PQC 失败、PQC 合格数量不足、PQC 多候选歧义、rawPayload-only PQC 绑定或非活跃订单样本分别执行负向路径，断言页面显示明确阻塞原因。
22. finally 清理任务自有数据或记录无法清理的正式原因。

## API Verification

API 只允许用于最终只读核验、数据准备证据和清理证据。必须核验：

- 生产提交：报工、记录本条目、记录本事件和工序池事件存在且互相关联。
- PQC 提交：PQC 任务、逐件明细、质量结果和工序池 PQC 事件存在且互相关联。
- 电子签名：生产提交、PQC 提交、生产组长复核、PQC 组长复核均保存签名 ID、签名员工和签名快照。
- FIFO 分配：分配明细只指向活跃生产工单，分配总数等于确认数量。
- 质量数量：PQC 检验数量、合格数量、可分配数量、已消耗数量和 FIFO 确认数量可勾稽；确认数量超过合格可分配数量时不产生分配、完成或批记录字段审计。
- 工序完成：订单工序完成记录包含目标数量、确认数量、最后事件、最后复核和完成时间。
- 统一回填：正式批记录执行、过程检验单回填证据、字段审计 batch、字段审计 item、来源幂等键存在；有损耗时存在损耗单回填证据，无损耗时不存在空损耗单。
- 统一 trace：按事件返回的每个节点都能落到正式 ID，不依赖文案拼接。
- 幂等性：生产提交、PQC 提交和组长确认重复执行时，不产生第二条有效主事件、PQC 事件、复核记录、分配记录、完成记录或批记录字段审计。
- 结构化绑定：PQC 到生产提交、复核到来源事件、分配到来源复核、批记录字段审计到来源分配必须来自正式字段或关系；rawPayload、备注或中文摘要只能作为辅助审计。
- 闭环证据包：九个审计问题的答案均有正式来源 ID、同源校验和只读复验入口；任一缺失时真实 E2E 不得 PASS。
- schema 一致性：运行态表结构、迁移版本、测试 schema 合同和 trace DTO 字段一致；缺字段或索引时真实 E2E 记录 `BLOCKED`。
- 运行态迁移核验：`verify_p0_runtime_migration.py` 必须通过真实 MySQL 只读连接确认 P0 required columns、indexes 和 historical checks；缺 `P0_RUNTIME_DB_*` 或验证器非 PASS 时不得执行浏览器写入。
- 迁移策略：新增 SQL 已通过 release migration policy gate，证据文件明确 `PASS` 且无 `BLOCKED/FAIL/FAILED` 标记；历史缺正式来源 ID 时运行态必须保留 blocker，不得通过默认值、rawPayload backfill 或人工说明让 trace 完成。
- 同源隔离：trace 只聚合本次 run 捕获的租户、工单、路线工序、MES 工序和来源事件链，跨边界事实不得进入完成谓词。

## Console and Log Checks

- Playwright 捕获 `pageerror`、console error、目标接口 4xx/5xx，未解释错误必须导致失败或阻塞。
- 记录关键目标请求、方法、HTTP 状态和业务码：生产提交 `POST 2xx code=0`、PQC 提交 `POST 2xx code=0`、复核签名 `POST 2xx code=0`、FIFO 确认 `POST 2xx code=0`、活跃订单完成 `POST 2xx code=0`、批次执行文件上传 `POST 2xx code=0`、trace 查询 `GET 2xx code=0`；统一回填由后端 trace 和只读核验证明。
- trace、时间轴和看板详情查询不得产生写请求。
- 真实 E2E 证据必须分开记录目标请求命中、URL、Method、HTTP Status、Business Code、只读核验请求、外部资源异常和目标链路异常；目标链路异常不得被外部资源异常掩盖。
- 日志和证据不得包含密码、token、cookie、Authorization、电子签名密码或私钥。

## Test Blockers

- 前后端运行态未启动或 URL 不成对。
- 缺少登录凭据、测试租户、生产组长、PQC 组长、生产员工、PQC 员工或电子签名测试能力。
- 缺少活跃生产工单、目标工序、设备、工作站、PQC 任务、QA 规程快照、正式批记录绑定或字段映射。
- 缺少 P0 E2E 脚本、spec、路由、页面入口、权限按钮或 trace 页面。
- 缺少 `P0_RUNTIME_DB_*` 只读核验环境，或运行态缺 P0 迁移、schema 字段、唯一约束、索引、历史断链检查 PASS 或与当前代码分支不一致。
- 测试账号只能通过超管越权路径完成普通一线、PQC 或组长动作。
- 任一写路径只能通过 API 或数据库直接调用完成时，真实 E2E 必须 BLOCKED。
