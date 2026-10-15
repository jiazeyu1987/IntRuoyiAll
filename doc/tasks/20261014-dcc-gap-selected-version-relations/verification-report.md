# G11 A04/A07 最终定向验证报告（2026-10-15）

## M11最终正式验证与软件域接收（最新）
Root句柄39818实际exit0，23reactor BUILD SUCCESS；RevisionAttachments37+PageDB16+FormalAuthority13=66/66，0failure/error/skip，3066路径manifest零漂移。G11两集成类PageDB16、FormalAuthority13正式通过，已获Root软件域review接收，原失败保留历史并经本票复验消除。

本线程只读复核归档m11-junit：PageDB SHA7f1c3131b3fe55526f1bd0ba14b7eed68d0a448768cd8b373d1c1f2a4c4e98a2；FormalAuthority SHA d5fde8024ada4a96c1f78348e5c43b9bcb4bba505bc2037617ca0b8de726cec0，tests16/13、0F/E/skip与m11-verification.json一致，不使用旧报告或privatePASS冒正式验收。

G11生产/tests继续冻结，未重复已过套件。Root当前T04唯一完整ts:check noEmit，G11不并行Maven/types/build；此软件域接收不扩展为全项目最终freeze、E2E、发布或Git收尾completed。下文等待M11/未覆盖PageDB等为历史时点记录，以本节为准。

M11排期：Root已独占-am三类正式复验（RevisionAttachmentsDatabase整37、ReferenceUsagePage整16、FormalAuthorityIntegration）；G11生产/tests冻结且无并行构建/私有target依赖读取。此票尚在执行，正式PageDB/authority数目及freshXML等待Root回执；私有16保持独立证据，不能提前视为M11或全工程PASS。

## A33最新验证（覆盖后文旧checkpoint）

Root另已独立复跑下述四份FE原命令34/34、0skip、exit0，并接收三入口history source、metadata/binary同wire、关闭late保护；FE及相关tests已冻结。本线程参数runner整16结果也已完成交付（会话50286），未再次跑16/34/97或自发Maven。

RF11-01已实现：Panel/Stage/UsageDialog的目标VERSION_HISTORY/raw版/ID；实际SFC handler经过实际common/filePreview及当前workflow metadata/binary函数，元数据params与binary header同canonical wire，不借父申请BPM。模板正式绑定previewSource；关闭/切版/unmount清除，错版/待生效拒绝。

- RED：新增三handler测试先3FAIL；引用test loader补原生URLSearchParams后用原handler精确RED1FAIL（实际DAILY!=VERSION_HISTORY），避免把loader缺API算产品RED。GREEN同A33三项3/3PASS。
- `node --test tests/unit/dcc-selected-version-relations.test.cjs tests/unit/dcc-relation-stage-evidence.test.cjs tests/unit/dcc-reference-usage-dialog.test.cjs tests/unit/dcc-body-read-wrapper-g01.test.cjs` -> exit0，34/34PASS，0skip。
- `node --test --test-name-pattern="selected snapshot loads|public detail relations loads|name-authorized relations|readonly detail relations" tests/unit/dcc-current-relation-source.test.cjs tests/unit/dcc-detail-closure.test.cjs` -> exit0，5/5PASS；只更新这些SFC加载所需actual preview依赖，不削弱旧断言。
- 三FE生产文件eslint exit0；定向diff --check exit0。Stage旧strip import单行正则被Prettier多行暴露失败后改AST排除实际import，最终行为真实执行PASS，不计加载错误/skip为PASS。

M09 Root正式368项中PageDB两FAIL已取得其归档XML。A33私有运行当前Query/Guard/Reference/Controller源码及原PageDB，实际Spring unit-test H2，输出目录在本任务private-page-*，不写target：

```text
python -X utf8 doc/tasks/20261014-dcc-gap-selected-version-relations/run-private-page-review.py red
=> javac PASS，JUnit 2 tests、0PASS/2FAIL，完全复现原115/127两个断言
python -X utf8 doc/tasks/20261014-dcc-gap-selected-version-relations/run-private-page-review.py green
=> exit0，2/2PASS、0skip
python -X utf8 doc/tasks/20261014-dcc-gap-selected-version-relations/run-private-page-review.py regression
=> exit1，16tests、15PASS/1FAIL（唯一HTTP缺私有javac -parameters）
```

实际诊断并保持原断言：body正例此前仅目的项目2可读，源项目1没有正式读取规则；补该H2规则后仍先无PREVIEW则false、再授PREVIEW才true，明确无directory-management且不调用assertContentReadable。negative此前仍有合法OBSOLETE名称管理资格，撤销此独立grant并清QUERY后原name-denied/tenant/page断言通过；无Query/Guard生产改动，无默认角色/权限或删弱断言。

HTTP唯一失败是私有runner缺Maven已有-parameters；已补脚本，不改Controller/不删HTTP。M10独占期间暂停Java/target读取；Root明确M10结束释放后，按授权完整复验同一PageDB类，实际会话50286/exit0，16found/16started/16successful，0FAIL/ERROR/skip/abort，测试14.880秒。当前Query/Guard/Reference/Controller与原PageDB明确私有javac，真实Spring/H2，包含原两个权限失败及HTTP，无共享target写入。**最新PageDB私有整类16/16PASS**；Root最终正式-am验收仍待其统一票，工程未完成。指纹及范围见a33-checkpoint.json。

M05正式结论：Root句柄38484已结束exit0、23reactor BUILD SUCCESS，四类80tests/0F/E/skip。G11在本票正式执行ExactVersion10+Transaction19=29PASS；只读核对归档m05-junit两份XML及m05-verification.json摘要完全一致。Root2285源/drift6，票为mutable四类范围，不是全项冻结验收。旧M05暂停已解除/当前无活跃Maven，worker未自行启动下一票。

M05未涵盖UsagePageDatabaseTest与FormalAuthorityIntegrationTest，现存XML分别2026-10-07和10-12旧报告，不计fresh PASS；该正式Query/Controller/并发page集成继续交Root后续唯一票。M04原失败、私有29和本票正式29分开保留，不复用旧组合冒新业务或全工程PASS。

M04最终状态（Root回执）：句柄59548、exit1/60秒，23reactor current main编译PASS；testCompile因旧SignatureGovernancePolicyDccAdapterTest.Capturing缺新增interface抽象method阻塞，A28由G01修，四target未执行。当前无活跃Maven，旧M04暂停已解除；下一M05句柄/范围仍Root独占。worker未启动Maven/并写target，不因mainPASS称完整targetGREEN。

暂停解除后的本轮私有exact-reference/Transaction，current源明确私有javac、M04 fresh main依赖，实际29/29PASS（10+19）、0skip。旧testclasses仍不完整，只用于readonly依赖classpath，无正式Spring BaseDb/四target执行；不以本轮29或旧270组合替代M05新公共/来源护栏。

## Root review后摘要补强（最新证据）
Root指出getUsage缺page相同身份guard，坏master可被COUNT过滤伪成功。已按相同正式guard最小补强：getUsage在名称权限通过后、计数前执行store.assertUsageIdentity，Store本轮无改动。

精确真实H2：`run-private-reference-tests.py red summary-review` -> exit1，1/4PASS、3FAIL（wrong-master/缺目的项目/缺文件夹本应失败却成功）；`green summary-review` -> exit0，4/4PASS。另一version/另一tenant坏引用仍不污染本版；不可读目的项目缺失只报正式identity错误，无项目名/明细读取或正文授权。

相邻回归初次26/29：3项自有旧事务summary fixture缺正式文件表，真实SQL失败；补齐合法schema/版本/项目文件夹事实后，最终`regression` -> exit0，29/29PASS（ExactVersion10+Transaction19），0skip。当前目标源fresh私有javac/H2，未运行Maven/共享target/前端重复全测；下文25/25为此前阶段历史证据，最新为29/29。

当前实现仍待Root四类唯一Maven/全类型票与工程收尾，未称正式工程全完成。

## 当前结论
所选版本/申请关系隔离、验真History选择事件、精确所选版正式持久引用计数/明细、前端摘要身份/逐行版本校验及固定正文预览已实现。没有新增引用提交快照/字段/迁移。真实 RED/GREEN 和私有当前源回归通过，旧混合测试已真正接入依赖/迁移断言后执行通过；工程收尾仍待 Root 唯一Maven/全类型票、接线整体review和Git授权，不标completed。

全部为隔离开发测试：真实 SFC脚本/模板/父事件及 H2 SQL/事务行为；依赖读调用使用受控测试数据，权限公共依赖按既有测试mock。不是E2E、运行服务、真实库或部署验收。

## RED / GREEN（所有RED均在对应生产改动前）

|范围|RED|GREEN|
|---|---|---|
|DetailRelationsPanel|新专属10项 1PASS/9FAIL：历史读取请求当前A/2，模板无冻结X/1，未请求所选申请证据|同命令10/10PASS，后补null阻断/显示helper/异步边界，最终14项PASS|
|申请null未就绪|1FAIL，仍以版本history替代申请|1PASS，零读取|
|G08显示接入|1FAIL，真实模板泄露A/1-2|1PASS，草稿显示、raw保持|
|History selection-change|真实Vue父监听及模板0/6PASS，缺事件/clear/null和小版本显示|6/6PASS；正式NOT_RECORDED空态另先RED1FAIL再GREEN，最终7项PASS|
|引用model/摘要wrapper|6/8PASS：页内另一个selectedID未拒绝、master-only摘要未拒绝|8/8PASS|
|引用弹窗handler/模板|1/4PASS：handler变造目标、pending可开正文、小版本泄露|4/4PASS|
|BE current Store/Service实SQL|私有javac成功，实际java exit1，1/5PASS：A/1global预期2实际3（含A/2项目），后补/无可见scope同样串版，另一版坏引用阻塞所选版|同源fresh编译后5/5PASS，后续真实事务扩展25/25PASS|

各阶段完整命令、改前hash、失败事实与GREEN见execution-log.md，非旧报告复制。

## 最终前端回归

工作目录 IntRuoyiFronted：

```text
node --test tests/unit/dcc-selected-version-relations.test.cjs tests/unit/dcc-current-relation-source.test.cjs tests/unit/dcc-selected-application-context.test.cjs tests/unit/dcc-reference-usage.test.cjs tests/unit/dcc-reference-usage-dialog.test.cjs tests/unit/dcc-selected-version-reference-summary.test.cjs tests/unit/dcc-detail-closure.test.cjs tests/unit/dcc-browser-html-alignment.test.cjs tests/unit/dcc-relation-history-read-contract.test.cjs tests/unit/dcc-relation-stage-evidence.test.cjs tests/unit/dcc-relation-remediation-notification.test.cjs tests/unit/dcc-application-node-records.test.cjs tests/unit/dcc-version-display.test.cjs
```

实际exit0，97/97PASS，0FAIL/0skip。真实Vue模板检查冻结名字/编号/版、原上传/后补/整改/签名证据；真实父组件监听initial/切换/失败/卸载null和验真选择；错误文件/版/租户/master/project/申请轮次、晚异步及pending正文均覆盖。摘要loader/wrapper、页内所选版、Long、服务器count/page合同覆盖。版本helper不改raw请求。

之前dcc-detail-closure 5FAIL已在A04授权后修正：加载真实application-node-records/relation-contract/version-display模块并执行原实际组件行为；四项过时current-set断言迁移到所选快照/零写入/权限边界，未加skip或恢复废弃current tab。History格式化后的静态请求断言只放宽空白，真实请求参数仍锁定。

G10并行修改父页/浏览fixtures，G11未写这些文件；当前当轮browser回归执行通过，最终父页接线review归G10/Root，不用源码观察冒称端到端页面验收。

## 后端私有验证

根目录执行：

```text
python -X utf8 doc/tasks/20261014-dcc-gap-selected-version-relations/run-private-reference-tests.py red
python -X utf8 doc/tasks/20261014-dcc-gap-selected-version-relations/run-private-reference-tests.py green
python -X utf8 doc/tasks/20261014-dcc-gap-selected-version-relations/run-private-reference-tests.py regression
```

RED已在改前源执行，exit1，1PASS/4FAIL；GREEN exit0，5/5PASS；最终REGRESSION exit0，25/25PASS（ExactVersion6项+Transaction19项），无跳过。

runner javac编译当前Contracts/Store/ReferenceService/Controller及ExactVersion/Transaction/UsagePageDatabaseTest到本任务private-reference-*，从历史XML只发现并验证readonly依赖classpath路径，当前目标源码每次记录SHA并编译，绝不复用旧目标测试PASS。jdbc URL强制h2:mem，没有真实库连接，共享target零写入。

覆盖tenant/master/selectedID同时过滤，global/visible/distinct项目/page一致，另一版引用不入所选版、后补同版同项目目录不重复项目数、其他版坏引用不污染所选版、所选坏引用准确拒绝、受限目的项无名称泄露，summary Long JSON，创建/取消/原版重放/不同版返回count独立，并发及审计晚失败事务恢复原记录。

限制：Query和公共依赖来自readonly既有classes，Spring BaseDb DccReferenceUsagePageDatabaseTest只fresh编译未执行。真实Query/Controller权限/并发read快照须Root正式reactor票执行，不能称Maven集成PASS。票建议见maven-validation-request.md。

## 静态/文档门禁
- pnpm exec eslint（7个授权FE生产文件：Panel、relation-contract、History、browse-records、relations wrapper、reference-usage、UsageDialog）-> exit0，无诊断。
- 定向git diff --check（仅本轮授权生产及已跟踪测试）-> exit0，无空白错误；普通LF/CRLF提示无失败。
- Prettier仅授权前端资产；改前/改后SHA在execution-log与changed-files。
- 任务记录UTF-8/必需结构、cleanup preview/apply实际状态见execution-log；private输出只能删任务目录内产物，保留可复跑Python脚本和正式报告。
- cleanup preview在ready_for_closeout -> exit0、无warnings；Git未授权转blocked后apply -> exit1/BLOCKED。清理3份已核对任务私有编译目录的命令遭自动审批Rejected/blocked by policy，未执行、产物保留，未绕过或冒称清理完成。

## 未执行
Maven、全量vue-tsc/大构建未获唯一票；E2E、服务启动/重启、远程、发布、真实业务库写入和Git提交推送本轮禁止。未将隔离测试或源码观察当这些门禁PASS。
