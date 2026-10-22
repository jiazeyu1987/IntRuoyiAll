# E2E-08 项目/类型切换保护

业务验收 PASS，Root 独立复核已接受（ROOT_ACCEPTED，2026-10-18 00:50，Asia/Shanghai）。复核依据：[e2e08-root-review.json](../20261016-dcc-24-e2e-coordination/e2e08-root-review.json)。原范围为完整关联七分支及 3+7 独立分支，汇总：acceptance-result-20261018.json。E2E-08已按Root指令停止执行；浏览器保持关闭，原失败、trace及自有数据全部保留。工程closeout因用户禁止Git提交/推送继续独立blocked，本次仅同步复核结论。

| 完整源件、手改属性/名单及 ACTIVE 关联前提 | 结论 | 实际通过证据 |
|---|---|---|
| E2E-08-PROJECT-CANCEL | PASS | fixture-evidence\ui-1792251298793\run-linked-1792252819298\linked-result.json |
| E2E-08-PROJECT-INNER-CANCEL | PASS | fixture-evidence\ui-1792251298793\run-linked-1792252905876\linked-result.json |
| E2E-08-PROJECT-CONFIRM | PASS | fixture-evidence\ui-1792251298793\run-linked-1792253627178\linked-result.json |
| E2E-08-TYPE-CANCEL | PASS | fixture-evidence\ui-1792251298793\run-linked-1792253627178\linked-result.json |
| E2E-08-TYPE-CONFIRM | PASS | fixture-evidence\ui-1792251298793\run-linked-1792254146169\linked-result.json |
| E2E-08-OTHER-CANCEL | PASS | fixture-evidence\ui-1792251298793\run-linked-1792254146169\linked-result.json |
| E2E-08-OTHER-CONFIRM | PASS | fixture-evidence\ui-1792251298793\run-linked-1792254146169\linked-result.json |

完整分支验证取消逐字段保持；项目两层确认取消；确认项目清空旧关联、重载 P2 属性/目录/配置并重新选择 P2 A/1；已维护 T2 矩阵与默认下发重载；OTHER 本次矩阵及独立下发选择。类型切换的源件、编号、关联按实际状态记录，不强加需求外清空要求。

运行态：int_qms，8061/PID24564、48061/PID36320；SHA256 1e8d1c791ed7f6342c29ec44a4d3d50523a8cb8cc172b505c702b6920167feb0。完整七项和名单/类型七项均沿用此包；属性三项原 SHA6379718091162ce1e80a1d7a5a08af8a3c65483285612f11d9cd36a55b232d35 证据保留，不冒充新包重跑。

前提与保留资产：原 PUBLIC 候选 REFP1/REFP2仍待共享文控签名，未完成、未删除、未计入 ACTIVE。Root授权另由APPLICATION_CUSTOM准备 BASEP1/BASEP2，经本例 AUTHOR→SUPERVISOR→负责人指派SIGNER→SIGNER→APPROVER确认AUTHOR负责人→DOCCTRL正式签名，文件 #2054545668044084195/#2054545668044084196 均真实显示受控已生效 A/1；fresh作者已点浏览、等待遮罩消失并核对非空画布，两项目关联窗口各实际选择对应固定版本。正文截图和证据见 fixture-evidence/ui-1792251298793/verify-active-baselines-1792254394043/active-baselines.json。基目录 DCC241016C08STORE/913905 仅新增作者启用、名称/内容查看，下载关闭；未改派生叶或共享权限。

历史失败全部保留于 historicalAttempts：远程下拉刷新定位竞态、异步切换/上传退出动画过早断言、直接setInputFiles同一路径跳过ElementPlus选择按钮重置。最终按真实文件选择器、公开DOM完成状态及真实清理响应严格复验。没有已证实的生产缺陷，也没有本例生产代码变更。结构检查PASS仅为结构证据。

当前及早期运行临时源件已清理或证实未产生服务器上传，见 historical-cleanup-review.json；原始失败JSON不改写为PASS。四份正式候选、任务账号、项目、类别、矩阵及签名配置按Root要求保留审查。本例浏览器已关闭；未重启服务、未运行API/SQL业务动作、未改共享岗位或任务assignee、未提交推送。私有DPAPI凭据只留本机，禁止进入Git。

最终隐私扫描与收尾状态见 evidence-privacy-20261017.json、closeout-status-20261018.json。用户明确禁止Git提交/推送，正式completed另受仓库收尾规则约束；业务PASS与收尾状态分开记录。

最终隐私扫描PASS：48403个文件/ZIP成员，未检出当前已知凭据字符串，未输出凭据值。浏览器及逐分支trace均已关闭。cleanup preview exit0，apply因blocked状态exit3，未删除任何文件。

## 第二轮新重跑结果

17分支本轮均有PASS证据，首遍16 PASS/1 BLOCKED，TYPE-CANCEL独立复测1 PASS。间歇分类解析错误E2E08-R2-01保留OPEN_INTERMITTENT。详见rerun-20261020-handoff.json和rerun-20261020-round2-1792462063637/report.md；20份trace隐私/CRC扫描PASS。两次runner退出0且进程不存在，浏览器关闭、context=0。只进行了真实前端验收及本例测试资产校准，无产品/公共配置/数据库/Git/服务/子Agent操作；未启动下一轮。工程closeout因禁止Git独立blocked。历史ROOT_ACCEPTED仅属于历史运行，本轮不继承该复核结论。

## 分类首次加载根因诊断交付

本轮仅生产代码只读+本例竞态行为测试。实际生产onMounted并行启动基础类别目录和类型候选；正式分类解析先返回时，availableCategories为空产生唯一分类错误；目录后到不会触发重新解析，categoryId持续null。历史失败自然响应事件active-category(8)早于file-categories(9)，与此时序一致。根因报告classification-race-diagnosis-20261020/diagnosis-report.md；RED退出1（3 PASS/1 FAIL），诊断对照退出0（4/4），后者不是产品GREEN。未开启浏览器，无API/DB业务动作或响应体读取；无Git/构建/重启/子Agent/产品文件修改。Upload由02独占，待Root串行安排初始目录完成屏障；问题保持OPEN，历史BLOCKED不变。

## 分类竞态串行修复验证（2026-10-20）

Root串行放行后，Upload类型框增加初始类别目录loading/disabled及目录失败禁用门禁，产品diff仅+6/-1，原解析器不变。新专属业务RED 4 PASS/2 FAIL，修复后GREEN 6/6，三个相邻单测26/26，共32项行为测试PASS。旧静态合同的Number比较正则在精确修复前后均失败，属于PRE_EXISTING，不能记为PASS。测试VM首次缺Boolean的校准过程原样保留。

精准diff、全部命令、原始结果和SHA256见classification-race-fix-20261020/report.md及source-verification.json；最新机器交接repair-20261020-fix-handoff.json。用户明确本轮不开浏览器、不完整E2E；真实页面验证推迟到Round2，不计E2E PASS，历史FAIL/BLOCKED保留。现hold等待，未构建重启、提交推送、使用API/DB或子Agent。工程收尾因本轮Git禁令blocked，未执行cleanup apply。

## 正式第二轮 rerun-20261020-r2-round2

用户本轮正式EXECUTE，实际int_qms及协调运行证明已核对并写gate.json；本轮新证据独立，原17分支全部执行：完整关联7 PASS，属性2 PASS/1 FAIL，名单/类型6 PASS/1 FAIL，共15 PASS/2 FAIL/0 BLOCKED，整体FAIL。分类首次加载另专项PASS（首次P1/T1即正式分类与路线就绪，无重选）；本轮自然响应目录先于分类解析，不声称覆盖强制迟到时序。业务断言均真实UI，非历史PASS/接口JSON。

E2E08-R2R2-01 OPEN_UI_DISPLAY：ATTRIBUTE-ONLY-INNER-CANCEL和DISTRIBUTION-ONLY-CANCEL取消后项目显示294，首次截图与DOM一致，其余快照字段保持。两首次FAIL原样保存，无校准重跑；收到有界等待建议时原场景context已经关闭，因此持续性尚未确定。静态推断候选列表替换与恢复旧项目ID有关，未经进一步证明且未修改产品。

逐17项、首次失败、业务终态、专项、截图/trace及所有关闭证据见rerun-20261020-r2-round2/handoff.json。runner PID12732退出0（不能代替业务PASS），browser已关闭、context=0、本例六个登记进程全消失。19份trace与7482项隐私/ZIP CRC检查PASS；临时源件由页面全部清理，保留正式基线。未改产品/构建/重启/提交推送/DB或启用子Agent；HOLD，不启动第三轮，工程closeout独立blocked。

最终归档复扫：隐私/ZIP CRC PASS，19份trace、7485项；六个冻结前端文件哈希全部保持，任务结构核验PASS。未启动新业务轮次。

## E2E08-R2R2-01项目名称取消回填定位

按本轮授权只读核对两个失败的截图/DOM/trace及当前int_qms源码，写最小BDD后执行node --test doc/tasks/20261016-dcc-e2e-08/project-label-diagnosis-20261020/project-label-cancel.test.cjs，退出1：2 PASS/2 FAIL。测试使用生产SFC函数、Vue响应式和实际Element Plus标签查询/卸载缓存回调；远程P2候选在确认期间替换后，两个取消分支均恢复ID294但丢失正式映射、显示294，其他表单值保持；未过滤候选对照均PASS。首次脚本标签正则提取失败仅属harness，原日志保留，修正SFC解析后获得有效RED。

结论是产品选中项目映射缺口，不能通过放宽脚本断言解决。原trace网络流为空，未声称还原精确历史响应时刻；原页面已关闭，症状持续性仍未确定。只提出局部5hunk +8/-1 diff（保留已接受正式项目对象，成功搜索时维护显示映射，接受/重置时更新清空），未应用或GREEN。详见project-label-diagnosis-20261020/report.md、handoff.json、red-observations.json。等待Root共享Upload串行窗口，仅建议两个失败路径及必要边界GREEN，真实UI只跑失败分支另行放行。没有浏览器、已通过分支重跑、产品/公共矩阵修改、构建、重启、Git或清理；原首次FAIL不改写，test进程已退出。

## Root串行放行项目标签局部修复

用户明确E02已HOLD，批准E08只修复取消后项目名显示ID。先读完整工作区diff及E02交接，精确快照upload-before.vue保存原17+/2-等已有改动。只实施五处局部+8/-1：保留已接受正式项目对象、成功搜索结果维护其选项、接受新项目更新对象、清空/完成重置清除。请求参数过滤、矩阵、提交规则、E02属性编辑/capture恢复及类别初载修复不变。

BDD沿用两取消路径并补接受/清空/失败边界。命令node --test doc/tasks/20261016-dcc-e2e-08/project-label-fix-20261020/project-label-cancel.test.cjs，原有效诊断RED已先于修改；本轮扩展RED 3 PASS/4 FAIL退出1，GREEN 7/7退出0。清空测试VM缺message.confirm仅属harness校准，保留原输出；校准后用E2E08_SOURCE=upload-before.vue精确快照复核RED，再对当前源GREEN，未放宽断言。GREEN后停止所有测试执行。python verify-delivery.py核对仅五处授权字节替换，所有旧改动及8份原首次失败证据完整保留。

最终精准diff、命令、结果、字节SHA877479ffcfbe4f9266094d0305f9ae9e0df2458c107729c3a79d3974eb769530、规范LF哈希和后续建议见project-label-fix-20261020/report.md、handoff.json、source-verification.json。测试进程已退出；无Playwright/浏览器/业务重跑、Maven/build、重启、Git或清理。HOLD，后续真实UI仅建议Root另行放行两个原失败分支，并同页有界观察；原FAIL不改写，工程closeout因Git禁令blocked。

2026-10-20 16:34：project-label-ui-20261020 定向真实 Playwright 复验 2 PASS / 0 FAIL / 0 BLOCKED，runner exit 0。两次正常登录、菜单进入，实际自然加载 SFC SHA 均为 877479ffcfbe4f9266094d0305f9ae9e0df2458c107729c3a79d3974eb769530。属性第二层取消后 5055ms、仅下发修改取消后 5052ms 采样均保持 P1 正式名称与逐字段原值。原 FAIL 的 8 个文件哈希未变；2 trace CRC/隐私扫描 PASS；终点截图人工查看。浏览器已关闭/context0，自有 runner+4 浏览器进程均退出。前后服务 PID 和源码 SHA 一致，窗口交回 Root。
状态里程碑：实现与本轮定向验证 ready_for_closeout；因当前 Git/清理禁令，最终工程状态 blocked，不执行 preview/apply、不写 completed。其余 15 项未复跑；本轮没有生产代码修改、构建、重启、Git 或业务清理。结果与边界见 project-label-ui-20261020/report.md、handoff.json。


## Root 接受定向修复复验（2026-10-20）

Root 已 review 并接受 ATTRIBUTE-ONLY-INNER-CANCEL、DISTRIBUTION-ONLY-CANCEL 两项当前修复真实页面复验。旧失败现存，新独立复验2PASS；旧 rerun-20261020-r2-round2 仍为15 PASS/2 FAIL，原 result/handoff/截图/trace 八文件 SHA 全部保留。新 project-label-ui-20261020 为独立2 PASS，非新跑完整17项；其余15项未复跑。模块自然加载源码指纹、5秒采样/截图、trace CRC/隐私、PID closure 已获Root接受。最新交接：[handoff](project-label-ui-20261020/handoff.json)。

本轮仅文档/交接及协调台账同步；按本轮明确授权，仅为本例向协调台账08项追加 repairRevalidation，覆盖原“Root独占协调目录”的本次必要例外，其余项及旧计数保持。结构与八文件SHA检查后HOLD；不操作浏览器/业务数据，不重跑其余15项。工程closeout保持blocked。


## R3第二轮独立完整合同结果

结果：PASS，17 PASS / 0 FAIL / 0 BLOCKED，共17分支。本轮全部真实页面新执行，不复用旧PASS。分类首次加载专项 PASS。

当前运行证明 doc/tasks/20261016-dcc-24-e2e-coordination/rerun-r3-runtime-proof.json；2095来源文件哈希核验一致，8061/PID20976、48061/PID41508、Jar709e78bd。原作者芋道源码/DCC241016C08AUTHOR，各业务分支fresh context真实登录。

|分支|状态|证据目录|
|---|---|---|
|E2E-08-PROJECT-CANCEL|PASS|doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-linked|
|E2E-08-PROJECT-INNER-CANCEL|PASS|doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-linked|
|E2E-08-PROJECT-CONFIRM|PASS|doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-linked|
|E2E-08-TYPE-CANCEL|PASS|doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-linked|
|E2E-08-TYPE-CONFIRM|PASS|doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-linked|
|E2E-08-OTHER-CANCEL|PASS|doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-linked|
|E2E-08-OTHER-CONFIRM|PASS|doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-linked|
|E2E-08-ATTRIBUTE-ONLY-CANCEL|PASS|doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-contract|
|E2E-08-ATTRIBUTE-ONLY-INNER-CANCEL|PASS|doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-contract|
|E2E-08-ATTRIBUTE-ONLY-CONFIRM|PASS|doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-contract|
|E2E-08-SIGNOFF-ONLY-CANCEL|PASS|doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-contract|
|E2E-08-DISTRIBUTION-ONLY-CANCEL|PASS|doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-contract|
|E2E-08-TYPE-CANCEL-NO-RELATION|PASS|doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-contract|
|E2E-08-TYPE-CONFIRM-NO-RELATION|PASS|doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-contract|
|E2E-08-OTHER-CANCEL-NO-RELATION|PASS|doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-contract|
|E2E-08-OTHER-CONFIRM-NO-RELATION|PASS|doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-contract|
|E2E-08-PROJECT-CONFIRM-NO-RELATION|PASS|doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-contract|

首次监控错误把短导航GET中止判阻塞，原17项未跑result保留；校准监听耗时后执行合同。Python UTF-8读取脚本首次失败亦未触发浏览器，后显式UTF-8修正；不计业务失败。完整合同阶段P1正式A/1列表ACTIVE，但原5秒canvas未出现，7关联分支暂BLOCKED保留；一次同对象只读核验显式等待45秒，两基线非空正文通过，之后仅续执行原未跑7分支，未复跑已PASS10项。

详细原错误、DOM、实际基线fileId/版本、每分支逐字段前后值、临时源件/清理/残留见handoff和两个原始result。合同只测试切换及临时上传，不正式提交/签名/创建新版。现有A/1用于关联，不被重新创建。所有业务均Playwright正常UI，无API/DB业务动作或JSON oracle。

所有浏览器/context/runner已关闭，trace CRC及凭据扫描PASS，索引见handoff.closureIndex与evidence-index.json。原R2 FAIL及8文件SHA不变，旧未知事故不因本轮结果被关闭。未改产品/数据库/公共配置，不构建/重启/Git/子Agent，不改协调台账。工程closeout仍blocked，HOLD等Root。

最终结构核验PASS：17唯一分支、notRun0、residual0、21trace/8166扫描项隐私CRC通过、全部资源关闭；2095当前源码快照结束复核一致。首次监控误阻塞和5秒预览失败保留。最终handoff为本轮分阶段17项新执行汇总，三个原始result不改写。HOLD。
