# Execution Log

## 2026-08-10 Task Bootstrap

- 用户意图：在独立 worktree 中逐项修复已分析的 DCC 缺陷，逐项验证，全部通过后融合到 `int_main`。
- 原始基线：`E:\IntRuoyi` 位于 `int_main`，HEAD 为 `5699b8045`；主工作区存在大量其它任务的未提交改动，本任务不暂存、不提交、不清理这些改动。
- 已读取门禁：`docs/worktree-restrictions.md`、`docs/branch-runtime-ports.md`、`docs/task-closeout-rules.md`、`docs/powershell-memory.md`、`docs/experience-index.md` 及其中匹配的 DCC 上传、预览、审批、分发和脏工作区融合经验。
- worktree 已创建：`D:\IntRuoyiWorktree\dcc-critical-remediation`，分支 `codex/dcc-critical-remediation`，基线 HEAD `5699b8045`。
- 创建过程说明：初次 `git worktree add` 的调用在检出过程中返回，但底层 Git 进程继续运行；在检测到 `locked initializing` 和活动进程后未写入该目录，等待底层进程结束并确认锁消失、索引与 HEAD 均为 23569 个条目、工作树干净后才继续。
- Git 边界：用户明确授权最终融合到 `int_main`，因此本任务允许创建分支、提交和融合；用户未要求 push，当前不把 push 作为完成动作。
- M1 验证：`git status --short --branch` 仅返回 `## codex/dcc-critical-remediation`；任务目录和监督状态开始建立。
- 当前状态：进入监督式需求规划，尚未修改生产代码。

## 2026-08-10 Planning Gate

- 规划代理仅写入 `request-analysis.md` 与 `prd.md`，未修改监督状态、执行日志或生产代码。
- 审查结果：PASS。两个工件反映真实仓库边界，明确列出约束、未知项、风险和阻塞前置；`AC-01` 至 `AC-18` 与用户报告逐项对应且可测试。
- 特别门禁：AC-06/AC-07 不把 `NOT_APPLICABLE` 猜作现场 1080000092 的唯一原因；AC-18 在落约束或迁移前必须先只读盘点共享 `sourceFileId` 的来源与合法版本边界。
- 状态推进：`planner_review_status=approved`，进入依赖任务图与测试计划审查。

## 2026-08-10 Decomposition Gate

- `dev-plan.md` 定义 14 个有依赖任务，并标明 DCC 查询、工作流、上传控制器等共享高风险写入冲突必须串行。
- `test-plan.md` 定义 18 个逐项测试和 4 个集成测试；结构检查确认每个任务/测试均含工件合同要求的全部字段。
- 审查结果：PASS。所有 `AC-01..AC-18` 均映射到实现和测试，系统验证未被推迟为泛化的“稍后测试”。
- 状态推进：`plan_review_status=approved`，T1 为唯一 ready 任务，先进行只读取证与不变量冻结。

## 2026-08-11 T1 现场取证与不变量冻结

- 取证范围：本机 Docker MySQL `ruoyi-vue-pro`、当前 `int_main` 运行日志、DCC/infra 源码；只读查询，未访问远端、未写数据库、未输出凭据。
- 环境证据：本机 MySQL/Redis/MinIO 容器均运行，MinIO ready HTTP 200，当前基准后端 health 为 UP。
- AC-18：共享 `sourceFileId` 共 43 组，其中 37 组跨不同 master，影响 289 条正式记录；因此不能把共享解释为纯粹的同 master 版本历史。
- AC-01：临时记录与正式 DCC artifact 的 fileId 重叠共 4360 条，其中 379 条仍是 `AVAILABLE + ACTIVE + unbound`，按当前清理条件会进入物理删除候选。
- 现场样本：正式文件 `2054545668044070311` 引用 source `9198354916487` 和 drawing PDF `9198354916488`；临时记录 `540/541` 对同一两个 fileId 仍为可清理状态，而 `542/543` 已绑定到该正式文件。该样本可直接复现“清理临时会话会删除正式文件”的危险前置。
- 根因代码：`DccControlledFileUploadServiceImpl` 调用 `fileService.createFile(...)` 后再按 URL `selectFirstOne`，而 infra 全局文件路径时间戳后缀当前关闭；重复路径会产生多个 `infra_file` 行指向同一对象 URL，并可能反查到旧 fileId。当前库有 3677 组重复有效 path，涉及 14622 个文件行。
- AC-06/07：发布文件中共有 139 组四条签名满足数据库浅层 `evidenceStatus=VALID` 且四条均为 `controlledCopyHashStatus=NOT_APPLICABLE`；`getSignatureExportSummary` 只做浅层字段判断，导出则调用完整 HMAC/图片验证器，静态确认二者不是同一事实源。
- 签名结构核对：四签名组的必填字段、签名图片记录、fileId 和数据库快照 hash 无缺失/错配；当前 `int_main` 日志明确报签名 HMAC 运行配置缺失（1080000086），因此本轮不能安全复算并断言现场 1080000092 的唯一 HMAC 原因。此未知不以 `NOT_APPLICABLE` 猜测替代。
- AC-16：当前库同阶段 snapshot 包含 admin、但同阶段 Flowable assignee/candidate 不含 admin 的数量为 0。存在大量“未来阶段 snapshot 含 admin、当前阶段任务分配给其他人”的合法情况；实现必须按当前 stage 比对，不能把完整路线展示当成当前可审批权。
- T1 结果：PASS（取证与边界冻结完成）；T2、T6、T11 依赖满足并进入 ready。

## 2026-08-11 T2 临时清理引用保护与幂等状态机

- 状态推进：T2 `in_progress`，当前仅完成设计冻结，尚未修改生产代码。
- BDD: 清理已被正式受控记录引用的临时文件 -> Given 同一 `storageFileId` 同时被未绑定临时记录和正式受控记录的 source/original/drawing/training/published/stamped 字段引用；When 用户清理临时上传会话或定时清理过期上传；Then 仅释放临时记录，底层 `infra_file` 与对象保持可读，正式记录不受影响。
- BDD: 清理独占临时文件 -> Given 未绑定临时记录是某 `storageFileId` 的唯一 DCC 引用；When 清理命中该记录；Then 先以 CAS 将状态从 `ACTIVE` 置为 `CLEANING`，再物理删除，最后置为 `CLEANED`，且并发请求只有一个请求取得清理权。
- BDD: 物理删除后状态写入失败可确定恢复 -> Given 首次清理已取得 `CLEANING` 且底层文件已经删除，但最终状态更新未完成；When 相同或新的 requestId 重试该会话；Then 系统识别文件已不存在并完成 `CLEANED`，不会再次调用删除或返回“文件不存在”。
- BDD: 删除失败不伪装成功 -> Given 清理已取得 `CLEANING` 但存储删除抛错；When 接口返回失败后再次清理；Then 首次错误原样暴露，记录保持可恢复状态；恢复后重试只执行尚未完成的删除并最终成功。
- BDD: 清理审计失败不产生部分清理 -> Given 边界审计持久化失败；When 用户发起会话清理；Then 清理服务尚未被调用，正式文件、临时文件和状态均不变化。
- 数据设计结论：现有 `cleanup_status varchar(32)` 足以表达 `ACTIVE/CLEANING/CLEANED`；不新增 fallback 状态或列。正式引用判断集中在 DCC mapper，并显式带 tenant 条件；历史共享对象优先保证不误删。
- 实现结果：DccUploadTicketServiceImpl 采用 ACTIVE -> CLEANING -> CLEANED CAS 状态机；清理前调用 DccControlledFileTemporaryFileMapper.countActiveDccStorageReferencesByStorageFileId(tenantId, storageFileId) 保护正式 DCC artifact；CLEANING 记录允许重试，底层文件记录已不存在时只完成状态，不再次物理删除。
- RED: mvn "-pl" "yudao-module-dcc" "-Dtest=DccUploadTicketServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL，旧实现无法满足正式引用不删除、CAS 清理和 CLEANING 重试断言。
- GREEN: mvn "-pl" "yudao-module-dcc" "-Dtest=DccUploadTicketServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS，Tests run: 16, Failures: 0, Errors: 0, Skipped: 0。
- T2 结论：AC-01/AC-12 的目标单元验证通过；完整 DCC 模块回归保留到系统回归阶段。

## 2026-08-11 T3 文件真实可读性与预览投影

- 状态推进：T3 in_progress，先补 AC-02 RED，目标是让详情、列表、版本历史和预览元数据在 infra_file 记录缺失时形成一致不可预览结果。
- BDD: 缺失已发布源文件关闭详情预览 -> Given 已发布受控记录仍保存 publishedFileId，但对应 infra_file 记录已经不存在；When 用户打开文件详情；Then canPreview=false、previewUnavailableReason 指向缺失 artifact，动作投影不再包含 PREVIEW，版本历史同样关闭预览。
- BDD: 缺失已发布源文件关闭列表预览 -> Given 列表返回同一类已发布受控记录；When 用户查询文件列表；Then 行级 canPreview=false 且原因与详情一致，不因有权限或请求人身份显示“可以预览”。
- BDD: 预览元数据不为缺失文件签发访问证据 -> Given publishedFileId 对应的 infra_file 记录不存在；When 用户请求预览元数据；Then 只返回不可预览原因，不创建 access event、viewer token 或 OnlyOffice token。
- RED: mvn "-pl" "yudao-module-dcc" "-Dtest=DccControlledFileQueryServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL，新增 AC-02 断言要求 DccControlledFileRespVO 和 DccControlledFileVersionHistoryRespVO 暴露 previewUnavailableReason，旧 VO/服务尚未实现。
- 实现结果：详情、列表和版本历史通过 resolvePreviewArtifactProjection(...) 统一检查预览 artifact 的 infra_file 记录；缺失时 canPreview=false、写入 previewUnavailableReason，并从动作投影移除 PREVIEW。预览元数据在 artifact 缺失时不调用 previewAccessService.prepareAccess(...)，不签发 viewer token 或 OnlyOffice token。
- GREEN: mvn "-pl" "yudao-module-dcc" "-Dtest=DccControlledFileQueryServiceTest#getPreviewMetadata_activeOfficeFileWithMissingPublishedArtifactReturnsUnavailableReason+getControlledFile_missingPublishedFileRecordDisablesPreviewProjection+getControlledFilePage_missingPublishedFileRecordDisablesPreviewProjection" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS，Tests run: 3, Failures: 0, Errors: 0, Skipped: 0。
- GREEN: mvn "-pl" "yudao-module-dcc" "-Dtest=DccControlledFilePreviewProtectionTest,DccOnlineFilePreviewServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS，Tests run: 11, Failures: 0, Errors: 0, Skipped: 0。
- 相邻回归：mvn "-pl" "yudao-module-dcc" "-Dtest=DccControlledFileQueryServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL，当前仅剩基线已存在的 ordinaryResponseVoTypes_doNotExposeUnderlyingFileCapabilities 断言与当前 VO 公开 sourceFileId/originalFileId/publishedFileId/stampedFileId 的既有接口合同冲突；正常文件可预览用例的 infra_file 夹具已补齐，其它 78 个 QueryService 用例通过。
- T3 结论：AC-02 的后端详情/列表/版本历史/预览元数据目标验证通过；完整 QueryService 类剩余的既有 VO 暴露合同冲突保留到后续前端/API 合同收敛阶段处理。

## 2026-08-11 T4 分类前置校验与上传槽位幂等

- 状态推进：T4 `in_progress`，先补 AC-09/AC-11 的失败测试和数据库结构合同。
- BDD: 无效分类预上传零副作用 -> Given 分类不存在、已停用或生命周期阶段无效；When 调用 DCC 预上传；Then 在大小策略和二进制持久化前返回明确业务错误，不创建 `infra_file`、临时记录或 ticket。
- BDD: 同内容上传重试复用活动 ticket -> Given 相同 tenant、用户、session、purpose 和文件内容已有可绑定活动 ticket；When 顺序重试或并发预上传；Then 返回原 ticket，活动临时记录保持一条，重试请求不新增底层文件。
- BDD: 同槽不同内容明确冲突 -> Given 同一活动槽位已有不同内容；When 再次预上传；Then 返回上传槽位冲突，不创建第二个 ticket 或底层文件。
- BDD: 数据库并发唯一性 -> Given 两个请求在应用层同时未查询到活动槽位；When 并发插入；Then数据库唯一约束只允许一条活动记录，失败请求按已提交赢家的内容 hash 复用 ticket 或返回冲突。
- 数据设计：MySQL 临时表增加基于 `deleted/status/cleanup_status/bound_controlled_file_id` 计算的 `active_slot_unique_flag` 生成列，并以 `(tenant_id, uploader_id, session_id, purpose, active_slot_unique_flag)` 建唯一键；历史重复活动槽位必须显式阻塞迁移，禁止静默删除或合并。
- RED: mvn "-pl" "yudao-module-dcc" "-Dtest=DccControlledFileUploadApiTest,DccUploadTicketServiceTest,DccUploadSlotSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL，Tests run: 42；旧实现未在预上传前校验分类、同槽同内容生成新 ticket、同槽不同内容未报冲突、并发唯一键异常直接外泄，且缺少 `20260811_dcc_upload_slot_idempotency.sql`。
- 实现边界：分类存在性、启用状态和生命周期阶段在大小策略与文件读取前校验；预检以内容 SHA-256 复用同槽 ticket 或拒绝冲突；并发窗口由数据库唯一键裁决，失败请求只按已提交记录的 hash 决定复用或冲突；竞争产生的 loser `infra_file` 由上传服务显式删除，删除异常不得吞掉。
- GREEN: mvn "-pl" "yudao-module-dcc" "-Dtest=DccControlledFileUploadApiTest,DccUploadTicketServiceTest,DccUploadSlotSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS，Tests run: 45, Failures: 0, Errors: 0, Skipped: 0。
- T4 结论：AC-09 的分类零副作用和 AC-11 的活动槽位应用/数据库幂等已通过定向验证；不同内容已输出独立冲突业务码，HTTP 409 投影按任务依赖在 T5/AC-13 统一验证。

## 2026-08-11 T5 DCC 显式租户头与端点 HTTP 契约

- 状态推进：T5 `in_progress`，先补 AC-10/AC-13 的租户校验、响应状态和前端请求合同 RED。
- BDD: 已登录请求缺少显式租户头零副作用 -> Given 用户已登录且 token 归属 tenant 31；When 对 upload-preview、upload-temporary/status 或 session-cleanup 省略 `tenant-id`；Then 在控制器和业务服务前返回 HTTP 400，响应体业务码非成功。
- BDD: 非法或跨租户头被拒绝 -> Given 用户归属 tenant 31；When `tenant-id` 为非整数、非正数或 tenant 32；Then 非法格式返回 HTTP 400、跨租户返回 HTTP 403，上传、查询和清理服务均不执行。
- BDD: DCC 目标端点 HTTP 状态与业务码一致 -> Given 目标端点产生未登录、无权限、参数错误、资源不存在、槽位冲突或内部异常；When 响应写出；Then HTTP 状态分别为 401/403/400/404/409/500，响应体仍保留原业务码；成功保持 2xx。
- BDD: 前端显式发送租户头 -> Given 前端缓存存在正整数系统租户；When 调用三个目标接口；Then 每个请求配置显式携带 `tenant-id`；缓存缺失或非法时客户端在发请求前抛出合同错误，不依赖 Axios 全局补头。
- 设计边界：不全局改写 CommonResult 或租户 starter；DCC MVC 校验器仅匹配三个目标 URI，HTTP 状态过滤器同样只包装三个目标 URI，避免缓存下载等大响应。
- RED: `mvn "-pl" "yudao-module-dcc" "-Dtest=DccExplicitTenantRequestValidatorTest,DccApiHttpContractFilterTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，测试编译明确缺少 `DccExplicitTenantRequestValidator` 和 `DccApiHttpContractFilter`。
- RED: `node tests/e2e/dcc-upload-explicit-tenant-http-contract-static.spec.js` -> FAIL，`workflow.ts` 尚无正整数租户 fail-fast helper，三个目标请求也未显式附带校验后的 `tenant-id`。
- 实现：为 DCC 预上传、临时会话清理与专用上传端点增加显式租户头校验；缺失租户头返回 400，租户上下文不一致返回 403；HTTP 状态映射仅覆盖这三个端点，其他 `CommonResult` 接口合同不变。前端 DCC 上传与清理请求统一显式透传 `tenant-id`。
- 实现：恢复详情、列表与版本历史中的文件 ID 字段赋值，保持既有 API/适配器合同；T3 的 `canPreview` 真实可读性判断不变。
- GREEN: `mvn "-pl" "yudao-module-dcc" "-Dtest=DccExplicitTenantRequestValidatorTest,DccApiHttpContractFilterTest,DccControlledFileTemporaryCleanupControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，Tests run: 8，Failures: 0，Errors: 0，Skipped: 0。
- GREEN: `node tests/e2e/dcc-upload-explicit-tenant-http-contract-static.spec.js` -> PASS。
- 结论：T5 完成，AC-10 与 AC-13 的 DCC 端点合同通过；进入 T6 审批参与人岗位门禁与明确错误码。

## 2026-08-11 T6 路线参与人岗位门禁与明确错误

- 状态推进：T6 `in_progress`，先补 AC-03/AC-04 的路线、转办、加签、签名与 HTTP 合同 RED。
- BDD: 无岗位候选人不得进入路线 -> Given 路线解析或路线管理预览得到 `postIds=[]` 的启用用户；When 解析可审批人；Then 返回独立岗位缺失业务码并阻止流程创建，补齐岗位后同一路线可通过。
- BDD: 无岗位转办/加签目标零副作用 -> Given 转办或加签目标用户存在但无系统岗位；When 当前审批人提交转办或加签；Then 在签名和 Flowable 操作前明确拒绝，不写签名、不更新路线快照、不推进任务。
- BDD: 签名前岗位被移除明确拒绝 -> Given 用户已取得实际审批任务但签名前岗位配置被移除；When 校验密码并签名；Then 返回“审批人未配置系统岗位”的独立业务码，不创建证据、签名记录或签名图片引用。
- BDD: 审批动作岗位错误使用 HTTP 4xx -> Given approve/reject/return/transfer/sign 端点返回岗位缺失业务码；When DCC 响应合同写出；Then HTTP 状态为 400 且响应体保留独立业务码。
- RED: `mvn "-pl" "yudao-module-dcc" "-Dtest=DccApprovalParticipantPostValidatorTest,DccControlledFileApprovalRouteAssigneeResolverTest,DccControlledFileSignatureServiceTest,DccControlledFileWorkflowServiceImplTest,DccApiHttpContractFilterTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，测试编译明确缺少岗位校验器和独立错误码；旧 HTTP 过滤器也不识别审批动作路径。
- 实现：新增 `DccApprovalParticipantPostValidator` 和稳定业务码 `1080000199/审批人未配置系统岗位`；路线解析、路线管理预览、转办与加签在用户存在校验后统一执行岗位门禁。签名快照把无岗位从 1080000023 中拆出，并在签名图、证据和签名记录写入前拒绝。
- 实现：DCC HTTP 合同过滤器增加 approve/reject/return/transfer/sign 动态路径识别，岗位缺失响应投影为 HTTP 400；其它 DCC 与系统端点合同不变。
- 修复验证前置：T4 测试建表脚本的生成列移除 MySQL 专用 `STORED`，保留同一生成表达式和唯一键，使 H2 数据库测试恢复可执行；正式 MySQL 迁移未改变。
- GREEN: `mvn "-pl" "yudao-module-dcc" "-Dtest=DccApprovalParticipantPostValidatorTest,DccControlledFileApprovalRouteAssigneeResolverTest,DccControlledFileSignatureServiceTest,DccControlledFileWorkflowServiceImplTest,DccApiHttpContractFilterTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，Tests run: 125，Failures: 0，Errors: 0，Skipped: 0。
- GREEN: `mvn "-pl" "yudao-module-dcc" "-Dtest=DccApprovalRouteAdminServiceImplTest,DccUploadSlotSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，Tests run: 15，Failures: 0，Errors: 0，Skipped: 0。
- 结论：T6 完成，AC-03/AC-04 的后端门禁、明确错误与零副作用合同通过；进入 T7 聚合路线就绪性与提交人组织预检。

## 2026-08-11 T7 聚合路线就绪性与提交人组织预检

- 状态推进：T7 `in_progress`，先补 AC-05/AC-17 的聚合缺项与正式提交强校验 RED。
- BDD: 同一路线一次返回全部审批人缺项 -> Given 同一节点分别包含缺岗位、缺阶段权限、未授权电子签名和无有效签名图片的用户；When 提交人执行路线预检；Then 响应 `ready=false`，按节点和用户一次返回四类稳定 blocker，而不是在首个缺项处终止。
- BDD: 上传人派生岗位组织缺项结构化返回 -> Given 路线包含编制人直接主管/部门负责人岗位且提交人无启用部门、无负责人或负责人无效；When 执行路线预检；Then 返回提交人组织映射 blocker 和具体原因，不创建流程或正式记录。
- BDD: 正式提交复用权威就绪性服务 -> Given 页面预检后任一审批人配置失效；When 调用正式提交；Then 提交事务重新强校验并拒绝，受控记录、路线快照、ticket 绑定和 Flowable 实例均不产生。
- RED: `mvn "-pl" "yudao-module-dcc" "-Dtest=DccControlledFileRouteReadinessServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，测试编译明确缺少聚合路线就绪性服务及 `resolveRouteForReadiness` 合同。
- 实现：新增结构化路线就绪响应 `ready/nodes/blockers`，blocker 固定携带 reasonCode、消息、阶段和用户；路线解析提供不提前抛岗位错误的 readiness 模式，使岗位、阶段权限、电子签名授权和有效签名图可以一次聚合。
- 实现：上传人派生岗位的组织映射错误转换为 `SUBMITTER_ORG_MAPPING_INVALID`，保留底层具体原因；手工会签用户在同一服务内覆盖 MATRIX_REVIEW 节点并执行同等就绪检查。
- 实现：`/dcc/controlled-files/route-preview` 返回聚合 readiness，正式提交在事务内调用同一服务的 `requireReady()`；不就绪时返回 1080000200，且不插入受控记录、不绑定 ticket、不写快照或创建 Flowable 实例。
- GREEN: `mvn "-pl" "yudao-module-dcc" "-Dtest=DccApprovalPositionRuntimeResolverTest,DccControlledFileRouteReadinessServiceTest,DccControlledFileApprovalRouteAssigneeResolverTest,DccControlledFileWorkflowServiceImplTest,DccControlledFileSignatureServiceTest,DccApiHttpContractFilterTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，Tests run: 131，Failures: 0，Errors: 0，Skipped: 0。
- 结论：T7 完成，AC-05/AC-17 的后端聚合预检与提交事务强校验通过；上传页展示和请求集成留在依赖 T13，进入 T8 最终批准就绪性与 snapshot/runtime 一致性。

## 2026-08-11 T8 最终批准就绪性与 snapshot/runtime 一致性

- 状态推进：T8 `in_progress`，先补 AC-15/AC-16 的最终批准多缺项聚合、批准强校验和快照/运行时任务漂移 RED。
- BDD: 最终批准一次返回全部必要条件 -> Given 文控最终批准尚未提供盖章 PDF、确认目录、培训记录和分发范围；When 加载任务动作就绪性；Then 一次返回全部 blocker，不按固定顺序逐次报错。
- BDD: 最终批准事务重复聚合强校验 -> Given 页面加载后必要条件仍不完整；When 提交批准；Then 返回独立的最终批准未就绪业务码，且不写签名、不绑定盖章 ticket、不更新文件、不推进 Flowable。
- BDD: 快照包含用户但实际任务分配给他人 -> Given 路线快照包含 admin，但 Flowable 当前任务 assignee 为另一用户；When admin 操作任务；Then 返回路线运行态不一致错误，快照不授予审批权限，签名和任务均不推进。
- BDD: 正式转办/加签后的运行态一致 -> Given 转办或加签通过 DCC 正式接口更新 Flowable 和阶段快照；When 新审批人执行任务；Then 快照与实际 assignee 一致并允许继续，不把合法变化误判为漂移。
- RED: `mvn "-pl" "yudao-module-dcc" "-Dtest=DccControlledFileWorkflowServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，测试编译明确缺少任务就绪度 DTO、服务方法以及最终批准未就绪/路线运行态不一致错误码。
- 实现：新增 `/dcc/controlled-files/{id}/task-action-readiness`，对最终文控批准一次聚合盖章 PDF、确认目录、培训记录和分发部门 blocker；正式批准复用同一 evaluator 并在签名、ticket 绑定、文件更新和 Flowable 推进前强校验。
- 实现：Flowable 拒绝任务操作时只读比对当前流程实例、当前阶段、当前阶段快照和实际 assignee；快照授权用户与实际分配用户不一致时返回 1080000201。正常转办/加签继续先更新 Flowable、再同步阶段快照，既有一致性回归保持通过。
- 实现：最终批准不就绪统一返回 1080000202 并携带全部中文缺项；任务就绪度端点和审批动作错误纳入 DCC HTTP 400 合同。
- GREEN: `mvn "-pl" "yudao-module-dcc" "-Dtest=DccControlledFileWorkflowServiceImplTest,DccApiHttpContractFilterTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，Tests run: 112，Failures: 0，Errors: 0，Skipped: 0。
- 结论：T8 完成，AC-15/AC-16 的最终批准聚合预检、事务内零副作用强校验和当前阶段 snapshot/runtime 漂移诊断通过；进入 T9 签名证据单一事实源与受控副本哈希绑定。

## 2026-08-11 T9 签名验证单一事实源与受控副本绑定

- 状态推进：T9 `in_progress`，先补 AC-06/AC-07 的逐条复算、摘要/导出一致性、发布绑定事件和副本篡改 RED。
- BDD: 摘要与导出复用同一验证结果 -> Given 同一文件存在有效、HMAC 篡改、签名图片失效或上下文不完整的签名；When 查询摘要、单条验证或导出；Then 每条签名返回相同稳定失败原因，任一无效时摘要为 false 且导出阻断，摘要为 true 时导出成功。
- BDD: 发布副本建立不可变绑定事件 -> Given 最终化已解析盖章 PDF 且文件存在四条签名；When 发布或训练门禁准备完成；Then 为每条签名写入唯一绑定事件，记录原 evidenceHash、发布 fileId、PDF SHA-256、绑定时间、操作者、事件键和事件哈希，不更新原签名 evidenceHash。
- BDD: 发布绑定幂等且冲突失败 -> Given 相同签名已绑定相同发布 PDF；When 最终化重试；Then 不新增或改写事件；若 fileId、hash 或原 evidenceHash 不同则明确失败，不覆盖历史。
- BDD: 受控副本篡改可定位 -> Given 发布后绑定事件有效；When 底层 PDF 内容被替换；Then 摘要和单条验证返回 `CONTROLLED_COPY_HASH_MISMATCH`，导出拒绝且不把数据库浅层 `evidenceStatus=VALID` 当成有效。
- RED: `mvn "-pl" "yudao-module-dcc" "-Dtest=DccControlledFileSignatureBindingServiceTest,DccElectronicSignatureManagementServiceTest,DccControlledFileFinalizationServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，测试编译明确缺少签名绑定事件 DO/Mapper/Service/Verification、绑定错误码和摘要逐条失败原因字段。
- 实现：新增 `dcc_controlled_file_signature_binding` 不可变事件表和 `(tenant_id, signature_id, deleted)` 唯一约束；事件保存原 evidenceHash、发布副本 fileId/SHA-256、绑定操作者/时间/事件键及含租户上下文的 SHA-256 事件摘要。重复同副本绑定幂等，不同副本或原证据冲突明确失败，原签名记录不更新。
- 实现：最终文控批准在签名记录产生后、文件和 BPM 写副作用前直接为所有阶段签名绑定盖章 PDF；流程最终化重复执行同一绑定并验证事件幂等，训练门禁发布同样覆盖。
- 实现：摘要、单条详情、单条验证和导出共用 `verifySignature`，统一复算 payload 版本、HMAC 算法/key、当前租户 canonical payload、源文件内容 SHA-256、签名图片快照和受控副本绑定/实际内容；每条摘要返回稳定 `verificationReason`。摘要为 true 与导出使用完全相同的计算结果。
- 实现：发布副本绑定通过独立投影显示 `BOUND`、fileId、SHA-256、事件键和绑定时间，不改写签名时 canonical payload 或 evidenceHash；底层 PDF 被替换时返回 `CONTROLLED_COPY_HASH_MISMATCH` 并阻断导出。
- 数据库合同：新增 `sql/mysql/20260811_dcc_signature_copy_binding.sql`，H2 测试 schema 同步，结构测试验证事件字段和租户内签名唯一键。
- GREEN: `mvn "-pl" "yudao-module-dcc" "-Dtest=DccControlledFileSignatureBindingServiceTest,DccElectronicSignatureManagementServiceTest,DccControlledFileFinalizationServiceImplTest,DccControlledFileWorkflowServiceImplTest,DccSignatureBindingSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，Tests run: 160，Failures: 0，Errors: 0，Skipped: 0。
- 结论：T9 完成，AC-06/AC-07 的摘要/导出一致性、稳定失败原因、最终签名直接绑定、早期签名发布绑定、事件幂等和 PDF 篡改检测通过；进入 T10 有效电子分发收件人访问。

## 2026-08-11 T10 有效电子分发收件人访问

- 状态推进：T10 `in_progress`，先补 AC-08 的具体文件/租户/电子介质/有效状态授权 RED。
- BDD: 有效电子收件人访问三入口 -> Given 用户是当前租户、当前受控文件 `PUBLIC_FOLDER` 分发的有效收件人，且不是申请人、目录管理员或查看矩阵成员；When 访问详情、预览和下载；Then 三入口均放行，详情能力投影与访问解释一致，读取继续产生原有审计证据。
- BDD: 非收件人保持拒绝 -> Given 同一文件的普通用户没有分发收件关系；When 访问详情、预览或下载；Then 与现有拒绝合同一致返回 403，不扩大目录、审批、打印或其它文件权限。
- BDD: 纸质/回收/跨租户关系不授权 -> Given 用户仅有 `PAPER` 分发、`RECOVERED` 分发或另一租户的同编号关系；When 访问目标文件；Then 不作为电子分发授权，详情、预览和下载继续拒绝。
- RED: `mvn "-pl" "yudao-module-dcc" "-Dtest=DccControlledFileQueryServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，测试编译明确缺少当前租户有效电子分发收件人查询契约。
- 实现：分发授权查询显式联结 recipient/distribution 并同时限定两表 tenant、目标 controlledFileId、recipientUserId、`PUBLIC_FOLDER`、未回收有效状态和逻辑删除标记；仅 `ACTIVE` 当前发布文件将该关系用于详情、预览和下载，保留原下载策略与审计，不授予审批、打印或目录管理权限。
- GREEN: `mvn "-pl" "yudao-module-dcc" "-Dtest=DccControlledFileQueryServiceTest#getControlledFile_activeElectronicDistributionRecipientCanAccessWithoutViewMatrix+getPreviewMetadata_activeElectronicDistributionRecipientCanAccess+readDownloadFile_activeElectronicDistributionRecipientCanDownloadWithoutCategoryOrDirectoryGrant+getControlledFile_recoveredPaperOrCrossTenantRelationDoesNotAuthorize" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，Tests run: 4，Failures: 0，Errors: 0，Skipped: 0。
- 回归说明：整类 `DccControlledFileQueryServiceTest` 另有既存 `ordinaryResponseVoTypes_doNotExposeUnderlyingFileCapabilities` 失败，原因是当前详情 VO 暴露原始 fileId；该冲突属于依赖 T13 的能力投影清理，不影响 T10 新增 4 个测试，必须在 T14 前消除。
- 结论：T10 完成，AC-08 的当前租户有效电子分发收件人详情/预览/下载放行及纸质、回收、跨租户拒绝通过；进入 T11 通用上传危险可执行文件拦截。

## 2026-08-11 T11 通用上传危险可执行文件拦截

- 状态推进：T11 `in_progress`，先补 AC-14 的扩展名、内容特征、创建前零副作用及 HTTP 400 RED。
- BDD: 可执行扩展名在创建前拒绝 -> Given 文件名以 `.exe`、大小写变体或尾随点空格结尾；When 调用 `/infra/file/upload`；Then 返回明确业务错误和 HTTP 400，且不调用存储或插入文件记录。
- BDD: PE 内容伪装仍拒绝 -> Given 文件名伪装为 PDF 但内容包含有效 DOS/PE 头；When 上传；Then 内容策略识别并在创建前拒绝。
- BDD: 正常文件保持原行为 -> Given 普通 PDF、文本或图片内容且文件名不含危险扩展；When 上传；Then 通过策略并返回既有管理端代理 URL。
- RED: `mvn "-pl" "yudao-module-infra" "-Dtest=FileUploadSecurityPolicyTest,FileControllerTest,InfraFileUploadHttpContractFilterTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，测试编译明确缺少上传安全策略、可执行文件错误码和 `/infra/file/upload` HTTP 400 合同过滤器。
- 实现：`/infra/file/upload` 在调用 `FileService` 前执行规范化文件名扩展校验和 DOS/PE 头复核；`.exe` 大小写/尾随点空格/双扩展及伪装 PE 均返回 1001003020。仅该端点的该业务码映射 HTTP 400，正常上传仍返回原管理端代理 URL。
- GREEN: `mvn "-pl" "yudao-module-infra" "-Dtest=FileUploadSecurityPolicyTest,FileControllerTest,InfraFileUploadHttpContractFilterTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，Tests run: 17，Failures: 0，Errors: 0，Skipped: 0。
- 结论：T11 完成，AC-14 的扩展名/内容特征双重拒绝、创建前零副作用、正常文件兼容和 HTTP 400 合同通过；进入 T12 正式 sourceFileId 所有权隔离与历史迁移。

## 2026-08-11 T12 正式 sourceFileId 所有权隔离与历史迁移

- 状态推进：T12 `in_progress`，先补 AC-18/AC-01 的正式源认领、raw fileId 隔离复制、并发唯一性和历史迁移证据 RED。
- BDD: 正式提交独占源文件 -> Given 两次正式提交使用同名或相同内容，或异常指向同一 ticket storageFileId；When 创建正式记录；Then 每条记录最终 sourceFileId 唯一，租户内 source ownership 唯一约束阻止并发共享。
- BDD: raw fileId 路径先复制 -> Given 撤回重提、NAS 或表单中心以已有 fileId 提交；When 创建新正式记录；Then 系统先创建并校验独立源副本，sourceFileId 指向副本，originalFileId 保留版本历史来源。
- BDD: 历史共享源可重试迁移 -> Given 当前租户多条正式记录共享 sourceFileId；When 文控管理员执行迁移；Then 保留首条 owner，其余逐条复制、复核 SHA-256、更新 sourceFileId 并写迁移状态；中断后按持久化状态继续且不重复复制。
- BDD: 任一源副本损坏不传播 -> Given 两条记录已隔离且内容哈希相同；When 删除或篡改其中一个底层对象；Then 另一条记录仍指向独立对象且可读。
- RED: `mvn "-pl" "yudao-module-dcc" "-Dtest=DccControlledFileSourceOwnershipServiceTest,DccControlledFileWorkflowServiceImplTest,DccSourceOwnershipSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，测试编译明确缺少正式源所有权 DO/Mapper/Service、租户内认领查询、业务错误码和迁移 schema。
- RED: `mvn "-pl" "yudao-module-dcc" "-Dtest=DccControlledFileSourceMigrationServiceTest,DccControlledFileSourceMigrationCommitServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，测试编译明确缺少历史迁移编排、事务提交服务、租户限定未归属扫描/条件换绑 SQL 和迁移并发冲突错误码。
- 实现：新增租户内 `(controlled_file_id)` 与 `(source_file_id)` 双唯一所有权表；正式提交在受控记录插入事务内认领源文件。raw fileId 路径、已被正式记录引用或已认领的 ticket 源先复制到 `dcc/source-owned`，并从存储重新读取副本复核 SHA-256；`originalFileId` 保留来源历史，`sourceFileId` 仅指向该正式记录独占的可变底层记录。
- 实现：历史迁移按当前租户扫描所有未归属记录（包含逻辑删除历史），首条记录认领原源，其余记录创建独立副本；`PENDING/COPY_VERIFIED/FAILED/COMPLETED` 证据表持久化已校验副本，数据库换绑失败后重试复用同一副本，不重复复制。
- 实现：source 条件换绑、所有权认领和迁移完成标记在同一数据库事务内；源记录发生并发漂移时返回 1080000206，绝不覆盖新值。文控角色且具有更新权限的管理员可通过 tenant-scoped readiness/run API 查看缺口并以 1..200 的有界批次执行。
- GREEN: `mvn "-pl" "yudao-module-dcc" "-Dtest=DccControlledFileSourceOwnershipServiceTest,DccControlledFileWorkflowServiceImplTest,DccSourceOwnershipSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，Tests run: 113，Failures: 0，Errors: 0，Skipped: 0。
- GREEN: `mvn "-pl" "yudao-module-dcc" "-Dtest=DccControlledFileMapperTest,DccControlledFileSourceMigrationServiceTest,DccControlledFileSourceMigrationCommitServiceTest,DccControlledFileSourceOwnershipServiceTest,DccControlledFileWorkflowServiceImplTest,DccSourceOwnershipSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，Tests run: 127，Failures: 0，Errors: 0，Skipped: 0；H2 真实 Mapper 证明逻辑删除历史仍被扫描、已归属记录被排除、共享计数正确且条件换绑只更新期望旧 source。
- 结论：T12 完成，AC-18/AC-01 的新提交独占、raw 路径复制校验、历史共享源可重入迁移和并发漂移保护通过；进入 T13 前端就绪性、预览与错误体验接入。

## 2026-08-11 T13 前端就绪性、预览和错误体验接入

- 状态推进：T13 `in_progress`；按 `frontend-feature-delivery` 技能建立 `frontend-feature-evidence.md`，覆盖 API、loading/blocked/error 状态和真实路径门禁。
- BDD: 上传路线全部缺项可见 -> Given 类别路线或手工会签用户存在岗位、权限、签名授权、签名图或组织映射缺项；When 上传页预检或正式提交；Then 页面一次展示全部 blocker，提交前重复请求权威 readiness 且不就绪时不创建流程。
- BDD: 最终批准条件进入弹窗即聚合 -> Given 当前任务为最终文控批准；When 打开批准弹窗；Then 立即用当前盖章 PDF、目录和分发范围请求 task readiness 并展示全部缺项；提交前再次请求，未就绪时不调用 approve-task。
- BDD: 普通页面只消费能力投影 -> Given 后端源文件记录缺失；When 查询详情、浏览或版本历史；Then 普通响应无底层 fileId，页面由 `canPreview/previewUnavailableReason` 和 artifact availability 驱动，不从 ID 猜测。
- BDD: 岗位/运行态错误明确 -> Given 审批接口返回 1080000199 或 1080000201；When 页面处理失败；Then 直接显示“审批人未配置系统岗位”或路线运行态不一致信息。
- RED: `node tests/e2e/dcc-readiness-capability-contract-static.spec.js` -> FAIL，普通响应 VO 仍暴露 source/original/published/stamped fileId，上传和审批弹窗尚未接入聚合 readiness。
- 实现：普通详情和版本历史响应移除底层 fileId，改为 `publishedArtifactAvailable/stampedArtifactAvailable`；详情、浏览和版本展示仅消费业务能力，底层文件缺失时直接显示后端 `previewUnavailableReason`。
- 实现：上传页在既有提交前校验区调用聚合路线就绪性，一次列出岗位、文控权限、电子签名授权、签名图片和组织映射缺项；提交前再次强校验，不恢复旧的路线预览黄框。
- 实现：审批弹窗进入最终批准时立即请求任务动作就绪性，并在盖章 PDF、目录和分发范围变化时刷新；提交前再次请求，任何 blocker 均阻止 approve-task。1080000199/1080000201 映射为明确岗位/运行态提示。
- GREEN: `node tests/e2e/dcc-readiness-capability-contract-static.spec.js` -> PASS。
- RED: `pnpm ts:check` -> FAIL，详情页使用的 `previewUnavailableReason` 尚未纳入 `ControlledFileVO` TypeScript 合同。
- GREEN: `pnpm ts:check` -> PASS，补齐能力字段后 Vue/TypeScript 全量宽松合同检查无错误。
- GREEN: `node` 顺序运行 `dcc-readiness-capability-contract-static`、`dcc-controlled-file-detail-sfc-parse-static`、`dcc-preview-unavailable-reason-static`、`dcc-detail-approval-render-safety-static`、`dcc-upload-layout-static`、`dcc-upload-explicit-tenant-http-contract-static`、`dcc-upload-project-taxonomy-revision-static`、`dcc-controlled-content-matrix-real-flow-contract-static` -> PASS，8/8。
- GREEN: `mvn -pl yudao-module-dcc "-Dtest=DccApprovalTaskAdapterTest,DccControlledFileQueryServiceTest,DccControlledFileWorkflowServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，Tests run: 205，Failures: 0，Errors: 0，Skipped: 0。
- 技能门禁：`frontend-feature-delivery` 要求的功能证据、loading/ready/blocked/request-error 状态和真实路径验证计划已记录；T13 完成，进入 T14。

## 2026-08-11 T14 系统回归、真实 E2E 与集成验收

- BDD: 合法类别自动绑定真实分类树 -> Given 上传场景指定的 DCC 标准类别存在正式 `fileTypeTaxonomyId`；When Playwright 选择文件类型；Then 页面自动绑定该类别并继续真实上传路径，不再通过文本点击猜测类别。
- RED: `node tests/e2e/dcc-controlled-content-matrix-real-flow-contract-static.spec.js` -> FAIL，脚本仍在分类树选择后手工点击“文件类别”，与当前自动绑定合同冲突。
- GREEN: `node tests/e2e/dcc-controlled-content-matrix-real-flow-contract-static.spec.js` -> PASS，脚本等待 `dcc-upload-category-leaf-display` 显示精确类别，不再执行旧手工点击。
- RED: 同一静态合同 -> FAIL，脚本任意选择首个文件类型分类树叶子，不能证明其与目标 DCC 类别正式绑定。
- GREEN: 同一静态合同 -> PASS，脚本先从 `/dcc/file-categories` 读取目标类别的 `fileTypeTaxonomyId`，再选择对应叶子并记录绑定证据。
- RED: 同一静态合同 -> FAIL，预检会把缺少分类树绑定但有路线的标准类别误标为可执行，直到页面选择阶段才失败。
- GREEN: 同一静态合同 -> PASS，预检在任何写请求前以 `DCC category has no formal file type taxonomy binding` 明确阻断，且不猜测其它类别或路线。
- 数据库迁移：三份正式迁移已在本机 `ruoyi-vue-pro` 执行；运行态复核 `duplicate_groups=0`，三个目标表/约束和受控副本绑定列均存在。迁移政策门禁 PASS，`migrationCount=462`，证据保存在 `migration-policy-gate.json`。
- GREEN: DCC 模块除五个已在 `int_main` 精确复现的既有失败类外全量运行 -> PASS，Surefire 聚合 `reports=163, tests=1165, failures=0, errors=0, skipped=0`。
- 基线对照：`DccBaseSchemaTest`、`DccFormCenterPolicyMigrationTest`、`DccDirectoryAdminServiceImplTest`、`DccViewMatrixIndependentContractTest`、`SignatureGovernanceMenuContractTest` 的失败均在未含本任务改动的 `int_main` 精确复现，分别属于既有非破坏性 SQL 断言和旧前端路径合同，不归因于本任务。
- GREEN: infra 定向 `FileUploadSecurityPolicyTest,FileControllerTest,InfraFileUploadHttpContractFilterTest` -> PASS，Tests run: 17，Failures: 0，Errors: 0，Skipped: 0。infra 全量的 3 failures + 1 error 同样在 `int_main` 精确复现，属于既有 runtime ops 测试。
- GREEN: `pnpm ts:check` -> PASS；8 项 DCC 前端静态合同全部 PASS。前端 DCC 全量中唯一 `dcc-access-rule-bound-directory-list-static.spec.js` 失败已在 `int_main` 复现，属于既有合同。
- GREEN: 全后端 Maven package -> PASS，30 个 reactor module 全部 SUCCESS；工作树专属端口 48094/8094 启动后 backend health=UP、frontend HTTP=200，验证后已停止且端口释放。
- 真实 Playwright：完整矩阵预检通过真实前后端和测试租户执行；WORK_INSTRUCTION、INSPECTION_PROCEDURE、DRAWING 被新的聚合 readiness 正确识别为提交人部门负责人/岗位配置缺失，SOP 路线本身可就绪。
- 真实 Playwright 阻塞：本地正式数据中，带有效审批路线的标准类别均没有 `file_type_taxonomy_id`；带分类树绑定的类别均没有审批路线。SOP 目标类别 906101 因缺正式分类树绑定在预检阶段阻断，最终状态 `BLOCKED`，`writeRequests=[]`，未创建文件、ticket、正式记录或流程。
- 安全边界：未为通过测试而直接修改组织、类别、路线或租户基线，未用 API-only、默认类别或 mock success 替代真实用户路径；敏感登录参数未写入任务证据。
- GREEN: `git diff --check` -> PASS，仅输出仓库行尾转换提示，无空白错误。
- GREEN: `validate_backend_api.py --evidence backend-api-evidence.md`、`validate_database_schema.py --evidence database-schema-evidence.md`、`validate_frontend_feature.py --evidence frontend-feature-evidence.md` -> PASS；技能证据的关键 RED/GREEN、迁移和前端状态结论已归档到本日志及 `verification-report.md`，临时 evidence 文件可由 closeout 清理。
- 经验沉淀：按 `project-experience-consolidation` 将“DCC 完整发布 E2E 必须联合校验正式 taxonomy 绑定和同一类别有效路线，阻断时证明零写请求且不得写成发布成功”合并到既有 `docs/e2e-rules.md`，并更新 `docs/experience-index.md`；未新建长期经验文档。
- 融合预检：任务路径与 `5699b8045..int_main` 的 53 个主线已提交路径交集为 0；与主工作区 575 个 dirty 路径仅两个长期经验文档相交。实现提交排除这两个同文件并发路径，待主线代码融合后以独立 hunk 落入主工作区，禁止覆盖或提交其它 dirty 内容。
- 结论：T14 完成。AC-01..AC-18 均有生产实现和自动化 GREEN；真实运行还额外证明新的组织/分类前置门禁会在零写入条件下失败。完整发布成功路径仍依赖测试租户补齐一条同时具备正式分类树绑定和有效审批路线的标准类别，作为环境数据阻塞保留，不静默降级。
