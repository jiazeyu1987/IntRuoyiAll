# DCC 最小实现技术设计

## I00 设计边界与共用模型

配套：[业务范围](README.md)、[双文件上传设计](dual-artifact-upload.md)、[验收计划](acceptance.md)。以下是拟新增/修改的合同，不表示当前接口、字段或表已经存在。只使用现有Java/Spring、MySQL、Vue、BPM、PDF处理及站内消息能力。

### I00.1 三种身份

| 身份 | 表达 | 用途 |
|---|---|---|
| 逻辑文件 | `masterId` | 长期文件身份、名称占用、负责人、当前有效指针 |
| 业务版本 | `masterId + versionNo` | 例如A/1、A/2、B/1，用户理解的升版 |
| 内容快照 | `controlledFileId` | 一份不可变的不可编辑浏览版PDF及元数据、审批轮次、签名、批注、引用的实际对象；可编辑源文件作为可选附属件 |

继续用 `dcc_controlled_file` 每行存一个内容快照。增加 `approvalRoundNo`、`contentRevisionNo`、`changeKind`，复用 `predecessorControlledFileId`。重提新增行，不原地替换PDF或抹掉旧流程；`processInstanceId`、签名的 `revisionId/controlledFileId` 继续指向该行。

| 操作示例 | 业务版本 | 审批轮次 | 内容修订序号 | 审批结果来源 |
|---|---|---|---|---|
| 首次送审 | A/1 | 1 | 1 | 本行本轮签名 |
| 驳回后换PDF再审 | A/1 | 2 | 2 | 本行新轮签名 |
| A/1受控后小版本检入 | A/2 | 空 | 1 | 无内容审批，记录免审批检入 |
| A/2例外替换 | A/2 | 空 | 2 | 无内容审批，记录文控执行签名 |
| 升大版本送审 | B/1 | 1 | 1 | 本行本轮签名 |

`approvalRoundNo`仅在实际送审时赋值，按同Master同业务版本已用最大轮次＋1；`contentRevisionNo`按同业务版本已存内容最大序号＋1。没有审批不能补成第1轮，不能复制前轮签名或批准时间。`changeKind`只需NEW、RESUBMIT、MINOR_CHECKIN、MAJOR_CHECKIN、EXCEPTION_REPLACE。

同Master只能有一个在审候选和一个当前有效内容；受控小版本、例外替换只允许以当前有效内容为基础，且不得同时存在检出或在审变更。大版本审批时保留旧有效内容。为简化主流程，不增加并行修订分支。

### I00.2 数据增量清单

表名为建议命名；实施前对随库迁移和实际目标schema核实后生成正式SQL，本文不提供可执行迁移。

| 对象 | 最小变化 | 必须保持的约束 |
|---|---|---|
| 项目 | 规范化项目编码唯一；记录启用使用的模板快照ID | 原项目名称不参与编码唯一键；现有正式产品、注册证关联复用 |
| 新 `dcc_project_directory_template` | projectId、templateVersion、nodesJson、active、创建人/时间 | 保存即生成新有效快照，旧快照不可改；初始树由服务端明确创建 |
| 现有目录 | projectId、templateSnapshotId、templateNodeKey、允许类型列表 | 项目目录只属于一个项目；父子归属一致；目录权限复用 |
| 新 `dcc_file_name_claim` | tenantId、normalizedName、masterId | tenantId＋normalizedName唯一；批准作废删除占用行，历史文件不删除 |
| 文件Master | responsibleUserId、versionPolicySnapshot、recreatedFromMasterId、初始化业务规则标记 | 负责人必须有效账号；当前有效指针继续唯一；旧Master作废后仍可查 |
| 文件快照 | approvalRoundNo、contentRevisionNo、changeKind、readOnlyFileId、可空sourceFileId、修订原因/主要变更/涉及范围、资料说明、依据附件列表 | masterId＋versionNo＋contentRevisionNo唯一；同版本非空轮次唯一；新流程readOnlyFileId必填，sourceFileId不得被当作预览兜底 |
| 路线快照 | 复用节点/人员快照，增加requiredFunctionsJson | 保存每个必需职能及实际分配人员，不能为空；不只存合并名单 |
| 新 `dcc_file_review_note` | controlledFileId、taskId、authorId、pageNo、text、createdAt | 绑定精确内容；提交后不可修改/删除；不改变PDF |
| 审核记录 | reviewChecklistJson及意见 | 文控批准时六项必须确认；驳回不强制勾满 |
| 新 `dcc_file_replacement_record` | masterId、oldSnapshotId、newSnapshotId、reason、minorChangeDeclared、actorId、executedAt、signatureId | 独立申请与执行记录，不伪造processInstanceId或APPROVED |
| 现有文件关系 | relationKind、sourceMasterId或sourceDirectoryId、sourceProjectId、targetSnapshotId、目标产品/项目/版本/内容修订快照、responsibleUserId、active | ASSOCIATION归属源Master，REFERENCE归属源项目目录；同源、同类、同目标Master仅一条有效关系 |
| 现有审计/消息 | 新增关联动作、替换动作及通知事件类型 | 保留前后目标ID、操作者、时间、原因；消息任务失败可见 |

Master旧复合身份唯一约束需要改成“未作废逻辑文件唯一”，或等价的有效身份占用机制，避免作废后同名同编号新建仍被旧Master挡住。保留名称唯一与编号身份两个明确规则，不删除编号校验。索引方案以实际表结构为准。

不要新增通用事件总线、通用版本中心或抽象工作流引擎。新增业务职责可用小型服务实现，现有Workflow/Query只负责调用，不继续堆积所有逻辑。

### I00.3 权限与最小事务

| 动作 | 权限规则 |
|---|---|
| 模板维护、启用项目 | 项目正式OWNER＋现有项目维护权限；创建项目时显式配置OWNER |
| 上传、返工重提、小版本检入 | 项目OWNER/EDIT＋类别上传/编辑权限；重提仅原申请人，检入仅当前检出人 |
| 大版本 | 当前检出人＋项目正式OWNER＋现有升版/类别权限 |
| 审批、批注 | 本轮任务办理人＋阶段权限；签字再检查本人签名条件 |
| 作废 | 现有作废权限和审批规则 |
| 例外替换 | doc_control角色＋新增例外替换动作权限＋目标内容权限；本人签名 |
| 关联/引用维护 | 新增关系维护动作权限＋源项目OWNER/EDIT；引用可直接创建于本项目目录；目标名称可见才可检索，正文仍按原内容权限 |
| 更换文件负责人 | 源项目OWNER；使用有效账号显式选择，记录审计 |

新建、重提、版本切换、作废释放占用、关系修改均使用本地事务。沿用Master行锁及唯一索引即可；不增加分布式锁。先完成PDF读取/盖章及文件写入，再在事务中切换指针与状态；失败不替换旧内容、不释放检出锁或名称占用。临时孤立文件使用现有清理机制。

业务写请求继续使用既有幂等能力，新增接口接收requestKey并绑定实际载荷。同一点击重试不再新增轮次、内容或通知；无需另外建设通用幂等系统。消息发送在业务提交后调用现有消息任务，失败明确记录，不假报生效失败或通知成功。

## I01 项目、目录模板与上传位置

覆盖 M01—M04。

1. 产品建档、项目—产品—注册证关联复用现有正式服务。补项目编码规范化唯一校验及schema唯一约束；先只读列出现有重复编码，不能自动合并项目。
2. 项目创建后，首次打开目录模板明确初始化“项目文件”根节点草案。配置有效叶子类型并保存后才成为有效模板。模板不存在时进入这个建模动作，不在上传中临时补默认目录。
3. 一个模板保存一棵JSON树：nodeKey、parentKey、name、sort、allowedFileTypeIds；至少一个可上传叶子，叶子必须绑定有效类型。不绑定预定义文件名称。
4. 负责人保存后生成有效模板快照；启用命令接收templateSnapshotId，在一个事务内插入项目目录、绑定来源并启用项目。重复启用返回既有结果，不生成第二套目录。
5. 改模板不改已实例化目录。目录层级不再依赖旧“分类至少三级”的校验；目录是摆放位置，文件类型仍引用正式taxonomy/category，不生成虚假分类来凑层级。
6. 上传需同时提交projectId、projectDirectoryId、fileTypeId。服务端确认目录在该项目下、是叶子、允许该类型；最终归档使用送审快照的目录，不能被类别默认目录覆盖。
7. 第一版普通受控文件的新上传/换稿采用双文件合同：不可编辑浏览版PDF必填，可编辑源文件可选。浏览版必须是真实可读PDF；可编辑源文件只用于授权下载和留存，不参与日常在线浏览。旧Word、Excel、图纸历史及外来文件模块保持原只读/既有行为，本方案不顺带重构历史非PDF流程。

专项设计见 [DCC双文件上传开发设计](dual-artifact-upload.md)。后续开发按该文档新增 `READ_ONLY_VIEW` 与 `EDITABLE_SOURCE` 上传用途、`readOnlyUploadTicket` 与 `editableUploadTicket` 提交字段、`readOnlyFileId` 快照字段；新流程禁止把 `originalUploadTicket`、`sourceUploadTicket` 或 `DRAWING_PDF` 当成浏览版PDF的兜底。

拟新增模板/启用接口：

| 接口（均在 `/dcc` 下） | 输入与输出 |
|---|---|
| `GET /project-codes/{id}/directory-template` | 返回有效模板快照、树及canManage |
| `PUT /project-codes/{id}/directory-template` | nodes、requestKey；返回新snapshotId/version |
| `POST /project-codes/{id}/activate` | templateSnapshotId、requestKey；返回projectId/rootDirectoryId |

前端复用项目详情和目录树组件，旧“文件名清单模板”不再限制新流程名称；改上传名称为必填输入框。文件类型与目录分别展示，避免用户误把二者当成同一个字段。

## I02 名称、身份与受控信息保护

覆盖 M04、M05、M23、M29。

名称规范化只执行trim和英文大小写统一；不去内部空格、不删标点、不自动去掉扩展名。逻辑文件名称和实际上传附件文件名分开，不要求二者相同。

新建事务依次校验权限、创建Master、占用名称、创建快照和送审；任何失败回滚占用。名称预检只改善体验，最终以唯一键为准；冲突返回业务码 `DCC_FILE_NAME_EXISTS` 和业务范围中的准确文案，不向无权限用户暴露重复文件详情。

驳回、撤回、失效历史不改变Master名称占用；升大小版本继续使用同一Master。批准作废清除名称占用和有效逻辑身份占用；新建同名文件分配新Master，保存recreatedFromMasterId供历史跳转。不能把旧Master状态改回受控来实现同名重传。

已受控文件的目录、分类、类型、项目、名称和编号禁止通过通用metadata接口、导入、识别修正接口直接变更。姓名文本等不影响本方案的独立管理功能不扩展。负责人/关系维护走独立动作，均有审计。

返工仅允许未受控候选的新一轮改变目录、分类和类型，MasterID保持不变；最终有效时同步当前身份字段，旧轮快照保持旧分类。若是大版本候选，禁止通过返工修改原受控文件分类，明确提示先作废重传，避免绕过M29。

## I03 两阶段审批、资料、批注、签名与自动受控

覆盖 M06—M13。

### I03.1 路线

新增正式BPM定义键建议为 `dcc-controlled-file-main-v2`，流程只有 `DOC_CONTROL_REVIEW → PARALLEL_SIGNOFF → END`。第二节点使用并行多实例、100%完成，拒绝策略固定为终止流程，不允许退回前序任务。

每个文件类型配置一个文控办理人、一个或多个必需会签职能、每个职能一个或多个必需人员。复用现有岗位及人员配置；主流程中每个必需职能逐项解析并校验，任一为空直接阻止送审。多职能同一人可只办理一次，但快照必须明确其覆盖的职能；一份批准只能覆盖快照中分配给本人且已核验的职能。

上传人选的额外会签人员与必需名单取并集去重。不得删除配置的必需人员；额外人员批准也成为生效条件。普通新增签名人无需虚构必需职能。送审前核验账号启用、审批权限、岗位、本人签名授权和签名图片。

发起时冻结完整路由及职能快照。已开始的旧流程继续绑定其原定义和原快照；不部署覆盖旧定义，不改旧任务及已完成签名。新路线只用于新上传和新轮次。配置页面只提供本模型真实支持的字段，不再展示ANY、比例或任意环节排序。

### I03.2 文控检查和PDF批注

固定六项清单：上传路径、分类/类型、资料完整性、原始数据/相关依据、必需职能和人员、送审不可编辑浏览版PDF及业务版本。文控批准必须全部确认，意见必填；驳回仅要求原因和签名。依据附件作为只读附件引用写入快照，不另建证据管理系统。

PDF批注只需页码、文字和作者时间。办理人可在自己的本轮待办有效期间追加批注；历史只读，不覆盖正文，不开放PDF编辑或替换。复用现有受保护预览，保存前校验页码在PDF页数范围内。

### I03.3 签名与生效

签名复用统一内核及DCC投影；明确绑定controlledFileId、masterId、versionNo、approvalRoundNo、contentRevisionNo、readOnlyFileId、readOnlySha256、taskId、stageCode、actor、time、opinion、result。有效批准仅从本快照本轮读取。

最后一名会签人批准后，服务端核验文控和全体会签人的有效签名以及每个必需职能覆盖，读取本快照锁定的不可编辑浏览版PDF生成盖章副本。关闭普通新流程的 `APPROVAL_PDF` 上传用途及前端入口，不允许文控另外上传一份“盖章后PDF”冒充批准内容。

复用 `DccPdfStampService`；受控副本记录不可编辑浏览版PDF哈希、盖章后哈希、所属快照和存储目录。文件路径由项目目录决定。浏览版PDF保留，受控章只是派生副本；可编辑源文件单独保留，不作为盖章输入。事务切换成功才显示“受控”；失败复用现有失败状态/重试，不另设人工发布节点。

全员判定改为消费本流程保存的节点快照，不能继续硬编码“必须四个节点”。旧四阶段实例根据原定义验证原签名要求，新两阶段根据新定义验证；这是明确的流程版本共存，不是缺配置时的fallback。

拟调整/新增接口：

| 接口 | 最小合同 |
|---|---|
| 现有提交及路线预览 | 增加项目目录、资料说明、依据附件、负责人、extraSignoffUserIds、readOnlyUploadTicket和可选editableUploadTicket；服务端计算required名单 |
| 现有approve-task | taskId、password、reason、文控时reviewChecklist；不再接收替换PDF |
| 现有reject-task/withdraw | 保留必填原因，终止本轮全部剩余任务，旧证据不删 |
| `GET/POST /controlled-files/{snapshotId}/review-notes` | 查询本快照批注；新增pageNo/text/taskId/requestKey |

## I04 同版本多轮送审与历史保护

覆盖 M12—M16。

复用文件行作为审批对象，新增 `POST /controlled-files/{snapshotId}/resubmit` 请求体：readOnlyUploadTicket（换正文时必填，沿用旧浏览版时由服务端复制封存）、editableUploadTicket（可选）、directoryId、categoryId/fileTypeId、资料说明、依据附件、extraSignoffUserIds、changeReason、requestKey。

主流程：验证原申请人和当前权限→确认原轮为REJECTED/WITHDRAWN→校验至少一项真实变化→锁Master→分配同业务版本的新轮次和内容修订序号→保存新的不可编辑浏览版PDF及元数据快照→按需保存可编辑源文件→保存新路线→启动新流程。没有新浏览版PDF但只改了信息时，使用现有独占文件服务复制/封存原浏览版PDF字节，不能把旧行改成新分类。

原轮永远保留自己的状态、PDF、BPM实例、批注、签名。旧轮通过predecessor/successor关系跳转新轮。重提不再借用nextMinor或nextMajor，不改变业务版本号。

用户撤回时结束本轮任务，记录原因；允许修改后重提。拒绝撤回ACTIVE、SUPERSEDED、OBSOLETE内容。旧的“撤回后直接复制原件一键重提”和“删除撤回流程”写入口退役，不保留绕行能力。若重提没有可编辑源文件，这是合法状态；若没有不可编辑浏览版，则拒绝进入新轮。

版本历史按业务版本→内容修订/审批轮次展开；文件预览、下载、签名导出、批注和对比都接受明确snapshotId。名称和版本相同的两行不能被查询层去重丢掉，也不能按最大ID替用户选择审批对象。

## I05 小版本、大版本、规则设置和状态展示

覆盖 M17—M20、M30。

### I05.1 小版本直接受控

复用checkout/checkin接口，MINOR仅允许当前受控快照作为基础。请求包含新的不可编辑浏览版PDF票据、可选可编辑源文件票据、修改说明、一般修改声明。保存新小版本后调用一个明确的“免审批变更生效”服务：检查声明及权限、从浏览版PDF生成盖章副本、旧快照失效、新快照受控、更新Master指针、释放检出锁、写审计和通知任务。

不得调用“要求审批签名齐全”的普通批准入口，也不得复制旧签名使校验通过。统一受控平台适配器需要增加明确的MINOR_CHECKIN生效动作和依据（操作者、原因、内容哈希、源快照），以保持本地及平台状态一致。`approvedTime`为空，`publishedTime`为本次生效时间。

源件未受控、首次上传、驳回/撤回候选均不能使用此入口取得免审批受控；它们使用I03/I04。旧仅改备注的工作迭代能力不能成为普通受控文件的免审批内容替换入口，本次正式小版本要求新的不可编辑浏览版PDF，可编辑源文件仍可为空。

### I05.2 大版本

MAJOR检入保留现有项目OWNER和检出人约束；新的不可编辑浏览版PDF、revisionReason、changeSummary、affectedScope必填，可编辑源文件可选。创建下一大版本后在同一业务命令中发起新审批，用户不再另外寻找“待提交版本”按钮。所有正式切换发生在新内容完整批准后，期间旧版仍有效。

大小版本使用同一个 `DccControlledFileVersionPolicy` 分配。最小新流程从当前有效版本修改，不新增任意历史内容作为修订来源的页面。已有历史查看和证据不删除。

### I05.3 版本设置

增加简单基础设置：新文件初始大版本（默认A）、显示分隔符（点或斜杠），小版本起点固定1、步长固定1。已有majorIdentitySegmentCount策略保留在技术配置；本次新流程使用一段大版本身份。设置保存只作用新Master，Master冻结其规则快照，历史展示和比较读取该快照。

内部versionNo继续规范存储，新增displayVersion返回给页面。比较使用版本策略，不比较展示字符串；不做用户输入任意公式、改旧版本或回拨编号。

### I05.4 生命周期与进度

不必把旧枚举全量迁移。查询投影增加 `lifecycleStatus`：ACTIVE→受控、SUPERSEDED→失效、OBSOLETE→作废；未生效候选为空，页面显示“尚未受控”。`approvalProgress`单独显示待文控、待会签、驳回待修改、撤回待修改；失败显示失败原因。

所有受控入口共用一个指针切换方法：锁Master→核对基础快照→旧ACTIVE改SUPERSEDED→新内容ACTIVE→当前指针更新→审计。仅作废把生命周期设OBSOLETE，不拿作废替代普通升版失效。历史内容不删除、不在正常升版时移动旧文件。

在线浏览解析：审批中、返工中和未受控历史读取 `readOnlyFileId`；受控、失效和作废历史优先读取 `stampedFileId/publishedFileId` 这类不可编辑派生件。任何普通浏览入口都不得因浏览件缺失而读取 `sourceFileId` 或 `originalFileId`。可编辑源文件下载使用单独权限、单独接口和单独审计。

## I06 作废、同名重传与例外替换

覆盖 M22—M24、M29。

作废继续复用表单中心和现有审批。新增只读影响预览，返回直接关联/引用清单、各源项目及负责人；无目标内容权限时不泄露正文。提交增加impactReviewed=true和预览摘要（关系ID及目标快照ID），保存为申请事实；第一版不追踪多层影响或强制所有负责人回复。

批准执行时同一事务记录作废、清空当前有效指针、释放名称和有效身份占用并登记通知。Mapper必须实际执行指针置空，不能依赖会忽略null值的实体更新。原始PDF、受控副本、轮次、签名和关系历史继续存在；原文件的作废不级联作废引用方。

例外替换接口 `POST /controlled-files/{snapshotId}/exception-replace`：readOnlyUploadTicket、可选editableUploadTicket、sessionId、reason、minorChangeDeclared、password、requestKey。文控打开独立申请弹窗，提交即执行，无审批待办。服务检查当前有效、无开放检出或审批、不可编辑浏览版PDF实际变化、角色权限，建立同版本的新内容修订；以“执行例外替换”签名绑定新旧快照和哈希，生成盖章副本后共用I05切换。签名复用统一内核，操作对象为替换记录/请求键，不伪造BPM taskId；平台适配器增加明确的EXCEPTION_REPLACE动作，与MINOR_CHECKIN一样不借用审批通过事件。

例外替换不允许修改名称、编号、项目、目录、类型或分类，不能用它修复分类错误。结果不得显示新内容已获得旧轮全员批准；显示文控操作及来源批准历史。重复提交只返回原替换记录。

新增 `GET /controlled-files/{snapshotId}/obsolete-impact`；现有obsolete请求添加影响确认字段。同名重传仍使用正常新建提交接口，可传recreatedFromMasterId，服务器校验旧文件确已作废。禁止按名称自动找到一份历史文件并继承其证据。

## I07 文件关联、跨项目引用和通知

覆盖 M25—M28。

文件关联归属于源逻辑文件Master；跨项目引用归属于本项目目录，不要求存在源文件Master。两者的目标均为精确内容快照。扩展现有关系存储，记录ASSOCIATION/REFERENCE及前后变更审计；ASSOCIATION要求sourceMasterId，REFERENCE要求sourceProjectId/sourceDirectoryId和引用负责人，不新建空壳Master。每次文件送审另冻结当时文件关联清单到该内容快照，受控后维护不回改历史送审清单。

ASSOCIATION要求双方项目通过正式产品关联指向同一产品ID，允许不同项目；缺正式产品绑定则明确拒绝，不能按产品名称相同猜测。REFERENCE允许其他项目，但仅同租户且目标名称可见；目标内容权限始终独立校验。选择器只列当前受控目标，旧引用在目标失效后仍按原snapshotId访问历史，不自动换成当前指针。目录引用项标明“引用”及来源产品、项目、文件名、业务版本和内容修订；原文件名只是展示，不在本项目再次占用名称。

文件详情维护关联，项目目录维护引用，复用选择器和维护面板。两类均可新增、解除（inactive）、更新到明确选中的当前有效快照；每个动作写原因、人员、时间及前后目标。调整关系不改源/目标PDF、业务版本、审批、生命周期或访问权限。文件关联跟随源Master存在，不再为检入复制一套可变关系。

拟新增接口：

| 接口 | 内容 |
|---|---|
| `GET /controlled-file-masters/{id}/links` | 当前文件关联、来源项目、目标版本/内容修订、目标状态、有无新版 |
| `POST /controlled-file-masters/{id}/links` | 创建ASSOCIATION；targetSnapshotId、reason、requestKey |
| `PUT /controlled-file-masters/{id}/links/{linkId}` | 显式targetSnapshotId、reason、requestKey；更新前后目标都入审计 |
| `DELETE /controlled-file-masters/{id}/links/{linkId}` | reason、requestKey；只解除关系，不删审计或目标文件 |
| `GET /controlled-file-masters/{id}/link-candidates` | 同产品关联候选，按项目/关键词分页并校验可见性 |
| `GET/POST /project-codes/{projectId}/directories/{directoryId}/references` | 查询/创建目录引用；创建含targetSnapshotId、responsibleUserId、reason、requestKey |
| `PUT/DELETE /project-codes/{projectId}/directories/{directoryId}/references/{linkId}` | 确认更新目标/解除；保留前后记录，不改目标文件 |
| `GET /project-codes/{projectId}/reference-candidates` | 其他项目的当前受控候选，按项目/关键词分页并校验可见性 |

大小版本生效、作废、例外替换后，收集直接关联的双方，以及指向改变文件的项目目录引用。关联收件人取对方Master明确负责人，引用收件人取该引用明确的responsibleUserId；再加文件自身负责人并按用户去重。关联要同时查正向和反向，引用按targetMasterId查反向；保留更早旧版的引用在以后再次升版时仍能被发现，不能只查上一有效snapshotId。

复用现有站内消息任务，内容为来源文件、旧/新版本或内容修订、动作、原因、处理入口；不含正文。同一次生效引起的旧版失效合并发送一次。增加MINOR_CONTROLLED、MAJOR_CONTROLLED、OBSOLETED、EXCEPTION_REPLACED事件；失效事实包含在对应切换事件中，禁止独立任意“设失效”动作。

第一版不要求读回执、自动催办、负责人审批或递归影响任务。保留现有大版本影响评估能力，但不让其成为小版本/作废/替换通知的额外阻塞；不自动新建或作废引用方文件。默认有效检索不把指向失效/作废内容的引用当作受控结果；项目目录的引用面板仍保留该入口，明确标注目标状态和待评估提示，历史查看继续按权限校验。

## I08 最小PDF对比

覆盖 M10、M21；批注写入见I03。

复用现有PDF.js及受保护下载/预览。新增只读对比元数据入口 `GET /controlled-files/pdf-compare?leftSnapshotId=...&rightSnapshotId=...`，逐份校验内容权限并返回各自受保护读取信息；不能通过对比绕过任一文件权限。

前端按相同页码、固定渲染比例在浏览器canvas渲染两份PDF，提供左右并排及半透明叠加，计算像素差异并标红。逐页计算，不一次加载全部页；页数不同时列出新增/删除页，页面尺寸不同明确提示。存在插页时后续同页差异按实际结果显示，不开发自动页对齐。批注是独立审阅记录，不参与PDF像素差异。

不引入外部对比服务、OCR、语义模型或新商业组件。受保护文件仍走现有访问日志。不可读PDF明确报错，不返回“无差异”。

## I09 现有代码改造锚点

路径均为仓库相对路径；锚点为本轮静态复核的方法/组件。实现前重读实际文件，不依赖历史行号。

| 路径 | 锚点 | 改造责任 |
|---|---|---|
| `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/projectcode/DccProjectCodeServiceImpl.java` | `validateProjectCodeUnique` | 项目编码单独唯一，启用命令调用模板实例化 |
| `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/projectcode/DccProjectFileTemplateServiceImpl.java` | `validateUploadSelection` | 新流程不再绑定预设名称；与新目录模板职责分开 |
| `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileWorkflowServiceImpl.java` | `prepareSubmitContext`、`persistApprovalRouteSnapshots`、`resubmitWithdrawnControlledFile` | 名称占用、两阶段快照、同版本新轮重提、指定目录 |
| `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/file/vo/DccControlledFileSubmitReqVO.java` | `originalUploadTicket`、`sourceUploadTicket`、`drawingPdfUploadTicket` | 新增浏览版必填和可编辑源文件可选提交字段，停止新流程混用同一ticket |
| `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/file/vo/DccControlledFileUploadPreviewReqVO.java` | `purpose` | 增加 `READ_ONLY_VIEW`、`EDITABLE_SOURCE` 上传用途和对应校验 |
| `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/dal/dataobject/file/DccControlledFileDO.java` | `sourceFileId`、`originalFileId`、`publishedFileId`、`stampedFileId` | 新增 `readOnlyFileId`；`sourceFileId` 可空且只表示可编辑源文件 |
| `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileRouteReadinessService.java` | `applySelectedSignoffUsers` | 必需名单保留，允许增加，逐职能覆盖 |
| `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/route/DccFixedApprovalRoutePolicy.java` | `validateRouteNodes` | 新流程两阶段正式策略；旧实例按原定义读取 |
| `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccFrozenApprovalSignatures.java` | `requireComplete` | 按新轮快照逐人及职能验证，不写死四节点 |
| `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccSignatureVerificationServiceImpl.java` | `persistSignatureEvidence` | 新增轮次/内容修订绑定，不继承旧批准 |
| `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileFinalizationServiceImpl.java` | `finalizeOrdinaryApproval`、`activateRevision` | 由锁定PDF自动盖章，统一原子切换 |
| `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileQueryServiceImpl.java` | `doCheckinControlledFile`、`copyForCheckin`、`resolveBinaryFileId` | 小版直生效、大版直接送审、历史多轮返回；普通浏览只解析不可编辑链路 |
| `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileMetadataUpdateServiceImpl.java` | `updateMetadata` | 禁止受控身份字段直接改写 |
| `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileObsoleteServiceImpl.java` | `applyApprovedObsoleteControlledFile` | 影响确认、批准后释放占用、保留历史 |
| `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileRelatedFileServiceImpl.java` | `validateAndBindRelatedFiles`、`inheritRelatedFiles` | 同产品关联、跨项目引用、关系归属Master |
| `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccPublicationFollowupServiceImpl.java` | `recordPublishedRevision` | 保留大版评估，补其他事件直接通知 |
| `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/file/DccControlledFileController.java` | `uploadPreviewFile`、`updateMetadata`、`deleteWithdrawnControlledFile` | 入口授权、用途约束、封堵历史删除 |
| `IntRuoyiFronted/src/views/dcc/controlled-file/upload/submitter.ts` | `EDITABLE_SOURCE_EXTENSIONS`、`buildSubmitPayload` | 可编辑源文件去除PDF，提交浏览版必填ticket和可选源文件ticket |
| `IntRuoyiFronted/src/views/dcc/controlled-file/upload/index.vue` | `formData`、`previewUpload`、`drawingPdfUpload` | 自由名称、指定目录、资料和额外会签；拆分不可编辑PDF必填槽与可编辑源文件可选槽 |
| `IntRuoyiFronted/src/views/dcc/controlled-file/browser/index.vue` | `checkinForm`、`checkinUpload` | 大小版主流程和差异入口；检入也执行双文件合同 |
| `IntRuoyiFronted/src/views/dcc/controlled-file/detail/index.vue` | `obsoleteDialog`、详情预览和下载入口 | 轮次、批注、影响确认、关系和替换；默认预览不可编辑链路，源文件下载独立展示 |
| `IntRuoyiFronted/src/views/dcc/controlled-file/shared/lifecycle.ts` | `DCC_CONTROLLED_FILE_STATUS_DEFINITIONS` | 生命周期和审批进度分开 |

## I10 数据接入与新旧流程边界

只读预检至少列出：重名的未作废文件、重复项目编码、缺正式产品/负责人、缺目录归属、无法解析版本规则的历史文件。明确对象ID和原因；不猜测、不自动重命名、不删除数据，不做全量历史重建。

历史记录原值保留。新字段在旧行为空时，历史界面明确显示“历史流程未记录此字段”，不能编造轮次、签名或成功状态。普通旧ACTIVE文件要进入新版本/替换流程，先通过一次显式的接入检查，按现有确定事实登记名称占用、负责人和规则；不能在任意读取时自动补数据。存在歧义的文件保留可读历史，禁止新的变更，交人工修正。

新送审统一使用新定义。切换前已经开始的流程只按其原路线完成，审批人员及结果不重算；旧定义停止新建但保留在途执行及历史查询。过程版本路由和领域适配器必须显式支持这两种正式定义，不能用缺配置兜底。

建表/索引及必要占用初始化属于后续正式迁移，只有当轮授权数据库写入后才执行。本文的数据字段是开发目标，不是已经核验的线上schema。
