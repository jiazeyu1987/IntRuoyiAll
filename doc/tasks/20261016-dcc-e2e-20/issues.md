# E2E-20 问题历史与当前结论

## 2026-10-19 R2 当前结论及替代关系

原27项业务PASS，Root 05:36接受并在10:20/10:40保留；工程Git/清理未收尾。本轮不改生产代码、不重跑已通过E2E。下列问题是历史观察，不能据旧“等待/尚未执行”再次重放原流程。

| 历史问题 | 当前有效结论/替代证据 |
| --- | --- |
| P01类别UPLOAD前置及误导文案 | 本例精确类别前置真实页面补齐及同文件预览已通过；错误文案生产修复由Root/19负责，不扩大本例验收 |
| P03自定义换版审批基线 | Root后续custom上下文修复记录见协调verification-report.md的2026-10-18 23:07检查点，21/23原流程复验；本例P03原换版动作未单独复跑，不据别例宣称该动作独立PASS，也不作为已接受27参数的阻塞 |
| P04延迟摘要不一致 | 原4242稳定保存/刷新/确认均DELAYED，最终A2待生效及签名947真实成立；旧竞态候选未专门证实，不能称生产竞态已修复 |
| P05检出/租户500 | Root归因后端退出；后续恢复且业务补齐，旧环境阻塞解除；不把此认定为原生崩溃根因已修复 |
| P06待生效SQL及后续检入链 | Root实际SQL RED expected1/actual0、后续同类两测试GREEN及检入链112项exit0；原A2真实检出/检入生成WORKING4251，原八项拒绝通过，替代“待Root执行RED/缺WORKING” |

P06测试依据位于协调目录e2e1624-red-and-history-candidate-DccWorkingSubmissionConditionTest.xml、e2e20-complete-binding-green-DccWorkingSubmissionConditionTest.xml、e2e20-complete-checkin-regression-green-process.json及e2e20-complete-checkin-green-summary.json。业务依据为本目录parameter-results.json及artifacts/acceptance-resume/2026-10-18T20-20-07-652Z/original-27-permission-parameters-final.json。Root补验包括后续source-preview/attachment守卫失败及修复，不把首次SQL窄修等同整条检入已通过。

## 历史现场（原文保留，按上述证据替代）

## E20-P01 上传被误报为项目OWNER不足

- Given：本例Manager（910350），正式项目283，类别908767、类型160；审批路线页面已就绪，共4节点。类别权限页实际读回0条规则。
- When：真实上传页选择同项目/文件夹/类别并上传DCC241016C20-A.pdf。
- RED：自然POST `/admin-api/dcc/controlled-files/upload-preview` HTTP400；DOM提示`Current user is not the DCC project owner`，未产生可用预览，未提交受控文件。
- 直接根因：`DccControlledFileUploadServiceImpl.validatePreviewCategory` 检查类别UPLOAD，缺失时复用了DCC_PROJECT_ACCESS_DENIED。Root已用19的实际栈证实同分支，并将该生产文件/UploadApiTest唯一写者分配给19；20未改生产代码。
- 本例准备修正：仅通过正式类别权限页，为本例类别908767增加Manager USER/UPLOAD规则。关闭后重开读回1条；自然PUT及随后GET均200。项目OWNER和Reader/Denied阅读规则未改。
- 同触发流程准备GREEN：同项目283、同类别908767、同一DCC241016C20-A.pdf重新通过真实选择文件入口上传；自然POST upload-preview由400变200，页面预览出现、文件名正确、预览错误为空、路线4节点就绪。证据E20-P01-same-file-retest.json。这里证明本例精确UPLOAD前置补齐，不冒充Root负责的错误文案生产修复已验收。
- Root安全更新窗口：未提交该源件形成正式申请，随后通过正式文件行“移除”清理；DOM提示“已清理本次上传临时文件”，文件行/预览消失，session-cleanup POST200，runtime-pause-cleanup.json记录。正式fileId/申请/BPM/task均无。
- 证据目录：artifacts/acceptance-resume/2026-10-18T03-06-47-233Z/；E20-P01-owner-rejection.png、upload-preview-not-ready.png、E20-P01-category-rules-before.txt、E20-P01-category-rules-readback.txt、E20-P01-facts.json及Manager对应trace。

本轮其它已遇到的脚本问题为：登录页自动跳转、控件加载遮罩、下拉覆盖确认按钮、文件类型确认框、源件文件选择器定位。均在本例脚本中按真实DOM修正；没有强制点击、注入状态或修改业务守卫。

## E20-P03 换版申请无法读取自有审批基线

- Given：A/1已ACTIVE，同链工作稿2054545668044084242由真实检入产生，原申请使用APPLICATION_CUSTOM审批人。
- When：Manager在“准备本次申请”中选择换版变更。
- RED：可见“所选工作稿原审批矩阵基线未记录，请核对正式保存来源”，申请属性及生效方式控件不再显示，未提交换版。
- 直接代码触发点：DetailApplicationPanel.vue的selectIteration读取换版属性后，未进入APPLICATION_CUSTOM分支，进入draftApprovalMatrix为空且无新基线准备资格的拒绝分支。为什么返回属性没有可用基线尚未证实，不能把推测写成后台根因。生产代码未修改。
- BDD：Given自有定制审批基线及已检入草稿，When选择换版申请，Then正式页面必须读回明确有效的审批基线或准确解释禁止原因，不得丢失可用基线。

## E20-P04 延迟草稿与提交确认摘要不一致（待恢复后复核）

- Given：同一工作稿4242改选局部变更，选择延迟生效/无需培训并通过真实页面保存，出现“本次申请草稿已保存”。
- When：填写变更说明后点击提交正式变更。
- RED观察：确认框却显示“生效方式：文控审核后立即生效”；此时会话超时，确认框未点确定，没有正式revision提交请求。尚不能判定是选择重载、保存后前端状态变更还是运行态契约差异。
- 业务处置：保留草稿，不以立即生效提交来凑待生效数据。Root更新窗口内关闭浏览器，恢复后先重新读取4242申请属性、核对单选状态与确认摘要，不重复提交。
- 证据：artifacts/acceptance-resume/2026-10-18T07-45-23-331Z/ui-actions.jsonl步骤62—65，pause-checkpoint.json记录可见摘要及自然写请求。
- BDD：Given明确保存延迟生效申请，When提交正式变更，Then控件、保存后回显、确认摘要与冻结申请生效方式一致。

- 新包复核（2026-10-18，264efbd4，run13-18-44）：原4242初次正式回显为AFTER_STANDARDIZATION_SIGNATURE；等待申请面板及局部变更提交按钮真正就绪后选择DELAYED，DOM单选、revision投影、确认摘要均为延迟生效。取消确认后保存同一草稿，刷新重新准备申请，DELAYED仍被选中。未提交错模式、未另造申请。
- 结论修正：当前稳定加载后的保存/投影/确认链路没有复现模式不一致，不能把旧观察认定为已证实生产缺陷。旧脚本仅等自定义审批控件出现即操作；displaySavedCustom在异步route-readiness返回后还会回填effectiveMode/needTraining/部门，而控件先于这次回填显示。这是旧观察的具体竞态候选，尚未专门复现，不授权生产改动。脚本后续必须等待正式提交按钮可用后再改字段，并逐次断言checked值。
- 新证据：E20-P04-saved-draft-readback.json、E20-P04-before-local-delayed.json、E20-P04-local-delayed.json、E20-P04-delayed-confirmation.json/png、E20-P04-after-stable-delayed-save.json、E20-P04-persisted-delayed-reload.json，均在本例artifacts/acceptance-resume/2026-10-18T13-18-44-749Z/。
- Root源码线索逐项答复：在原草稿仍保存IMMEDIATE时，稳定选择DELAYED后直接进入submitRevision确认，确认摘要仍DELAYED，取消后自然请求仅route-preview，无正式submit POST。因此此轮没有复现preparePanelMatrix/refreshPanelMatrix覆盖；该函数本身未调用displaySavedCustom。displaySavedCustom的两次回填发生在open/selectIteration的异步加载流程。前者不能直接当作本例已证根因。
- 实际组件RED设计（未执行，不能标RED通过）：挂载DetailApplicationPanel.vue的APPLICATION_CUSTOM原4242等价fixture，让selectIntent/selectIteration触发的route-readiness处于待完成状态；在控件已出现但selectionLoading仍true时点击DELAYED，随后释放真实组件依赖的readiness Promise，断言effectiveMode单选和applicationFacts保持DELAYED。当前源码第二次回填saved.effectiveMode是候选失败点。若组件正忙时禁止编辑也符合防覆盖要求，测试应验证禁用而非绕过遮罩。此设计不扩DTO/后台，不进行生产修改；稳定操作实际结果不构成此竞态RED。
- 原对象继续：4242明确保存DELAYED并刷新读回、再次确认DELAYED后才提交；正式产生A/2文件2054545668044084245，主管待审，B-A2-submitted-real-ui.json留证。没有按IMMEDIATE提交，没有另建无关申请。

## E20-P05 同链待生效基线检出500，随后租户识别也500

- Given：A1 ACTIVE，原4242形成A2正式局部变更，主管、负责人指派及实际会签、批准、文控均实际签名通过；原编制人Manager通过页面选择2026-10-25 12:00:00并签名947，A2真实回显受控（待生效）。
- 夹具变化：原工作C4241和B4242均在A2受控后显示已替代；直接来源为DccControlledFileFinalizationServiceImpl.supersedeStaleWorkingIterations，对较旧unfinished working迭代标SUPERSEDED。本例没有把这些历史草稿记作当前WORKING通过。
- When：Manager从A2详情的真实“检出/检入”入口进入，URL workingFileId=4245/master=2054545668044079203；检出按钮可用，输入本例修改原因并点击检出。
- 实际：自然POST /admin-api/dcc/controlled-files/2054545668044084245/checkout HTTP500。刷新后仍显示A2待生效，无检入/撤销检出入口；未上传新源件、未重放检出，未创建新申请。E20-P05-pending-checkout-500.json/png留证。后台具体异常尚未取得，不能认定业务拒绝或生产根因。
- 后续独立前置：fresh Reader context真实登录页显示“租户识别失败：请检查租户名称、本机后端服务和租户配置。”自然GET tenant/get-by-website及tenant/get-id-by-name均500，未进入账号登录；同段notify unread也500。因此检出500暂不能与运行态依赖故障分离，不扩大修改QueryService/DTO/后台。
- 最小BDD：Given已完成延迟确认的同链最新受控待生效版，When正式检出并检入任务自有正文，Then若业务允许，应生成WORKING且保留ACTIVE/待生效；若规则禁止，应明确可见禁止原因，不得系统500。运行依赖正常后先只读核验检出锁，再判定是否需要原UI复现；不因timeout重放。
- RED设计：先验证同账号租户识别及原A2回读恢复；冻结原file/master/状态后，用实际允许的检出组件行为测试验证受控待生效候选的按钮资格与正式命令一致。后台500根因缺日志前不指定实现补丁或伪造单测RED。
- 环境证据：tenant-identification-500-blocker.json；业务证据与自然请求均在artifacts/acceptance-resume/2026-10-18T13-18-44-749Z/。8个DRAFT参数因无WORKING夹具阻塞，其余未执行参数因租户识别故障阻塞；不是产品权限FAIL。
- Root22:54已明确核实：后端PID34440退出、48061端口连接拒绝，登录与业务500由后端不可达造成，非本例账号权限错误。P05当前归为运行态阻塞，不据此申请生产检出修复。等待同一200656软件版本恢复、健康及真实登录明确放行；先UI只读核验A1/A2、历史草稿及检出锁，再决定续办入口，不假定500意味着业务写入绝未发生，不重复检出/检入/提交/签名。

## E20-P06 恢复后待生效基线检出命中不一致的状态条件

- 新运行PID14864、同200656包264efbd4，Manager新独立context本人真实登录完成；先通过原列表、A2详情及检出/检入入口只读确认A1 ACTIVE、A2待生效、旧草稿已替代，页面无检出人/检入/撤销检出入口，检出按钮可用。证据run2026-10-18T15-03-51-377Z/recovery-original-list-readback.json、recovery-checkout-lock-readback.json。
- 仅在以上只读回读后执行一次正式检出（未检入/上传、无新申请）。自然POST /2054545668044084245/checkout HTTP200，但未出现检入入口。trace导出的checkout-frame-7/9/10.jpeg显示真实提示“受控文件已由用户910350检出，不能重复检出”。不能把HTTP200当成功，也不能单凭该提示断言数据库存在锁。
- 直接源码链：DccControlledFileVersionPolicy.isControlledBaseline包含CONTROLLED_PENDING_EFFECTIVE；QueryServiceImpl.canCheckoutLockedVersion接受最新受控基线、编制人；但DccControlledFileMapper.checkoutByIdAndTenantWhenAvailable的UPDATE仅含WORKING/REJECTED/ACTIVE/SUPERSEDED/PENDING_APPLICANT_REWORK，缺CONTROLLED_PENDING_EFFECTIVE。doCheckoutControlledFile在claimed!=1时复用ALREADY_CHECKED_OUT(userId)，可产生上述误导提示。此状态条件不一致与本次真实A2触发吻合；未读取接口响应JSON或DB造结论，最终锁状态仍须正式UI回读。
- BDD：Given最新受控待生效A2、本人有编制权限且正式UI允许检出，When正式检出，Then若合同允许应得到本人的锁及检入入口；若合同禁止应在资格和页面一致禁止，不得把状态条件拒绝误报为本人已检出。
- 最小RED设计交Root：对实际mapper UPDATE做受控待生效状态的数据库集成测试，先证明允许资格的A2更新行数为0（不以mock mapper默认1冒充覆盖）；QueryService对应错误分支应验证状态拒绝不复用已检出原因。Root核对业务规则后唯一授权实现范围，本例不改QueryService/Mapper/生产测试。
- 本例停止检出重放，8个WORKING参数保持具体阻塞；继续原ACTIVE/待生效及撤权参数，不用历史草稿或别链替代。
- Root已进一步核对真实policy并确认待生效基线被允许：修复边界为Mapper原checkout UPDATE补一个状态，不改资格/表/迁移/错误码/其它工作流。本例当前仅获准新增DccWorkingSubmissionConditionTest中的一个真实SQL RED方法，已写BDD并添加pendingEffectiveControlledBaselineCanBeCheckedOutOnceWithoutWeakeningGuards；Root独占Maven尚未执行，本例不预记RED或GREEN。
- 新测试使用UUID私有H2、真实MyBatis Mapper与真实VersionPolicy；正常待生效行应影响1并回读锁、时间、原因，第二次0且保留原原因；已有锁/跨租户/删除/四个待审阶段均0。旧submission/checkout并发方法保持原文，Mapper/Query未改；范围核对见p06-sql-red-review.json。
- 独立E2E已继续至19 PASS/8 WORKING BLOCKED：Reader ACTIVE四入口、待生效四入口，Denied两状态各四入口，撤权后旧页实际“重新加载正文后重试计时”、旧页reload及新页均可见拒绝且无canvas。Reader VIEW及原备注已恢复并读回，原A1正文重新非空渲染。
- 脚本断言修正已留痕：详情页“查看受控版本正文”按钮存在不等于能读正文，已实际点击并立即断言打开的原A2 DAILY viewer拒绝；Denied列表隐藏目标项目/文件后，仍通过真实地址拒绝页的“查看详情”打开只读详情，确认无正文控件/canvas。没有把按钮存在、已加载缓存或隐藏DOM直接判成权限漏洞或PASS。
