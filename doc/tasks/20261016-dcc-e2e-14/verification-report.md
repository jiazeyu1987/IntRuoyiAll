# E2E-14 文控后立即生效验证报告

## 2026-10-19 r4 遗留导出菜单合同

结论：旧测试与正式产品约定冲突，生产功能不需要修改。正式依据为`doc/tasks/20260908-unified-csv-approval-evidence-entry/task.md`的Goal/Design Constraints及`verification-report.md`的账号安全/旧入口清理：用户清单与通用账号证据导出统一到CSV质量包；当前CSV账号入口保留`system:user:export`、正式`UserApi.exportUser`和`exportGenericAccountUsers`。旧工具栏测试仍要求用户管理页导出，故本轮只更新该测试。

- RED（旧测试）：前端目录运行`node --test`并同时输出TAP/JUnit，exit1，首个失败`advanced menu must render 导出`；原始日志/XML保留在`repair-20261019-r4/legacy-red.*`。这是过时合同失败，不是新生产缺陷。
- GREEN：更新后的`system-user-toolbar-advanced-actions.test.mjs`为2 PASS/0 FAIL，exit0，见`green.log`/`green.xml`。保留新增、导入、钉钉导入、批量删除、删除组织及五条原分派流程、无重置和短标签约束；旧导出正向断言替换为菜单/分派块内负向约束，并补CSV权限及两个正式API保留断言。
- 有效负向验证：`node doc/tasks/20261016-dcc-e2e-14/verify-toolbar-contract-r4.cjs` exit0，8项内存源码变异各自exit1且命中预期断言：恢复旧导出标签/命令/分派、删除新增命令/流程、移除CSV导出权限/用户清单API/通用账号API。没有修改任何生产文件，不是E2E或业务接口模拟。
- 相邻合同：`node --test tests/e2e/signature-governance-csv-evidence-entrypoints-static.spec.cjs` exit0/1 PASS，见`csv-regression.log`/`csv-regression.xml`。仅静态源代码检查，不启动Playwright，不记为新页面PASS。
- 本轮唯一前端修改为工具栏测试；index.vue与UserForm.vue原始SHA256仍与r3相同。未重复14+6组件/相邻检查、全量类型检查或已通过页面；相应PASS引用r2/r3交接和原Root review。未运行Maven、构建、服务重启、业务写入、Git写入或Agent。
- 文档更正：下节r3时间显示原误用了America/New_York，已按用户Asia/Shanghai更正为19:47:26—19:49:20；原始UTC日志和结果JSON不变。

Root独立review已接受R4，状态`ROOT_ACCEPTED_R4_REVIEW`。Root确认正式合同依据有效、旧导出断言修正未改变生产代码、新静态合同2/2、CSV回归1/1及8项负向变异符合预期；原立即生效和r2的14+6、r3全量类型检查PASS保留且未重跑。Review来源为Root线程`01a13ad1-10c0-72a3-b271-f192b6058114`于2026-10-19交接的明确接受结论；接受范围与工程收尾状态分开记录。

Git与cleanup仍未授权，任务工程状态blocked。此前cleanup preview实际exit3/BLOCKED（工具仅接受ready_for_closeout/completed），尚未列keep/delete；未执行apply或删除历史资产。此次只更新review记录，没有运行E2E、类型检查、Git或cleanup。完整命令、XML、归属、保留证据及未执行边界见`repair-20261019-r4-handoff.json`。

## 2026-10-19 r3 全量类型检查

唯一类型检查窗口已完成并释放：在IntRuoyiFronted执行正式`pnpm ts:check`，使用package.json既定`NODE_OPTIONS=--max-old-space-size=8192`、`vue-tsc --noEmit -p tsconfig.relaxed.json`，exit0/PASS，无类型诊断。启动前后其他类型检查进程均0。执行时间2026-10-19 19:47:26—19:49:20（Asia/Shanghai），耗时约114秒。

本轮没有生产代码修改，两份用户页源码SHA256与r2已验证版本一致，因此按本轮派工保留14项定向/6项相邻及真实页面PASS，没有重复运行。旧导出菜单合同FAIL仍单独保留，没有改正式菜单或重跑旧断言。r2“类型检查待窗口”已由本节实际PASS解决。

证据：[原始日志](repair-20261019-r3/type-check.log)、[退出码及进程检查](repair-20261019-r3/type-check-result.json)、[r3交接](repair-20261019-r3-handoff.json)。无需后端新包或服务操作；待Root独立review，工程Git因当轮禁止提交推送仍blocked，未cleanup apply或标completed。以下r2记录保留其当轮历史状态。

## 2026-10-19 r2 用户异步身份定向修复

本轮定向实现/组件GREEN/真实页面验证通过，待Root全量类型检查排期和独立review。工程Git收尾独立blocked。原立即生效链的ROOT_ACCEPTED保持有效，没有重跑。

已证实：实际列表函数在旧分页或角色补充响应晚到时覆盖新查询，旧请求还会提前清除新请求loading；实际UserForm在关闭D后打开U/新增时可被旧D详情覆盖，辅助选项失败后仍可能提交。证据为实际SFC编译后执行的组件测试与受控Promise完成顺序，**没有宣称在真实页面重现历史910170事故，也没有把缺少row-key单独认定为事故根因**。

最小修复仅涉及用户页两文件：表格row-key=id；列表冻结参数并在分页及角色补充完成后各校验请求序号；编辑详情/部门/岗位全部加载完成且属于本次打开才回填，校验详情ID；加载失败显示错误并禁止提交，旧校验与旧提交完成不能改动新表单状态。原部门清空修复保持。

- RED：`node --test doc/tasks/20261016-dcc-e2e-14/user-async-identity.test.cjs`，修复前9项中8 FAIL/1 PASS；旧D实际覆盖U、loading提前清除、失败后产生模拟保存调用等断言失败。
- GREEN：新增身份拒绝用例后，同一组件测试10 PASS；原部门清空测试按真实open加载前置更新夹具后4 PASS，合计14 PASS。夹具迁移前的2项失败已记录，不改部门清空断言。
- 相邻回归：列表模板、角色/岗位、部门负责人排序、组织删除共6 PASS。工具栏旧合同1 FAIL（要求导出菜单）；已只读加载HEAD原index运行相同合同，确认修复前同一失败，未改该合同或新增导出功能。
- 两个实际SFC script/template编译PASS，目标源码git diff --check PASS。全量`pnpm ts:check`依本轮排期未运行，准确命令已交Root，不运行Maven或切包/重启。
- 真实Playwright：`node doc/tasks/20261016-dcc-e2e-14/verify-user-identity-r2.cjs` exit0。芋道源码/admin仅作为用户管理操作者，目标仅自有D 910351/U 910368。连续D/U查询最终为U唯一行；两人的自然GET详情ID、行编号、账号和弹窗昵称一致；取消D编辑后正式新增表单为空并取消。用户写请求0，pageerror 0，独立context已关闭。没有强制响应乱序、mock或接口/DB代办；真实网络时序不能代替组件测试中的确定性乱序覆盖。
- 唯一证据目录：[result.json](repair-20261019-r2/2026-10-19T10-34-12-733Z/result.json)，同目录4张截图与trace.zip。编辑U及取消后新增空表单截图已人工视觉核对。
- 取证校验：[artifact-check.json](repair-20261019-r2/artifact-check.json)，trace ZIP完整187 entries，network为空，已知凭据命中0；本轮trace登录后才开启，snapshots/sources关闭，仅本轮trace去除console/pageError载荷，页面错误另有明确计数0。旧证据均保留。
- 运行态：8061 PID20976为本仓库Vite branch-qms，前端改动稳定后新context开始；后端仍是Root reboot1019证据里的workflow-v6。本次只改前端，不需要新后端，不声明13/22/24的新后端修复已加载或PASS。

本轮未创建新账号/业务资产，没有保存、重置密码、授权或删除动作，原D/U及全部历史资产保留。完整文件/方法、命令、排期和证据见[交接JSON](repair-20261019-r2-handoff.json)。以下各节是原10月18日已验收业务记录，时间和运行态不用于替代本轮证据。

## 当前结果

2026-10-18，业务结果 **ROOT_ACCEPTED**：Root 于03:18:13（上海时间）独立接受14-A—14-D 4/4及空密码、空意见负例，见 [Root review](../20261016-dcc-24-e2e-coordination/e2e14-root-review.json)。保留秒级页面精度边界；工程 blocked 仅为Git未授权与资产保留，不再运行新业务或清理。

本轮只修改本例目录、脚本与证据；没有生产修改、Maven、服务/数据库操作、Git 写入、新 Agent 或线程消息。没有新建恢复项目，始终复用项目284（D OWNER / U EDIT）。历史基线账号910170恢复证据保留，未再次操作。

## 精确身份与运行态

- 前端8061、后端48061；Root提供PID31976/24564和e2e10-rework-matrix-runtime-proof.json；Jar SHA256为9f90ac84c858b2b0db3ba25de6a885760a6f5a4ff2798c3531389850198300cc。
- 项目：DCC241016C14P / 284；文件编号：DCC241016C14IMMEDIATE；文件ID：2054545668044084215；版本：A/1。
- BPM：490a44fc-ca50-11f1-a253-b082e25ec548；原文控任务：efd7b247-ca54-11f1-a253-b082e25ec548。
- D：DCC241016C14D / 910351 / DCC241016C14文控D；正式文控签名：#829 · VALID。
- 审批来源为本例正式 APPLICATION_CUSTOM，选择本例部门/A/D；本报告不声明覆盖 PUBLIC 类型矩阵。

## 四分支及负向证据

| 分支 | 结果 | 实际页面断言与证据 |
|---|---|---|
|14-A 取消文控签名|PASS|D本人打开同一文件/版本/任务后取消，重载仍为原BPM/原任务，实际生效为“尚未生效”、受控为“尚未受控”，没有有效文控签名；再次打开仍同一taskId。历史trace见resume-20261018/cancel-and-empty-password-history/。|
|14-B 重试并签名通过|PASS|同一D/任务重新完成签名，最终节点记录为文控标准化审核与签名、审批通过·已通过、#829 VALID。最终审批请求仅1次，未重复签名。screenshots/14-C-doc-control-signature.png。|
|14-C 真实生效事实|PASS|签名和实际生效原始可见值均2026-10-18 02:21:18；受控与日常启用均2026-10-18 02:21:20。没有预填生效日期或回写原时间。screenshots/14-C-effective-detail.png。|
|14-D 日常目录|PASS|当前生效受控版本中精确编号唯一行，版本A/1、状态“受控（已生效）”；同一行浏览打开同一文件，正文PDF真实非空canvas。screenshots/14-D-active-row.png、14-D-nonempty-pdf-canvas.png。|

空密码：页面提示“请输入登录密码完成电子签名”；仅填真实密码而不填意见：提示“请输入审批意见”。两者均未发审批请求，取消/刷新后仍同一待办且尚未生效。空密码/空意见结果原文位于e2e-14-result.json.negativeBranches，截图screenshots/14-A-missing-signature-inputs.png。密码输入、签名确认请求均处于trace关闭区间，清空密码后才恢复trace/截图。

## 时间精度与文案

上述时间是页面原始可见字符串，未剥离小数。当前只读节点和详情均只显示到秒，因此本轮页面证据只证明该精度一致，不证明服务端微秒级值完全相等；既有后端微秒合同没有被改动。

上传汇总显示“文控审核后立即生效”，当前只读详情实际显示“文控标准化签名后生效”，与workflow/effective-facts.ts的同一立即生效模式映射一致。报告分别保留实际文案，以原始签名时间、生效事实和ACTIVE终态验证语义，没有为脚本修改产品文案。未补造第8项缺失的盖章细则；正文中出现的受控章仅为实际观察。

## 前置与过程中的问题

1. 原U全部版本按本例前缀真实回读0条，确认未提交后沿原编号提交1次。旧UNREGISTERED_DEFINITION在新包已消失，不再作为当前blocker。
2. 新页面明确反馈S/L/A/D缺电子签名授权；admin正式授权页只配置本例S/L/C/A/D，未新建或修改全局electronic_signature_admin角色。C为本例实际被指派会签人，也单独授权。
3. S本人签名后原待办消失。L前三次只因旧placeholder/不稳定占位文本/祖先section歧义而停止，均未写入；第四次已实际签名指派，随后返回待办，脚本等待旧成功文案超时。没有再签L；C本人精确待办及L→C VALID节点记录确认后完成C签名。A本人批准并确认U为文件负责人。原节点检查点见resume-20261018-checkpoint.json。
4. D第一次主脚本在页面异步加载前读取“当前没有待办”，未写入；改为等待明确文控任务。取消已通过后记录checkpoint，后续不重复该分支。空意见错误同时出现在提示条与字段，修正为字段容器唯一定位。
5. D最终签名提交后旧加载层wait因两个遮罩报歧义；此后只运行verify-final-state-readonly.cjs，无业务写入，未重跑D签名。主脚本失败结果保留在resume-20261018/main-signed-before-final-readback/，不冒称单次主脚本全程exit0。
6. 日常正文按钮禁用属于本例查看权限缺口：配置身份只给本例类别中的用户D一条VIEW矩阵，实际预览/重开均只解析D 1人；没有全局READ/OWNER、没有改审批正文权限或共享矩阵。证据live-evidence/resume-20261018-D-only-view-matrix-reread.png。fresh D后同一原列表行可浏览。

## 命令与最终证据

- prepare-controlled-file.cjs --submit：最终exit0，submitRequests=1，结果upload-preparation-result.json。
- advance-original-flow.cjs S / C / A / D：分别exit0；D仅核对待办。L最后一次exit2是已提交后旧文案等待失败，实际指派由C只读签名记录及后续本人签名证明，不重复L动作。
- e2e-14-immediate-effective.cjs：最后签名运行exit1，14-A及空密码/空意见已PASS，唯一D签名请求已发；失败来自签名后的加载层定位，不当作产品失败或重提依据。
- verify-final-state-readonly.cjs：最终exit0、PASS、businessWrites=0，核对#829 VALID/真实可见时间/ACTIVE/正文canvas。final-readonly-result.json记录原始值、精度边界和唯一行。
- 最终完整只读trace：resume-20261018/2026-10-17T19-02-23-692Z-final-readonly/trace.zip；同目录result.json、final-active-browser.png。节点签名和PDF canvas局部截图已由Agent打开目视复核。
- 取消及空签名输入trace：resume-20261018/cancel-and-empty-password-history/、e2e-14-before-negative-secret-trace.zip、e2e-14-pre-signature-trace.zip。所有失败/旧申请现场保留待Root review。

## 清理与工程状态

任务自有账号、角色、目录、类别、查看/上传规则、项目和已生效文件保留；完整residual在fixture-manifest/prepared/e2e-14-result三份JSON一致。当前未清理，因此cleanupCompleted=false；浏览器与本任务执行进程均已关闭。没有Git提交/推送授权，不标completed，不在Root review前删除证据。

正式review入口：acceptance-result.json，逐项引用14-A/B/C/D、原文件/BPM/任务、签名829、单次提交/签名、取消及缺签名零推进、原始秒级时间、ACTIVE行、真实PDF非空canvas以及对应脚本/JSON/截图/trace。历史失败目录和旧问题记录保留，未进行扩大归档或技能清理。

最终证据完整性检查：当前4份新helper trace定向脱敏exit0；verify-own-evidence.cjs exit0，检查288项文本/trace、77份trace及全部空network条目，当前已知凭据命中0，manifest一致性PASS。仅处理本例当前证据，没有扩大归档/清理。


## 2026-10-20 第二次完整重跑第三轮收口

本轮 rerun-20261020-r2-round3 已结束并HOLD；新文件2054545668044084364/A1，BPM a1fd2026-cc4b-11f1-ada5-b082e25ec548，D任务2760ec1d-cc4c-11f1-ada5-b082e25ec548。U单次提交，S/L/C/A真实前置完成；D取消、空密码及空意见均阻止推进，正式approve-task仅1次，HTTP完成响应未捕获（UNKNOWN）。首次D弹窗65秒未关闭且页面/阅读统计显示Cannot access，原失败保留。fresh D FINAL exit0确认#1198 VALID、上海时间14:05:43签名及生效、14:05:49受控和日常启用、ACTIVE及非空PDF。追加U只读本人核验浏览按钮禁用，UREAD exit2；state中14-C/14-D保留U FAIL，其余4项PASS，不能记无异常全绿。CHECK旧待办预期与EXACTD原URL头部超时均保留，不重签、不扩权。15trace/48截图证据检查exit0，10个runner及全部browser/context关闭，Jar哈希及8061/48061 PID复核一致。只保留任务资产，未改产品/共享权限、未构建重启、未Git/cleanup；工程blocked，不标completed。详见 rerun-20261020-r2-round3-report.md、rerun-20261020-r2-round3-handoff.json及rerun-20261020-r2-round3/result.json。


## 2026-10-20 签名错误分离定向 UI 复验（新 R/O 单据）

Root 明确释放 E14 唯一串行窗口，并从 Vite HTTP inline source map 验证两份已接受修复源码。前后端 20976/8061、41508/48061 与 Jar 709e78bd...aaa147 沿 Root 新确认；没有复用旧前端 sha 作为加载证明。官方 U 登录 preflight exit0 后，按本人 UI 完成两个新夹具 U/S/L/C/A 前置，所有阶段均仅一次业务动作。

R：文件2054545668044084377，编号DCC241020E14R1792487781321，D一次专用错误口令控制，自然HTTP400，弹窗保留“电子签名身份验证失败”、无Cannot access覆盖或未确认误标，fieldErrors为空，正文无阅读拒绝。原runner因文案正则漏项exit2；原JSON/trace不改，Root独立review业务PASS，旁置run-R/interpretation.json。新只读会话确认同一D待办仍在、文控VALID签名0、尚未生效。绝不重放R，也没有重复其前置。

O：独立文件2054545668044084378，编号DCC241020E14O1792488015305，D一次正常签名，POST 17:21:39.893→HTTP200 17:21:54.346（上海时间），弹窗关闭、待办消失；新只读会话看到同编号A/1“受控（已生效）”。未确认与阅读拒绝均NOT_EXERCISED，不制造故障、不追加样本。原#1198未动，原现场网络根因继续OPEN。

定向证据23trace CRC/69PNG/15阶段结果PASS，已知凭据命中0，110份历史诊断/修复/业务文件哈希不变，两份生产源码前后哈希不变。全部14个实际浏览器/context及15阶段runner/官方preflight子进程已关闭，自有后代进程0，服务PID不变。trace只读扫描，未删事件或重写；签名关键窗口用无凭据请求时间线和密码框遮挡截图补证。R错误及O列表关键截图已目视复核。原始runner失败不覆盖或改写，细节见r2-signature-ui-recheck/verification-report.md、result.json及handoff.json。

本次窗口执行结束并HOLD；无产品代码修改、API/DB业务动作、权限变更、构建/重启、Git写入或清理。R待文控夹具和O已生效夹具保留交Root，工程blocked，不标completed。
