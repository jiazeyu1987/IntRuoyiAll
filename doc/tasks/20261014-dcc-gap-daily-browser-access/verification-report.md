# G07 验证报告

## M09正式-am回执（覆盖下文“待M09”状态）

Root已真实结束唯一M09，并回执以下当前正式结果：

|G07相关范围|Root报告结果|
|---|---|
|DccControlledFileQueryServiceTest whole|272 PASS|
|DccControlledFileBodyReadCurrentSourceTest|23 PASS|
|DccControlledFileBodyReadContextTest|3 PASS|
|DccControlledFileDetailAuthorizationGuardTest|19 PASS|
|DccOnlyOfficePreviewTokenServiceTest|8 PASS|
|DccControlledFileBodyReadHttpContractTest|2 PASS|
|G09真实登记相关|正式PASS，具体计数/freshXML以Root票据为准|

上述是Root正式-am回执，本线程未自行运行Maven，不把以前private计数冒充正式执行。Root本次总368中仅ReferenceUsagePageDB2FAIL，故整体M09并非全通过。Root已正式接收G07原软件域并冻结G07生产/测试；后续不重复旧private组合。

Root A33另派G11修三固定关系/引用FE入口显式VERSION_HISTORY；当前G07公共context/viewer/source helper保护不改。此整体入口接入不能靠放宽DAILY状态修复，也尚未由G07取得新实际验收。Git收尾未授、工程未completed；Root继续统筹。

## 当前新四出口结果（优先于历史RED）

Root M08回收后，当前 `DccControlledFileBodyReadCurrentSourceTest` 新23执行/23PASS/0FAIL/0ERROR/0skip，exit0、sourcehashstable；命令phase=`four-exits-history-final`，其result/source-hashes保留在verification同名目录。当前source已被owner修复延迟状态metadata投影缺口，原普通/drawing/Office3条RED新用例GREEN，旧RED保留历史，不能重复列为未实现。

|出口|实际运行|新反向证据与限制|
|---|---|---|
|metadata|真实Query+Guard+actualprovider，source/type/rawversion、task/round、登记时间投影|每日稿拒绝；task改派/账号停用/HTTPignore零token和bytes；PREVIEW持久化端口mock|
|binary|真实Query+Guard+actualprovider，精确source/图纸配套PDF/保留正式件|账号停用/任务改派/ignore零storagebytes；viewer token HMAC是mockverify端口但expectedContext hash/version/event身份实际被捕获断言|
|Office callback|真实OfficeTokenService签发/验签+真实Query/Guard/provider|source引用改变/错fileID/event旧round/purpose/停用/改派/跨tenant拒绝；SecurityContext无伪造、scope清空；实际Office服务未运行|
|infra PREVIEW guard|真实DccBusinessFileAccessProvider.assertAllowed→真实Query|无context默认DAILY，现有task不代grant；正常ACTIVE先成功再停用/ignore/硬scope撤销拒绝；完整infra coordinator/裸storage归属路由未实测，不冒四完整HTTP出口验收|

额外合法历史：非requester的精确hist-task assignee读旧selected published body且file.currentRound已改变；改派后零bytes。Office TenantIgnore合法入口真实token签名tenant内重授权、恢复外scope；停用账号即使外层ignore也零bytes，不将ignore当grant。

预期异常类型fixture差异21中20PASS/1FAIL→21PASS、23中22PASS/1FAIL→23PASS均记录在各private phase；不称这些为新产品RED，不改生产异常合同。Query/QueryTest未被本轮修改，M09正式reactor由Root后续执行；旧270不代新source/入口证据。

## M04后新source验证状态（优先于历史报告）

实际当前结果：

- `.../run-private-tests.ps1 -Phase current-source-m05-r2 -TestClass DccControlledFileBodyReadCurrentSourceTest -MethodPrefix ''`：6执行/4PASS/2FAIL/0skip、exit1、source前后稳定。成功项包括实际OfficeTokenService签发/验签后callback原源正文成功、source引用变化在业务文件授权及storage前拒绝，以及event旧round/purpose拒绝；未冒用旧270。
- `.../run-private-tests.ps1 -Phase current-source-m05-office-red -TestClass DccControlledFileBodyReadCurrentSourceTest -MethodPrefix delayedOffice`：1执行/1FAIL，expectedOFFICE/actualPDF，source稳定；是延迟状态同一metadata投影缺口的Office正向路径证据，不单列成第二根因。
- 保留失败：新延迟普通source/图纸source的metadata实际使用业务fileName而非实际文件，Office实际source.docx被业务名selected.pdf误判PDF；`resolvePreviewReferenceId`正确但`expectsPreviewArtifact`不包含新状态导致nullprojection。必要修复在已移交Query范围，请Root协调当前owner；G07未写Query/QueryTest。
- private仅当前G07授权源重新编译至任务目录，依赖Root M05刚完成freshmain/testclasses与本地jar；业务库/真实BPM图与运行服务均未执行，不是Maven官方测试或运行验收。

M05回收后实际运行 `run-private-tests.ps1 -Phase current-source-m05 -TestClass DccControlledFileBodyReadCurrentSourceTest -MethodPrefix ''`：源码编译成功，6执行/2PASS/4FAIL/0skip，exit1、hashstable。失败实际为新延迟状态未进入expectsPreviewArtifact，metadata只有业务名/无真实source投影；Officeurl null为同缺口下游结果，初测试NPE不冒第二业务缺陷。Query源不归本线程，不改；精确差异已放review-ready顶部供Root当前owner接。

当前Query新增延迟确认source/Office实际infra护栏已由当前owner实现，G07独立test `DccControlledFileBodyReadCurrentSourceTest` 六场景已完成（6项不是旧270）。Root正在M05，私有编译尚未运行，无新PASS/RED；不得把依赖可能不完整当产品失败，源代码静态接入也不算行为GREEN。M04的主23reactor编译PASS不代表其四target业务执行，它们因旧Capturing testCompile阻断尚未执行。

测试主体为实际Query/实际Guard/实际OfficeTokenService。当前合法BPM/task/round来源使用明确mock只读端口，businessFileAccess端口递归调用真实Query.assertBusinessFileAccess复核同scope；没有业务库/Flowable真实图/签名审批写入，也不冒运行验收。待M05正式回收后以当前源重新编译到任务私有目录，记录sourcebefore/after，不读旧testclasses当目标通过。

## 2026-10-15 当前检查点（覆盖下面第一阶段历史结论）

实现：正规正文意图＋server身份/任务/历史/Master/培训scope；metadata、binary、Office签名payload与event同hash；日常必须ACTIVE且实际activatedTime；单独保留metadata授权与精确办理读。FE同形wire已接common/source/BPM/导航，detail/browser父页已归还；G01已接公共wrapper并由本线程实际wrapper端口测试。

|实际命令/范围|最终结果|证据限制|
|---|---|---|
|run-private-tests.ps1 -Phase source-red（Query当前生产+专属测试）|4 FAIL，均应拒绝却实际没抛；exit1|真实服务RED；只读target依赖，非Maven|
|run-private-tests.ps1 -Phase query-handoff-final -MethodPrefix ''|270 PASS / 0 FAIL / 0skip，exit0|当前授予Query/Guard/interface/context/Office/Actor/Iteration/Controller/VO源码编译至自有目录；其他邻域旧编译依赖，非reactor|
|同脚本 -Phase guard-handoff -TestClass DccControlledFileDetailAuthorizationGuardTest -MethodPrefix ''|19 PASS|启用tenant/actualactivatedAt/RequesterMaster/精确currenttask/history/独立training，旧metadata保持|
|同脚本 -Phase office-regression -TestClass DccOnlyOfficePreviewTokenServiceTest -MethodPrefix ''|8 PASS|真实签发验签context/参数改造/跨文件与purpose冲突、原token模式|
|同脚本 -Phase canonical-final-regression -TestClass DccControlledFileBodyReadContextTest -MethodPrefix ''|3 PASS|Java/TS同canonical字节、purpose46字符、重复键/非规范/类型/非法UTF8/actor；非法JSON有真实错误log|
|同脚本 -Phase http-final-green -TestClass DccControlledFileBodyReadHttpContractTest -MethodPrefix ''|2 PASS|MockMvc运行真实Controller现有metadata/binary资源，服务port为mock；不是HTTP服务/E2E|
|同脚本 -Phase iteration-red/iteration-green -MethodPrefix g07_revisionOptions|1 FAIL→1PASS|两份草稿真实query投影createTime/changeDescription/null、原rawid/排序不改|
|node --test tests/unit/dcc-g07-body-read-context.test.cjs|12 PASS|实际helper/common source/正规wrapper函数/BPM template/browser/detail/导航，纯端口开发测试|
|node --test tests/unit/dcc-g07-preview-registration.test.cjs|4 FAIL→4 PASS|actual viewer脚本；metadata等待/成功text/pdf/同URLoffice/真实media DOM回执/stale/error/local未记录|
|G07+BPM身份/通知+G08browser显示+readonlyBPM/trace|25 PASS|原精确选择/null-round与权限/导航回归，不是页面验收|
|G09 viewer/pdf/office（配置真实已装G09_CANVAS_MODULE）|20 PASS|含真正PDF.js文本/扫描件/源bytes，Office组件行为，不是远程服务UI|
|TypeScript createProgram helper+BPMidentity，strict ES2022|0 diagnostics|当前新增pure边界，未运行全类型|
|ESLint Node API 7个G07 FE授予文件 + C04 viewer最终复跑|errors0 / warnings0|原configured eslint，无自动fix/大构建|
|git diff --check 指定G07授权prod/frontend|exit0|只CRLF转换提示；不clean/回滚并行资产|

私有输出在 verification/private-<phase>/result.txt/source-hashes.txt，每次编译显式Java源与依赖位置见run-private-tests.ps1。Java参数文件绕过Windows命令长度上限，只用于开发工具；没有业务fallback。首次runner缺launcher失败后使用与engine版本一致已安装1.12.2 launcher，不混入不同JUnit版本。

Query首次全回归267中246PASS/21FAIL；正规context/login/精确Master/round/eventfixture迁移后261PASS/6FAIL，再补来源hash及tenant、旧2nodeprojection的taskId/signedAt/真实历史task后267PASS。两条node失败属于旧场景事实缺失，未改NodeRecordProjection或签名核验。新增Office callback/revoke与精确task服务行为，以及C04nulltime最终270PASS。不得把首次混合失败全部说成业务RED。

## 当前不能关闭的接入与审查

- Query已归还Root→G06，hash13797566779c6f562a5c335c60a2a8013eddcdbc；之后所有Query复修必须Root串行回授。detail/browser父页也停止写入。
- G01 PENDING_DELAYED_EFFECTIVE_CONFIRM 当前在Querysource选择无映射；需正式已审批件唯一source合同，不能用from/default current body替代。
- Office callback需Root后续复核并加 `token.infraFileId == resolveBinaryFileId(selectedFile, PREVIEW)` 明确实际source等值；本线程检查点后发现，已交integration-notes，未在已归还Query并写。
- Viewer已订阅error同URL清理，但OnlyOffice子组件当前未emit加载后error；需Root转G09最小事件；父页处理器测试不能冒充链路接线已完。
- 上一bullet已解除：G09实际子组件当前已emit同URL/currentgeneration error，G07生产父处理器组合测试实际PASS；最终前端定向53/53PASS，0skip（此前新增errorRED52中2FAIL如实保留于日志）。测试仍不是远程Office服务验收。
- C04成功metadata需要registeredAt，unavailable保留specificreason允许null；decoder归G01，本线程没有碰她共享文件。新flow/确认fields和完整reactor由Root统一验证。
- 最终FE定向命令包含G07两tests、readerBpm/trace/notification/obsolete/browser显示与G09viewer/pdf/office三tests，安装真实canvas路径沿G09报告设置；exit0/53PASS，见本turn实际输出。G09片段模板测试提示未提供registeredPreviewVisible的host字段（2 warning），生产SFC已正式声明且ESLint0/0，登记状态由G07专属行为测试覆盖；该fixture小差异留G09/Root复审，不改生产以迎合测试。
- Maven、全量类型/构建、服务/实库/E2E、Git收尾均未执行或未授权；不能标工程completed，也不宣称正式环境验收。

下面保留的是第一阶段未实施时的证据，不代表当前状态。

## 证据结论

当前可确认源码边界与接口接线缺口，不能确认修复 PASS。未修改生产实现。

|验证|结果|证据边界|
|---|---|---|
|git branch --show-current|int_qms|实际执行，只读|
|AGENTS、backend-development、database-rules、task-closeout-rules、requirements、coordination|已读取与适用门禁核对|Maven 必须 -am；本轮唯一验证票覆盖默认验证流程|
|普通正文调用链复核|工作稿 requester 放行；待生效全局拒绝；普通 Controller 无正规读上下文|静态事实，不是运行复现|
|G07 服务行为用例|已编写 2 个方法/4 个场景|当前直接调用生产 Query，不是文本测试；尚未执行|
|RED|NOT RUN|Root 当前发 G12 M01 票；本线程冻结，等待唯一 Maven 票|
|GREEN / REGRESSION|NOT RUN|依赖正式上下文与共享接线授权、RED 执行|
|文档结构核验|PASS|PowerShell 实际校验 6 个非空交付记录、task 必需标题与 in_progress 状态；git diff --check 自有记录无错误。不证明业务实现|
|运行态 / E2E|NOT RUN|本轮明确禁止|
|Git 收尾|NOT RUN|本轮明确禁止，不标 completed|

## 保留的强回归

日常与专门办理必须成对验证；旧工作稿源件/图纸成功测试只转到合法 AUTHORING context，保留精确文件 ID 与零错误文件读取断言。审批要验证当前 assignee 与精确 task/轮次；历史要验证选中版本不替换为现行；普通 token 不能换 context，Office callback 必须验签并重新核验同一上下文。所有拒绝先于正文存储读取与 token 签发。

## 生产未改核验

交付时重新 git hash-object，两份授权生产文件指纹与改前完全一致：QueryServiceImpl `53c8c11fa3ef7c183ccbbedeee15b8dda02ab7cb`；DetailAuthorizationGuard `277aefbe1a7089f4f7a2b3f92fdf9150aa409547`。
