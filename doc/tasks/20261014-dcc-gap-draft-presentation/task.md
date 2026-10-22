# G08 工作稿隐藏小版本号

## 最新软件验收回执（2026-10-15）

Root 已正式接受全软件域。依据本轮最终回执：后端最终有效47份class报告1008/1008通过，11865输入零漂移；前端F02 38/38通过，T04完整类型与B01构建通过，2429生产输入零漂移。生产和tests继续冻结，本线程不重复套件或扩大范围。下方旧阶段的失败、待接入和验收状态保留为当时历史证据，不覆盖本次最终接收。

## Goal

检入工作小版本在目录、搜索、正文、详情、升版选择、审批与历史页面统一仅显示“草稿”，正式版本显示保持不变；后台正文、申请、签名及升版请求继续使用精确 ID 和原始版本身份。

## Current Status

blocked — 软件验收已由Root正式接受；唯一工程未完成项为Git提交/推送收尾未获授权，因此不标completed。最终清理和报告由Root统一办理；生产/tests继续冻结。

## Scope and Authorization

- 可写：IntRuoyiFronted/src/views/dcc/controlled-file/shared/version-display.ts（新增）。
- 可写：IntRuoyiFronted/tests/unit/dcc-version-display.test.cjs（新增）及本任务目录。
- A03 新授：browser/presentation.ts、browser/index.vue、browser/ProjectFolderFileTable.vue、browser/ProjectFileRelationsDialog.vue、detail/presentation.ts、detail/DetailApplicationHistory.vue、revision/DccRevisionPanel.vue、detail/DetailRelationArrangements.vue、detail/DetailRelationStageEvidence.vue、relations/DccFileRelations.vue、relations/DccProjectReferences.vue（均在 IntRuoyiFronted/src/views/dcc/controlled-file 下）；对应专属测试及旧 raw 工作号可见断言可更新。
- 不可写：browser/detail 父页、ControlledFileBasicInfoPanel、ControlledFileBrowseRecords 及其它共享生产文件。
- A03 覆盖上条的 browser 父页禁止；仍不可写 detail/index.vue、ControlledFileBasicInfoPanel、ControlledFileBrowseRecords、DetailApplicationPanel、DetailRelationsPanel、view/presentation.ts、upload、公共 API。
- A03 调整：DetailApplicationHistory.vue 尚未改即暂停写入，改前副本和 hash 已归档；Root 转交 G11 接 helper 与验真轮次事件，本线程不并写。
- 禁止：子 Agent、Git 提交/推送、分支切换、worktree、E2E、服务操作、远端、真实业务库写入。
- Maven、全类型检查、大构建需 Root 唯一验证票；本阶段仅独立 Node 测试。
- A09 最新授权覆盖 A03：只写 logs/index.vue 的结构化版本格及专属真实测试；保留 revision/DccRevisionPanel.vue 的只读 label 范围，消费 G07/G01 正式只读字段，不写公共 API/后端。全部 browser 正式归还，其他 A03 文件冻结；generic 详情待 G10 交还。
- A12 追加：detail/index.vue 整文件仅剩 generic 可见 version/history/prompt 的 helper 接线和所属测试；不改 readContract/flow/A04 null/late-event/精确路由/权限/正文布局。BasicInfoPanel、BrowseRecords 仍 Root 保护；DetailApplicationPanel 为 G01 owner，只交精确显示 hunk。
- A15/A17 最新：detail/index.vue 已归还并转 G07，不再写入；全部 browser 已归还并转 G06。A17 新授 workbench/presentation.ts 仅已证明 WORKING 来源的影响升版候选版本显示及测试，不改 raw/比较/命令/ID/CSV/审计。
- A19最新：workbench/index.vue仅a17-workbench-wiring.md两个显示锚点获授；不改value/key/排序/defaultfirst/handler/其它布局，验证后整页归还Root。
- 最新review授权：原G08显示源继续冻结，仅read当前source/独立现有Node handler tests，3-5个当前场景，报告精确business gap；不写upload/Query/Panel/main API，不Maven/fulltype/E2E，不重跑118/43/11大suite。
- R08例外：DccRevisionPanel.vue仅字段名“是否在文控标准化后生效”→“生效方式”，及actual helper/SFC相邻测试；revisionEffectiveModeLabel由G03修改，不并写model/命令/授权/rawID。
- A35最新：仅测试文件IntRuoyiFronted/tests/unit/dcc-revision-target-confirmation.test.cjs解除冻结；加载当前实际production helper、明确合法隔离serverhash和两份saved附件fixture，保留原来源/目标/迟到/零写断言；新增缺hash和确认期间漂移。不改Panel/API/model或其它生产/测试源。
- A36覆盖A35：A35已Root接收并冻结；唯一回授IntRuoyiFronted/tests/unit/dcc-detail-application-ic05.test.cjs做旧声明宿主依赖适配，加载实际isPublicMatrixExecution/helper/真实Vue必要依赖，不改生产或旧业务断言。补依赖后若有真实产品RED只记录Root。

## Milestones

1. 核对 int_qms、规则和当前源码；保存改前指纹。
2. 冻结唯一显示合同，记录 BDD；测试原始展示行为获得真实 RED。
3. 最小 helper 实现 GREEN，运行相邻行为回归。
4. 形成 review-ready/changed-files/integration-notes/verification-report，申请分阶段转交。

## BDD

- BDD: 工作稿展示 -> Given A/1-2 工作小版本，When 任一页面将原始 versionNo 交给唯一 helper，Then 仅得到“草稿”，不包含 A/1-2。
- BDD: 生命周期独立 -> Given 同一 A/1-2 处于 WORKING、审批中、驳回或历史记录，When 展示，Then 均为“草稿”，不依赖 status 猜身份。
- BDD: 正式版本不变 -> Given A/1、AA/2 或配置的多段正式版本，When 展示，Then 保留原始正式版本文案。
- BDD: 精确身份 -> Given 两个 ID 不同且文案同为“草稿”的版本，When 调用既有正式升版命令或正文导航，Then 使用所选原始 ID，原 versionNo/签名 objectVersion 不被改写。
- BDD: 缺少展示事实 -> Given versionNo 未记录，When 展示，Then 明确显示“未记录”，不伪造“草稿”或某正式版本。
- BDD A03: 浏览/搜索真实渲染 -> Given 选中 A/1-2，When 实际目录名称/编号摘要与版本选择渲染，Then 只显示草稿；两个草稿选项携真实时间/说明并保持独立精确 ID，不默认第一份。
- BDD A03: 升版真实提交 -> Given 同名草稿选项，When 用户选择第二份并点击提交，Then 正式命令 selectedIterationId 为第二份精确 ID，raw versionNo 不改；未选择时不提交。
- BDD A03: 历史/关系真实渲染 -> Given 所选申请或固定关系工作版本，When 实际历史摘要/快照/项目目录/关联引用表显示，Then 草稿替代小号且正文/关系读取仍使用所选精确 ID。
- BDD A09: 日志结构化格 -> Given 日志 row.versionNo=A/1-2 且 summary/oldValue/newValue/detailJson 原文含 A/1-2，When 实际加载日志并点详情，Then 结构化版本格为草稿，审计原文逐字保留，精确请求 ID/raw row 仍原值；正式 A/1 正常。
- BDD A09: 升版逐稿辨识 -> Given 正式返回两稿 createTime/changeDescription，When 实际下拉显示并选第二稿，Then 各 label 使用自身真实时间/说明、无 A/1-n，命令仍第二稿 ID；字段缺失明确未记录，不造时间、不默认首稿。
- BDD A12: generic详情 -> Given 所选 A/1-2，When 实际基本版本格/历史列/身份摘要/审批提示渲染，Then 仅草稿，正式 A/1 不变；历史 ID、原始签名 objectVersion、轮次与请求保持精确，A04 同步 null 和 late event 隔离回归通过。
- BDD A17: 影响升版候选 -> Given 后端真实候选集合可含 WORKING A/1-2，When 只读 presentation 格式化该候选，Then 仅草稿并保留精确 ID 标识，正式 A/1 不变、原始 option不改；页面调用 hunk须 Root明确授 workbench/index.vue。
- BDD A19: 真实radio -> Given 影响候选有两份工作稿，When 原radio模板显示并从第一/第二份间切换，Then 可见草稿不泄漏A/1-n，label绑定和model值始终各自原LongID；既有默认首稿行为保持原合同，不在显示变更中重写。
- BDD R08: Given三种正式mode/null/unknown，When actual helper与revision SFC渲染，Then生效方式分别为延迟生效/文控审核后立即生效/按预设日期生效（历史申请）/未记录；DELAYED仍null日期，不改命令。

## Expected Verification

A36只运行canonical `pnpm --dir IntRuoyiFronted exec node --test tests/unit/dcc-detail-application-ic05.test.cjs`；先记录isPublicMatrixExecution等宿主缺依赖，不当产品RED；补依赖后保留全部现有28项（以实际数量为准）断言。仅单文件语法/UTF-8/前后指纹核验，不复跑A35/43/118，不Maven/fulltype/E2E。

A35只运行canonical `pnpm --dir IntRuoyiFronted exec node --test tests/unit/dcc-revision-target-confirmation.test.cjs`，原3项+新增2项；适配前ReferenceError属于fixture缺依赖，不叫业务RED。检查两个真实附件名称/ID/hash进入确认与payload，缺hash确认前拒绝、漂移确认后零write。单文件语法/diff/UTF-8核验即可，不重跑旧大套。

本次只读review范围：实际选择草稿原始ID与同标签、DELAYED空日期、ordinaryAttachmentManifestHash缺省/漂移、草稿恢复实际属性/部门/类型/关联、历史不换current；按当前源选择3-5项现有独立Node行为测试与只读handler探针，记录覆盖边界，不以字符串存在称全完。

- pnpm --dir IntRuoyiFronted exec node --test tests/unit/dcc-version-display.test.cjs：真实 helper 行为 RED/GREEN，含原对象不变和既有命令/导航的精确 ID。
- 相邻测试：dcc-browser-selected-version-display、dcc-browser-checkin-minor-only、dcc-exact-working-browser-scope；只报告本轮实际结果。
- 新增 helper 的独立严格 TypeScript 编译、格式检查；不冒充全量类型检查。
- 限定自身新增文件的差异/UTF-8/文档结构检查。
- 页面接线后需真实 Vue 渲染与精确请求行为测试；当前未接线不得宣称页面验收通过。
- A03 定向独立 Node：新增 dcc-version-display-wiring.test.cjs 实际 SFC/模板/handler；更新已授浏览与检入显示断言；运行相邻目录/关系/升版与历史测试。仍不运行全量类型/Maven/E2E。
- A09 定向独立 Node：dcc-version-display-a09.test.cjs 的日志完整 SFC 挂载、真实读取 wrapper/详情点击、审计原文/正式版本/失败边界，以及升版 label/真实选择命令。新变化重新取得 RED/GREEN，不引用 118 旧 PASS 冒充。

## 设计约束检查

- 已核对实际分支 int_qms，HEAD 822c31fc0856516cf59ba9cd84bf2bc7f455d17f；工作区有大量并行改动，不接触。
- 当前源码可见 raw versionNo，不能依据旧报告称已实现。
- 唯一纯显示函数仅接收原始 versionNo，返回文案；不改 VO、不创建新身份、不接受文案作为请求输入。
- 后端 DccControlledFileVersionPolicy.parse 定义工作稿为正式版本后追加正整数 -n；支持配置的多段正式身份。helper 不承担后端版本合法性或权限校验。
- 正式版本/已存历史标签原样展示；未记录明确展示，禁止 fallback、吞异常、假成功。
- 先等待 G10 交还详情、G07/G06 明确浏览排期；其它共享组件需另行列路径申请。
- 已读 AGENTS.md、docs/task-closeout-rules.md、docs/frontend-development.md、docs/powershell-encoding.md、docs/powershell-preflight-lessons.md、协调 requirements.md/coordination.md；复核版本策略和相邻测试。

## Cleanup Keep

- doc/tasks/20261014-dcc-gap-draft-presentation/review-ready.md
- doc/tasks/20261014-dcc-gap-draft-presentation/changed-files.md
- doc/tasks/20261014-dcc-gap-draft-presentation/integration-notes.md
- doc/tasks/20261014-dcc-gap-draft-presentation/baselines/DetailApplicationHistory.before.vue
- doc/tasks/20261014-dcc-gap-draft-presentation/a03-review-notes.md
- doc/tasks/20261014-dcc-gap-draft-presentation/a12-handback.md
- doc/tasks/20261014-dcc-gap-draft-presentation/a17-workbench-wiring.md
- doc/tasks/20261014-dcc-gap-draft-presentation/reachable-coverage.md
- doc/tasks/20261014-dcc-gap-draft-presentation/baselines/detail-index-a12.before.vue
- doc/tasks/20261014-dcc-gap-draft-presentation/baselines/workbench-index-a19.before.vue
- doc/tasks/20261014-dcc-gap-draft-presentation/review-followup.md
- doc/tasks/20261014-dcc-gap-draft-presentation/baselines/dcc-detail-application-ic05-a36.before.cjs.txt
