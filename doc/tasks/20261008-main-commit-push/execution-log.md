# Execution Log

## 授权与范围

- 用户当轮指令：提交推送主干代码。
- 执行范围：当前主工作区可归属的主干代码快照、Git 推送及本任务记录。
- 使用 task-closeout-cleanup 与 project-experience-consolidation 技能。

## M1

- 已读取提交/收尾、分支端口、worktree、UTF-8 与 PowerShell 规则。
- 已检索 docs/worktree-memory.md；现有“主干快照提交”规则涵盖本任务所需经验，暂无新增长期经验。
- Git 状态扫描进行中，尚未暂存或提交。

- 新增行凭据模式扫描：1768 行，未发现私钥、令牌或明确凭据赋值模式；此项不等于完整安全审计。

## M1 / M2 快照完整性

- 起始 HEAD：033be6219eb066b7ca9ba157c197979d3618fa18。
- 精确暂存 57 个文件，暂存前后 SHA-256、HEAD、路径与索引正文一致。
- 端口守卫 PASS：int_main/int_main，8081/48081。
- UTF-8、冲突标记和重复回车检查 PASS；git diff --cached --check PASS。
- 冻结后新增的其他任务文件不纳入。

| Path | SHA-256 | Git blob |
| --- | --- | --- |
| AGENTS.md | 90438d5da58d660796f668b0e7229c2b04d9250d34b1a8e55c8f1efc9d50f597 | 39b61b5ecf179aadc532ec20b44d0ad8b983f482 |
| IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/feedback/vo/frontline/MesFrontlinePqcProcessRespVO.java | 4945a3ec9447a683d466132efa26e4b3e9c6a18791fafb550742ed0312672f76 | 928d539d8242073eb5e385c835bd12d1395bc2a6 |
| IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/team/MesProcessPoolTeamLeaderController.java | 94e406e42ec4fef7c3d443b30e45bdc169ce167c664f58fc81ea11d1a2a8806e | 7c0954c1753b2ed0bb561400eeebd168a5359b94 |
| IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/team/vo/MesTeamLeaderActiveOrderDetailRespVO.java | 8d2bede952b38609724645dda757f2692daae299cf73d7d99b5db50c70a80f3e | 9e5834e0f75fbc2f305db804c7a74a873ba12eaf |
| IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/processpool/team/MesProcessPoolActiveOrderDetailReadMapper.java | db7e405d73f9dd33ab1741fc901d496ee5c17c1457514a6d51240f7d136ff3ce | e2cb7027897a59c1e773b8248da421939342b927 |
| IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesEdhrQaDispositionAggregateHash.java | b46b9d61223e49cd28df673c22d740b50160c098302f6da29cf6d1c0555505d5 | 87b1cc82b636107b5f5ef37393054ccc077e24be |
| IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrBatchExecutionServiceImpl.java | e8b3fea28ebd1de9399697fda56ee51307a5a196bb61653835aed9d16ed9839b | efc45ada48d865a8b0ce1d19c862a62ca42cc81b |
| IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrNonconformanceReviewServiceImpl.java | 7f45616b38119ef3e7c0abfdc3170475b8aa3c52d31a642fdff7e5b4b8c561e1 | db9178464e95763365172208e978afbd0d914b77 |
| IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrProcessFormPermissionRuleServiceImpl.java | 332ff7c91ead96d2f08dae59006387de2d12adbea4036ad8c8149804e693b6bf | befecd7fe6c576e8b372698fcee089b52e5695fb |
| IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrWorkTaskServiceImpl.java | 74c94f595b13fca5c01887ce0383041540e8461ad28230cae8af5a3945555d41 | f7ec274d6aef995faa92a27ce3e2aa0c2428da5b |
| IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/frontline/MesFrontlinePqcContextServiceImpl.java | ae2fc30b722b35ddb1309513e7b3fd309a8bead5ac6afb687237fafe314684b0 | 4009b971337ba571acd45393dc6577b12ff0523b |
| IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/frontline/MesFrontlineSubmitAuthorizationServiceImpl.java | 9235e8d6a2a6f45e9b150506d2f834b2e74f68cc0ec21e22ad6bc5c4b2a54429 | d57233135ed7dbcd9ef19d5bb8caf6300fc601b0 |
| IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderDetail.java | 15d38c67a9b22e2ea7006e72f1ad250f739f0ce8c0ce53bbaec756d537a06a71 | 10ef2527f35240d60b1e05af06098d607b02d6c8 |
| IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderDetailServiceImpl.java | 6f9e8c3c8f70b6ec5897fcf6563d3d0c53e22071c6f9495765adf640f5c9f4a0 | cf7ff9674ea3aa6143b9f0b5daa998410cbaa7d4 |
| IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/simulation/stage2_5/MesStage2_5BackfillBatchExecutionSimulationServiceImpl.java | dbb7b2ce528f30aa650290934d2349f8ae687a6ec7c1a4780cda6be4fbe65efc | 774939247dba5ff455b25756170eba40ead7464f |
| IntRuoyiBackend/yudao-module-mes/src/main/resources/mapper/pro/processpool/MesProcessPoolActiveOrderDetailReadMapper.xml | 3cfd753a1b92b2217f71af0cbd4a9df4931ddfa9b20d24652a314d6ac753d62c | d8a9659a7659d393dbb981c50527314f72f9c5df |
| IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/processpool/team/MesArchivedReworkDetailMapperTest.java | e3344e395127b56ec3c5890df067bd6d3ac8d81878bec17ac7062ad4bb036002 | 53fc930c91126a36475b5e2222ee282a2b37e8d8 |
| IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesNcrScopeAuditTransactionTest.java | 191a39571aeaaa9f4271cc95d23acb3c1dd725ff1b3f5d5878a3841d3185182a | c7cbf70162565fc894b1a515e86911bb54817ce1 |
| IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrBatchExecutionServiceTest.java | 33dd7574e237fdef86965925979caabd010754c6772f5ffaa65c009508633673 | 408173c72f40fedb767d4eff43e2b0811862c687 |
| IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrProcessFormPermissionRuleServiceImplTest.java | 0678d148afe67b3bdd66d7035c31eb32879bb7541c7ef9f87d595f616a7001bb | 0c2cbadd2eca82e507b62f3882e96ac300bcbcb1 |
| IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrWorkTaskOwnershipTransferTest.java | 7904cab1e00ce6322a9c4476f5a173276e73dfb2e8466cd94521a4f5c3c27a22 | 1cb6c9cb1351b05bb4f4765a9c9cd322ff67e460 |
| IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/feedback/frontline/MesProFrontlineFeedbackSubmitServiceTest.java | ee4d7627db769c935f5560f28de14498f3241b12af381ed331fb6552f559513a | 854704b856e11b88f4dbf1c5651d343db40b1597 |
| IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/frontline/MesFrontlinePqcContextServiceTest.java | 6a024bf214e790d6956906faed8f91c137973d1a7fd53c5ee312d63a320ff93f | 1ca720ce0e6f9231228b52223f6917d05f11bf0f |
| IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/frontline/MesFrontlineSubmitAuthorizationTest.java | 29575c95ea302cae10ef3fca563a972f6f841d1772edbe282e20b9bfce0f6368 | bf3421429f60a9704cd9323362497dc560155fc7 |
| IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderDetailServiceImplTest.java | 0e406cbaf4b520d5bd43a0f158e72766b0084b6bbdc28b5b082bc52ba7115498 | 39a27d38e76bc7fb14b2aac162cffe8868042821 |
| IntRuoyiBackend/yudao-module-showroom/src/test/java/cn/iocoder/yudao/module/showroom/integration/ShowroomHttpApiIntegrationTest.java | 842eea9483876037e5b68ca70c55b8e7785ac0f0a2cfa20a2fc96cac4e515c73 | 893989e4e1f0f7101f326fcd5e6224e0d4b1bd54 |
| IntRuoyiBackend/yudao-module-showroom/src/test/java/cn/iocoder/yudao/module/showroom/workflow/ShowroomAssignmentWorkflowTest.java | 85e6fea9f4c7b0adfd6021d9815be682efa97f00f31a32cdc3cd76f403aecc13 | 2b5d0607073ebd32a4fef822d625891302c67369 |
| IntRuoyiBackend/yudao-module-showroom/src/test/resources/sql/create_tables.sql | 6c6f7e12d51453279c7c9155a604d39dc7fea669bb943ee6462a859358ae7b62 | 9e34cdba241d39fbd8c4cf3032cadeef57b25de9 |
| IntRuoyiBackend/yudao-module-system/src/main/java/cn/iocoder/yudao/module/system/dal/mysql/gxpaudit/GxpAuditEventMapper.java | 53fee3bbb76cc449836902c8dd06cde953b3a8e2a60219150f2cc3f0d22ab690 | 9ac75b2c35bc89df5ee7076adb69330354634889 |
| IntRuoyiBackend/yudao-module-system/src/test/java/cn/iocoder/yudao/module/system/service/gxpaudit/GxpAuditScopedPageProjectionTest.java | ab3ef364fa1d0bba0dd7df39e2321dc6a462d33f21f013894abbaf0c297e48bc | 46999d39a68cf94e1e0041132ba8436ee3d2287e |
| IntRuoyiFronted/src/api/mes/pro/edhr/release.ts | 9025c8bf22e2a29bb1c8e371bb8b6826e1fab57b4a569b1ed8f290f1d9668413 | 456488e3590ecc0c4bb64da2ea0b807fb2cd4277 |
| IntRuoyiFronted/src/api/mes/pro/feedback/index.ts | a8925801509ba419894c17799e3e637d00979781b8f53ae68dcb37f0e2fa45d3 | df33932336aab075b70defdeb05c83b50b277cad |
| IntRuoyiFronted/src/api/mes/pro/processpool/teamLeader.ts | 48a5cc810b291858d711d49304841d77cc1e64551bef9a2e557d43898271e5a6 | b2cbad6e05284ec1ccbd8c696537896fcacf8195 |
| IntRuoyiFronted/src/views/mes/pro/batchrecordformlist/BatchRecordCellRulesConfirmDialog.vue | 4eddd3fea802cb9d9ba89846b12ee84a518d4f3b4fb19092fb9d9e5284ee4c60 | 30249c5257c32db84913c7f8f77fa41a25cc6892 |
| IntRuoyiFronted/src/views/mes/pro/edhr-deviation/index.vue | 3a7b8429085e59d74fde938ee3766006e75392450dc94e957177a36c0b80889e | de345262c47320c4b4ce2dae3e1878bb06a64cfa |
| IntRuoyiFronted/src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue | ed6c280626ae356e84a7d45c93892f836a4245b6a03d8945212469405e677ff7 | ef1674fe61fa7335e3f50f9bf4a9f24abccfd7bb |
| IntRuoyiFronted/src/views/mes/pro/feedback/frontline-pqc-production-process-boundary.behavior.spec.cjs | 43619c57af8398fa6489f3709da6f66434b5bbac8edd91269d875a963daea745 | a4679313a3c27a3dd8f42aeb99a95d41c47ad8be |
| IntRuoyiFronted/src/views/mes/pro/feedback/frontline-pqc-submit-error-behavior.spec.cjs | 3570fdef71a405035a729548c4395e2dd5b8f64ce3ecdfddd7a362c3f91cfb5e | ba0017b587da1360b68c5edef9e30e92d7ee3042 |
| IntRuoyiFronted/src/views/mes/pro/feedback/frontline-pqc-task-switch-employee-behavior.spec.cjs | 019269065e5f01af46961594c3bf2ec51bd3edc74f039f67976101d3014e50f3 | 6b0cc17b0f2268efc7926ce94681112c619bd701 |
| IntRuoyiFronted/src/views/mes/pro/feedback/frontline-pqc-task-switch-employee-static.spec.cjs | bea403031dea73e1d66fb4067a25aa20f6dc786145a2c440a786abeb37859b5e | acd47cff2a90812f1e7ea3553e3a9ea552dd0b21 |
| IntRuoyiFronted/src/views/mes/pro/feedback/frontlinePqcProductionProcess.ts | 44b55ef3abdee6baaeb29a261b96a00bbc8527d48d4e41ce1158b55296aec59f | b79ce94bed539d19987196f83326a304e66bb05e |
| IntRuoyiFronted/src/views/mes/pro/handoff/ActiveOrderHandoffPanel.vue | ba6b10c4943f130b5b4c8d1d3a63b5002272ff2f2e8ba586247651ff6150f33e | b19d7a48230d325be271db55e49c9ea9cd7e604f |
| IntRuoyiFronted/src/views/mes/pro/processpool/active-order-rework-source-behavior.spec.cjs | 6bb0ab36393d20e275a20c231e17c101088a6a9dddd272dba402d89a30e55086 | 2d383f792568e0fbd7fe77673e009995a5f274b8 |
| IntRuoyiFronted/src/views/mes/pro/processpool/components/ActiveOrderReworkSourcePanel.vue | 387097247c17c5b911e31d895cd18113ceaf7dacb1337fd4da84d28c1648e104 | 0cf5934b223f7fb25430bac32572eb2378e39d29 |
| IntRuoyiFronted/src/views/mes/pro/processpool/components/ActiveOrderSubmissionDetailPanel.vue | fb9aedd421ea2eb74b34c8b0fcbf0e50c54d143dbc4680e31e69d7d95266b999 | f26dda77b9e8bf7b06924447b6f8e1373e7caa8c |
| IntRuoyiFronted/src/views/mes/pro/processpool/components/activeOrderReworkSourceLocation.ts | dfe3ed3e9112b6f9c0dde2cbd26875ab838257f7c416beb22c57c6b0bb2f9c20 | 0468c2f181429e9ea9c00b0d43edc705f2f6cf26 |
| IntRuoyiFronted/src/views/system/role/index.vue | de4c70ce86eba3e0e1f16d32e928fadbc8bcc3e4623ce3c943060493f70436db | db76e25a3bbe3236726fd6722faf91d5f41e03f8 |
| IntRuoyiFronted/tests/e2e/active-order-dossier-upload-timeout.behavior.spec.cjs | 944ffbeafd3a0843c6702eaa3dbc3b7f59a7aeac419cc9865e0ee4e38ae0b337 | e896817777406631b2150f7859ae2bffef362740 |
| IntRuoyiFronted/tests/e2e/active-order-handoff-behavior.spec.cjs | 7872fc1451394982126e920c772853729ea5ac15f12e3b00538fbaa257a77cf7 | da877a5a9138f6db7816e2c54de088a64244276b |
| IntRuoyiFronted/tests/e2e/active-order-p2-backfill-timeout.behavior.spec.cjs | 1a1ac46326c9da62f42d6be8ad165a04439decb78d67431006b09d24362911de | 1d1fec8749e7c8297dadcca80c9b0e01d065b281 |
| IntRuoyiFronted/tests/e2e/batch-record-whole-form-fill-rule-static.spec.js | a95e7262835ca9911bc6d9e0a32514221b1d6cf3614127a8f0ace430313d8c9d | 03ff60e8715a0ab69aa81fe84fe3249c63c68b85 |
| IntRuoyiFronted/tests/e2e/edhr-market-release-approval-timeout.behavior.spec.cjs | d9253f373a921772488f0e92f350d0f4bf69f8ff636749936e5706da9c9e524a | aba395800b1ce83ff62c2c7e5ac05679166b4237 |
| IntRuoyiFronted/tests/e2e/frontline-own-return-parent-isolation-behavior.spec.cjs | 5c32335f91829d6c340f75b7e81d5192630f973b70fe68349d9aa771d28d410d | ba6b7ad51407de1d6de1b2a575e678a163790eb8 |
| IntRuoyiFronted/tests/e2e/frontline-pqc-submit-timeout.behavior.spec.cjs | 30458897485e23d0fbce941e4561d25e535081b8bdfd9822b59c8ad5c0bfbdc7 | 69ff9f9e74fb5d573aca8eafd9e7efd2b488a7e1 |
| docs/e2e-rules.md | 403be21a465e15e4f0d8e1e233d0876e52f56019f555c3a082626729cffd8f15 | d2a12874cd319c6954a943724316edac8e516c9f |
| docs/experience-index.md | 7a3197cf1226bf377b64de29614d6a2d034f4fa9956737a928adef60f89f544c | 39c1c23d49d22d7b5c9ee1f84bb83dd198ff6484 |
| docs/local-runtime.md | 59ab95ba081b0a1c038c8c8bd132003700971c23b704df26a9b72a95184833a7 | c62dac3ff27a043c5cb3d79ceb999d8ab96e032c |

## M2 提交

- 代码快照提交：72cfde8e0ab5d658ee56abb88624c258bec7f0c4。
- 提交前再次核对 57 个路径、工作区 SHA-256、索引 blob 与起始 HEAD，全部通过。
- 完整暂存差异新增 3096 行；2 项密码模式候选已确认为仅用于测试的虚拟传输夹具，无未核明候选。
- git diff --cached --check 与 pre-commit 端口守卫通过。
- 未执行构建、业务回归或 E2E；本任务没有修改业务源码。

## M3 收尾清理

- ready_for_closeout 已写入 task.md，技能识别状态正确。
- task-closeout-cleanup preview PASS：keep 3、delete 3、blocked 0、warnings 0。
- task-closeout-cleanup apply PASS：仅删除本任务 code-commit.txt、paths.txt、snapshot.json；指纹、blob 和提交号已归档到本日志。
- 当前为主工作区：未执行 merge 或 worktree 移除。
- 三份核心记录被本机 .git/info/exclude 的 /doc/tasks/*/ 忽略；为满足本任务记录提交要求，将仅对三份记录使用 git add -f，不修改忽略规则。
- 既有 docs/worktree-memory.md 的主干快照提交规则已经覆盖本轮经验，未创建或改写长期经验文档。

## 共享工作区剩余改动

代码提交后的状态扫描记录以下并行新增修改；保留原处，不纳入本任务收尾提交：

- doc/tasks/20261006-user-password-session-fix/task-state.json
- doc/tasks/20261006-user-password-session-fix/task.md
- docs/local-runtime.md

Git 推送前的端口守卫再次 PASS；推送进行中。

## 最终代码验证

- git push origin int_main：PASS，远端 f68e333e4 -> 72cfde8e0。
- 本地 HEAD 与 origin/int_main：72cfde8e0ab5d658ee56abb88624c258bec7f0c4；left/right = 0/0。
- 初始已有 4 个提交连同本任务代码快照共 5 个提交已推送。
- 本任务收尾提交仅包含 task.md、execution-log.md、verification-report.md；三份记录结构及 UTF-8 校验通过。
