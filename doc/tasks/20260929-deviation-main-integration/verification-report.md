# 集成验证报告（进行中）

## 范围
用户授权本地接管已停止任务的主线未提交修复并融合偏差工作区；明确仅本地合并，不推送。

## 已验证
- 主线前端现有9文件合同逐一通过；main-front-stable.log。
- 主线 pnpm ts:check，exit 0；main-stable-types.log。
- 偏差来源前端17项合同通过，SFC编译通过，类型检查exit 0，ESLint 0 errors/1原有warning。
- 偏差签名加锁次序3项真实RED，修复后4类34项GREEN。此为Mockito顺序及H2事务测试，不冒充真实数据库并发或E2E。
- 端口门禁：主线8081/48081；来源8094/48094。

## 未完成
主线稳定后端回归、融合后定向回归、提交及快进合并尚未完成。
本轮不运行E2E、不写数据库、不重启主线服务。历史全链路E2E声明存在证据限制，不能据此宣称全部通过。

# Runtime policy v2 alignment

## Goal
Make the actual packaged backend policy load against v2 and retain its meaning in activation and reason validation. No database activation or external approval claim.

## Milestones
1. RED: real bundle loading, v2 activation projection, USER_REQUIRED blank-reason rejection.
2. Minimal runtime YAML/schema and consumers alignment.
3. GREEN: ReleaseServiceImplTest and policy loader/activation/reason tests.

## Current Status
ready_for_closeout

BDD: Runtime bundle -> Given the actual META-INF/gxp resources / When loaded / Then all registered operations validate against v2.
BDD: Activation projection -> Given multiple v2 sourceLocators and ownerRole / When operations are projected for persistence / Then every locator and the role are retained.
BDD: Required user reason -> Given USER_REQUIRED / When the reason is blank / Then append fails without an event.

Root config remains historical. Backend config is the Maven-packaged authoritative file. Draft release fragments stay excluded. Existing approval metadata is preserved, not newly asserted or activated.

## Mapping and limits
- sourceLocator -> sourceLocators (array); owner -> ownerRole.
- writeBoundaryScan -> coverageScope.writeBoundaryScan; R1 scope explicitly enumerates the 28 registered operation IDs, with no exclusions. This does not claim full-system coverage.
- REQUIRED_CATEGORY_AND_TEXT -> USER_REQUIRED; add/restore -> SYSTEM per the operation contract. NOT_REQUIRED -> NONE. Existing persisted REQUIRED* policy constraints remain enforced alongside USER_REQUIRED.
- MES profiles follow operation-contract.md. Deviation uses NCR; legacy field/file/signature/permission/configuration/migration operations use explicit EXECUTION_FIELD/CONTROLLED_FILE/SIGNATURE/SECURITY/CONFIGURATION/MIGRATION classifications. These classify snapshots, without inventing additional business behavior.
- Activation persists all sourceLocators as JSON in the existing sourceLocator column and ownerRole in the existing owner column; the canonical policy keeps the full v2 object.
- Historical ACT04 fixture bytes are frozen from the main legacy configuration, retaining SHA-256 5fc4d4be12df26dcb88f127a0e900f0830535ae4db079884d2c4022a3cfee1a0. Production loader rejects it; only explicit H2 test seed constructs its historical record. Current v2 is tested under the same historical version to prove conflict and no rewrite.

## Activation is separate
No database writes or tenant activation were executed. Before deployment, use the controlled activation process with the reviewed bundle hash, coverage hash, tenant and actual approval reference, under the tenant maintenance window; verify ledger/schema prerequisites and all writer versions. Existing code approval metadata is not evidence of a new external approval. Draft terminal/preparation proposals remain outside the runtime bundle.

## Verification
Final command: mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-mes -am test '-Dtest=GxpAuditPolicy*Test,GxpAuditServiceImplTest,MesProEdhrReleaseServiceImplTest' '-Dsurefire.failIfNoSpecifiedTests=false' '-DfailIfNoTests=false' '-Dstyle.color=never'.
PASS: policy-v2-final-green.log (2026-09-29 10:06:58 +08:00), 185 tests (system 130 + MES 55), all selected classes including both historical conflict cases and both ReleaseServiceImplTest classes; no skipped tests. Task-owned diff whitespace check PASS. No commits or external activation performed. Integration closeout belongs to coordinator.

BDD: Python消费v2策略 -> Given sourceLocators数组、ownerRole与coverageScope.writeBoundaryScan，When 加载并扫描，Then 每个来源均验证且登记，缺失来源与未登记写入口仍拒绝。

RED: `python -m pytest IntRuoyiBackend/script/tests/test_gxp_audit_method_boundaries.py -q` -> FAIL，33 个 v2 夹具用例失败；旧消费代码只读 sourceLocator 和根 writeBoundaryScan。日志 policy-python-red.log。
实现：使用 YAML 对象读取 operations，严格要求非空 sourceLocators 字符串数组；每个定位均参与源文件/方法验证及登记、注解匹配和摘要。消费 ownerRole、真实 testIds 数组、coverageScope.writeBoundaryScan。报告中的单条 sourceLocator 为扫描发现记录身份，保留原格式；不属于策略输入的旧字段。不改扫描写边界规则或批准排除清单。
GREEN: `python -m pytest IntRuoyiBackend/script/tests/test_gxp_audit_method_boundaries.py IntRuoyiBackend/script/tests/test_gxp_audit_policy_static.py IntRuoyiBackend/script/tests/test_gxp_audit_d09_static.py -q -k 'not reports_current and not finds_current'` -> PASS，47 passed / 2 deselected。日志 policy-python-targeted.log。
回归先运行三个文件全量，44 PASS / 1 FAIL（D09 精确字典缺新版 snapshotProfile，后已补 ATTACHMENT 并在上述47项重验通过）。其中两个全库扫描测试已 PASS：候选发现与真实未覆盖报告；未将检测到覆盖缺口误称为发布门禁PASS。日志 policy-python-green.log 保留首次真实结果。
全库门禁命令：`python IntRuoyiBackend/script/gxp_audit_coverage_gate.py --root . --boundary-report doc/tasks/20260929-deviation-main-integration/policy-python-boundaries.jsonl`；输出 policy-python-coverage.log。未运行 Maven/Java，未修改 YAML/JSON 批准hash。

全库门禁实际 exit=1，FAIL gxp audit coverage gate；未登记写入口 35 项，排除文件哈希变化 113 项。首个未登记入口为 MesProBatchRecordExecutionFieldAuditServiceImpl#saveSystemCellLinkChanges。保留原批准清单，不刷新hash；这些全库覆盖缺口不属于本次schema消费适配。


## 主线稳定前置验证
主线类型检查PASS，前端9文件36项PASS，v2审计130项PASS，Python策略合同47项PASS（2个全库扫描报告测试未在主线重复执行；source已运行）。
来源提交39b53adc2已保存；来源签名13+MES356=369项PASS。

## Main baseline verification updates
- main-stable-final: system GxP tests 130 PASS; MES 578 executed, 18 failures retained as RED evidence.
- main-fixture-final: active-order service 99 PASS after restoring tenant, audit and historical-cleanup dependencies. Batch/NCR remaining fixture failures were not hidden.
- FLOW-16 stale precheck race 2 PASS and manager-stage initializer 7 PASS in stable final run; same transaction is locked/re-read and promoted without replacing its identity.
- Main frontend 9 script files / 36 cases PASS and pnpm ts:check exit 0; source frontend 14 script files / 17 cases PASS and pnpm ts:check exit 0. These are static/behavior tests, not real browser E2E.
- main-fixture-green: 206 PASS, 0 failures/errors/skips; active-order 99 PASS previously. All failures from baseline selected 578 MES tests resolved; GxP system 130 PASS. Main baseline is ready to commit before replaying deviation implementation.

## Source implementation evidence (before rebase)

# 来源集成验证记录

前端14个脚本、17项合同通过；类型检查退出0；ESLint无错误、1条原有warning。
# 批次服务正式完工夹具适配
BDD: 完工批记录上下文 -> Given 测试工单、路线和批号，When 既有业务用例打开批记录，Then 测试解析器提供同上下文稳定的活跃订单、正式完工回执与哈希，重复打开复用同一记录；显式完工请求原样保留。
BDD: 既存批记录补齐工序 -> Given 正式完工来源且缺工序任务的既存记录，When 重开，Then 原批记录任务补齐，保留原有数量与工序断言。
RED: source-merged-tests-green.log / MesProEdhrBatchExecutionServiceTest -> FAIL, 144 errors + 7 failures；空身份夹具被 completionContextKey 拒绝。
约束：不修改生产守卫；只补测试解析器正式完工事实及历史预置来源。解析器和凭据验证器在本类原本就是 mock，此项不是 E2E 或真实回执签发验证。

## 执行与结果
仅测试解析器为未显式 entry 的既有用例构造工单/批号稳定完工身份；显式 entry 继续原样透传。回执哈希使用 SHA-256，来源凭据 ID 与完工回执 ID 保持一致。历史缺工序任务的预插 provisioning record 改为同一正式完工来源，原任务补齐断言保留。

首轮执行（仓库根）：
```powershell
mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-mes -am test '-Dtest=MesProEdhrBatchExecutionServiceTest,MesProEdhrReleaseServiceImplTest' '-Dsurefire.failIfNoSpecifiedTests=false' '-DfailIfNoTests=false' '-Dstyle.color=never' *> doc/tasks/20260929-deviation-main-integration/batch-fixture-green.log
```
结果：BatchExecution 192 项仅剩 1 error / 1 failure；Release 53 项剩 2 个 coverageScope 策略校验错误（主任务处理），同名上层 Release 2 项 PASS。

剩余 BatchExecution 失败定位：首次创建带 routeId、再次打开不带 routeId，因此正式回执夹具不能以可省略请求字段 routeId 作为身份的一部分，改为工单/批号稳定身份；SYNC 审计正式入口已切为 recordInCallerTransaction，测试保留全部业务匹配条件，仅同步方法名。

最终执行（仓库根）：
```powershell
mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-mes -am test '-Dtest=MesProEdhrBatchExecutionServiceTest' '-Dsurefire.failIfNoSpecifiedTests=false' '-DfailIfNoTests=false' '-Dstyle.color=never' *> doc/tasks/20260929-deviation-main-integration/batch-fixture-final.log
```
GREEN: 上述最终命令 -> PASS，192 tests，0 failures，0 errors，BUILD SUCCESS。未修改生产守卫或生产实现；测试覆盖为 mock 解析端口加 H2 服务回归，不是业务 E2E。
# Ledger lock integration
BDD: 签名入口统一锁顺序 -> Given 偏差发起、处理签名和关键偏差转评审与上市放行共享审计账本，When 请求取得业务锁，Then 必须已先取得审计账本锁。
RED: mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-mes -am test -Dtest=MesProEdhrDeviationServiceImplTest#ledgerLockPrecedesBusinessLocks,MesProEdhrDeviationHandlingServiceTest#ledgerLockPrecedesBusinessLocks,MesProEdhrDeviationNcrIntegrationTest#ledgerLockPrecedesBusinessLocks -Dsurefire.failIfNoSpecifiedTests=false -DfailIfNoTests=false -Dstyle.color=never -> FAIL, 3 tests failed because acquireLedgerLock was not invoked. Evidence: review-lock-red.log.
GREEN: mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-mes -am test -Dtest=MesProEdhrDeviationServiceImplTest,MesProEdhrDeviationHandlingServiceTest,MesProEdhrDeviationNcrIntegrationTest,MesProEdhrDeviationSignatureIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false -DfailIfNoTests=false -Dstyle.color=never -> PASS, 34 tests, no failures/errors. Evidence: review-lock-green.log.

Changed only three signature-bearing write entry points to acquire ledger before business locks. save/closeNormally do not acquire the ledger downstream, so no artificial lock added. NCR fixtures now include tenant counter dependencies and use formal active-order origins. Mockito order proves call order, not actual database concurrency.

Experience review: project-experience-consolidation SKILL.md read. Existing docs/backend-development.md destructive-operation read check already requires consistent audit-ledger/business lock order and distinguishes mock evidence from database concurrency; no duplicate long-term rule added.

# 前端冲突融合定向验证

BDD: 活跃订单详情与偏差正式来源隔离 -> Given 活跃订单关联零个或多个正式批记录，When 打开详情及偏差页签，Then 详情直接按 activeOrderId 加载，偏差按正式关联逐批展示；偏差失败不阻断详情。
BDD: 路由切换防串数据 -> Given 旧偏差请求尚未完成，When 切换身份或页签，Then 清空旧关联且拒绝过期响应。

## 持久合同
`IntRuoyiFronted/tests/e2e/active-order-detail-deviation-retains-batch-static.spec.cjs` 保留“详情失败不清除偏差身份”和“新身份先清理旧关联”的业务约束，替换原先强制唯一批次解析的旧实现断言。新增正式批次多条展示、空态、独立失败/重试、主线审计和反查合同。测试目录虽称 e2e，本次仅静态合同，未执行真实业务 E2E。

## RED / GREEN
从仓库根执行：

```powershell
node IntRuoyiFronted/tests/e2e/active-order-detail-deviation-retains-batch-static.spec.cjs --head-fixture
```

RED: 上述命令 -> FAIL, HEAD 主线原文件缺少偏差页签及独立 loadDeviationBatches；4 项中 1 PASS、3 FAIL。`--head-fixture` 使用 `git show HEAD:<正式源码路径>` 只读加载原文件，不改工作树，不改写业务源码制造失败。首次运行发现测试动态正则转义问题，修正为 String.raw 后重新执行，本节记录修正后的结果。

```powershell
node IntRuoyiFronted/tests/e2e/active-order-detail-deviation-retains-batch-static.spec.cjs
```

GREEN: 上述命令 -> PASS, 合并后的工作树文件 4/4 通过。

## SFC 验证
直接 require 根 node_modules/@vue/compiler-sfc 失败是 pnpm 依赖布局问题，非依赖缺失；未安装或修改依赖。通过 Vue 包的解析上下文定位现有编译器。前端目录执行：

```powershell
node -e "const {createRequire}=require('module');const r=createRequire(require.resolve('vue/package.json'));const c=r('@vue/compiler-sfc');const fs=require('fs');const p='src/views/mes/pro/edhr-batch/BatchExecutionActiveOrderDetailPage.vue';const s=fs.readFileSync(p,'utf8');const result=c.parse(s,{filename:p});if(result.errors.length)throw result.errors[0];c.compileScript(result.descriptor,{id:'merge-check'});const t=c.compileTemplate({source:result.descriptor.template.content,filename:p,id:'merge-check'});if(t.errors.length)throw t.errors[0];console.log('PASS SFC parse/script/template',r.resolve('@vue/compiler-sfc'));"
```

GREEN: SFC parse/script/template -> PASS，解析到 pnpm 的 @vue/compiler-sfc@3.5.12。

## 其它验证与边界
- `node tests/e2e/mes-edhr-deviation-p4-static.spec.cjs`（前端目录）PASS。
- 类型检查由主任务统一执行，当前子任务未执行全仓类型检查。
- 未 stage、commit、切分支或写数据库。

## 历史详情入口合同同步与 17 项回归
主代理提供的 `source-merged-front.log`：16 PASS / 1 FAIL，失败为旧 `data-edhr-history-active-order-detail` 标识。核对真实模板，主线已使用 `data-edhr-history-detail-action`，按钮仍明确显示“详情”且执行 `openActiveOrderDetail(row)`；handler 仍带正式 `batchExecutionId` 和历史来源路由跳详情。仅更新测试选择器，并加强为同一个按钮的标识、点击动作、可见文字三者一起断言；未修改业务源码。

GREEN: 前端目录执行下列完整命令 -> PASS 17/17，输出 `source-merged-front-rerun.log`。

```powershell
$frontLog = Get-Content ../doc/tasks/20260929-deviation-main-integration/source-merged-front.log -Raw
$frontContracts = @('tests/e2e/active-order-detail-deviation-retains-batch-static.spec.cjs') + @([regex]::Matches($frontLog, 'tests\\e2e\\[a-z0-9-]+\.spec\.(?:cjs|js)') | ForEach-Object { $_.Value.Replace('\', '/') } | Sort-Object -Unique)
node --test @frontContracts *> ../doc/tasks/20260929-deviation-main-integration/source-merged-front-rerun.log
$frontExit = $LASTEXITCODE
Get-Content ../doc/tasks/20260929-deviation-main-integration/source-merged-front-rerun.log -Tail 10
exit $frontExit
```

## 范围限制
本轮无E2E、数据库写入、策略激活或主线服务重启。历史手动批次路径不能证明正式完工全链路通过。用户要求仅本地合并，不推送远程。

# Runtime policy v2 alignment

## Goal
Make the actual packaged backend policy load against v2 and retain its meaning in activation and reason validation. No database activation or external approval claim.

## Milestones
1. RED: real bundle loading, v2 activation projection, USER_REQUIRED blank-reason rejection.
2. Minimal runtime YAML/schema and consumers alignment.
3. GREEN: ReleaseServiceImplTest and policy loader/activation/reason tests.

## Current Status
ready_for_closeout

BDD: Runtime bundle -> Given the actual META-INF/gxp resources / When loaded / Then all registered operations validate against v2.
BDD: Activation projection -> Given multiple v2 sourceLocators and ownerRole / When operations are projected for persistence / Then every locator and the role are retained.
BDD: Required user reason -> Given USER_REQUIRED / When the reason is blank / Then append fails without an event.

Root config remains historical. Backend config is the Maven-packaged authoritative file. Draft release fragments stay excluded. Existing approval metadata is preserved, not newly asserted or activated.

## Mapping and limits
- sourceLocator -> sourceLocators (array); owner -> ownerRole.
- writeBoundaryScan -> coverageScope.writeBoundaryScan; R1 scope explicitly enumerates the 28 registered operation IDs, with no exclusions. This does not claim full-system coverage.
- REQUIRED_CATEGORY_AND_TEXT -> USER_REQUIRED; add/restore -> SYSTEM per the operation contract. NOT_REQUIRED -> NONE. Existing persisted REQUIRED* policy constraints remain enforced alongside USER_REQUIRED.
- MES profiles follow operation-contract.md. Deviation uses NCR; legacy field/file/signature/permission/configuration/migration operations use explicit EXECUTION_FIELD/CONTROLLED_FILE/SIGNATURE/SECURITY/CONFIGURATION/MIGRATION classifications. These classify snapshots, without inventing additional business behavior.
- Activation persists all sourceLocators as JSON in the existing sourceLocator column and ownerRole in the existing owner column; the canonical policy keeps the full v2 object.
- Historical ACT04 fixture bytes are frozen from the main legacy configuration, retaining SHA-256 5fc4d4be12df26dcb88f127a0e900f0830535ae4db079884d2c4022a3cfee1a0. Production loader rejects it; only explicit H2 test seed constructs its historical record. Current v2 is tested under the same historical version to prove conflict and no rewrite.

## Activation is separate
No database writes or tenant activation were executed. Before deployment, use the controlled activation process with the reviewed bundle hash, coverage hash, tenant and actual approval reference, under the tenant maintenance window; verify ledger/schema prerequisites and all writer versions. Existing code approval metadata is not evidence of a new external approval. Draft terminal/preparation proposals remain outside the runtime bundle.

## Verification
Final command: mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-mes -am test '-Dtest=GxpAuditPolicy*Test,GxpAuditServiceImplTest,MesProEdhrReleaseServiceImplTest' '-Dsurefire.failIfNoSpecifiedTests=false' '-DfailIfNoTests=false' '-Dstyle.color=never'.
PASS: policy-v2-final-green.log (2026-09-29 10:06:58 +08:00), 185 tests (system 130 + MES 55), all selected classes including both historical conflict cases and both ReleaseServiceImplTest classes; no skipped tests. Task-owned diff whitespace check PASS. No commits or external activation performed. Integration closeout belongs to coordinator.


BDD: Python消费v2策略 -> Given sourceLocators数组、ownerRole与coverageScope.writeBoundaryScan，When 加载并扫描，Then 每个来源均验证且登记，缺失来源与未登记写入口仍拒绝。

RED: `python -m pytest IntRuoyiBackend/script/tests/test_gxp_audit_method_boundaries.py -q` -> FAIL，33 个 v2 夹具用例失败；旧消费代码只读 sourceLocator 和根 writeBoundaryScan。日志 policy-python-red.log。
实现：使用 YAML 对象读取 operations，严格要求非空 sourceLocators 字符串数组；每个定位均参与源文件/方法验证及登记、注解匹配和摘要。消费 ownerRole、真实 testIds 数组、coverageScope.writeBoundaryScan。报告中的单条 sourceLocator 为扫描发现记录身份，保留原格式；不属于策略输入的旧字段。不改扫描写边界规则或批准排除清单。
GREEN: `python -m pytest IntRuoyiBackend/script/tests/test_gxp_audit_method_boundaries.py IntRuoyiBackend/script/tests/test_gxp_audit_policy_static.py IntRuoyiBackend/script/tests/test_gxp_audit_d09_static.py -q -k 'not reports_current and not finds_current'` -> PASS，47 passed / 2 deselected。日志 policy-python-targeted.log。
回归先运行三个文件全量，44 PASS / 1 FAIL（D09 精确字典缺新版 snapshotProfile，后已补 ATTACHMENT 并在上述47项重验通过）。其中两个全库扫描测试已 PASS：候选发现与真实未覆盖报告；未将检测到覆盖缺口误称为发布门禁PASS。日志 policy-python-green.log 保留首次真实结果。
全库门禁命令：`python IntRuoyiBackend/script/gxp_audit_coverage_gate.py --root . --boundary-report doc/tasks/20260929-deviation-main-integration/policy-python-boundaries.jsonl`；输出 policy-python-coverage.log。未运行 Maven/Java，未修改 YAML/JSON 批准hash。

全库门禁实际 exit=1，FAIL gxp audit coverage gate；未登记写入口 35 项，排除文件哈希变化 113 项。首个未登记入口为 MesProBatchRecordExecutionFieldAuditServiceImpl#saveSystemCellLinkChanges。保留原批准清单，不刷新hash；这些全库覆盖缺口不属于本次schema消费适配。


## 来源最终定向回归
GREEN: mvn -pl yudao-module-mes -am test (source-test-selection.txt中的15类) -> PASS。2026-09-29 10:15:41，签名模块13+MES356=369项，0failure/0error/0skip；source-stable-final.log。此结果基于640993221与本任务源码，仍须接入主线dirty基线后复验重叠部分。
