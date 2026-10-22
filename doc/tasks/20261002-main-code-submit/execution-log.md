# 执行记录

## 用户授权与范围
- 用户请求：提交推送主干代码。
- 授权：本轮 int_main 提交和推送；不包括发布、服务重启或数据修改。
- 起始 HEAD：fe5323819；当前分支 int_main，索引为空。
- 已读根 AGENTS.md、docs/task-closeout-rules.md、worktree-restrictions.md、powershell-encoding.md 及相关经验；采用 task-closeout-cleanup 与 project-experience-consolidation。
- 项目 AGENTS.md 优先：BDD/TDD 不作为本次提交任务默认门禁；真实 E2E 未获本轮授权，故不执行。
- 待提交既有变更作为独立基线，本次仅新增任务记录。所有输出入库前脱敏。

## 验证与提交
- 远端 fetch 成功，HEAD 与 origin/int_main 均为 fe5323819，ahead/behind=0/0；branch-runtime-port-guard PASS。
- 初始白名单 66 文件，SHA-256 核对及 git diff --check、git diff --cached --check PASS；已选择性暂存基线，尚未提交。
- RED: node tests/e2e/edhr-ai-loop-static-suite.cjs -> FAIL，S05 仍要求已移除的内嵌活跃订单详情标记；S08 独立运行亦因旧历史详情选择器失败。两者不是本次初始变更新增的失败。
- 用户明确选择“先修复该既有失败，再提交推送”；因此修复测试脚本入口及对应断言，保持当前应用交互。
- Root Cause：现行 NCR 使用新建/评审弹窗；新建后返回列表，上传材料为原生文件输入与 ul/li。正式来源为 ACTIVE_ORDER，sourceId 是活跃订单 ID。历史标准列表使用 data-edhr-history-detail-action。旧 S05/S08 脚本未同步以上合同；不能把页面回退到旧布局来通过测试。
- 修复验收：S05 只在正式弹窗中创建/处置、精确选择创建回执对应评审、验证正式来源和材料；S08 用现有历史行详情入口。复跑 20 项静态套件、相关行为测试及新增脚本行为回归。

## 初始提交白名单与 SHA-256

- AGENTS.md | DB51D611745996BDEE973A0ABAFD0B26FAB5C4939D2904470BF641CB1DB2D0CB
- docs/database-rules.md | AFABF9F26D015A3C5A60B7CE3BB6BE0AEC5180B4ACD6F33D2F9C1D33814C0533
- docs/e2e-rules.md | 2D482268431693542E2DB06F476C982B0C26B98B4BBD18346CB6FAD8603FEDA1
- docs/powershell-memory.md | ED7AFE5EF6E2E47C998D76808B6F3638459843256D6566FC46E0D323A1A9C9CC
- docs/task-closeout-rules.md | E4C7E6944F44B54A4B54616DD624C47E208B02F393F7284B2D74FFACDC817430
- docs/worktree-memory.md | 44789E1EB9C4010BAF93A5F206DC0AD6CD726FAE0D60CFF3D3183A473A316697
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/approval/MesFeedbackApprovalErrorCodeConstants.java | 63D46ED661251A94B668F71724F455FDD9F60C18B2D7CB709BA8BB9DC112552F
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/approval/MesFeedbackFormalReviewProjection.java | 432218695CC39A19D01D2A8F6F3585B7785F6D5E34177F78784F1A9E9A72A3CC
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/approval/MesProFeedbackApprovalTaskAdapter.java | AB78133D7E2F8C53A225867A1EDB1B1780EEAEF427D2FA72797861AC20F423BC
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/gxpaudit/MesGxpAuditControllerSupport.java | E0B50C2BD9E4D5D183B736F0609AD0257FFF52FEBD72D4E15E77FC043945C0BC
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/team/MesActiveOrderSignatureEvidenceController.java | D087B98FA8B5C81A538513E9D362BE38D349C638B64163E691651F449F987467
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/team/vo/MesActiveOrderSignatureEvidenceRespVO.java | 7890553E4122710260612D82EC57E8BAF3A5E6A65EF221224617658B91477F60
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/feedback/MesProFeedbackMapper.java | 3C088A64A1B51080CB6FE1D9BA09113FB4A76FAA1DF52E7729FB59CA9FE0D6E2
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/processpool/MesProProcessPoolEventMapper.java | 70772B3B2069E8264E0772A80235914E9B3FA01BD12FD121BAC7A1A12EB52F81
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/processpool/team/MesProcessPoolSubmissionReviewMapper.java | FBA126824CC4F3340497D68981EF7183425A0DA015BA57A9E07C25FFCDF39223
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProBatchRecordExecutionSignatureService.java | CF1ED603ECCFC5B7A0722BC7355B6F16248B52065521F7FD3E609969BAC82735
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProductionSubmitSignatureContext.java | 80CAA634299B3D392EE0EFBBE1D92F48FB22A42331DCA9A413B6A7B378F06687
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/feedback/frontline/MesProFrontlineFeedbackSubmitServiceImpl.java | B1FC4C7250B7A58E5B303B9B426B182B3948532E867279899291E8CDC2F6D243
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/feedback/MesProFeedbackServiceImpl.java | 6A88683EF4ECCE6C30A65DBFE541D0415D75DDE4342B951A6BF6CE2ADB4051CA
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/frontline/MesFrontlineRouteProcessTemplateBindingSource.java | A40D41918F013E980D5BB6D9160058B1FEC3D3D86AD2BB94D0F429355596E5B9
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesActiveOrderSignatureEvidenceService.java | 3422AAFD9B85224C0FF9261F38A265F144A5B590909401675181BABD14537B91
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/approval/MesFeedbackFormalApprovalQueryContractTest.java | 374DDA4566DFB0429B9385CAF6F3ED6CC3DC90F0ABB47683F926EEF9B284D502
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/approval/MesFeedbackFormalReviewProjectionTest.java | F6E1F863519FA56EF33A839D41CE1DF63467CD72A0C1166D22C4581569E3AAD1
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/approval/MesProFeedbackApprovalTaskAdapterTest.java | CB8BFE7F36E166A1BD8981C631B5B7FB9D77CF55A276F69DCFDD15678CC8B210
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/approval/MesProFeedbackFormalApprovalTaskAdapterTest.java | 11CF4CC0254ECD218FDC87C453C7C2E38D9477FFB2B6F207C0A11043A25F8312
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/controller/admin/pro/gxpaudit/MesGxpAuditTimestampSupportTest.java | 83BAA9F881DF5EE1108094984973FE236B257228AD889E2BFCF68F716B4747FA
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/team/MesActiveOrderSignatureEvidenceControllerTest.java | 09C12B005A389F889917CA1A1908190BC4442784AEDC8926D20C74201E2CB125
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProBatchRecordExecutionSignatureServiceTest.java | 55BD9086F093B9BE81A14BD013856956394BC94AD9F21676FFF48B6FEA19C6C6
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProductionSubmitSignatureContextTest.java | 7D9EB7F578BE7661C9D8A6D7CE0393DF10188E0BA2B65884F15D00EA59D913A5
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesSignatureProjectionAuditTransactionTest.java | 56F9187712E051C119F04F83DC6A2D9F9474AF4A663D40E33B988727BCDA0C2D
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/feedback/frontline/MesP0ProductionSubmitClosedLoopContractTest.java | BD1E890AFE2B692CB6B7DAE7B13ACB9C5A1BA01ED076C05CEB0A82052751290E
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/feedback/frontline/MesProFrontlineFeedbackRawLimitBypassTest.java | 72F3A114C86994B36B618A053BCA40BE06C40966D4ED966CF5C5DBB8A852DDEB
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/feedback/frontline/MesProFrontlineFeedbackRouteOrderGateTest.java | 4B010E10570C77C132EAC729C0FF80F1A8BB780B028F51A184137981D34331F3
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/feedback/frontline/MesProFrontlineFeedbackSubmitDetailContractTest.java | 6485C632FFA143FB88B8EE630F03F6D8D8B062CECF9D32CA0BE502DB1E1AA572
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/feedback/frontline/MesProFrontlineFeedbackSubmitRollbackTest.java | 772DCE493EF0341A20E0365A8527695C4C9833848938A55D6B6C1BA2CA4FEB19
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/feedback/frontline/MesProFrontlineFeedbackSubmitServiceTest.java | F775B730A307D94B06B90B5914E34548EE299C89203B0AB77D2E016C41DB5693
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/feedback/MesProFeedbackLegacyFormalGuardTest.java | 9734470117EECAB0E68F65CA206304F5A61C1AAE9569EFB599938AE2B2ADA9BC
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/feedback/MesProFeedbackServiceImplTest.java | 9E6234B3E447941402B9A732F11DA38D707C55EB07612C09E7A5998D5526E295
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/frontline/MesFrontlineRouteProcessTemplateBindingSourceTest.java | 744E0917BCD816424DF5E73A75A9D1DAC2BC26300DA354E0BB720759B7FB7202
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesActiveOrderSignatureEvidenceServiceTest.java | 9CA7D2A9A2E07DB57A93EAA40AEF6947EC6B5ABCDF0FC796984D82491F2768A2
- IntRuoyiBackend/yudao-module-system/src/main/java/cn/iocoder/yudao/module/system/controller/admin/gxpaudit/GxpAuditEventController.java | E6B49076E971637C0327BE39710C88EB664F6601D4C05D5A119F4A947CEF0360
- IntRuoyiBackend/yudao-module-system/src/main/java/cn/iocoder/yudao/module/system/controller/admin/gxpaudit/vo/GxpAuditEventPageReqVO.java | EC95E6C0307533739BD9A21F19886A6CCCB80B06903601B9B9EF4923C66513CC
- IntRuoyiBackend/yudao-module-system/src/main/java/cn/iocoder/yudao/module/system/controller/admin/gxpaudit/vo/GxpAuditEventRelationRespVO.java | BCBD2042A2AFA5CD4B42DA12320E48E7E6D5A0B5506639D4713539EF694782CF
- IntRuoyiBackend/yudao-module-system/src/main/java/cn/iocoder/yudao/module/system/controller/admin/gxpaudit/vo/GxpAuditEventRespVO.java | F52544635892B7E49E3BDD07AB9BBA7A861D5A002A4601558A36891D8DDD6397
- IntRuoyiBackend/yudao-module-system/src/main/java/cn/iocoder/yudao/module/system/controller/admin/gxpaudit/vo/GxpAuditUtcTimestampSerializer.java | 58DF16825125E881AC9D945AF5B2408741A3D4B3D365DC23E76A8C4830E1379E
- IntRuoyiBackend/yudao-module-system/src/test/java/cn/iocoder/yudao/module/system/service/gxpaudit/GxpAuditTimestampContractTest.java | FBE2EC5A347B72D1B508AFAC12DF419597E11A702245B1DFD3D39F975F065140
- IntRuoyiFronted/src/api/mes/pro/edhr/activeOrderAudit.ts | A38DBFAC971851025AFEDED00F69065824A8B4215A57E69F88A5DB87B2B607E4
- IntRuoyiFronted/src/api/mes/pro/edhr/activeOrderSignature.ts | 7FCD954F28B460266E52EE9858FBE0A849F69B0E440C498330AB23BDF509AE63
- IntRuoyiFronted/src/views/approval-center/index.vue | 58B674BAC492A07569864B754E4393663DAB3416CFB1804C4451630EC06C0E3A
- IntRuoyiFronted/src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue | B337A1D5723C46382739EF789B1D5316CCB3FAEA5E1B838E984525AE03528DAD
- IntRuoyiFronted/src/views/mes/pro/processpool/active-order-signature-evidence-behavior.spec.cjs | 72D91B009F1DCED155746AC4BA510FC580585BC5BEEFD9CCD45A36A5473066C8
- IntRuoyiFronted/src/views/mes/pro/processpool/components/activeOrderSignatureEvidenceViewer.ts | 58C8F698A2618A20946EE685D2C30640D0A536E367D110BE732ED81D35F8C1EB
- IntRuoyiFronted/src/views/mes/pro/processpool/components/ActiveOrderSubmissionDetailPanel.vue | CF1A67336D00A9F3684083CA9EF72F9223B5A32966337632485BA408519FB7E0
- IntRuoyiFronted/src/views/mes/pro/processpool/production-review-event-navigation.behavior.spec.cjs | 40552A8334945768D9411B616426884BED247F1649B2EEB3F1A78D019A5223BD
- IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue | 9A85770033B1FAB9989701A8A42759469F44EA69A4406B6B9C7187BDD2D6313A
- IntRuoyiFronted/tests/e2e/approval-center-chinese-copy-static.spec.js | 2B04148EA8BA7F6B71A41D7C528AC2683C86B54156F6A7BD0ECF1718818AE49A
- IntRuoyiFronted/tests/e2e/edhr-ai-loop-dynamic-coverage.spec.cjs | F4EBFBCF1F611D0C42F1EA7ABE47F21AEE749D6FD798CFB2D95549E6E18B3FBD
- IntRuoyiFronted/tests/e2e/edhr-ai-loop-orchestration.spec.cjs | 1C9D80BFCC58AD6335442B09B2D004412F6C7F8BEF976A451DCD8ABB04F6682B
- IntRuoyiFronted/tests/e2e/edhr-ai-loop-pqc-s03-static.spec.cjs | D65FDD902B244A64CFFD0178B77052FE01B7909B28E90B218E7F8B3D64175BEA
- IntRuoyiFronted/tests/e2e/edhr-ai-loop-pqc-write-safety.spec.cjs | BAC622A0FB91EA50FB1F9B6118F6A63AC26580C46F7E3EC318FFDA52CA06CBB8
- IntRuoyiFronted/tests/e2e/edhr-ai-loop-static-suite.cjs | A13519468C438750528E9056343C5FF8D9DDAB31933F52CAE16CF6BF4034CCFA
- IntRuoyiFronted/tests/e2e/edhr-ai-loop/coverage.cjs | 6D096641D39F7CC3B28AEAC9452FCC296F263E825315A9109157C886DC038EF9
- IntRuoyiFronted/tests/e2e/edhr-ai-loop/runner.cjs | 1D53589F6B5B1EB5A7EE85CC634924B526A0A8D6CE6EF7C4647CA45A0B85D4B9
- IntRuoyiFronted/tests/e2e/frontline-production-scaled-action-space-static.spec.cjs | 0A4A07F4BE78A419148EB3ACCFDF82F245853E131C5B136A7D7D12E0B770724E
- IntRuoyiFronted/tests/e2e/frontline-production-scaled-action-wrap-static.spec.cjs | 2BE5EDF3A19DE9A3361B367430E3822AA2A16CAD995723C90E4F02BE19EF8F62
- IntRuoyiFronted/tests/e2e/team-leader-workbench-static.spec.cjs | 2400730F5F395BE13F6971370F31314B387E9F28ABF9BB72A71A32CD581D4622

## 后端本轮实际验证

- Command: mvn --batch-mode -f IntRuoyiBackend/pom.xml -pl yudao-module-mes,yudao-module-system -am test -Dtest=MesFeedbackFormalApprovalQueryContractTest,MesFeedbackFormalReviewProjectionTest,MesProFeedbackApprovalTaskAdapterTest,MesProFeedbackFormalApprovalTaskAdapterTest,MesGxpAuditTimestampSupportTest,MesActiveOrderSignatureEvidenceControllerTest,MesProBatchRecordExecutionSignatureServiceTest,MesProductionSubmitSignatureContextTest,MesSignatureProjectionAuditTransactionTest,MesP0ProductionSubmitClosedLoopContractTest,MesProFrontlineFeedbackRawLimitBypassTest,MesProFrontlineFeedbackRouteOrderGateTest,MesProFrontlineFeedbackSubmitDetailContractTest,MesProFrontlineFeedbackSubmitRollbackTest,MesProFrontlineFeedbackSubmitServiceTest,MesProFeedbackLegacyFormalGuardTest,MesProFeedbackServiceImplTest,MesFrontlineRouteProcessTemplateBindingSourceTest,MesActiveOrderSignatureEvidenceServiceTest,GxpAuditTimestampContractTest -Dsurefire.failIfNoSpecifiedTests=false -Dstyle.color=never
- Maven exit=0 / BUILD SUCCESS；Fresh Surefire: {"tests": 155, "failures": 0, "errors": 0, "skipped": 0}
- MesFeedbackFormalApprovalQueryContractTest: 4 tests PASS
- MesFeedbackFormalReviewProjectionTest: 14 tests PASS
- MesProFeedbackApprovalTaskAdapterTest: 10 tests PASS
- MesProFeedbackFormalApprovalTaskAdapterTest: 10 tests PASS
- MesGxpAuditTimestampSupportTest: 2 tests PASS
- MesActiveOrderSignatureEvidenceControllerTest: 3 tests PASS
- MesProBatchRecordExecutionSignatureServiceTest: 20 tests PASS
- MesProductionSubmitSignatureContextTest: 2 tests PASS
- MesSignatureProjectionAuditTransactionTest: 10 tests PASS
- MesP0ProductionSubmitClosedLoopContractTest: 2 tests PASS
- MesProFrontlineFeedbackRawLimitBypassTest: 1 tests PASS
- MesProFrontlineFeedbackRouteOrderGateTest: 1 tests PASS
- MesProFrontlineFeedbackSubmitDetailContractTest: 3 tests PASS
- MesProFrontlineFeedbackSubmitRollbackTest: 3 tests PASS
- MesProFrontlineFeedbackSubmitServiceTest: 20 tests PASS
- MesProFeedbackLegacyFormalGuardTest: 3 tests PASS
- MesProFeedbackServiceImplTest: 21 tests PASS
- MesFrontlineRouteProcessTemplateBindingSourceTest: 6 tests PASS
- MesActiveOrderSignatureEvidenceServiceTest: 17 tests PASS
- GxpAuditTimestampContractTest: 3 tests PASS
- Git initial staged whitelist 66 paths MATCH；all other baseline hashes unchanged；unmerged=0；added-line credential signature scan=0；normal and staged diff whitespace PASS.

## 实现与经验提交
- Baseline commit: 1adade43aea059424c9db34376557b91eab65dfd，66 文件；完整清单见初始白名单。此提交保存已有变更，随后实现提交修复已确认的基线测试失败，组合结果已验证。
- Fix commit: e58b9eba7dd4139f6142518426241a241f128a00，6 文件：
  - IntRuoyiFronted/tests/e2e/edhr-ai-loop/runner.cjs
  - IntRuoyiFronted/tests/e2e/edhr-ai-loop-release-s05-static.spec.cjs
  - IntRuoyiFronted/tests/e2e/edhr-ai-loop-archive-s08-static.spec.cjs
  - IntRuoyiFronted/tests/e2e/edhr-ai-loop-dialog-contract.spec.cjs
  - IntRuoyiFronted/tests/e2e/edhr-ai-loop-static-suite.cjs
  - docs/e2e-rules.md
- GREEN: node tests/e2e/edhr-ai-loop-static-suite.cjs -> PASS，21 specs；其中错误中止测试输出预期的模拟 FAIL 日志，测试总体退出码为 0，不是真实业务 E2E。
- GREEN: node --check tests/e2e/edhr-ai-loop/runner.cjs -> PASS。
- GREEN: bug-regression-fix-loop validator -> PASS，证据已归入 verification-report.md。
- project-experience-consolidation：复用 docs/e2e-rules.md 记录弹窗/列表改版需同步 runner 的定位、正式来源、异步加载与错误中止边界，未新增长期经验文件。
- 两次 commit hooks 端口门禁 PASS；实现完成后状态已设为 ready_for_closeout。

- GREEN: cleanup preview/apply -> PASS；keep=3、delete=20、blocked=0、warnings=0。20 个删除项仅为本任务 verify.py、原始测试日志与 JSON 汇总，核心测试与验收证据已归档。当前为主工作区，未合并或删除 worktree。

## 推送与最终收尾
- GREEN: git push origin int_main -> PASS，fe5323819..e58b9eba7；git status --short --branch 仅分支行，git rev-list --left-right --count HEAD...origin/int_main 为 0/0。
- 所有本次运行的验证命令已结束；没有创建或遗留任务自有服务。
- 收尾提交仅包含 doc/tasks/20261002-main-code-submit/{task.md,execution-log.md,verification-report.md}，通过 git add -f 精确加入被全局忽略的任务记录；不纳入其它任务记录。
- 收尾提交标识：父提交 e58b9eba7dd4139f6142518426241a241f128a00，消息 docs: complete main code push verification；可用 git log -1 --format=%H -- doc/tasks/20261002-main-code-submit 定位其最终 hash，避免自引用 hash 无法写入自身提交的问题。
- 最后提交推送后执行 git status、ahead/behind 和 git ls-remote origin refs/heads/int_main，对照本地 HEAD 与 origin/int_main；核验结果以本轮最终工具输出为准，任何失败均不得报告完成。
