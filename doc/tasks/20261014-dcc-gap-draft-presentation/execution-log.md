# G08 Execution Log

## 最终Root接收回执（2026-10-15，最新状态）

- Root明确全软件域正式接收：M14实际exit0、23 reactor SUCCESS，G48声明18+原父类27=45/45、0F/E/skip；M12其余45类963PASS且无生产差异；最终47份class报告1008/1008，11865输入零漂移。
- 前端F02 38/38、0skip、exit0，T04完整类型和B01构建均通过，2429生产输入零漂移。
- 本线程依据当轮Root最终回执更新task/verification/execution顶段，未自行重跑验证。生产/tests继续冻结；下方旧阶段错误/待接入/总体待验收为时点记录，已由本次最终接收覆盖。
- 工程状态仅因Git提交/推送收尾未授权而blocked，不写completed；未要求的E2E/部署不列软件失败。Root统一最终清理和报告，本线程不再执行cleanup或新增工作。

## A36 旧确认声明宿主fixture收口

- Root唯一回授dcc-detail-application-ic05.test.cjs；所有生产/A35/其它测试冻结，当前M12 Root在运行，不Maven/fulltype/E2E或重跑大套。
- 实际核int_qms，重读closeout/前端规则和coordination；Root F01日志实际位于coordination目录f01-final-application-node.log（其root/前缀在本机不存在，已定位真实文件），缺isPublicMatrixExecution属于宿主提前失败。
- 先记录改前test和只读production指纹、运行当前canonical；补实际声明/helper/真实Vue必要依赖，不改原组变更/取消/IC21/实际值断言迎合通过；真实业务失败再交Root。
- A36诊断：canonical原suite实际28tests/10PASS/18FAIL；isPublicMatrixExecution缺声明造成open早停与后续空值，不作为产品RED。
- A36仅setup接当前实际isPublicMatrixExecution声明、真实version/ordinary附件helper、明确两份saved附件serverhash事实；...vue原已含真实markRaw，不做假替代。
- 同canonical命令28/28PASS、0FAIL/skip、exit0；加载补齐后此suite未见新产品RED，不改生产。
- 原28个test声明及全部业务断言与改前副本规范LF后逐字相同，AST对照PASS；node --check/限定diff-check exit0。测试untracked可见，直接UTF-8核验PASS；四份观察production前后hash相同。不复跑A35/43/118/Maven/fulltype/E2E。
- A36归档preview:ready_for_closeout时exit0，15份自有记录/副本全keep、delete空、warnings none；最终Git禁止/整体Root验收而blocked，不标completed。
- A36 apply实际exit1，blocked状态门禁拒绝，无文件删除；准确记BLOCKED，不写PASS。

## A35 仅测试fixture正式依赖适配

- Root回授唯一测试dcc-revision-target-confirmation.test.cjs；G03不并写，Panel/API/其余生产/测试继续冻结。
- 实际核分支int_qms，读取closeout/前端与coordination:A35；先保存测试改前及只读目标生产指纹，再运行原3项确认当前宿主失败；缺helper不称业务RED。
- 计划只加载真实readApplicationOrdinaryAttachmentExpectation与版本显示helper，给两份正式已存普通附件及明确隔离serverhash，不生成生产hash、不stub helper恒成功。原来源A/1/目标B/1/late-target拒绝保留；草稿可见断言按已接收合同为草稿，raw身份仍独立精确核对。
- 原canonical实跑3tests/1PASS/2FAIL，首失败ReferenceError readApplicationOrdinaryAttachmentExpectation；属于缺fixture依赖，不记录为产品RED。
- 适配后canonical同命令5/5PASS；原3target/late断言继续，新增确认前缺hash、确认期间hash漂移两永久场景；两个普通附件的名称、有序LongID、payload serverhash与sourceID均精确核对。
- 仅注入真实production附件/版本显示helper，无恒成功stub/生产hash生成；旧可见小号断言更新为草稿，另加raw A/1-1不变与实际submit sourceID11断言，不弱化目标保护。
- node --check及限定git diff --check exit0；测试UTF-8和Panel/model/version helper三份只读production前后指纹相同；未写其它owner源、不跑43/118/Maven/fulltype/E2E。

## P01 / R08 current review and display-only follow-up

- P01五个定向场景4PASS/1旧宿主import ERROR；只重开场景以ic05现有单项实际PASS补证，无大回归。
- 复用现有handler宿主只读stdin probe：稳定DELAYED/null/所选id11/普通附件hash成功callback；缺hash确认前拒绝；确认中hash改变零write。真实VueDELAYED子面板显示未记录复现。publicV2已保存精确定义在open请求被丢失也已复现，报告RF08-01。
- 当前review-followup.md列RF08-01(P1)与RF08-02(P2)、精确位置/触发/输出/最小修复/验收，区分Node前端行为与Java静态后果。不重复G11 RF11-01/RF11-02归属，不触A33源。
- Root R08回授G08仅DccRevisionPanel字段名与相邻实际helper/SFC测试；G03唯一owner更新mode helper。先写BDD及改前hash，再真实RED。
- R08 fresh RED: `pnpm --dir IntRuoyiFronted exec node --test --test-name-pattern='R08' tests/unit/dcc-version-display-a09.test.cjs` -> exit1，2tests/1PASS/1FAIL；G03此时已正式落helper三种mode，actualhelper PASS，真实SFC仍旧字段名FAIL。随后本线程只改一处字段label。
- R08 GREEN同命令2/2PASS；mode覆盖三种正式/null/unknown，DELAYED无预设日期输入；eslint面板exit0，限定diff-check exit0。所有实际命令句柄已exit0，不含Maven/types/E2E。
- review-followup含publicV2漏精确定义期待的真实handler复现与BE静态拒绝依据、未检出的manifest绕过、历史固定版/保存值回读PASS及测试宿主限制。等待Root派currentowner，不自行修Panel/API/Workflow/原组helper。
- P01/R08归档preview：ready_for_closeout时exit0，14份自有记录/副本全keep、delete空、warnings none；最终Git禁止/跨owner总体验收未收口而blocked，不completed。
- P01/R08 apply实际exit1：BLOCKED task status is blocked; expected ready_for_closeout or completed；无删除，未冒称PASS。

## 当前只读 follow-up review

- Root新增review协助：G03/G06合入后的浏览→实际所选草稿→申请详情/附件确认；所有G08显示源冻结。
- 已核分支int_qms、重读规则。只读source/少量独立现有Node场景；报告review-followup.md给Root分currentowner，不改upload/Query/Panel/main API或测试源，不运行Maven/fulltype/E2E。

## A19 当前授权与工作台整页交回

- 用户明确授workbench/index.vue仅既定两个显示hunk；分支int_qms，重读coordination/前端/closeout规则。此前真实模板RED保留，本轮只最小GREEN和精确控件ID/必要相邻，不重跑118/43。
- 先保存当前页改前hash/副本，追加真实控件label/model双向切换断言；value/key/排序/defaultfirst/handler/其它布局不改。完成即整页归还Root。
- A19 fresh RED：`pnpm --dir IntRuoyiFronted exec node --test tests/unit/dcc-version-display-workbench-a17.test.cjs` -> exit1，4tests/2PASS/2FAIL，实际radio仍A/1-n，真实SFC无formatter binding。之后仅按授权两行改动。

## A17 影响升版可见候选

- Root 接受 A12 父页并转 G07；本线程不再写 detail/index.vue。43项仅当时定向软件范围，500顶层语句相同仅作最小差异证据。
- A17 授 workbench/presentation.ts 影响升版候选显示与专属测试，禁止命令/ID/CSV/raw/审计改变。
- 已核对真实候选显示在 workbench/index.vue:sourceIterations radio；presentation.ts 现有函数没有影响候选入口。先实现唯一显示helper的纯适配函数并交模板 import/文字具体hunk；index.vue 未授不擅写。
- A09真实数字时间戳补测已 GREEN 7/7，eslint logs/revision exit0；后端Jackson事实与G01类型要求一并归档。
- A17 RED: `pnpm --dir IntRuoyiFronted exec node --test tests/unit/dcc-version-display-workbench-a17.test.cjs` -> exit1，3 tests / 1PASS / 2FAIL；真实formatter返回A/1-1且真实radio仍输出A/1-1、A/1-2。纯formatter随后按A17授权最小GREEN，未授权页面仍等待精确import/文字hunk。
- 后续只读确认G01 applicationRead已加createTime:string|number|null、changeDescription:string|null及decoder；G07 BE投影也存在。按新共同合同仅复跑A09新7项，exit0；字符串与数字时间戳都走真实当前wrapper后挂载label，未重复118/43旧组合。
- A09/A12相关日志/label源的eslint exit0（父页交还前有一条旧style warning）；A17 presentation eslint exit0，限定git diff --check exit0。
- A17当前formatter2/2PASS；实际模板集成用例仍应FAIL，待Root对a17-workbench-wiring两处hunk明确授权或由owner接入；已向用户异步提出精确范围核准问题，未获答复不会写index.vue。
- 当前可达覆盖表reachable-coverage.md、原始审计边界、G01确认框hunk/时间类型已归档；不把所有含versionNo页面盲改。
- 本阶段归档preview：ready_for_closeout下exit0，12个自有任务文件/副本全keep，delete空、warnings none；UTF-8/结构核验exit0。最终blocked，A17未授权模板接线与禁止Git收尾如实保留。
- 最终cleanup apply：session72954最终exit1，真实输出BLOCKED task status is blocked; expected ready_for_closeout or completed，无删除；结果BLOCKED不写PASS。

## A12 详情父页限定显示授权

- Root 实跑 G10 正文/导航9 + selection8 共17PASS 并将 detail/index.vue 整文件暂交 G08，仅 generic 可见 version/history/prompt 显示。完成整页交还 Root→G07 context→G01 确认待办。
- 保留 A04 null/undefined、迟到事件、精确路由、权限和最小正文布局；不写 BasicInfoPanel/BrowseRecords/DetailApplicationPanel，不改 readcontract 或其它 flow。
- 先记录 BDD/改前指纹，再添加真实模板与 handler RED；A09 logs 和 revision label 已取得 fresh26/26 PASS，旧118不用于覆盖新改动。
- A12 RED: `pnpm --dir IntRuoyiFronted exec node --test tests/unit/dcc-version-display-detail-a12.test.cjs` -> exit 1，6/6 FAIL。模板抽取基数初设5但当前实际4，校准真实4后再跑仍全部行为 FAIL：generic版本/4列/送审正文/历史身份/签名提示/打印结构化格泄漏 A/1-2。未把初始基数断言当产品 RED。

## A09 2026-10-15 当前授权

- 分支再次实际核对 int_qms；重读 AGENTS.md、task-closeout-rules、前端规则和 coordination.md:A09。
- Root 独立实跑 A03 wiring12 + History6/refDialog4 22PASS 并接受限定实现；所有 browser 文件正式归还，G08 不再写 browser/index 或其它 browser 文件。
- 新授 logs/index.vue 仅结构化版本格 helper+真实测试；revision/DccRevisionPanel 仅 label 消费正式 createTime/changeDescription；其它 A03 冻结。公共 API/后端由 G01/G07 分工，不自行补。
- 初读时 DccControlledFileRevisionOptions.java.Iteration 与 applicationRead.ts 还未加字段，先实现可空显示消费与 isolated 行为，最终须重新读取字段合同才能声明投影链路已接通。
- BDD 和验证范围已同步 task.md；下面先保存改前指纹，再新增真实挂载测试 RED。
- A09 test harness 首次运行日志 request fixture 缺 __esModule，产生宿主 interop 失败，不计产品 RED；仅修测试 fixture 后重新运行同一测试。
- A09 RED: `pnpm --dir IntRuoyiFronted exec node --test tests/unit/dcc-version-display-a09.test.cjs` -> exit 1，5 tests / 1 PASS / 4 FAIL；实际日志版本格 A/1-2 != 草稿、缺版本 - != 未记录；真实 revision option label 缺每稿时间/说明及未记录文案。日志读取失败可见性用例通过。生产此时尚未改。
- A09 GREEN: 同命令 -> exit 0，5/5 PASS；真实日志加载/点击、原始审计字段逐字保留、正式版/缺值/错误可见、升版正式投影字段 label 与第二份精确 ID。
- 新增正式 option 回归 RED：同命令 6 tests / 5 PASS / 1 FAIL，正式 A/1 label 被草稿缺字段提示扰动；最小限制只对草稿追加时间/说明，保留正式 label。
- 实际 Jackson 合同复核发现未注解 LocalDateTime 按 TimestampLocalDateTimeSerializer 输出 number；新增真实数字时间戳挂载用例先 RED TypeError(item.createTime.trim)，在自身 label 授权范围支持正式 string|number 可空格式，不猜时间、不改API。

## 2026-10-14 Preflight

- 实际分支：int_qms；HEAD：822c31fc0856516cf59ba9cd84bf2bc7f455d17f。
- 授权：WRITE_GRANTED_LIMITED，仅新增唯一 helper 和自有测试/记录；Root 主动读报告，本线程不自行跨线程发消息。
- 既有工作区有并行改动，不做基线提交、不回滚、不接管。
- 规则冲突按当轮明确禁止 Git 优先；最终工程收尾须记录 blocked，不伪称 completed。
- 当前代码证据：browser/presentation.ts:getBrowserVersionSummary 返回原 versionNo；detail/index.vue 多个版本展示；BasicInfoPanel/ControlledFileBrowseRecords 及 DccRevisionPanel 也直接读 versionNo。
- 后端策略证据：DccControlledFileVersionPolicy.parse 以正式版本 + -正整数识别工作稿，非仅 WORKING 状态。因此不能靠 status 或“草稿”显示文案推断精确身份。
- 相邻测试已读：dcc-browser-selected-version-display.test.cjs、dcc-browser-checkin-minor-only.test.cjs、dcc-exact-working-browser-scope.test.cjs；旧测试保留工作小版本号的断言不视为新需求验收。

## Before SHA256 (bytes, 2026-10-14)

只读参考指纹用于交接复核，不主张拥有共享文件；并行 owner 后续可合法改变这些文件。

| Path | Before SHA256 |
|---|---|
| IntRuoyiFronted/src/views/dcc/controlled-file/shared/version-display.ts | ABSENT |
| IntRuoyiFronted/tests/unit/dcc-version-display.test.cjs | ABSENT |
| IntRuoyiFronted/src/views/dcc/controlled-file/browser/presentation.ts | 1EFFC95832D2FE1C481A92C22FABF3A09B0023B1811F4C59E0D90E0685E51166 |
| IntRuoyiFronted/src/views/dcc/controlled-file/browser/index.vue | 1CFD5986E376529D10CD3AB04887F960E36F3DDDACDD9EEA5F25AB2A4FFCF177 |
| IntRuoyiFronted/src/views/dcc/controlled-file/detail/index.vue | 3D5578626A6C44F645FF686BECF8EAC1B49A54F8BA6ADCCA3474575C79168CF4 |
| IntRuoyiFronted/src/views/dcc/controlled-file/shared/ControlledFileBasicInfoPanel.vue | D3A176035EACFA4463D6564899C2F6BD82EA0E03C2CFB280C6E94BA66DBACF2B |
| IntRuoyiFronted/src/views/dcc/controlled-file/detail/ControlledFileBrowseRecords.vue | 1D1934A01E0C04415A9E45688DEBC21F0E588538160D3A575C47250C999D8B97 |
| IntRuoyiFronted/src/views/dcc/controlled-file/revision/DccRevisionPanel.vue | 58F25E9B490238F6510604FF08114A0FF9A643029DDAB369A4390CD953BF8151 |
| IntRuoyiFronted/src/views/dcc/controlled-file/revision/revision-model.ts | 5CC077DC987FEF916FCB0B57F265C28A8DD3E07AD42859AE9CD8F1BD22311267 |

## Verification Events

1. RED: `pnpm --dir IntRuoyiFronted exec node --test tests/unit/dcc-version-display.test.cjs` -> exit 1；8 tests，2 pass / 6 fail。原文展示实现实际返回 A/1-1、A/1-2、A/1/2-3，断言期望“草稿”；非字符串缺少明确合同错误。正式版本及缺失值测试通过。此为行为 RED，非 import 缺失。随后才修改 helper。
2. GREEN: 同上 -> exit 0；8/8 pass。
3. REGRESSION: `pnpm --dir IntRuoyiFronted exec node --test tests/unit/dcc-version-display.test.cjs tests/unit/dcc-browser-selected-version-display.test.cjs tests/unit/dcc-browser-checkin-minor-only.test.cjs tests/unit/dcc-exact-working-browser-scope.test.cjs` -> exit 0；34/34 pass。旧浏览可见 raw 工作号断言仍通过，明确表示页面未接入，不冒称用户页面已修复。
4. Helper type: `pnpm --dir IntRuoyiFronted exec tsc --noEmit --strict --skipLibCheck --types node --target ES2022 src/views/dcc/controlled-file/shared/version-display.ts` -> exit 0；仅 helper，不是全量。
5. Helper format: `pnpm --dir IntRuoyiFronted exec prettier --check src/views/dcc/controlled-file/shared/version-display.ts` -> exit 0。
6. Git visibility: 分支再次为 int_qms；helper/test/task/log 新文件均 untracked 可见。`git check-ignore -v -- <helper> <test>` exit 1 且无输出表示均未被忽略，不是产品测试失败。
7. Review artifacts: 形成四份交付及页面转交清单；只改本阶段新 helper/测试/记录。未运行 Maven/全类型/构建/E2E，未操作服务/远端/数据库/Git 写入。
8. 状态先记 ready_for_closeout（授权阶段资产可审查），准备清理核对；G08 整体待共享接线且 Git 当轮禁止，随后保持 blocked。
9. Cleanup preview: `python -X utf8 C:/Users/D01020/.codex/skills/task-closeout-cleanup/scripts/task_closeout.py --task-id 20261014-dcc-gap-draft-presentation --mode preview` -> exit 0；ready_for_closeout；keep 为六份任务文档，delete 为空，warnings none。使用仓库规则指向的既有脚本，不新增脚本/清理其它任务。
10. `git diff --check -- <helper> <test> <own-task-dir>` -> exit 0；新文件仍 untracked，另做直接 UTF-8/空白/结构检查，不把空 diff 当完整新文件校验。
11. helper 最终 SHA256（bytes）：7F82D35710AD92A250E08158BCB807870C20EE092032A7662C5F8972DDABEB9D；test：980FE815788CF26F17D643BF9CC7EBBCC6BA1BFFDD567E2F0F1A8B22C6A8EB35。
12. 最终状态 blocked：全项仍需 Root 共享接线授权，Git 提交/推送当轮禁止。未把阶段 helper 的 review-ready 状态伪装为工程 completed。
13. Final structural validation: 独立 Python 只读核验 -> exit 0；8 份自有文件 UTF-8 no-BOM、无 replacement char、末尾换行、无尾空格/冲突标记；6 份交付文档及任务必需标题/blocked 状态；integration-notes 中 23 个精确生产路径和详情方法锚点均存在。规范 UTF-8/LF SHA256 与上述两个新增源码字节指纹相同。
14. Cleanup apply: 同一既有脚本 `--mode apply` -> tool observed exit 1，输出 `BLOCKED task status is blocked; expected ready_for_closeout or completed`。没有删除文件；这是工程状态门禁阻塞，不记录 PASS。

## A03 页面接线阶段

- Root 已独立实跑 helper 8 tests 并接受纯显示合同；coordination.md:A03 新授 11 个既有前端文件及所属测试。授权原文和边界同步 task.md；先复核 int_qms 和规则。
- 真实辨识字段：browser versionHistory 正式 VO 含 createTime/changeDescription；revisionOptions.iterations 只有 id/versionNo/canPartial/canReplacement/canPreview，逐稿时间/说明尚缺公共接口合同。不得伪造或默认第一稿；将该缺口交 Root。本阶段先保持精确文件 ID 可见与独立 value。
- 下一步：保存 11 文件改前指纹，新增实际 Vue 渲染/事件 handler 测试先 RED，再接线；非授权页面仍保护。
- A03 RED: `pnpm --dir IntRuoyiFronted exec node --test tests/unit/dcc-version-display-wiring.test.cjs` -> exit 1；9 tests 全部 FAIL；实际 Vue 项目目录/关系/升版/历史表及浏览选项仍显示 A/1-1、A/1-2，时间线实际原文也泄漏，均为目标行为失败，无 import/setup 假 RED。

### A03 Before SHA256 (bytes and normalized UTF-8/LF)

| Path | Bytes | UTF-8/LF |
|---|---|---|
| IntRuoyiFronted/src/views/dcc/controlled-file/browser/presentation.ts | 1effc95832d2fe1c481a92c22fabf3a09b0023b1811f4c59e0d90e0685e51166 | 546430f66cfcfad6341d819df1e2f79e8b61985c7166ff3aa92d0ddf307d77ab |
| IntRuoyiFronted/src/views/dcc/controlled-file/browser/index.vue | 1cfd5986e376529d10cd3ab04887f960e36f3dddacdd9eea5f25ab2a4ffcf177 | 48076b8697fb19fc4d55d809fc5e53f2fd7f38332789ce3a2e0f275a27a22117 |
| IntRuoyiFronted/src/views/dcc/controlled-file/browser/ProjectFolderFileTable.vue | d15774f1beaf71cee85c0b965c28895dff993d5a164aee663d3479438490120f | d15774f1beaf71cee85c0b965c28895dff993d5a164aee663d3479438490120f |
| IntRuoyiFronted/src/views/dcc/controlled-file/browser/ProjectFileRelationsDialog.vue | 7b85123d6dee96409a5883c3ccb3cb930668c36742f099919a1d93887cd27dda | 9dac8a534747683c58cb63669b63df24b681cf45fe10f121d884e7a8081578de |
| IntRuoyiFronted/src/views/dcc/controlled-file/detail/presentation.ts | f5d4d8c9d0db9c1df836235624bc15e79d33cee13693d902cfc7cbef2543c27b | 27d1c8c10f7c03f989877eb983403099376b228fb68cb9497ab5f0d9478b78b9 |
| IntRuoyiFronted/src/views/dcc/controlled-file/detail/DetailApplicationHistory.vue | e62ccade85ff5c451262aa80cdf2b9d090c0cde696429b2ea532e187a0f74802 | 24dbbcc247e5ed8a0c8e898345abf6d14a519729e6f1666511649dbe0825ac51 |
| IntRuoyiFronted/src/views/dcc/controlled-file/revision/DccRevisionPanel.vue | 58f25e9b490238f6510604ff08114a0ff9a643029ddab369a4390cd953bf8151 | 54c0130fab7b2800cfcb696e77b97140e94eb7f65bd3e6718bd436ab89d51b17 |
| IntRuoyiFronted/src/views/dcc/controlled-file/detail/DetailRelationArrangements.vue | 6600f227fe750dea80cb6da1896444dbb9f744f2f07614249e46f3a9ba64a4eb | 9e027efc64fb222c323ca05d03a9a20498807462dce0dbd99a3e1346c0ea4042 |
| IntRuoyiFronted/src/views/dcc/controlled-file/detail/DetailRelationStageEvidence.vue | 29a8e20f0438145516f28d4265d945659776b4e7e97668dc13a3c6fce73c3a00 | 29a8e20f0438145516f28d4265d945659776b4e7e97668dc13a3c6fce73c3a00 |
| IntRuoyiFronted/src/views/dcc/controlled-file/relations/DccFileRelations.vue | b7f2fd8a6ede7af601b3c1dabc3ee338c1241b11e5ac6c1e8fc5c9456215f549 | 4128af0bb701081e21e33b8c85beac6ebb845eea27142ccfa7a849f49f5c5676 |
| IntRuoyiFronted/src/views/dcc/controlled-file/relations/DccProjectReferences.vue | 061929ec902c9272395ec6765c5f5de8564d51ecee2c84684d2cae6b6bf3f588 | 3028641525e00a00859c5f45c3cc09ed4d8fc4207576e23ad4efb53b0ecbf54a |

### A03 调整：DetailApplicationHistory 归还

- 尚未修改该文件，暂停写入，待 Root 转交 G11；本轮未给该组件加 helper 或任何事件。
- 当前/改前原始 SHA256: e62ccade85ff5c451262aa80cdf2b9d090c0cde696429b2ea532e187a0f74802。
- 改前副本: doc/tasks/20261014-dcc-gap-draft-presentation/baselines/DetailApplicationHistory.before.vue；字节校验相同。
- G11 将加入验真 round selection-change 及复用唯一 version helper；其余 A03 文件继续。

### A03 相邻测试 dependency 接入前指纹

- IntRuoyiFronted/tests/unit/dcc-relation-stage-evidence.test.cjs: 34c2f60fd6874cebe6fb6fdef2a89f56f87bb21247d81daebbba803fb3c4cdd8
- IntRuoyiFronted/tests/unit/dcc-e2e-findings-fix.test.cjs: 6b502bca39dd06eb2b4ac0236275fb2e16411524ad7c46d6a8e8fa7765c2cd76
- IntRuoyiFronted/tests/unit/dcc-nc-e2e-findings.test.cjs: 181de6bf160e16c05828777793c48900d915b2e65f21c1294e61b6b7a8f180ae
- IntRuoyiFronted/scripts/dcc-relations-vue-runtime.test.mjs: efb6c4872d12be99354804ca0bc4668a84e82b966fc84241b70a4283f68e9e45

### A03 GREEN / REGRESSION

- 扩充快照/检入提示 RED 后，接线首轮 30 tests 29 PASS / 1 FAIL：升版测试宿主未显式提供实际父页使用的 applicationReady=true；Vue 将可选 Boolean 默认 false，依现行合同正确阻止。修测试夹具后 30/30 PASS，无生产放宽。
- 单独初始化 RED：expected 第二份 9223372036854775702，actual 首份 9223372036854775701。改为本行响应精确 ID、已选 ID 不存在明确失败，接线+working navigation 25/25 PASS。
- 初次相邻组合 69 tests 有一个 loader 还不认识新增 helper；相邻 .mjs 同类 loader 失败。只补真实 helper module 映射后，组合 102/102 PASS。
- 最终 `pnpm --dir IntRuoyiFronted exec node --test` 的 11 个定向文件完整命令见 verification-report.md：118/118 PASS，exit 0；包含 helper 8 项和接线 12 项（11 行为+8 SFC 编译）。本轮没有复用旧 PASS。
- 日期摘要独立筛选命令 1/1 PASS；nc 同文件另两项 viewerMode 旧宿主失败不由本线程接管，未称全文件 PASS。
- 旧 dcc-revision-component.test.mjs 18 tests 10 PASS / 8 FAIL，fixture 缺 selectedDistributionDepartmentIds/effectiveMode；独立 model 两用例重跑 0/2 FAIL，model 指纹与改前相同，详见 a03-review-notes.md，不改原申请合同。
- 全 A03 源码+helper 的 ESLint exit 0：0 errors / DccProjectReferences 第5/11行 2条旧样式 warnings。
- 不运行 Maven/全类型/构建/E2E，不碰服务器/服务/数据库/Git 写入。A03 当前 10文件可 review，DetailApplicationHistory 已归还；升版真实说明合同与日志新增授权仍待 Root。
- A03 cleanup preview: 曾标 ready_for_closeout 后运行既有 task_closeout.py --mode preview，exit 0；8个任务文件（含 .before.vue 归还副本）全 keep，delete 空、warnings none。无不属于当前任务的清理。
- 最终分支仍 int_qms；17个 A03 源码/测试文件 UTF-8 和规范 LF 指纹已归档，限定 git diff --check exit 0。新测试不依赖 cwd，使用 __dirname 解析前端根。
- 依规则最终状态 blocked（剩余共享接入/说明字段合同/禁止 Git），本轮不标 completed。
- A03 cleanup apply 实际 exit 1，输出 BLOCKED task status is blocked; expected ready_for_closeout or completed；没有删除文件，准确保留阻塞而非 PASS。

### A03 Final source SHA256 (normalized UTF-8/LF)

- IntRuoyiFronted/src/views/dcc/controlled-file/browser/presentation.ts: 3d4e89516d27ff905caecca5b4b62ddd5d7193537e31f8835f902271c2a0dbb2
- IntRuoyiFronted/src/views/dcc/controlled-file/browser/index.vue: 19e096786e9cd71c56f44edaf371c5f947493dc26bf6b4c01508cc69e9095b73
- IntRuoyiFronted/src/views/dcc/controlled-file/browser/ProjectFolderFileTable.vue: f03fd2b0abc401195b96db794bb9a045c47d242b7250d87bc7c0a5cc56233ecf
- IntRuoyiFronted/src/views/dcc/controlled-file/browser/ProjectFileRelationsDialog.vue: ccf13e351f8aa6349e0391a8cf9b47653022dd496781030e16a0895074f687e9
- IntRuoyiFronted/src/views/dcc/controlled-file/detail/presentation.ts: ca1eaa966d1d79ffb8c16e22cd4f65ae32439541b582c33e018f572be3ddfbd9
- IntRuoyiFronted/src/views/dcc/controlled-file/revision/DccRevisionPanel.vue: a4f3ca9ce43b2f72cca76140e882a5856ca9b9806a019289f952bf273ebf38ce
- IntRuoyiFronted/src/views/dcc/controlled-file/detail/DetailRelationArrangements.vue: e65863986630933dfe6446999ace8d12c7c66209e2e832a9099435662e332d93
- IntRuoyiFronted/src/views/dcc/controlled-file/detail/DetailRelationStageEvidence.vue: da9d6d793f92c85eb54f37506fe63fce9b5af866e07ef35629e898cf2a5e3a5a
- IntRuoyiFronted/src/views/dcc/controlled-file/relations/DccFileRelations.vue: 2c937ab6e189ec5cbd60854f8f9ba42831ba003ad0fc913798e0c575d9b649a5
- IntRuoyiFronted/src/views/dcc/controlled-file/relations/DccProjectReferences.vue: 4ee1d865eed96fe0c3a003fa3b39e360ae8d085491ed591ce3e49184c497d7bc
- IntRuoyiFronted/tests/unit/dcc-version-display-wiring.test.cjs: 8e0f98e9b5ab9ba4ee34d6fdb70e15c47e85d514f2d85e17deb6b3ebdffcc620
- IntRuoyiFronted/tests/unit/dcc-browser-selected-version-display.test.cjs: 61508d0f5c63fd21ea2f109070e55b0a4e53c38794ca7535674c779da3df8ce7
- IntRuoyiFronted/tests/unit/dcc-browser-checkin-minor-only.test.cjs: c352f852fd6a59def0b6a764dc039b7ef42039a60ce78e50461ad1d4caf79bfe
- IntRuoyiFronted/tests/unit/dcc-relation-stage-evidence.test.cjs: 3bb997156b7b9ef2a2f1afcc086d1fb99207796fda022c0d9a28e5d8e7558d68
- IntRuoyiFronted/tests/unit/dcc-e2e-findings-fix.test.cjs: c4b990c13d46454eb3609e00b5416b90d15e7e178576259a550bbe77a292fec9
- IntRuoyiFronted/tests/unit/dcc-nc-e2e-findings.test.cjs: 7b612bdf9dbd821b7ca9d63082e17525db50e0ae545727b7901e5d5e39a9894b
- IntRuoyiFronted/scripts/dcc-relations-vue-runtime.test.mjs: 458dfda73dea181026125d17edac40ea2e78aff77203d99b9f91166da2fb34d6

### A09 Before SHA256

- IntRuoyiFronted/src/views/dcc/controlled-file/logs/index.vue: bytes=d94d62dca1788c17dbb4767ed11dd824f17aab785bc12409847c108551fbd8c5; UTF-8/LF=78c9049b5c17fef523fb19fdac24215dfbca317094e4f7a5566031c241bffd5f
- IntRuoyiFronted/src/views/dcc/controlled-file/revision/DccRevisionPanel.vue: bytes=0f5e693a829c4d90bce5fc42bf17b5e695367d9b26b9e9fde13546a0cc4ba4b9; UTF-8/LF=a4f3ca9ce43b2f72cca76140e882a5856ca9b9806a019289f952bf273ebf38ce

### A12 Before SHA256

- IntRuoyiFronted/src/views/dcc/controlled-file/detail/index.vue: bytes=bd834799fd8ddb803f4168ba549e2d04e54acd4851cdef8cc67f9b7e546241fe; UTF-8/LF=c4d1ec3bb00c515dfbe3282c6b0fed114084d37fe6eefcd61bcb99cd70e15911
- 改前字节副本 baselines/detail-index-a12.before.vue 已保存。

### A12 affected handler test hosts before hashes

- IntRuoyiFronted/tests/unit/dcc-approval-dialog-context.test.cjs: 38be981c78afdf2f2a8301cd3076f16bbc8ead1ec52a325cfd4cc76ebf1a03f2
- IntRuoyiFronted/tests/unit/dcc-native-completion-summary.test.cjs: 18b99c48179cd78bff7bd0818e19dc09a043805c430de2ed55286b4a3f071992
- IntRuoyiFronted/tests/unit/dcc-upload-effective-activation-times.test.cjs: a6ed0655e9d778eaefef02f5f5fd51b718f889290840829bc57e19bbee67c5e9

### A17 before SHA256

- IntRuoyiFronted/src/views/dcc/controlled-file/workbench/presentation.ts: 1f4203d4e7feecf087c7fe64376e37a026b82e58543211dcf0947084da102ac1
- IntRuoyiFronted/src/views/dcc/controlled-file/workbench/index.vue: df5ba7a570e6bf7370be1303b8d8a245260a85eca171a2ad9922d3749fbcee19

### A09/A12 test / A17 current SHA256 (UTF-8/LF)

- IntRuoyiFronted/src/views/dcc/controlled-file/logs/index.vue: 2c574a3be700c110647e3f51174b1ae5619cff5c194155910c31331512cd5092
- IntRuoyiFronted/src/views/dcc/controlled-file/revision/DccRevisionPanel.vue: 379680a27d61efa70c1e90f44ded56b95c8110de6d4618f6318846f8231ce965
- IntRuoyiFronted/src/views/dcc/controlled-file/workbench/presentation.ts: c783c154359578ce4dd0d24899ded8822f0a9266d10abc33c7564b30bdb9cb39
- IntRuoyiFronted/tests/unit/dcc-version-display-a09.test.cjs: 7a21c3cf7fb7347892c513f9969a5cb038fb26b4ce2e6b0763076d06313ba16b
- IntRuoyiFronted/tests/unit/dcc-version-display-detail-a12.test.cjs: 927d018ea92ee05de3d2d162b28f64b520678e00b10f9368145f5bb2fe7ba536
- IntRuoyiFronted/tests/unit/dcc-version-display-workbench-a17.test.cjs: 1ef70331c091401ad380aa5ba03261aaa0d1e438f674b8a6f7ab6326f698e153

### Last A09/A17 fingerprints (UTF-8/LF)

- IntRuoyiFronted/src/views/dcc/controlled-file/logs/index.vue: 2c574a3be700c110647e3f51174b1ae5619cff5c194155910c31331512cd5092
- IntRuoyiFronted/src/views/dcc/controlled-file/revision/DccRevisionPanel.vue: 379680a27d61efa70c1e90f44ded56b95c8110de6d4618f6318846f8231ce965
- IntRuoyiFronted/src/views/dcc/controlled-file/workbench/presentation.ts: c783c154359578ce4dd0d24899ded8822f0a9266d10abc33c7564b30bdb9cb39
- IntRuoyiFronted/tests/unit/dcc-version-display-a09.test.cjs: cdb505730dd4119b434b8bcb5a21e5087f80394f3bb2ce012e47bd8d42f9a3b1
- IntRuoyiFronted/tests/unit/dcc-version-display-workbench-a17.test.cjs: 1ef70331c091401ad380aa5ba03261aaa0d1e438f674b8a6f7ab6326f698e153

### A19 Before SHA256

- IntRuoyiFronted/src/views/dcc/controlled-file/workbench/index.vue: bytes=df5ba7a570e6bf7370be1303b8d8a245260a85eca171a2ad9922d3749fbcee19; UTF-8/LF=93805cd0cb4eed5aa2712434db914dde7dea72ea21e0b099326aff999d6094a2
- 字节副本: baselines/workbench-index-a19.before.vue

### A19 GREEN / handback

- 专属4项+必要工作台process/权限回归:11/11 PASS，exit0；未重跑118/43。
- 两文件ESLint exit0无warning/error；限定git diff --check exit0。
- 字节对照(规范LF)严格仅formatter import+radio插值两处，PASS；value/key/排序/defaultfirst/handler/其它布局未改。
- workbench/index.vue归还时SHA256(bytes): f3228ed707ced753314341d33e9f110253b160c110e8bb6a822951b76482431a；即刻整页交还Root，本线程不再写。

- A19最终文档UTF-8/结构/交还hash核验PASS；不把其他owner当前变化写回已归还文件。
- A19 cleanup preview：ready_for_closeout下exit0，13份任务记录/基线全keep，delete空、warnings none。按Git当轮禁止及整体owner验收未收口最终blocked，不标completed。
- A19 cleanup apply：实际exit1，被blocked状态门禁拒绝，无文件删除；记录BLOCKED，不写PASS。

### P01 evidence / R08 before fingerprints (UTF-8/LF)

- IntRuoyiFronted/src/views/dcc/controlled-file/revision/DccRevisionPanel.vue: 379680a27d61efa70c1e90f44ded56b95c8110de6d4618f6318846f8231ce965
- IntRuoyiFronted/src/views/dcc/controlled-file/revision/revision-model.ts: 969a76be62b68a3b54da9338a35f10b24ed8c15e8a54be6efc68cb4982bb6b2a
- IntRuoyiFronted/src/views/dcc/controlled-file/detail/DetailApplicationPanel.vue: 270493d94a5517ab89bf814a013e96db1438a6fba192a6ede2ddc41e88ea7329
- IntRuoyiFronted/src/views/dcc/controlled-file/upload/approval-matrix.ts: 4674f6fe928f1359df663292c2284dd9e900b5d8d18c37ac2d033c8dce2d2c93
- IntRuoyiFronted/src/views/dcc/controlled-file/upload/signoff-departments.ts: 01687f583d1b49ee328dca97e2a86bf75b574a3ead8290f74e93be7477222565
- IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileWorkflowServiceImpl.java: 9c5af0bcb24afe4d1fe9f89134d649f3c2e3e1a105680ed1cbcd6364927911ab
- IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccApplicationExecutionContractService.java: 2b6102af6711a64505d321b2849a75229b51577efb7627808e7edd844f2548c6

- R08精确对照PASS：逆替换唯一字段名后规范SHA256等于P01记录改前指纹，无其它面板差异。当前面板交回SHA256(bytes)=f0858f7cb905d285c372f079bf41e69ab23ecd80909a1dfb756e0a5222368fde。文档UTF-8/结构PASS。

### A35 Before fingerprints (UTF-8/LF)

- IntRuoyiFronted/tests/unit/dcc-revision-target-confirmation.test.cjs: a880a904cdd2291ffb81421e1ecaefe96e3b164a180aa6e1e83301da061fcd97
- IntRuoyiFronted/src/views/dcc/controlled-file/detail/DetailApplicationPanel.vue: d1ff6b473f5ecdcd6e5eab594e42852e2138b0916a87086ad27ac950f6059aeb
- IntRuoyiFronted/src/views/dcc/controlled-file/revision/revision-model.ts: 0e2c607c403f181f991e59e4cbb2aca230b1f00564565ca62600a210898d1c6a
- IntRuoyiFronted/src/views/dcc/controlled-file/shared/version-display.ts: 7f82d35710ad92a250e08158bcb807870c20ee092032a7662c5f8972ddabeb9d

### A35 After fingerprints (UTF-8/LF)

- IntRuoyiFronted/tests/unit/dcc-revision-target-confirmation.test.cjs: 057467d54ef6b7f5eae421c917fe598c61d114a0d8a00133a808e5e69f4be1b0; authorized test fixture update
- IntRuoyiFronted/src/views/dcc/controlled-file/detail/DetailApplicationPanel.vue: d1ff6b473f5ecdcd6e5eab594e42852e2138b0916a87086ad27ac950f6059aeb; production unchanged
- IntRuoyiFronted/src/views/dcc/controlled-file/revision/revision-model.ts: 0e2c607c403f181f991e59e4cbb2aca230b1f00564565ca62600a210898d1c6a; production unchanged
- IntRuoyiFronted/src/views/dcc/controlled-file/shared/version-display.ts: 7f82d35710ad92a250e08158bcb807870c20ee092032a7662c5f8972ddabeb9d; production unchanged

### A36 Before fingerprints (UTF-8/LF)

- IntRuoyiFronted/tests/unit/dcc-detail-application-ic05.test.cjs: dfc10778c6936a947d5d00b296fe8003c317e47934172d470f8945eb23c2b782
- IntRuoyiFronted/src/views/dcc/controlled-file/detail/DetailApplicationPanel.vue: d1ff6b473f5ecdcd6e5eab594e42852e2138b0916a87086ad27ac950f6059aeb
- IntRuoyiFronted/src/views/dcc/controlled-file/revision/revision-model.ts: 0e2c607c403f181f991e59e4cbb2aca230b1f00564565ca62600a210898d1c6a
- IntRuoyiFronted/src/views/dcc/controlled-file/upload/approval-matrix.ts: b0ae164c3c8a08c2fb53895976770c69acafe2ba44eb6f894c7344353397670c
- IntRuoyiFronted/src/api/dcc/controlledFile/applicationRead.ts: 0fc18174caceccc92ea956fcd7e402323ad2f0300795a0c6a3f8d9d59af921af

### A36 After fingerprints (UTF-8/LF)

- IntRuoyiFronted/tests/unit/dcc-detail-application-ic05.test.cjs: efd32d72dfa1586ab97cf39afed534255244d5c300b0caa2ee7d1d3e98af79b2; authorized fixture setup only
- IntRuoyiFronted/src/views/dcc/controlled-file/detail/DetailApplicationPanel.vue: d1ff6b473f5ecdcd6e5eab594e42852e2138b0916a87086ad27ac950f6059aeb; production unchanged
- IntRuoyiFronted/src/views/dcc/controlled-file/revision/revision-model.ts: 0e2c607c403f181f991e59e4cbb2aca230b1f00564565ca62600a210898d1c6a; production unchanged
- IntRuoyiFronted/src/views/dcc/controlled-file/upload/approval-matrix.ts: b0ae164c3c8a08c2fb53895976770c69acafe2ba44eb6f894c7344353397670c; production unchanged
- IntRuoyiFronted/src/api/dcc/controlledFile/applicationRead.ts: 0fc18174caceccc92ea956fcd7e402323ad2f0300795a0c6a3f8d9d59af921af; production unchanged
