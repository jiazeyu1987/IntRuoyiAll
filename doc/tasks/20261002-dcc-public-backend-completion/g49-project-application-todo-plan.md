# G49 / LD04 — 项目及产品申请接入统一待办与站内信

状态：`prepared_for_root_design_review`。只读现有源码；本批仅形成方案，未改源/测试、运行测试、写数据库或调用 API、启停服务、执行浏览器或 Git 写操作。LD03 正式代码尚未开始；本方案用于后续顺序开发，不冒称 LD04 已实现。

## 最小业务结果

进入 DCC 产品目录 → 新建项目代码及产品 → 按后台配置带出审核人并提交 → **本次申请冻结的审核人收到统一审批中心待办和站内信** → 从待办打开原申请 → 审核通过 → **当前合法批准人收到批准待办和站内信** → 从原申请批准。

审核或批准驳回 → **原申请人收到含驳回原因、可打开该原申请的站内信** → 查看冻结的原申请资料 → 修改后重提 → 生成后继申请，重新冻结当次审核人并进入审核；旧申请及历史决定不覆盖。不会增加另一套项目审批工作台、批准人配置、文件流程节点或签名图片生成。

## 当前准确接线

BE 路径相对 `IntRuoyiBackend/`，FE 相对 `IntRuoyiFronted/src/`。

| 当前入口 | 当前事实与有限接入点 |
| --- | --- |
| DCC `service/projectcode/productcreate/DccProjectProductCreateServiceImpl.createPendingRequest` | 已冻结 reviewer ID/username/nickname、实际申请人、提交时间、项目属性及模板快照，状态 PENDING_REVIEW；**没有 BPM 实例** |
| 同服务 `review` | 锁 request；`assertFrozenReviewer` 校验该次 reviewer 和当前启用租户账号；通过→PENDING_APPROVAL，拒绝→REJECTED，真实原因/决定/Gxp 同事务 |
| 同服务 `approve / requireAdmin` | 当前批准规则是合法系统账号 username 为 admin，并受现有 Controller `dcc:project-code:update`；本批不擅自增加后台批准人配置或换审批角色 |
| `DccProjectProductCreateStateService.markApprovalDecision` | 独立状态事务，批准通过→WRITING，拒绝→REJECTED；之后 WriteService 成功→COMPLETED，失败有 WRITE_FAILED 与正式重试 |
| `DccProjectProductCreateServiceImpl.resubmitRejectedRequest` | 原申请人且 REJECTED、无已生成后继才能重提；生成新 requestId，保存 previousRequestId，不改原拒绝资料 |
| `DccProjectProductCreateRequestMapper.selectPendingList` | 当前 records 包含 pending/rejected/write-failed 等，排除 COMPLETED；不能把该列表直接当准确当前用户待办或当全部历史详情 |
| `DccApprovalTaskAdapter` | 现只适配文件 Flowable 任务，sourceTaskType=DCC_CONTROLLED_FILE_TASK；不能把项目 requestId 塞进 fileId/Flowable taskId |
| BPM `approval/service/provider/ApprovalTaskProviderRegistry` | 一个 ApprovalModuleCode 仅允许一个 provider；第二个 DCC provider 会启动失败 |
| FE `views/approval-center/index.vue` | 已支持 PROCESS_IN_MODULE 按模块 detailRoute/detailQuery 打开正式页面；仅 DCC 文件详情前缀有其文件专用 handling 参数 |
| FE `views/dcc/controlled-file/basic-data/components/ProductCatalogTabPanel.vue` | 已有 records 弹框、实际 reviewer 动作、原申请修改后重提；没有按 requestId 打开精确申请的路由消费 |
| SYSTEM `api/notify/NotifyMessageSendApi.sendSingleMessageIdempotentlyToAdmin` | 正式接口按租户 businessKey、收件人、模板、全部参数校验精确重放，创建 system_notify_message 并返回真实 ID |
| FE `utils/notifyMessageNavigation.ts` | 仅支持文件发布/BPM/eDHR/showroom 等明确目标；尚不识别项目产品申请链接 |

## 统一待办：复用现 DCC provider 并组合独立 delegate

新增项目申请查询/投影 delegate，由**现有唯一 DccApprovalTaskAdapter 调用**；delegate 不是第二个 `ApprovalTaskProvider @Component`，不改全平台 Registry。

- 模块继续 DCC，独立 sourceTaskType 建议 `DCC_PROJECT_PRODUCT_REVIEW` / `DCC_PROJECT_PRODUCT_APPROVAL`。stable summary.id/sourceTaskId 使用独立 `DCC_PROJECT_PRODUCT:<tenant>:<requestId>:<stage>` 命名空间，businessKey 保留真实 requestId 正整数 Long 原字符串。该 composite 是 native 请求阶段身份，不是伪 Flowable task ID；source-type 路由分支必须先核对完整绑定。
- TODO：PENDING_REVIEW 且 frozen reviewer 为当前合法账号；PENDING_APPROVAL 且当前账号满足现批准资格和操作权限。WRITING、WRITE_FAILED、REJECTED、COMPLETED 不伪装成新审批待办；既有失败写入处理保持其原模块入口。全局 approval_admin 查看可沿框架已有 globalView，但不能授予 review/approve 权限。
- 批准接收人由服务端按**当前租户准确 username=admin** 与现有批准资格查找，并用正式账号 API核对 enabled/tenant/update 权限；不写死 userId=1、不借别的租户 admin、不用 nickname 模糊搜索或第一条用户。账号失效/缺失准确报错。冻结 reviewer 的来源仍仅提交快照，不能读最新 reviewer 配置改变老任务收件人。
- DONE：按真实 reviewedTime/reviewerUserId 和 approvedTime/approverUserId 分别生成已处理阶段行；审核已完成但批准仍 pending 时，审核人 DONE 和批准人 TODO 可以同时存在。由真实记录字段给出结果/原因；没有日期/actor 的历史行不伪造完成事实。申请人/全局查看沿已有权限处理，不将所有记录对任意登录者暴露。
- currentNodeCode/name、businessStatus、businessTitle/code、initiatedAt、taskCreatedAt、taskCompletedAt 均取实际 request stage/state/time；批准待办创建时间使用实际 reviewedTime。processInstanceId 保持无，不拼假 BPM ID。文件任务保持原字段、过滤、阶段和签名能力。
- 两类来源**在同一 DCC provider 内合并后**按框架相同时间顺序、稳定第二排序键排序，并应用一个 page window；total 是各准确过滤来源计数之和。不能先各自切当前页再拼、拿文件 total 当总数或让第一页空行但 total 非零。若采用源分页窗口收集，必须像现文件 source loop 那样收集到足够全局窗口并正确累积源 count；keyword/tenant/actor/DONE 必须在 count 和 rows 使用同一合同。
- 项目行 availableActions 仅 `PROCESS_IN_MODULE`，不接统一中心 quick APPROVE/REJECT。现平台 quick-review 总是要求签名密码/图片，原项目申请接口仅真实意见；HTML 尚未新增项目签名要求，本批不得借待办接线改变其业务批准合同。项目行 requiresSignature=false，不展示其没有的文件签名证据；row capabilities 只声明实际实现内容。
- timeline 若复用现 DCC TIMELINE 能力，delegate 从真实申请/审核/批准/重提链投影，并校验申请人、冻结 reviewer、合法 approver/已有全局视图。sourceTaskType 分支必须在文件 BPM timeline校验**之前**，避免无 processInstanceId 的 native申请被旧文件 guard 拒绝；不能声明能力后返回空 timeline 或 fabricated Flowable history。

## 站内信：同步事务内写正式消息，最小无新 job 表

当前站内信“发送”实际是正式数据库 INSERT：`NotifyMessageSendApiImpl` 调用 `NotifySendServiceImpl.sendSingleNotifyToAdminIdempotently`（REQUIRED），最终 `NotifyMessageServiceImpl.createNotifyMessage` 写当前租户消息表；没有该接口内的远程发送。优先同一 DataSource/physical transaction 复用它，不扩文件专属 message-job 表，也不把 requestId 充受控 fileId。

| 原业务事务 | 通知接收人及事件 |
| --- | --- |
| createRequest / resubmit 的 PENDING_REVIEW 创建事务 | 此新 request 已冻结的 reviewer；REVIEW_TODO |
| review通过→PENDING_APPROVAL 的事务 | 当前准确合法批准账号；APPROVAL_TODO |
| review拒绝→REJECTED 的事务 | 原 applicantUserId；REVIEW_REJECTED |
| markApprovalDecision拒绝→REJECTED 的事务 | 原 applicantUserId；APPROVAL_REJECTED |

业务 state/update、identity claim 与 Gxp 先通过其真实校验，在同一事务写正式站内信，并要求返回真实正整数 message ID；事务提交前其他用户不能读到成功消息。模板缺失/停用、收件人失效、参数冲突、消息 ID 缺失或 INSERT 错误均使该次原业务事务回滚；不能 catch 后当成功。无需另写 SENT 标识或先发后标，也不通过 afterCommit 裸回调发送。批准通过后 WRITING→COMPLETED/WRITE_FAILED 的现有分事务语义保持，不把 APPROVAL_TODO 或批准决定当项目已成功物化。

正式 businessKey 示例：`DCC_PROJECT_PRODUCT:<tenant>:<requestId>:<event>:<recipientId>`，<=255、无动态当前时间。模板参数从已冻结/锁定的 request/stage/actor/真实原因构造并固定；同键同参数回读同一消息、同键任何载荷变化拒绝。不同后继申请使用真实新 requestId，不重用原 rejected 通知键；此接线不宣称原 create HTTP 已有通用幂等键。

建议一个启用的正式模板 `dcc-project-product-application-event`，参数明确 project/product code/title、真实 requestId、阶段、结果/原因和受限 actionUrl。模板 SQL、权限/索引实际环境及同物理事务组合验证由后端 owner准备、Root review/授权执行；不能在运行时静默补模板或将模板关闭当发送成功。平台已有消息 businessKey 唯一/冲突合同复用，不新增平行通知平台。

若实现时确认实际消息 provider已改为远程投递/不在同 DataSource，则这条同步方案必须明确停下来重审有限 outbox，不能仍声称同事务。当前读取源码支持上述数据库消息方案。

## 原申请链接和修正入口

- 正式页面 component 是 `dcc/controlled-file/basic-data/product-catalog/index`，现 menu component_name=`DccProductCatalogBasicDataPage`、child path=`product-catalog`。最终完整 route须核当前父菜单，不依据截图猜路径。直接复用这个已有页面，不新增平行工作台。
- 新增准确 query 合同，例如 requestId 原 Long 字符串与 `requestOpen=records`，source 从 `approval-center` 或 `notification`。前端读它后只加载被授权的真实 request、打开既有 records/详情并定位该行；route marker不能作为权限证明，不能传 fileId、旧文件 processInstanceId/taskId/handling。
- 需要有限只读 `GET /dcc/project-product-requests/{id}`，或在已有 service 中等效精确读方法：核 tenant、存在/deleted 及真实 requester/frozen reviewer/current合法批准人/既有允许查看的管理权限。该详情包含 COMPLETED 与 REJECTED，不能依赖现 pending 列表寻找已经完成的原申请。新增读入口只为正式身份查询，不允许 arbitrary registry/session/bootstrap。
- 通知导航 helper 新增 **DCC_PROJECT_PRODUCT_REQUEST** 专用目标：仅当前 origin、已有产品目录正式路由、白名单 query、合法 Long requestId；params 使用实际 frozen requestId。不开放任意 detailUrl、router injection 或 generic外链。消息详情/统一未读列表点击沿现 helper；需要 label“查看项目及产品申请”，并用 text渲染消息/原因。
- 审核/批准继续既有 formal service/Controller 和意见对话框；待办 row不绕过当前权限。被冻结 reviewer 或合法批准账号缺统一中心 `bpm:task:query`、项目 query/update 页面权限时，应显示真实配置问题，由已有管理员正式维护；不能本批自动授角色或账号。
- rejected 原申请只读展示旧属性、模板快照、actor/time/reason，原申请人可使用已有 `editRejectedProjectProduct` / `resubmitRejectedRequest`。后继生成后只显示历史链接，不给原申请再提供重复重提。失败原记录和签名/Gxp历史不覆盖。

## 实现 owner 的有限文件边界

| Owner | 必需范围 |
| --- | --- |
| BE | projectcreate Service/State 的四个事务事件接线；独立 recipient/query/notification helper；RequestMapper 精确 actor/status/count/DONE读取及 byId授权；现 DccApprovalTaskAdapter 内 source-type delegate组合、timeline 和聚合窗口；正式通知模板前向 seed；相关 controller/VO（Long string） |
| FE | ProductCatalogTabPanel 及其既有页面 wrapper消费准确 request query/只读详情；project requests API/type；notifyMessageNavigation 专用目标及现消息详情/列表 label；现审批中心如只需原 PROCESS_IN_MODULE 入口不改，无需新平台 UI |
| Root | Review 跨模块事务/分页与身份；实际模板/schema/权限配置前置和范围授权；同源 types/build/package；真实账号页面审核/批准/驳回/重提链与只读证据 |

先实现 positive REVIEW_TODO → APPROVAL_TODO → 原页面批准，再接两种拒绝通知和原申请重提。LD01 product identity/原申请物化/LD02文件上传/LD03目录映射不在此批改写。必要的 schema 仅明确模板seed或实现确需的实际字段，不因 native待办额外引入 BPM实例或 todo表。

## 有限 BDD / RED→GREEN 验证计划

| Given / When | Then |
| --- | --- |
| 当前配置 reviewer R 提交申请，之后后台改为 S | R 的准确当前租户 TODO 和消息指向原 request；S 不接老任务；缺模板失败时业务/Gxp/消息整体回滚 |
| R 正式审核通过 | REVIEW TODO消失、R DONE出现、当前合法 approver收到 APPROVAL TODO/消息；actor/status/time/name是实际资料，旧文件待办 count/page 不变 |
| reviewer/approver拒绝申请 | applicant收到唯一实际原因通知；点开准确原申请；非申请人不能修正；新申请保 previousRequestId、重新冻结 reviewer |
| 同事件重放、不同事件或后继 | 同键精确消息只有一条；载荷冲突明确拒绝；后继有不同真实 requestId 与消息，旧记录保留 |
| 本页含文件任务和 native申请、用户/keyword/DONE不同 | 合并排序/count/window准确，不 duplicateDCC provider、不空第一页假count、不向无权账号泄漏 |
| 通知 requestId > JS安全整数、foreign origin/query额外身份 | 正常 Long 字符串精确定位；非白名单/非法身份拒绝，不能接文件或Flowable路由 |
| 真实页面提交→审核→批准，以及拒绝→通知→修改重提 | 所有业务动作走Playwright真实页面与实际账号；Root DB/API只读核消息/状态/历史，不代替动作 |

生产实现必须先记录旧源码有效 RED，再有限 GREEN/相关回归；本方案未执行上述验证。不把旧离线报告、源码存在或实际只有登录当本链 E2E PASS。

## 本次读取指纹

2026-10-04 当前 raw SHA-256：

| 文件 | SHA-256 |
| --- | --- |
| DccProjectProductCreateServiceImpl.java | ec8a94441ff84a7d835e86f9bf0f6a47ec83becf4c3149de9b1ffa30275b3a95 |
| DccProjectProductCreateStateService.java | 64cf1f870738b852d16ddf97409d793a12e8d0c431afd06d7e0a4145dffa47c9 |
| DccProjectProductCreateRequestMapper.java | 3bd27d8dabe602a9f914061426f7dec00724d90ec2a151fcbc80c5ccf184d1b2 |
| DccApprovalTaskAdapter.java | 094ecbd59352c3e73917613d7c11e1a44541483a95ad9f39ae118b9891505360 |
| ApprovalTaskProviderRegistry.java | aa59b5e18bcd479063546c04b388ad7a3c1313ce97bcbcf8c92ae8c8b6599c55 |
| NotifySendServiceImpl.java | 9e0ed125ecdc368c8d1e8644eef5dbecb3e01badbb01b2208b28a01304bc64cb |
| ProductCatalogTabPanel.vue | 9362e17c23304293b7c56c165101d6b4a1e4e22fe259a7d350a783667c6c8ec8 |
| notifyMessageNavigation.ts | 3d600fa73125dbb4253b624d4e5a7359c089d0a169159ad5ed728c09abfc53f0 |

这些指纹标识本次方案依据；不覆盖 Owner未来交付或 Root实际数据回执，也不证明平台/runtime配置已可用。
