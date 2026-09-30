# DCC 三套流程代码实施与开发验证

## Goal

依据 `docs/dcc-three-workflows/` 的开发文档，逐步实现并验证 DCC 上传、升版、作废三套独立流程及部门负责人会签规则。本任务先从 P1 范围切入：三动作路由合同、会签矩阵只指定部门、部门唯一负责人解析、逐部门会签义务和任务创建时负责人快照。

## Milestones

1. 核对当前源码、测试和迁移边界，确认 P1 可落地的最小代码切片。
2. 记录 P1 的 Given/When/Then，并先运行目标测试取得 RED。
3. 实现部门会签候选解析、唯一负责人校验、同一负责人多部门不去重、三动作 key 的候选严格合同。
4. 运行定向后端测试取得 GREEN，并执行相邻回归。
5. 记录剩余 P2-P4 未实现范围、验证边界和后续门禁。

## Expected Verification

- RED：目标单元测试在实现前失败，失败原因对应“DEPT 会签候选未支持/同人多部门会签义务被合并/三动作 key 未纳入严格候选合同”等 P1 缺口。
- GREEN：受影响 DCC/BPM 后端测试通过，且执行数非零。
- REGRESSION：至少运行路线解析、BPM 候选严格校验和 DCC 签名/快照相关的相邻测试；若缺少运行依赖，按规则 fail fast 并记录真实 blocker。
- 数据库写入、服务重启、真实 Playwright E2E、远程操作、Git 提交或推送均须当轮明确授权；本轮已获授权，但最终业务 E2E 被租户 action 路线配置阻断。

## BDD

- BDD: 部门矩阵只保存部门 -> Given 会签矩阵配置生产部和质量部，When 保存新三流程的 SIGNOFF 配置，Then 配置中只持久化 deptId，不接受用户/岗位/角色候选，也不接受前端提交负责人 ID。
- BDD: 唯一负责人解析 -> Given 矩阵部门中有部门缺负责人或负责人无效，When 创建会签任务，Then 整批失败且不创建部分任务，不自动选择上级、管理员、申请人或第一个候选。
- BDD: 任务创建时冻结负责人 -> Given 创建会签任务时生产部负责人为 A，When 后续管理员把负责人改为 B，Then 已创建任务仍归 A 办理，历史快照显示 A。
- BDD: 同人负责多部门 -> Given 生产部和质量部负责人都是 A，When 创建会签任务，Then 生成两个不同 departmentId/obligationId 的义务，A 完成一个部门后仍需完成另一个部门。
- BDD: 三动作候选严格合同 -> Given 上传、升版、作废分别使用独立 processDefinitionKey，When BPM 计算候选人，Then 三个 key 都执行 DCC 严格候选校验，不进入通用空人补人或跳过发起人策略。
- BDD: 三动作待办识别 -> Given 上传、升版、作废使用 `dcc-controlled-file-upload/revision/obsolete`，When DCC 审批中心查询 TODO/DONE，Then 三个新 key 均被纳入 DCC provider 查询；FormCenter 作废仍按 objectId 定位文件，不把 `FORM_ACTION` 业务键当文件 ID。
- BDD: 三动作事件监听边界 -> Given 上传/升版新 key 流程结束事件与作废 FormCenter 事件，When DCC 状态监听收到事件，Then 上传/升版进入受控文件 finalization 监听，作废仍交由 FormCenter 效果执行器，不被 DCC 通用 listener 重复作废。
- BDD: 作废工作台定位 -> Given 作废流程实例 businessKey 为 `FORM_ACTION:*` 且流程变量包含 `objectId=6003`，When DCC 工作台加载作废审批待办，Then 前端使用只读 `businessObjectId=6003` 打开受控文件，不把 `FORM_ACTION:*` 当作文件 ID。
- BDD: 升版送审培训开关 -> Given 工作稿升版提交审批时请求 `needTraining=true`，When 服务端认领送审并创建 BPM 流程，Then 文件本轮送审记录和 BPM 变量均保存 `needTraining=true`，且幂等 payload 包含该值。
- BDD: 升版检入培训选择 -> Given 用户在检入弹窗勾选“需要培训”，When 检入生成新的 WORKING 版本，Then 新版本保存本次选择的 `needTraining=true`，后续提交审批沿用该版本事实。
- BDD: 新上传独立流程 -> Given 普通新上传请求通过服务端校验，When 创建上传审批实例，Then 文件记录、流程 DTO 和 BPM 实例均使用 `dcc-controlled-file-upload`，不再启动旧公共 approval key。
- BDD: 新流程任务动作识别 -> Given 文件记录的 processDefinitionKey 为 upload 或 revision，When DCC 处理待办、批准或驳回，Then 校验使用该文件冻结的 native key，不能因公共入口仍保留旧 key 而拒绝合法任务。
- BDD: 三动作独立路线选择 -> Given 同一类别分别配置 NEW、REVISION、OBSOLETE 三条激活路线，When 新上传或升版创建审批实例，Then 服务端按流程 key 映射的 actionType 读取对应路线，不读取同类别旧公共路线，也不在缺少 actionType 路线时 fallback。
- BDD: 新流程首节点为会签 -> Given NEW/REVISION 路线首节点是 MATRIX_REVIEW，When 提交新上传或升版，Then 文件初始状态为 `PENDING_MATRIX_REVIEW`，BPM `startUserSelectAssignees` 只包含会签节点，后续 `approveUserSelectAssignees` 包含批准与文控审核节点。
- BDD: 作废发起使用独立路线 -> Given 受控文件原始 processDefinitionKey 为 upload 或 revision，且同类别配置了 OBSOLETE 路线，When 发起作废且请求没有显式 startUserSelectAssignees，Then 服务端按 OBSOLETE actionType 解析作废路线，不读取原上传/升版路线，也不 fallback 到 legacy 路线。
- BDD: 三套 BPMN 静态部署合同 -> Given 发布迁移包含 DCC 三流程 BPMN，When 静态验证 migration，Then 必须包含 upload/revision/obsolete 三个独立 process key；upload/revision 包含会签、批准、needTraining 条件、培训、分发、文控审核；obsolete 只包含会签、批准、文控审核且不包含培训或分发。
- BDD: 逐部门义务快照落库 -> Given MATRIX_REVIEW 路线配置生产部和质量部，且两个部门负责人都是 A，When 上传审批创建会签任务快照，Then 系统保存两条不同 `departmentId/obligationId` 的 task assignee snapshot，`assigneeUserId` 均为 A，且唯一约束按 `(tenantId, controlledFileId, stageCode, departmentId)` 防止同部门重复，不按 userId 去重。
- BDD: BPM 多实例保留同人多部门任务 -> Given MATRIX_REVIEW 路线配置生产部和质量部，且两个部门负责人都是 A，When Flowable 为 DCC 上传/升版/作废流程创建多实例会签任务，Then 多实例候选列表保留 `[A, A]` 两个条目，不被 BPM 公共候选 `Set` 合并为一个任务。
- BDD: BPM 运行任务绑定部门义务 -> Given DCC 会签多实例任务由部门义务列表 `[生产部义务, 质量部义务]` 创建，When Flowable 根据 `loopCounter` 创建每个 runtime task 且 DCC 办理该任务，Then runtime task 的本地变量保存对应 `obligationId/index`，DCC 按 `obligationId` 将 `bpmTaskId/nodeInstanceId` 回写到对应部门义务快照，且找不到唯一快照时报错。
- BDD: 培训分发等待节点运行态连接 -> Given 上传/升版三流程在批准后按 `needTraining` 进入培训或直接进入分发，When 培训记录上传完成或手动分发完成，Then DCC 触发对应 BPM receiveTask，培训完成进入待分发，分发完成进入文控审核，不直接激活文件；未勾选培训时分发放行不要求培训确认。
- BDD: 上传升版培训分发放行 -> Given upload/revision 三流程已上传本轮要求的培训记录，且文控审核前尚未生成生效阶段的受训人确认记录，When 计算分发动作权限或执行手动分发，Then `needTraining=true` 以本轮培训记录作为前置完成证据，`needTraining=false` 跳过培训确认门禁，二者都须满足类别 `DISTRIBUTE` 权限；旧公共流程继续使用原受训人确认规则。
- BDD: 文控审核终态闭环 -> Given 上传/升版新流程已完成分发并处于 `PENDING_DOC_CONTROL_REVIEW`，且数据库尚无发布件/盖章件绑定，When 文控审核任务批准且流程结束，Then DCC finalization 按冻结的 `MATRIX_REVIEW`、`MATRIX_APPROVAL`、`DOC_CONTROL_REVIEW` 三阶段校验完整签名证据；`MATRIX_REVIEW` 按每个部门义务分别验签，即使两部门负责人相同也必须存在两个不同 taskId 的有效签名，然后从已审批源文件生成并绑定盖章发布件，最后使文件受控生效；旧审批状态仍校验其四阶段冻结签名并遵守预绑定发布件/盖章件合同。Given 作废新流程由 FormCenter action 使用 `dcc-controlled-file-obsolete` 审批定义完成文控审核，When 流程结束或效果执行，Then DCC 通用 listener 不消费 obsolete key，作废效果仅由 `DccControlledFileObsoleteFormEffectExecutor` 执行，避免双所有者重复作废。
- BDD: 新合同拒绝手选会签人 -> Given 上传或升版提交请求携带废弃的 `selectedSignoffUserIds`，When 服务端处理三流程发起，Then 直接以路线未就绪错误拒绝并提示不得手选会签人，不读取客户端人员列表、不创建文件/义务快照、不启动 BPM；会签人只能由会签矩阵部门和任务创建时部门负责人配置决定。
- BDD: 类别权限规则页面维护 -> Given 类别已有可配置权限规则和矩阵托管的 REVIEW/APPROVE 规则，When 管理员打开类别权限规则并新增、修改或删除非矩阵动作，Then 页面通过既有类别权限查询/替换接口保存；保留未编辑规则的主体、范围、启用状态和备注，不提交 REVIEW/APPROVE，且任何规则主体缺失时阻止保存。

## Current Status

in_progress

### 2026-09-27 子 Agent 监督式最终 E2E 结果

- 真实 Playwright 五条生命周期路径均通过：上传免培训、升版免培训、上传带培训、升版带培训、作废；业务动作均由真实页面完成，测试环境签名材料使用任务目录 PNG。
- 最终后端 BPM+DCC 定向回归 `294/294 PASS`；前端索引静态契约 `52/52 PASS`，额外审批中心/作废/路线契约 `7/7 PASS`，`pnpm ts:check` PASS。
- 作废服务已修复为一次提交复用同一个 `ResolvedRoute`，避免负责人配置变更导致会签/批准候选不一致；作废服务、FormCenter/DCC 作废回归与审批适配器均已复核通过。
- 当前不标记 `ready_for_closeout`：Git 提交/推送与 cleanup 尚未执行；这不影响本轮真实 E2E 结论。

### 2026-09-26 测试图片替代与作废入口复核

按测试环境口径生成三份 PNG 签名材料并保存在任务 artifacts：生产负责人、质量负责人、admin。通过真实用户管理页面为 `shanglei`/`1468` 和 `xujianhai`/`1524` 重置测试密码、补齐文控角色/项目经理岗位并启用电子签名授权；两账号登录时仍被真实页面阻止在“首次或重置后必须修改密码”，未用 API/数据库绕过。共享 8061 的任务部门负责人当前实际显示为 admin；已通过真实“我的签名”页面上传并启用 admin 测试签名图片。

真实 Playwright 只读复核 8062 的 B/1 `DCC-E2E-ADMIN-1790431784325` 可见、发布件/盖章件均已生成，但“更多”菜单仍只显示“修改基础信息”，没有“作废当前版本”；作废提交脚本因页面入口缺失而阻塞，未用 API/数据库代替业务动作。任务状态继续保持 `in_progress`。

### 2026-09-26 当前续测结果

已完成真实上传与升版主链路验证：任务专用项目 `269` 的模板通过真实 `/mdm/project-code?projectCodeId=269` 页面保存唯一文件项 `DCC-E2E-DOCFINAL-MI-20260926-01.docx`，分类叶子为 `13`。真实上传免培训流程创建文件 `2054545668044084021` / `A/1`，两条同一负责人但不同部门的会签任务、批准、手动分发、文控审核均通过真实页面完成，最终 `ACTIVE`，发布件/盖章件均为 `9198354931261`，无活动 Flowable 任务。

随后从同一真实页面对 A/1 检出并检入生成 B/1 文件 `2054545668044084022`，真实提交审批、两条会签、批准、手动分发、文控审核全部通过；只读核验确认 A/1 `SUPERSEDED`、B/1 `ACTIVE`、master 当前生效版本指向 B/1，B/1 发布件/盖章件均已生成。

作废仍未完成：类别 `908710` 的 `OBSOLETE / USER / admin / GLOBAL` 规则尚未通过真实页面保存确认。任务专用前端 8062 当前运行进程的类别页仍未稳定刷新到当前源码的“类别权限”页签；尝试 HMR 后仍返回旧页面结构，前端重启被执行策略拦截，未通过 API/数据库绕过。作废业务 E2E 和 Git/cleanup closeout 继续保持开放。

### 2026-09-26 当前继续验证

已通过真实页面补齐测试环境的代签前置：管理员 `admin` 的电子签名授权已确认启用，并通过“我的签名”页面上传、启用新的任务专用 PNG；两个任务部门负责人已通过 `/system/dept` 页面切换为 admin。原生三流程 route-preview 返回 `ready=true`，会签矩阵仍解析为两个部门义务且同一负责人 `[1,1]`，批准与文控审核候选均解析为 admin。

真实页面还发现一个测试 fixture 前置缺口：类别 `908710` 是 `DCC_FVM_DHF_002`，服务端要求启用 MDM 产品。已通过真实“产品建档申请”页面选择现有启用产品、提交并审批申请 8，生成绑定 `productMasterId=9` 的任务专用项目代码 `269 / DCC-E2E-DCFINAL-20260926-01`。模板编辑器真实页面在保存前暴露前端级问题：Vue 内部 taxonomy tree 已正确生成叶子 `id/value=13`，但 Element Plus 可见弹层仍渲染索引值且不展开子节点，尚未发出模板 PUT，因此上传/升版/作废业务 E2E 仍未完成。

本轮已完成模板选项显式 `value/label` 修复、静态合同、前端类型检查和 221 项 BPM/DCC 后端回归；任务状态继续保持 `in_progress`，尚未进入 `ready_for_closeout`，也未执行 Git 提交/推送或 cleanup apply。

### 2026-09-24 文控终态修复与当前验收状态

修复真实文控审核后受控生效失败：原生 upload/revision 的 `PENDING_DOC_CONTROL_REVIEW` 不再要求审核前存在发布件/盖章件，而是要求两者均未预绑定，完成签名校验后由 finalization 从已批准源文件生成并绑定；旧审批状态仍要求预绑定发布件。签名终态校验按流程 key 区分原生三阶段与 legacy 四阶段；部门会签按冻结的每部门负责人多重集合校验，不同部门负责人相同仍须在两个不同 taskId 上分别签名，缺少任何部门义务签名均阻止盖章。先后两项回归均按 Given/When/Then 记录 RED 后 GREEN，`DccControlledFileFinalizationServiceImplTest` 52 tests PASS，配合 `DccControlledFileWorkflowServiceImplTest` 145 tests 的同轮回归共 197 tests PASS；normal-flow、training/distribution/finalization、core Expected Verification、obsolete/FormCenter replay 聚合重新执行并已刷新索引。

真实运行验证使用登记的任务 worktree `int_qms` slot 1（前端 8062、后端 48062），后端为本次完整构建产物且 health `UP`；共享 `8061/48061` 未重启。租户 `芋道源码` 的上传页对类别 908710 返回 `NEW` route readiness `ready=true`，矩阵会签两部门义务和批准/文控候选均可见。真实上传端到端仍未完成：首次样本文件名已被既有 tenant-wide name claim 占用，数据库只读核对确认没有创建 file/master 行；第二个既有项目无有效 MDM 产品绑定，提交被 `Submit request is missing required metadata` 拒绝，亦无 file/master 行。为消除这些测试数据缺口，已通过真实页面创建并审核 task-owned DCC 项目/产品 `DCC-E2E-DCFINAL-20260924-DCFINAL-01`（projectCodeId 267），并在该项目下通过真实模板编辑页保存唯一文件项 `DCC-E2E-DOCFINAL-SOURCE-20260924.docx`，分类为 `技术文档 / 设计和开发策划阶段 / 技术调研报告`；真实上传页对该项目的 route readiness `ready=true`。这些 task-owned 准备数据保留供续测，不是 DCC 流程通过证据。

当前部门负责人已按真实业务负责人替换：通过真实 `/system/dept` 页面，将生产测试部门 `910333` 配置为 `shanglei`（用户 ID `1468`，尚磊），质量测试部门 `910334` 配置为 `xujianhai`（用户 ID `1524`，徐建海）。两账号在租户 1 中均启用、未锁定且 `password_credential_status=ACTIVE`；原测试账号 `dccE2EProd0922v1` 保留，不再承担本任务会签。替换证据见 `artifacts/dcc-leader-replacement-20260924.json`。

新的负责人尚不能进入真实文件提交：使用任务专用项目 `267` 和其类别模板从真实上传页面运行 `upload-preview` 时，HTTP 400 的页面预检明确报告两个部门负责人均缺系统岗位、当前阶段文控权限、电子签名授权和有效签名图片（共 8 项 blocker）。浏览器没有发送 `/dcc/controlled-files/submit`；失败证据的 `submitRequestData` 为空；只读 DB 核验项目 267 的 controlled-file master/file 均为 0，最近 30 分钟没有本轮源 PDF 临时上传行。租户 1 中当前没有非 admin 用户同时满足这四项会签资格：`zhaojie` 已有电子签名授权和有效签名图片但无系统岗位；admin 满足部分检查但禁止代替部门负责人。没有给任何用户加角色/岗位、授权电子签名或复制/生成签名图片。

任务状态保持 `in_progress`。后续真实 E2E 需业务方提供已具备岗位、DCC 阶段权限、电子签名授权和有效签名图片的生产/质量负责人账号；或者另行明确授权按正式治理流程为指定真实负责人办理权限/岗位配置，并由账号本人提供、启用其真实签名图片。不得用 admin 代签或伪造签名材料。真实 upload/revision/obsolete、培训、分发、文控审核和 Git/cleanup closeout 仍未完成。

下方较早的实施摘要和 2026-09-22/23 记录均按当时状态保留；其中“NEW/REVISION/OBSOLETE 路线未配置”的阶段性阻塞已被 2026-09-23 管理页配置记录取代，当前状态只以上述 2026-09-24 节及其后续日期记录为准。

### 2026-09-25 当前开发验证刷新

当前源码定向 BPM/DCC 回归已重新通过：BPM 42 tests + DCC 245 tests，共 287 tests，failures/errors/skipped 全为 0。首次复跑发现 `DccApprovalTaskAdapterTest` 缺少 `assertTrue` 静态导入，随后发现同一新增测试存在未使用的 Mockito `task.getName()` stub；两项均已按 RED/GREEN 修正。前端类别权限组件同步到任务运行 worktree 后，真实页面刷新可见“类别权限”页签，并能打开 `技术调研报告` 的权限编辑抽屉；当前 `OBSOLETE / USER / admin / GLOBAL` 规则只存在于未保存编辑态，未计为配置生效或作废入口通过。`pnpm ts:check`、7 个定向静态合同、任务脚本 `node --check` 和 `pnpm build:local` 均 PASS。

当前最终门禁仍未完成：OnlyOffice `127.0.0.1:8080` 已恢复健康，B/1 旧失败实例尚未重新从真实页面定位并复验；类别作废权限尚未获得保存确认；真实生产/质量负责人仍缺岗位、DCC 当前阶段权限、电子签名授权和有效签名图片，完整上传/升版/作废页面闭环及 Git/cleanup closeout 继续保持 `in_progress`。

### 2026-09-25 OnlyOffice 恢复

用户开启代理后，已通过仓库 Compose 独立启动 `intruoyi-onlyoffice`，成功拉取 `onlyoffice/documentserver:latest`（digest `sha256:3ab6ebc7c605e5a32b7ae3ff19daed4925090245acc8100ce2230bd766c88212`）。容器映射 `8080 -> 80`，健康状态为 `healthy`，容器内 `docservice` 与 `converter` 均为 `RUNNING`，`GET http://127.0.0.1:8080/healthcheck` 返回 HTTP 200/body `true`。该恢复未重启 `8062/48062`，未写数据库。之前“OnlyOffice 未恢复”的结论仅保留为历史记录，当前升版最终化可继续复验。

已完成 P1 的第一批后端代码与开发验证：部门候选解析到负责人、缺负责人失败、同一负责人负责多部门时保留重复会签义务、管理端预览支持 DEPT、BPM 三个新流程 key 纳入严格候选与通用审批签名守卫。已补充路线保存 VO/API 与前端路线表单的多部门 `candidateSourceIds` 配置切片。已补充 DCC 审批中心/状态监听/前端工作台对上传、升版、作废新流程 key 的识别切片；作废在工作台通过 BPM 只读 `businessObjectId` 投影定位受控文件，并区分 FormCenter `FORM_ACTION` businessKey 与 obsolete 流程定义 key。已补充工作稿送审 `needTraining` 写入幂等 payload、文件记录和 BPM 变量的后端切片，并让前端检入弹窗可选择培训、送审请求传递当前版本的培训值。已补充 route `actionType` 数据结构、NEW/REVISION/OBSOLETE action 路线读取、上传/升版按流程 key 选择独立路线、首节点 MATRIX_REVIEW 的状态与 BPM 候选变量拆分。已补充管理端按 `actionType` 保存、筛选、预览和表单配置三套 action 路线的 UI/API 切片，NEW/REVISION/OBSOLETE 路线使用三节点固定策略且首节点必须为 DEPT 会签。已补充作废发起入口在请求未显式传候选人时按 OBSOLETE actionType 解析作废路线，不再受原文件 upload/revision/legacy processDefinitionKey 影响。已补充三套独立 BPMN seed migration 的静态部署合同：上传/升版包含会签、批准、`needTraining` 条件培训、分发、文控审核，作废包含会签、批准、文控审核且不含培训/分发，并通过 release migration policy gate。已补充 `dcc_controlled_file_task_assignee_snapshot` 逐部门义务快照表、DO/Mapper、上传/升版提交时按 DEPT 节点保存每部门义务，保证同一负责人负责多个部门时不按 userId 合并，并通过 release migration policy gate。已补充 BPM 多实例候选列表保留 DCC 发起人/审批人自选的重复 userId，避免同一负责人负责多个部门时被 BPM 公共 `Set` 去重成一个任务。已补充 BPM 创建多实例任务时按 `loopCounter` 写入 DCC obligation task local variable，并在 DCC 办理任务校验时按 `obligationId` 回写义务快照的 `bpmTaskId/nodeInstanceId`。已补充上传/升版 BPMN 培训与分发节点改为 receiveTask，培训记录上传触发 `TRAINING` 并进入待分发，手动分发触发 `DISTRIBUTION` 并进入文控审核，未勾选培训时不要求培训确认。已补充文控审核终态闭环单元级验证：upload/revision 新流程可从 `PENDING_DOC_CONTROL_REVIEW` 进入现有受控生效 finalization；作废流程效果所有权保持在 FormCenter executor，DCC 通用 listener 不消费 obsolete key，避免双所有者重复作废。已补充 seed BPMN 的隔离 Flowable 内存引擎部署与逐节点停靠验证：upload/revision 的培训 true/false 路径和 obsolete 路径均可部署并跑到结束，且运行态断言每一步只停在应停节点，培训/分发 receiveTask 不会提前激活文控审核。真实数据库迁移、正式 Flowable 部署表读取和服务健康检查已完成；完整真实页面 E2E 尚未完成。
最终验证被阻断：真实数据库五份迁移已在本机 Docker MySQL `127.0.0.1:23306/ruoyi-vue-pro` 首次及重复执行成功；正式 Flowable `ACT_*` 只读核验确认三条独立流程 key 各有 tenant 1 和 tenant 122 的部署版本及预期节点；`int_qms` 服务已切换到稳定运行包并在 `48061` 健康，前端 `8061` 返回 200。真实 Playwright 已通过登录、上传页、文件上传预览和升版工作稿检入，但上传/升版提交前 readiness 均因租户 1 的分类 `908710`“技术调研报告”没有配置 `NEW`/`REVISION` action 路线而 fail fast，后端返回 `CONTROLLED_FILE_ROUTE_NOT_CONFIGURED`；该分类当前只存在 `LEGACY` 路线。尝试通过管理员真实页面配置时，页面虽然能打开 action 路线表单，但租户部门数据存在多个同名部门且当前无法无猜测地确认生产部、质量部的唯一有效负责人，因此没有保存伪造配置。作废流程同样不能进入业务动作。任务状态为 `blocked`，待业务管理员在页面完成唯一部门负责人及三条 action 路线配置后，才能继续真实页面办理和最终收尾。

2026-09-22 授权最终验证：执行真实 MySQL migration 首次/重复运行、正式 Flowable `ACT_*` 表只读核验、`int_qms` 前后端运行验证和 Playwright 真实页面验证。迁移目标为本机 Docker `int-ruoyi-mysql` 的 `ruoyi-vue-pro`，端口 `23306`；运行端口为前端 `8061`、后端 `48061`。数据库结构、Flowable 部署、health check 和页面登录/上传预览/升版检入证据均通过；真实业务提交被分类 `908710` action route 未配置阻断，未使用 API/数据库写入绕过页面业务动作。

### 2026-09-23 状态校正

本节及后续验证记录优先于本文件较早段落中的过期总结：真实数据库迁移和正式 Flowable 部署表核验已经完成，不应再表述为“尚未执行”。当前 `task.md` 状态为 `in_progress`；前端生产构建在 2026-09-23 本轮重跑成功；完整真实页面审批链、Git 收尾及 cleanup 尚未完成，不得标记 `ready_for_closeout` 或 `completed`。

2026-09-23 管理端真实页面配置已保存并完成只读复核：在租户 1、分类 `908710` 下，已通过管理端页面保存任务专用生产部 `910333`、质量部 `910334`，两部门均配置唯一负责人用户 `910325`；`NEW`、`REVISION`、`OBSOLETE` 三条激活路线分别为版本 5、4、4，首节点会签矩阵均只引用这两个任务专用部门，批准和文控审核节点保持原岗位候选配置。该配置满足“矩阵决定哪些部门、部门负责人决定谁来签、任务创建时冻结负责人”的前置条件。

2026-09-23 真实 Playwright 账号前置条件阻塞：使用任务专用测试用户进入真实登录页时，系统返回“首次或重置后必须修改密码后再登录”，因此尚未进入会签、批准、培训、分发、文控审核或作废业务动作。未通过数据库/API 修改密码、重置账号或模拟业务成功；需要先通过允许的用户界面完成该账号的首次密码变更，或提供一个已满足首次登录条件的任务专用测试账号，再继续真实页面 E2E。

2026-09-22 只读配置核验：`dcc_category_approval_route` 在 tenant 1、category 908710 下只有 `LEGACY` 路线，active 版本为 5，没有 `NEW`、`REVISION`、`OBSOLETE`。`dcc_controlled_file_upload` 和 revision 的真实页面 readiness 分别在提交前抛出 `CONTROLLED_FILE_ROUTE_NOT_CONFIGURED`；管理员路线页面可进入新增路线表单，但部门下拉有多个同名部门，当前无法依据真实数据无猜测地选择唯一生产部/质量部并保存。

2026-09-22 早期局部开发复验记录：三流程相关前端静态合同、`pnpm ts:check`、DCC 三流程核心局部聚合 236 个后端回归和 seed BPMN 隔离 Flowable 引擎路径验证均 PASS。该 236 为当轮局部统计，不代表当前全部后端索引总量；当前总量以 `test-evidence-index.json` 的 17 组 / 1298 indexed tests 为准。完成审计确认：当前开发期证据仍不足以替代 `acceptance.md` 的真实 DB/正式部署表/页面 E2E/Git 收尾要求，因此任务继续保持 `in_progress`。

2026-09-22 继续刷新 schema-inclusive 跨模块回归：修复后的 `DccBaseSchemaTest` broad schema gate 已纳入 BPM+DCC 核心组合命令同轮执行，BPM 39 tests + DCC 254 tests，合计 293 tests PASS，0 failures/errors/skipped。该证据证明当前 schema gate、三流程核心、作废入口、受控生效、FormCenter 回调和 BPM 候选合同在同一轮编译/测试中兼容；真实 DB migration、正式 Flowable 部署表读取、真实页面 E2E、服务启动和 Git 收尾仍未获当前轮授权，状态保持 `in_progress`。

2026-09-22 继续补强 source-governance tenant 迁移合同：新增 `test_dcc_source_governance_batch_tenant_id_sql.py` 锁定 `20260922_dcc_source_governance_batch_tenant_id.sql` 的 release metadata、MySQL `information_schema.COLUMNS` + dynamic DDL 幂等写法、禁止 `ADD COLUMN IF NOT EXISTS`，以及禁止对 `dcc_controlled_file_source_governance_batch` 做业务 DML/破坏性表操作。当前 bundled Python 缺少 `pytest`，因此 `python -m pytest` 记录为环境 RED；直接导入执行 3 个 `test_*` 函数 PASS，`DccSourceOwnershipSchemaTest,DccBaseSchemaTest` 组合 40 tests PASS。真实 DB migration 首次/重复执行仍未获授权，状态保持 `in_progress`。

2026-09-22 继续补强迁移/schema 合同组合验证：`DccThreeWorkflowBpmnMigrationTest,DccTaskAssigneeSnapshotMigrationTest,DccFormCenterPolicyMigrationTest,DccSourceOwnershipSchemaTest,DccBaseSchemaTest` 同轮 PASS，合计 44 tests，0 failures/errors/skipped。该证据把 source-governance tenant schema 修复、三流程 BPMN seed、逐部门义务快照 migration 和 FormCenter obsolete policy migration 放在同一 DCC 测试轮次内验证；真实 DB migration 首次/重复执行、正式 Flowable 部署表读取、真实页面 E2E、服务启动和 Git 收尾仍未获授权，状态保持 `in_progress`。

2026-09-22 继续刷新前端当前态验证：三流程/工作台/readiness 相关 14 个静态脚本同轮 PASS，覆盖部门候选、三流程 key、上传培训 checkbox、工作稿升版送审、培训/分发工作台、作废 FormCenter、管理端 action route、路线列表、readiness、上传治理 UX、工作台入口和我的培训工具栏；`pnpm ts:check` PASS。该证据不替代真实页面 E2E，状态保持 `in_progress`。

2026-09-22 继续刷新后端治理入口当前态：`DccControlledFileRouteReadinessServiceTest,DccCategoryApprovalMatrixAdminServiceImplTest,DccAdminFullConfigPackageServiceTest,DccFileCategoryControllerConfigPackageContractTest,DccControlledFileApprovalRouteAssigneeResolverTest,DccControlledFileObsoleteServiceTest` 同轮 PASS，合计 56 tests，0 failures/errors/skipped。该证据覆盖 route readiness、矩阵负责人配置、管理端配置包、部门负责人解析和作废 OBSOLETE 路线收口；真实 DB/正式 Flowable/真实页面 E2E/Git 收尾仍未授权，状态保持 `in_progress`。

2026-09-22 继续补齐收尾门禁证据：只读执行 `task_closeout.py --mode preview`，工具按规则拒绝当前 `in_progress` 状态并返回 `BLOCKED task status is in_progress; expected ready_for_closeout or completed`。该结果证明当前不能运行 cleanup preview/apply，也不能把任务标记为完成；状态保持 `in_progress`。

2026-09-22 继续刷新 BPM/统一审批中心当前态验证：`BpmMessageServiceImplTest,BpmTaskConvertTest,BpmTaskServiceImplApprovalFilterTest,BpmProcessInstanceServiceImplTest,BpmNativeApprovalTaskProviderTest` 同轮 PASS，合计 62 tests，0 failures/errors/skipped。该证据覆盖 BPM 消息、任务转换、审批过滤、流程实例投影和原生审批任务 provider 相邻边界；真实页面 E2E、正式 Flowable 部署表读取和 Git 收尾仍未授权，状态保持 `in_progress`。

2026-09-22 继续刷新培训/分发/文控生效当前态验证：`DccTrainingAssignmentAckServiceTest,DccTrainingConcurrentAcknowledgementTest,DccTrainingTaskServiceTest,DccDistributionReceiptServiceImplTest,DccDistributionTaskServiceImplTest,DccPaperDistributionAckServiceTest,DccCategoryTrainingRuleAdminServiceImplTest,DccCategoryDistributionRuleAdminServiceImplTest,DccControlledFileFinalizationServiceImplTest` 同轮 PASS，合计 85 tests，0 failures/errors/skipped。该证据覆盖培训确认、并发确认、培训任务、电子/纸质分发、分发回执、分类培训/分发规则和最终文控生效服务层边界；真实页面 E2E 和正式运行态证据仍未授权，状态保持 `in_progress`。

2026-09-22 继续刷新历史/审批列表/日志时间线当前态验证：`BpmProcessInstanceControllerVisibilityContractTest,DccApprovalTaskAdapterTest,DccApprovalTaskTimelineAdapterTest,DccControlledFileAuditControllerTest,DccControlledFileLogControllerTest,DccControlledFileAuditQueryServiceTest,DccControlledFileLogQueryServiceTest` 同轮 PASS，合计 36 tests，0 failures/errors/skipped。该证据覆盖 BPM 流程实例可见性、DCC 审批适配、审批时间线、审计/日志 controller 和审计/日志查询相邻边界；真实页面审批中心/历史列表 E2E 仍未授权，状态保持 `in_progress`。

2026-09-22 补写历史/审批列表证据后复跑轻量文档门禁：任务状态与 `Cleanup Keep` 结构检查 PASS，TDD 合规检查 PASS，`git diff --check` 无 whitespace error，仅保留当前工作区既有 LF/CRLF warning。任务仍缺真实 DB/正式 Flowable/真实页面 E2E/Git 收尾授权，状态保持 `in_progress`。

2026-09-22 继续刷新权限/预览下载/签名与查看矩阵当前态验证：DCC 后端权限、预览下载、目录访问、类别权限、电子签名授权/管理和下载策略组合回归 111 tests PASS；前端审批中心/详情/权限/签名/审阅矩阵/查看矩阵 26 个静态脚本同轮 PASS，`pnpm ts:check` PASS。过程中 `dcc-view-matrix-unified-source-static.spec.js` 先 RED 于旧合同仍把独立查看矩阵断言落在审阅矩阵表，已修正为当前审阅矩阵/查看矩阵拆分口径后 GREEN。真实多账号页面权限、签名下载和查看矩阵页面 E2E 仍未授权，状态保持 `in_progress`。

2026-09-22 补写权限/预览/签名/查看矩阵证据后复跑轻量门禁：任务状态与 `Cleanup Keep` 结构检查 PASS，TDD 合规检查 PASS，`git diff --check` 无 whitespace error，仅保留当前工作区既有 LF/CRLF warning。任务仍缺真实 DB/正式 Flowable/真实页面 E2E/Git 收尾授权，状态保持 `in_progress`。

2026-09-22 继续刷新作废/FormCenter/撤回重提/幂等重放当前态验证：BPM+DCC 组合回归覆盖 FormCenter BPM 回调桥、effect orchestrator、repository boundary、DCC 作废服务、作废 effect executor、workflow 撤回/重提、发布通知/后续事务、消息重放与幂等，合计 208 tests PASS，0 failures/errors/skipped。该证据补强 BDD-22 至 BDD-24 的开发期相邻覆盖；真实作废页面流程、撤回/重提、同键冲突、旧回执迟到和效果失败重放运行态 E2E 仍未授权，状态保持 `in_progress`。

2026-09-22 补写作废/FormCenter/撤回重提/幂等重放证据后复跑轻量门禁：任务状态与 `Cleanup Keep` 结构检查 PASS，TDD 合规检查 PASS，`git diff --check` 无 whitespace error，仅保留当前工作区既有 LF/CRLF warning。任务仍缺真实 DB/正式 Flowable/真实页面 E2E/Git 收尾授权，状态保持 `in_progress`。

2026-09-22 继续刷新 BDD-25 并发/幂等开发期证据：BPM+DCC 组合回归覆盖 FormCenter 幂等查找、培训最后两人并发确认、发布通知并发串行化、消息投递事务、影响评估事务、发布后续事务、工作稿提交/检出并发抢占、消息投递幂等和重放，BPM 10 tests + DCC 26 tests，合计 36 tests PASS，0 failures/errors/skipped。该证据补强 BDD-25 的单元/集成层当前态覆盖；真实 MySQL 事务交错、升版/作废竞争和页面级并发仍未授权，状态保持 `in_progress`。

2026-09-22 补写 BDD-25 并发/幂等证据后复跑轻量门禁：任务状态与 `Cleanup Keep` 结构检查 PASS，TDD 合规检查 PASS，`git diff --check` 无 whitespace error，仅保留当前工作区既有 LF/CRLF warning。任务仍缺真实 DB/正式 Flowable/真实页面 E2E/Git 收尾授权，状态保持 `in_progress`。

2026-09-22 继续刷新 BDD-30 配置变更/冻结计划失效开发期相邻证据：BPM+DCC 组合回归覆盖 DCC 严格候选、矩阵负责人预览/阻断、路线负责人解析、培训/分发规则、培训任务/确认、电子/纸质分发任务与签收，BPM 5 tests + DCC 64 tests，合计 69 tests PASS，0 failures/errors/skipped。该证据补强“负责人/名单由创建时快照和冻结计划决定，运行中不被当前配置静默替换”的服务层边界；真实提交后矩阵/负责人变更、冻结计划人员失效页面和运行态验证仍未授权，状态保持 `in_progress`。

2026-09-22 补写 BDD-30 配置变更/冻结计划失效证据后复跑轻量门禁：任务状态与 `Cleanup Keep` 结构检查 PASS，TDD 合规检查 PASS，`git diff --check` 无 whitespace error，仅保留当前工作区既有 LF/CRLF warning。任务仍缺真实 DB/正式 Flowable/真实页面 E2E/Git 收尾授权，状态保持 `in_progress`。

2026-09-22 继续刷新 BDD-29 迁移/部署静态证据：三份 release migration policy gate 当前态重新 PASS，`migration-policy-gate-three-workflows.json` 为 `status=passed,migrationCount=3`，`migration-policy-gate-task-assignee-snapshot.json` 为 `status=passed,migrationCount=4`，`migration-policy-gate-source-governance-batch-tenant.json` 为 `status=passed,migrationCount=4`。DCC migration/schema 组合 `DccThreeWorkflowBpmnMigrationTest,DccTaskAssigneeSnapshotMigrationTest,DccFormCenterPolicyMigrationTest,DccSourceOwnershipSchemaTest,DccBaseSchemaTest` 同轮 44 tests PASS，0 failures/errors/skipped。该证据补强 SQL 发布策略、三流程 BPMN seed、逐部门义务快照、FormCenter obsolete policy 和 source-governance tenant schema 的开发期静态/隔离引擎覆盖；真实 DB migration 首次/重复执行、正式 Flowable `ACT_*` 部署表读取、真实页面 E2E、服务启动和 Git 收尾仍未授权，状态保持 `in_progress`。

2026-09-22 补写 BDD-29 迁移/部署静态证据后复跑轻量门禁：任务状态与 `Cleanup Keep` 结构检查 PASS，TDD 合规检查 PASS，`git diff --check` 无 whitespace error，仅保留当前工作区既有 LF/CRLF warning。任务仍缺真实 DB/正式 Flowable/真实页面 E2E/Git 收尾授权，状态保持 `in_progress`。

2026-09-22 继续补齐重复任务记录门禁审计：当前同主题目录仅有 `20260921-dcc-three-workflows-design` 与 `20260921-dcc-three-workflows-implementation`；design 任务 `Current Status` 第一条非空文本为 `blocked`，implementation 任务为 `in_progress`。二者职责分别为前置设计文档和代码实施验证，不存在第二个同主题实施任务继续悬挂在 `pending/in_progress` 抢占当前证据口径；任务仍缺真实 DB/正式 Flowable/真实页面 E2E/Git 收尾授权，状态保持 `in_progress`。

2026-09-22 继续刷新提交/动作守卫当前态验证：`DccWorkingIterationSubmissionServiceTest,DccWorkingSubmissionConditionTest,DccControlledFilePendingActionGuardTest,DccOrdinaryApprovalRemovedActionsTest,DccApprovalTaskAdapterTest,DccControlledFileTaskActionApiTest,DccApprovalReasonValidationTest` 同轮 PASS，合计 44 tests，0 failures/errors/skipped。该证据覆盖工作稿升版提交、提交条件、待办动作守卫、普通审批动作移除、DCC 审批适配、任务动作 API 和审批原因校验，补强 BDD-15/BDD-20/BDD-23 的开发期服务层边界；真实页面办理、真实撤回/重提和正式 Flowable 运行态仍未授权，状态保持 `in_progress`。

2026-09-22 继续刷新 BPM 候选策略当前态验证：`BpmTaskCandidateDeptLeaderStrategyTest,BpmTaskCandidateDeptLeaderMultiStrategyTest,BpmTaskCandidateDeptMemberStrategyTest,BpmTaskCandidateStartUserDeptLeaderStrategyTest,BpmTaskCandidateStartUserDeptLeaderMultiStrategyTest,BpmTaskCandidateStartUserSelectStrategyTest,BpmTaskCandidateMixedStrategyTest,BpmTaskCandidateInvokerTest,BpmDccRequiredCandidatesTest,BpmUserTaskActivityBehaviorTest` 同轮 PASS，合计 25 tests，0 failures/errors/skipped。该证据覆盖普通候选策略、DCC 三流程严格候选、多实例任务行为以及 null/blank legacy process key 不应误入 DCC 守卫的相邻边界，补强 BDD-14/BDD-15 的开发期 BPM 层证明；正式 Flowable 运行态和真实页面办理仍未授权，状态保持 `in_progress`。

2026-09-22 继续补强 BPM 引擎集成开发期证据：seed BPMN 隔离 Flowable 测试已加入逐节点停靠断言，覆盖 upload/revision 的 MATRIX_REVIEW -> MATRIX_APPROVAL -> TRAINING/DISTRIBUTION -> DOC_CONTROL_REVIEW、跳过培训时不得停 TRAINING、obsolete 不得停 TRAINING/DISTRIBUTION；目标测试 2 tests PASS，BPM+DCC 组合 200 tests PASS。

2026-09-22 继续补强作废接口合同：在全局 JSON mapper 允许未知字段的当前配置下，作废请求携带 `needTraining` 曾被静默忽略；已新增显式拒绝测试和 VO 写入拒绝，目标 RED/GREEN 通过，`DccControlledFileObsoleteServiceTest` 更新为 9 tests PASS，BPM+DCC 组合回归更新为 201 tests PASS。

2026-09-22 继续补强上传/升版提交接口合同：`selectedSignoffUserIds` 旧字段仍可在上传/升版提交路径被服务端接收并传入 readiness，违反“矩阵决定部门、负责人配置决定人员”的新合同；已新增 RED 用例覆盖上传和工作稿升版提交携带手选会签人时必须失败，随后在三流程提交前统一拒绝非空手选列表，并将旧的“手选人匹配矩阵”测试改为废弃字段拒绝合同。目标 RED/GREEN 通过，`DccControlledFileWorkflowServiceImplTest` 更新为 144 tests PASS，BPM+DCC 当前组合回归更新为 160 tests PASS（BPM 5，DCC 155）。

2026-09-22 继续补强前端废弃字段移除合同：上传页 route readiness、新上传 submitter 和浏览器工作稿升版送审不再发送 `selectedSignoffUserIds`；前端 API 类型仅保留可选字段用于服务端兼容拒绝边界，外部评审参与人链路不纳入本次三流程移除范围。旧静态合同先 RED，随后更新为“不得发送废弃手选会签人字段”并 GREEN；`pnpm ts:check` PASS。

2026-09-22 继续补强 route readiness actionType 合同：上传页预检现在显式传 `actionType=NEW`，后端 `/route-preview` 将 actionType 传入 readiness resolver；NEW/REVISION/OBSOLETE action route 的预检请求若携带非空 `selectedSignoffUserIds` 直接以路线未就绪错误拒绝。目标 RED/GREEN 通过，`DccControlledFileRouteReadinessServiceTest` 与 `DccControlledFileWorkflowServiceImplTest` 组合 147 tests PASS，前端 readiness/governance 静态合同和 `pnpm ts:check` PASS。

2026-09-22 继续刷新 actionType readiness 后的 BPM+DCC 组合回归：`BpmDccRequiredCandidatesTest,DccControlledFileRouteReadinessServiceTest,DccControlledFileWorkflowServiceImplTest,DccControlledFileObsoleteServiceTest,DccThreeWorkflowBpmnMigrationTest` 同轮 PASS，BPM 5 tests + DCC 158 tests，合计 163 tests，0 failures/errors/skipped。真实 DB migration、正式 Flowable 部署表读取、真实页面 E2E、服务启动和 Git 收尾仍未获当前轮授权，状态保持 `in_progress`。

2026-09-22 继续刷新前端与迁移策略开发期证据：三流程相关 9 个前端静态脚本同轮 PASS；WindowsApps `python` 占位启动器不可用，改用 Codex bundled Python 刷新四个 SQL 的 release migration policy gate，`status=passed`，`migrationCount=4`，证据 JSON 已更新。该证据不替代真实数据库迁移执行。

2026-09-22 继续补强升版 readiness 合同：工作稿升版送审前此前没有调用 `/route-preview`，无法在前端提交前按 `REVISION` action route 发现矩阵/负责人缺配置。已先更新静态合同取得 RED，再在浏览页提交审批前调用 `checkControlledFileRouteReadiness({ categoryId: row.categoryId, actionType: 'REVISION' })`，不发送废弃 `selectedSignoffUserIds`；未就绪时以后端 blocker 阻断提交。相关 5 个前端静态脚本 PASS，`pnpm ts:check` PASS。

继续补强当前证据：BPM 相邻回归 17 tests PASS，release migration policy gate 使用当前脚本路径合同重跑 PASS，`migrationCount=4`。

继续扩大 DCC current-state 回归：首次加入 `DccControlledFileQueryServiceTest` 的组合套件失败，原因是该测试类未注入 `DccControlledFileAttachmentService`，导致查询响应组装先 NPE。已补齐测试夹具 mock 和空结果默认值后，`DccControlledFileQueryServiceTest` 149 tests PASS，包含查询服务在内的 DCC broad regression 426 tests PASS。

继续补强前端相邻静态合同：`dcc-admin-full-config-route-static.spec.js` 首次失败于旧 `ruoyi-vue-pro` SQL 路径锚点，已修正为当前 `IntRuoyiBackend/sql/mysql` 路径并保持原业务断言。随后工作稿送审、培训/分发工作台、作废 FormCenter、管理端配置路由和路线列表静态合同均 PASS，`pnpm ts:check` PASS。

继续补强后端相邻回归：培训确认、并发签收、培训任务、分发签收、分发任务、纸质分发、分类培训/分发规则和受控生效组合套件 85 tests PASS。

继续扩大开发验证：依赖模块全链路 `-am` 测试被上游 `yudao-module-system` 的本机绝对路径文件缺失阻断；DCC 模块全量 2064 tests 暴露多组既有非本任务失败，同时发现本任务相关旧测试夹具仍按 actionType 前方法签名 stub。已修复 route-readiness、working-iteration 和 route preview 夹具后，相关 38 tests PASS；当前任务扩展 DCC 主回归 435 tests PASS。

2026-09-22 继续刷新当前证据：三流程核心前端静态合同、工作稿送审、培训/分发工作台、作废 FormCenter、管理端配置入口、路线列表静态合同和 `pnpm ts:check` 均 PASS；release migration policy gate 在当前双目录工作区先暴露 root-level `sql/mysql` 路径边界，改用 `IntRuoyiBackend/sql/mysql` 实际 SQL 根后 PASS，`migrationCount=4`；`git diff --check` 无 whitespace error，仅保留已有 LF/CRLF warning。

继续对齐 `acceptance.md` 点名的测试范围：补跑矩阵管理与管理端全量配置包相邻后端回归。`DccCategoryApprovalMatrixAdminServiceImplTest` 17 tests PASS，覆盖矩阵配置、部门负责人预览和阻断风险；`DccFileCategoryControllerConfigPackageContractTest` 5 tests 与 `DccAdminFullConfigPackageServiceTest` 11 tests PASS，覆盖配置包中矩阵/路线/培训分发等治理配置合同。

继续补强上传/升版发布边界：`DccControlledFileUploadApiTest` 34 tests、`DccControlledFilePublicationFlowTest` 2 tests、`DccControlledFileVersionNumberAllocationTest` 3 tests、`DccApprovalVersionBindingTest` 2 tests 均 PASS，覆盖上传入口治理、发布失败处理、版本号分配顺序和审批版本绑定相邻合同。

继续补强上传/来源/预览邻近验证：`DccSourceUploadContextTest` 7 tests、`DccControlledFileUploadNameOptionQueryServiceTest` 4 tests、`DccControlledFileUploadNameOptionApiTest` 1 test、`DccControlledFilePlatformAdapterTest` 1 test、`DccOnlyOfficeControlledPreviewTest` 7 tests 均 PASS，覆盖来源上传上下文、上传名称选项、平台适配和受控预览相邻合同。

继续补强前端审批中心与上传入口静态合同：旧 DCC 审批任务 view-mode/summary/publish-transition 静态脚本先暴露过期断言，仍要求旧专属待办页 view switch、旧 `待文控发布` 文案和过窄表格锚点。已按当前统一审批中心与三流程“最终文控后系统生效”设计收敛静态合同后，12 个审批中心/上传/FormCenter 静态脚本 PASS，`pnpm ts:check` PASS。

继续补强审批中心/最终生效相邻后端回归：`DccApprovalTaskTimelineAdapterTest` 2 tests、`DccApprovalPrintTemplateServiceTest` 10 tests、`DccControlledContentAdapterTest` 11 tests、`DccControlledFilePublishFormEffectExecutorTest` 7 tests、`DccControlledFilePublishServiceTest` 10 tests、`DccWorkingSubmissionConditionTest` 1 test 均 PASS，覆盖审批时间线、审批打印模板、统一受控内容状态转换、发布 effect executor、发布服务和工作稿提交条件相邻合同。

继续补强作废入口候选来源：作废服务此前在请求携带 `startUserSelectAssignees` 时会优先采用请求值，存在绕过 OBSOLETE action route 的风险。已收口为始终按 OBSOLETE action route 解析发起候选，不读取请求手工 assignee；focused 后端 `DccControlledFileObsoleteServiceTest` 9 tests PASS，新增静态合同 PASS，相关前端静态组和 `pnpm ts:check` PASS。随后纳入 BPM+DCC 组合回归，BPM 39 tests + DCC 166 tests，合计 205 tests PASS；再纳入 expanded DCC 任务回归，440 tests PASS。

继续补强交付面验证：最新代码通过 BPM+DCC 依赖闭包 Maven package，23 个 reactor 模块 SUCCESS；前端 `pnpm build:local` PASS，生成生产构建。该证据只证明编译/打包/前端构建边界，不替代真实 DB、正式 Flowable、真实页面 E2E、服务启动或 Git 收尾。

继续补强审批签名/授权/理由相邻后端回归：`DccApprovalReasonValidationTest` 1 test、`DccSignatureBindingSchemaTest` 3 tests、`DccControlledFileSignatureServiceTest` 13 tests、`DccControlledFileSignatureEvidenceServiceTest` 5 tests、`DccControlledFileSignatureBindingServiceTest` 8 tests、`DccFrozenApprovalSignaturesTest` 4 tests、`DccApprovalParticipantPostValidatorTest` 2 tests、`DccElectronicSignatureAuthorizationAuditServiceTest` 3 tests、`DccElectronicSignatureAuthorizationServiceTest` 13 tests、`DccElectronicSignatureManagementServiceTest` 39 tests、`DccElectronicSignatureImageServiceImplTest` 2 tests、`DccElectronicSignatureFailureAuditServiceTest` 3 tests、`DccElectronicSignatureAuthorizationControllerTest` 2 tests 均 PASS，共 98 tests，覆盖审批理由、签名绑定 schema、签名证据、冻结签批、岗位校验和电子签名授权/审计/管理相邻合同。

PASS：补充审批签名/授权/理由验证证据后 `git diff --check` 无 whitespace error；输出仅包含当前脏工作区已有 LF/CRLF warning。

继续补强审批动作/预览保护/详情页相邻验证：后端 `DccControlledFileTaskActionApiTest` 7 tests、`DccControlledFilePendingActionGuardTest` 2 tests、`DccOrdinaryApprovalRemovedActionsTest` 4 tests、`DccRevisionSourceSelectionTest` 1 test、`DccControlledFileVersionPolicyTest` 3 tests、`DccControlledFilePreviewDownloadApiTest` 12 tests、`DccControlledFilePreviewProtectionTest` 8 tests、`DccControlledPreviewAccessServiceTest` 3 tests、`DccControlledFileReviewMatrixAccessServiceTest` 4 tests、`DccControlledFileViewMatrixAccessServiceTest` 11 tests 均 PASS，共 55 tests，覆盖审批动作入口、待处理动作门禁、旧普通审批动作移除、升版来源、版本策略、预览/下载保护和矩阵/视图访问相邻合同。

RED/GREEN 前端文控终态与详情页静态合同：首次扩展运行详情页/工作台静态组时，`dcc-detail-distribution-summary-static.spec.js`、`dcc-detail-route-snapshot-summary-static.spec.js`、`dcc-browser-finalization-retry-entry-static.spec.js`、`dcc-detail-handling-summary-static.spec.js`、`dcc-approval-task-load-error-context-static.spec.js` 先失败于过期静态锚点或旧表达式；已收敛到当前正式源码边界。`dcc-doc-control-path-confirmation-static.spec.js` 首次失败于详情审批弹窗缺少“存入路径确认”字段；已补齐最终文控签名弹窗的存入路径选择、目录树加载、`confirmedDirectoryId` 与分发范围提交载荷。重跑 11 个详情/工作台/文控静态脚本均 PASS，`pnpm ts:check` PASS。

PASS：本轮后端/前端/文档更新后 `git diff --check` 无 whitespace error；输出仅包含当前脏工作区已有 LF/CRLF warning。

继续补强前端相邻静态合同：15 个发布后续/矩阵/签名/培训/分发/文控下发范围/FormCenter 发布静态脚本 PASS，`pnpm ts:check` PASS。过程中修复两个产品缺口：审阅矩阵弹窗旧“四层文控固定”提示改为当前三流程口径；最终文控弹窗补回“文件下发范围”部门树多选、介质选择和空范围本地校验。

继续补强后端提交/撤回/再提交与审批动作相邻验证：`DccApprovalTaskAdapterTest`、`DccApprovalReasonValidationTest`、`DccControlledFilePendingActionGuardTest`、`DccControlledFileTaskActionApiTest`、`DccOrdinaryApprovalRemovedActionsTest`、`DccWorkingIterationSubmissionServiceTest`、`DccWorkingSubmissionConditionTest` 合计 44 tests PASS，覆盖作废审批适配、审批理由必填、待办动作保护、旧普通审批动作移除、工作稿送审/撤回/再提交和提交条件相邻合同。

继续补强详情页、上传页和工作台核心前端开发验证：15 个核心 DCC 静态脚本全部 PASS，`pnpm ts:check` PASS。过程中修复 5 类产品缺口：详情页普通动作统一受后端 actionProjection 二次门禁保护；影响评估工作台支持 `sourceIterations` 现有版本选择；上传提交前等待 route readiness 并按 blocker 硬阻断；选择已有模板文件名时默认计算下一大版本号；产品编号只读并直接来源于所选 DCC 项目代码。

继续补强上传优化、项目代码识别和备注检入静态合同：`dcc-static-022-remark-only-checkin-static.spec.cjs` 首次暴露旧合同未包含 `needTraining` 与大版本/返工必须上传新源文件门禁，已收敛到当前正式检入合同；`dcc-upload-optimization-static.spec.js`、`dcc-project-code-recognition-static.spec.js`、`dcc-static-022-remark-only-checkin-static.spec.cjs` 均 PASS，`pnpm ts:check` PASS。`dcc-loss-order-form-center-chain-static.spec.js` 仍指向缺失的历史损耗单 FormCenter runner 和 20260719 任务目录，该历史资产断裂不作为本任务三流程代码证据。

继续补强重放、通知和逻辑身份相邻后端验证：`DccLogicalIdentityConcurrencyTest`、`DccMessageDeliveryIdempotencyTest`、`DccMessageDeliveryTransactionIntegrationTest`、`DccPublicationNotificationTransactionIntegrationTest`、`DccPublicationNotificationPostCommitSchedulerTest`、`DccPublicationNotificationDispatchOrchestratorTest`、`DccPublicationNotificationServiceTest`、`DccPublicationFollowupServiceTest`、`DccPublicationFollowupStatusServiceTest`、`DccPublicationFollowupQueryServiceTest` 合计 41 tests PASS，覆盖开发期的消息投递幂等、afterCommit 失败记录、发布通知事务、发布后续查询/状态和逻辑身份并发相邻合同；该组不替代真实 MySQL 并发或真实最终流程事件重放验收。

继续补强历史/审批列表/日志时间线相邻后端验证：`BpmProcessInstanceControllerVisibilityContractTest`、`DccApprovalTaskAdapterTest`、`DccApprovalTaskTimelineAdapterTest`、`DccControlledFileAuditControllerTest`、`DccControlledFileLogControllerTest`、`DccControlledFileAuditQueryServiceTest`、`DccControlledFileLogQueryServiceTest` 合计 36 tests PASS，覆盖旧/新 key 审批列表适配、FORM_ACTION 定位、BPM 流程实例可见性、DCC 时间线、日志和审计查询相邻合同；该组不替代真实审批中心页面 E2E。

继续补强前端广覆盖静态回归：三流程部门候选、process key、上传培训、升版送审、培训/分发工作台、作废 FormCenter、管理端全量配置、路线列表、审批中心、工作台、我的培训、分发上下文、权限页拆分和 FormCenter 上传嵌入等 20 个 DCC/相邻静态脚本同轮 PASS。该证据覆盖前端合同广回归，但仍不替代真实页面 E2E。

继续补强后端治理入口复验：路线 readiness、矩阵负责人、管理端配置包、路线负责人解析和作废发起服务组合回归 56 tests PASS；同时验证任务文档 `Current Status` 第一条非空文本仍为 `in_progress`，符合当前未授权真实 DB/E2E/Git 收尾边界。

继续补强迁移/模型开发期复验：三流程 BPMN seed、逐部门义务快照 migration、FormCenter 作废 policy migration 组合测试 4 tests PASS，其中三流程 BPMN 继续通过隔离 Flowable 内存引擎部署和路径推进；release migration policy gate 重新 PASS，`migrationCount=4`。该证据不替代真实数据库首次/重复执行或正式 Flowable 部署表读取。

继续刷新跨模块核心回归：BPM+DCC 组合套件覆盖候选严格合同、BPM 多实例任务、FormCenter 回调桥、三流程 BPMN/migration、作废入口、受控生效和状态监听，BPM 39 tests + DCC 219 tests，合计 258 tests PASS，0 failures/errors/skipped。真实 DB migration、正式 Flowable 部署表读取、真实页面 E2E、服务启动和 Git 收尾仍未授权，状态保持 `in_progress`。

继续补齐验收差距审计：按 `docs/dcc-three-workflows/acceptance.md` 的 BDD-01 至 BDD-30 分组核对，当前开发期证据已覆盖三 key、培训 checkbox、部门矩阵、唯一负责人、快照、receiveTask、作废 owner、迁移静态合同和多组相邻回归；仍缺真实 DB 首次/重复迁移、正式部署表读取、真实页面五路径/E2E、真实并发和 Git 收尾证据。

2026-09-22 继续刷新前端三流程当前态验证：从 `IntRuoyiFronted` 同轮运行 14 个 DCC 三流程/工作台/readiness 静态脚本全部 PASS，随后 `pnpm ts:check` PASS。该证据覆盖部门候选、三流程 key、上传培训 checkbox、升版送审预检、培训/分发工作台、作废 FormCenter、管理端 action route、路线列表、readiness、上传治理 UX、工作台入口和我的培训工具栏；仍不替代真实页面 E2E、真实服务运行或正式 Flowable/DB 证据。

2026-09-22 继续补强完成度审计资产：新增 `acceptance-coverage-matrix.json`，逐条列出 BDD-01 至 BDD-30 的需求引用、开发期证据、最终缺口、当前状态和不可关闭结论。该矩阵明确全部场景当前均为 `development_verified_final_blocked` / `not_complete`，防止把单元、静态、隔离 Flowable 或 migration policy PASS 误判为最终验收完成；任务仍保持 `in_progress`。

2026-09-22 继续补强证据资产清单：新增 `evidence-assets-manifest.json`，记录任务文档、三份 migration policy gate JSON 和 `acceptance-coverage-matrix.json` 的 Git 可见性、忽略状态和 SHA-256。该 manifest 只证明证据资产存在且内容可核对，不替代真实 DB、正式 Flowable、真实页面 E2E、真实并发或 Git 收尾；任务仍保持 `in_progress`。

2026-09-22 继续刷新核心开发期聚合回归：`mvn -pl yudao-module-bpm,yudao-module-dcc -am "-Dtest=DccControlledFileApprovalRouteAssigneeResolverTest,DccApprovalRouteAdminServiceImplTest,DccControlledFileWorkflowServiceImplTest,DccTaskAssigneeSnapshotMigrationTest,DccThreeWorkflowBpmnMigrationTest,BpmDccRequiredCandidatesTest,BpmTaskExternalSignatureGuardTest,BpmUserTaskActivityBehaviorTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` 使用仓库 `.runtime` Maven/JDK 17 PASS；BPM 14 tests + DCC 187 tests，合计 201 tests，0 failures/errors/skipped，23 个 reactor modules SUCCESS。该证据刷新部门负责人解析、同人多部门义务、路线 actionType 管理、上传/升版提交、逐部门义务快照 migration、三流程 BPMN 隔离 Flowable 路径、DCC 严格候选、多实例任务行为和外部签名守卫的当前源码证明；仍不替代真实 DB、正式 Flowable 部署表、真实页面 E2E、服务运行或 Git 收尾。

2026-09-22 继续刷新交付面构建验证：后端 `mvn -pl yudao-module-bpm,yudao-module-dcc -am "-DskipTests" package` PASS，23 个 reactor 模块全部 SUCCESS；前端 `pnpm build:local` PASS，输出 `Build successful. Please see dist directory`。该证据证明当前 BPM/DCC 依赖闭包可编译打包、前端生产构建可生成 `dist`，但后端命令跳过测试，且两项均不替代真实服务启动、真实 DB migration、正式 Flowable 部署表读取或真实页面 E2E。

2026-09-22 继续补充提交前可审计性验证：`git status --short --branch --untracked-files=all` 显示当前分支为 `int_qms...origin/int_qms`，工作区仍有大量已修改/未跟踪文件，暂存区为空；本任务 `task.md`、`execution-log.md`、`verification-report.md` 和三份 migration policy JSON 均通过 `git check-ignore -v` 可见性检查，未被 `.gitignore` 忽略。同主题任务目录复核结果仍为：`20260921-dcc-three-workflows-design -> blocked`，`20260921-dcc-three-workflows-implementation -> in_progress`。后续若进入 Git 收尾，需当前轮明确授权并按仓库规则处理脏工作区基线/实现/收尾提交。

2026-09-22 按 Expected Verification 继续补强 P1 聚合回归：`mvn -pl yudao-module-bpm,yudao-module-dcc -am "-Dtest=DccControlledFileApprovalRouteAssigneeResolverTest,DccApprovalRouteAdminServiceImplTest,DccControlledFileWorkflowServiceImplTest,DccTaskAssigneeSnapshotMigrationTest,DccThreeWorkflowBpmnMigrationTest,BpmDccRequiredCandidatesTest,BpmTaskExternalSignatureGuardTest,BpmUserTaskActivityBehaviorTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` PASS，BPM 14 tests + DCC 187 tests，合计 201 tests，0 failures/errors/skipped。该证据把部门负责人解析、同人多部门义务、路线 actionType、上传/升版提交、逐部门义务快照 migration、三流程 BPMN seed 隔离 Flowable、DCC 严格候选和 BPM 签名守卫放在同一轮编译/测试中验证；仍不替代真实 DB/正式 Flowable/真实页面 E2E/Git 收尾。

2026-09-22 继续补强测试证据索引：新增 `test-evidence-index.json`，从当前 surefire XML 逐类索引核心聚合 201 tests PASS 与正常流程聚合 140 tests PASS，合计 341 tests，0 failures/errors/skipped。索引结构、manifest 哈希、Cleanup Keep 一致性、任务状态、TDD 合规、`git diff --check`、尾随空白扫描和 Git 可见性均已复核；该证据仍不替代真实 DB migration、正式 Flowable 部署表读取、真实页面 E2E、服务启动和 Git 收尾，状态保持 `in_progress`。

2026-09-22 继续补强矩阵/负责人配置侧验证：按 `acceptance.md` 的“部门/路线单元测试”要求补跑 `DccControlledFileRouteReadinessServiceTest,DccCategoryApprovalMatrixAdminServiceImplTest`，合计 20 tests PASS，0 failures/errors/skipped，覆盖路线 readiness 阻断、矩阵部门候选、负责人预览和缺负责人阻断等当前态合同。已纳入 `test-evidence-index.json`；真实页面配置与真实账号办理仍未执行，状态保持 `in_progress`。

2026-09-22 继续补强负责人解析与矩阵访问相邻验证：补跑 `DccApprovalPositionRuntimeResolverTest,DccControlledFileReviewMatrixAccessServiceTest,DccControlledFileViewMatrixAccessServiceTest,DccCategoryViewMatrixAdminServiceImplTest`，合计 28 tests PASS，0 failures/errors/skipped，覆盖部门负责人运行时解析、审阅矩阵访问、查看矩阵访问和查看矩阵管理预览。已纳入 `test-evidence-index.json`；真实页面权限、真实多账号访问和历史列表 E2E 仍未执行，状态保持 `in_progress`。

2026-09-22 继续补强前端合同证据：重跑三流程/工作台/readiness 相关 14 个前端静态脚本，全部 PASS；重跑 `pnpm ts:check` PASS。新增 `frontend-evidence-index.json` 记录脚本列表、覆盖点和类型检查命令，并加入 `Cleanup Keep`；该证据仍不替代真实页面 E2E、真实账号权限和历史列表回看，状态保持 `in_progress`。

2026-09-22 继续补强验收证据交叉审计：新增 `evidence-coverage-crosscheck.json`，将 BDD-01 至 BDD-30 按 8 个验收分组映射到 `test-evidence-index.json`、`frontend-evidence-index.json`、`source-anchor-audit.json` 和三份 migration policy gate 资产。当前 8 组均有开发期证据资产，且全部保持 `not_complete`，明确不替代真实 DB、正式 Flowable、真实页面 E2E、服务运行和 Git 收尾，状态保持 `in_progress`。

2026-09-22 继续执行收尾预检：本机存在 `task-closeout-cleanup` 脚本。首次按历史参数尝试 `--workspace --json` 失败，因为当前脚本仅支持 `--task-id` 与 `--mode`；随后按实际 help 运行 preview，结果 BLOCKED：`task status is in_progress; expected ready_for_closeout or completed`。该阻塞符合当前最终验收未齐全的事实，未执行 cleanup apply，状态保持 `in_progress`。

2026-09-22 继续补强 Git 收尾预检：新增 `git-closeout-preflight.json`，记录当前分支 `int_qms...origin/int_qms`、暂存区为空、同主题任务仅有 design=`blocked` 与 implementation=`in_progress`、本任务 12 个证据资产均为 Git 可见未跟踪且未被 ignore。该预检不执行 `git add/commit/push`，真实 Git 收尾仍需当轮明确授权，状态保持 `in_progress`。

2026-09-22 继续补强最终验收准备清单：新增 `final-evidence-readiness.json`，逐项列出真实 DB migration、正式 Flowable ACT_* 读取、真实页面 E2E、服务运行验证和 Git 收尾 5 个最终门禁的现状、未执行原因、授权要求和授权后的下一步动作。该文件用于防止把开发期证据误判为最终完成，状态保持 `in_progress`。

2026-09-22 继续补强 BDD-26 至 BDD-28 前端静态证据：重跑审批中心、详情、权限、签名、审阅矩阵、查看矩阵、分发摘要与生命周期/历史相关 26 个静态脚本，全部 PASS。已在 `frontend-evidence-index.json` 新增 `permissionHistoryStaticContracts` 分组；该证据仍不替代真实多账号页面访问、签名下载权限、长部门名 UI 或历史列表 E2E，状态保持 `in_progress`。

## 设计约束检查

- 本任务不把完整 P1-P4 宣称一次性完成；每次只按实际代码和测试证据标记完成范围。
- 不修改真实数据库、不向正式 Flowable 表部署 BPM 模型、不重启 `int_main` 服务、不执行 E2E；允许仅在单元测试内使用 Flowable 内存引擎验证 seed BPMN 可部署和推进。
- 不使用 fallback、兼容补丁、默认成功、静默过滤或 Set 去重来掩盖部门义务。
- 负责人唯一来源暂按已核对的 `system_dept.leader_user_id`；实施前通过当前源码和测试夹具再次确认。
- 工作区已有无关脏改动；本任务只触碰当前实现需要的文件和本任务资产，不回滚他人改动。

2026-09-22 继续刷新正常流程链路聚合回归：`DccTrainingAssignmentAckServiceTest,DccTrainingConcurrentAcknowledgementTest,DccTrainingTaskServiceTest,DccDistributionReceiptServiceImplTest,DccDistributionTaskServiceImplTest,DccPaperDistributionAckServiceTest,DccCategoryTrainingRuleAdminServiceImplTest,DccCategoryDistributionRuleAdminServiceImplTest,DccControlledFileFinalizationServiceImplTest,DccControlledFileObsoleteFormEffectExecutorTest,DccControlledFileObsoleteServiceTest,FormCenterRuntimeBpmCallbackTest,FormCenterBpmEventBridgeTest,FormEffectOrchestratorTest,FormCenterRepositoryBoundaryTest` 同轮 PASS，BPM 38 tests + DCC 102 tests，合计 140 tests，0 failures/errors/skipped。该证据覆盖培训确认/并发确认/培训任务、电子/纸质分发、分发回执、分类培训/分发规则、文控生效、作废 effect executor、作废服务和 FormCenter BPM 回调/效果编排相邻边界；仍不替代真实 DB migration、正式 Flowable 部署表读取、真实页面 E2E、服务运行或 Git 收尾授权，状态保持 `in_progress`。

2026-09-22 继续补强完成度审计门禁：对 `acceptance-coverage-matrix.json` 执行机器可读完整性检查 PASS，确认 BDD-01 至 BDD-30 共 30 条全部存在，所有条目均为 `development_verified_final_blocked` / `not_complete`，并且 5 个最终 blocker（真实 DB migration、正式 Flowable ACT_* 表、真实页面 E2E、真实服务运行态、授权 Git closeout）仍保留。该证据防止后续将 201/140 等开发期 PASS 误判为最终完成，任务继续保持 `in_progress`。

2026-09-22 继续补强源码锚点静态审计：使用 `rg` 对当前源码/SQL/前端关键锚点复核 PASS，确认三流程 key 在 `DccControlledFileProcessDefinitionKeys.java` 与前端 `workflow.ts` 中显式定义；上传/升版 BPMN seed 在 `20260922_dcc_three_workflow_bpmn_seed.sql` 中包含 `needTraining` 条件、`TRAINING`/`DISTRIBUTION` receiveTask 和文控审核流转；作废请求 VO 显式拒绝 `needTraining`；逐部门义务快照表、DO、Mapper、上传/升版保存点、BPM `loopCounter` task-local 绑定和 DCC 回写点均存在；负责人解析链路按 `actionType` 读取 action route，并由 `candidateSourceIds` 部门集合解析 `leaderUserId`。该审计证明当前源码锚点仍与开发文档口径一致，但仍不替代真实 DB migration、正式 Flowable 部署表、真实页面 E2E 或服务运行态。

2026-09-22 继续校验源码锚点资产：对 `source-anchor-audit.json` 执行路径/行号完整性校验 PASS，7 个 anchors、14 个 evidence items 的文件路径均存在，所有记录行号均落在当前文件范围内，assertion 非空，且 5 个最终缺口仍保留。该证据确保锚点审计不是失效行号或悬空路径，任务继续保持 `in_progress`。

2026-09-22 继续校验源码锚点语义：对 `source-anchor-audit.json` 对应源码窗口执行语义 token 校验 PASS，14 组检查均在锚点行附近命中预期关键字，包括三流程 key、`needTraining` 条件、`receiveTask`、作废拒绝培训字段、`obligationId`、`loopCounter`、`actionType`、`candidateSourceIds` 和 `leaderUserId`。该证据比单纯行号存在更强，但仍是静态源码证据，不替代真实 DB/Flowable/E2E/服务运行态。

2026-09-22 继续补强 cleanup 保留一致性门禁：首次将 `Cleanup Keep` 与 manifest assets 做一一比对时 RED，原因是 `evidence-assets-manifest.json` 为避免自哈希递归未列入自身 assets，但仍需要在 `Cleanup Keep` 中保留。修正校验规则允许 manifest 自身作为保留项后 GREEN，确认 `Cleanup Keep` 6 项与 manifest 8 项资产一致，所有资产文件存在，且没有额外未登记保留项。该证据降低后续 cleanup 漏删/误删任务证据的风险，任务继续保持 `in_progress`。

2026-09-22 继续尝试更宽回归：`mvn -pl yudao-module-dcc -am test` 在上游 `yudao-module-system` 失败，原因是 `InvoiceVoucherPrintAssistantErpConfigBridgeContractTest` 依赖本机 `C:\ProjectPackage\erp-invoice-voucher-print-assistant\server.js`，当前机器缺该外部文件；DCC 模块未执行。随后改跑带 `-am` 的 DCC 命名广域测试 `-Dtest=Dcc*Test,*Dcc*Test`，上游只编译/跳过无匹配测试，BPM 侧 `BpmDccRequiredCandidatesTest` PASS，DCC 侧实际执行 1988 tests，但注册证、产品目录、项目码等相邻域存在 14 failures + 4 errors，命令整体 FAIL。该结果说明三流程核心定向/相邻证据仍有效，但 DCC 模块广域回归当前不能作为通过证据，任务继续保持 `in_progress`。

2026-09-22 同步开发文档状态说明：`docs/dcc-three-workflows/README.md`、`technical-design.md`、`acceptance.md`、`review-report.md` 已将“开发前设计/拟开发”措辞校正为“设计基线，后续实施状态以本实施任务报告为准”，避免把已完成的开发期验证误读为最终运行验收。`node doc/tasks/20260921-dcc-three-workflows-design/verify-docs.cjs` PASS，旧状态措辞 `rg` 检查无命中，文档范围 `git diff --check` PASS。

2026-09-22 继续细化 DCC 广域失败归因：解析 `yudao-module-dcc/target/surefire-reports/TEST-*.xml` 后，失败类总数 11，按域归类为 registrationcertificate 9 类（12 failures + 2 errors）、productcatalog 1 类（1 error）、projectcode 1 类（2 failures + 1 error）；按 `ControlledFile|DccThreeWorkflow|TaskAssigneeSnapshot|ApprovalRoute|Training|Distribution|Obsolete|FormCenter` 过滤，三流程/受控文件相关失败类为 0。另核对 `doc/tasks/20260920-dcc-project-product-approval-implementation/verification-report.md`，其验收口径明确旧项目代码和产品目录直接新增服务入口应拒绝绕过审批，因此当前 product/project 失败属于相邻任务新规则与旧测试口径未完全同步，不作为三流程实现失败；DCC 广域回归仍不能作为完成门禁，状态保持 `in_progress`。

2026-09-22 继续修正文档验收口径：`docs/dcc-three-workflows/acceptance.md` 顶部状态已从“未来实施/本次均未运行”改为“最终验收与 TDD 计划，当前证据和剩余 blocker 以本实施任务为准”，并明确文档结构、静态合同和定向单元测试不等于真实 DB、正式 Flowable、真实页面 E2E 已通过。该修正只更新证据边界，不改变 BDD-01 至 BDD-30 验收范围。

## Cleanup Keep

- doc/tasks/20260921-dcc-three-workflows-implementation/migration-policy-gate-three-workflows.json
- doc/tasks/20260921-dcc-three-workflows-implementation/migration-policy-gate-task-assignee-snapshot.json
- doc/tasks/20260921-dcc-three-workflows-implementation/migration-policy-gate-source-governance-batch-tenant.json
- doc/tasks/20260921-dcc-three-workflows-implementation/acceptance-coverage-matrix.json
- doc/tasks/20260921-dcc-three-workflows-implementation/evidence-assets-manifest.json
- doc/tasks/20260921-dcc-three-workflows-implementation/source-anchor-audit.json
- doc/tasks/20260921-dcc-three-workflows-implementation/test-evidence-index.json
- doc/tasks/20260921-dcc-three-workflows-implementation/frontend-evidence-index.json
- doc/tasks/20260921-dcc-three-workflows-implementation/evidence-coverage-crosscheck.json
- doc/tasks/20260921-dcc-three-workflows-implementation/git-closeout-preflight.json
- doc/tasks/20260921-dcc-three-workflows-implementation/final-evidence-readiness.json
- doc/tasks/20260921-dcc-three-workflows-implementation/continue-manual-distribution-real-ui.cjs
- doc/tasks/20260921-dcc-three-workflows-implementation/tmp-admin-upload-real.cjs
- doc/tasks/20260921-dcc-three-workflows-implementation/continue-upload-approval-real-ui.cjs
- doc/tasks/20260921-dcc-three-workflows-implementation/artifacts/dcc-admin-upload-real-doccontrolfix-20260924-01.json
- doc/tasks/20260921-dcc-three-workflows-implementation/artifacts/dcc-admin-upload-real-doccontrolfix-20260924-02.json
- doc/tasks/20260921-dcc-three-workflows-implementation/replace-leader-real-ui.e2e.cjs
- doc/tasks/20260921-dcc-three-workflows-implementation/artifacts/dcc-leader-replacement-20260924.json
- doc/tasks/20260921-dcc-three-workflows-implementation/artifacts/dcc-admin-upload-real-leader-replacement-upload-20260924-01.json
- doc/tasks/20260921-dcc-three-workflows-implementation/artifacts/test-signature-admin-20260926.png
- doc/tasks/20260921-dcc-three-workflows-implementation/artifacts/test-signature-shanglei-20260926.png
- doc/tasks/20260921-dcc-three-workflows-implementation/artifacts/test-signature-xujianhai-20260926.png

## Verification Summary

- GREEN: `mvn -pl yudao-module-dcc -am "-Dtest=DccControlledFileApprovalRouteAssigneeResolverTest,DccApprovalRouteAdminServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` using `.runtime` Maven and JDK 17 -> PASS, 33 tests.
- GREEN: `mvn -pl yudao-module-bpm -am "-Dtest=BpmDccRequiredCandidatesTest,BpmTaskExternalSignatureGuardTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` using `.runtime` Maven and JDK 17 -> PASS, 12 tests.
- GREEN: `mvn -pl yudao-module-dcc -am "-Dtest=DccApprovalRouteAdminServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` using `.runtime` Maven and JDK 17 -> PASS, 26 tests.
- GREEN: `node tests/e2e/dcc-route-department-candidate-static.spec.cjs` -> PASS.
- GREEN: `pnpm ts:check` -> PASS.
- GREEN: `mvn -pl yudao-module-dcc "-Dtest=DccControlledFileWorkflowServiceImplTest#submitWorkingIteration_persistsNeedTrainingAndPassesItToBpmVariables" test` using `.runtime` Maven and JDK 17 -> PASS, 1 test.
- GREEN: `pnpm ts:check` -> PASS.
- GREEN: `mvn -pl yudao-module-dcc "-Dtest=DccControlledFileQueryServiceTest#checkinRealSourceCreatesWorkingA2AndLeavesA1FormalPointerUnchanged,DccControlledFileWorkflowServiceImplTest#submitWorkingIteration_persistsNeedTrainingAndPassesItToBpmVariables" test` using `.runtime` Maven and JDK 17 -> PASS, 2 tests.
- GREEN: `pnpm ts:check` -> PASS.
- GREEN: `mvn -pl yudao-module-dcc -am "-Dtest=DccApprovalTaskAdapterTest,DccControlledFileStatusListenerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` using `.runtime` Maven and JDK 17 -> PASS, 27 tests.
- GREEN: `node tests/e2e/dcc-three-workflow-process-keys-static.spec.cjs` -> PASS.
- GREEN: `pnpm ts:check` -> PASS.
- GREEN: `mvn -pl yudao-module-bpm "-Dtest=BpmProcessInstanceConvertTest" test` using `.runtime` Maven and JDK 17 -> PASS, 3 tests.
- GREEN: `node tests/e2e/dcc-three-workflow-process-keys-static.spec.cjs` -> PASS.
- GREEN: `node tests/e2e/dcc-route-department-candidate-static.spec.cjs` -> PASS.
- GREEN: `pnpm ts:check` -> PASS.
- PASS: `git diff --check` scoped to changed implementation, tests and task docs.
- GREEN: `mvn -pl yudao-module-dcc "-Dtest=DccControlledFileWorkflowServiceImplTest#submitControlledFile_usesUploadActionRouteAndStartsAtMatrixReview" test` using `.runtime` Maven and JDK 17 -> PASS, 1 test.
- GREEN: `mvn -pl yudao-module-dcc "-Dtest=DccControlledFileApprovalRouteAssigneeResolverTest,DccControlledFileWorkflowServiceImplTest#submitControlledFile_usesUploadActionRouteAndStartsAtMatrixReview" test` using `.runtime` Maven and JDK 17 -> PASS, 12 tests.
- GREEN: `mvn -pl yudao-module-dcc "-Dtest=DccControlledFileWorkflowServiceImplTest#submitWorkingIteration_persistsNeedTrainingAndPassesItToBpmVariables,DccControlledFileQueryServiceTest#checkinRealSourceCreatesWorkingA2AndLeavesA1FormalPointerUnchanged" test` using `.runtime` Maven and JDK 17 -> PASS, 2 tests.
- GREEN: `mvn -pl yudao-module-dcc -am "-Dtest=DccControlledFileApprovalRouteAssigneeResolverTest,DccControlledFileWorkflowServiceImplTest,DccApprovalRouteAdminServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` using `.runtime` Maven and JDK 17 -> PASS, 176 tests.
- PASS: repository `git diff --check` -> no whitespace errors; command output only included existing LF/CRLF warnings from the dirty worktree.
- RED: `mvn -pl yudao-module-dcc "-Dtest=DccApprovalRouteAdminServiceImplTest#testSaveRoute_actionTypeNewPersistsActionRouteAndKeepsLegacyActive,DccApprovalRouteAdminServiceImplTest#testPreviewRoute_actionTypeNewUsesActionRouteNotLegacy,DccApprovalRouteAdminServiceImplTest#testSaveRoute_actionTypeSignoffRejectsUserCandidate" test` using `.runtime` Maven and JDK 17 -> FAIL at test compile before implementation because route save/preview VO did not expose `actionType`.
- GREEN: `mvn -pl yudao-module-dcc "-Dtest=DccApprovalRouteAdminServiceImplTest" test` using `.runtime` Maven and JDK 17 -> PASS, 29 tests.
- GREEN: `mvn -pl yudao-module-dcc "-Dtest=DccControlledFileWorkflowServiceImplTest" test` using `.runtime` Maven and JDK 17 -> PASS, 139 tests.
- GREEN: `mvn -pl yudao-module-dcc -am "-Dtest=DccApprovalRouteAdminServiceImplTest,DccControlledFileApprovalRouteAssigneeResolverTest,DccControlledFileWorkflowServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` using `.runtime` Maven and JDK 17 -> PASS, 179 tests.
- GREEN: `node tests/e2e/dcc-route-department-candidate-static.spec.cjs` -> PASS.
- GREEN: `pnpm ts:check` -> PASS.
- RED: `mvn -pl yudao-module-dcc "-Dtest=DccControlledFileObsoleteServiceTest#obsoleteControlledFile_derivesAssigneesFromObsoleteActionRouteRegardlessOriginalProcessKey" test` using `.runtime` Maven and JDK 17 -> FAIL at test compile before implementation because `DccControlledFileApprovalRouteAssigneeResolver` had no actionType-specific `resolveStartUserSelectAssignees(...)` overload.
- GREEN: same obsolete target test after adding the overload and using OBSOLETE actionType in the obsolete entry -> PASS, 1 test.
- REGRESSION: `mvn -pl yudao-module-dcc "-Dtest=DccControlledFileObsoleteServiceTest" test` using `.runtime` Maven and JDK 17 -> initially failed 1 old fixture that still stubbed the legacy two-argument resolver; after updating it to the OBSOLETE contract -> PASS, 8 tests.
- GREEN: `mvn -pl yudao-module-dcc -am "-Dtest=DccControlledFileApprovalRouteAssigneeResolverTest,DccControlledFileObsoleteServiceTest,DccApprovalRouteAdminServiceImplTest,DccControlledFileWorkflowServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` using `.runtime` Maven and JDK 17 -> PASS, 187 tests.
- RED: `mvn -pl yudao-module-dcc "-Dtest=DccThreeWorkflowBpmnMigrationTest" test` using `.runtime` Maven and JDK 17 -> FAIL before migration implementation because `sql/mysql/20260922_dcc_three_workflow_bpmn_seed.sql` did not exist.
- GREEN: `mvn -pl yudao-module-dcc "-Dtest=DccThreeWorkflowBpmnMigrationTest" test` using `.runtime` Maven and JDK 17 -> PASS, 1 test.
- RED: `run-release-migration-policy-gate.py` for only `20260922_dcc_three_workflow_bpmn_seed.sql` -> FAIL because dependency `20260921_dcc_category_approval_route_action_type` was not included.
- RED: migration policy gate with actionType dependency included -> FAIL because `20260921_dcc_category_approval_route_action_type.sql` lacked release metadata.
- GREEN: migration policy gate with dependency closure `20260513_dcc_base_schema.sql`, `20260921_dcc_category_approval_route_action_type.sql`, `20260922_dcc_three_workflow_bpmn_seed.sql` -> PASS, `migrationCount=3`; evidence saved to `doc/tasks/20260921-dcc-three-workflows-implementation/migration-policy-gate-three-workflows.json`.
- GREEN: surefire reports for combined DCC regression show `DccThreeWorkflowBpmnMigrationTest` 1, `DccControlledFileApprovalRouteAssigneeResolverTest` 11, `DccControlledFileObsoleteServiceTest` 8, `DccApprovalRouteAdminServiceImplTest` 29, `DccControlledFileWorkflowServiceImplTest` 139 -> PASS, 188 tests.
- RED: `mvn -pl yudao-module-dcc "-Dtest=DccControlledFileWorkflowServiceImplTest#submitControlledFile_persistsDepartmentObligationSnapshotsWithoutUserDedup" test` -> FAIL at test compile because `DccControlledFileTaskAssigneeSnapshotDO/Mapper` did not exist.
- GREEN: same target after adding task assignee snapshot DO/Mapper/schema and submit-time persistence -> PASS, 1 test.
- GREEN: `mvn -pl yudao-module-dcc "-Dtest=DccControlledFileWorkflowServiceImplTest#submitWorkingIteration_persistsNeedTrainingAndPassesItToBpmVariables,DccControlledFileWorkflowServiceImplTest#submitControlledFile_persistsDepartmentObligationSnapshotsWithoutUserDedup,DccTaskAssigneeSnapshotMigrationTest" test` -> PASS, 3 tests, covering both upload and revision task assignee snapshots.
- RED note: `DccBaseSchemaTest#mysqlSchemaShouldCoverEveryDccDoTableAndColumn` failed before checking the new table because the existing full DCC runtime SQL scan matches destructive SQL in older migration files; this is recorded as an existing broad-schema-test blocker, not a product failure for this slice.
- GREEN: `mvn -pl yudao-module-dcc "-Dtest=DccControlledFileWorkflowServiceImplTest#submitControlledFile_persistsDepartmentObligationSnapshotsWithoutUserDedup,DccTaskAssigneeSnapshotMigrationTest" test` -> PASS, 2 tests.
- GREEN: migration policy gate with dependency closure through `20260922_dcc_task_assignee_snapshot.sql` -> PASS, `migrationCount=4`; evidence saved to `doc/tasks/20260921-dcc-three-workflows-implementation/migration-policy-gate-task-assignee-snapshot.json`.
- GREEN: `mvn -pl yudao-module-dcc -am "-Dtest=DccControlledFileWorkflowServiceImplTest,DccControlledFileApprovalRouteAssigneeResolverTest,DccTaskAssigneeSnapshotMigrationTest,DccThreeWorkflowBpmnMigrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 153 tests.
- RED: `mvn -pl yudao-module-bpm "-Dtest=BpmDccRequiredCandidatesTest#dccMultiInstanceCandidateListPreservesDepartmentObligationsWithSameLeader" test` -> FAIL at test compile because `BpmTaskCandidateInvoker.calculateUserListByTask(...)` did not exist.
- GREEN: same BPM DCC duplicate-obligation target after adding list-preserving multi-instance candidate calculation -> PASS, 1 test.
- GREEN: `mvn -pl yudao-module-bpm "-Dtest=BpmDccRequiredCandidatesTest" test` -> PASS, 5 tests.
- GREEN: `mvn -pl yudao-module-dcc -am "-Dtest=DccControlledFileWorkflowServiceImplTest#submitControlledFile_persistsDepartmentObligationSnapshotsWithoutUserDedup,DccControlledFileApprovalRouteAssigneeResolverTest,DccTaskAssigneeSnapshotMigrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 13 tests.
- RED: `mvn -pl yudao-module-bpm "-Dtest=BpmUserTaskActivityBehaviorTest#recordDccTaskObligationLocalVariables_usesMultiInstanceLoopCounter" test` -> FAIL at test compile before implementation because DCC obligation process/task variable constants and runtime recording method did not exist.
- RED: `mvn -pl yudao-module-dcc -am "-Dtest=DccControlledFileWorkflowServiceImplTest#approveTask_bindsDepartmentObligationSnapshotFromBpmTaskLocalVariable" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL before implementation because the BPM compile RED above exposed the missing constants/method.
- GREEN: `mvn -pl yudao-module-bpm "-Dtest=BpmUserTaskActivityBehaviorTest#recordDccTaskObligationLocalVariables_usesMultiInstanceLoopCounter" test` -> PASS, 1 test.
- GREEN: `mvn -pl yudao-module-dcc -am "-Dtest=DccControlledFileWorkflowServiceImplTest#approveTask_bindsDepartmentObligationSnapshotFromBpmTaskLocalVariable" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 1 test.
- GREEN: `mvn -pl yudao-module-bpm "-Dtest=BpmUserTaskActivityBehaviorTest,BpmDccRequiredCandidatesTest" test` -> PASS, 6 tests.
- GREEN: `mvn -pl yudao-module-dcc -am "-Dtest=DccControlledFileWorkflowServiceImplTest#approveTask_bindsDepartmentObligationSnapshotFromBpmTaskLocalVariable,DccControlledFileWorkflowServiceImplTest#submitControlledFile_persistsDepartmentObligationSnapshotsWithoutUserDedup,DccTaskAssigneeSnapshotMigrationTest,DccThreeWorkflowBpmnMigrationTest,DccControlledFileApprovalRouteAssigneeResolverTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 15 tests.
- RED note: one parallel Maven attempt failed while both commands generated/read BPM MapStruct generated sources concurrently; sequential rerun passed, so this is recorded as a local concurrent compilation collision, not product behavior.
- RED: `mvn -pl yudao-module-dcc -am "-Dtest=DccThreeWorkflowBpmnMigrationTest,DccControlledFileWorkflowServiceImplTest#uploadTrainingRecord_newUploadProcessTriggersTrainingReceiveTaskAndMovesToDistribution,DccControlledFileFinalizationServiceImplTest#releaseManualDistribution_newUploadProcessTriggersDistributionReceiveTaskAndMovesToDocControlReview" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL because BPMN training/distribution were auto-completing serviceTasks, upload training still moved to doc-control approval, and distribution still entered old activation path.
- GREEN: same target after switching BPMN training/distribution to receiveTask and connecting DCC trigger points -> PASS, 3 tests.
- GREEN: `mvn -pl yudao-module-dcc -am "-Dtest=DccControlledFileWorkflowServiceImplTest,DccControlledFileFinalizationServiceImplTest,DccThreeWorkflowBpmnMigrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 191 tests.
- RED note: first release migration policy rerun failed because `.runtime` had no Python and Windows `python`/`py` launchers were unavailable or empty; rerun with Codex bundled Python and required `--sql-root`.
- GREEN: release migration policy gate with dependency closure through `20260922_dcc_task_assignee_snapshot.sql` after receiveTask SQL update -> PASS, `migrationCount=4`; evidence refreshed in `migration-policy-gate-task-assignee-snapshot.json`.
- RED: `mvn -pl yudao-module-dcc -am "-Dtest=DccControlledFileStatusListenerTest#onApplicationEventDelegatesNativeObsoleteProcess,DccControlledFileFinalizationServiceImplTest#handleProcessInstanceStatusChanged_uploadDocControlReviewActivatesControlledFile" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL in the initial hypothesis run; it correctly exposed upload/revision doc-control review finalization, while the obsolete-listener expectation was later rejected after rechecking FormCenter effect ownership.
- GREEN: same finalization target after allowing upload/revision `PENDING_DOC_CONTROL_REVIEW` through ordinary finalization and the initial obsolete-listener hypothesis -> PASS, 2 tests; this obsolete portion was immediately followed by the correction below.
- GREEN: `mvn -pl yudao-module-dcc -am "-Dtest=DccControlledFileFinalizationServiceImplTest,DccControlledFileStatusListenerTest,DccControlledFileWorkflowServiceImplTest,DccThreeWorkflowBpmnMigrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 198 tests.
- RED correction: `mvn -pl yudao-module-dcc -am "-Dtest=DccControlledFileStatusListenerTest#onApplicationEventIgnoresNativeObsoleteProcessBecauseFormCenterOwnsTheEffect" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL because obsolete key had been incorrectly added to DCC native finalization listener.
- GREEN correction: removed obsolete from native finalization keys; `mvn -pl yudao-module-dcc -am "-Dtest=DccControlledFileStatusListenerTest#onApplicationEventIgnoresNativeObsoleteProcessBecauseFormCenterOwnsTheEffect,DccControlledFileStatusListenerTest#onApplicationEventIgnoresObsoleteFormCenterProcess,DccControlledFileObsoleteFormEffectExecutorTest,DccControlledFileObsoleteServiceTest,DccApprovalTaskAdapterTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 40 tests.
- REGRESSION correction: `mvn -pl yudao-module-dcc -am "-Dtest=DccControlledFileFinalizationServiceImplTest,DccControlledFileStatusListenerTest,DccControlledFileWorkflowServiceImplTest,DccThreeWorkflowBpmnMigrationTest,DccControlledFileObsoleteFormEffectExecutorTest,DccControlledFileObsoleteServiceTest,DccApprovalTaskAdapterTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 236 tests.
- GREEN current-state frontend: `node tests/e2e/dcc-route-department-candidate-static.spec.cjs`, `node tests/e2e/dcc-three-workflow-process-keys-static.spec.cjs`, `node tests/e2e/dcc-upload-training-checkbox-static.spec.cjs` -> PASS/exit 0.
- GREEN current-state frontend: `pnpm ts:check` -> PASS.
- GREEN current-state backend: `mvn -pl yudao-module-dcc -am "-Dtest=DccControlledFileFinalizationServiceImplTest,DccControlledFileStatusListenerTest,DccControlledFileWorkflowServiceImplTest,DccThreeWorkflowBpmnMigrationTest,DccControlledFileObsoleteFormEffectExecutorTest,DccControlledFileObsoleteServiceTest,DccApprovalTaskAdapterTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 236 tests.
- GREEN current-state isolated Flowable: `mvn -pl yudao-module-dcc -am "-Dtest=DccThreeWorkflowBpmnMigrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 2 tests, including seed BPMN extraction, in-memory deployment, upload/revision/obsolete path progression, and per-node stop assertions for user tasks and receive tasks.
- GREEN current-state cross-module refresh: `mvn -pl yudao-module-bpm,yudao-module-dcc -am "-Dtest=DccControlledFileWorkflowServiceImplTest,DccThreeWorkflowBpmnMigrationTest,DccTaskAssigneeSnapshotMigrationTest,DccControlledFileObsoleteFormEffectExecutorTest,DccControlledFileObsoleteServiceTest,FormCenterRuntimeBpmCallbackTest,FormCenterBpmEventBridgeTest,BpmDccRequiredCandidatesTest,BpmTaskCandidateInvokerTest,BpmUserTaskActivityBehaviorTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 200 tests.
- RED current-state obsolete request contract: `mvn -pl yudao-module-dcc -am "-Dtest=DccControlledFileObsoleteServiceTest#obsoleteRequestJsonRejectsNeedTrainingEvenWhenGlobalMapperAllowsUnknownFields" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL before VO contract fix because global JSON mapper ignored `needTraining`.
- GREEN current-state obsolete request contract: same target after adding explicit write-only `needTraining` rejection to `DccControlledFileObsoleteReqVO` -> PASS, 1 test.
- GREEN current-state obsolete service: `mvn -pl yudao-module-dcc -am "-Dtest=DccControlledFileObsoleteServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 9 tests.
- GREEN current-state cross-module refresh after obsolete request contract: `mvn -pl yudao-module-bpm,yudao-module-dcc -am "-Dtest=DccControlledFileWorkflowServiceImplTest,DccThreeWorkflowBpmnMigrationTest,DccTaskAssigneeSnapshotMigrationTest,DccControlledFileObsoleteFormEffectExecutorTest,DccControlledFileObsoleteServiceTest,FormCenterRuntimeBpmCallbackTest,FormCenterBpmEventBridgeTest,BpmDccRequiredCandidatesTest,BpmTaskCandidateInvokerTest,BpmUserTaskActivityBehaviorTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 201 tests.
- PASS current-state whitespace after obsolete request contract and evidence refresh: `git diff --check` -> 无 whitespace error，仅 LF/CRLF warning。
- RED current-state upload/revision manual signoff contract: `mvn -pl yudao-module-dcc -am "-Dtest=DccControlledFileWorkflowServiceImplTest#submitWorkingIteration_rejectsClientSelectedSignoffUsersForThreeWorkflow,DccControlledFileWorkflowServiceImplTest#submitControlledFile_rejectsClientSelectedSignoffUsersForUploadWorkflow" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL before fix; upload accepted the request, working iteration did not reject at the deprecated-field contract boundary.
- GREEN current-state upload/revision manual signoff contract: same target after rejecting non-empty `selectedSignoffUserIds` for upload/revision submissions -> PASS, 2 tests.
- GREEN current-state workflow service: `mvn -pl yudao-module-dcc -am "-Dtest=DccControlledFileWorkflowServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 144 tests.
- GREEN current-state cross-module focused refresh after manual signoff rejection: `mvn -pl yudao-module-bpm,yudao-module-dcc -am "-Dtest=BpmDccRequiredCandidatesTest,DccControlledFileWorkflowServiceImplTest,DccControlledFileObsoleteServiceTest,DccThreeWorkflowBpmnMigrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 160 tests.
- RED/GREEN current-state frontend deprecated signoff field removal: `dcc-readiness-capability-contract-static.spec.js` and `dcc-upload-governance-ux-static.spec.js` first failed because they still required `selectedSignoffUserIds`; after updating upload readiness/new submit/working iteration submit to stop sending the deprecated field and tightening static assertions, `dcc-upload-training-checkbox-static.spec.cjs`, `dcc-working-iteration-submit-static.spec.js`, `dcc-readiness-capability-contract-static.spec.js`, and `dcc-upload-governance-ux-static.spec.js` all PASS.
- GREEN current-state frontend typing after deprecated signoff field removal: `pnpm ts:check` -> PASS.
- RED current-state route readiness actionType/manual signoff: `mvn -pl yudao-module-dcc -am "-Dtest=DccControlledFileRouteReadinessServiceTest#evaluate_actionRouteRejectsDeprecatedManualSignoffUsers" "-Dsurefire.failIfNoSpecifiedTests=false" test` first failed before fix because action route readiness still accepted the deprecated selected signoff field path and fell into the old selected-user overlay path.
- GREEN current-state route readiness actionType/manual signoff: same target after rejecting non-empty `selectedSignoffUserIds` for NEW/REVISION/OBSOLETE readiness and passing actionType through `/route-preview` -> PASS, 1 test.
- GREEN current-state readiness/workflow regression: `mvn -pl yudao-module-dcc -am "-Dtest=DccControlledFileRouteReadinessServiceTest,DccControlledFileWorkflowServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 147 tests.
- GREEN current-state frontend readiness actionType contract: `dcc-readiness-capability-contract-static.spec.js`, `dcc-upload-governance-ux-static.spec.js`, `dcc-upload-training-checkbox-static.spec.cjs` -> PASS; `pnpm ts:check` -> PASS.
- GREEN current-state focused BPM+DCC after actionType readiness: `mvn -pl yudao-module-bpm,yudao-module-dcc -am "-Dtest=BpmDccRequiredCandidatesTest,DccControlledFileRouteReadinessServiceTest,DccControlledFileWorkflowServiceImplTest,DccControlledFileObsoleteServiceTest,DccThreeWorkflowBpmnMigrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, BPM 5 tests + DCC 158 tests, total 163 tests.
- GREEN current-state frontend three-workflow static combination: 9 scripts PASS in one loop, covering route department candidate, process keys, upload training checkbox, working iteration submit, distribution/training workbench, obsolete FormCenter, obsolete entry, admin full config route and routes list display.
- GREEN current-state migration policy gate refresh: Codex bundled Python ran `run-release-migration-policy-gate.py` for the four-file dependency closure through `20260922_dcc_task_assignee_snapshot.sql` -> PASS, `status=passed`, `migrationCount=4`.
- RED current-state working iteration revision readiness: `node tests/e2e/dcc-working-iteration-submit-static.spec.js` first failed because browser working-iteration submit did not precheck `REVISION` action route readiness before submit.
- GREEN current-state working iteration revision readiness: same static test after adding `checkControlledFileRouteReadiness({ categoryId: row.categoryId, actionType: 'REVISION' })` -> PASS; related readiness/upload/process-key static group PASS; `pnpm ts:check` PASS.
- PASS asset visibility: `git check-ignore -v -- <new isolated Flowable test and implementation task evidence files>` -> no output/exit 1, and `git status --short -- <same paths>` shows them as untracked rather than ignored.
- GREEN current-state BPM: `mvn -pl yudao-module-bpm -am "-Dtest=BpmDccRequiredCandidatesTest,BpmUserTaskActivityBehaviorTest,BpmTaskExternalSignatureGuardTest,BpmProcessInstanceConvertTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 17 tests.
- GREEN current-state migration gate: `run-release-migration-policy-gate.py` with `sql/mysql/...` file paths -> PASS, `migrationCount=4`.
- RED current-state broad DCC: `mvn -pl yudao-module-dcc -am "-Dtest=DccControlledFileFinalizationServiceImplTest,DccControlledFileStatusListenerTest,DccControlledFileWorkflowServiceImplTest,DccThreeWorkflowBpmnMigrationTest,DccTaskAssigneeSnapshotMigrationTest,DccControlledFileApprovalRouteAssigneeResolverTest,DccApprovalRouteAdminServiceImplTest,DccControlledFileQueryServiceTest,DccControlledFileObsoleteFormEffectExecutorTest,DccControlledFileObsoleteServiceTest,DccApprovalTaskAdapterTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL because `DccControlledFileQueryServiceTest` missed the `attachmentService` mock after the service dependency was added.
- GREEN current-state query service: `mvn -pl yudao-module-dcc -am "-Dtest=DccControlledFileQueryServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 149 tests.
- GREEN current-state broad DCC: same broad DCC command after query fixture fix -> PASS, 426 tests.
- RED current-state frontend static: `node tests/e2e/dcc-admin-full-config-route-static.spec.js` -> FAIL because the script used the old `ruoyi-vue-pro` SQL path.
- GREEN current-state frontend static: `node tests/e2e/dcc-admin-full-config-route-static.spec.js`, `node tests/e2e/dcc-working-iteration-submit-static.spec.js`, `node tests/e2e/dcc-distribution-training-workbench-static.spec.js`, `node tests/e2e/dcc-obsolete-form-center-static.spec.js`, `node tests/e2e/dcc-obsolete-form-center-entry-static.spec.js`, `node tests/e2e/dcc-controlled-file-routes-list-display-static.spec.js` -> PASS/exit 0.
- GREEN current-state frontend: `pnpm ts:check` -> PASS after static-contract path fix.
- GREEN current-state backend adjacent: `mvn -pl yudao-module-dcc -am "-Dtest=DccTrainingAssignmentAckServiceTest,DccTrainingConcurrentAcknowledgementTest,DccTrainingTaskServiceTest,DccDistributionReceiptServiceImplTest,DccDistributionTaskServiceImplTest,DccPaperDistributionAckServiceTest,DccCategoryTrainingRuleAdminServiceImplTest,DccCategoryDistributionRuleAdminServiceImplTest,DccControlledFileFinalizationServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` using `.runtime` Maven and JDK 17 -> PASS, 85 tests.
- RED current-state broad with dependencies: `mvn -pl yudao-module-dcc -am "-Dsurefire.failIfNoSpecifiedTests=false" test` using `.runtime` Maven and JDK 17 -> FAIL before DCC, blocked by `yudao-module-system` test requiring missing local file `C:\ProjectPackage\erp-invoice-voucher-print-assistant\server.js`.
- RED current-state DCC full module: `mvn -pl yudao-module-dcc test` using `.runtime` Maven and JDK 17 -> FAIL, 2064 tests, 19 failures, 11 errors; task-adjacent stale fixture failures were fixed in this turn, remaining failures are non-task registration-certificate/project-code/product-catalog/schema broad-suite issues.
- GREEN current-state task fixture repair: `mvn -pl yudao-module-dcc -am "-Dtest=DccWorkingIterationSubmissionServiceTest,DccControlledFileRouteReadinessServiceTest,DccApprovalRouteAdminServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` using `.runtime` Maven and JDK 17 -> PASS, 38 tests.
- GREEN current-state expanded DCC task regression: `mvn -pl yudao-module-dcc -am "-Dtest=DccControlledFileFinalizationServiceImplTest,DccControlledFileStatusListenerTest,DccControlledFileWorkflowServiceImplTest,DccThreeWorkflowBpmnMigrationTest,DccTaskAssigneeSnapshotMigrationTest,DccControlledFileApprovalRouteAssigneeResolverTest,DccControlledFileRouteReadinessServiceTest,DccWorkingIterationSubmissionServiceTest,DccApprovalRouteAdminServiceImplTest,DccControlledFileQueryServiceTest,DccControlledFileObsoleteFormEffectExecutorTest,DccControlledFileObsoleteServiceTest,DccApprovalTaskAdapterTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` using `.runtime` Maven and JDK 17 -> PASS, 435 tests.
- GREEN current-state frontend/static refresh: `dcc-route-department-candidate-static.spec.cjs`、`dcc-three-workflow-process-keys-static.spec.cjs`、`dcc-upload-training-checkbox-static.spec.cjs`、`dcc-working-iteration-submit-static.spec.js`、`dcc-distribution-training-workbench-static.spec.js`、`dcc-obsolete-form-center-static.spec.js`、`dcc-obsolete-form-center-entry-static.spec.js`、`dcc-admin-full-config-route-static.spec.js`、`dcc-controlled-file-routes-list-display-static.spec.js` -> PASS/exit 0；`pnpm ts:check` -> PASS。
- RED/GREEN current-state migration gate refresh: root-level `--sql-root sql/mysql` 在当前工作区 FAIL；改用 `--sql-root IntRuoyiBackend/sql/mysql` 和 `IntRuoyiBackend/sql/mysql/...` 输入后 PASS，`migrationCount=4`。
- PASS current-state whitespace: `git diff --check` -> 无 whitespace error，仅 LF/CRLF warning。
- GREEN current-state matrix/config package: `mvn -pl yudao-module-dcc -am "-Dtest=DccCategoryApprovalMatrixAdminServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 17 tests；`mvn -pl yudao-module-dcc -am "-Dtest=DccAdminFullConfigPackageServiceTest,DccFileCategoryControllerConfigPackageContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 16 tests。
- GREEN current-state upload/publication/version adjacent: `mvn -pl yudao-module-dcc -am "-Dtest=DccControlledFileUploadApiTest,DccControlledFilePublicationFlowTest,DccControlledFileVersionNumberAllocationTest,DccApprovalVersionBindingTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 41 tests。
- GREEN current-state upload/source/preview adjacent: `mvn -pl yudao-module-dcc -am "-Dtest=DccSourceUploadContextTest,DccControlledFileUploadNameOptionQueryServiceTest,DccControlledFileUploadNameOptionApiTest,DccControlledFilePlatformAdapterTest,DccOnlyOfficeControlledPreviewTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 20 tests。
- RED/GREEN current-state frontend approval/upload static refresh: old DCC approval task view-mode/summary/publish-transition static contracts first failed on stale dedicated-page/view-switch and `待文控发布` assumptions; after narrowing them to the unified approval center redirect, DCC summary resolver, and final doc-control system activation design, the 12-script static suite passed. `pnpm ts:check` -> PASS。
- GREEN current-state approval/finalization adjacent backend: `mvn -pl yudao-module-dcc -am "-Dtest=DccApprovalTaskTimelineAdapterTest,DccApprovalPrintTemplateServiceTest,DccControlledContentAdapterTest,DccControlledFilePublishFormEffectExecutorTest,DccControlledFilePublishServiceTest,DccWorkingSubmissionConditionTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 41 tests。
- RED note: first `mvn` commands failed before product tests because `mvn` was not on PATH; rerun with repository `.runtime` Maven and `JAVA_HOME` fixed. This is recorded as an environment-command RED, not a product behavior RED.
- RED note: direct `pnpm exec vue-tsc --noEmit -p tsconfig.relaxed.json --pretty false` failed with Node heap out of memory; rerun with repository `pnpm ts:check` script, which sets `NODE_OPTIONS=--max-old-space-size=8192`, passed.
- RED note: first `DccApprovalTaskAdapterTest,DccControlledFileStatusListenerTest` run failed because the new FormCenter obsolete-key check called `Set.of(...).contains(null)` when legacy mocks did not expose processDefinitionKey; fixed by treating null key as the existing numeric businessKey path and reran GREEN.
- RED note: first `BpmProcessInstanceConvertTest` target with `-am` failed before reaching BPM because upstream `yudao-module-infra` test compile references missing `FileServiceImpl.updateUploadTime(...)`; rerun without `-am` for the BPM module. The BPM product RED then exposed missing explicit `businessKey` mapping from `HistoricProcessInstance`; fixed together with `businessObjectId` projection and reran GREEN.
- RED/environment current-state source-governance tenant static runner: bundled Python `python -m pytest IntRuoyiBackend/script/tests/test_dcc_source_governance_batch_tenant_id_sql.py` failed because `pytest` is not installed in that runtime.
- GREEN current-state source-governance tenant static contract: direct Python import/call of the three `test_*` functions in `IntRuoyiBackend/script/tests/test_dcc_source_governance_batch_tenant_id_sql.py` -> PASS, 3 tests.
- GREEN current-state source-governance schema regression: `mvn -pl yudao-module-dcc -am "-Dtest=DccSourceOwnershipSchemaTest,DccBaseSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` using repository `.runtime` Maven/JDK 17 -> PASS, 40 tests.
- GREEN current-state migration/schema combination: `mvn -pl yudao-module-dcc -am "-Dtest=DccThreeWorkflowBpmnMigrationTest,DccTaskAssigneeSnapshotMigrationTest,DccFormCenterPolicyMigrationTest,DccSourceOwnershipSchemaTest,DccBaseSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` using repository `.runtime` Maven/JDK 17 -> PASS, 44 tests.
- PASS current-state source-governance tenant gates: new static test asset is visible to Git and not ignored; `verify_tdd_compliance.py` with the new static test path included -> PASS; `git diff --check` -> PASS with no whitespace errors, only existing LF/CRLF warnings.
- GREEN current-state frontend static/type refresh: 14 DCC three-workflow/workbench/readiness static scripts PASS in one loop from `IntRuoyiFronted`; `pnpm ts:check` -> PASS.
- GREEN current-state backend governance entrypoint refresh: `mvn -pl yudao-module-dcc -am "-Dtest=DccControlledFileRouteReadinessServiceTest,DccCategoryApprovalMatrixAdminServiceImplTest,DccAdminFullConfigPackageServiceTest,DccFileCategoryControllerConfigPackageContractTest,DccControlledFileApprovalRouteAssigneeResolverTest,DccControlledFileObsoleteServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` using repository `.runtime` Maven/JDK 17 -> PASS, 56 tests.
- BLOCKED current-state cleanup preview gate: `task_closeout.py --task-id 20260921-dcc-three-workflows-implementation --mode preview` -> refused because task status is `in_progress`; expected `ready_for_closeout` or `completed`.
- GREEN current-state BPM/approval-center adjacent refresh: `mvn -pl yudao-module-bpm "-Dtest=BpmMessageServiceImplTest,BpmTaskConvertTest,BpmTaskServiceImplApprovalFilterTest,BpmProcessInstanceServiceImplTest,BpmNativeApprovalTaskProviderTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` using repository `.runtime` Maven/JDK 17 -> PASS, 62 tests.
- GREEN current-state training/distribution/finalization adjacent refresh: `mvn -pl yudao-module-dcc -am "-Dtest=DccTrainingAssignmentAckServiceTest,DccTrainingConcurrentAcknowledgementTest,DccTrainingTaskServiceTest,DccDistributionReceiptServiceImplTest,DccDistributionTaskServiceImplTest,DccPaperDistributionAckServiceTest,DccCategoryTrainingRuleAdminServiceImplTest,DccCategoryDistributionRuleAdminServiceImplTest,DccControlledFileFinalizationServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` using repository `.runtime` Maven/JDK 17 -> PASS, 85 tests.
- GREEN current-state BPM/approval-center adjacent: `mvn -pl yudao-module-bpm "-Dtest=BpmMessageServiceImplTest,BpmTaskConvertTest,BpmTaskServiceImplApprovalFilterTest,BpmProcessInstanceServiceImplTest,BpmNativeApprovalTaskProviderTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` using `.runtime` Maven and JDK 17 -> PASS, 62 tests.
- PASS current-state whitespace after BPM evidence refresh: `git diff --check` -> 无 whitespace error，仅 LF/CRLF warning。
- GREEN current-state history/approval-list/audit-log/timeline adjacent refresh: `mvn -pl yudao-module-bpm,yudao-module-dcc -am "-Dtest=BpmProcessInstanceControllerVisibilityContractTest,DccApprovalTaskAdapterTest,DccApprovalTaskTimelineAdapterTest,DccControlledFileAuditControllerTest,DccControlledFileLogControllerTest,DccControlledFileAuditQueryServiceTest,DccControlledFileLogQueryServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` using `.runtime` Maven and JDK 17 -> PASS, 36 tests.
- PASS current-state doc gates after history/approval-list evidence refresh: task status/cleanup keep structure check PASS; `verify_tdd_compliance.py` PASS; `git diff --check` PASS with no whitespace errors and LF/CRLF warnings only.
- GREEN current-state permission/preview/download/signature backend refresh: `mvn -pl yudao-module-dcc -am "-Dtest=DccControlledFilePreviewDownloadApiTest,DccControlledFilePreviewProtectionTest,DccControlledPreviewAccessServiceTest,DccControlledFileReviewMatrixAccessServiceTest,DccControlledFileViewMatrixAccessServiceTest,DccDirectoryAccessPermissionServiceTest,DccCategoryPermissionAdminServiceImplTest,DccControlledFileCategoryPermissionSupportTest,DccElectronicSignatureAuthorizationServiceTest,DccElectronicSignatureAuthorizationControllerTest,DccElectronicSignatureManagementServiceTest,DccElectronicSignatureImageServiceImplTest,DccElectronicSignatureFailureAuditServiceTest,DccDownloadPolicyServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` using `.runtime` Maven and JDK 17 -> PASS, 111 tests.
- RED/GREEN current-state permission/detail/signature/view-matrix frontend static refresh: selected DCC frontend static loop first failed at `dcc-view-matrix-unified-source-static.spec.js` because the old contract asserted independent view-matrix preview tokens inside `CategoryReviewMatrixTable.vue`; after updating the contract to the current split `CategoryReviewMatrixTable.vue` + `CategoryViewMatrixTable.vue` boundary and current testids, the failed script PASS and the full selected 26-script loop PASS. `pnpm ts:check` PASS.
- PASS current-state doc gates after permission/preview/signature evidence refresh: task status/cleanup keep structure check PASS; `verify_tdd_compliance.py` PASS; `git diff --check` PASS with no whitespace errors and LF/CRLF warnings only.
- GREEN current-state obsolete/FormCenter/withdraw-resubmit/idempotent-replay backend refresh: `mvn -pl yudao-module-bpm,yudao-module-dcc -am "-Dtest=DccControlledFileObsoleteServiceTest,DccControlledFileObsoleteFormEffectExecutorTest,DccControlledFileWorkflowServiceImplTest,DccPublicationNotificationTransactionIntegrationTest,DccPublicationNotificationServiceTest,DccPublicationNotificationPostCommitSchedulerTest,DccPublicationNotificationDispatchOrchestratorTest,DccPublicationFollowupTransactionIntegrationTest,DccPublicationFollowupStatusServiceTest,DccPublicationFollowupServiceTest,DccPublicationFollowupQueryServiceTest,DccMessageDeliveryIdempotencyTest,DccControlledFileMessageReplayServiceTest,FormCenterRuntimeBpmCallbackTest,FormCenterBpmEventBridgeTest,FormEffectOrchestratorTest,FormCenterRepositoryBoundaryTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` using `.runtime` Maven and JDK 17 -> PASS, 208 tests.
- PASS current-state doc gates after obsolete/FormCenter/withdraw-resubmit evidence refresh: task status/cleanup keep structure check PASS; `verify_tdd_compliance.py` PASS; `git diff --check` PASS with no whitespace errors and LF/CRLF warnings only.
- GREEN current-state BDD-25 concurrency/idempotency backend refresh: `mvn -pl yudao-module-bpm,yudao-module-dcc -am "-Dtest=FormCenterRuntimeIdempotencyLookupTest,DccTrainingConcurrentAcknowledgementTest,DccPublicationNotificationTransactionIntegrationTest,DccMessageDeliveryTransactionIntegrationTest,DccImpactAssessmentTransactionIntegrationTest,DccPublicationFollowupTransactionIntegrationTest,DccWorkingSubmissionConditionTest,DccMessageDeliveryIdempotencyTest,DccControlledFileMessageReplayServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` using `.runtime` Maven and JDK 17 -> PASS, BPM 10 tests + DCC 26 tests, total 36 tests.
- PASS current-state doc gates after BDD-25 concurrency/idempotency evidence refresh: task status/cleanup keep structure check PASS; `verify_tdd_compliance.py` PASS; `git diff --check` PASS with no whitespace errors and LF/CRLF warnings only.
- GREEN current-state BDD-30 config-change/frozen-plan backend refresh: `mvn -pl yudao-module-bpm,yudao-module-dcc -am "-Dtest=BpmDccRequiredCandidatesTest,DccControlledFileApprovalRouteAssigneeResolverTest,DccCategoryApprovalMatrixAdminServiceImplTest,DccTrainingTaskServiceTest,DccTrainingAssignmentAckServiceTest,DccTrainingConcurrentAcknowledgementTest,DccDistributionTaskServiceImplTest,DccDistributionReceiptServiceImplTest,DccPaperDistributionAckServiceTest,DccCategoryTrainingRuleAdminServiceImplTest,DccCategoryDistributionRuleAdminServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` using `.runtime` Maven and JDK 17 -> PASS, BPM 5 tests + DCC 64 tests, total 69 tests.
- PASS current-state doc gates after BDD-30 config-change/frozen-plan evidence refresh: task status/cleanup keep structure check PASS; `verify_tdd_compliance.py` PASS; `git diff --check` PASS with no whitespace errors and LF/CRLF warnings only.
- GREEN current-state BDD-29 migration policy gate refresh: `run-release-migration-policy-gate.py` using Codex bundled Python and full SQL paths -> PASS for three JSON evidence files: three workflows `migrationCount=3`, task assignee snapshot closure `migrationCount=4`, source-governance batch tenant closure `migrationCount=4`.
- GREEN current-state BDD-29 migration/schema backend refresh: `mvn -pl yudao-module-dcc -am "-Dtest=DccThreeWorkflowBpmnMigrationTest,DccTaskAssigneeSnapshotMigrationTest,DccFormCenterPolicyMigrationTest,DccSourceOwnershipSchemaTest,DccBaseSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` using `.runtime` Maven and JDK 17 -> PASS, 44 tests.
- PASS current-state doc gates after BDD-29 migration/deployment static evidence refresh: task status/cleanup keep structure check PASS; `verify_tdd_compliance.py` PASS; `git diff --check` PASS with no whitespace errors and LF/CRLF warnings only.
- PASS current-state duplicate task record audit: `doc/tasks/20260921-dcc-three-workflows-design` first `Current Status` line is `blocked`; `doc/tasks/20260921-dcc-three-workflows-implementation` first `Current Status` line is `in_progress`; no second same-scope implementation task remains `pending/in_progress`.
- GREEN current-state submission/action guard backend refresh: `mvn -pl yudao-module-dcc -am "-Dtest=DccWorkingIterationSubmissionServiceTest,DccWorkingSubmissionConditionTest,DccControlledFilePendingActionGuardTest,DccOrdinaryApprovalRemovedActionsTest,DccApprovalTaskAdapterTest,DccControlledFileTaskActionApiTest,DccApprovalReasonValidationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` using `.runtime` Maven and JDK 17 -> PASS, 44 tests.
- GREEN current-state BPM candidate strategy refresh: `mvn -pl yudao-module-bpm -am "-Dtest=BpmTaskCandidateDeptLeaderStrategyTest,BpmTaskCandidateDeptLeaderMultiStrategyTest,BpmTaskCandidateDeptMemberStrategyTest,BpmTaskCandidateStartUserDeptLeaderStrategyTest,BpmTaskCandidateStartUserDeptLeaderMultiStrategyTest,BpmTaskCandidateStartUserSelectStrategyTest,BpmTaskCandidateMixedStrategyTest,BpmTaskCandidateInvokerTest,BpmDccRequiredCandidatesTest,BpmUserTaskActivityBehaviorTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` using `.runtime` Maven and JDK 17 -> PASS, 25 tests.
- RED/GREEN current-state frontend workbench/route/training/distribution/permission static refresh: old static contracts first failed on stale `发布失败` wording, obsolete workbench-entry placement, category-only route preview, route form without department/actionType, old `正式下发权限` wording, old training mine `<el-form>` toolbar, old `category.id` rule loading and old `ruoyi-vue-pro/sql/mysql` path. After narrowing to current three-workflow wording, unified approval center/list template, actionType route preview/form, department signoff candidate and current workspace SQL path contracts, 14 scripts PASS; `pnpm ts:check` PASS。
- PASS current-state whitespace after frontend static evidence refresh: `git diff --check` -> 无 whitespace error，仅 LF/CRLF warning。
- GREEN current-state submission/action adjacent backend: `mvn -pl yudao-module-dcc -am "-Dtest=DccWorkingIterationSubmissionServiceTest,DccWorkingSubmissionConditionTest,DccControlledFilePendingActionGuardTest,DccOrdinaryApprovalRemovedActionsTest,DccApprovalTaskAdapterTest,DccControlledFileTaskActionApiTest,DccApprovalReasonValidationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` using `.runtime` Maven and JDK 17 -> PASS, 44 tests.
- PASS current-state whitespace after submission/action evidence refresh: `git diff --check` -> 无 whitespace error，仅 LF/CRLF warning。
- GREEN current-state FormCenter/DCC obsolete owner adjacent backend: `mvn -pl yudao-module-bpm,yudao-module-dcc -am "-Dtest=FormCenterTemplateObsoleteRuntimeTest,FormCenterRuntimeBpmCallbackTest,FormCenterBpmEventBridgeTest,FormTemplateObsoleteBusinessApprovalEffectExecutorTest,FormBpmBindingServiceTest,BusinessApprovalBpmEventListenerTest,BusinessApprovalEffectExecutorRegistryTest,DccFormCenterPolicyMigrationTest,DccControlledFileObsoleteFormEffectExecutorTest,DccControlledFileObsoleteServiceTest,DccObsoleteFileStorageServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` using `.runtime` Maven and JDK 17 -> PASS, BPM 51 tests + DCC 19 tests = 70 tests.
- PASS current-state whitespace after FormCenter/DCC obsolete owner evidence refresh: `git diff --check` -> 无 whitespace error，仅 LF/CRLF warning。
- RED/GREEN current-state BPM candidate strategy adjacent: first run failed in `BpmTaskCandidateInvokerTest#testCalculateUsersByTask_none` and `#testCalculateUsersByTask_some` because ordinary legacy tasks with null `processDefinitionKey` hit `Set.of(...).contains(null)` in the DCC process-key guard. Added a blank-key guard so null/blank process keys remain ordinary non-DCC tasks. Rerun `mvn -pl yudao-module-bpm -am "-Dtest=BpmTaskCandidateDeptLeaderStrategyTest,BpmTaskCandidateDeptLeaderMultiStrategyTest,BpmTaskCandidateDeptMemberStrategyTest,BpmTaskCandidateStartUserDeptLeaderStrategyTest,BpmTaskCandidateStartUserDeptLeaderMultiStrategyTest,BpmTaskCandidateStartUserSelectStrategyTest,BpmTaskCandidateMixedStrategyTest,BpmTaskCandidateInvokerTest,BpmDccRequiredCandidatesTest,BpmUserTaskActivityBehaviorTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` using `.runtime` Maven and JDK 17 -> PASS, 25 tests.
- PASS current-state whitespace after BPM candidate strategy null-key fix and evidence refresh: `git diff --check` -> 无 whitespace error，仅 LF/CRLF warning。
- GREEN current-state frontend static/type refresh: from `IntRuoyiFronted`, 14 DCC three-workflow/workbench/readiness scripts PASS in one loop; `pnpm ts:check` PASS.
- GREEN current-state delivery build refresh: backend `mvn -pl yudao-module-bpm,yudao-module-dcc -am "-DskipTests" package` PASS, 23 reactor modules SUCCESS; frontend `pnpm build:local` PASS with `Build successful. Please see dist directory`.
- PASS current-state Git audit: branch/status inspection shows no staged files; task docs and three migration policy JSON evidence files are visible to Git and not ignored; duplicate same-scope task audit remains design `blocked` and implementation `in_progress`.
- GREEN current-state Expected Verification aggregation: BPM+DCC P1 regression command PASS, BPM 14 tests + DCC 187 tests, total 201 tests.
- GREEN current-state normal-flow aggregation: `mvn -pl yudao-module-bpm,yudao-module-dcc -am "-Dtest=DccTrainingAssignmentAckServiceTest,DccTrainingConcurrentAcknowledgementTest,DccTrainingTaskServiceTest,DccDistributionReceiptServiceImplTest,DccDistributionTaskServiceImplTest,DccPaperDistributionAckServiceTest,DccCategoryTrainingRuleAdminServiceImplTest,DccCategoryDistributionRuleAdminServiceImplTest,DccControlledFileFinalizationServiceImplTest,DccControlledFileObsoleteFormEffectExecutorTest,DccControlledFileObsoleteServiceTest,FormCenterRuntimeBpmCallbackTest,FormCenterBpmEventBridgeTest,FormEffectOrchestratorTest,FormCenterRepositoryBoundaryTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` using `.runtime` Maven/JDK 17 -> PASS, BPM 38 tests + DCC 102 tests, total 140 tests.
- RED current-state broad DCC module attempt: `mvn -pl yudao-module-dcc -am "-Dsurefire.failIfNoSpecifiedTests=false" test` using `.runtime` Maven/JDK 17 -> FAIL before DCC module; upstream `yudao-module-system` had 772 tests with 1 error, `InvoiceVoucherPrintAssistantErpConfigBridgeContractTest` missing `C:\ProjectPackage\erp-invoice-voucher-print-assistant\server.js`.
- RED current-state DCC named broad regression: `mvn -pl yudao-module-dcc -am "-Dtest=Dcc*Test,*Dcc*Test" "-Dsurefire.failIfNoSpecifiedTests=false" test` using `.runtime` Maven/JDK 17 -> FAIL, DCC module 1988 tests, 14 failures, 4 errors, 0 skipped. Failures/errors are in registration-certificate, product catalog, and project-code adjacent suites; controlled-file three-workflow classes observed in the run remained PASS.

- BDD: 新三流程最终文控节点禁止通用审批中心快审 -> Given upload/revision 当前任务为 DOC_CONTROL_REVIEW 或文件状态为 PENDING_DOC_CONTROL_REVIEW, When 统一审批中心生成待办动作, Then 只暴露 PROCESS_IN_MODULE，不暴露 APPROVE/REJECT，必须进入 DCC 详情页完成专用证据与签名处理。
