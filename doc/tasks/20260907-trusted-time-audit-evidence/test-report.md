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

## P2 数据库时间采集修订独立复验

P2 PASS：真实只读检查发现的宿主机 `.env` 解析缺陷已按失败关闭原则修复；本轮本地代码、合同和静态验证全部通过，不改变上一节 P2 PASS 结论。

### Security And Read-only Review

- `show-int-ruoyi-trusted-time.ps1` 不再 source/dot-source 宿主机 `.env`，也不再使用 `set -a`；复杂 `JAVA_OPTS` 不会参与数据库时间采集。
- 数据库命令固定为 `docker exec intruoyi-mysql sh -c`。容器 shell 先执行 `test -n "$MYSQL_ROOT_PASSWORD"`，缺失即非零退出；密码只在容器进程内导出为 `MYSQL_PWD`。
- mysql 调用只执行 `SELECT UTC_TIMESTAMP(6)`，没有 `-p<secret>`、`--password`、写 SQL、宿主机密码展开或默认凭据分支。
- SSH 成功输出仅返回 MySQL 查询值；非零错误只拼接远端标准输出/错误，命令文本中的变量为字面量 `$MYSQL_ROOT_PASSWORD` 而不是密码值。最终 JSON 字段固定为环境、主机、chrony/timedatectl、服务器 UTC、数据库 UTC 和检查 UTC，不包含密码字段。
- MySQL 输出通过 `DateTime.ParseExact('yyyy-MM-dd HH:mm:ss.ffffff', InvariantCulture, AssumeUniversal|AdjustToUniversal)` 严格解析，再固定输出 `yyyy-MM-ddTHH:mm:ss.ffffffZ`；格式不符直接失败，不做宽松解析或本地时间回退。

### Re-verification Evidence

1. 部署脚本聚焦合同：

```powershell
mvn -pl yudao-module-infra -am "-Dtest=RuntimeOpsTrustedTimeDeploymentScriptContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

结果：PASS，`Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`。合同断言要求容器内查询和 `MYSQL_PWD`，并禁止 `. ./.env`、`set -a`、mysql `-p` 及密码输出语句。

2. 完整 P2 定向测试：

```powershell
mvn -pl yudao-module-infra -am "-Dtest=RuntimeOpsTrustedTime*Test,RuntimeOpsInspection*Test,RuntimeControlSpringWiringTest,RuntimeControlCanonicalContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

结果：PASS，`Tests run: 31, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`。

3. PowerShell AST：PASS，无语法错误。

4. Backend evidence validator：PASS，`Backend API evidence is valid.`；文档已记录旧脚本真实 RED、脚本合同 RED/GREEN、容器内密码边界和修订后的远程复验边界。

5. `git diff --check`：PASS，仅有 Windows LF/CRLF 提示，无空白错误。

### Boundary

- 按独立测试指令未执行脚本、未连接正式服或审查服、未启动服务。修订后在真实容器中获得数据库 UTC 的结果仍须由已授权的 P4 远程只读验证证明，本节不将该外部运行态记为 PASS。

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

## P4 真实环境与页面闭环复验

P4 PASS：本节以经授权、已启动的任务自有运行态取代上一节的远程环境与真实 E2E `NOT_RUN` 边界。Playwright 从真实登录页完成巡检和证据导出动作；正式服、审查服可信时间行均为 PASS，下载 ZIP 的冻结巡检、三文件和 SHA-256 离线核验全部通过。

### Runtime Preflight

- 分支：`codex/timestamp_20260907`；worktree：`D:\IntRuoyiWorktree\timestamp_20260907`。
- 前端：`http://127.0.0.1:8161`，监听 PID `62604`，命令行指向当前 worktree 的 Vite；入口 HTTP 200。
- 后端：`http://127.0.0.1:48161`，监听 PID `40004`，运行包为当前 worktree 的 `yudao-server-exec.jar`；启动参数包含 `--server.port=48161`、当前 worktree `repo-root/state-dir`；health 为 `UP`。
- 验证身份：本机 E2E 租户/账号标签 `芋道源码/admin`。报告不记录密码、令牌或 Cookie。

### Real Playwright Path

1. 使用独立 Playwright CLI session `timestamp-p4` 打开 `http://127.0.0.1:8161/login?redirect=/index`，从登录页填写测试租户账号并点击“登录”，进入首页。
2. 通过真实菜单依次点击“基础设施” -> “监控中心” -> “运行控制台”，最终 URL 为 `http://127.0.0.1:8161/infra/monitors/runtime-control`，页面标题为“瑛泰管理系统 - 运行控制台”。
3. 在“可信时间证据”区域点击“执行巡检”。页面自然请求 `POST /admin-api/infra/runtime-control/inspection-runs` 返回 HTTP 200，并显示冻结巡检编号 `2`、完成时间 `2026-09-08 00:06:13`、整体结论“不放行”。
4. 页面可信时间表格显示两条真实结果：
   - 正式服 `172.30.30.57`：时间源 `139.199.214.202`，Last `0.211 ms`，RMS `0.554 ms`，Leap `Normal`，检查时间 `2026-09-08 00:06:10`，状态“通过”。
   - 审查服 `172.30.30.59`：时间源 `139.199.214.202`，Last `-0.005 ms`，RMS `0.219 ms`，Leap `Normal`，检查时间 `2026-09-08 00:06:13`，状态“通过”。
5. 在同一页面点击“导出时间戳证据”。页面自然请求 `GET /admin-api/infra/runtime-control/inspection-runs/2/time-evidence.zip` 返回 HTTP 200，浏览器真实下载 `可信时间证据_巡检2.zip`。没有使用 `fetch`、APIRequest 或 shell HTTP 调用代替巡检、导出动作。

### Downloaded Evidence Verification

- 对浏览器实际下载 ZIP 进行只读离线核验，固定且仅包含 `审查摘要.html`、`原始证据.json`、`SHA256SUMS.txt`。
- `SHA256SUMS.txt` 中 HTML 与 JSON 两项散列分别按文件字节重新计算，均一致。
- `原始证据.json` 的巡检 ID 为 `2`、整体状态为 `NO_GO`，两条可信时间检查均为 `PASS`；目标主机精确为 `172.30.30.57`、`172.30.30.59`，时间源均为 `139.199.214.202`。
- `审查摘要.html` 明确保留 `结论：NO_GO`，没有因两条时间检查通过而把整体巡检伪造成 PASS。整体 NO_GO 来自本地 website 探针不可达及任务 worktree 日志目录缺失等相邻现状，不影响两条可信时间检查的独立 PASS 结论。

### Browser Diagnostics

- Playwright console：共记录 4 条消息，`Errors: 0`、`Warnings: 0`；可见 info 仅为系统欢迎日志。
- 页面未出现巡检或下载失败提示。外部 Iconify 资源请求均为 HTTP 200，本轮没有外部图片证书错误。
- 页面相邻既有状态仍显示本机 `4173` website 探针不可达、任务 worktree 日志目录不存在以及 `48081` 基准后端不可达/错误文本乱码；这些状态使整体巡检 NO_GO，但目标任务运行态 `8161/48161`、可信时间巡检和下载链路均正常。
- 非阻塞口径观察：可信时间区域和发布/矩阵区域均显示“审查服”，但相邻“探针状态”表仍把技术环境值渲染为英文 `Backup`。该既有探针展示不属于本次可信时间 AC-01 至 AC-06 的验收动作，不影响本节 PASS；若要求运行控制台所有区域严格统一口径，应单独修正其环境标签映射。

### Final Decision

- P4-AC1：PASS，定向回归与真实环境/页面闭环均已有证据。
- P4-AC2：PASS，分支门禁和定向回归证据见上一节，本节补齐运行态证据。
- P4-AC3：PASS，经授权的正式服/审查服真实时间采集与 Playwright E2E 已完成。
- P4-AC4：PASS，真实巡检、异常整体状态保留、真实下载及离线内容校验全部完成。

最终结论：P4 PASS。可信时间最小闭环具备真实页面巡检、正式服/审查服状态展示、异常不伪装通过及一键导出可离线核验证据的完整验证。

## P2 SSH stderr 分流修订独立复验

P2 FAIL：stdout/stderr 分流、已知 Windows OpenSSH 诊断白名单和严格数据库 UTC 解析均已实现，规定测试也全部通过；但非零诊断脱敏未覆盖本功能实际使用的 `MYSQL_ROOT_PASSWORD` 与 `MYSQL_PWD` 键，秘密值仍可进入异常消息，因此不能按“非零诊断脱敏、密码不进入日志”合同放行。本节结论取代此前 P2/P4 的 PASS 状态，修复并重新独立验证前不得收尾。

### Finding

1. `resolve-trusted-time-ssh-result.ps1` 的脱敏正则只匹配独立单词 `password|passwd|token|secret|api-key`。由于下划线属于正则单词字符，`MYSQL_ROOT_PASSWORD=<value>` 不会在 `PASSWORD` 前形成单词边界；`MYSQL_PWD=<value>` 也完全不在键集合中。带空格的引号值同样只会脱敏第一个词并留下剩余内容。`Resolve-TrustedTimeSshResult` 在 exit 非 0 时把该不完整脱敏结果直接拼入异常消息。

独立使用合成秘密值调用辅助函数得到：

```text
INPUT=MYSQL_ROOT_PASSWORD=<synthetic-secret>
ERROR=SSH command failed with exit code 255: MYSQL_ROOT_PASSWORD=<synthetic-secret> connection failed

INPUT=MYSQL_PWD=<synthetic-secret>
ERROR=SSH command failed with exit code 255: MYSQL_PWD=<synthetic-secret> connection failed
```

这证明当前“非零诊断脱敏”测试只覆盖 `password=topsecret`，无法支撑实际 MySQL 密钥变量不会泄漏的声明。

### Verified Behavior

- 主脚本使用 `ProcessStartInfo`、`ssh -n`、独立异步 `ReadToEndAsync` 读取 stdout/stderr，并在读取任务启动后等待进程退出，未发现管道死锁路径。
- exit=0 时 stderr 按非空行逐行检查，仅允许精确正则匹配 `close - IO is still pending on closed socket. read:<n>, write:<n>, io:<hex>`；混合未知行会失败。
- exit 非 0、未知 stderr 和空 stdout 均明确失败；成功返回仅使用修剪后的 stdout。
- 数据库结果仍使用固定 `yyyy-MM-dd HH:mm:ss.ffffff` 的 `ParseExact`，随后输出 `yyyy-MM-ddTHH:mm:ss.ffffffZ`，没有放宽或 fallback。

### Verification Evidence

1. 聚焦部署合同：PASS，5/5；但现有非零用例仅使用通用 `password=topsecret`，未覆盖 Finding。
2. 完整 P2 定向测试：PASS，35/35，0 失败、0 错误。
3. `show-int-ruoyi-trusted-time.ps1` 与 `resolve-trusted-time-ssh-result.ps1` PowerShell AST：PASS。
4. Backend evidence validator：PASS，`Backend API evidence is valid.`；该验证器只验证证据结构，不能推翻上述可复现泄漏。
5. `git diff --check`：PASS，仅有 Windows LF/CRLF 提示。
6. 本轮没有执行远程连接、脚本真实采集或服务启动。

### Required Correction

- 脱敏必须至少完整覆盖 `MYSQL_ROOT_PASSWORD`、`MYSQL_PWD` 以及已有通用秘密键，并正确处理单引号/双引号包裹或含空格的值；不得在错误消息中保留值的任何片段。
- 将上述实际变量名和带空格引号值加入可执行 RED/GREEN 测试，再复跑聚焦与完整 P2 门禁。

## P2 SSH 诊断脱敏修订独立复验

P2 PASS：本节结论取代上一轮 P2 SSH stderr 分流修订 FAIL。实际 MySQL 密钥键和通用秘密键均已采用显式、大小写不敏感的完整赋值脱敏；独立运行矩阵未发现秘密片段残留，非秘密诊断保持可读。

### Independent Redaction Matrix

独立直接加载 `resolve-trusted-time-ssh-result.ps1`，对以下键逐一运行 exit=255 失败路径：

```text
MYSQL_ROOT_PASSWORD
MYSQL_PWD
PASSWORD
PASSWD
TOKEN
SECRET
ACCESS_KEY
SECRET_KEY
API_KEY
```

每个键分别验证未引号值、单引号含空格值、双引号含空格值，并混用大小写，共 27 个赋值场景。每次均断言错误包含 `<REDACTED>`，且合成秘密的所有词片段都不存在。结果：PASS。

另独立验证 mysql `-p` 的未引号、单引号含空格、双引号含空格三种形式，均整体替换为 `-p<REDACTED>`；验证 `host=prod node=audit reason=connection_refused retry=disabled` 四项非秘密诊断全部保留。结果：PASS。

### Implementation Review

- 赋值正则显式列出九类秘密键，使用 `(?i)` 实现大小写不敏感匹配；值分支完整匹配双引号、单引号或无引号值。
- mysql `-p` 使用独立正则并覆盖相同三种值形式。
- 替换只针对秘密键或 `-p` 参数，未对普通 `host/node/reason/retry` 键做泛化删除。
- exit 非 0 与未知 stderr 均在抛出前调用同一脱敏函数；成功路径仍只返回 stdout。

### Re-verification Evidence

1. 聚焦部署合同：PASS，`Tests run: 6, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`。
2. 完整 P2 定向测试：PASS，`Tests run: 36, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`。
3. 两个 PowerShell 文件 AST：PASS。
4. Backend evidence validator：PASS，`Backend API evidence is valid.`。
5. `git diff --check`：PASS，仅有 Windows LF/CRLF 提示，无空白错误。
6. 本轮没有连接远程服务器、执行真实采集或启动服务。

### Test Coverage Note

- 仓库内第 6 个聚焦测试覆盖实际 MySQL 两个键、大小写及三种赋值格式的代表组合，并证明非秘密诊断保留；`PASSWD`、`API_KEY` 和 mysql `-p` 的具体运行行为由本节独立 30 组矩阵补充验证。当前实现与 backend evidence 声明一致。

## P2 旧版 timedatectl 与阈值透传独立复验

P2 PASS：旧版 timedatectl 输出已按同一可信时间语义规范化，否定或冲突信号继续失败关闭；部署 compose 将批准阈值作为 backend 必填环境变量显式透传，不存在默认值。本节维持最新 P2 PASS 结论。

### Timedatectl Compatibility Review

- 旧格式仅出现 `NTP synchronized: yes` 与 `NTP enabled: yes` 时，分别规范化为 `systemClockSynchronized=true`、`ntpServiceState=active`；聚焦测试实际断言这两个结构化值和最终 PASS。
- 旧格式 `NTP synchronized: no` 或 `NTP enabled: no` 任一出现时分别产生“系统时钟未同步”或“NTP service 不是 active”，参数化测试证明最终 BLOCKED。
- 新旧同步字段并存时，`allPresentSignalsAffirmative` 对每个已出现值分别要求 `yes`；任一 `no` 或其它值使布尔结果为 false。
- 新旧 NTP 服务字段并存时，新字段必须为 `active` 且旧字段必须为 `yes`；新字段非 active 直接保留为失败状态，旧字段非 yes 统一为 inactive。双肯定才返回 active。
- 对 `RuntimeTrustedTimeParser` 的差异审查确认，除新增四个旧字段模式、同步/NTP 归一化和两个私有辅助函数外，其它选中源、Leap、Stratum、服务器目标、UTC 格式、Last/RMS 必填及偏差阈值逻辑未改动。

### Deployment Contract

- `int-ruoyi-test/docker-compose.yml` 的 backend `environment` 显式包含：

```yaml
INTRUOYI_TRUSTED_TIME_MAX_OFFSET_MILLIS: ${INTRUOYI_TRUSTED_TIME_MAX_OFFSET_MILLIS:?INTRUOYI_TRUSTED_TIME_MAX_OFFSET_MILLIS is required}
```

- `${VAR:?...}` 要求变量存在且非空；源码及合同测试均确认没有 `${INTRUOYI_TRUSTED_TIME_MAX_OFFSET_MILLIS:-...}` 默认值，不会静默采用阈值。

### Re-verification Evidence

1. Parser 与部署合同聚焦测试：PASS，`Tests run: 26, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`；其中 parser 20 项、部署合同 6 项。
2. 完整 P2 定向测试：PASS，`Tests run: 39, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`。
3. `show-int-ruoyi-trusted-time.ps1`、`resolve-trusted-time-ssh-result.ps1` PowerShell AST：PASS。
4. Backend evidence validator：PASS，`Backend API evidence is valid.`。
5. `git diff --check`：PASS，仅有 Windows LF/CRLF 提示，无空白错误。
6. 本轮没有连接正式服或审查服，没有执行远程脚本或启动服务。

### Boundary

- 本轮证明本地解析和部署合同成立；真实审查服旧版 timedatectl 输出以及正式部署变量是否已配置，仍需经授权的远程只读验证，不能由本地 PASS 外推。

## P4 审查服术语修订真实复验

P4 FAIL：术语修订的后端与前端静态合同通过，刷新后的探针状态已把 `backup` 显示为“审查服”；但真实运行态巡检无法执行时间采集，正式服和审查服两项均为 BLOCKED，同时整页“最近操作”的历史可见内容仍出现“备份服务器/备份服”旧称。当前不满足“真实时间项通过”和“整页不再显示用户可见旧称”的验收要求。

### Independent Local Regression

1. `mvn -pl yudao-module-infra -am "-Dtest=RuntimeControlAuditServerTerminologyTest,RuntimeOpsTrustedTime*Test,RuntimeOpsInspection*Test,RuntimeControlSpringWiringTest,RuntimeControlCanonicalContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：PASS，`Tests run: 40, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`；其中 `RuntimeControlAuditServerTerminologyTest` 1/1 PASS。
2. `node --check tests/e2e/runtime-control-trusted-time-static.spec.js` 与 `node tests/e2e/runtime-control-trusted-time-static.spec.js`：PASS。
3. `node --check tests/e2e/runtime-control-release-package-static.spec.js` 与 `node tests/e2e/runtime-control-release-package-static.spec.js`：PASS。
4. 运行态预检：分支 `codex/timestamp_20260907`；前端 `8161` PID `62604`、命令行指向当前 worktree；后端 `48161` PID `74544`、运行当前 worktree `yudao-server-exec.jar`，health 为 `UP`。

### Real Playwright Path

- 使用全新 Playwright CLI session `timestamp-p4-term`，从 `http://127.0.0.1:8161/login?redirect=/index` 以本机 E2E 身份标签 `芋道源码/admin` 登录；报告不记录密码、令牌或 Cookie。
- 经真实菜单“基础设施” -> “监控中心” -> “运行控制台”进入 `http://127.0.0.1:8161/infra/monitors/runtime-control`。
- 点击“执行巡检”，页面自然请求 `POST /admin-api/infra/runtime-control/inspection-runs` 返回 HTTP 200，生成巡检 ID `1`、整体 `NO_GO`。
- 点击“导出时间戳证据”，页面自然请求 `GET /admin-api/infra/runtime-control/inspection-runs/1/time-evidence.zip` 返回 HTTP 200，浏览器实际下载 `可信时间证据_巡检1.zip`。没有使用 API、`fetch`、APIRequest 或 shell HTTP 替代巡检和导出动作。
- 点击页面“刷新”后，探针状态的审查服后端、前端和 website 三行均显示“审查服”，不再显示英文 `Backup`，证明共享环境映射在该区域生效。

### Findings

1. 真实巡检无法采集时间证据。正式服 `172.30.30.57` 和审查服 `172.30.30.59` 两行的时间源、Last/RMS、Leap 和检查时间均为 `-`，状态均为“已阻断”；页面原因一致为：`运行控制台脚本不存在：D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\deploy\show-int-ruoyi-trusted-time.ps1`。运行页的容量路径也显示 `D:\ProjectPackage\Int\IntRuoyi\...`，与当前 worktree 不一致，说明后端有效 `repo-root/state-dir` 没有按本任务运行态生效。该证据与“后端 repoRoot/stateDir 均指向本 worktree”的前置声明矛盾，不能把 health `UP` 当作时间巡检可用。
2. 整页旧称仍未清零。“最近操作”历史行仍可见 `上线备份服务器`、`E2E上线发布包A到备份服务器` 和 `上线备份服务器 completed`；Playwright 对页面文本查找 `备份服` 得到 3 个匹配。当前静态合同只覆盖代码映射，未覆盖已保存历史展示字段，因而无法证明整页统一口径。

### Downloaded ZIP Verification

- ZIP 固定且仅包含 `审查摘要.html`、`原始证据.json`、`SHA256SUMS.txt`；HTML 和 JSON 的 SHA-256 重新计算均与清单一致。
- JSON 巡检 ID 为 `1`、整体状态为 `NO_GO`，包含 `trusted-time-prod` 与 `trusted-time-audit`，主机精确为 `172.30.30.57`、`172.30.30.59`。
- 两条时间项状态均为 `BLOCKED`，原因均为上述错误 repo root 下采集脚本不存在；因此 ZIP 导出功能本身通过，但不能证明正式服/审查服时间同步状态通过。

### Browser Diagnostics

- Playwright console：`Errors: 0`、`Warnings: 0`。
- 登录、菜单、巡检 POST 和 ZIP 下载 GET 均由页面自然触发且 HTTP 200。
- 本轮没有外部图片证书错误；Iconify 请求均为 HTTP 200。

### Required Corrections

- 使用项目标准分支启动方式重启任务自有后端，确保 `--yudao.runtime-control.repo-root=D:/IntRuoyiWorktree/timestamp_20260907/IntRuoyiBackend`、任务 `state-dir` 和批准阈值在有效运行配置中可证明生效；重新通过页面执行巡检，必须看到正式服和审查服两行真实来源、Last/RMS、Leap、检查时间及“通过”。
- 对“最近操作”中的历史 `actionName/reason/summary` 明确展示策略：保留原始审计值时，应另用统一显示字段/安全展示映射输出“审查服”，且原始值只能保留在审计证据中；不得修改历史审计事实。增加包含旧历史记录的服务或页面回归测试，证明整页用户可见文本无 `Backup`、`备份服`、`备用服务器`。
- 修订后必须重新运行同一真实 Playwright 路径和 ZIP 核验；当前 P4 不能放行。

## P4 审查服术语与可信时间最终独立复验

P4 PASS：本节取代上一节 P4 FAIL。任务自有后端已按正确 `repo-root/state-dir` 启动，真实页面巡检中正式服与审查服可信时间均通过；探针区域和最近操作区域不再显示 `Backup`、`备份服`、`备份服务器` 或 `备用服务器`，浏览器下载的证据包可独立校验。

### Runtime Preflight

- 首次预检时 `8161/48161` 均未监听，因此独立测试明确暂停 E2E 并通知主 Agent，没有把静态测试当作运行态成功。
- 服务恢复后再次核验：前端 `8161` PID `37540`，命令行指向当前 worktree 的 Vite；后端 `48161` PID `42784`，运行当前 worktree 的 `yudao-server-exec.jar`。
- 后端命令行明确包含 `--server.port=48161`、`--yudao.runtime-control.repo-root=D:/IntRuoyiWorktree/timestamp_20260907/IntRuoyiBackend` 和 `--yudao.runtime-control.state-dir=D:/IntRuoyiWorktree/timestamp_20260907/.runtime/runtime-control`；health 为 `UP`。

### Independent Regression

1. 后端定向测试：PASS，`Tests run: 40, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`。命令覆盖 `RuntimeControlAuditServerTerminologyTest`、`RuntimeOpsTrustedTime*Test`、`RuntimeOpsInspection*Test`、`RuntimeControlSpringWiringTest` 和 `RuntimeControlCanonicalContractTest`。
2. `runtime-control-trusted-time-static.spec.js` 语法检查与执行：PASS；其中可执行断言把历史 `Backup`、`备份服`、`备份服务器`、`备用服务器` 映射为审查服口径，同时保留技术路径 `Backup/ReleasePackage/r1`。
3. `runtime-control-release-package-static.spec.js` 语法检查与执行：PASS。
4. `pnpm ts:check`：PASS，退出码 `0`。

### Real Playwright Path

- 使用全新 Playwright CLI session `timestamp-p4-retest`，从真实登录页以测试身份标签 `芋道源码/admin` 登录，经菜单“基础设施” -> “监控中心” -> “运行控制台”进入页面；报告不记录密码、令牌或 Cookie。
- 在页面点击“执行巡检”，页面自然请求 `POST /admin-api/infra/runtime-control/inspection-runs` 返回 HTTP 200，生成巡检 ID `3`、完成时间 `2026-09-08 09:56:25`；整体因相邻本地探针和日志目录问题保持 `NO_GO`，没有伪装成功。
- 正式服 `172.30.30.57`：时间源 `139.199.214.202`，Last `-0.312 ms`，RMS `0.571 ms`，Leap `Normal`，检查时间 `2026-09-08 09:56:22`，状态“通过”。
- 审查服 `172.30.30.59`：时间源 `139.199.214.202`，Last `-0.232 ms`，RMS `0.186 ms`，Leap `Normal`，检查时间 `2026-09-08 09:56:25`，状态“通过”。
- 对探针区域单独读取页面自然文本，审查服三行均显示“审查服”；该区域不存在 `Backup`、`备份服`、`备份服务器` 或 `备用服务器`。最近操作区域自然文本为“暂无操作记录”，同样不存在四类旧称；历史旧值的非空投影由上述可执行静态合同覆盖。
- 在同一页面点击“导出时间戳证据”，页面自然请求 `GET /admin-api/infra/runtime-control/inspection-runs/3/time-evidence.zip` 返回 HTTP 200，并真实下载 `可信时间证据_巡检3.zip`。巡检和导出动作均未使用 API、`fetch`、APIRequest 或 shell HTTP 代替。

### Downloaded Evidence Verification

- ZIP 固定且仅包含 `审查摘要.html`、`原始证据.json`、`SHA256SUMS.txt`。
- 重新按文件字节计算 HTML 与 JSON 的 SHA-256，均与 `SHA256SUMS.txt` 一致。
- `原始证据.json` 的巡检 ID 为 `3`、整体状态为 `NO_GO`；`trusted-time-prod` 与 `trusted-time-audit` 均为 `PASS`，主机精确为 `172.30.30.57`、`172.30.30.59`，两项时间源、Last/RMS、Leap、检查时间及服务器/数据库 UTC 字段完整。
- `审查摘要.html` 保留 `NO_GO`，没有因两条时间项通过而篡改整体巡检结论。

### Browser Diagnostics

- Playwright console：`Errors: 0`、`Warnings: 0`；唯一 info 为系统欢迎日志。
- 浏览器请求清单证明登录、巡检 POST 和证据 ZIP GET 均由真实页面自然触发且返回 HTTP 200。
- 可信时间区域截图：`.playwright-cli/element-2026-09-08T02-02-13-085Z.png`；最终页面快照和下载 ZIP 均位于本任务 worktree 的 `.playwright-cli` 临时目录，供主 Agent 收尾时按规则处理。

### Final Decision

- P4-AC1：PASS，术语、可信时间、巡检和导出定向回归全部通过。
- P4-AC2：PASS，TypeScript 与前端静态合同通过。
- P4-AC3：PASS，正式服与审查服真实时间证据均由页面巡检取得并通过阈值判定。
- P4-AC4：PASS，真实页面导出、固定三文件、SHA-256 和异常整体状态保留均已验证。

最终结论：P4 PASS，可以进入主 Agent 的状态同步与收尾门禁。

## P5 测试阶段停用偏差阈值独立复验

P5 PASS：未配置偏差阈值的任务运行态仍采集完整 Last/RMS 偏差，正式服和审查服时间项均通过；页面保留唯一的“导出时间戳证据”按钮，下载证据包明确记录 `maxOffsetMillis=null`。

### Runtime Preflight

- 前端 `8161` PID `54240`，后端 `48161` PID `51308`；两者命令行均指向当前 worktree。
- 后端命令行包含正确的 `repo-root/state-dir`，`/actuator/health` 为 `UP`，前端 HTTP 为 `200`。
- Process/User/Machine 三个范围均未配置 `INTRUOYI_TRUSTED_TIME_MAX_OFFSET_MILLIS`。

### Independent Regression

1. P5 聚焦后端测试：PASS，`Tests run: 32, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`；覆盖 parser、collector 和部署合同。
2. 完整可信时间后端定向回归：PASS，`Tests run: 36, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`；覆盖巡检、端点与 ZIP 导出。
3. `node --check tests/e2e/runtime-control-trusted-time-static.spec.js` 和静态合同执行：PASS。
4. `pnpm ts:check`：PASS，退出码 `0`。
5. `git diff --check`：PASS，仅有 Windows LF/CRLF 提示，无空白错误。

### Real Playwright Path

- 使用全新 Playwright CLI session `timestamp-p5`，真实登录后从菜单搜索结果进入“基础设施 -> 监控中心 -> 运行控制台”。
- 在页面点击“执行巡检”，生成巡检 ID `4`。整体因相邻的本地探针和日志目录问题保持 `NO_GO`，未伪装整体通过。
- 正式服 `172.30.30.57`：时间源 `139.199.214.202`，Last `-1.225 ms`，RMS `0.630 ms`，Leap `Normal`，状态“通过”。
- 审查服 `172.30.30.59`：时间源 `139.199.214.202`，Last `-0.820 ms`，RMS `0.302 ms`，Leap `Normal`，状态“通过”。
- 页面快照中“导出时间戳证据”按钮计数为 `1`，不存在 `100 ms`、`maxOffset` 或“偏差阈值”启用文案。
- 在同一页面点击该按钮，浏览器真实下载 `可信时间证据_巡检4.zip`；巡检和导出业务动作未使用 API、`fetch` 或 APIRequest 替代。

### Downloaded Evidence Verification

- ZIP 仅包含 `审查摘要.html`、`原始证据.json`、`SHA256SUMS.txt`；HTML 和 JSON 的 SHA-256 重新计算后均与清单一致。
- JSON 巡检 ID 为 `4`、整体状态为 `NO_GO`；`审查摘要.html` 同样保留 `NO_GO`。
- `trusted-time-prod` 与 `trusted-time-audit` 均为 `PASS`，两项的主机、时间源、Last/RMS、Leap、同步状态和 NTP 状态完整。
- 两项 `maxOffsetMillis` 均为 `null`，直接证明本次运行态未启用 `100 ms` 或其它数值偏差阈值。

### Browser Diagnostics

- Playwright console：`Errors: 0`、`Warnings: 0`；唯一 info 为系统欢迎日志。
- 页面截图：`.playwright-cli/page-2026-09-08T03-03-15-041Z.png`；下载 ZIP 与快照位于本任务 worktree 的 `.playwright-cli` 临时目录。

### Final Decision

- P5-AC1：PASS，未配置阈值时不执行 Last/RMS 数值超限判定，但仍保留真实数值。
- P5-AC2：PASS，chrony、同步、NTP、Leap、Stratum、UTC 和命令失败门禁回归通过。
- P5-AC3：PASS，现有唯一导出按钮可用，证据包三文件与哈希合同成立。

最终结论：P5 PASS，可以进入主 Agent 的状态同步与收尾门禁。

## P6 第二轮修复验证

P6 修复轮 PASS：第二轮 reviewer 指出的三项阻塞均已按最小实现修复，并完成本地定向回归。最终是否放行以第三轮独立 reviewer 结构化报告为准。

### Fixed Blocking Issues

- 证据导出：导出 ZIP 前要求 `trusted-time-prod` 与 `trusted-time-audit` 各 1 项，且每项必须含目标环境/固定主机匹配的 `trustedTime`；历史 PASS、缺失、重复、错配证据均拒绝导出。
- Stratum 边界：可信时间 parser 明确接受 `1..15`，`0` 和 `16` 均 BLOCKED。
- 签名选择：活动执行页和只读表单统一按服务器签署时间 `signedAt`、数值签名 ID 选择最新签名；缺少有效编号或服务器签署时间时失败关闭。

### Verification Evidence

1. Infra 定向回归：`mvn -q -pl yudao-module-infra -am "-Dtest=RuntimeOpsTrustedTime*Test,RuntimeOpsInspection*Test,RuntimeControlSpringWiringTest,RuntimeControlCanonicalContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS。
2. MES 归档/签名回归：`mvn -q -pl yudao-module-mes -am "-Dtest=ExecutionArchiveRendererTest,MesProBatchRecordExecutionSignatureServiceTest,MesProEdhrBatchArchivePdfAComplianceTest,MesProBatchRecordExecutionArchiveContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，36 项、0 失败、0 错误。
3. 前端签名选择合同：`node tests\e2e\edhr-latest-signature-selection-static.spec.js` -> PASS。
4. 可信时间 UI 合同：`node tests\e2e\runtime-control-trusted-time-static.spec.js` -> PASS。
5. 类型检查：`pnpm ts:check` -> PASS。
6. 空白检查：`git diff --check` -> PASS，仅有 Windows LF/CRLF 提示。

### Boundary

- P6 未新增真实 E2E 或远程服务器操作；P4/P5 已覆盖真实页面巡检与导出证据包。

## int_main 七项审查要求最终映射

本节补充 `int_main` 融合后的真实 E2E 证据。验证基于当前 `int_main` HEAD `e8f572f2e`，48081 当前活跃运行包包含可信时间 collector/parser/exporter，Playwright 通过真实登录页和真实菜单“基础设施 > 监控中心 > 运行控制台”进入页面后完成巡检与导出。

| 序号 | 审查要求 | int_main 结论 | 证据 |
| --- | --- | --- | --- |
| 1 | 正式服、审查服 chrony 时间检查 | PASS | 巡检 ID `2` 中 `trusted-time-prod` 与 `trusted-time-audit` 均为 PASS；正式服 `172.30.30.57`，审查服 `172.30.30.59`，均有时间源、Last/RMS、Leap、同步状态和 UTC 证据。 |
| 2 | `signedAt` 保持服务器生成 | PASS | MES 签名服务、执行归档和签名选择回归测试通过；正式签名展示只读取服务器 `signedAt`。 |
| 3 | `selectedSignedAt` 仅作为业务发生时间，不覆盖正式签名展示 | PASS | P6 回归验证 PDF、XLSX、最终可打印批归档和活动执行表单不再用 `selectedSignedAt` 覆盖正式签名时间。 |
| 4 | 复用现有 Runtime Control 巡检、存储和告警 | PASS | 真实页面点击现有“执行巡检”，自然触发 `POST /admin-api/infra/runtime-control/inspection-runs`，巡检 ID `2` 被保存并用于后续导出。 |
| 5 | 运行控制台增加“导出时间戳证据”按钮 | PASS | Playwright 在现有运行控制台页面点击“导出时间戳证据”，自然触发 ZIP 下载 HTTP 200。 |
| 6 | ZIP 只包含 `审查摘要.html`、`原始证据.json`、`SHA256SUMS.txt` | PASS | 离线解包确认仅三文件；HTML 与 JSON 的 SHA-256 均与 `SHA256SUMS.txt` 匹配。 |
| 7 | 异常、缺失或无法检查时，报告不能显示通过 | PASS | 导出逻辑要求双目标可信时间项完整且主机匹配；巡检整体 ID `2` 为 `NO_GO` 时，HTML/JSON 原样保留 `NO_GO`，未因时间项通过而伪装整体通过。 |

### int_main E2E Artifacts

- 浏览器执行记录：`E:\IntRuoyi\output\playwright\int-main-e2e-trusted-time-20260908-184158\browser-flow-result.json`
- 页面截图：`E:\IntRuoyi\output\playwright\int-main-e2e-trusted-time-20260908-184158\runtime-control-inspection-2.png`
- 下载 ZIP：`E:\IntRuoyi\output\playwright\int-main-e2e-trusted-time-20260908-184158\可信时间证据_巡检2.zip`

### int_main Boundary

- 48081 当前后端健康为 `UP`，当前运行 Jar 为 `E:\IntRuoyi\output\runtime\int_main\backend-runtime-control-20260908-185534.jar`，SHA-256 `C27197556A7ABF822B9958F6AF234020E4DFBAF67E29AB46896D7E46C6811644`。
- 本次 E2E 未启用 `100 ms` 阈值；证据 JSON 中正式服、审查服两项 `maxOffsetMillis=null`。
- Playwright 记录 `pageErrors=0`；非目标 console/failure 仅为审批待办数量相邻异常与外部图片证书错误，不影响运行控制台巡检和证据包下载链路。
