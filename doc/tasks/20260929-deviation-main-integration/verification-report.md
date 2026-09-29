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
