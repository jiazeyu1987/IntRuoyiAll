# G07 当前检查点供 Root 审查

## M09正式结果与冻结（Root回执，当前优先）

Root已通报M09真实结束，G07 Query whole272、新BodyReadCurrentSource23、BodyContext3、Guard19、OfficeToken8、BodyReadHTTP2及G09登记相关正式PASS。原先私有23/旧270检查点不再作为正式验证的替代；正式执行/源漂移/freshXML由Root保管，本线程没有运行该Maven票。

M09总体368中仅ReferenceUsagePageDB2FAIL，属于Root当前分派引用关系域，不能将总体368说成全部通过。Root正式接收G07原软件域，并要求保持冻结：本线程停止任何G07生产/测试写入，不重复私有旧套件。

新增整体接入事项：G11发现三处固定关系/引用FE入口漏VERSION_HISTORY，导致合法历史正文走DAILY后被拒绝；Root A33已授G11修3FE。公共context/viewer/source helper继续保护，若需要公共hunk由Root明确转交，G07不自行接线或放松后端DAILY状态门禁。该入口修复及整体联合关闭由Root继续统筹，不因G07原软件域通过自动标completed。

下文M09待票/私有结果均为保留历史，以本节正式回执与冻结指令为准。

## 最新新护栏 GREEN（M08回收后，供M09正式接入）

`DccControlledFileBodyReadCurrentSourceTest` 当前23/23PASS、0skip、exit0，命令 `& doc/tasks/20261014-dcc-gap-daily-browser-access/run-private-tests.ps1 -Phase four-exits-history-final -TestClass DccControlledFileBodyReadCurrentSourceTest -MethodPrefix ''`。当前授权生产源码重新编译至任务私有目录，sourcebefore/after稳定；不是旧270/19/8/3相加，不是正式-am结果。fresh结果与SHA256 manifest在 verification/private-four-exits-history-final/。

当前读取Query blob7fa123ccbc5778df650c3b424da614767f5e46b0，新增测试blob8e6a8dfeb3a79615ed776733bd730f13e679b02f。本线程没有写已移交Query/QueryTest。R06 expectsPreviewArtifact/missingreason已被当前owner补齐，普通source、当前图纸PDF和Word实际OFFICE/真实签名URL新用例均已GREEN；旧缺口现为已修并私有复验，不再要求重复派单。

四出口新证据：

- 实际Query metadata/binary读精确所选source，实际BodyGuard核验当前账号/任务；每日普通接口拒绝新延迟稿。预先合法成功后账号停用、task改派、HTTP TenantIgnore均拒绝，零新viewer签发/正文storage调用。
- 实际OfficeTokenService真实签发/验签＋实际Query callback：原source成功，签发后sourceId改变、另一fileId、event旧round或purpose、当前停用/改派/外部跨tenant均在正文读取前拒绝；只读actorscope最终清空，无伪造SecurityContext。
- 实际DccBusinessFileAccessProvider.assertAllowed→实际Query.assertBusinessFileAccess默认DAILY，无现存taskcontext也不可借任务看延迟稿；ACTIVE合法scope能授权，账号停用/HTTPignore/硬scope撤销拒绝。该provider guard不提供bytes。
- 实际历史task assignee可读所选SUPERSEDED published body，保留旧round/raw版，文件当前process已不同仍不换版；hist assignee变更后零bytes。

精准弱边界：infra BusinessFileAccessService协调器、其存储/Mapper归属发现、HTTP viewer token HMAC、持久PREVIEW事件写入、真实BPM图/历史查询均是明确mock端口；其中gateway mock转发实际DCCprovider守卫，不是返回成功绕过。Office签名HMAC真实执行；HTTP viewer token只校验传入expected context端口。MockMvcController两资源早期2PASS只证明transport，不宣称Controller→真实infra完整HTTP链当前合并通过。M09应纳入当前23类及原相关正式类。

TenantIgnore边界：HTTPmetadata/binary/directprovider不能凭ignore读正文，零bytes；OnlyOffice官方callback本身TenantIgnore入口，合法token必须先真实验签再在token租户内忽略关闭重授权，完成后恢复外层tenant/ignore。该合法callback成功与停用后零bytes均被测试，不能把未验签ignorecontext当授权。

两次新fixture异常类型修正如实保留：21首轮20PASS/1FAIL是跨tenant验签真实抛IllegalStateException而非期望ServiceException；随后23首轮22PASS/1FAIL是跨file canonical真实抛IllegalArgumentException而非ServiceException。两者生产已拒绝/无bytes，均改测试为精确类型+message，不改生产或吞异常。

Root已通报T03全类型exit0、M08新签名55PASS，G07不复跑这些结果或将其当本23项业务通过；M09正文/出口正式reactor仍待Root唯一票。下文为历史RED与检查点，不覆盖本节。

## 新实际 RED：延迟确认 metadata 未投影真实正文（M05后）

二次 `current-source-m05-r2` 当前源实际6执行/4PASS/2FAIL，0skip、源stable。Office两项已从已有真实公共矩阵任务入口成功到达：实际签名callback原源成功、源引用改变在业务/storage前拒绝；旧event轮次/purpose重放拒绝。剩余两项完全保留新延迟source与drawingPDF的真实metadata失败。另加延迟Office自身actualkind/源文件名/真实签名url用例，以明确该同一投影缺口的Office表现，未改断言或生产代码绕过。

Root：新独立current-source测试已私有重编译当前生产源码执行，6项首次2PASS/4FAIL，源hash稳定、exit1。两个V6延迟metadata用例返回 fileName `selected.pdf`（文件业务名），不是实际source/当前图纸PDF；两个Office同V6用例拿不到OnlyOfficeDocumentUrl，原测试NPE只是上述投影缺口的下游表现，不能算独立四缺陷。

当前Query `resolvePreviewReferenceId` 已支持新状态，但 `resolvePreviewArtifactProjection` 首先调用 `expectsPreviewArtifact`，后者未列 `PENDING_DELAYED_EFFECTIVE_CONFIRM`，直接回 `(null,null)`。故合法task尽管正文引用可解析，metadata仍无真实文件/contentType，source为Office时无法走Office签发。

必要最小修复锚点：Query#expectsPreviewArtifact 加新状态（仍受外层正规context/task守卫），#buildPreviewArtifactMissingReason 给同状态明确所选审批源件缺失；不能修改全局版本状态/DAILY规则或回退到其它artifact。Query当前整文件G06持有，本线程未写，Root请串行转当前owner接此真实RED差异。

新Office source等值测试将改用真实已有PENDING_MATRIX_REVIEW任务入口独立证明同一新Office护栏，避免V6投影失败阻断它的正向到达；V6两个失败用例保留，不降低断言。

## Current Status

in_progress — 正文合同及主要读取链路已实施，私有软件证据已交；剩余接入/正式统一验证不能冒称全项完成。

## M04后新source护栏验证准备（非旧270重跑）

Root通报M04主23reactor编译已通过、testCompile旧Capturing method阻断（G01 A28），四target未执行。G07已立即继续现有范围：读取当前Query，发现Root/G06已正式接入延迟确认source与Officeactualinfra等值护栏（当前blobc80c8dbb2d176613ee404e275b68b989b112a710）。新增唯一专属 `DccControlledFileBodyReadCurrentSourceTest.java`，运行真实Query+Guard+实际Office签名token，6个场景覆盖延迟精确task成功、daily/改派拒绝、缺源零fallback、当前drawingPDF、Office当前源成功→引用改变拒绝、旧event轮次/purpose拒绝。未修改共享Query/QueryTest。

Root现持唯一M05，故新测试暂未编译/运行；不读正在变动的targetclasses当产品RED。待Root回收M05后privatecompile当前源，并以新计数/hash单列，旧270不作为新护栏PASS。

## 文件已正式归还

- QueryServiceImpl：13797566779c6f562a5c335c60a2a8013eddcdbc。已被 Root A20 接收并转 G06；本线程 Query 源/QueryTest 均冻结，不再写。C04正文/附件registeredAt与issuedAt-null guard已包含，270/270 privatePASS、0skip、源hash稳定。
- browser/index.vue：ff0c620b539f347cdb5222c2e2be355580710003，已转G06；只日常DAILY/精确专门工作稿AUTHORING source接线，保留G08显示/选择/rawID/检入。
- detail/index.vue：2faf14ca332a3efc0dc600b1f70281a19fe28461，已转G01；两个正文source及任务/历史/编制/培训正规上下文，保留G10最小正文/只读详情/null-roundselection和G08显示。后续owner代码不归本线程成果。

## 当前 G07 实现与实证

- 唯一Java/TS严格正文意图联合；服务端启用租户主体、Master/Requester/精确task/历史参与人/培训及正文scope；DAILY必须ACTIVE且实际激活。metadata授权独立，不全局封死合法办理。
- metadata/binary/event/token同canonical hash；Office token签名携带context，callback从已验签payload恢复只读actor并重新查当前授权，不伪造登录主体。
- current wrapper经G01接入，G07实际生产wrapper函数测试证明same metadata params/binary headers和下发互斥；未新增endpoint。
- Query270PASS；Guard19PASS；OfficeToken8PASS；canonical3PASS；真实MockMvcController现有两资源2PASS。全部private开发测试，只读旧邻域compiled/dependency，当前授权源重新编译至任务目录；没有Maven/target写入/运行验收。
- FE专属正文合同12PASS，C04成功显示/失败clear/媒体DOM回执/Office同URLopened/error/local未记录4PASS；最终完整定向53/53PASS、0skip。包含实际G09子组件服务error→当前父viewer处理器组合测试，旧URL/旧generation/卸载错误均拒绝。先前52中50PASS/2FAIL明确是G09新增error的RED，当前已真实复跑GREEN。
- strict新增helper+BPMidentity类型0diagnostics，G07授予7file lint errors0/warnings0；正式全类型T01由Root执行，已告知4errors在其它owner，目前无完整类型工程PASS。

## C04 Viewer 接入与子组件依赖

view/index.vue当前G07 hash：ef2feb912bead592544050f530a32cb977aadebe。登记时间只来自正式metadata，画面未成功/失败/切版/卸载无时间；本地blob成功为未记录，不用watermark/clientNow。图片/音视频真实load按generation/src验证，PDF/text成功后显示，Office仅sameURL opened。

父viewer已订阅error({documentUrl,message})，当前URL复核后clear并显示原服务错误；G09当前实际子组件已emit并独立复核currentURL/generation，G07最终组合复测PASS。未写她组件，也未运行远程Office服务UI/E2E。

## 尚需 Root 串行处理的精确差异

1. G01新PENDING_DELAYED_EFFECTIVE_CONFIRM正文唯一source及4projection合同：Query当前无此状态source映射。Root已转G01正式hunk并将给一次接入；G07不并写G06持有Query。
2. Office callback实际source等值：建议Query取File/evidence后明确require token.infraFileId == resolveBinaryFileId(file,PREVIEW)，拒绝签发后源件引用变化。此review差异发现于Query归还后，已写integration-notes，待Root串行回授，不隐瞒为全项安全闭环。
3. C04 successRequired/unavailableNullable decoder由G01接，保留既有不可用具体原因；Root统一reactor/完整type/四出口复核。没有真实E2E/服务/业务库或Git授权。

完整实际命令、RED混合失败分类、fixture迁移依据、已实现/待接入边界见 verification-report.md / execution-log.md / changed-files.md。
