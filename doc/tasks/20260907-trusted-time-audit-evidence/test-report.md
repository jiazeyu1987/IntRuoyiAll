# 测试报告：可信时间最小闭环

## Status

P1 PASS：签名时间边界修复通过独立验证，允许进入 P2。

## P1 独立验证范围

- P1-AC1：正式签名展示时间固定使用服务器 `signedAt`，用户选择时间仅作为业务发生时间。
- P1-AC2：两条服务端签名持久化链路及对应回归测试已落地。
- P1-AC3：正式列表与签名格不再回退到 `selectedSignedAt`，业务字段文案已明确为“业务发生时间”。
- P1-AC4：完成后端定向测试、前端最小静态合同、变更边界检查及 `git diff --check`。

## Requirement-to-Evidence

| 验收项 | 结论 | 独立证据 |
| --- | --- | --- |
| P1-AC1 | PASS | `MesProBatchRecordExecutionSignatureServiceTest` 断言 `signatureDisplayAt == signedAt` 且不等于 `selectedSignedAt`；服务实现的 `displayAt` 固定取传入的服务器 `signedAt`。 |
| P1-AC2 | PASS | 签名服务测试全类 13/13 通过；批执行关闭和质量拒收两条聚焦测试 2/2 通过。 |
| P1-AC3 | PASS | 前端静态合同 8/8 通过；`ApprovalDetailPage.vue`、`SignaturePage.vue` 正式列表读取 `signedAt`，`ExecutionPage.vue` 签名格排序和展示读取 `signedAt`；三个页面均保留独立“业务发生时间”展示或录入。 |
| P1-AC4 | PASS | 下列命令均独立执行并通过；diff 仅涉及 P1 的 8 个源码/测试文件，无 schema、迁移或历史数据更新文件。 |

## Verification Evidence

1. 后端签名服务全类：

```powershell
mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordExecutionSignatureServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

结果：PASS，`Tests run: 13, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`。

2. 批执行签名相关聚焦回归：

```powershell
mvn -pl yudao-module-mes -am "-Dtest=MesProEdhrBatchExecutionServiceTest#closeCreatesArchiveWorkTaskAfterBatchClosedWhenFinalInspectionDossierPending+qualityReject_unarchivedBatch_marksRejectedSignsAndCancelsActiveTasks" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

结果：PASS，`Tests run: 2, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`。

3. 前端最小静态合同：使用 PowerShell `Get-Content -Raw -Encoding utf8` 读取 `ApprovalDetailPage.vue`、`ExecutionPage.vue`、`SignaturePage.vue` 和 `signatureTime.ts`，执行 8 项正向/负向断言。

结果：PASS，确认正式列表/签名格只读取 `signedAt`，旧 `signatureDisplayAt || selectedSignedAt || signedAt` 回退链不存在，“业务发生时间”及其校验文案存在。

4. 变更边界：

```powershell
git diff --name-only
git diff --check
```

结果：PASS。变更仅覆盖计划列出的两个 Java 服务、两个 Java 测试和四个前端文件；未修改 SQL、数据库 schema、迁移、历史记录更新脚本或哈希算法表达式。`git diff --check` 无空白错误，仅有 Windows 工作副本 LF/CRLF 提示。

## Boundary Notes

- `selectedSignedAt`、`selectedTimeZone`、`selectedTimeReason` 仍按独立字段持久化；没有删除或用 `signedAt` 覆盖。
- 原有 `selectedTimeAuditHash` 计算字段、策略版本和持久化字段未删除；本阶段没有修改既有历史记录或执行数据迁移。
- `IntRuoyiFronted/node_modules` 不存在，因此未执行 `pnpm ts:check`。P1 独立门禁采用开发计划要求的最小前端静态合同；未将类型检查记为通过，也未安装依赖或使用 fallback。
- `MesProEdhrBatchExecutionServiceTest` 全类在执行者回归中存在 2 个与本次时间边界无关的既有失败；本轮按 P1 授权范围独立复跑两条时间相关方法并通过，未修改无关产品代码或测试。

## P2 独立验证结果

P2 FAIL：固定节点采集、巡检聚合、三文件 ZIP 和只读导出主路径已经落地，但存在一个失败关闭缺陷及多项未形成可执行回归证据的失败路径，当前不允许进入 P3。

### Findings

1. `RuntimeTrustedTimeParser` 解析并对外保存 `RMS offset`，但 `validate` 只检查 `Last offset` 是否缺失或超阈值。`RMS offset` 缺失或明显超出批准阈值时仍可能得到 `PASS`，与 P2 数据合同中“保存 Last/RMS offset”和“证据缺失、偏差超限均 BLOCKED”冲突。证据：`RuntimeTrustedTimeParser.java:83` 赋值 RMS；`:128-131` 仅验证 Last offset。
2. 执行者记录声称 18 项测试覆盖同步状态、Stratum、UTC 格式、接口不存在 ID 和 HTML 转义，但当前测试没有这些失败用例。现有解析器测试只覆盖正常、无选中源、Last offset 超限、tracking 缺失和 Leap 异常；导出测试没有恶意 HTML 字符，接口测试只有映射/权限反射，且不存在的巡检 ID 没有服务或接口测试。静态代码中可看到对应分支或转义调用，但严格 TDD 与 P2-AC4 要求的是可执行回归证据，不能用未执行分支替代。

### Requirement-to-Evidence

| 验收项 | 结论 | 独立证据 |
| --- | --- | --- |
| P2-AC1 | FAIL | 正式服 `172.30.30.57`、审查服 `172.30.30.59`、巡检持久化及指定巡检 ZIP 已落地；但 RMS 偏差证据可缺失/超限后仍 PASS，最小闭环尚未失败关闭。 |
| P2-AC2 | FAIL | `trusted-time-prod`、`trusted-time-audit` 会写入现有 `inspection-runs.json` 并参与 pre/post release 和总状态；RMS 偏差验证缺口使“时间状态解析与巡检必检项”不完整。 |
| P2-AC3 | PASS | `GET /infra/runtime-control/inspection-runs/{id}/time-evidence.zip` 使用 query 权限，从 `inspectionRunStore.get(id)` 读取并固定生成 `审查摘要.html`、`原始证据.json`、`SHA256SUMS.txt`；测试证明 SHA-256 正确、同一巡检重复导出字节一致且不重新采集。 |
| P2-AC4 | FAIL | 定向测试 18/18、PowerShell AST、evidence validator、diff check 均通过，但上述必要失败路径缺少回归测试，且 RMS 行为本身不符合失败关闭合同。 |

### Verification Evidence

1. P2 Maven 定向测试：

```powershell
mvn -pl yudao-module-infra -am "-Dtest=RuntimeOpsTrustedTime*Test,RuntimeOpsInspection*Test,RuntimeControlSpringWiringTest,RuntimeControlCanonicalContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

结果：PASS，`Tests run: 18, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`。该结果只证明当前 18 项断言通过，不覆盖 Findings 中列出的遗漏路径。

2. PowerShell AST：PASS，`show-int-ruoyi-trusted-time.ps1` 无语法错误；本轮未执行脚本、未连接远程服务器。

3. Backend evidence validator：

```powershell
python -X utf8 C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260907-trusted-time-audit-evidence/backend-api-evidence.md
```

结果：PASS，`Backend API evidence is valid.`；该验证器只验证证据文档结构，不证明其中覆盖声明为真。

4. `git diff --check`：PASS，仅有 LF/CRLF 提示；另对全部未跟踪 P2 文件执行尾随空白扫描，PASS。

5. 原巡检测试重命名核对：PASS。HEAD 中 `RuntimeInspectionServiceImplTest` 的业务健康 NO_GO、探针 PASS、pre/post release NO_GO、保存后按 ID 回读等断言均保留在 `RuntimeOpsInspectionServiceImplTest`，并新增两节点时间证据及 JSON 持久化断言，没有丢失旧覆盖。

### Required Corrections Before Re-test

- 对 RMS offset 明确合同并失败关闭：至少缺失时 BLOCKED；若批准阈值同时约束 RMS，则超限也必须 BLOCKED，并增加对应 RED/GREEN。
- 增加同步状态为 `no`、NTP service 非 `active`、Stratum 缺失/无效、服务器/数据库/检查 UTC 缺失或格式无效的参数化失败测试。
- 增加不存在巡检 ID 导出失败测试，并核对正式错误码。
- 增加 HTML 特殊字符/脚本片段转义测试，证明摘要不会注入未转义内容。
- 修正 `backend-api-evidence.md` 中超出现有测试覆盖范围的声明，或在补齐测试后保留该声明。

## P2 修订后独立复验

P2 PASS：本节结论取代上一轮 P2 FAIL。上轮发现的 RMS 失败关闭缺陷已修复，缺失的失败路径均已形成可执行回归证据；P2-AC1 至 P2-AC4 全部通过，允许进入 P3。

### Requirement-to-Evidence

| 验收项 | 结论 | 独立证据 |
| --- | --- | --- |
| P2-AC1 | PASS | Runtime Control 固定采集正式服 `172.30.30.57` 与审查服 `172.30.30.59`，保存到现有巡检记录；指定巡检可生成固定三文件 ZIP。 |
| P2-AC2 | PASS | Last/RMS offset 均为必填并受相同批准阈值约束；命令失败、无选中源、Leap 异常、同步状态异常、NTP 未运行、Stratum 缺失/无效、三类 UTC 缺失/无效及阈值缺失均被测试证明为 BLOCKED；时间项参与 pre/post release 与巡检总状态聚合。 |
| P2-AC3 | PASS | 导出接口使用 `infra:runtime-control:query`，只读取 `inspectionRunStore.get(id)`；固定生成 `审查摘要.html`、`原始证据.json`、`SHA256SUMS.txt`，SHA-256 正确、重复导出字节一致、不重新采集、HTML 内容正确转义，不存在 ID 返回正式参数错误。 |
| P2-AC4 | PASS | P2 定向 Maven 31/31、PowerShell AST、backend evidence validator、`git diff --check` 和未跟踪文件尾随空白检查全部通过；执行证据已分别记录首轮 18 项实际范围和修订后 31 项范围，没有继续用首轮结果冒充新增覆盖。 |

### Re-verification Evidence

1. P2 定向测试：

```powershell
mvn -pl yudao-module-infra -am "-Dtest=RuntimeOpsTrustedTime*Test,RuntimeOpsInspection*Test,RuntimeControlSpringWiringTest,RuntimeControlCanonicalContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

结果：PASS，`Tests run: 31, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`。

2. 上轮问题专项核对：

- `RuntimeTrustedTimeParser.java:132-135` 对 RMS 缺失和超阈值明确加入失败原因。
- `RuntimeOpsTrustedTimeParserTest` 的 12 组参数化失败用例覆盖 RMS 缺失/超限、同步状态、NTP 状态、Stratum 缺失/无效，以及 server/database/checked UTC 缺失/无效。
- `RuntimeOpsTrustedTimeEvidenceExportTest` 证明 HTML 中 `<script>`、`&`、`"` 被转义，并证明巡检 ID `999` 不存在时返回 `RUNTIME_CONTROL_ACTION_PARAMETER_INVALID`。
- 原 `RuntimeInspectionServiceImplTest` 重命名后的业务健康、探针、pre/post release、总状态和持久化断言继续保留。

3. PowerShell AST：PASS；只进行语法解析，未执行远程脚本、未连接正式服或审查服。

4. Backend evidence validator：PASS，`Backend API evidence is valid.`。

5. `git diff --check`：PASS，仅有 Windows LF/CRLF 提示；未跟踪文件尾随空白检查 PASS。

### Boundary

- 本次结论只覆盖 P2 本地代码与测试验收。正式服、审查服真实 chrony 状态、受控 NTP 地址、批准阈值和主机密钥仍属于 P4 经授权环境验证范围。

## P3 独立验证结果

P3 FAIL：现有运行控制台已经具备可信时间表格、执行巡检和按当前巡检 ID 导出 ZIP 的主路径，但用户可见环境口径仍不统一，且开发计划明确要求的类型检查没有执行，当前不允许进入 P4。

### Findings

1. 可信时间表格将技术值 `backup` 映射为“审查环境”，而本任务已明确要求用户可见口径统一为“审查服”。证据：`IntRuoyiFronted/src/views/infra/runtime-control/index.vue:937`。技术枚举和接口值继续使用 `backup` 是正确的，但展示值应为“审查服”。
2. `IntRuoyiFronted/node_modules` 不存在，因此无法执行 P3 Verification Gate 要求的前端类型检查。`frontend-feature-evidence.md` 已如实记录未执行，但这不能替代 `development-plan.md` 中“前端静态测试和类型检查 GREEN”的放行条件。
3. 静态合同对“导出不触发新巡检”的验证范围不完整。`runtime-control-trusted-time-static.spec.js` 用非贪婪正则截取 `exportTimeEvidence`，会在 `if (!inspectionId)` 的第一个右花括号处结束，因此只检查 ID guard，无法检测 guard 之后新增的 `runRuntimeControlInspection` 调用。当前产品代码经人工检查确实没有触发巡检，但该关键约束尚未被稳定回归测试锁定。

### Requirement-to-Evidence

| 验收项 | 结论 | 独立证据 |
| --- | --- | --- |
| P3-AC1 | FAIL | 页面已展示巡检结果并按返回的 `inspectionRun.id` 导出，但审查服在可信时间表格中显示为“审查环境”，不符合统一用户口径。 |
| P3-AC2 | FAIL | 环境、节点、时间源、Last/RMS 偏差、Leap、检查时间、状态和说明字段均已展示；其中 `backup` 的用户可见映射不符合“审查服”口径。 |
| P3-AC3 | PASS | 无 ID 时按钮禁用且 handler 二次守卫；BLOCKED/NO_GO 不参与禁用条件；导出读取当前保存 ID，不重新执行巡检；失败进入 `reportActionError`，执行巡检使用 `operate` 权限，导出后端使用 `query` 权限。 |
| P3-AC4 | FAIL | Node 语法检查、静态合同、证据验证器和 diff check 通过，但类型检查缺失，且静态合同没有完整锁定“导出不触发巡检”。 |

### Verification Evidence

1. `node --check tests/e2e/runtime-control-trusted-time-static.spec.js`：PASS。
2. `node tests/e2e/runtime-control-trusted-time-static.spec.js`：PASS，输出 `PASS: runtime control trusted-time UI contract is present`。该结果受 Findings 3 的合同截取缺陷限制。
3. `validate_frontend_feature.py --evidence .../frontend-feature-evidence.md`：PASS；验证器只证明证据文档结构完整，不证明所有验收行为成立。
4. `validate_frontend_feature.py --self-test`：PASS。
5. `git diff --check`：PASS，仅有 Windows LF/CRLF 提示，无空白错误。
6. `IntRuoyiFronted/node_modules`：不存在；未安装依赖、未使用替代环境、未把 `pnpm ts:check` 记为通过。
7. 响应式静态检查：面板宽度为 `100%`，标题区在 `768px` 以下改为纵向，操作区可换行，Element Plus 表格承载固定列宽；未执行浏览器或真实 E2E，因此这里只能证明源码约束，不作为实际视口渲染证据。

### Required Corrections Before Re-test

- 将可信时间环境映射中的 `backup: '审查环境'` 改为用户统一口径 `backup: '审查服'`，技术字段和值保持 `backup` 不变，并让静态合同精确锁定该映射。
- 调整静态合同的 handler 截取边界，使检查覆盖完整 `exportTimeEvidence` 函数体，再断言其中不存在 `runRuntimeControlInspection`。
- 恢复项目依赖后执行正式 `pnpm ts:check`；依赖缺失时不得用其他工具冒充类型检查通过。

### Boundary

- 本轮没有启动服务、执行 E2E、连接远程服务器或修改产品代码；只追加本独立测试结论。

## P3 修订后独立复验

P3 PASS：本节结论取代上一轮 P3 FAIL。审查服用户口径、完整导出函数静态合同和正式前端类型检查均已补齐，P3-AC1 至 P3-AC4 全部通过，允许进入 P4。

### Requirement-to-Evidence

| 验收项 | 结论 | 独立证据 |
| --- | --- | --- |
| P3-AC1 | PASS | 现有运行控制台展示当前巡检的可信时间检查；执行巡检将后端返回值保存为 `inspectionRun`，导出严格使用当前 `inspectionRun.id`。 |
| P3-AC2 | PASS | 页面展示环境、节点、时间源、Last/RMS 偏差、Leap、检查时间、状态和说明；技术值 `backup` 映射为“审查服”，页面源码不存在 `Backup` 标签、“备份服务器”、“备用服务器”、“备份服”或“审查环境”等旧展示称谓，`backup` 技术枚举及 `Backup/...` NAS 路径保持不变。 |
| P3-AC3 | PASS | 无 ID 时按钮禁用且 handler 二次守卫；禁用条件不依赖 PASS/BLOCKED/NO_GO 状态，异常巡检仍可导出；完整 `exportTimeEvidence` 只调用指定 ID 下载接口且不调用 `runRuntimeControlInspection`；下载异常进入 `reportActionError`。执行按钮使用 `operate` 权限，导出接口使用运行控制台只读 `query` 权限。 |
| P3-AC4 | PASS | `pnpm ts:check`、Node 语法检查、静态合同、frontend evidence validator/self-test 和 `git diff --check` 全部通过。 |

### Re-verification Evidence

1. `pnpm ts:check`：PASS，`vue-tsc --noEmit -p tsconfig.relaxed.json` 退出码 0。本轮直接使用目标 worktree 按锁文件恢复的依赖，没有安装或切换替代工具。
2. `node --check tests/e2e/runtime-control-trusted-time-static.spec.js`：PASS。
3. `node tests/e2e/runtime-control-trusted-time-static.spec.js`：PASS，输出 `PASS: runtime control trusted-time UI contract is present`。
4. 修订后的静态合同通过 `extractTopLevelConstFunction` 从 `const exportTimeEvidence =` 截取到下一个顶层 `const`，并在完整函数体内断言 loading 开始/清理、ID guard 及不存在 `runRuntimeControlInspection`；人工独立抽取同一完整函数体复核也通过。
5. `validate_frontend_feature.py --evidence .../frontend-feature-evidence.md`：PASS；`validate_frontend_feature.py --self-test`：PASS。
6. 用户可见旧称专项检索：PASS。`备用服务器|备份服务器|备份服|>Backup<|label: 'Backup'|审查环境` 在运行控制台页面无匹配；保留的 `backup` 和 `Backup/...` 仅为技术键与 NAS 路径。
7. 可信时间类型字段专项核对：PASS。前端类型覆盖后端响应的环境、节点、主机、时间源、Stratum、Last/RMS/阈值、Leap、同步状态、NTP 状态、服务器/数据库/检查 UTC 及三项原始命令输出。
8. `git diff --check`：PASS，仅有 Windows LF/CRLF 提示，无空白错误。

### Evidence Reconciliation

- `frontend-feature-evidence.md` 第 70 行记录的是首次 P3 执行时 `node_modules` 缺失的原始边界；后续依赖恢复及类型检查已记录在 `execution-log.md` 第 46 至 47 行，并由本轮独立重新执行确认。该历史描述不再代表当前 worktree 状态，本节的独立结果作为当前类型检查证据。

### Boundary

- 本轮未启动服务、执行 E2E、连接远程服务器或修改产品代码；只执行本地静态/类型验证并追加独立测试结论。响应式结论仍限于源码静态约束，真实视口和真实环境行为归 P4 经授权验证。

## P4 独立最终验证

P4 PASS（本地交付范围）：P1 至 P3 的任务定向回归、证据验证器、脚本语法和分支端口门禁全部通过；P4-AC1 至 P4-AC4 按开发计划中“经授权证据或明确未授权边界”的口径满足。正式服/审查服远程验证、服务启动和真实 E2E 均为 `NOT_RUN`，不得据此宣称真实环境已经闭环或检查项 2.9 已关闭。

### Requirement-to-Evidence

| 验收项 | 结论 | 独立证据 |
| --- | --- | --- |
| P4-AC1 | PASS（本地） | MES 签名服务 13/13、批执行两条相关方法 2/2、infra 可信时间与相邻合同 31/31、前端静态合同和类型检查全部通过；端口门禁通过。远程与 E2E 按未授权边界记录为 `NOT_RUN`。 |
| P4-AC2 | PASS | 本节记录了全部定向回归、证据验证器、PowerShell AST、工作区边界、`git diff --check` 和 branch runtime port guard 的独立结果。 |
| P4-AC3 | PASS（边界已记录） | 当前用户只授权执行/独立测试子 Agent，未授权远程服务器、服务启动/重启或真实 E2E；因此没有连接 `172.30.30.57`/`172.30.30.59`，没有读取真实 chrony/NTP，也没有运行 Playwright。开发计划明确允许以该未授权边界完成 P4 本地交付。 |
| P4-AC4 | PASS | 独立测试者重新执行本节全部命令并审阅 PRD、测试计划、开发计划及当前代码差异；没有用执行者日志代替独立结果。 |

### Independent Verification Evidence

1. `mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordExecutionSignatureServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：PASS，`Tests run: 13, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`。
2. `mvn -pl yudao-module-mes -am "-Dtest=MesProEdhrBatchExecutionServiceTest#closeCreatesArchiveWorkTaskAfterBatchClosedWhenFinalInspectionDossierPending+qualityReject_unarchivedBatch_marksRejectedSignsAndCancelsActiveTasks" "-Dsurefire.failIfNoSpecifiedTests=false" test`：PASS，`Tests run: 2, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`。
3. `mvn -pl yudao-module-infra -am "-Dtest=RuntimeOpsTrustedTime*Test,RuntimeOpsInspection*Test,RuntimeControlSpringWiringTest,RuntimeControlCanonicalContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：PASS，`Tests run: 31, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`。
4. `node --check tests/e2e/runtime-control-trusted-time-static.spec.js`：PASS；`node tests/e2e/runtime-control-trusted-time-static.spec.js`：PASS，输出 `PASS: runtime control trusted-time UI contract is present`。
5. `pnpm ts:check`：PASS，`vue-tsc --noEmit -p tsconfig.relaxed.json` 退出码 0、无类型错误。
6. Backend/frontend evidence validators：PASS，分别输出 `Backend API evidence is valid.` 和 `Frontend feature evidence is valid.`。
7. `show-int-ruoyi-trusted-time.ps1` PowerShell AST 解析：PASS；这里只解析语法，没有执行脚本。
8. `git diff --check`：PASS，无空白错误；输出只有 Windows 工作副本 LF/CRLF 提示。
9. `scripts/preflight/branch-runtime-port-guard.ps1`：PASS，分支 `codex/timestamp_20260907` 属于 `int_main` profile，登记端口为前端 `8161`、后端 `48161`。

### Boundary Audit

- 工作区边界：`git status --short --untracked-files=all` 只列出本任务 P1-P3 的 MES 签名时间、Runtime Control 可信时间/导出、运行控制台 UI、测试和部署采集脚本。`RuntimeInspectionServiceImplTest` 删除与 `RuntimeOpsInspectionServiceImplTest` 新增是本任务内测试类重命名；未发现其它非任务资产。
- 数据边界：变更文件中没有 `.sql`、schema、migration 或数据修复脚本；没有修改历史签名记录。正式签名哈希字段和历史策略字段仍保留，当前变更只令新签名的 `signatureDisplayAt` 使用同一服务器 `signedAt`。
- 签名边界：两条后端生成路径均固定 `displayAt = signedAt`；前端正式签名列表和签名格只读取 `signedAt`，`selectedSignedAt` 继续作为独立业务发生时间，不存在回退覆盖链。
- 导出边界：`RuntimeOpsInspectionServiceImpl.exportTimeEvidence` 只读取 `inspectionRunStore.get(id)` 并生成 ZIP；原子计数测试证明首次和重复导出都不会重新执行时间采集，也不会修改巡检、签名或审计记录。
- 节点边界：采集目标固定为正式服 `172.30.30.57` 和审查服 `172.30.30.59`；用户可见口径为“审查服”，`backup` 仅保留为既有技术枚举。
- 失败关闭：批准阈值缺失/非法、目标主机不匹配、命令/JSON 异常、证据缺失、无选中源、同步/NTP/Leap 异常及 Last/RMS 偏差超限均返回 `BLOCKED` 并参与整体 `NO_GO`；没有默认 PASS、mock 或客户端时间 fallback。

### Not Run and Residual Risks

- `NOT_RUN`：正式服/审查服远程 chrony、受控 NTP、服务器 UTC 与数据库 UTC 的真实采集和偏差验证。原因：未获当轮远程服务器操作授权，且经批准阈值、受控时间源及登记主机密钥仍是环境前置。
- `NOT_RUN`：本任务服务启动、真实页面巡检、ZIP 下载和 Playwright E2E。原因：未获当轮服务启动/重启及 E2E 授权。
- 剩余风险：当前证据证明本地实现和失败关闭合同成立，不证明生产/审查环境当前已同步，也不证明真实部署权限、SSH、chrony、MySQL 容器名和页面下载链路可用；这些风险必须在获得授权后以真实环境/E2E证据消除，不能将本地 PASS 外推为 2.9 审查结论。

### Final Decision

P4 本地开发验证：PASS。远程环境验证：`NOT_RUN`。真实 E2E：`NOT_RUN`。允许主 Agent按开发计划推进本地任务收尾，但不得把正式服/审查服或真实页面链路标记为 PASS。
