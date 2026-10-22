# DCC 详情精确版本隔离修复

## Task Goal

修复 DCC 历史版本、申请记录和审批任务打开详情时的精确版本隔离：正文、附件、关联引用、审批记录和状态必须绑定来源版本/申请/任务；当前关联与历史关联明确分区；服务端按精确对象授权，不依赖前端隐藏。

## BDD

- BDD: 从列表、搜索或正文打开历史版本 -> Given 用户选择历史版本 V1 且当前正式版本为 V2 -> When 详情页加载 -> Then 正文、附件、关联引用、审批记录和状态均属于 V1，不能自动换成 V2。
- BDD: 从审批申请/任务打开详情 -> Given 申请轮次 R1 或审批任务 T1 指向版本 V1 -> When 打开详情 -> Then 查询上下文固定为 V1/R1/T1，陈旧响应不能覆盖新上下文。
- BDD: 当前与历史关联分区 -> Given 版本 V1 有历史关联且当前版本 V2 有当前关联 -> When 查看关系面板 -> Then 两组关联分别展示，不能互相补齐或推断。
- BDD: 精确对象授权 -> Given 用户仅有 V1/申请 R1 的查看权限 -> When 请求 V2 或其他申请的正文、附件、关系或审批数据 -> Then 服务端拒绝，不能由 UI 隐藏替代授权。
- BDD: Long ID 与陈旧响应 -> Given 版本/任务 ID 超过 JS 安全整数或旧请求晚返回 -> When 路由切换/响应返回 -> Then ID 以字符串保持精确，旧响应被丢弃。

## Milestones

1. 复核现有规则、HEAD、G10/G11 和并行改动，建立基线。
2. RED：补充或运行精确版本隔离契约，记录当前失败/缺口。
3. GREEN：修复前端上下文传递、竞态防护和后端精确查询/授权。
4. REGRESSION：运行 DCC 相关定向测试，记录未执行 E2E/服务运行限制。

## Expected Verification

- 前端详情、关系、申请历史、附件和审批任务静态/单元契约测试。
- 后端 detail/read、application round、relation、attachment、approval binding 定向 Maven 测试。
- 不执行 E2E、服务重启、数据库写入、Git 提交或推送。

## 续办授权与 BDD（2026-10-15）

- 沿用本目录；Maven/shared target/fulltypes/build 由 Root 统一。本线程不重跑上轮 43/149 套件，不执行私有 javac/JUnit；只运行本轮新增 Node 定向测试。
- 独占 detail/index.vue、BrowseRecords、DetailApplicationHistory、DetailRelationsPanel、SelectedDetailService 及相关测试；Controller/workflow.ts 仅维护既有精确详情区域；view/index.vue 与 reading API/service 只读。
- Root 交付实际 Maven 7 类 115/115（含 SelectedDetailService 7 + HTTP 3）；上轮 reading 缺依赖编译阻塞已解除，旧失败记录保留为历史。
- BDD R01 -> Given canonical wire 同时携带明文 BPM/task/type（相同或不同），When 路由解析或详情 wrapper 被调用，Then 精确拒绝且无 HTTP 请求，不静默丢弃任一来源。
- BDD R02 -> Given context 为 null、错文件、损失精度 Long，或仅给申请类型/任务未给 BPM，When 详情 wrapper 被调用，Then 请求前明确失败，不能变成普通详情或推断 native 轮次。
- BDD R03 -> Given 详情解析得到合法历史 source，When 加载详情并构建正文/阅读会话来源，Then GET/body metadata/binary/reading header 共享同一 canonical wire，late 响应仍受原 ID/route/generation 约束；附件/分发不冒充普通正文阅读来源。
- F01 追加授权：可在 Query interface/Impl 增加显式 context 的精确详情窄入口，原 generic getControlledFile 不变，保护 assertPreviewReadingAccess；生产 GREEN 必须等 Root 执行下列新增行为 RED，后端不自行运行。
- F01 最新授权覆盖上述等待要求：保存改前最小 hunk 后可先完成 GREEN 候选；Root 在 Java 冻结后仅恢复 reader 一行执行真实 RED，再恢复 GREEN。本线程不执行后端验证，也不把未执行 RED 写成 PASS；回放点见 f01-red-green-replay.md。
- BDD F01 -> Given 已激活 ACTIVE A 与同 Master 且同为本人申请人的 WORKING W，When 经精确 reader 以 DAILY 读 A，Then A 正常可预览、W 的 canPreview=false 且动作不含 PREVIEW，其他元数据/管理动作保持原投影。
- BDD F02 -> Given 同一 W，When DAILY 读 W 或合法 AUTHORING/任务/历史 context 读对应文件，Then 前者由真实 Guard 拒绝，后者按原 context 放行，不套 DAILY 过滤；generic getControlledFile 仍保留原编制投影。
- BDD F03 -> Given 根行和历史项 ID 不同，When 构建精确详情链，Then 不把根 context 的 ThreadLocal 套入整个历史遍历，也不改变 reading 授权 hunk。

## 设计约束检查

- 只处理本任务资产，不回滚并行改动。
- 不用 fallback、当前版本替代、UI 隐藏或猜测申请轮次。
- “工序开始”“批记录表单”“表单槽位”链路不互相补齐。
- 当前关联与历史关联必须有独立查询/展示语义。

## 本轮已加载事实后续动作 BDD

- 已关闭C01-C06、200/200、types/build保留为前轮证据。本轮不重跑78/200全集，不猜测缺失原文。
- BDD L01 -> Given 引用/关系/申请轮次已在身份A成功加载，When actor或原始base/visitTenant变为B且props/route不变后再调用preview/select，Then 请求前拒绝复用A事实，不以B发新请求后把A列表视为已授权。
- BDD L02 -> Given 详情已成功加载且computed current已缓存，When 原始身份变化后打开正文/历史/BPM/详情，Then 实时核对已加载身份；rawversion/wire/BPM/task保持精确且不自动当前。
- BDD L03 -> Given 当前错误存在或load/preview请求在途，When close/unmount或refresh后旧请求返回，Then 不在关闭实例发新请求、不恢复旧错误/页面，新刷新已获授权可继续原固定版本。

## 新一轮未覆盖边界复核

- 继续本任务与现有线程；int_qms/f06833f06，Root125/181/完整types为上一轮证据，不重记为本次执行。
- Root执行2635输入完整Vite构建期间，本线程生产前端只读；仅可新增专属测试/BDD/文档，等待解除冻结再修复。所有Maven/shared target/fulltypes/build仍归Root。
- BDD S01 -> Given 同一路由、同一大Long文件、同一用户，旧详情请求仍在途且访问租户从缓存T1切到T2、当前页面刷新/卸载尚未结束，When T1成功回包或失败回包返回，Then 不发布T1文件/权限/签名，也不在T2页面通知T1错误；不能只依赖非响应式tenant缓存的watch。
- BDD S02 -> Given 签名留痕请求在T1发出，When 同文件/路由/分页不变而租户缓存已是T2，Then T1签名晚回包不能成为当前列表。
- BDD S03 -> Given 账号ID在同路由真实变化，When 实际同步watch触发reload，Then 新读取不被旧账号成功/失败回包覆盖；作为S01的相邻正向控制。
- BDD S04 -> Given 旧作废申请wire的BPM/task与文件native上传BPM不同，When 从详情返回BPM，Then 跳回wire的原申请/任务，不读取native替代。
- Root已接收真实租户RED并解除生产构建冻结：可在本线程详情归属内做最小GREEN；Query/其他owner文件仍不写，后端不执行。
- R05追加归属：Root已将relations/DccReferenceUsageDialog.vue及其新增专属测试唯一交本线程。仅新边界/该Dialog受影响旧套件，不重跑58全集；四个详情Vue尽量不再改，不写browser无关区域。
- BDD R05-A -> Given Dialog的source/props/context-key不变且load/preview在途，When actor、baseTenant或visitTenant之一切换后旧成功/失败回包到达，Then 不发布旧page/打开旧正文/显示旧错误，且不能只比较generation。
- BDD R05-B -> Given 身份不变且固定大Long版本获准，When load与preview完成，Then 保持原固定ID/raw版本canonical历史wire；当前真实权限拒绝仍显示，非法Long在请求前拒绝，不自动新版。

## Current Status

blocked — 本轮L01..L03自有缺口已实际RED/GREEN并冻结交还：6文件72/72PASS、0skip；4生产Vue lint exit0/0error，仅两条既有warning。绑定loadedAuthority并实时检查后续动作，保留portal原raw/wire/BPM/task，关闭/卸载旧提示与刷新受控。仅工程Git提交推送未授权不能completed；附件组件loaded-scope静态交Root/附件owner，本线程未改其源码或宿主。旧78/200/types/build不冒充本次执行。

## Cleanup Keep

- doc/tasks/20261015-dcc-detail-exact-version-isolation/task.md
- doc/tasks/20261015-dcc-detail-exact-version-isolation/execution-log.md
- doc/tasks/20261015-dcc-detail-exact-version-isolation/verification-report.md
- doc/tasks/20261015-dcc-detail-exact-version-isolation/run-targeted-junit.py
- doc/tasks/20261015-dcc-detail-exact-version-isolation/SelectedDetailJUnit.java
- doc/tasks/20261015-dcc-detail-exact-version-isolation/f01-red-green-replay.md
- doc/tasks/20261015-dcc-detail-exact-version-isolation/handoff-fingerprints.json
- doc/tasks/20261015-dcc-detail-exact-version-isolation/review-handoff.json
- doc/tasks/20261015-dcc-detail-exact-version-isolation/authority-boundary-followup.md
- doc/tasks/20261015-dcc-detail-exact-version-isolation/authority-handoff.json
- doc/tasks/20261015-dcc-detail-exact-version-isolation/r05-handoff.json
- doc/tasks/20261015-dcc-detail-exact-version-isolation/loaded-fact-followup.md
- doc/tasks/20261015-dcc-detail-exact-version-isolation/loaded-fact-handoff.json
