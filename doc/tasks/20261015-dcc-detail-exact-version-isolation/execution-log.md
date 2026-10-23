# Execution Log

## Baseline

- Branch: int_qms
- 用户指定源码基线：a3b8234ec；复核时实际 HEAD 为 f06833f06，其直接父提交为 a3b8234ec，新增部分为已验证本地交付收尾文档。未切分支或重置 HEAD。
- Existing dirty/untracked files were preserved as parallel baseline and will not be reverted.

## RED

- 已读 AGENTS.md、task-closeout、frontend/backend-development、PowerShell UTF-8、DCC 浏览流程、版本 ADR、关系/版本分工及申请轮次正式映射规则。
- 已复核 G10 `20261014-dcc-gap-content-detail-split` 与 G11 `20261014-dcc-gap-selected-version-relations`。沿用其所选版本固定关联/精确引用事实，不复制旧 PASS 冒充本次验收；本任务补的是正文 wire 到详情/申请面板上下文缺口、精确详情服务端授权及刷新竞态，非重复建档。
- 首轮既有 FE 五套件 67 项：54 PASS/13 FAIL；失败先出在测试 harness 漏载已集成 detailBodyPreviewState/version-display，修正为加载实际依赖，未调整生产代码迎合旧断言。其后附件 manifest fixture 同步正式空清单与服务端摘要后，原 integration 14/14 PASS。
- RED: `node --test tests/unit/dcc-detail-exact-read-context.test.cjs tests/unit/dcc-detail-route-read.test.cjs` -> 3 PASS/4 FAIL：新解析入口缺失，实际 reload 失败后仍保留旧 fileDetail。
- RED: `mvn -q -pl yudao-module-dcc -am "-Dtest=DccSelectedDetailReadServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL：缺少精确详情读取服务，测试编译失败。

## GREEN

- 新增 `detail-read-context.ts`，解析 canonical bodyReadContext 和普通申请/任务路由；不允许 wire 与明文轮次/任务/类型冲突，不允许 orphan task 或损失精度的 Long ID。
- detail/index.vue 详情 GET 传同一来源上下文；编码历史轮次也交到申请历史/关系父入口，原始版本与申请类型双核验。刷新清空旧详情、签名/打印/待办状态；切路由/账号/租户与卸载使旧请求失效；打印迟到响应也受 sequence/ID/path 限制。待办归属用精确 Long 字符串。
- `ControlledFileBrowseRecords.vue` 按 wire 锁定历史申请，签名区按该轮过滤；当前版本参考独立分区，所选版本引用数量明确标注。`DetailRelationsPanel.vue` 明示历史冻结范围，保持不读 current set 的 G11 契约。
- 新增 `DccSelectedDetailReadService`，Controller 同一详情资源按精确 wire 或正式 BPM 映射读取。服务端在返回正文相关详情、附件清单及其他投影之前调用真实 intent/file/version/task 守卫；拒绝错租户/版本/申请/任务、重复映射或响应换版。显式申请的签名摘要按 BPM 过滤。
- 定向 FE GREEN/REGRESSION 见下；私有 fresh javac/JUnit 最终 43/43 PASS，包含新增服务与真实 HTTP 契约、当前真实授权守卫和既有正文/申请 HTTP 测试。

## REGRESSION

- FE 主回归 13 文件 106/106 PASS，0 skip：详情来源/路由刷新/历史事件/关系冻结/引用/附件/正文 canonical metadata+binary。
- 补充 FE 9 文件 50/50 PASS，0 skip：含新增详情 wrapper、打印迟到响应、既有 closure/evidence/loader/附件下载/版本显示；与主回归存在重叠，不累加为独立用例总数。
- `pnpm exec eslint` 六个改动前端生产文件 -> exit 0，0 error；detail/index.vue 两条既有 first-attribute-linebreak warning，未整页格式化。
- `git diff --check -- <本任务已跟踪文件>` -> exit 0。
- 基线 Maven 五类 40/40 PASS（HistoryGuard 5、RoundMapping 3、VersionBinding 2、DetailAuthorization 19、RelatedFile 11），不作为新实现 Maven GREEN。
- 新实现 Maven `DccSelectedDetailReadServiceTest,DccControlledFileDetailAuthorizationGuardTest,DccApplicationReadHttpContractTest` reactor 测试编译 BLOCKED：并行新增 `DccOnlineFilePreviewReadingSessionServiceTest` 引用尚缺的 filepreview VO、session DO/Mapper/Service。未修改该并行资产，也未宣称 reactor PASS。
- `python -X utf8 doc/tasks/20261015-dcc-detail-exact-version-isolation/run-targeted-junit.py` -> exit 0，43/43 PASS，0 skip/abort。脚本使用本次 Maven 报告提供的依赖 classpath，fresh 编译当前服务、Controller、真实 Guard 和六个测试类到任务自有 private-junit。此证据仅为定向 fresh 编译/执行，不代表整个 reactor/运行中服务已加载。
- 未执行 E2E、业务数据库写入、服务启动/重启、远程操作、Git 提交或推送；Maven 既有 RoundMapping 使用隔离 H2 单测数据，不访问业务库。

## Final Verification And Closeout

- 最后将主回归与补充回归合并为 20 个去重测试文件，当前源码一次执行 `node --test` -> exit 0，149/149 PASS，0 fail/skip/abort。最终命令为 verification-report.md 两个前端命令的文件并集；最终结果取代中间 106/50 分组计数。
- 后端最后一次私有 fresh JUnit 43/43 PASS，包含移动后的 Controller 注入位置；日志中的坏 JSON/缺 BPM 参数为负向测试预期拒绝，不代表真实运行态日志。
- 本任务 tracked diff --check exit 0；两份保留验证脚本未命中 ignore 规则（独立 check-ignore exit 1/无输出）。
- 已再次读取 task-closeout-rules，状态先设 ready_for_closeout。
- `python -X utf8 C:/Users/D01020/.codex/skills/task-closeout-cleanup/scripts/task_closeout.py --task-id 20261015-dcc-detail-exact-version-isolation --mode preview` -> exit 0；keep 为本任务三报告与两验证脚本；delete 只包含本任务 private-junit 下的 class/args，warnings 无条目，未触碰任何其他任务。
- 用户禁止提交/推送优先于项目提交门禁，因此 preview 后转 blocked，cleanup apply 应按 blocked 状态拒绝，结果另记。
- cleanup apply -> exit 1，输出 `BLOCKED task status is blocked; expected ready_for_closeout or completed`，未执行删除；任务自有 private-junit 临时编译产物仍在，未用其他删除手段绕过状态门禁。
- 最终三报告 UTF-8/章节/状态及 Cleanup Keep 路径核验通过；本任务新源码与测试均可在 Git untracked/modified 清单中识别。

## 2026-10-15 续办与当前结果

- 用户/Root 续派沿用本任务目录，并提供实际 Maven 七类 115/115（SelectedDetailService7 + HTTP3 在内）。旧 reading 缺依赖只保留历史失败，不再作为当前 blocker；本轮未重跑旧 43/149、Maven、shared target、私有 javac/JUnit、fulltypes 或 build。
- 复核 actual reading API/service/viewer 与详情上下文：controlled正文沿用 canonical wire；API closure 捕获证据与header；service按event purpose/版本/token/session/context独立核验。DCC附件、分发、local blob不冒充此普通正文阅读来源。固定关系目标的 VERSION_HISTORY 不借父BPM。
- 新缺口 R01/R02：getControlledFile wrapper 混用wire/plain时静默丢弃明文；null bodyReadContext走普通metadata。已先写BDD与新Node文件。首次fixture default-export错误单独修正后，真实RED命令 `node --test tests/unit/dcc-detail-reading-context-boundary.test.cjs` -> 1PASS/2FAIL，均为 Missing expected rejection。
- GREEN：精确详情wrapper拒绝混用（相同值也拒绝）、显式null/非法body context、孤立task/type、非法BPM/Long；route helper与服务器混用拒绝一致；loader只提交canonical或plain一类身份。
- 新Node初次GREEN3/3；追加同一Long文件切换轮次的实际loader late-response断言、reading API header捕获后ctx改写仍不变，最终4/4PASS，0skip。没有重复旧套件。
- 本轮定向eslint三个FE改动文件exit0/error0；index.vue仍为第2/561行两条旧属性换行warning。
- R04 reading权限review：controlled reading Controller query菜单门禁可能挡住只有review/approve的合法审批人。Root已接受并正式转交阅读owner，仅受控四路改isAuthenticated、保留真实Guard、统一file-preview原权限不变。本线程未写该跨区源码，复核记录不冒称已验证修复。
- Root追加F01授权：精确DAILY ACTIVE详情返回同Master本人WORKING的PREVIEW。已新增DccSelectedDailyDetailProjectionTest真实reader/Query/history/action/Guard用例，所有数据/权限端口为mock，无DB操作。
- 最新Root允许先保存最小hunk再写GREEN候选：f01-red-green-replay.md已在生产修改前保存真实两参reader旧调用与三参新调用。仅恢复这一行即可回放行为RED，不重写整份Query或reading hunk。
- F01 GREEN候选：Query interface/Impl新增三参context入口，context+Guard明确授权后调用未改动generic两参投影，仅DAILY复用restrictDailyBodyProjection；没有根context ThreadLocal历史遍历。SelectedDetailService将context转交，七项旧服务用例stub/verify同步三参。
- 后端测试尚未运行，不能标RED/GREEN PASS；Root恢复点/方法/命令详f01-red-green-replay.md。仅做fixture字段锚点结构检查（PASS），不等于编译或JUnit执行。
- Root要求尽快给Java ready检查点后，已创建review-handoff.json，冻结Query interface/Impl精确入口、SelectedDetailService及后端测试。Root可只恢复reader三参→两参一行运行F01指定方法RED，再恢复三参执行新类6项及邻域；不需要回滚共享完整文件。
- Java冻结时点指纹已写review-handoff.json；后续文档/前端指纹补充不会重新写Java。Maven结果仍待Root回执，不能把源码结构检查或Root旧115套件冒称本轮新后端通过。

## M01 Root 统一验证进行中

- Root 已明确确认读取 review-handoff.json 并接收 Java 冻结；正在唯一 M01 中临时恢复 Reader 一行、ReadingService gap 两行及 ReadingController 四个 controlled 注解执行真实 RED，随后恢复精确原字节 GREEN。
- 本线程仅更新 task.md / execution-log.md / verification-report.md；没有更改 Java、接口、测试或前端源码，没有运行 Maven/shared target 或重复 Node。
- 前端新增边界4/4PASS与最终指纹已交还，不存在待补前端用例。review-handoff.json / handoff-fingerprints.json 保持原 GREEN 冻结快照，不将 Root 临时 RED 写成新交付指纹。
- M01 尚未提供执行结果；本记录只证明 Root 已开始验证，不标 RED/GREEN PASS。

## 全部源码冻结交还

- Root 确认 M01 后端已启动，Reader 暂处规定 RED 一行；本线程未读取或改写该临时源码，不重新采集 Java 指纹。
- 前端源码与测试也全部冻结：workflow.ts 既有详情区域、detail-read-context.ts、detail/index.vue、BrowseRecords、History、RelationsPanel 及相关 tests 均不再编辑。新增边界4/4PASS和 handoff-fingerprints.json 原 GREEN 指纹继续有效。
- Root 已 review loadData 的 wire/plain 互斥传输与实际 wrapper 组合测试，并提供 viewer 组82/82稳定通过回执；此结果属于 Root 的独立检查点，不加到本线程4项用例。
- 详情/上下文相邻组及一次完整 ts:check 由 Root 下一步执行，本线程不启动、不重复。仅更新本任务文档，等待正式结果。

## M01 真实 RED 与前端邻域回执

- Root 实际执行 `DccSelectedDailyDetailProjectionTest#explicitDailyDetailClosesSameMasterRequesterWorkingPreview`：前置原 generic 的真实 WORKING canPreview/PREVIEW 授予断言通过，所选 DAILY 返回 WORKING canPreview=true，指定 assertFalse 实际 FAIL。F01 已由源码推导转为真实 Query/Guard 行为 RED，非 fixture 或编译失败。
- Root 已按原始字节 SHA 恢复三个冻结生产文件为 GREEN；唯一十类 Maven 正在执行，尚未提供 GREEN 结果。本线程保持 Java/接口/测试/前端冻结，不触碰恢复后的源码，不重采交还指纹。
- Root 独立前端 wrapper/详情/附件/关系十二文件99/99PASS，六输入0drift；完整 ts:check 仍进行中。此为 Root 新回执，不累加到本线程4/4或历史149/43。
- 本轮仅修改自有四份文档；未执行任何源码改动、测试或共享 target 操作。

## M01 exit0 与类型合同修正待回执

- Root回执：完整十类Maven已exit0，正在核验freshXML。本线程未重跑，不在XML核验前填写最终总用例数或跳过数。
- Root一次完整ts:check唯一错误是BrowseRecords:269读取signatureSummary.processInstanceId但FE ControlledFileSignatureSummaryVO未声明。Root已核对后端SummaryRespVO确有String processInstanceId且Query实际映射，正式接管仅补 `processInstanceId?: string | null` 并复跑完整types。
- 该修正是已有后端合同的类型补齐，不改变精确详情运行行为；workflow.ts及所有源码/测试继续由本线程停写。原handoff whole-file指纹保留为Root类型修正前检查点，不覆盖重采，也不把已授权的接口字段补齐报作意外漂移。
- 等待Root的freshXML与types最终回执，本轮只补三报告。

## M01 正式125回执与软件关闭

- Root正式回执：-am定向十类Maven exit0，freshXML125/125PASS、0F/E/skip。分项ReadingSession29、ReadingController12、SelectedDaily6、SelectedDetailService7、SelectedDetailHTTP3、DailyBrowser11、BodyReadCurrent27、DetailGuard19、BodyContext3、OfficeToken8。
- 已只读核对 `doc/tasks/20261015-dcc-six-thread-followup-review/backend-green-summary.json` 十类计数与零F/E/skip；真实RED证据在同目录backend-red-summary.json，整体RED11中3断言失败+4实际旧权限拒绝已保存。三生产文件由Root按原始字节SHA恢复一致0mismatch。
- Root前端viewer82与详情99各自独立PASS/0drift；不与本线程4项或历史115/149/43混加。只读到fe-types-green-result.json已exit0、sourceDrift=[]、inputs=2429；正式类型最终回执由Root统一归档，本线程不执行types。
- 当前R01/R02/F01软件验证均关闭，不再保留待Maven或reading缺依赖阻塞。源/接口/测试继续冻结；仅维护自有文档。
- 工程状态因本轮未授权Git提交/推送保持blocked；运行服务/MySQL迁移/真实业务库/E2E/远端/Git仍未验，不把软件测试当上线验收。

## Root 最终软件验证回执（当前结论）

- M01 -am十类Maven exit0，freshXML125/125PASS、0F/E/skip；3个临时恢复生产源均匹配原GREEN字节指纹。F01/真实Guard/non-DAILY保留等后端待验已关闭。
- Root Viewer12文件82 + Detail12文件99 共181PASS，各组0fail/skip、0drift；仅本次两组汇总，不混加旧115/149/43或本线程4项。
- Root完整 `pnpm run ts:check` exit0，2429源码输入0漂移。唯一原错误是后端已有processInstanceId漏声明；Root只补FE可选string|null，且新旧TypeScript emitted runtime已验证字节相同，因此未重复业务套件。本次类型待验已关闭。
- Root当前10生产lint exit0/0error，detail两条旧换行warning保留。证据统一引用doc/tasks/20261015-dcc-six-thread-followup-review；当前R01/R02/F01软件验证全部关闭，不再保留待Maven/缺依赖/类型待验。
- 本线程仅改自有文档；所有生产/tests继续冻结，不重跑，不重新采集Root临时状态或覆盖原交还JSON。Root正在最终指纹与清理，本线程不参与并行清理。
- 工程Git提交推送本轮未授权，task按规则blocked；运行/MySQL/E2E/Git仍为明确未验边界，不作为新的软件阻塞。

## 新一轮边界 S01..S04（不同于上轮已关闭项）

- int_qms/f06833f06核对；Root29源码无漂移为其基线回执，上轮125/181/types不重记为本次执行。仅先读源，Root2635输入构建冻结期间只写自有BDD/新tests。
- 新Node真实RED初始4项3FAIL/1PASS；最终7项6FAIL/1PASS：详情成功/失败/签名晚回包、History与Relations旧tenant发布、wire→BPM回跳错native；真实actor同步watch相邻控制PASS。证据/诊断见authority-boundary-followup.md。
- Root接受前三项租户RED并解除生产构建冻结，授权本线程精确详情最小GREEN；相邻History/Relations同类问题亦在本线程归属。未修改附件组件、viewer/reading、Query或其他owner测试宿主。
- 本轮GREEN：index公共回包守卫捕获actor/baseTenant/visitTenant并实时比较，签名分页独立快照；父级readAuthority透传到History/Relations/BrowseRecords，各自成功/失败/preview/摘要回包前重新读取。BPM回跳复用严格wire解析，保留old-obsolete-round/old-task，不借native-upload-round。
- 新Node最终8/8PASS；受影响7文件58/58PASS/0skip（含专属8，不累加）。四生产Vue定向eslint exit0/error0，仅旧2条换行warning。own route/partial-template tests补载实际新getter，未改原断言或其他owner宿主。
- Browse引用摘要原plain fixture未形成真实computed缓存，改用Vue reactive props/route后实际RED旧tenant计数9，再GREEN；重复VM声明装配错误已单独修正不作产品RED。
- R05 independent DccReferenceUsageDialog只读发现同类租户缓存/迟到page+preview风险，未改跨owner源码/测试，未实跑其RED；已写authority-boundary-followup.md给Root分配。
- Root已完成上一轮2635输入构建并解除冻结，本轮4Vue已新改不能据此冒称构建通过；本线程不运行Maven/fulltypes/build。最终authority-handoff.json记录本轮源/测试指纹后冻结交还。

## R05 owner接收与实际 RED

- Root唯一转交DccReferenceUsageDialog.vue及新增专属测试；沿用本目录，四详情Vue继续冻结。不改browser/reading/附件/Java/Query。
- 先写R05-A/B BDD，再新增真实SFC load/preview测试；`node --test tests/unit/dcc-reference-usage-authority.test.cjs` -> exit1，13项1PASS/12FAIL、0skip。12项为actor/baseTenant/visitTenant变化后的旧page/preview成功和失败回包实际串入当前状态或窗口。
- 身份不变的精确大Long/raw版canonical history wire、真实权限拒绝控制PASS。没有装配/缺依赖失败。GREEN采用Dialog独立读取auth/userStore的同构实时身份key，不新增父级透传或重定位到当前版。
- R05最小GREEN：Dialog新增readReferenceAuthority，分别在load与preview请求捕获actor/baseTenant/visitTenant；所有成功/失败/finally发布点实时比较，同时保留原generation/sequence/open状态检查。
- 首次GREEN新增13+旧Dialog5=18/18；追加当前真实网络失败可见与损失精度Long请求前拒绝后，最终两文件20/20PASS、0fail/skip。旧Dialog test只补auth/userStore同身份依赖，不改原断言。命令见verification-report.md；未重复58全集。
- Dialog定向eslint exit0、0error/0warning。只读核对authority-handoff.json：四个已冻结详情Vue指纹全相等，无父入口/browser接线改动；新Dialog独立当前身份守卫同时覆盖两个既有父入口。
- R05不再待owner/静态未验证，当前实际修复关闭。Dialog与两tests完成后冻结，r05-handoff.json归档新指纹；此前125/181/types/build均不充作本次验证。

## 当前L01..L03 已加载事实后的后续动作

- Root新委派明确不重开C01-C06、不猜缺失原文；先只读。发现request-start authority并不证明已加载事实的owner：R05 loaded后切actor/base/visit仍发metadata；History与Relations复用旧集合；cached-current主详情仍开旧窗口/审批弹框；close旧提示/unmount后刷新仍工作。
- 专属实际RED：R05新5项5FAIL；History/Relations2项2FAIL；主详情cached-current真实4次导航及旧弹框开启FAIL。portal另实际RED证实APPROVAL_HISTORY丢成DAILY。清空旧事实断言也先FAIL后GREEN。均为真实SFC/handler输出失败，非装配错误。
- 最小GREEN只写owner4Vue：History/Relations/R05的loadedAuthority门禁与失效清理；R05 closed/disposed；index live ensureCurrentDetailContext与portal canonical wire。审批仅检查已存在上下文，不改写业务规则/payload/服务端权限。
- 最终6文件72/72PASS，0F/skip/abort；含新增11项，不与旧78/200累加。中间partial宿主缺新helper3个ReferenceError仅为装配，补实际声明/当前scope端口后原断言保持。四生产Vue ESLint exit0/0error，index两条既有warning。
- 附件loaded-before-click scope只读静态note交Root/附件owner；未写ControlledFileAttachments/viewer/reading/其他owner宿主/Java/Query。本轮无Maven/types/build/E2E/实库/服务/Git执行，Root决定后续集成。
- loaded-fact-followup.md保存实际命令、范围与交接；loaded-fact-handoff.json记录最终源/tests raw和LF指纹后冻结。
