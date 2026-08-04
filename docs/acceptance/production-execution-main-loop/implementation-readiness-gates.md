# P0 生产执行主闭环实现前置门禁

## Purpose and Scope

本文档用于确保后续开发按 P0 BDD/TDD 文档实施时，可以真实达到“谁、在哪台设备、做了哪个工序、做了多少、质量结果怎样、签名是谁、进入哪个生产工单、班组长是否复核、最后如何进入批记录追溯”的目标。它不是新范围，只是实现前必须通过或明确阻塞的 M0 门禁。

## M0 Required Gates

| Gate | 必查项 | 通过标准 | 阻塞处理 |
| --- | --- | --- | --- |
| P0-M0-01 | 后端命令工作目录 | 所有 Maven 命令在 `D:\IntRuoyiWorktree\worktree_20260803_p0\IntRuoyiBackend` 执行，或显式使用该 worktree 下的 `IntRuoyiBackend/pom.xml`。 | 命令只因工作目录错误失败时，不得记录业务 RED；先修正文档/脚本。 |
| P0-M0-02 | 前端脚本入口 | `IntRuoyiFronted/package.json` 存在 `e2e:p0-production-execution-loop:static` 和 `e2e:p0-production-execution-loop:real`。 | 当前脚本缺失应作为第一个前端 RED，不能把 `ERR_PNPM_NO_SCRIPT` 当业务验证结果。 |
| P0-M0-03 | 前端 spec 文件 | 存在 P0 static spec 和 real E2E spec，且脚本指向这些正式文件。 | 缺 spec 时记录 E2E 前置 blocker，不得用 API wrapper 或旧脚本冒充。 |
| P0-M0-04 | 事件身份 | 生产提交、PQC 提交、复核、分配、完成、批记录字段审计都能保存正式来源 ID。 | 缺来源 ID 的链路不得进入 GREEN；trace 顶层必须 `complete=false`，对应分组必须 `BLOCKED`。 |
| P0-M0-05 | 批记录正式来源 | 已核对逐工序批记录表单绑定、字段映射和目标单元格。 | 缺绑定或映射时阻塞；禁止使用 `formBindings`、默认 `MAIN` 或工序开始配置替代。 |
| P0-M0-06 | 电子签名接口 | 生产提交、PQC 提交、生产组长复核、PQC 组长复核均有正式签名字段和测试签名能力。 | 缺签名接口或测试能力时阻塞；禁止用登录人、备注或确认弹窗替代。 |
| P0-M0-07 | 质量可分配状态 | 后端白名单明确哪些质量状态可进入 FIFO。 | 未冻结白名单时，除明确 PASS 外全部不可分配。 |
| P0-M0-08 | 真实 E2E 数据 | 测试租户、账号、活跃订单、PQC 任务、QA 规程、设备、工作站、批记录绑定、字段映射和清理方案已确认。 | 任一缺失时真实 E2E `BLOCKED`，不改用 mock、SQL 或 API-only。 |
| P0-M0-09 | worktree 路径和端口 | 开发、RED/GREEN、前端脚本和真实 E2E 均指向 `D:\IntRuoyiWorktree\worktree_20260803_p0`，端口使用 `8092/48092` 或已合入的 `8081/48081`。 | 跑到主工作区、错误分支或端口不成对时，结果无效，必须重跑。 |
| P0-M0-10 | 旧测试假绿 | PQC 旧单测已从 `never()` 改为期望创建 PQC 工序池事件，再记录 RED。 | 旧断言通过不得当 GREEN；必须先修改测试期望。 |
| P0-M0-11 | trace 完成算法 | trace 后端合同已冻结顶层 `complete`、六个分组、`sourceIds`、`blockers` 和 `candidateEvents`。 | 只返回页面摘要、空分组或前端计算 complete 时阻塞。 |
| P0-M0-12 | 签名验真 | 已确认签名服务能验证签名动作用途、签名员工、快照和当前操作者关系。 | 仅传 `signatureId`、登录用户或确认弹窗不得进入 GREEN。 |
| P0-M0-13 | Maven 命令形状 | 窄范围 `-Dtest=...` 命令追加 `"-Dsurefire.failIfNoSpecifiedTests=false"`，且目标 MES 测试确实执行。 | 目标测试执行前失败时记录 `COMMAND-BLOCKED`，不得用跳过测试或兄弟模块失败冒充 RED/GREEN。 |
| P0-M0-14 | 强制复核角色 | 后端明确生产组长复核和 PQC 组长复核的强制性、触发条件和 FIFO 前置关系。 | 未冻结时生产组长复核视为强制，PQC 组长复核必须返回配置状态；不得把待复核显示为已复核。 |
| P0-M0-15 | trace 成熟度拆分 | 区分 M3 initial trace GREEN 与 P0 完成验收；质量绑定、多候选、复核聚合、来源分配和字段审计必须各有 TDD。 | 只有六分组或 endpoint PASS 时，不得进入 M6 completed；记录为 initial GREEN 并继续 P0-T09A/P0-T09B/P0-T10。 |
| P0-M0-16 | PQC 结构化绑定 | PQC 到生产提交事件的绑定沉淀为正式字段或关系表，可查询、可索引、可唯一性校验。 | 绑定只存在于 rawPayload、备注、扩展 JSON 或页面摘要时，质量链路和 FIFO 均阻塞。 |
| P0-M0-17 | 幂等与并发 | 生产提交、PQC 提交、复核/确认和批记录回填均有业务幂等键或正式唯一约束，并覆盖重复/并发请求。 | 只能靠前端按钮禁用、浏览器请求 ID 或页面时间戳防重时阻塞。 |
| P0-M0-18 | TDD 证据缺口 | 每个已改生产行为都有原始 RED、同命令 GREEN 和回归证据；历史缺 RED 必须明确标记为证据缺口。 | 缺 RED 的 slice 不得在 M6 标记完成，除非定位到原始日志或用户明确接受带风险的证据缺口。 |
| P0-M0-19 | schema 正式持久化 | 新增正式字段、来源 ID、幂等键、索引和唯一约束同步到迁移 SQL、测试 schema、DO/Mapper 和 schema 合同测试。 | 只改 VO/DTO/前端类型或 rawPayload 时阻塞；不得把接口字段当落库事实。 |
| P0-M0-20 | 租户权限同源 | trace、复核、FIFO、批记录回填必须校验租户、操作者权限、生产工单、路线工序和 MES 工序同源。 | 跨租户、跨工单、越权复核或用超管绕过普通路径时阻塞。 |
| P0-M0-21 | 批记录回填顺序 | 批记录回填必须在正式分配和订单工序完成来源 ID 齐备后执行；异步回填必须持久化任务状态和来源幂等键。 | 提前写字段审计、无来源值、无来源分配或无幂等键时阻塞。 |
| P0-M0-22 | 闭环收口证据 | P0 最终验收必须能生成九个审计问题的脱敏证据包，且每项都有正式来源 ID、同源校验和只读复验入口。 | 只能展示页面摘要、截图、历史 ID 或分段 GREEN 时阻塞；不得标记 completed。 |
| P0-M0-23 | 迁移发布策略 | 每个新增正式 SQL 必须通过 release migration policy gate；字段收紧前必须检查未删除历史行是否缺正式来源 ID。 | SQL 缺 metadata、dependsOn、风险等级、policy gate PASS、历史断链 fail-fast blocker 或运行态迁移核验时阻塞；不得默认填值、解析 rawPayload 或跳过迁移。 |
| P0-M0-24 | 闭环根事件类型 | 最终 trace、FIFO、批记录回填和闭环证据包的 `processPoolEventId` 必须是 `PRODUCTION_SUBMIT` 生产提交根事件。 | PQC 事件只能作为质量子事件；无法解析唯一生产提交根事件时阻塞，不得把 `PQC_INSPECTION` 当完整闭环根。 |
| P0-M0-25 | PQC 数量勾稽 | 质量门禁必须冻结 PQC 检验数量、合格数量、可分配数量、已消耗数量和确认数量的勾稽规则。 | 合格数量不足、已消耗数量不明或只凭 `inspectionResult=SUCCESS` 时阻塞；不得写确认、分配、完成或批记录终态。 |
| P0-M0-26 | 运行态只读迁移核验 | 真实 E2E 写入前必须通过 `P0_RUNTIME_DB_HOST`、`P0_RUNTIME_DB_PORT`、`P0_RUNTIME_DB_NAME`、`P0_RUNTIME_DB_USER`、`P0_RUNTIME_DB_PASSWORD` 调用 `verify_p0_runtime_migration.py`，确认真实 MySQL required columns、indexes 和 historical checks PASS。 | 缺 DB env、验证器返回 `P0_RUNTIME_ENV_MISSING`、`P0_RUNTIME_SCHEMA_BLOCKED` 或历史断链 blocker 时阻塞；不得启动浏览器写入。 |
| P0-M0-27 | 真实 E2E 证据 freshness | `p0-real-e2e-evidence.md` 必须包含本轮 `Generated At`，且为 ISO UTC `Z` 时间戳；completion gate 必须能解析 `realE2e.generatedAt`，并阻塞早于或晚于 evidence 文件写入时间超过 6 小时的 evidence。 | 缺 `Generated At`、格式错误、手工复用旧 evidence、未来时间戳 evidence 或无法证明来自本轮 run 时阻塞；不得把旧 PASS evidence 当本轮完成证据。 |
| P0-M0-28 | 运行态 URL 配对 | `p0-real-e2e-evidence.md` 的 `Frontend` / `Backend` 必须成对使用当前 worktree `8092/48092`，或已合入 `int_main` 的 `8081/48081`；completion gate 必须解析 `realE2e.frontendUrl/backendUrl`。 | URL 缺失、端口混配、页面和接口指向不同运行态、或无法证明代码已合入对应运行态时阻塞；不得把混配 evidence 当 P0 主闭环 PASS。 |
| P0-M0-29 | 迁移策略证据内容 | `Migration Policy Evidence` 路径必须存在，且文件内容必须明确 `PASS`，不得包含 `BLOCKED/FAIL/FAILED` 标记；completion gate 必须解析 `realE2e.migrationPolicyEvidenceStatus`。 | 文件缺失、缺 PASS、包含失败标记、或只提供空文件/失败文件时阻塞；不得只因 evidence 路径存在就放行 M6。 |
| P0-M0-30 | 浏览器预检同源 | `Browser Preflight` 必须等于 `Frontend`，或以该 `Frontend` 后接 `/`、`?`、`#` 开始；completion gate 必须解析 `realE2e.browserPreflightUrl` 并拒绝其它端口或其它前端实例。 | 浏览器预检 URL 与 `Frontend` 不一致时返回 `P0_COMPLETION_BROWSER_PREFLIGHT_URL_MISMATCH`；不得把浏览器页面和 evidence 运行态不一致的结果当同一次 P0 PASS。 |
| P0-M0-31 | 目标请求后端同源 | 每个 `Target Request <LABEL>` 必须同时输出 `Hit` 和实际 `URL`；URL 必须等于 evidence `Backend` 加对应正式 endpoint，或以该 URL 后接 `?` 开始。 | 目标请求 URL 缺失、只写 `Hit=true`、或 URL 指向其它后端时返回 `P0_COMPLETION_TARGET_REQUEST_URL_MISMATCH`；不得把旧后端请求当同一次 P0 PASS。 |
| P0-M0-32 | 目标请求方法 | 每个 `Target Request <LABEL>` 必须输出实际 `Method`；生产提交、PQC 提交、班组长复核和 FIFO 确认必须是 `POST`，trace 查询必须是 `GET`。 | `Hit=true` 但 Method 缺失或与正式边界不一致时返回 `P0_COMPLETION_TARGET_REQUEST_METHOD_MISMATCH`；不得用 GET 预检、缓存或只读查询冒充写入闭环。 |
| P0-M0-33 | 目标请求 HTTP 状态 | 每个 `Target Request <LABEL>` 必须输出实际 `HTTP Status`；只要 `Hit=true`，对应目标请求状态必须为 200-299。 | `Hit=true` 但 HTTP Status 缺失、非数字或非 2xx 时返回 `P0_COMPLETION_TARGET_REQUEST_HTTP_STATUS_NOT_OK`；不得用失败请求、后端 4xx/5xx 或只看到请求发出冒充闭环成功。 |
| P0-M0-34 | 目标请求业务码 | 每个 `Target Request <LABEL>` 必须输出实际 `Business Code`；只要 `Hit=true`，CommonResult 业务码必须为 `0`。 | `Hit=true` 但 Business Code 缺失、非数字或非 0 时返回 `P0_COMPLETION_TARGET_REQUEST_BUSINESS_CODE_NOT_OK`；不得用 HTTP 2xx 掩盖业务失败。 |
| P0-M0-35 | result.json 目标请求唯一性 | `result.json.targetRequests` 必须按 `label + endpoint` 保留五个 canonical 目标请求，每个 required label 和 endpoint 只能出现一次；重复提交、重复 PQC 或重复 FIFO 确认只能通过专门 duplicate evidence 证明，不得混入主目标请求列表。 | 重复 label、重复 endpoint 或重复 `label + endpoint` 时返回 `P0_COMPLETION_REAL_E2E_RESULT_TARGET_REQUEST_DUPLICATE`；不得让重复响应覆盖或污染 Markdown 五个目标请求证据。 |
| P0-M0-36 | result.json 目标请求边界封闭 | `result.json.targetRequests` 只能包含五个 P0 required `label + endpoint` 边界；背景刷新、登录、权限预检、相似 URL 或未知 label 不得进入主目标请求列表。 | 出现任一非 required target request 时返回 `P0_COMPLETION_REAL_E2E_RESULT_TARGET_REQUEST_UNEXPECTED`；不得用混入的非主干请求包装或稀释 P0 五个目标请求证据。 |
| P0-M0-37 | result.json 目标请求数量 | `result.json.targetRequests` 必须精确等于五条 canonical P0 目标请求；不少于五条、不多于五条，也不得依赖 missing、duplicate 或 unexpected 分支间接推断数量。 | 数量不等于五条时返回 `P0_COMPLETION_REAL_E2E_RESULT_TARGET_REQUEST_COUNT_MISMATCH`；不得把四条主请求、六条混入请求或旧 run 残留请求当作完整主闭环证据。 |
| P0-M0-38 | result.json 目标响应身份边界 | `result.json.targetResponseIdentities` 只能包含五个 P0 required response identity key，且每个 key 必须对应正式目标请求响应身份；背景刷新、登录、权限预检、相似 URL 或旧 run 的额外身份不得进入主响应身份列表。 | 出现任一非 required target response identity key 时返回 `P0_COMPLETION_REAL_E2E_RESULT_TARGET_RESPONSE_UNEXPECTED`；不得用额外响应身份包装、稀释或替代五个正式目标响应身份。 |
| P0-M0-39 | result.json 目标响应身份数量 | `result.json.targetResponseIdentities` 必须精确等于五个 canonical P0 响应身份 key；不少于五个、不多于五个，也不得依赖 missing 或 unexpected 分支间接推断数量。 | 数量不等于五个时返回 `P0_COMPLETION_REAL_E2E_RESULT_TARGET_RESPONSE_COUNT_MISMATCH`；不得把四个响应身份、六个混入身份或旧 run 残留身份当作完整主闭环响应证据。 |
| P0-M0-40 | result.json 目标响应身份来源请求 | `result.json.targetResponseIdentities.<LABEL>.sourceRequestLabel` 必须等于同一个 canonical `<LABEL>`，证明该响应身份来自对应目标请求边界，而不是来自其它请求、背景刷新或旧 run。 | 来源 label 与当前 key 不一致时返回 `P0_COMPLETION_REAL_E2E_RESULT_TARGET_RESPONSE_REQUEST_LABEL_MISMATCH`；不得只用 key、field、value 三元组证明响应身份未串用。 |
| P0-M0-41 | result.json 目标请求证据 flush | `result.json.targetRequestEvidenceFlushed` 必须为 `true`，证明真实 E2E 已等待目标请求 response body 解析完成后才写入 `targetRequests` 的 Business Code 证据。 | 标志缺失或不是 `true` 时返回 `P0_COMPLETION_REAL_E2E_RESULT_TARGET_REQUEST_EVIDENCE_NOT_FLUSHED`；不得在异步 businessCode 解析未完成时写出 PASS result。 |
| P0-M0-42 | result.json 目标请求与响应身份同源集合 | `result.json.targetResponseIdentities` 的 key 集合必须与同一个 `result.json.targetRequests[].label` 观测集合完全一致，证明响应身份来自本轮真实观测目标请求，而不是来自手写 key、旧 run、诊断副本或背景请求。 | 两个集合不一致时返回 `P0_COMPLETION_REAL_E2E_RESULT_TARGET_RESPONSE_REQUEST_SET_MISMATCH`；不得只因五个 canonical key 独立存在就跳过请求/响应同源集合校验。 |
| P0-M0-43 | result.json 目标请求业务码存在性 | `result.json.targetRequests[*].businessCode` 必须逐条存在且可解析为数字；Markdown `Business Code=0` 不能替代 JSON artifact 中缺失、字符串 `MISSING`、空值或非数字业务码。 | 缺失或非数字时返回 `P0_COMPLETION_REAL_E2E_RESULT_TARGET_REQUEST_BUSINESS_CODE_MISSING`；不得只依赖 Markdown 行或 HTTP 2xx 推断 CommonResult 业务成功。 |
| P0-M0-44 | result.json 目标请求 HTTP 状态存在性 | `result.json.targetRequests[*].httpStatus` 必须逐条存在且可解析为数字；Markdown `HTTP Status=200` 不能替代 JSON artifact 中缺失、字符串 `MISSING`、空值或非数字 HTTP 状态。 | 缺失或非数字时返回 `P0_COMPLETION_REAL_E2E_RESULT_TARGET_REQUEST_HTTP_STATUS_MISSING`；不得只依赖 Markdown 行或业务码推断目标请求真实收到 HTTP 响应。 |
| P0-M0-45 | result.json 目标请求方法存在性 | `result.json.targetRequests[*].method` 必须逐条存在且非空；Markdown `Method=POST/GET` 不能替代 JSON artifact 中缺失、空值或占位的 HTTP 方法。 | 缺失或为空时返回 `P0_COMPLETION_REAL_E2E_RESULT_TARGET_REQUEST_METHOD_MISSING`；不得只依赖 Markdown 行或 endpoint 推断目标请求实际方法。 |
| P0-M0-46 | result.json 目标请求 URL 存在性 | `result.json.targetRequests[*].url` 必须逐条存在且非空；Markdown `URL=<Backend>/<endpoint>` 不能替代 JSON artifact 中缺失、空值或占位的 URL。 | 同 label 目标请求缺 URL 时返回 `P0_COMPLETION_REAL_E2E_RESULT_TARGET_REQUEST_URL_MISSING`；不得只依赖 label、endpoint 或 Markdown 行推断目标请求真实 URL。 |
| P0-M0-47 | result.json 目标请求 label 存在性 | `result.json.targetRequests[*].label` 必须逐条存在且非空；Markdown `Target Request <LABEL>` 不能替代 JSON artifact 中缺失、空值或占位的 label。 | 缺失或为空时返回 `P0_COMPLETION_REAL_E2E_RESULT_TARGET_REQUEST_LABEL_MISSING`；不得只依赖 URL endpoint、数组顺序或 Markdown 行推断目标请求身份。 |
| P0-M0-48 | result.json 目标请求对象类型 | `result.json.targetRequests[*]` 必须逐条是 JSON object；字符串、数组、数字或其它非对象项不能进入主目标请求证据列表。 | 任一项不是 JSON object 时返回 `P0_COMPLETION_REAL_E2E_RESULT_TARGET_REQUEST_OBJECT_MISSING`；不得把非对象项泛化为 unexpected 后继续倒推目标请求完整性。 |
| P0-M0-49 | result.json 目标响应身份对象类型 | `result.json.targetResponseIdentities.<LABEL>` 必须逐项是 JSON object；字符串、数组、数字或其它非对象项不能进入主响应身份列表。 | 任一响应身份不是 JSON object 时返回 `P0_COMPLETION_REAL_E2E_RESULT_TARGET_RESPONSE_OBJECT_MISSING`；不得把非对象响应身份泛化为 missing 后继续倒推响应身份完整性。 |
| P0-M0-50 | result.json 目标响应身份字段名存在性 | `result.json.targetResponseIdentities.<LABEL>.field` 必须逐项存在且非空；Markdown `Target Response <LABEL> <field>` 不能替代 JSON artifact 中缺失、空值或占位的字段名。 | 缺失或为空时返回 `P0_COMPLETION_REAL_E2E_RESULT_TARGET_RESPONSE_FIELD_MISSING`；不得把字段名未捕获泛化为 field mismatch 后继续误判为仅字段不一致。 |
| P0-M0-51 | result.json 目标响应身份值存在性 | `result.json.targetResponseIdentities.<LABEL>.value` 必须逐项存在且可解析为正整数；Markdown `Target Response <LABEL> <field>=<id>` 不能替代 JSON artifact 中缺失、空值、非数字或非正数的响应 ID。 | 缺失或非正整数时返回 `P0_COMPLETION_REAL_E2E_RESULT_TARGET_RESPONSE_VALUE_MISSING`；不得把响应 ID 未捕获泛化为 value mismatch 后继续误判为仅值不一致。 |
| P0-M0-52 | result.json 目标响应身份来源请求存在性 | `result.json.targetResponseIdentities.<LABEL>.sourceRequestLabel` 必须逐项存在且非空；Markdown 请求/响应 key 不能替代 JSON artifact 中缺失、空值或占位的来源请求 label。 | 缺失或为空时返回 `P0_COMPLETION_REAL_E2E_RESULT_TARGET_RESPONSE_SOURCE_REQUEST_LABEL_MISSING`；不得把来源请求 label 未捕获泛化为 request label mismatch 后继续误判为仅来源串用。 |

## Command Conventions

```powershell
# 当前 worktree 后端定向测试：工作目录必须是 D:\IntRuoyiWorktree\worktree_20260803_p0\IntRuoyiBackend
mvn -pl yudao-module-mes -am "-Dtest=<TestClassOrPattern>" "-Dsurefire.failIfNoSpecifiedTests=false" test

# 前端静态合同和真实 E2E：工作目录必须是 D:\IntRuoyiWorktree\worktree_20260803_p0\IntRuoyiFronted
pnpm e2e:p0-production-execution-loop:static
pnpm e2e:p0-production-execution-loop:real

# 正式 SQL 迁移策略门禁：工作目录必须是 D:\IntRuoyiWorktree\worktree_20260803_p0
python -X utf8 IntRuoyiBackend\script\release\run-release-migration-policy-gate.py --sql-root IntRuoyiBackend\sql\mysql

# 真实运行态只读迁移核验：工作目录必须是 D:\IntRuoyiWorktree\worktree_20260803_p0
python -X utf8 IntRuoyiBackend\script\p0\verify_p0_runtime_migration.py
```

PowerShell 命令不得使用 `&&`。若需要连续执行，逐条运行并分别记录退出码；不得用后一个 PASS 掩盖前一个 FAIL。`-DskipTests`、`-Dmaven.test.skip=true` 或跳过目标 MES 测试的命令不能作为 P0 GREEN 证据。

## Development Entry Order

1. 先完成 P0-M0 前置门禁，尤其是脚本/spec、事件身份和签名接口冻结。
2. 后端先写 RED：PQC 入工序池事件、复核签名 schema、复核签名服务、统一 trace。
3. 后端 GREEN 只能做最小正式链路，不得加 fallback、默认成功或 mock。
4. 前端先写 static RED：真实入口、请求字段、签名字段、trace 页面、错误展示。
5. 最后跑真实 E2E；浏览器写入前必须先让运行态只读迁移核验 PASS；若真实数据、入口或 `P0_RUNTIME_DB_*` 缺失，记录 `BLOCKED` 和解除条件；若前置已齐但页面步骤或断言未实现，记录 `FAIL/RED`；不得改写成 PASS。

## Milestone Entry and Exit Gates

| Milestone | Entry Gate | Exit Gate |
| --- | --- | --- |
| M1 PQC 入池 | P0-M0-01、04、06、07、10、12、16 已核对；旧 PQC 测试已准备改为事件创建 RED。 | PQC 请求、命令、服务、前端 payload 均携带正式设备/工作站/签名/幂等字段；后端测试捕获 `MesProcessPoolCreatePqcInspectionReqDTO` 并 PASS；结构化绑定缺口不得用 rawPayload 代替。 |
| M2 复核签名 | M1 GREEN；复核字段合同、强制复核角色、Maven 命令形状和 TDD 证据要求已冻结，缺 schema 或迁移时先读数据库规则。 | 生产组长和 PQC 组长复核/确认均要求正式签名；缺签名、签名主体不一致、越权复核和强制复核未完成仍 FIFO 均 RED/GREEN 覆盖；原始 RED 证据缺失时保留 blocker。 |
| M3 统一 trace | M1、M2 GREEN；trace DTO 六分组、`sourceIds`、`blockers`、`candidateEvents` 已冻结；P0-M0-15/16 已确认不会把 initial GREEN 或 rawPayload-only 绑定当 DoD。 | 按 `processPoolEventId` 查询返回完整结构；缺 PQC、缺复核、缺批记录投影、多候选、rawPayload-only 绑定均 `complete=false`；质量绑定、强制复核聚合和批记录来源分配的成熟度测试至少进入 RED/GREEN 或明确 BLOCKED。 |
| M4 幂等/FIFO/批记录 | 质量状态白名单、PQC 数量勾稽、生产提交根事件类型、活跃订单、批记录正式来源、P0-M0-17 幂等并发、P0-M0-19 schema 持久化、P0-M0-21 回填顺序、P0-M0-23 迁移策略门禁已核对。 | 重复提交/重复确认不产生重复事实；FIFO 只消耗绑定到生产提交根事件、质量状态可分配且合格数量覆盖确认数量的片段；完成后正式批记录字段审计可追溯并包含来源值、旧值、新值、来源分配和幂等键；新增 SQL policy gate 和 schema 合同均 PASS。 |
| M5 前端/E2E | 后端合同 GREEN；前端静态合同 RED 已覆盖真实入口、请求字段、签名字段、trace 错误展示、schema 版本核验、P0-M0-26 运行态只读迁移核验和租户权限边界。 | static PASS；real E2E 在前置或 `P0_RUNTIME_DB_*` 缺失时 BLOCKED，在运行态迁移核验和业务前置齐备后必须 PASS 完整主路径，不能只登录、只打开 trace 或用超管越权路径替代。 |
| M6 收尾验证 | M1-M5 按当前可用前置记录 GREEN/BLOCKED/FAIL；闭环证据包字段形状已冻结；验证范围与任务文档一致；P0-M0-27 freshness、P0-M0-28 URL 配对、P0-M0-29 迁移策略证据内容、P0-M0-30 浏览器预检同源、P0-M0-31 目标请求后端同源、P0-M0-32 目标请求方法、P0-M0-33 目标请求 HTTP 状态和 P0-M0-34 目标请求业务码门禁已纳入 completion gate。 | 定向回归、文档验证、`git diff --check`、闭环证据包或 blocker 归档完成；未完成实现则保持 `in_progress` 或明确 blocker，不标记 completed。 |

## Document-Driven Development Checklist

每次开发一个 P0 slice 前，执行人必须在 `execution-log.md` 明确记录：

- 本 slice 对应的 BDD 场景名、TDD Step、追溯矩阵行和测试数据前置。
- RED 命令的工作目录、预期失败原因，以及为什么该失败证明的是当前缺口而非环境问题。
- Maven 命令若未执行目标测试，必须记录为 `COMMAND-BLOCKED` 并列出失败模块；修正后再记录真正 RED/GREEN。
- GREEN 的最小实现范围；若需要 schema、SQL、菜单、运行态或真实 E2E，先读取对应规则文件。
- 本 slice 是否触及 `formBindings`、批记录表单、工序开始配置；若触及，必须按三类配置术语契约分别验证。
- 完成后 trace 六分组中哪些从 `BLOCKED` 变为完成，哪些仍应保持 `BLOCKED`。
- 如果本 slice 只完成 endpoint、DTO 或初始聚合，必须标注 `initial GREEN`，并列出仍待 P0-T09A/P0-T09B/P0-T10 覆盖的正式来源 ID。
- 如果本 slice 使用 rawPayload、扩展 JSON 或摘要字段作为过渡读取，必须标注为 `BLOCKED` 或 `DIAGNOSTIC-ONLY`，不能进入完成谓词。
- 本 slice 是否新增正式字段、索引、唯一约束或迁移；若新增，必须同步记录 schema 合同 RED/GREEN。
- 本 slice 是否新增正式 SQL；若新增，必须记录 release migration policy gate 命令、结果、metadata、dependsOn、历史断链检查和运行态迁移核验计划。
- 本 slice 是否准备执行真实 E2E；若准备执行，必须先记录 `verify_p0_runtime_migration.py` 的真实 MySQL 只读核验结果，缺 `P0_RUNTIME_DB_*` 或验证器非 PASS 时只能记录 `BLOCKED`。
- 本 slice 的 `processPoolEventId` 是否为生产提交根事件；若使用 PQC 事件、复核事件或批记录事件作为输入，必须记录如何解析到唯一生产提交根事件或为什么阻塞。
- 本 slice 是否影响 PQC 检验数量、合格数量、可分配数量、已消耗数量、确认数量或分配数量；若影响，必须记录数量勾稽 RED/GREEN 和失败时不写终态的证据。
- 本 slice 是否涉及跨租户、跨工单、跨工序或权限边界；若涉及，必须记录正向和负向同源校验。
- 本 slice 是否影响九个审计问题中的任一答案；若影响，必须更新闭环证据包字段、来源 ID 和只读复验断言。
- 未跑的验证命令必须记录原因和影响，禁止写成 PASS。

## Verification Evidence Requirements

- 每条 RED 必须包含命令、工作目录、退出状态和预期失败原因。
- 每条 GREEN 必须复跑同一命令并 PASS。
- 每条 `COMMAND-BLOCKED` 必须包含命令、失败阶段、失败模块、目标测试未执行的证据和后续合法命令。
- E2E PASS 必须包含真实页面路径、测试租户标签、数据前缀、目标写请求数量、只读核验结果和清理/保留证据。
- E2E PASS 必须包含本轮 `Generated At` ISO UTC `Z` 时间戳，并由 completion gate 解析；时间戳早于或晚于 evidence 文件写入时间超过 6 小时、旧 evidence 或未来时间戳 evidence 不得复用。
- E2E PASS 必须包含成对 `Frontend` / `Backend` URL，并由 completion gate 校验为 `8092/48092` 或 `8081/48081`；混配 URL 不得作为同一次闭环 evidence。
- E2E PASS 必须包含与 `Frontend` 同源的 `Browser Preflight` URL，并由 completion gate 校验；浏览器预检落到其它端口或前端实例时不得作为同一次闭环 evidence。
- E2E PASS 必须包含每个目标请求的实际 URL、Method、HTTP Status 和 Business Code，并由 completion gate 校验 URL 属于同一个 `Backend`、Method 匹配正式边界、HTTP Status 为 2xx 且 Business Code 为 `0`；只写命中布尔值、URL 指向其它后端、Method 错误、HTTP Status 非 2xx 或业务码非 0 时不得作为同一次闭环 evidence。
- E2E BLOCKED 必须包含缺失前置、影响范围和解除条件。
- 证据缺口必须明确标记 `EVIDENCE-GAP`，并说明缺哪条 RED/GREEN/回归证据、影响哪个 milestone、是否阻塞 M6。
- schema 证据必须包含迁移文件、测试 schema、DO/Mapper、索引或唯一约束核验命令；缺任一项时不得写成 GREEN。
- migration policy 证据必须包含 `run-release-migration-policy-gate.py` 命令、目标 SQL 或全量 SQL root、PASS/FAIL、失败阶段和历史断链处理结果；单文件诊断未包含依赖闭包导致的缺依赖错误只能记为 `COMMAND-BLOCKED`，缺全量 policy gate PASS 或证据文件包含 `BLOCKED/FAIL/FAILED` 标记时不得写成 schema GREEN。
- runtime migration 证据必须包含 `verify_p0_runtime_migration.py` 命令、脱敏 DB 目标、required columns、required indexes、historical checks 和 PASS/BLOCKED/FAIL；缺 env 或 schema blocked 不得写成真实 E2E PASS。
- 租户权限证据必须包含正向授权路径和跨租户/跨工单/跨工序负向路径；缺负向路径时不得写成完整 trace PASS。
- 闭环证据必须逐项覆盖九个 P0 审计问题；缺任一正式来源 ID、同源校验或只读复验入口时，M6 不得 completed。
- 验证报告不得记录密码、token、cookie、Authorization、电子签名密码或私钥。

## Non-Negotiable Blockers

- 无 `processPoolEventId` 或正式事件关联 ID。
- PQC 结果没有工序池质量事件。
- PQC 到生产提交的绑定只存在于 rawPayload 或页面摘要。
- 复核或确认分配没有正式电子签名。
- FIFO 使用非活跃订单、质量未知数量或默认合格。
- FIFO 只凭 `inspectionResult=SUCCESS` 放行，未校验 PQC 合格数量覆盖确认数量。
- trace 或闭环证据包把 `PQC_INSPECTION` 事件 ID 当作完整闭环根事件。
- 新增正式字段缺迁移、测试 schema、DO/Mapper、索引或唯一约束。
- 新增正式 SQL 缺 release migration policy gate PASS，或历史未删除行缺正式来源 ID 时迁移仍默认成功。
- migration policy evidence 文件缺失、缺明确 PASS、或包含 `BLOCKED/FAIL/FAILED` 标记。
- 真实 E2E 浏览器写入前缺 `P0_RUNTIME_DB_*`，或运行态迁移验证器未对真实 MySQL 返回 PASS。
- 真实 E2E evidence 缺 `Generated At`、时间戳格式错误、早于或晚于 evidence 文件写入时间超过 6 小时，或无法证明来自本轮 run。
- 真实 E2E evidence 的 `Frontend` / `Backend` URL 缺失、不成对或跨运行态混配。
- 真实 E2E evidence 的目标请求 Method 缺失或与正式 POST/GET 边界不一致。
- 真实 E2E evidence 的目标请求 HTTP Status 缺失、非数字或非 2xx。
- 真实 E2E evidence 的目标请求 Business Code 缺失、非数字或非 0。
- trace、FIFO 或批记录查询跨租户、跨工单、跨工序拼接事实。
- 重复提交或并发确认会产生第二条有效终态事实。
- 批记录回填不是正式逐工序批记录表单绑定。
- trace 需要页面拼接、备注文本、默认槽位或截图才能回答 P0 审计问题。
- trace 缺少顶层 `complete` 布尔值、机器可读 blockers 或正式 sourceIds。
- 真实 E2E 只完成前置检查、健康检查或登录，没有完成主写链路和 trace 断言。
- 最终证据包不能逐项回答九个审计问题，或答案需要页面截图、历史 ID、rawPayload-only、`formBindings`、人工拼接才能成立。
