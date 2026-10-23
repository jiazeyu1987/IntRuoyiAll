# 执行日志

## 本轮：身份在首次点击前变化的 loaded-fact 窗口

- 读取详情 owner 的 loaded-fact-followup.md；核对 int_qms 与根/收尾/前端规则。Root 接收上轮 19 并独立 36 PASS，仅记历史回执。
- 本轮 BDD 已写 task.md：file 在 T1 接收而身份于首次点击前切 T2，preview/download 必须在确认/API 前拒绝并清旧选择；同身份及显式新 file 对象刷新保留合法操作。先新增真实 SFC RED，不修改其他 owner 文件。
- 实际 RED：`node --test --test-name-pattern='loaded T1|same authority remains' tests/unit/dcc-attachment-download-disposal.test.cjs` exit 1，3 项 1 PASS/2 FAIL、0 skip。旧事实点击 previewVisible=true（期望 false），旧下载 confirmation=1（期望 0）；合法同身份/显式刷新对照通过。
- 最小修复仅组件：loadedAuthority 由 props.file 对象首次接收/明确替换时绑定；不在 auth/fingerprint watch 重绑旧数据。preview/download 在任何业务确认/API 前核对 loadedAuthority，错误时失效 generation、清旧选择/预览/下载并明确要求刷新。
- 同一新边界命令 GREEN exit 0，3/3、0 skip；追加同一 file 对象内继续改附件事实仍不得重绑断言后，仅执行原 3 文件受影响组，exit 0，22/22 PASS、0 skip。身份变化分别覆盖 actor/base tenant/visit tenant；同身份与新对象刷新正向对照保留。没有新 API、无父页/viewer/后端改动。
- 最后 GREEN 后组件和专属测试冻结；只继续任务证据归档，最终 SHA 保存 loaded-authority-review.json。Root 之前 36 PASS 不当作本修复后执行。
- ready_for_closeout 下结构/指纹和 diff whitespace 检查通过；cleanup preview/apply 各 exit 0，keep 10/delete 0/warnings none。最终 blocked 仅为 Git 提交/推送禁止，无定向软件待修项；生产/测试冻结保持。

## 最新轮次：组件保留时的新状态边界

- 核对 int_qms/f06833f06，读取 AGENTS/收尾/开发规则；上轮 200/200、类型/build 为历史有效证据，本轮不重跑。C01-C06 不重开；缺失原文第2尾/第6/8及旧文档冲突不猜测。
- 写入同页事实变化、并发确认 BDD 后新增专属测试：实际 RED 7 项 5 PASS/2 FAIL；分别为旧清单变化仍 requests=1、双击 requests=2。补 props 未变的 actor/tenant 缓存变化 RED 后 8 项 5 PASS/3 FAIL，身份变化仍 requests=1。
- 最小生产修复：组件下载锁前移到 ElMessageBox 前，重复点击零请求；事实指纹包含 raw version、完整附件清单、下载权限、actor 和两个 tenant 缓存；响应式变化同步 generation 失效，确认后/回包后直接重读指纹。保持卸载保护及旧请求 finally 的 generation 限定。
- 初次合并验证 16 项 11 PASS/5 FAIL，5 个失败均为本组件专属回归 fixture 未提供新 auth/store import，非业务失败；仅在同组件专属宿主加两行显式端口，原测试名/断言未改，随后 16/16 通过。
- 补同 ID 新对象、原地权限变化、旧预览关闭/旧回包不释放新锁、纯 tenant 缓存改变后零保存对照，最终同一 3 文件 canonical 命令实际 exit 0，19/19、0 skip。未运行旧业务大套件或后端/fulltypes/build。
- 上传 ready/清理失败保票/retryCleanup/markSaved 及服务票据事务只读未见新缺口；不触碰后端/Query/workflow/viewer/详情父页。最终变更与 SHA 归档 retained-state-review.json。
- 最终结构/JSON/三份自有源码稳定性检查 PASS；原组件宿主去掉新增两行 auth/store 端口后，UTF-8/LF SHA 与前次完整文件完全相同，证明原断言未改。ready_for_closeout 下 cleanup preview/apply 各 exit 0，keep 9/delete 0/warnings none；最终 blocked 仅为 Git 提交/推送禁止，更广集成验证由 Root 决定。

## 本次继续：未覆盖边界只读检查

- 再次核对 int_qms/f06833f06，读取 AGENTS/收尾/后端/前端规则及原报告。Root 上轮 125 后端、181 前端和完整类型检查仅作为用户提供的历史证据，本轮未执行这些命令。
- SelectedDetailReadService 当前调用 `query.getControlledFile(actor,file.getId(),context)`，原 ID/raw version 校验在投影前后保留；Query overload 仅 DAILY 收口 PREVIEW，附件和 manifest 仍由正式 toRespVO 原分支生成。附件服务/Revision/检入/确认组件继续只读。
- 新边界候选：ControlledFileAttachments.vue 的失效保护只在 props watch 增加 generation；组件卸载没有增加 generation 或清理选择。其异步确认和文件回包仍可能在卸载后命中相同 token；本轮先建立专属实际 Vue SFC RED，不改公共生产文件或他人测试宿主。
- Root 当前执行 Vite 构建，所有生产前端保持只读；独占服务也未修改。
- 只读检查发现：download 在确认框前不设置 downloading，理论上允许第二次点击并行确认；props watch 非 deep，原对象原地改变附件行/清单可能不增加 generation。先新增专属 RED，未假设 auth/tenant 变化一定能被组件观察。
- 实际 RED：`node --test tests/unit/dcc-attachment-download-disposal.test.cjs`（cwd IntRuoyiFronted）exit 1，2 tests/0 PASS/2 FAIL/0 skip。真实 SFC 卸载后接受旧确认仍 requests=1；已发起请求的旧响应在卸载后仍 browser saves=1，均期望 0。属于新覆盖的公共组件生命周期缺口，不是环境失败，不把它写成已通过或服务端越权。
- 未实施生产修复；先交 integration-note-download-disposal.md，待 Root 分配公共组件 owner/解除构建冻结，再执行新用例 GREEN 与受影响组件回归。本线程未改公共组件、其他 owner 文件或测试宿主，未重跑旧业务套件。
- 指定的其它零提交、确认取消/关闭、来源小版本、首次申请轮次、历史培训不复制及 SelectedDetailReadService manifest 投影边界已只读核对，详细源码依据写入 verification-report.md 当前章节；没有以缺历史 hash 作为新需求或恢复已退役 API。
- 对照补强后新专属同一命令：5 项、3 PASS/2 FAIL、0 skip；取消/关闭保留双附件且零请求，合法当前版本下载和真实服务器文件名保留，撤销权限零请求均通过；两个卸载分支仍真实 FAIL。旧 hash/API/权限测试宿主没有修改。
- Root 随后明确解除生产前端冻结，并将 ControlledFileAttachments.vue 与专属测试唯一转交本线程；允许在该文件增加最小卸载 generation 失效和状态清理。Root 修改前 Vite exit 0/2635 零漂移/3239 产物仅记录历史回执，不当作本修复后验证。
- GREEN：同一专属 Node 命令实际 exit 0，5/5、0 skip；两个卸载分支均正确拒绝旧操作，三个对照保持成功。
- REGRESSION：仅 `node --test tests/unit/dcc-shared-attachments-component.test.cjs tests/unit/dcc-attachment-download.test.cjs` 实际 exit 0，8/8、0 skip。原精确身份、真实文件名/审计头、JSON 错误、无权限和跨版本拒绝断言全部保留；未改旧测试宿主。
- 唯一生产改动为 ControlledFileAttachments.vue 的 onBeforeUnmount import 与三行卸载失效钩子；AttachmentService/RevisionService/Query/browser/DetailApplicationPanel/SelectedDetailReadService 没有本线程修改。没有运行后端或全量类型/构建，后续统一验证由 Root 决定。
- RED 快照后观察到 viewer/index.vue 与 reading-lifecycle.ts 由阅读 owner 并行改变，已在 focused-boundary-review.json 标为 PARALLEL_CHANGE。本线程未编辑它们，不把当前附件组件/SFC 定向结果当作 viewer 全量验证；新生产组件和专属测试的最终 SHA 单独交 Root。
- 最终本轮文档/JSON/自有最终指纹通过；ready_for_closeout 下 cleanup preview/apply 均 exit 0，keep 8/delete 0/warnings none。当前 Expected Verification 已同步为新 5 项 RED/GREEN 与受影响 8 项，旧 211/129 和 Root 125/181/类型/修改前构建不再作为本线程重复门禁。最终状态 blocked 仅为 Git 禁止收尾，无定向软件待修项；Root 集成验证由 Root 决定。

## 继续任务：阅读计时与精确详情合并复核

- 本轮再次核对分支 `int_qms`，读取根 AGENTS、task-closeout-rules、后端/前端相关规则及前次三份任务报告。未新建重复任务目录。
- 用户提供 Root 独立 7 类 115 PASS（含附件 DB 41）、前端 10 文件 77 PASS；不将其计为本线程执行，不与前次 211/129 相加，不重跑已经通过的套件。
- 本轮仅静态复核当前工作区合并影响，禁止 Maven/shared target/fulltypes/build、E2E/实库/服务/Git/remote/subagents；受限父页和阅读 viewer 只读。
- 前次 RED/GREEN、收尾清理与源码观察保持历史证据，当前复核的新指纹单独保存，不覆盖前次 validation-sources.json。
- 本轮只读 Root 现有七份 Surefire XML：27+11+41+3+7+23+3=115，均 0 FAIL/ERROR/skip；前端 77 仅采信用户的 Root 执行回执。不运行 Maven、私有 JUnit、Node 业务测试或其它已过套件。
- 当前 33 份文件原字节及 UTF-8/LF SHA 写入 adjacent-integration-review.json，保留旧快照。核心附件服务、Revision、Workflow、上传、检入组件/父页、确认面板/manifest helper、共享附件组件及自有测试保持前次快照；变化集中于相邻阅读和精确详情接线。
- 已核对：阅读 lifecycle 对 DCC_CONTROLLED_FILE_ATTACHMENT 直接不创建正文计时；附件 metadata/binary 仍走双 ID 专属路径。精确详情由原 ID 读取并验证原 raw version，不从 Master 或当前 ACTIVE 推断附件；共享组件切换立即清旧状态、取消/迟到下载不写新详情。
- 已核对：Query 新增阅读鉴权和日常 PREVIEW action 收口没有修改检入 hash/全票绑定/保存响应；Revision 仍冻结有序普通附件事实，ordinaryRows 仍过滤掉历史 TRAINING_RECORD；确认/重提仍保留原完整清单和必须新正文门禁。未发现新业务缺口，不改生产或测试，不制造 RED，不再造 API。
- 范围内无 browser/DetailApplicationPanel 修改需求，view/index.vue 只读；相邻历史报告的未接线/旧 Maven blocker 已与当前源码和 Root XML 区分。
- 本轮文档/JSON 结构及 33 份复核输入稳定性检查 PASS，Root XML 115 计数与零失败/错误/跳过逐份确认；没有执行业务套件。ready_for_closeout 下 cleanup preview/apply 各 exit 0，keep 6、delete 0、warnings none；最终转 blocked，仅因用户禁止 Git 收尾，无软件域待修项。

## Baseline

- 分支：`int_qms`。
- 当前 HEAD：`f06833f06c868dc3a451eb63d01381a467ecfe92`；用户指定的 `a3b8234ec` 已包含在其历史中。
- 工作树已有并行改动：`IntRuoyiBackend/LOG_FILE_IS_UNDEFINED`、`LOG_FILE_IS_UNDEFINED` 及大量既有 `doc/tasks` 未跟踪资产；未回滚、未清理。
- 已读取：`AGENTS.md`、`docs/task-closeout-rules.md`、`docs/backend-development.md`、`docs/frontend-development.md`、`docs/database-rules.md`、`docs/e2e-rules.md`、`docs/worktree-restrictions.md`。

## BDD / TDD

- BDD 已记录于 `task.md`。
- RED：首轮 Maven 定向 207 项，169 PASS、1 FAIL、37 ERROR；重提子类初始化串入继承测试，真实验证夹具失败。
- GREEN：修复夹具并补完整来源轮次测试事实后，同一定向 Maven 命令 exit 0，211/211、0 FAIL/ERROR/skip。
- REGRESSION：最终 Maven 10 类及 Node 11 文件分别 211/211 和 129/129；均为当前源码 fresh 运行，不复用历史 PASS。

## 当前源码复核

- 检入前端 `browser/index.vue` 已阻断源文件/普通附件上传中、失败、清理失败，并发送有序 `attachmentUploadTickets`；成功响应缺普通附件清单时拒绝继续。
- 检入后端 `DccControlledFileQueryServiceImpl` 已计算并校验 `checkinPayloadHash`，绑定票据并要求保存清单；响应投影返回附件和 manifest hash。
- 升版服务 `DccControlledFileRevisionServiceImpl` 已从所选工作稿读取普通附件 manifest 并冻结至目标版本，且在精确版本/重放路径校验清单。
- 升版详情 `DetailApplicationPanel.vue` 已在确认文本中列出普通附件，并在确认前后比较附件 ID 与服务器 manifest hash。
- 已进一步核对：新建上传 buildUploadPayload 把全部附件票据纳入载荷；Query 与前端对 REJECTED/PENDING_APPLICANT_REWORK 强制新正文；检入持久化与响应清单由 requireSavedCheckinManifest/toCheckinRespVO 核对，正式候选由冻结清单服务核对。

## 当前验证与修复依据

- 纠正一次静态误判：`DccControlledFileFormEffectExecutor` 的旧映射方法未映射附件，但该入口 `execute/preflight` 已显式退役，不是当前可执行入口；不修改、不恢复该路径。
- 前端定向 7 文件实际 106/106 PASS，0 skip。
- 后端首轮 Maven reactor 定向 10 类：207 项，169 PASS、1 FAIL、37 ERROR、0 skip。附件/检入/升版等其余 9 类全部通过；38 个失败均为 `DccCustomRejectedCheckinResubmitTest` 中继承的父类用例被子类 `@BeforeEach customSources` 错套自定义 V6 准备，触发 `EXECUTION_CONTRACT_PREPARED_IDENTITY_CONFLICT` 或初始轮次断言错误。子类自己声明的驳回重提用例通过。此为验证夹具 RED，不能当作产品附件失败。
- 最小修复设计：保留全部 39 项发现和原断言，以 JUnit TestInfo 的真实方法声明类选择子类夹具；父类用例使用父类原清理，子类用例保留任务自有清理。不得用方法过滤/跳过继承测试制造通过。
- 并行变更观察：QueryService 的日常预览 action 收口和前端 view 页面出现其它任务修改；本任务未修改这些文件，后续 Maven 以当时当前源码编译。

## 最终执行结果

- 后端最终定向命令、前端 11 文件完整命令见 verification-report.md。本轮 Maven exit 0，23 个 reactor 项目 SUCCESS，211/211，0 FAIL/ERROR/skip；fresh 10 份 Surefire XML 已逐份校验非零测试数和 SHA256，保存于 verification-results.json。
- 前端最终 `node --test` exit 0，11 文件 129/129，0 FAIL/skip。最初 7 文件 106/106、新增用例后 108/108 仅为阶段结果，最终 129 为扩展至现有 manifest、摘要、详情附件、培训事实后的定向范围。
- 新增后端 4 个执行场景：两种状态（驳回/申请人返工）各测试“双附件+新正文成功且历史培训不复制”和“双附件不得替代新正文”；首次正向用例缺原绑定审批，补真实 H2 历史轮次事实与真实 DccApplicationRoundService 读取后通过。没有改产品门禁或用默认轮次掩盖缺失。
- 重提夹具修复后子类全部 39 项通过，父类继承方法和参数化断言均保留；未以方法过滤或 skip 绕过首轮失败。
- 新增前端 2 个用例：驳回/返工双附件不能绕过新正文要求；cancel/close 均保留正式双附件事实且零提交。
- 最终执行前观测 2227 份 DCC/前端输入。结束后发现 3 个并行页面漂移（ControlledFileBrowseRecords.vue、DetailApplicationHistory.vue、detail/index.vue）；DCC 后端输入、当前附件页面/组件/helper 及本任务四份测试没有漂移。完整路径在 verification-results.json；不声称全仓零漂移，不覆盖其它任务编辑。
- `git diff --check` 针对本任务四份测试与任务目录 exit 0。未运行全量类型/Vite 构建，因本轮未修改生产 TypeScript/Java；未将其它任务的生产改动纳入本任务结论。
- 已重读 task-closeout-rules；先置 ready_for_closeout 并进行 cleanup preview。Git 提交/推送禁止，最终必须 blocked，不能 completed。

## Closeout

- ready_for_closeout 状态下执行 `python <本机 task-closeout-cleanup/scripts/task_closeout.py> --task-id 20261015-dcc-multi-attachment-closeout --mode preview`：exit 0，保留三份任务 Markdown、verification-results.json、validation-sources.json；删除列表为五份当前任务临时 stdout 日志，无越界资产。
- 同状态执行相同脚本 `--mode apply`：exit 0，按 preview 清理五份临时日志。日志内 RED/GREEN/REGRESSION 结果、关键错误与正式命令已先归档本日志及 verification-report.md，XML 分项 SHA256 已保存 verification-results.json。
- 软件验证与清理完成后置 blocked，唯一工程阻塞为用户禁止本轮 Git 提交/推送；没有标 completed，没有将禁止操作当作产品失败，也没有请求执行这些禁止操作。
- 最后结构检查 exit 0：三份 Markdown、两份 JSON、blocked 状态、自有四份测试 SHA 不变、五份临时日志清理均通过。最终观测的并行漂移已扩展为 4 个其它页面与 5 份其它测试（精确路径见 verification-results.json）；第一轮结束时 3 页面漂移单独保留为 drift_at_first_test_completion。本轮 DCC 后端、附件组件/申请面板/helper、自有四份测试仍无漂移，不追改其它任务文件。
