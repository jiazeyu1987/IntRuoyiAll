# 共享工艺变更验证进度（2026-11-18 01:16，北京时间）

目标尚未完成；不标记 complete。源代码在 D:/ProjectPackage/IntRuoyi/IntRuoyiAll，验证 worktree 在 C:/Users/BJB110/.codex/worktrees/edhr-shared-validation/IntRuoyiAll。只运行任务自己的 8506/48506 服务，勿动主服务。

## 本轮已完成

- 工艺修改日志统一显示关联路线编码、名称、原版本、新版本及当前版本状态。后端 changeRoutes 批量读取冻结版本，校验批次身份，不读当前路线主数据。4 条新测试与 15 条整批事务测试通过。
- 已打包、部署并通过真实 UI 查看“验证共享参数方案同时升级两条路线”：A/B 两条路线显示 V2→V3，目前均“已替代”。证据 workspace/shared-craft-log-snapshot.txt。
- 工序统一替换候选补齐全部实际使用车间。新公共查询 MesProProcessWorkshopUsage 供解析复用、替换候选共同使用，去重并隔离租户及删除的路线/工作站/车间。解析测试6条、共享变更服务测试12条、前端测试7条通过；vue-tsc通过；菜单迁移检查退出0。
- 最新 backend exec jar 已部署，PID 66112，exec session 18182，启动日志确认 Started。frontend 最新 SharedChangeDialog.vue 已同步。playwright session edhr-shared-change。
- 真实 UI 工序替换候选显示“EDHR01-A7K2-A-GX-0002 / 粗洗工序共享验证修订 / EDHR01项目A7K2A / 车间1、车间2”。随后取消两层表单，无写入。证据 workspace/shared-workshop-current.txt。浏览器当前工序设置页。
- 设备 B09393→B09353 的真实 UI 预览成功：6条活跃引用路线，其中4条非本任务路线、2条本任务路线；另有多个未完成候选版本阻止提交。已取消，不得为通过测试修改那些无关路线。证据 workspace/shared-device-impact.txt。

## 当前数据

- 本任务路线A 2121453182031056963 / EDHR01-A7K2-A-GYLC-0001，路线B 2121453182031056964 / EDHR01-A7K2-B-GYLC-0001，当前均V5。
- 共享工序 EDHR01-A7K2-A-GX-0002，id 2121333283992494114，粗洗工序共享验证修订。
- 共享工艺 GY-0062，id 2121333283992494110，粗洗工序 / 参数组1。
- 两路线当前设备980009，对应台账B09393；功率20-32%，默认26；步骤1“共享步骤验证：检查清洗介质与设备状态”。本轮未修改这些数据。

## 下一步必须处理/验证

1. 历史参数页面的工序名称读取已在01:32修复验证，见下方续记。设备名称仍用teamDeviceMapper.selectBatchIds读取当前资料，必须继续补齐冻结写入和读取链路；不能修改已发布路线JSON/hash来伪装原始快照。
2. sharedSourceSnapshots 目前只有 MesProSharedChangeService.transform 写入和 MesProRouteServiceImpl继承，没有实际读取消费者。需核实设备/步骤版本数据使用。
3. 设备 UPDATE/REPLACE 完整成功路径尚未真实验收。台账与执行设备为两套身份，替换要求目标设备编码匹配唯一启用执行身份。B09353目前可成功预览。不要直接用B09393做整批提交，涉及无关路线和未完成版本。需要选择不影响其他路线的设备并通过前端配置本任务路线。
4. 设备执行身份新增UI旧代码在TeamLeaderWorkbenchPage班组配置中心，但实际生产组长包装页 show-production-module-tabs=true，该区被隐藏，真实菜单没有直接维护入口。不能用API/DB造数据绕过；如确需补入口，应在本功能范围内合理实现并验证，不修改无关配置。
5. 工序/工艺替换完整成功路径、审批模式完整路径，以及整体需求逐项验收尚未完成。当前租户DIRECT；不要仅为测试更改全局审批策略。

## 构建与验证

- Maven定向输出：-Dmes.main-output-directory=target-reuse-upgrade/classes，-Dmes.test-output-directory=target-reuse-upgrade/test-classes；测试加-Dfastjson.version=2.0.57。所有-D参数PowerShell完整单引号。
- 正确可执行包 yudao-server/target/yudao-server-exec.jar（约525MB），不是yudao-server.jar。
- 本轮日志：shared-log-regression.log、shared-workshop-regression.log、shared-workshop-typecheck.log。Maven -q成功日志为空；测试结果查target/surefire-reports。
- 原仓库有大量并行改动，仅操作任务文件，不全量覆盖、不提交其他变更。


## 续记：2026-11-18 01:32

- 本轮确实修改并验证了工序名称冻结。MesProRouteFlowConfigServiceImpl.getRouteProcessDeviceParameterConfig 读取 processConfig.processName；缺失即明确快照不完整，不改读当前工序。新测试 MesProRouteParameterIdentitySnapshotTest 5条，既有 FlowConfigServiceImplTest 48条，全部通过。
- 真实UI前后对照：修复前打开路线A的V2，观察页面自然GET响应，其processName为当前“粗洗工序共享验证修订”（断言失败，已真实复现）。部署修复后同一真实路径返回“粗洗工序”，清洗功率上限30、默认25；再打开V5确认新名称、上限32、默认26。均通过Playwright点击页面触发请求、只读检查响应，无API业务操作。
- 最新运行后端 exec session70115，PID44756，48506，Started确认；前端不变。浏览器仍session edhr-shared-change，当前路线A V5 设备参数字段。V5“查看”导航只带tab=flow，不带routeVersionId；点击节点后还需点击“查看设备参数字段明细”才发请求。V2页面已选择字段时reload会自动发参数请求。
- 测试日志 workspace/shared-parameter-identity-regression.log（-q成功为空），界面证据 shared-history-identity-after.txt。本轮未写业务数据。

### 设备历史名称的已确认调用链（待实现，不要反复从头搜索）

- 路线参数历史展示：MesProRouteFlowConfigServiceImpl约1006行，按版本读参数，却用teamDeviceMapper.selectBatchIds读设备code/name/status。
- 一线执行：service/pro/frontline/MesFrontlineRuntimeConfigServiceImpl.toDeviceOptions约446行，同样读当前teamDeviceMapper；resolveParameterRuntimeSnapshot约184行从MesProcessPoolActiveOrderProcessSnapshotDO拿冻结设备组及参数，未冻结设备code/name。
- 活跃订单冻结写入：MesTeamLeaderActiveOrderServiceImpl约4635行，productionConfigSnapshotJson只写deviceSelectionGroups、parameterRules等，没有设备名称。
- 提交：MesProFrontlineFeedbackSubmitServiceImpl.validateDeviceSelections约610行用allowedDevices的code/name覆盖请求，继而保存payload。已完成记录自带提交时名称，不能改写其payload；未提交的旧版本执行仍会跟着当前名称变化。
- 设备状态属于当前可用性校验，不应因为展示历史名称而取消禁用/报修检查。
- sharedSourceSnapshots目前无读取方，不足以证明历史资料冻结。旧路线快照没有设备名称，仅有deviceIds，不能伪造恢复不存在的历史证据。
- 新方案须同时处理：新候选保存/审批前冻结、共享DEVICE UPDATE的拟生效名称、发布投影、历史版本读取、已有活跃订单读取，以及旧数据明确迁移/基线记录。用户已授权必要迁移及任务服务重启。勿添加无声明的当前资料fallback，勿重写旧路线hash或原批记录。
- 已检查的集成点：MesProRouteCandidateConfigServiceImpl.validateProductionDevicesExist约391行已有批量加载执行设备；MesProCraftService.saveGroupedCandidate约384行先saveConfigSnapshots再加craftChangeId；MesProSharedChangeService.transform约300行有before/after与源执行设备集合；MesProRouteVersionWorkflowServiceImpl.createCandidate约100行从active创建候选；MesProRouteVersionLifecycleServiceImpl.publishCandidate约63行发布投影后更新hash。后续可沿这些节点设计设备版本资料的持久化，而不是仅在UI替换名字。

## 续记：2026-11-18 01:53（设备版本身份）

- 新增 MesProRouteDeviceIdentitySnapshotService 和 SQL 20261118_mes_route_device_identity_snapshot.sql。独立表按 tenant/version 保存设备 id/code/name、SHA256、来源和采集人时间；不改原路线 hash 或订单/批记录。
- 已接入路线创建、复制最终快照、候选创建/保存、Word 生成/更新；SHARED 变更从已保存 proposal 采集拟生效设备名称；审批前及发布前校验快照，非 DRAFT 不可重采。重复保存不会因 affected rows=0 重复插入。
- 历史参数页读保存的设备名称，当前设备状态仍实时读取，已删除设备标识 DELETED；一线有活跃订单时按 snapshot.routeVersionId 读冻结名称，仍过滤当前报修/禁用设备。无活跃订单的当前配置维持当前资料语义。
- 测试最新共189项通过：设备独立存储6、参数身份7、FlowConfig48、Candidate19、Lifecycle10、Workflow23、一线15、CraftGroup15、RouteService31、ProjectCreation3、CopiedParameter2、WordRegistration7、CandidateGovernance2、CodeRule1。第一次测试由于新增依赖未注入失败，修正后两批通过。日志 shared-device-identity-regression.log 和 shared-device-identity-creation-regression.log，实际 XML 在 target/surefire-reports。
- 已在当前配置数据库执行加法迁移，MySQL8.0.39，建立478个 LEGACY_BASELINE；重复运行逐行完全一致，480条原版本 JSON/hash 完全未改。1个原版本id11快照无效不纳入；1个版本2116486795650920448的13个唯一设备缺当前资料（20次引用），保存明确缺失，读取会报错。详见 workspace/shared-device-identity-migration.json；不可声称全部历史资料完整。
- 最新后端包构建成功并部署任务48506，PID24140，exec session53404，Started确认。前端未改，8506。Playwright session edhr-shared-change，当前路线A V2。
- 真实UI已验证V5与V2读取成功，设备B09393/超声波清洗机显示，V2旧工序名称与功率上限30/默认25、V5新工序名称与上限32/默认26保持。证据 shared-device-identity-ui-active.txt、shared-device-identity-ui-history.txt。未通过UI改设备主数据，本轮不能声称真实设备改名/替换发布成功；该行为目前由新测试覆盖。
- 剩余：安全任务设备的UPDATE/REPLACE成功发布UI、工序/工艺替换成功UI、审批模式完整路径、历史操作步骤读取与全需求审计。勿使用B09393整批更改，包含4条非任务路线并有开放候选。前述运行端口及主服务限制继续适用。未提交推送，目标仍未完成。

## 续记：设备、工艺替换实测与工序工作站修复（2026-11-18）

- 前一轮真实 UI 将仅本任务两条路线先绑定设备 A03190/41（V6），再通过设备台账统一替换为 C01017/980012（V7）；变更243 APPLIED。历史V6仍显示A03190，新V7显示C01017，5条参数设备ID同步。证据 shared-device-replace-proof.json、shared-device-replace-v7.txt、shared-device-replace-v6.txt、shared-device-log-detail.txt。
- 真实 UI 创建 GY-0063（GX0002，替换验证温度20-30℃/25），将两路线 GY-0062统一替换，发布V8；证据 shared-craft-impact.txt、shared-craft-replace-v8.txt。
- 真实 UI 创建 GY-0064（GX0001，目标工序温度30-40℃/35），将两路线工序GX0002统一替换为GX0001，并明确选择GY-0064，已发布V9。但是发布后工序列表刷新报错：工作站仍绑定GX0002。此路径尚不算通过。
- 当前修复：MesProRouteWorkstationService.forPublishedProcess 在发布事务中为发生工序变化的工作站建立独立副本，保留车间/产线/仓位/产能与设备、人力、工装资源；不改旧工作站。发布投影同步更新新版本routeProcessWorkstationId。工序不变则沿用原站。
- 当前定向48项测试全部通过（workstation7、publishProjection13+13、craftGroupTransaction15），日志 shared-workstation-replacement-tests.log。构建正在进行，尚未部署此修复；现运行48506 PID24140仍是旧包。
- 前端版本列表旧行竞态已修复（index.vue请求序列+加载先清空）。本轮再次发现返回已打开路线详情会显示缓存V8，刷新后实际V9。RouteEditPage.vue离开页面时清空请求key，使重进读取最新版本；已同步验证前端，类型检查正在运行。
- 后续先部署工作站修复，通过真实UI给两条任务路线创建并发布新版本修复绑定，再完整验证工序替换及历史工作站保留。禁止SQL/API写业务数据绕过UI，禁止修改任何非任务路线。
- 整体仍剩设备UPDATE、审批模式、解析差异参数新建实际路径、历史步骤读取与全需求审计。目标未完成，勿标记complete。

### 工作站修复已部署并完成真实流程验证

- 工作站修复包已构建成功并部署48506，PID68012，exec session78348；Started确认。前端RouteEditPage刷新修复已同步，8GB vue-tsc退出0，日志shared-route-refresh-typecheck.log。
- 真实UI分别给A/B保存并发布V10，发布投影自动生成GX0001独立工作站。然后从工序设置将GX0001统一替换为GX0002，并选择GY-0063，确认影响仅两条任务路线，成功统一发布V11，工序列表刷新无系统异常。
- 只读DB核验A/B的V8、V10、V11共6版本：每版工序ID与工作站工序ID一致，三个版本工作站各自独立、原车间相同；当前投影工序/工作站一致。证据shared-workstation-publish-proof.json、shared-workstation-replace-success.txt。
- 无浏览器刷新返回已打开路线A自动显示V11，设备参数自然GET成功返回GY-0063，验证详情缓存修复。shared-workstation-v11-ui.txt。
- 真实UI从设备台账将C01017名称改为“撤压机共享验证修订”，影响弹窗明确A/B两条V11→V12及各自车间，经二次确认成功发布；理由“设备资料统一修订验证：新版本使用新名称，历史版本保留原名称”。
- 只读DB验证两路线V11/V12共4份设备身份快照：旧版仍“撤压机”，新版“撤压机共享验证修订”，全部SHA正确，V12 ACTIVE。证据shared-device-update-proof.json。真实UI路线A V12已见新名称，历史V11界面正在核验。
- UI脚本个别等待失败为定位器错误（版本状态叫“已生效 ACTIVE”，关闭弹窗用Escape，版本标签用title，设备详情需展开），未重复提交已成功业务操作。启动时一次修改按钮请求未完成，服务确认Started后重新操作成功。
- 仍待历史步骤完整读取审计、审批启用完整路径、解析不同参数创建新工艺实际路径和整体逐项验收。无提交推送，目标继续active。

### 解析不同参数新建工艺：真实 UI 已验收

- 本轮在DCC项目代码→新建项目及产品正常页面创建专用项目 EDHR-SHARED-PARAM-1118 / 共享解析参数差异验证项目，产品 EDHR-SHARED-PARAM-1118-P；负责人当前管理员，现有启用模板1，明确测试用途。直接创建无需审批，未改已有项目。
- 简易解析导入既有测试Word IntRuoyiBackend/yudao-module-bpm/src/test/resources/formcenter/pressure-pump-cleaning-production-record.docx，通过页面删除14个无关解析步骤，仅保留粗洗；工序名称改为当前共享工序名称，车间1。
- 复用确认明确显示车间1、车间2及GX0002，选复用工序后提示“参数不同，新建工艺”。预览：工序使用已有，5参数工艺新建。页面确认创建后生成路线2121453182031056965及候选V2，新工艺GY-0065 id2121333283992494113，未创建重复工序。
- 注意简易解析创建的是初始V1和配置候选V2；首次只读核验误查active V1导致craftId缺失，追查确认配置完整在V2，并非绑定丢失。随后通过真实路线列表→版本→提交发布，V2 ACTIVE，路线保持禁用。
- 最终只读核验 shared-parser-new-craft-proof.json：新工艺功率上限30，原GY0062仍32；原A/B路线仍V12。界面证据shared-parser-different-parameters.txt、shared-parser-difference-craft-preview.txt、shared-parser-new-craft-active.txt。
- 设备UPDATE历史UI亦完成：shared-device-update-v12.txt显示新名，shared-device-update-v11.txt仍原名。
- 当前浏览器简易解析新路线的版本弹窗，session edhr-shared-change。审批BPM全路径、历史步骤可用性审计、整体验收仍待完成。当前审批策略是租户级维度，无按路线条件字段，不能为测试直接更改全租户策略。

### 审批验证现状

- 只读检查当前数据库所有路线版本发布策略，保存workspace/shared-approval-policy-inventory.json。当前有效租户“芋道源码”“测试1”“测试2”的启用发布策略均DIRECT，BPM_REQUIRED策略均DISABLED；不能声称真实审批路径已验证。
- BusinessApprovalPolicyDO只有tenant/domain/system/object/action/state等维度，没有routeId条件作用域。后续需在隔离范围验证BPM_REQUIRED，不得临时改全租户策略影响并行工作。
- 本轮实际完成：工作站替换代码修复及48测试、后端包构建/任务服务重启、详情缓存修复及vue-tsc、工序统一替换V11、设备改名V12及历史UI/DB核验、简易解析新工艺GY0065和路线V2正式发布。目标尚未全部完成。

### 续记：审批联动作废修复与跨服务验证（2026-11-18 03:15）

- 新增 MesProSharedApprovalTransactionTest，将真实审批执行器、整批协调服务、路线生命周期服务连接在 H2 事务中。7 项覆盖先批准一条再批准另一条、重复回调、驳回/撤回整批取消、迟到批准、第二条投影失败、审计失败、不同审批人联动作废及取消失败回滚。Mapper、投影和 BPM 边界是测试替身，不宣称为真实浏览器/Flowable E2E。
- 实际发现并修复：整批取消后执行器仍返回 DRAFT/REJECTED，导致业务结果与 CANCELLED 版本不一致。现保留审批事件的驳回/撤回结果，业务结果记录实际 CANCELLED。
- 沿调用链进一步发现：MesProCraftService.abort 原调用 BpmProcessInstanceApi.cancelProcessInstance，底层只允许发起人撤回，审批人与发起人不同会使整批驳回失败。新增内部 cancelBusinessApprovalProcess，校验审批请求的租户/上下文租户/实例/执行器/对象/待审批状态，再校验运行实例租户、业务键、流程定义及非子流程；联动作废记录真实审批人，不冒用发起人，原主动撤回接口保持原语义。
- 新增 BpmBusinessApprovalCancellationTest 12 项；连同 BPM 原事件与发起补偿、MES 审批/整批/生命周期，共58项通过，失败与错误均0。结果 workspace/shared-approval-cancellation-test-results.json；日志 shared-approval-cancellation-regression.log。早先状态修复回归实际37项（口头进度曾误写36），本轮58项已覆盖。
- 本轮涉及BPM四个主源码文件：api/task/BpmProcessInstanceApi.java、BpmProcessInstanceApiImpl.java、service/task/BpmProcessInstanceService.java、BpmProcessInstanceServiceImpl.java；MES两文件 MesProCraftService.java、MesProRouteVersionBusinessApprovalEffectExecutor.java。只对任务相关位置修改，git diff --check通过。
- 打包成功 workspace/shared-approval-package.log。仅重启归属已核对的48506任务服务；新jar SHA256 80EF7C5FA53E2704E1BEB4048DD4A96EB109AEE5E8181DF2C6C20B94217D787D，新进程PID83748、exec session1226。当前刚启动，仍需确认Started和页面读取；不要把旧PID68012当当前服务。
- 真实UI从审批中心→流程管理→业务审批策略确认：路线版本草稿/待发布策略均“关闭审批”，证据 shared-approval-policy-ui.txt。当前页面停在业务审批策略，未修改任何策略。
- 已向用户发出待答复的异步确认：真实审批验收需启用租户级策略，选择“允许创建本任务专用验证租户”或“允许临时开启芋道源码路线审批、验收后恢复关闭”。依据是现有开关不能限定本任务路线，项目任务归属规则禁止影响其他任务。未答复不代表同意，不要擅自启用全租户策略，也不要重复提问。
- 工序操作步骤消费审计：ProProcessForm/ProProcessContentList是当前步骤的页面消费者；SharedChangeService采集步骤到变更快照并在确认发布事务中更新主数据；未发现一线执行或批记录读取该主数据步骤的消费链。sharedSourceSnapshots尚无历史步骤展示消费者，不能据此声称历史步骤UI可回放；不得为了补这项审计随意扩展新界面。
- 原目标仍 active：继续完成审批真实路径的隔离验收及逐项完整性审计。不要把服务层事务测试等同全部业务验收，不提交推送。

- 03:16:39 已确认新服务 Started，PID83748/48506/session1226。真实浏览器刷新业务审批策略页，自然GET返回HTTP200、业务code0，路线两条策略仍关闭；证据 shared-approval-after-restart.txt。本轮未改业务策略或业务数据。异步验证范围问题尚未收到答复，保持待答，不自动视为同意。

### 续记：解析明确不复用与路线参数方案隔离（2026-11-18）

- 真实页面创建专用项目 EDHR-SHARED-INDEPENDENT-1118，导入同一测试 Word，保留粗洗并设置车间1。选择已有 GX0002 时唯一相同参数工艺 GY-0065 被识别，预览显示使用已有工序、已有工艺；返回后明确选择“新建独立工序及工艺”，确认创建独立 GX-0001（2121333283992494115）与 GY-0066（2121333283992494114）。新路线2121453182031056966通过版本页面提交V2，现ACTIVE且路线禁用。证据 shared-parser-independent-identical-match.txt、shared-parser-independent-reuse-preview.txt、shared-parser-independent-new-preview.txt、shared-parser-independent-published.txt、shared-parser-independent-active.txt。
- 只读脚本 verify-shared-parser-independent.py 核验并生成 shared-parser-independent-proof.json：GY0065/GY0066的5项参数完全相同，但工序和工艺身份独立；两个工序同名、不同编码。四条任务路线仍分别V12/V12/V2/V2。
- 本轮扩展同一只读核验，确认A/B/C三个路线节点均使用GX0002：A/B绑定GY0063及1项温度参数；C绑定GY0065及5项清洗参数，各自带非空craftFingerprint，两个方案指纹不同。D绑定新工序及GY0066。不是按工序名称推断参数方案。
- Playwright通过路线列表→版本→查看→设备参数详情完成已生效A V12与C V2页面对照，显示GY0063/1参数和GY0065/5参数，证据 shared-route-specific-comparison-ui.txt（result PASS）。之前定位器错误来自只读详情合并文本及小写p003，不属于业务失败；最终按实际页面文本重新核验成功。
- 检查中首次点“编辑”自动生成A的V13草稿2121453182031057012，未保存即通过正常“返回”离开；只读核验确认已撤销/删除，不留下开放候选，不影响A V12。当前浏览器停在A V12只读设备参数详情。未修改工艺参数或审批策略。
- 审查当前实现：RouteFlowGraphDesigner.vue使用接口返回的craftBindings展示编号；MesTeamLeaderActiveOrderServiceImpl.resolveProductionProcessConfigSnapshot按订单routeVersionId读取版本参数，创建冻结参数/设备/生产配置快照；MesFrontlineRuntimeConfigServiceImpl.resolveParameterRuntimeSnapshot按活跃订单工序读取并校验冻结内容，未从当前工艺库重新选方案。发布投影写路线主数据/配置，不写已执行订单参数或报工payload。历史步骤仍仅有变更快照，无独立历史步骤浏览入口，不能宣称有该入口。
- 已检查现有实际JUnit XML及相关断言，不重复运行未变源码：解析匹配6、解析发布35、多工艺绑定11、一线配置15、共享影响12、整批事务15均失败/错误0；其中5000路线影响1000项是服务测试，不是1000条真实UI压测。
- 原目标仍未全部完成：真实BPM审批开启后的整批通过、拒绝/撤回需要此前已发出的验证范围答复。所有当前有效租户的路线策略都为DIRECT；当前策略无法只限定任务路线，未擅自修改全租户开关。继续按原范围审计，不能将服务测试等同真实BPM验收。未提交、未推送、未重启服务。

### 阻塞核验：等待真实审批验证范围授权

- 上一目标轮为有效进展：完成跨路线参数方案的UI/DB对照，并核验未保存草稿已撤销。本轮重新刷新真实审批策略页面，草稿和待发布两条路线发布策略仍为“关闭审批”，证据shared-approval-blocker-recheck.txt（BLOCKER_CONFIRMED）。第一次用内部执行器编码定位页面文本超时，改用实际中文名称后验证成功，不将定位器超时当作服务停止。
- 当前BusinessApprovalPolicyDO仍只有tenant/dataDomain/systemCode/objectType/actionCode/objectState等策略范围，没有routeId条件；启用将影响同租户其他任务。此前授权问题未收到答复，不能将自动目标续做当作具体授权。
- 同一阻塞从审批修复轮起，连续经历解析独立新建、跨路线参数隔离和本轮核验，已超过三轮。可独立进行的解析、共享升级、冻结读取及故障事务验证已推进；剩余真实审批通过/驳回/撤回需明确验证范围。没有仍在运行、可等待完成的审批验收进程。目标转为blocked，不标记complete，不继续重复轮询。
- 恢复条件：用户选择此前“专用验证租户”或“临时开启现有租户路线审批并恢复”之一，或提供其他明确隔离且已授权的验收环境。恢复后继续原完整目标，不重新解释为仅服务测试通过。

### 用户明确按关闭审批流程验收：已完成

- 用户最新指示“如果关闭就按照关闭的流程验证”，取代上文等待开启审批范围的前提。不改审批开关，不再要求用户选择测试租户。
- 本轮只读核验8次实际UI批量变更的16个关联请求全部DIRECT_EXECUTED，无BPM实例，变更APPLIED；当前两条发布策略仍DIRECT。证据shared-direct-publish-proof.json，脚本verify-shared-direct-publish.py。
- 恢复已停止的任务服务，核对原预约槽91及端口空闲、jar哈希一致；后端PID35924/session11485，前端PID74012/session68597。真实登录后从工艺库打开修改记录，确认设备变更两条路线V11→V12均已发布，证据shared-direct-final-log-ui.txt（PASS）。未重复提交业务变更。
- 本轮重验解析独立绑定与四条路线版本状态仍通过。汇总逐项证据见shared-change-acceptance.md。按用户最新指定的关闭审批流程验收完成；启用审批已有服务测试，但不宣称真实BPM验收。未提交推送。

### 后续需求：独立批量变更审批已实现并验证（2026-11-18）

- 用户追加要求“是否可以将这个审批做成一类审批，可以开启和关闭”“帮我实现这个功能并验证”。已增加 MES_ROUTE_BATCH_CHANGE_PUBLISH / ROUTE_BATCH_CHANGE，一次变更一张审批单，所有候选同一实例；原单路线审批策略保持独立。此次用户提供签名验证信息后，已完成真实开启/关闭及审核操作，不再沿用上文旧目标的开启阻塞。
- 五次真实页面变更 252—256，对应审批请求 300—304，分别覆盖 DIRECT 发布、撤回、关闭开关后批准在途审批、设备驳回、工序批准。每次仅一张审批单，11 个候选版本无单路线请求。最终只读核验 PASS：batch-approval-final-proof.json。
- 真实发现并修复影响预览内裸 template 导致内容隐藏、嵌入 BPM 详情传入 BUSINESS_APPROVAL 请求标识无法定位变更的问题。最终详情页 254 与工序变更 256 均已用 Playwright 核验。
- 本轮 74 项测试通过，包括 1000 候选只提交一次、整批事务失败回滚；前端类型检查、后端打包通过。迁移三文件重复执行通过并保留现有审核人配置。完整证据、边界与运行状态见 batch-approval-acceptance.md。
- 最终全仓菜单迁移检查被并行 ERP 物料页面 erp:data-material:operate 缺少迁移登记阻塞；本任务条目未报错，不修改其他任务，不宣称全仓检查通过。日志 batch-approval-menu-check-final.log。
- 审批开关最终关闭，默认审核人角色恢复，无遗留本任务待审任务。A/B/C/D 当前 V18/V17/V3/V2，参数方案绑定保持独立。后端 PID82140/session32523/48506；jar SHA256 C5FDC84A347BDA1C5F4C0C6A8D65819DBE4A73F445367A183E8E72C557124970；前端8506。未发布生产、未提交推送。
