# Execution Log

## Request
管理者委派 SA06 隔离修复，完成必要本地回归后 review_ready；管理者执行提交和融合。

## Baseline verification
- 实际 HEAD：64f07c9ea2fd10968dd2ab85f64d065dc5956cdb，起始 git status --short 空。
- 四核心文件规范化 SHA256 完全匹配交接：detail mapper 4456750079EEB6C862D8D6B4DE85004E87B2E3EAA8FC33314129861C066CB7EE；timeline mapper A61DD3D380F2C7BC1BD09B74759BDA519320F839C1804B248741CF199B185DC4；detail service D98290B80C06A04580F0AC23C94C6E930BDDD35DF99415AC2B869134EC737098；evidence service 50C8357D8EF27F5DD801779E6B62920174546F906473045F3ADE75CFDAF5CFAA。
- 已读 nearest AGENTS、backend-development、database-rules、task-closeout-rules、worktree-restrictions、branch-runtime-ports、request-command-log、powershell-encoding 和测试前置文档。项目现行 AGENTS 覆盖文档与技能默认强制 TDD；本次仍用可执行回归证明修复。
## Repair and contract verification
- Root Cause: 原 reader 按数字 ID 混合查询 system_users / employee_profile，并将当前名单名称用于签名证据；同号不同身份域时可错名，人员改名可改变历史签名展示。
- Expected: 实际人员按持久身份域读取；正式提交签名读验证过的冻结 signatureIdentity；实际员工=签名者维持原合同，登录操作者可另为 A。不增加代签能力。
- 当前/归档详情共用 reader，生产/PQC mapper 和相邻报工修订姓名筛选分别核对；更正签名使用既有已验证 revisionSignatureSnapshot.actorName，不假定正式 FIELD_CHANGE canonicalContent 一定存在 submission signatureIdentity。
- BDD: 模拟记录可读 -> Given 明确 SIMULATION_SESSION 且无正式signatureId / When 详情单元格格式化 / Then 显示模拟记录、姓名和时间，正式无ID仍未签名且无ID按钮/跳转禁用。
- 前端初次测试加载失败：隔离worktree没有typescript包。最终定向脚本采用Node24原生stripTypeScriptTypes执行真实函数，不需要项目依赖或全量TS检查；该加载失败不是业务RED。
- RED: pnpm --dir IntRuoyiFronted exec node scripts/sa06-simulation-signature-display.test.mjs -> FAIL, 模拟记录实际显示未签名。
- GREEN: 同上 -> PASS, 实际格式化/跳转函数及按钮禁用合同7项检查。
- 基线 mapper 对照：以 git show HEAD:<mapper> 提取旧SQL到任务自有baseline-mappers，未切换或修改生产源码。前两次缺夹具列system_user_id / enabled是夹具错误，不属于业务RED。
- 历史SQL探索（不记业务复现/门禁）: Maven -Dtest=MesSa06IdentityMapperTest#actualDetailsAndPqcPartiesResolveSameIdOnlyWithinPersistedDomain -Dsa06.mapperRoot=<任务baseline-mappers>（其余参数见verification-report） -> FAIL, 1 test/1 failure/0 errors/0 skipped，expected Temporary B but was System S，退出1。证据maven-baseline-behavior-red.log。
- 相邻补正失败处理：232用例轮中69个补正测试里的1例错误，是曾把其它签名显示切换为evidence.actorDisplayName而真实FIELD_CHANGE fixture该字段为空。恢复其它动作原绑定来源；补正仅在现有完整revision/review/subject/audit验证后返回冻结revisionSignature.actorName。改名负例保留，未删除断言、未放宽关联/哈希守卫。
- 后续冻结源码编译失败：actorName校验误放入void requireCorrectionAudit，revisionSignature不在作用域；移回String requirePqcCorrection末尾。该轮0个测试，不能计入通过。
- 各轮精确计数及最终源码指纹见verification-report/source-fingerprints；不累加、不把旧通过轮作为最终源码的通过证据。

GREEN: 最终完整Maven选择命令（见verification-report） -> PASS，16类232例，0失败/0错误/0跳过，退出0；前端7项合同退出0。最终状态review_ready，未提交/融合，待管理者复审。

## 管理者最终收口指令
不将先RED作为默认门禁，不再扩展历史SQL夹具或历史数据工作；已发生探索失败及原因仅作记录，不记业务复现。最终真实mapper双域碰号、冻结身份与相邻回归、前端最小展示合同及精确指纹/每类计数已齐全。状态review_ready；停止构建测试和源码修改，保留target供管理者独立相邻验证。

## 管理者独立复审
2026-10-04：完整代码及身份守卫复审PASS，前端7合同PASS，主干真实P1输入组合Java17回归16类247例全通过。首轮复制保留旧时间导致增量编译缓存构造器不匹配，核实后仅更新临时输入时间并重编译；第二轮BUILD SUCCESS，未放宽断言。两项P1临时源码已按SHA256恢复，业务提交不含并行任务资产。详细实际证据保留在主仓doc/tasks/20261003-edhr-thread-management/sa06-repair-review.md及manager-sa06-integrated-results.json。授权范围为本地提交融合，不含远程或环境操作。
