# E2E-15 Verification Report

## 2026-10-19 r4：恢复条件与边界收口

本轮仅复用已完成来源检索，不重复扫描、不重跑业务、不启用全租户调度。PA2 的同 U、同 VIEW 权限 T 前禁用与 T 后非空正文，以及任务自有 5626 的自然触发成功继续按历史证据接受；本轮不把旧运行态或文档核验写成新包 PASS。

恢复条件已写入[本轮交接](repair-20261019-r4-handoff.json)：旧 PA2 07:06:30 日常观察和原 F 确认后活动段，只有找到带原始时间覆盖且可校验来源哈希的 runner trace/临时备份才可恢复；截图、DOM、operations 或新运行不能重构旧 trace。全租户自动调度仍是未验收项，未来必须有独立授权、任务自有样本、Root批准的当前运行态、真实页面配置、自然 tick 页面终态和暂停回读。

## 2026-10-19 r2：仅现存证据整理

本轮保留15-A至G业务PASS及Root原接受结论；没有执行新的页面E2E、构建、服务切换或调度动作，也不证明13/22/24等新修复已加载。历史时间以下均为原页面UTC+08:00；执行快照仍是PID39740/Jar060621。

|核对项|现存原始来源及结论|
|---|---|
|PA2同权限T前|`resume-artifacts/r2-2026-10-17T22-19-16-702Z/pending-access-evidence.json`、同目录`pa-daily-body-disabled-before-selected-time.png`及`operations.json`的`PA2-before-selected-time-daily-gate`：07:06:30早于计划07:15:14；待生效、浏览禁用、canvas0。`supplement-checkpoint.json`记录U910452、同一VIEW基线及此前4216正文正控。|
|PA2同权限T后|`resume-artifacts/r2-2026-10-17T23-22-04-301Z/final-job-and-file-evidence.json`中`accepted.2054545668044084227`、`PA2-final-body-rendered.png`及`operations.json`的`PA2-active-body-real-nonempty`：实际生效07:17:00，07:25:24.893观察到非空正文；签名874/受控07:05:15未变。|
|旧中断trace|遍历本任务目录54个现存zip/54条trace流，没有归档时间覆盖PA2的07:06:30观察；未找到独立`.trace/.tmp/.partial/.har`文件。该检索只限本目录，不代表外部缓存恢复结论。`22-19…/8-U-trace.zip`按trace时钟折算约07:04:13–07:05:15，只覆盖确认前；不能替代T前日常观察。`21-19…/1-U-trace.zip`约05:20:25–05:23:10，原F成功后活动段也未保存。截图/DOM/operations仍存在；不补造或宣称完整逐动作trace。|
|调度边界|只证明任务自有5626精确参数自然tick；全租户常驻自动调度仍为未验证。5626最终暂停及浏览器关闭为旧证据，不作本轮实时状态核验；本轮没有启用任务。|

结构核验及原始引用清单见[本轮交接](repair-20261019-r2-handoff.json)。业务证据整理与工程收尾分别记录；用户本轮禁止Git提交/推送，依`docs/task-closeout-rules.md`工程收尾记BLOCKED，不影响既有业务PASS。不运行cleanup apply，不删除原始证据。

Root于2026-10-18 07:46:16接受：`ROOT_ACCEPTED_WITH_EXPLICIT_SAMPLE_AND_TIME_BOUNDARIES`，见[Root review](../20261016-dcc-24-e2e-coordination/e2e15-root-review.json)。Root核对27个关键阶段trace、真实DOM时刻/disabled断言、非空PDF、自然作业日志/暂停及10历史文件不变。原4216原before-time未测、PA1权限混淆、PA2严格前后分工均保留；本例不证明全租户常驻调度。

2026-10-18：**15-A至G真实UI通过，Root已独立接受**。本轮仅整理已有证据，没有重开浏览器或业务。最终执行环境int_qms 8061/48061、后端PID39740/Jar060621、前端PID24564，SHA ee59c01bc755da17b29360f6ba21c253f048c537606ce006351935ac456eda4a；不是现运行18204。范围是Root允许的APPLICATION_CUSTOM，不冒称PUBLIC。

|分支|具体文件、任务及可见结果|主要证据|
|---|---|---|
|15-A PASS|原4216 DOC/A1；04:06:36确认无日期且不培训，04:08:33观察单次提交成功|15A-confirmation.json、r2-04/r2-05截图、7-U trace|
|15-B PASS|D签名849（04:55:01）后直接给原U任务05a1ecdf-ca6d-11f1-9342-b082e25ec548，无培训，精确提示完整|15B-direct-original-author-task.json、r2-19/20/21截图、6-D/8-U trace|
|15-C PASS|同一原U任务；04:58:28缺日期被具体错误阻止，确认POST0，未受控/签名|15C-missing-date.json、r2-22截图、8-U trace|
|15-D PASS|同一任务；04:59:51合法未来时间但无密码被拒，确认POST0|15D-missing-signature.json、r2-23截图、8-U trace|
|15-E PASS|同一任务；05:01:02拒绝过去04:56:01，POST0；该时间早于最终签名05:23:11|15E-past-time-refused.json、r2-24截图、9-U trace|
|15-F PASS|原4216同任务确认签名858/受控同为05:23:11，计划05:27:10，计划前未生效；原策略失败保留|15F-original-task-recovered.json、fg-03截图、确认前trace及成功DOM|
|15-G PASS|原4216承担自然生效/日常正文；PA2承担同U严格计划前后对照|下表及逐项JSON路径|

|样本分工|时间、正文及限制证据|自然job5626日志|
|---|---|---|
|原4216 DOC/A1|签名858/受控05:23:11保持；生效/启用06:24:00，下发06:24:02。补齐本类别唯一USER910452查看矩阵后，U日常PDF非空，686×970、10398采样/305非白；G-08截图已视觉复核|47958，参数1:2054545668044084216，06:24:00–06:24:05，成功/生效1|
|PA2/4227 PA2/A1|同一U同一VIEW矩阵，先有4216active正文正控。签名874/受控07:05:15；**07:06:30浏览禁用、canvas0，早于计划07:15:14**。生效/启用07:17:00，下发07:17:01；同U日常PDF非空686×970、10398采样/305非白|48011，参数1:2054545668044084227，07:17:00–07:17:06，成功/生效1|
|PA1/4223 PA1/A1|06:08:39禁用时缺查看矩阵，**不作独立pending guard PASS**。补齐同U矩阵后计划时刻之后仍禁用，只证状态隔离。保留签名868/受控06:06:34；最终正常生效/启用/下发07:27:00|48021，参数1:2054545668044084223，07:27:00–07:27:03，成功/生效1|

本例5626经正式页面创建/编辑，handler=dccControlledFileTargetedActivationJob，cron `0 * * * * ?`，重试/间隔/监控均0；自然tick，没有API触发或“执行一次”。最终UI暂停且刷新回读（07:30:40），保留注册及日志，旧5625未操作；07:31:33 browser/context/runner均结束。Root最终只读确认10份其他历史文件全列hash不变、5626status2，见协调目录targeted-activation-final-isolation-proof.json；本线程未自行查询DB。

## 2026-10-21 Runtime Diagnostic Status

The E22 candidate backend was reported healthy on PID 37016 / port 48061. The single real-page trigger of global job 5625 was assigned exclusively to E22; this E15 review did not trigger or wait for it. Read-only inspection of `output/runtime/int_qms/logs/e22-r5-20261021/application.log`, `stdout.log`, and `stderr.log` found no job 5625 activation or `DCC_EXECUTION_CONTRACT_MISMATCH` entry. The actual contract, processKey, and digest therefore remain unknown, and this run cannot establish E15 activation business GREEN.

Static contract conditions: `DccBoundWorkflowContractResolver.resolve` validates file and tenant identity, bound BPM instance identity, business key, process key, exact process definition and deployment IDs, and a single full BPMN model before calculating its normalized digest. If any prepared field exists, prepared contract, definition ID, and digest must all match the bound definition; only when all are absent does the existing legacy rule select `LEGACY_V4_ACTION`. `DccWorkflowDefinitionCatalog.require` then requires exact digest, contract, and UPLOAD/REVISION key equality and rethrows `EXECUTION_CONTRACT_UNREGISTERED_DEFINITION`; there is no latest-version fallback.

The catalog contains UPLOAD V1/V2 digests `dbf44a1bd3afd792a27b195f43f50f4efc886ea85ec8c35b1c05730e9221e162` / `36bc2379b56d0ada0571fccd573024fb05f951632e0963cbcd9ab1702af7b2c2` and REVISION V1/V2 digests `9922d53710050318a4827b3b61449727ff87e3c62f3146619be4d19c7cffcd13` / `84d039f7fab6c81159fb049637a0ff70b6eb3e57d6dfc6b47a64475e421935d5`. Existing evidence for the v6 transition registry (two entries, SHA prefix `eca016d3`) does not identify the runtime mismatch.

Minimal next step: after E22's single page trigger, compare its diagnostic triple and candidate Jar identity to the catalog. If the digest matches but contract or key differs, correct only the proven source of that mismatching bound value and add focused RED/GREEN coverage. If the digest does not match, compare the exact deployed full-model digest with the frozen prepared definition ID/digest, then correct the definition or registration only after confirming the intended model contents. Keep strict rejection intact; do not add fallback or rewrite historical bindings. No production change, build, restart, API/DB business write, or Git operation was performed in this review.

[最终JSON](e2e-15-result.json)逐项给出A–G的文件、任务、北京时间、截图、trace及日志路径；[checkpoint](checkpoint.json)固定本次执行PID39740/Jar060621。旧结果、checkpoint、report存于`previousEvidence/`。原审计策略失败、原全局超时、PA1权限混淆、runner输入截断/错误等待、popup父页等待错误及编辑加载竞态均保留，不把旧失败改写成成功。

时间比较按可见DOM秒值，不冒称DB微秒比较。已保存trace移除network并避开登录/密码输入；部分中断活动段未保存，关键截图、DOM JSON及operations保留，具体缺口在JSON披露，不声称完整逐动作trace。资产按Root要求保留；未执行Git/cleanup，工程批量收尾由Root管理。
