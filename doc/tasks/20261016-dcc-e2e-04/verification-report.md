# E2E-04 当前真实验证报告

当前业务结论：12 PASS / 0 FAIL / 0 BLOCKED。原主申请单次提交并刷新确认“待直属主管审核”；04-08原保存回显错误保留，Root修复后在其明确授权的唯一最小工作稿上取得真实UI GREEN。工程状态等待Root review和统一收尾，未标completed。

## 本轮环境和原身份

- int_qms，前端8061/后端48061，PID16776；Jar SHA494306625fb1d04261832e4145a8aa83c3734257c02069e9b1e7644749efcdec。Root健康证明overnight-runtime-proof.json；未重启或改运行态。
- 原U：DCC241016C04U。原项目293、类别908745、类型126、文件夹31均复用；未扩大权限、未重建/重传。原文件2054545668044084183，编号DCC241016C04DOC-R3，A/1，源件DCC241016C04-source-r2.pdf。
- 本例原岗位/权限/本人签名图片准备沿用。项目创建审批已有OWNER，本轮不改级别；类别UPLOAD已独立核对。本例曾跨例误加岗位已审计及当前弹框复核恢复，Root接受，完整incident.md；本轮未编辑其他例账号。

## 12分支当前结论

| 分支 | 状态 | 真实依据 |
|---|---|---|
|04-01|PASS（原证据保留）|原T1两份默认名单D1/D2，批准人A基线。|
|04-02a|PASS（原证据保留）|删会签D1，下发不变。|
|04-02b|PASS（原证据保留）|恢复D1后删D2，会签D1，下发不变。|
|04-03|PASS（原证据保留）|删最后会签，提示至少一个，最终提交请求不增加。|
|04-04|PASS（原证据保留）|会签D2/DS，下发D1/D2，路线就绪。|
|04-05a|PASS（原证据保留）|下发加DD，会签D2/DS不变。|
|04-05b|PASS（原证据保留）|下发删空，会签不变，再加DD；仅验证空名单页面操作，未单独保存空下发。|
|04-06|PASS（本轮原稿GREEN）|新context打开同一原稿，详情无TDZ/只读事务错误，已保存会签D2/DS、下发DD精确回显。|
|04-07|PASS（本轮）|移除会签DS，保存并核对正式配置，成功提示，刷新重开为会签D2/下发DD。|
|04-08|PASS（Root修复后真实GREEN）|原稿两名单持久化断言通过；原错误保留。最小ECHO1工作稿以DD→D1顺序保存成功、无回显错误，刷新会签D2/下发D1、DD。|
|04-09|PASS（原独立证据保留）|另开新申请同T1两名单仍D1/D2，批准人全文与04-01 trace相同；未改公共矩阵。|
|04-10|PASS（本轮）|实际二次确认会签D2/下发D1、DD，单次POST /{id}/submit；刷新显示待直属主管审核、BPM轮次及主管S路线快照。|

## 本轮恢复、保存和单次提交

- root-original-trigger-resume-result.json记录原触发链GREEN：原U新context、同fileId、详情读取和本次申请面板加载、会签D2/DS、下发DD；pageErrors=[]，无新建/重传。
- r6-04-07-result.json记录保存刷新精确名单；r6-04-08-result.json记录保存前下发顺序[DD,D1]、刷新后[D1,DD]、同一会签D2及真实错误提示。截图003为错误，004为刷新后的真实名单。
- root-save-echo-warning.json保留原缺陷和只读源码定位（已由后续真实GREEN关闭）。前端DetailApplicationPanel.vue:586直接JSON.stringify比较数组顺序；后端normalizeDistributionDepartments排序后持久化。相同集合但顺序不同可解释误报，当时为源码推断；Root随后完成仅两名单sorted副本比较修复及32项测试，本例未改production。
- 实际填写“初始申请说明”后打开二次确认。r6-submit-confirmation.json及截图006精确显示会签D2、下发D1/DD，批准人A、原项目/类型/编号/版本。
- 最终POST /admin-api/dcc/controlled-files/2054545668044084183/submit只有1次、HTTP200。脚本最初等旧/submit-working-iteration路径超时，未重新点击或重复提交；随后用真实DOM及刷新核验。r6-04-10-result.json明确submitRequestCount=1。
- BPM为22c11270-c993-11f1-8f08-b082e25ec548，页面状态“待直属主管审核”，提交时间2026-10-17 02:55:20（Asia/Shanghai）。路线快照主管DCC241016C04S、会签仅D2/L2、批准A、正式文控；提交入口已消失。终点是主管节点待办前状态，不是审批完成。
- 附加只读检查：S从真实菜单审批中心→待办显示“没有该操作权限”。未扩大角色，未进行审批；未宣称S个人待办队列已验证。04-10通过依据为U提交后刷新业务状态、BPM及正式路线快照。

## 证据与保留

- 汇总rounds/resume-494306/result.json，原8PASS来自历史run及04-09独立trace，本轮继续原fileId；非单次新建全跑，不删除原FAIL/BLOCKED记录。
- 原run保留runs/2026-10-16T17-53-32-047Z；新journal为ui-journal-r6-494306-resume.jsonl，脱敏trace见fixture-manifest.json，r6-494306-resume-ui-*为本轮截图/DOM。
- node --check e2e-04.js、e2e-04.js --check通过；后者只是静态合同。audit-evidence.js通过，69份trace无凭据明文且网络载荷移除。
- 浏览器关闭。保留已提交记录待Root review，不再编辑/保存/提交、不撤回删除、不冒充审批完成。业务PASS，任务状态等待Root review及工程收尾，未执行Git；cleanup最终preview/apply均通过（delete0）。

## 04-08直接保存缺陷真实GREEN

- Root明确授权：原稿已提交，不可退回重签；仅为排序误报建立一个最小工作稿，复用原项目293、类型126、类别908745、U及公共矩阵。
- 原source-r2文件名已被正确占用，页面拒绝，未创建工作稿；该HTTP400属于正确文件名规则，没有修改产品。旧浏览器句柄失效及本例Node进程0已核对，未把观察超时当进程停止盲目重跑。重新启动后仅建立ECHO1一个工作稿。
- 使用PDF技能创建唯一文件DCC241016C04-source-ECHO1.pdf，标记C04-ECHO1-20261017，SHA289f6d5886acb03739ca00d5baa178dbec13561defc74b8c5ae03aa4f52627ba。已渲染并检查布局。生成脚本首次缺fitz在任何文件写入前失败，改用已安装pypdfium2后成功；不是业务失败或模拟成功。
- 最小工作稿fileId2054545668044084184，编号DCC241016C04DOC-ECHO1，A/1。初始保存会签D2、下发DD；详情只向下发增D1，输入顺序[DD,D1]，真实页面显示“本次申请草稿已保存”，保存/刷新均无任何回显错误。刷新后会签[D2]、下发[D1,DD]。
- GREEN证据root-save-echo-green.json、save-echo-green-before.json、r7b-save-echo-green-ui-002输入顺序/003保存无错/004刷新名单截图及DOM；journal=ui-journal-r7b-save-echo-green.jsonl。旧RED为r6-04-08-result.json和r6截图003，未覆盖。
- ECHO1未提交（submit请求0）；最后只读重开原主申请，仍待直属主管审核/BPM22c11270-c993-11f1-8f08-b082e25ec548。原主申请submit请求恰为1，未退回或重签，截图r7b-save-echo-green-ui-005。
- 最终汇总rounds/final-echo-green/result.json，原8PASS保留，04-06/07/10在原稿续验，04-08经唯一授权工作稿回归关闭。明确为跨恢复轮次证据，非单次新建12分支run。
- 69份trace凭据/网络载荷脱敏审计PASS。原申请和最小工作稿均保留供Root review，浏览器关闭，无新权限/账号/项目/矩阵变更；主管本人待办的额外只读权限缺口作为非本例终点事实保留，不冒称其队列已验证。

## 收尾检查

最终文档/JSON结构核对PASS。任务曾置ready_for_closeout，cleanup最终preview/apply均PASS、delete0/warnings none。首次apply因新输出日志未登记keep而自删除遭WinError32，未删除证据；原失败保留，补keep后重跑通过。无提交推送授权、Root工程review待办，任务状态blocked不标completed；业务仍为12PASS。


## 2026-10-20 第一轮新重跑

当前轮12PASS/0FAIL/0BLOCKED，完整独立报告rerun-20261020-round1/verification-report.md和rerun-result.json。新文件2054545668044084287/DCC241020C04R1，BPM31450180-cc28-11f1-a8a5-b082e25ec548，单次提交后待直属主管审核；本轮断点恢复只使用该新对象，未复用历史终态。原始脚本FAIL及其校准原因全部保留；无已确认产品缺陷。浏览器/runner关闭，不执行下一轮。
