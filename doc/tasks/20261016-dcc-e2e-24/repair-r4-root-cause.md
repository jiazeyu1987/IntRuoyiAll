# E24 R4 根因定位（仅记录，等待 Root review）

## 状态、目标与约束

ready_for_closeout — 本次只读诊断已完成，等待 Root review；不表示 R3 业务通过或仓库收尾 completed。当前分支 int_qms。使用 ponytail，读取当前 AGENTS.md、docs/task-closeout-rules.md、docs/e2e-rules.md，以及 Root 的 repair-r4-supervision.json、rerun-r3-issues.md。监督表未列 E24 owner；本次明确委派仅授权 R3-11 的诊断与记录，不据此扩大为修复授权。

目标：以既有页面、trace、请求元信息和代码定位三个首错，区分脚本问题、产品风险和未证明事项。里程碑：原证据核对→代码链核对→最小建议及 BDD/RED 设计→结构检查→等待复核。设计约束检查：未启动浏览器、未修改产品/测试代码、未运行测试/构建/服务、未调用 API、未读取响应 JSON 作为业务 oracle、未写数据库/Git、未启用 Agent。只新增本报告及追加自身 execution-log.md。本报告不是修复验证。

## 结论分层

|首错|已经证实|分类与边界|
|---|---|---|
|DWG 重开 hash 不同|同一 A/1、Car Porch 标记、685×969 画布、100%/0°；可见裁剪仅23/348330像素、最大1色阶差|精确 canvas 编码 hash 不是源文件不变的充分业务判据；观察到的微差符合绘制取整/抗锯齿噪声，但全画布原因未证明。不是已证实的原件改写产品缺陷。|
|SLDDRW 恢复计时84秒不增长|恢复 start、心跳、summary 均自然发生；恢复心跳耗时22.915秒；35秒内 DOM 仍84|真实观察失败保留。延迟与15秒不确认间隔规则可以解释不累计，但客户端耗时不能证明服务端取时点/实际gap。尚未定位到具体产品耗时根因。|
|XLSX A/1 计时90秒不增长|start 尚在途时脚本切换焦点策略；start 完成约136ms后 stop；之后没有心跳/新start|优先核对焦点/页面活动前置；不是“心跳累计失败”的证据。脚本缺少持续有效焦点/可见性证明；具体 stop 触发分支及是否存在产品恢复竞争尚未证明。|

不调整15秒策略、不扩大35秒阈值、不删除或放宽首错使本轮变成PASS。9/11格式、7/8版本、2/2账号隔离原结论保留；XLSX后续三次切换仍为未执行。

## 证据入口与可信边界

自身 rerun-20261020-r3-round4/ 中：reading-result.json、version-result.json、published-DWG-DCC241016C24Reader18.json、published-SLDDRW-DCC241016C24Reader18.json、XLSX-version-result.json；DWG-offline-comparison.json 与 before/after/diff PNG；SLDDRW-timing-diagnostic.json、XLSX-version-timing-diagnostic.json；DWG-reading.zip、SLDDRW-reading.zip、XLSX-versions.zip。trace-seal.json 记录24份脱敏trace CRC通过、raw为0、凭据命中0；close-receipt.json 记录context为0、runner退出0、非阅读写请求0。

原 raw trace 已按既定隐私流程删除，现存的是脱敏原执行 trace；不声称还能重新读取原始 network。请求起止耗时来自删除前保存的只读诊断，方法/路径/状态可与脱敏trace旁的 network.json 对照。frame snapshot/动作时刻从现存trace读取。trace未记录完整原生 focus/blur/visibility事件日志；DOM快照不等于焦点证据。CDP send 的实际命令未进入trace，命令来源是当时保存脚本。没有用HTTP200替代页面计时结果。

下述展示时间为 America/New_York（EDT，2026-10-20）；诊断JSON保留原UTC。trace单调时间按context-options中的wallTime/monotonicTime换算。

## 1. DWG：为什么不能以精确 hash 认定原件改变

首错位于 rerun-20261020-r3-round4/verify-published-body.cjs:65。比较的是 canvas.toDataURL() 的SHA256，不是原始DWG或承载PDF。before/beforeClose均243fce...，reopened为5a384b...。同一文件2054545668044084277、A/1、正文搜索Car Porch，重开正文工具再次完成；原首错没有被覆盖。

已有截图离线比较：可见正文裁剪(526,490)-(1209,1000)，348330像素仅23不同，RGB最大差1，差异边界位于裁剪内部(226,304)-(618,430)。工具区10875像素完全相同；搜索行1148像素差异集中于搜索按钮的激活/焦点外观，不能据此称正文不同。两截图都是1550×1000。trace显式canvas属性有685×969和相同CSS尺寸；两截图100%/0°。下部约459像素不在截图内，不能把可见裁剪结论扩展到全画布。

代码位置：IntRuoyiFronted/src/views/dcc/controlled-file/view/index.vue:1268-1300，以devicePixelRatio构建renderViewport、Math.ceil设置canvas尺寸，PDF page.render完成后drawControlledPreviewStamp。presentation.ts:183-216画入canvas的是短章文本及固定样式；动态traceCode、actor、timestamp由presentation.ts:161-168和index.vue:129-158的DOM/overlay呈现。故不能直接把canvas编码差异归因于新水印时间/trace码；PDF中原有内容、栅格化和canvas绘制仍可能影响像素。观察到的23个1色阶像素符合栅格化微差，但未保存全canvas原始像素，无法确定是哪个绘制阶段。

最小建议（待批准，不实施）：先校准验证对象和采证，保存整张实际canvas像素、尺寸/DPR、工具状态、正文标记及只读业务写请求清单；区分DOM动态水印与正文canvas。源文件完整性若需字节级证明，须另经授权使用明确来源的只读证据，不能用显示hash冒充。不要直接删hash断言或引入宽松像素阈值关闭本首错；现有证据只能定为断言不具源件改写证明力，业务异常未证实。

BDD/RED方案：Given同一源版本及工具状态，When独立绘制产生1色阶微差，Then验证报告应区分显示编码差异与源件变更，不能报“原件被改写”；另有正文标记/几何确实变化的对照必须仍被拒绝。RED先以两组明确的原始像素/正文身份对照证明当前精确hash单独分类会误判；没有全画布像素前不编造本例完整输入、不声明RED已运行。后续授权页面取证应覆盖全画布，保留初次FAIL。

## 2. SLDDRW：长耗时与15秒规则的因果边界

文件2054545668044084283。正文、搜索、缩放/旋转已完成，初始72秒，前台增长至84，失焦12秒稳定，恢复后35秒DOM仍84。原断言verify-published-body.cjs:58超时。

|EDT时间|原自然请求/动作|耗时/观察|
|---|---|---|
|12:11:31.124|POST reading-session/start|1.147秒、HTTP200|
|12:11:42.278|POST heartbeat|1.144秒、HTTP200；随后summary|
|12:11:44.595|脚本背景页bringToFront|随后断言document.hasFocus()为false通过|
|12:11:44.578|POST stop|0.286秒、HTTP200；与创建背景页/激活时序相邻|
|12:11:57.684 / .693|原预览bringToFront / 标题click|恢复动作|
|12:11:57.692|POST start|0.890秒、HTTP200|
|12:11:58.581|GET reading-summary|0.981秒、HTTP200；页面84秒|
|12:12:08.584|POST heartbeat|22.915秒，约12:12:31.499完成，HTTP200|
|12:12:31.498|GET reading-summary|1.061秒、HTTP200；窗口结束仍84|

完整路径见SLDDRW-timing-diagnostic.json，均/admin-api/dcc/controlled-files/2054545668044084283/下的自然请求。恢复前台后脚本未持续采集hasFocus和visibilityState，只有激活/点击不能证明整段始终有效前台；也没有证据表明整段失焦。35秒内只看到一次恢复heartbeat。

代码链：reading-lifecycle.ts:48-59在beating未完成时跳过重叠tick；start完成后第10秒开始heartbeat，schedule见:78及index.vue:831-836。慢请求在途使20/30秒tick被跳过，不会并发补报。index.vue:814-818在heartbeat promise完成后刷新summary。后端DccOnlineFilePreviewReadingSessionService.java:126-140在authorize、资源锁及owned row锁之后取now，gap严格大于15秒则increment=0，但仍推进lastHeartbeatAt；10秒/15秒常量在:29-30。

因此：如果长耗时发生在后端取now之前，这次服务端确认gap很可能超过15秒并按规则不加时；如果耗时在取now之后，客户端22.9秒并不证明实际gap超窗。仅网络时序不能区分排队、鉴权/锁等待、事务后阶段或传输延迟，更不能宣布死锁。HTTP200也不证明应用成功，响应JSON没有被用作oracle。35秒窗口在慢heartbeat刚完成及summary刚刷新后不久截止，未覆盖下一次正常10秒心跳，不能推断永久停计。

最小建议：保持15秒规则，先经Root指定负责人只读关联本请求的服务端接收/授权/锁获取/取now/提交/响应完成时序，优先消除已定位的长耗时；当前没有精确服务端根因，不给出猜测性生产补丁。后续获准验收必须持续记录真实前台条件，并分别证明超窗不累计及后续有效10秒间隔恢复，不能用延长等待掩盖本次35秒首错。

BDD/RED方案：Given上次确认后超过15秒才到达advance取now，When heartbeat处理，Then不补计未确认时段但更新lastHeartbeatAt；随后有效10秒heartbeat应累计10秒。服务测试已有:95-114覆盖超窗及15秒微秒边界，应保留。另用deferred heartbeat验证10/20/30秒tick不重叠、完成后后继tick能继续；该契约可能本来GREEN，不能称已发现RED。真正RED应针对定位后的耗时点/恢复失效条件编写，未确认前只登记请求延迟和页面停计观察，RED本轮NOT_RUN。

## 3. XLSX：start后立即stop，尚无心跳累计失败证据

文件2054545668044084271，A/1历史详情，状态已作废；页面正文非空，Start of 2nd sheet标记匹配，openSeconds90。版本脚本history分支popup=main（versions-calibrated.cjs:49-50），同一页面导航，无新popup。

|EDT时间|动作/自然请求|耗时/观察|
|---|---|---|
|12:18:17.862|GET reading-summary|0.713秒、HTTP200，页面90秒|
|12:18:18.891|POST start|0.917秒，约12:18:19.808完成|
|12:18:19.034左右|创建CDP会话，脚本发送关闭焦点模拟|send实际时刻未被trace记录；调用顺序来源脚本:60|
|12:18:19.0368 / .0409|bringToFront / 标题click|click约.092完成，start仍在途|
|12:18:19.807|GET reading-summary|0.669秒、HTTP200|
|12:18:19.943|POST stop|0.472秒、HTTP200；start完成约136ms后|
|12:18:20.410|GET reading-summary|0.646秒、HTTP200|
|其后35秒|仅轮询可见DOM|90秒，无新start/heartbeat；三次后续切换未执行|

versions-calibrated.cjs:46先在main关闭焦点模拟，:60对popup再关闭；history路径是同一页面。:61直接轮询计时，没有先断言并持续核对document.hasFocus()和visibilityState。首个summary能在正文加载但未有效前台时取得（index.vue:841-844的else分支）；90秒不能证明已启动会话。

stop路径定位：index.vue:828-829、849-853要求页面活动、visible和hasFocus；focus/blur/visibilitychange监听在:1425-1430。reading-lifecycle.ts:69-72在start返回时若disposed、generation变化或visible=false立即stop，符合本次“start返回→紧接stop”形状；:31-42的pause在失焦/隐藏时也会使在途start失效并等待关闭。导航/代际重建也可关闭旧owner（index.vue:768-780）。trace中没有对应导航关闭动作或后续主动pause业务动作；焦点策略切换与起停最贴近，但缺失事件记录，不能唯一指定分支。

具体原因边界：可能是焦点真实丢失、短暂blur使generation失效、页面活动/owner变化，或恢复竞争；不能说已经证明OS抢焦点，也不能说已证明数据库未累计。start响应尚未处理完即遭pause时，关闭旧会话是正确保护。现有lifecycle测试包含“pending pause期间resume应等旧closure并新start”，源码:65先等closing再处理starting，不能简单指控已覆盖的pause/resume路径必然丢恢复。

另有静态待证风险：start返回时visible=false但没有pause事件，使late-start分支自行stop；若stop在途时visible恢复并resume，则:66返回同一个starting，关闭完成后没有再次检查/启动。只有可复现该条件且最新意图仍为可见时，才构成产品恢复缺陷；R3 trace未证明该条件，不能等同本次根因。

最小建议：先将真实计时前置的焦点/页面可见性取证设计补齐，避免在start在途时重复更改CDP焦点策略；之后经授权以真实前台验证。这里是测试校准建议，不是本轮已修测试；不得模拟visible=true、强行保持前台、延长超时或修改15秒策略遮蔽失焦暂停。产品只在独立RED确认上述缺口后最小修复恢复意图，旧capability关闭、无后台计时等保护必须保留。

BDD/RED方案A（测试前置）：Given计时前置尚未证实，When出现start→stop且无心跳，Then保留FAIL并归类前置/生命周期待证，不能报后端累计故障。获准真实页面时须采集只读focus/blur、visibilitychange及hasFocus/visibilityState，至少覆盖启动返回与整个计时窗口；此轮不执行。

BDD/RED方案B（产品候选）：Given deferred start在visible=false时返回并进入deferred stop，When可见性恢复并调用resume且stop仍在途，Then旧会话停止后应开启一次新client generation并恢复10秒heartbeat；隐藏/已dispose不得重启。可在现有tests/unit/dcc-preview-reading-lifecycle.test.cjs扩展deferred stop控制，验证旧stop完成后start计数由1变2。按当前:66静态推导此候选应RED，但本轮未写/未运行，不称实际RED。既有pending-pause恢复用例要原样保留回归。即便候选RED，仍需真实事件证据才能将其认定为本次XLSX根因。

## 最小工作边界与验收计划

- DWG：先修正诊断分类与完整采证对象；未证明产品绘制错误，不建议产品改动；任何语义判据变更须Root评审并有真实变更负例，不能直接放宽断言。
- 计时：先归因服务端长耗时与客户端真实焦点，再决定产品修复。15秒和微秒边界保持；不添加补计、吞异常、自动成功或心跳重试掩盖未知。
- 未来RED/GREEN入口：前端现有node:test lifecycle文件、后端现有DccOnlineFilePreviewReadingSessionServiceTest，命令/新测试须另获Root授权；本轮仅方案，NOT_RUN。
- 本报告结构与证据路径已离线检查；产品/测试代码未改。本任务等待Root review，不自动重跑成功业务或XLSX剩余三次切换，不进入新轮次。Git收尾按当轮禁止保持blocked，不声称completed。

## 源码指纹核对
- IntRuoyiFronted/src/views/dcc/controlled-file/view/index.vue: SHA256 8fdb3ffad4844959fdef90b4d2735ef5906a5ce35579cfae3d582b2432194a9b; R3 snapshot match=True。
- IntRuoyiFronted/src/views/dcc/controlled-file/view/reading-lifecycle.ts: SHA256 9f42d217f465c991ec84c5f57c6bccc131d260ad2f3ecb7b47fb73bbc4b2f4b2; R3 snapshot match=True。
- IntRuoyiFronted/src/views/dcc/controlled-file/view/presentation.ts: SHA256 092948491b40bd8dc7cf34b85fab3924fb9105de3d50bcf9297cde3a68616dda; R3 snapshot match=True。
- IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/filepreview/DccOnlineFilePreviewReadingSessionService.java: 当前SHA256 cfb2b24daf42abee1ec83b9ee761ed1ff49b2700bbd8ad08e48fa84b64c1a0e8；运行Jar取证另见R3 runtime-postcheck.json，不把当前源码自动当成运行栈证据。
