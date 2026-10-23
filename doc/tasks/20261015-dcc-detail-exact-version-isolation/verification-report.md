# Verification Report

## Result

**当前L01..L03结果**：旧数据成功加载后切actor/tenant再发动作的缺口已实际RED/GREEN修复，最终6文件72/72PASS、0skip；4生产Vue lint exit0/0error，仅两条既有warning。新增loadedAuthority将已加载事实与动作身份绑定，详情入口实时检查而非相信缓存computed；受控浏览portal保留原raw版和wire/BPM/task。close/dispose阻止旧提示与卸载后新请求，显式refresh/reopen仍只读原固定版本。详[本轮loaded-fact报告](loaded-fact-followup.md)与loaded-fact-handoff.json。所有本轮源码/tests已冻结，未运行78/200全集、Maven/types/build/E2E/实库/Git；下述20/8/58/125/181等均为前轮证据，不累加。

**最新R05结果**：Root已将独立引用对话框唯一交本线程，真实SFC首次13项1PASS/12FAIL后已最小GREEN；最终新增15项+原Dialog5项共20/20PASS、0skip。Dialog lint exit0/0error/0warning；本轮只改Dialog与两测试，四详情Vue指纹不变。R05已验证关闭，不再待owner或停留静态发现。命令/范围详[边界报告R05收口](authority-boundary-followup.md)，当前指纹与冻结声明见r05-handoff.json。未重跑58全集或Maven/types/build/E2E/实库/Git。

以下8/58为前一阶段结果；当前R05追加已授权并实际修复，其“跨owner静态待分配”描述只保留为当时历史，不是当前待办。

**前一阶段authority结果**：新增8/8、受影响7文件58/58PASS；4个详情Vue修复并冻结。当时R05只作跨owner静态交接，随后已获追加授权并由最新20项回归关闭。原8/58阶段诊断见[边界报告](authority-boundary-followup.md)，原指纹在authority-handoff.json，四详情Vue本次未改。

本轮未运行Maven/shared target/fulltypes/build。Root上轮29源码0drift、125后端/181前端/完整types及2635输入构建通过，均为修改前历史检查点，不能证明本轮4个新Vue输入已集成验证。以下R01/R02/F01与M01为上轮已关闭记录，保留且不混加本轮数字。

R01/R02/F01软件验证已关闭。wrapper混合/null context已完成新Node 4/4PASS；Root M01真实RED复现F01目标assertFalse FAIL后，恢复三个生产文件原始字节SHA一致0mismatch，-am定向十类Maven exit0、freshXML125/125PASS、0F/E/skip。本线程未自行重跑旧43/149、Maven、shared target、fulltypes或build。工程状态仅因本轮无Git提交/推送授权保持blocked，不再保留本次待Maven/缺依赖阻塞。

Root 提供的上轮实际 Maven 七类 115/115（含 SelectedDetailService7 + HTTP3）已解除此前 reading 缺依赖阻塞。该结果是上轮检查点，不作为本轮新三参 Query 入口的通过证据。

冻结协调历史：Root 接收 review-handoff.json 后启动唯一 M01。本线程源码、接口与测试一直冻结，只补自有文档；前端4/4已交还，两个指纹文件保留原 GREEN 候选。RED/GREEN 当前状态以下面最新回执与表格为准。

本次最终正式回执与证据：[后端GREEN十类汇总](../20261015-dcc-six-thread-followup-review/backend-green-summary.json)、[真实RED汇总](../20261015-dcc-six-thread-followup-review/backend-red-summary.json)。M01 fresh125全部PASS，三临时恢复源匹配原GREEN指纹。Root Viewer12文件82+Detail12文件99共181PASS、各组0fail/skip及0drift，不与本线程4项或历史数字混加。完整 `pnpm run ts:check` 已exit0、2429源码输入0漂移，见[类型GREEN结果](../20261015-dcc-six-thread-followup-review/fe-types-green-result.json)。signatureSummary.processInstanceId仅补正式可选字段，Root已证明新旧TypeScript emitted runtime字节相同，未重复业务套件。10生产lint exit0/0error，只有detail两条既有换行warning。当前不存在本次待Root后端/类型/lint验证项。

全部源码冻结历史回执：前端与相关 tests 已停止编辑并交还，原 handoff-fingerprints.json 保持不变。Root viewer 组82/82是较早的独立检查点；当时 Reader 暂处 RED 一行，随后 Root 已恢复原字节 GREEN。本线程未干预任一临时状态。

## Verification

| 检查 | 结果 | 证据边界 |
|---|---|---|
| 当前loaded-fact新增边界与受影响回归 | 6文件72/72PASS，0fail/skip | 本轮新增11项包含其中；不混加旧全集 |
| 当前loaded-fact四生产Vue lint | exit0/0error | index旧2条换行warning |
| 最新R05真实SFC专属+原Dialog回归 | 两文件20/20PASS、0fail/skip | 新15项+旧5项，不与8/58或125/181混加；R05关闭 |
| 最新R05 ESLint | exit0/0error/0warning | 仅改动Dialog生产文件 |
| 新一轮 authority/late-response 专属 | 8/8PASS | 新真实RED后本轮GREEN；不包含旧125/181 |
| 新一轮受影响邻域 | 7文件58/58PASS、0skip | 仅受影响详情/轮次/关系/wire回归；专属8项包含其中，不累加 |
| 新一轮生产Vue ESLint | exit0/0error | 4个改动Vue；index两条既有换行warning |
| 前阶段R05静态发现 | 已由Root追加归属并完成实际RED/GREEN | 不再待owner；以最新20项回归为准 |
| 本轮 wrapper/reading 上下文边界 | 4/4 PASS，0 fail/skip | 新增实际 wrapper、loader 与 reading API 跨边界 Node 用例；未跑旧套件 |
| 本轮 F01 后端 RED | Root 实际目标 assertFalse FAIL，预期缺陷已复现 | generic 前置实际授权通过，WORKING canPreview=true；非装配失败 |
| 本轮 F01 后端 GREEN | Root十类freshXML 125/125PASS，0F/E/skip | -am命令exit0；三生产文件原始字节恢复0mismatch，软件验证关闭 |
| Root 详情/上下文/附件/关系邻域 | 12 文件 99/99 PASS，6 输入0drift | Root 独立执行；不与历史/本线程用例累计 |
| Root viewer邻域 | 82/82PASS、0drift | 与详情99分别记录，不混加 |
| Root 前端本次两组总计 | Viewer12文件82 + Detail12文件99 = 181PASS | 各组0fail/skip、0drift；不混加历史或本线程4项 |
| Root 完整 ts:check | pnpm run ts:check exit0，2429输入0漂移 | 正式字段声明补齐；新旧emitted runtime字节相同，不重复业务套件 |
| Root 10生产lint | exit0，0error | detail两条既有换行warning |
| Root 上轮 Maven 七类 | 115/115 PASS（Root 回执） | 含 SelectedDetailService7 + HTTP3；旧 reading 缺依赖已解除，不证明本轮 F01 |
| 上轮 FE 合并回归 20 个去重文件 | 历史 149/149 PASS，0 fail/skip/abort | 下列旧命令只作历史记录，本轮未重跑 |
| FE 主回归 13 文件 | 106/106 PASS，0 skip | Node 执行实际 TS、Vue SFC 脚本/模板与 handler，不是真实页面 E2E |
| FE 补充回归 9 文件 | 50/50 PASS，0 skip | 包含最终 wrapper/打印竞态新增用例；与主回归重叠，不相加 |
| 上轮 BE 私有 fresh 编译/JUnit | 历史 43/43 PASS，0 skip/abort | 原六类证据；本轮未重跑私有 Java |
| 上轮 BE Maven reactor 缺依赖 | 历史阻塞，已解除 | 当时 reading VO/session DO/Mapper/Service 缺失；Root 后续实际 115/115，不能继续作为当前 blocker |
| 本轮 FE 定向 ESLint | PASS，0 error | workflow.ts、detail-read-context.ts、index.vue；两条既有 index.vue 属性换行 warning |
| 本任务 diff | PASS | 不回滚并行改动；UTF-8/结构收尾结果见下 |

## Commands

当前loaded-fact命令与真实RED诊断见loaded-fact-followup.md。只运行6个受影响文件及新增用例定向RED/GREEN，没有重复78/200全集。

最新R05实际命令（本次不重跑58全集）：

```powershell
node --test tests/unit/dcc-reference-usage-authority.test.cjs tests/unit/dcc-reference-usage-dialog.test.cjs
pnpm exec eslint src/views/dcc/controlled-file/relations/DccReferenceUsageDialog.vue
```

RED为actor/baseTenant/visitTenant三变种×page/preview×成功/失败，共12条实际业务断言FAIL；正常fixed Long/raw版权限控制1PASS。最小实现后13+5=18PASS，再加2项当前真实错误/非法Long回归得到最终20PASS；不把初次18与最终20累加。没有fixture/编译失败冒称RED。

新一轮实际命令：

```powershell
node --test tests/unit/dcc-detail-authority-late-response.test.cjs
node --test tests/unit/dcc-detail-authority-late-response.test.cjs tests/unit/dcc-detail-reading-context-boundary.test.cjs tests/unit/dcc-selected-application-context.test.cjs tests/unit/dcc-selected-version-relations.test.cjs tests/unit/dcc-detail-route-read.test.cjs tests/unit/dcc-content-application-selection.test.cjs tests/unit/dcc-content-detail-split.test.cjs
```

新增7项初始6FAIL/1PASS，另Browse摘要在reactive props/route下单独实际RED；GREEN为8/8。相邻宿主漏载新getter为测试装配问题，按真实声明补载后58/58；最后partial records模板漏载readBrowseAuthority的warning通过补载真实声明后定向1/1确认，无新增业务断言，不重复整套。原断言全部保留，未修改其他owner测试宿主。

以下命令/4项/125/181为上一轮证据：

本轮实际只执行新增 Node 文件：

```powershell
node --test tests/unit/dcc-detail-reading-context-boundary.test.cjs
```

初次搭建跨模块测试时的 default export fixture 错误单独修正；真实 RED 为 2 FAIL/1 PASS（混合参数和 null context 未拒绝）；GREEN 初次 3/3，追加同文件不同轮次迟到响应后 4/4 PASS。真实 loader/reading API 用同一 Long ID/raw version/BPM/task，四类 reading 请求 wire 与详情 GET 完全一致；构造新轮次后旧 reading API 仍只发送旧 wire，同文件 late detail 响应不覆盖新申请身份。

F01实际RED方法、最小恢复hunk与已完成GREEN回执见 `f01-red-green-replay.md`。Root真实RED11中3条断言失败与4条实际旧权限拒绝均有保存；F01前置generic授予通过、目标断言失败不属于装配错误。本线程未执行Maven。

M01十类freshXML分项（均0F/E/skip）：

| 类 | 通过数 |
|---|---:|
| ReadingSession | 29 |
| ReadingController | 12 |
| SelectedDaily | 6 |
| SelectedDetailService | 7 |
| SelectedDetailHTTP | 3 |
| DailyBrowser | 11 |
| BodyReadCurrent | 27 |
| DetailGuard | 19 |
| BodyContext | 3 |
| OfficeToken | 8 |
| M01合计 | 125 |

完整类名、freshXML时间与逐报告SHA256由Root的backend-green-summary.json保存；本线程仅只读核对十行计数与零失败/错误/跳过，不启动测试。

以下命令为上轮历史记录，本轮不重跑：

FE 主回归在 `IntRuoyiFronted` 执行：

```powershell
node --test tests/unit/dcc-detail-exact-read-context.test.cjs tests/unit/dcc-detail-route-read.test.cjs tests/unit/dcc-selected-application-context.test.cjs tests/unit/dcc-content-application-selection.test.cjs tests/unit/dcc-detail-application-round-context.test.cjs tests/unit/dcc-content-detail-split.test.cjs tests/unit/dcc-detail-integration.test.cjs tests/unit/dcc-selected-version-relations.test.cjs tests/unit/dcc-shared-attachments-component.test.cjs tests/unit/dcc-reference-usage.test.cjs tests/unit/dcc-reference-usage-dialog.test.cjs tests/unit/dcc-g07-body-read-context.test.cjs tests/unit/dcc-body-read-wrapper-g01.test.cjs
```

补充回归：

```powershell
node --test tests/unit/dcc-detail-route-read.test.cjs tests/unit/dcc-body-read-wrapper-g01.test.cjs tests/unit/dcc-detail-closure.test.cjs tests/unit/dcc-application-evidence-panel.test.cjs tests/unit/dcc-application-read-loader.test.cjs tests/unit/dcc-current-relation-source.test.cjs tests/unit/dcc-relation-history-read-contract.test.cjs tests/unit/dcc-attachment-download.test.cjs tests/unit/dcc-version-display-detail-a12.test.cjs
```

上轮后端定向 fresh 验证使用 `python -X utf8 doc/tasks/20261015-dcc-detail-exact-version-isolation/run-targeted-junit.py`；当前 Root 独占后端验证，本线程不再调用。

## BDD Coverage

- 编码历史申请、明文任务、跨文件/错版本/重复或冲突参数：固定原 ID/raw version/type/BPM/task，错误明确拒绝。
- 真实服务授权先于详情投影；当前任务与历史任务由真实服务端任务事实判定，不由前端宣称。
- 正文→详情的 wire 保留；历史申请版本/类型核验后才发布 selection-change；切换/失败/卸载 null 清理。
- 关系只取所选申请/版本冻结集合，引用只按 selectedControlledFileId；目标升版不替换历史目标。
- 附件维持精确文件/附件双 ID 与服务器权限；原始完整附件清单、下载拒绝及迟到确认均有既有定向回归覆盖。
- Long actor 不数字化；刷新失败清空旧文件、签名/打印/待办；打印响应迟到不覆盖新版本。
- 当前版本参考独立成区，历史冻结关联与所选版本引用显式标明范围。

## Remaining Limits

当前Dialog及两测试已冻结，四详情Vue继续保持前次冻结指纹；Root可按最新Dialog新输入选择集成types/build。2635输入构建exit0/0drift/3239产物仍只作历史基线。R05已获Root追加归属并以自身实际SFC/load/preview的20项回归关闭，不再存在待owner状态。

未执行真实前端E2E或大构建；没有重启服务、执行实际MySQL迁移或写入业务数据库，不声称运行服务已加载修复，也不以Mockito/MockMvc/源码复核替代运行/MySQL/外部Office/浏览器验收。Root M01 fresh125、FE181、完整类型exit0/2429零漂移与lint正式回执已关闭本次全部软件验证门禁，旧缺依赖与字段缺声明均只作历史记录。用户明确禁止Git提交/推送，工程不能标completed；Root统一最终指纹与清理，本线程不再执行清理或套件。

## 本轮复核与跨 owner 交接

- 精确详情 route/wrapper：仅接受单一 canonical wire 或明文 BPM/task/type；冲突或相同混用都拒绝，不吞 null context，不推断 orphan application/task 的 native BPM。
- 精确 reader / 新 Query 入口：保持租户、actor、file、raw version 与真实任务/轮次 Guard；仅显式 DAILY 对响应再收窄。未把根 context 放入 ThreadLocal 历史遍历；generic 两参语义不变。
- History / BrowseRecords：按当前 file、wire BPM/type、raw version 和 generation 发布选择；错误/null/切换清理仍在。新 Node 覆盖同一大 Long 文件仅轮次变化时旧详情晚回包。
- 固定关联目标自己构建 VERSION_HISTORY+目标 ID/raw version；不借父申请 BPM。preview permission 回包仍检查 generation/readSequence；不请求 current set 替换历史成员。
- 附件仍走文件 ID+附件 ID 双身份和独立授权，阅读 owner 的 startPreviewReading 明确跳过 DCC 附件、分发、本地 blob 等独立来源；本线程未变更附件/reading实现。
- 阅读会话 API 捕获同一 canonical body header；service 的 event purpose、file/version/actor/token/session/context 验证与 Query.assertPreviewReadingAccess 继续保留，未改其 hunk。
- R04 跨区 finding：复核时 controlled reading 四路的 query 菜单门禁与合法审批身份不一致。Root 已明确接受并正式转交阅读 owner：改为 isAuthenticated 并保留真实 Guard，统一 file-preview 原菜单门禁不变。本线程只记录交接，不写阅读 Controller/test，不以此历史发现覆盖阅读 owner 后续实现；最终集成结果由 Root 回收。

## 本轮新增改动与已核验结果

- workflow.ts 仅 getControlledFile 区域：严格拒绝混合/plain 非法身份与 null context；其余 reading/业务 API 区域未动。
- detail-read-context.ts 与 detail/index.vue：route 同等拒绝混用；loader 向 wrapper 只提交一种明确身份，保留来源校验及旧响应门禁。
- Query interface/Impl 新增三参 getControlledFile；SelectedDetailService 只把 query 调用加 context，保留原 Guard；SelectedDetailReadServiceTest 适配三参 stub/verify。
- 新增 DccSelectedDailyDetailProjectionTest，6项。Root真实RED确认目标缺陷，恢复GREEN后本类6/6PASS并进入十类125/125正式freshXML汇总；F01无待Maven项。
- 最终交还 UTF-8/LF SHA256（含共享窄 hunk 和受保护方法）见 handoff-fingerprints.json。不要用整份旧 Query/Controller 覆盖并行改动。
- Java/共享接口冻结 ready 检查点已保存为 review-handoff.json，包含 6 个 Java 文件原始与 UTF-8/LF SHA256、F01唯一恢复行、Root RED/相邻 GREEN 命令及尚未执行的方法名。Root 开始回放后不可用 RED 临时状态覆盖此 GREEN 候选指纹。
- Root已完成workflow.ts的ControlledFileSignatureSummaryVO字段补齐、完整types及emitted runtime字节相同验证；原whole-file指纹是补字段前检查点，不将授权类型补齐报作行为漂移。本线程不覆盖原JSON，最终集成指纹由Root归档。

## Closeout

以下cleanup为上轮实际执行；本轮未执行Git、cleanup删除或backend/shared target命令。续办沿用同一任务，R01/R02/F01软件验证已由Root正式关闭；工程Git收尾未授权，因此blocked，不把历史清理冒充本轮已完成提交/发布。

已先标 ready_for_closeout，cleanup preview exit 0，保留三报告与两验证脚本；delete 仅本任务 private-junit 的临时 class/args，warnings 无条目。用户禁止 Git 提交/推送，因此状态转 blocked；不把 cleanup apply 的状态门禁拒绝写为 PASS。

cleanup apply 实际 exit 1，因 blocked 状态拒绝执行，未删除文件；private-junit 临时产物仍在，仅归属本任务。最终三报告 UTF-8、必需章节、blocked 状态、Cleanup Keep 路径结构核验 PASS。

## Changed Files

- 后端：`controller/admin/file/DccControlledFileController.java`；新增 `service/file/DccSelectedDetailReadService.java`、`DccSelectedDetailReadServiceTest.java`、`DccSelectedDetailReadHttpTest.java`。未修改并行 QueryService/viewer/session/附件实现。
- 前端：`api/dcc/controlledFile/workflow.ts`、`detail/index.vue`、`ControlledFileBrowseRecords.vue`、`DetailApplicationHistory.vue`、`DetailRelationsPanel.vue`；新增 `detail/detail-read-context.ts`。
- 前端测试：新增 `dcc-detail-exact-read-context.test.cjs`、`dcc-detail-read-test-loader.cjs`；更新本任务关联的 route-read、application-round-context、content-application-selection、content-detail-split、detail-integration、selected-application-context、body-read-wrapper-g01 的实际依赖或断言。
- 任务目录：本报告、task.md、execution-log.md、run-targeted-junit.py、SelectedDetailJUnit.java。其余并行改动全部保留。
