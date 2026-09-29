# 执行记录

2026-09-29：用户要求融合进 int-main；实际主分支为 int_main。
已读取 AGENTS.md、task-closeout-rules.md、worktree-restrictions.md 及收尾/经验技能。
主分支 HEAD 895272ee0，来源分支 codex/piancha-0923 HEAD f46b0bdc2；rev-list 左右计数为 12/0，来源实现仍有大量未提交及未跟踪文件。
只读检查发现下列重叠修改：
- MesProEdhrNonconformanceReviewController.java
- MesProEdhrNonconformanceReviewMapper.java
- MesProEdhrBatchExecutionServiceImpl.java
- MesProEdhrNonconformanceReviewService.java
- MesProEdhrNonconformanceReviewServiceImpl.java
- MesProEdhrBatchExecutionServiceTest.java
- IntRuoyiFronted/src/api/mes/pro/edhr/nonconformanceReview.ts

BLOCKED：主分支共享文件存在未提交并行变更；根据 AGENTS.md 不动其他任务资产及 cleanup 主工作区必须清洁的规则，未执行合并。
未执行构建、E2E、数据库操作、服务重启、提交或推送。
历史验收声明须与实际证据重新核对；本轮没有将其认定为所有用例通过。

用户确认授权：接管主分支现有未提交改动，统一审查、处理冲突并提交合并。重新进入 in_progress。

## 本轮复验
- 端口门禁 PASS，git diff --check PASS。
- pnpm ts:check PASS（relaxed配置，退出码0）。
- 主线9个前端测试文件共36项：35 PASS / 1 FAIL。edhr-nonconformance-review-material-filename-static.spec.cjs 的正则依赖函数相邻顺序，匹配失败；源码解析函数实际存在。
- Maven选定18类541项：337通过、1 failure、203 errors，exit1。其中200 errors来自class资源消失导致Spring上下文初始化失败，其余4项位于正在修改的管理代表阶段初始化测试。运行受到共享目录并发编译/编辑影响，不能作为稳定版本的产品回归结论。
- 初始清单没有的 MesProductionReleaseManagerStageInitializerImpl.java 在检查期间被修改。
- 只读查看“主流程优化”任务（01a0e595-2ef0-7741-9702-6eeaf9084e95），确认该任务当前用户要求“先修复，然后提交”，正在修改相同目录并执行Maven。
- 本任务启动的全部检查已结束。不再并发运行Maven。未修改生产代码，未提交、合并、推送、重启或写库。
- 已有 docs/worktree-memory.md 包含并行编译释放及dirty归属门禁，本次复用，不重复新增共享规则。
- 下一步：待主流程优化修复并提交，重新核对main HEAD和基线，处理偏差分支冲突后集成。接管授权继续有效，无需重复确认。
2026-09-29 用户确认其他任务已停止，授权继续合并。main HEAD=640993221；无未解决Git冲突，无运行中Maven。已接管主线现有修复，先补齐并验证，再分别保存基线与偏差实现。
主线前端旧静态合同已按新函数边界修正；先前实际FAIL仍保留，首次修正仍FAIL，原因包含Windows CRLF；随后规范化源码换行再验证，不修改生产解析逻辑。

用户明确选择仅本地合并，不推送远程。继续本地提交与融合；项目推送完成门禁未执行，最终报告明确区分本地融合与远程交付。

主线稳定回归578项暴露22个失败（7failure/15error）：旧审计方法断言、两个手工构造服务未注入新增审计依赖、活跃订单夹具缺租户与未归档草案。保持生产守卫，补齐测试前提后复验。
主线验证前提修复：仅为测试注入已有审计依赖，mock账本水位和已激活版本以到达缺操作策略的真实失败分支；设置/清理租户；原始草案归档src/test/resources/gxp，不把草案纳入运行策略。
主线基线也包含来自source已验证的GxP v2加载/激活/原因修复及Python消费者契约；主线配置暂不含偏差操作，版本20260929-gxp-contract-01，偏差最终融合再加入独立操作。没有数据库激活。
审计全库覆盖扫描仍FAIL：35未登记写方法、90未批准候选、113批准哈希过期。此为发布覆盖门禁，本次本地合并的定向回归不等于生产放行；原批准清单不改写。

来源实现已提交39b53adc2（140个文件），来源369项定向回归与策略185项回归通过。主线基线尚待最终回归；后续将重放该本地提交到主线基线，以快进方式融合。不推送远程。

合并期间新增无关DCC输入worktrees/.ports/1.上传操作流程，连同crow-bike.svg、pelican-cycling.html均保留原文件并排除提交。不会覆盖或清理。
RED: main-stable-final Maven -> FAIL, 578项中5失败13异常；新增清理Mapper/签名证据依赖未进入旧测试夹具，审计断言仍调用旧record。保持生产校验和原业务断言，补齐真实契约的测试输入。
BDD: standalone precheck versus formal approval -> Given the same transaction is promoted to PENDING_APPROVAL, When a stale precheck runs, Then lock/re-read rejects overwriting the formal approval.
RED: MesReleasePrecheckPromotionRaceTest -> FAIL, both transaction-id and batch-id paths incorrectly completed the stale update.
GREEN: main-stable-final Maven MesReleasePrecheckPromotionRaceTest + MesProductionReleaseManagerStageInitializerTest -> PASS, 2 + 7 tests. Precheck locks/re-reads; manager promotion retains the transaction identity.
GREEN: main-fixture-final Maven MesTeamLeaderActiveOrderServiceTest -> PASS, 99 tests. Tenant and historical cleanup mapper fixtures restored; production guards and original assertions retained.
GREEN: main-fixture-green Maven -> PASS, BatchExecution 200 + NCR manager 6; together with active-order 99 and unchanged successful classes in main-stable-final, all 578 selected MES baseline cases and 130 system cases have passing evidence. No production guard weakened.

## Source implementation evidence (before rebase)

来源实现验证开始。主任务记录位于 E:/IntRuoyi/doc/tasks/20260929-deviation-main-integration。

来源回归：mes 328项，8 failures / 9 errors；签名模块单独结果见日志。11个批次整类历史失败待主线融合后复验。新来源守卫使4个偏差测试夹具缺正式来源；补齐H2真实关联，不放宽业务守卫。两个前端脚本从仓库根启动导致ENOENT，重跑从前端目录；列表旧断言仍要求主线已删除的当前工序/填写人/进度列，更新为现行列表合同。
BDD: 偏差只选择正式来源 -> Given 同租户正式与无来源批次，When 查询候选或发起偏差，Then 只接受正式活跃订单来源；签名失败仍回滚所有偏差写入。
RED: source-tests.log -> FAIL, 新来源守卫导致缺来源的4个夹具失败。
来源47项核心定向回归PASS，前端类型与ESLint PASS（1既有warning）。整类旧基线仍有13项失败，不提交失败基线。为接入已验证主线修复，临时保存本任务自有未提交源码，快进到主线后原样恢复并逐处解决冲突；不处理主线stash。

融合编译RED: source-merged-tests.log -> FAIL, 偏差NCR旧编号函数调用与主线全局序号新签名不匹配。已统一调用主线buildReviewCode(now)，保留新编号机制。

融合回归发现既有Release测试依赖未入Git的GxP任务草案。已从原GxP集成工作区读取原始草案，归档为src/test/resources/gxp正式测试夹具（不进入生产策略），修正缺策略用例沿用0租户却mock1租户的夹具矛盾。原始FAIL保留于source-merged-tests-green.log。

用户明确选择仅本地合并，不推送远程。继续本地提交与融合；项目推送完成门禁未执行，最终报告明确区分本地融合与远程交付。

经验沉淀：复用docs/backend-development.md审计策略门禁，补充真实打包资源不得由fixture转换掩盖、升级必须覆盖加载/激活/运行消费链。仅本任务经验变更，未新建长期文档。


## Runtime policy v2 alignment
BDD: Runtime policy loads -> Given actual Maven-packaged META-INF/gxp resources / When strict loader reads them / Then all 28 operations satisfy v2 schema.
BDD: Activation preserves metadata -> Given v2 sourceLocators and ownerRole / When projecting immutable operations / Then all locations and role persist.
BDD: Required reasons -> Given USER_REQUIRED or persisted REQUIRED/REQUIRED_CATEGORY_AND_TEXT / When reason is blank / Then no audit event is appended.
BDD: Historical policy remains immutable -> Given frozen legacy artifact and activation / When current v2 uses the same historical version or request / Then activation rejects and old records remain unchanged.
RED: mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-system -am test '-Dtest=GxpAuditPolicyActivationServiceContractTest,GxpAuditServiceImplTest#userRequiredPolicyRejectsBlankReasonWithoutWritingAudit' '-Dsurefire.failIfNoSpecifiedTests=false' '-DfailIfNoTests=false' '-Dstyle.color=never' -> FAIL, real bundle missing coverageScope and USER_REQUIRED accepted blank reason; policy-v2-red.log.
RED: mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-system -am test '-Dtest=GxpAuditPolicyActivationServiceContractTest#activationProjectionPreservesV2SourceLocationsAndOwnerRole,GxpAuditServiceImplTest#userRequiredPolicyRejectsBlankReasonWithoutWritingAudit' '-Dsurefire.failIfNoSpecifiedTests=false' '-DfailIfNoTests=false' '-Dstyle.color=never' -> FAIL, v2 projection demanded obsolete sourceLocator and legacy REQUIRED constraints were dropped; policy-projection-red.log (controlled old projection, then restored).

GREEN: mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-mes -am test '-Dtest=GxpAuditPolicy*Test,GxpAuditServiceImplTest,MesProEdhrReleaseServiceImplTest' '-Dsurefire.failIfNoSpecifiedTests=false' '-DfailIfNoTests=false' '-Dstyle.color=never' -> PASS, policy-v2-final-green.log; includes two historical immutable conflict scenarios and actual packaged runtime policy.

GREEN: 来源15类最终回归 -> PASS 369项；2026-09-29 10:15:41 source-stable-final.log。准备独立实现提交，后续重放到主线基线。

2026-09-29 reboot resume: source replay commit 73587dae8 is based on main baseline 613da490f; tracked workspaces clean. integrated-final was interrupted before completion and is not PASS. Re-running union Maven selection and frontend type check; frontend static/behavior checks already passed 17 + 36 cases. No remote push, DB write, service restart or browser E2E.
Conflict review: retained main NCR formal-file identity/upload page; API has both formal-file upload and critical-deviation creation; retained both service imports. Final v2 policy includes 28 operations and deviation version 20260929-edhr-deviation-01. Combined task evidence preserves both baseline and source records.
GREEN: integrated-after-reboot union Maven -> PASS, 884 tests (130+13+741), zero failures/errors/skips; frontend 53 cases and pnpm ts:check PASS. Reboot-interrupted run excluded. Source and main port guards PASS.
Cleanup source preview/apply PASS: ready_for_closeout, keep 3 core records, delete 41 task-owned temporary files, no blockers/warnings. Core reports contain the consolidated test evidence. worktree-closeout off because formal E2E remains outstanding; source checkout retained.
