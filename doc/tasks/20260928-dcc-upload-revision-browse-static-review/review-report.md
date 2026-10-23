# DCC 上传、升版、浏览静态审查报告

审查日期：2026-09-28 至 2026-09-29。范围：当前工作区源码的上传、升版、浏览及共用审批/生效链路。用户明确要求本轮暂不 E2E。

实现跟进：URB-001 至 URB-017 已在 `doc/tasks/20260929-dcc-upload-revision-browse-fixes/verification-report.md` 中完成代码修复与定向验证记录；本文件保留首次静态发现的触发条件和修复前证据，不再作为当前修复状态的唯一结论。

## 结论与证据边界

本轮记录 **17 项代码逻辑问题（5 项 P1、12 项 P2）及 5 项条件风险/需求待确认项**。均未修复。这里的“确认”表示在所列触发条件下存在可追踪的代码缺口，不表示已经在真实页面、数据库或并发环境复现。风险不计入确认缺陷数。

基线分支：`int_qms`；HEAD：`a9bcb6d36d96145ddc1252f111347b644b328deb`。工作区有大量既有未提交改动，本报告针对工作区文件，不等于 HEAD，也不等于运行中的附加 worktree。核心源码指纹另存 verification-report.md；接收方源码变化后须按方法锚点复核。

本轮只读取源码和编写文档，没有执行 E2E、API 业务调用、数据库操作、服务重启、测试或构建。既有子任务被通知暂停 E2E；改派只读审查的三个子任务均遇到服务 503，未产出有效本轮审查结果，下面的结论由主审查逐项核对。

优先处理：URB-001/002/003/012/017 涉及培训分支、流程/文件状态一致性、文件授权和动作选路。后续修复必须先补对应 BDD/RED，再实现；本文列出的验收均为未来要求，并未执行。

## 三条链路

1. 上传：`upload/index.vue` 选择项目模板/类别/目录和文件，取得源文件、图纸 PDF、普通附件票据；`submitter.ts` 组装含 session 幂等键及 needTraining 的请求；`submitControlledFile` 校验项目/类别/版本身份、绑定源文件和附件、保存路线/部门义务快照并启动 UPLOAD；会签、批准后按实例培训值进入培训记录或分发，再文控审核和受控生效。
2. 升版：浏览页选择具体版本并检出；`doCheckinControlledFile` 校验权限/检出归属、分配大小版本、绑定新源文件、创建 WORKING；浏览页显式送审；`submitWorkingIteration` 校验最新工作稿/现行版/并发候选并选流程；最终发布替代旧 ACTIVE，更新 Master 现行版。首次上传尚未生效的返工，与已经受控文件的修订须区别处理。
3. 浏览：目录/筛选参数进入 browser-page，服务端筛选权限和逻辑版本链后分页；详情加载文件、版本历史、路线、签名、培训/分发投影；预览通过受控元数据/令牌读取对应版本内容；下载使用独立授权链路。浏览页面还提供检出/检入、元数据和特定角色时间维护入口。

上传和升版的培训/分发共用缺陷只记一项，不按入口重复计数。作废不是本轮独立审查对象，仅在浏览历史/共享代码涉及它时注明。

## 源码索引

以下标识仅为缩短重复路径；每项代码依据均采用“标识:行号 + 方法名”。路径均相对仓库根目录。

| 标识 | 源码路径 |
|---|---|
| F1 | `IntRuoyiFronted/src/views/dcc/controlled-file/upload/index.vue` |
| F2 | `IntRuoyiFronted/src/views/dcc/controlled-file/upload/submitter.ts` |
| F3 | `IntRuoyiFronted/src/views/dcc/controlled-file/browser/index.vue` |
| F4 | `IntRuoyiFronted/src/views/dcc/controlled-file/detail/index.vue` |
| F5 | `IntRuoyiFronted/src/views/dcc/controlled-file/view/index.vue` |
| F6 | `IntRuoyiFronted/src/views/intern-user/time-maintenance/InternUserDccTimeForm.vue` |
| F7 | `IntRuoyiFronted/src/views/intern-user/time-maintenance/InternUserTimeAuditDialog.vue` |
| B1 | `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileWorkflowServiceImpl.java` |
| B2 | `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/dal/mysql/file/DccControlledFileMapper.java` |
| B3 | `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileFinalizationServiceImpl.java` |
| B4 | `IntRuoyiBackend/yudao-module-bpm/src/main/java/cn/iocoder/yudao/module/bpm/service/task/BpmTaskServiceImpl.java` |
| B5 | `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileQueryServiceImpl.java` |
| B6 | `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileAttachmentServiceImpl.java` |
| B7 | `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/file/DccControlledFileController.java` |
| B8 | `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/file/vo/DccControlledFileRespVO.java` |
| B9 | `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileFinalizationFailureService.java` |
| B10 | `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/file/DccPaperDistributionController.java` |
| B11 | `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccPaperDistributionAckServiceImpl.java` |

## 确认的问题

### URB-001 [P1] 工作稿送审漏存培训选项，BPM 与文件状态可分叉

- 触发条件：WORKING 已保存的 needTraining 与本次送审请求不同，包括其他合法调用方显式改变该值；当前浏览页默认回传原值，因此不能声称每次页面升版必现。
- 代码依据：B1:595 `submitWorkingIteration` 在 601 读取请求值，648 构造更新对象，661 更新内存 file，971 将其传给 BPM；B2:55 `claimWorkingIterationSubmission` 自定义 SQL 没有 `need_training` 列。
- 影响：数据库仍保留旧值。原 false、请求 true 时，BPM 等待 TRAINING，而后续批准从数据库判断无需培训并投影为待分发；反向改变时可显示等待培训却没有相应 BPM 节点。B1:1164 上传训练记录也按数据库值判断。
- 修复边界：在送审的原子状态更新中同时持久化培训值；以提交后数据库事实和 BPM 变量一致为准，不能只修页面。
- 验收：Given 两种相反的旧/新培训值，When 送审并完成批准，Then 数据库、BPM 变量、当前接收节点、详情状态四者一致；同幂等键异培训值拒绝。

### URB-002 [P1] 分发状态提交与 BPM 推进不在同一事务

- 触发条件：三流程人工分发时，文件状态更新成功，但 BPM 推进/创建文控任务失败；并发重复分发也存在相同窗口。
- 代码依据：B7:758 `releaseManualDistribution` 直接调用 B3:270；B3 类及该方法没有事务注解，283-292 分支先 `updateById`，再 `triggerTask`；307 的 transactionTemplate 仅属于后面的其他流程分支。
- 影响：数据库已经是 PENDING_DOC_CONTROL_REVIEW，BPM 仍可能停在 DISTRIBUTION；重试又被 278 的旧状态前置挡住。普通 updateById 也未用期望状态 CAS 防止旧请求覆盖新状态。
- 修复边界：文件锁/CAS、分发事实、BPM 推进放入同一事务，明确同一次操作重放语义。
- 验收：Given 分发节点存在，When BPM 推进抛错，Then 文件状态及流程均不变且可重试；并发两次分发只能产生一次有效文控推进。

### URB-003 [P1] BPM 目标接收节点缺失时静默返回，调用方仍可成功

- 触发条件：数据/运行模型不一致，TRAINING 或 DISTRIBUTION execution 缺失，而文件记录仍处于允许提交的状态。
- 代码依据：B4:1657 `triggerTask` 在 execution==null 时只 log.error 后 return；调用方 B1:1190-1198、B3:287-292 均先修改文件状态且没有检查推进结果。
- 影响：培训上传、分发接口可返回成功，但流程没有真正推进。即使为 URB-002 补事务，正常 return 也不会触发回滚，因此是独立缺陷。
- 修复边界：DCC 正式推进必须要求目标节点唯一存在且推进成功；共享 BPM 方法变更须核对其他调用方，不能把缺节点当已完成重放。
- 验收：Given 缺失接收节点，When 上传训练记录或分发，Then 明确失败，文件状态、票据绑定不产生本次成功变更；真实已完成重放按独立证据识别。

### URB-004 [P2] 普通附件上传未完成时仍可提交，已选附件可能漏入审批

- 触发条件：源文件上传成功，普通附件仍在上传，用户点击创建受控文件。
- 代码依据：F1:763 `submitBlockedByRouteReadiness` 不含 uploadAttachmentLoading；2017 异步上传结束才加入 attachmentUploads；2059 `submitForm` 不等待/拒绝附件上传；2139 只提交当时已完成的 attachmentUploads。F2 `buildSubmitPayload` 仅映射该数组。
- 影响：所选附件未进入提交载荷，后端无法知道附件遗漏；之后 uploadSubmitted=true，清理函数直接放行，迟到上传票据也可能留到定时清理。不能把附件选择列表等同于已绑定证据。
- 修复边界：按每个附件维护待处理状态并在提交时冻结完整集合；任何未完成/失败项须明确阻止或要求用户删除。
- 验收：Given 两附件一快一慢，When 慢附件未完成时提交，Then 不发业务提交；全部成功后全部绑定，失败时不得悄悄少传。

### URB-005 [P2] 上传中的文件被移除后，迟到响应可重新写回票据

- 触发条件：源文件/附件请求未返回时点击移除，或切换上传上下文使选择被清空。
- 代码依据：F1:452 el-upload 未按上传状态禁用移除；1175 清理仅识别已返回的 ticket；1899 `handleFileChange` 的 1927 返回后直接赋值，无 uid/上下文/请求序号校验；2048 移除尚未取得 ticket 的附件仍可成功，而2032响应后又加入附件数组。
- 影响：页面显示已移除，提交使用的后台数组却重新带回文件；可能绑定用户已取消的附件。源文件旧上下文返回也会污染新上下文状态。
- 修复边界：上传回写校验选择身份和上下文，移除取消/失效请求并妥善清理迟到票据；不能只改 loading 文案。
- 验收：Given 延迟上传响应，When 移除或换项目后响应返回，Then 已取消文件不进入载荷，临时资源按正式流程清理。

### URB-006 [P2] 上传成功但响应丢失后，前端预检挡住幂等回读

- 触发条件：首次提交已在服务端提交，但客户端没有收到成功响应，保留原 session/幂等键重试。
- 代码依据：F1:2078 总是先查当前版本，再按错误、已存在和 modifying 提前 return；B1:316-320 对尚无 ACTIVE 的已有 Master 报编号冲突；B1:841-874 原本支持相同键/载荷回读；F2 `buildSubmitPayload` 使用预览 session 作为幂等键。
- 影响：正常重试到不了幂等提交接口，用户看到版本链冲突/已有流程，而不是原提交结果；人工重新上传可能进一步造成重复尝试。
- 修复边界：区分首次尝试与同一次提交结果确认，保留服务端身份及载荷校验；不得简单取消重复身份守卫。
- 验收：Given 提交成功响应丢失，When 原页面原键重试，Then 回到原文件/流程且不新增；不同键仍拒绝同身份重复上传。

### URB-007 [P2] 检入幂等载荷比较遗漏培训选项

- 触发条件：一次检入已成功，使用同一源文件票据、说明、备注重试，但 needTraining 改变。
- 代码依据：B5:551-557 回读已检入结果；819 `matchesCheckinReplayPayload` 使用895-907的 CheckinReplayPayload，只有 changeDescription/remark；945-972 新建版本时却真实持久化 needTraining。
- 影响：不同业务载荷被当同一次成功重放，返回旧培训设置，用户的新选择没有生效，也没有冲突提示。
- 修复边界：幂等比较覆盖所有有业务效果的检入字段，特别是培训值；继续使用结构化载荷。
- 验收：Given 原检入 needTraining=false，When 同票据重试true，Then 明确载荷冲突；完全相同重试返回同一版本且零新写入。

### URB-008 [P2] 发布摘要把 Long 文件 ID 转 Number，丢失旧版替代关系

- 触发条件：当前文件 ID 大于 JavaScript 安全整数范围，例如 `2054545668044084024`，后端按字符串传输。
- 代码依据：F4:4044 `supersededPredecessorVersions` 先 Number(fileDetail.id)，再在4052与 supersededByFileId 原字符串比较；4077/4096用结果展示旧版收口摘要。
- 影响：真实指向当前文件的 SUPERSEDED 历史版匹配失败，已完成的版本替代被显示为没有对应旧版。
- 修复边界：身份字段全程用规范化字符串比较，不能先经过浮点数。
- 验收：Given 19位ID和精确后继引用，When 计算发布摘要，Then 显示全部真实前驱；相邻19位ID不得互相匹配。

### URB-009 [P2] 浏览页时间维护及审计入口传出精度损坏的文件 ID

- 触发条件：有相应时间维护权限的用户操作19位受控文件ID。
- 代码依据：F3:3675 `openDccTimeForm` 在3680执行 Number(file.id)；3686 `openDccTimeAudit` 同样转换。F6 `open` 原样赋给 controlledFileId 并提交；F7 `open/loadAuditList` 将数字ID传给审计接口。
- 影响：修改时间/查审计指向截断后的ID，通常查无对象；如果损坏ID恰好有效，也存在错对象风险。未声称实际发生错改。
- 修复边界：浏览入口、弹窗、API类型统一保留字符串ID；不改变服务端权限。
- 验收：Given 相邻19位ID，When 分别打开维护/审计入口，Then 传输身份与选择完全一致，结果互不串用。

### URB-010 [P2] 预览只保护部分 PDF 渲染，元数据和其他媒体响应可串版

- 触发条件：同一预览组件快速从文件/版本 A 切到 B，A 的元数据或内容请求晚于 B 返回。
- 代码依据：F5:627 `loadPreview` 虽生成 renderVersion，但643之后立即写元数据/OnlyOffice URL，668后写媒体URL/文本，686 catch写错误均未校验当前代次；537 `resolvePreviewBlob` 也会写 watermark。版本检查仅580/600的PDF阶段和689的loading收尾。
- 影响：B的页面可能使用A的Office地址、媒体内容、水印或错误；仅PDF绘制有检查不能保证整体版本一致。
- 修复边界：捕获请求上下文，在每次异步回写前统一验证，卸载后不得回写；清理旧对象URL。
- 验收：Given A慢B快，When切换至B，Then最终全部内容、标题、水印、URL、错误都属于B；A失败不得清空B成功结果。

### URB-011 [P2] 详情签名列表并发翻页时旧响应可覆盖当前页

- 触发条件：同一详情页快速切换签名页码/每页条数，或刷新与翻页并发，旧请求后返回。
- 代码依据：F4:1895分页事件直接调用 `loadDccSignatureEvidenceList`；4724方法没有请求序号，4745捕获当时分页参数，4750直接覆盖共享列表，失败和finally同样无代次保护。4840外层reloadAll的序号不会保护独立分页请求。
- 影响：分页控件显示新页，但表格显示旧页，或者旧请求失败清空已加载的新页；签名追溯结果与用户查询条件不符。
- 修复边界：签名查询捕获文件/页码/每页条数/代次，在成功、失败、finally统一检查；其他从属请求也应同样审查。
- 验收：Given签名第1页慢、第2页快，When切到第2页后第1页返回或失败，Then列表、总数、错误、loading保持第2页状态。不同文件路由按path创建新组件，本文不把它当本项必现条件。

### URB-012 [P1] 纸质分发记录读取缺少文件级授权

- 触发条件：用户拥有 `dcc:controlled-file:query` 菜单权限，但没有某个同租户文件的查看范围/矩阵授权，目标文件有纸质分发记录。
- 代码依据：B10:36 `getPaperDistributionRecords` 仅PreAuthorize菜单权限，没有传入当前用户；B11:66-93只验证文件存在，再查询分发、收件人和用户名称，没有调用文件级授权服务。与B5的详情/预览授权链路不一致。F4:4688正常详情会单独请求此读接口。
- 影响：这个独立读接口未执行详情页的文件范围守卫，可暴露目标文件的领取人、回收/签收人及相关记录。租户过滤仍适用，本文不声称跨租户可读，也没有实际发请求验证。
- 修复边界：把当前用户传到服务边界，复用正式文件读取授权后再查明细，不能靠前端“先加载详情”保障接口。
- 验收：Given同租户有/无文件授权的两账号，When查询同一文件纸质分发记录，Then无权者拒绝且零明细返回，有权者可读；再独立覆盖跨租户拒绝。

### URB-013 [P2] “负责人快照”名称来自当前目录，历史名称未冻结

- 触发条件：会签任务建立后，部门/用户改名或删除，再查看历史详情。
- 代码依据：B1:949-958保存departmentName为 `DEPT:<id>`，没有设置assigneeName；B5:3657读取当前部门名并在3679优先覆盖快照名，3681返回未赋值的人员名；F4:944和4155回退当前userNameMap。
- 影响：用户ID仍被冻结，因此不是换人签署；但标为“快照”的历史名称会随当前目录改变或退化成ID，不能还原当时展示身份。
- 修复边界：任务创建时保存真实名称，历史展示优先不可变快照；当前名称如有需要独立标注，不能覆盖历史。
- 验收：Given任务创建时生产部/人员甲，When目录改名或账号删除，Then历史仍显示创建时名称和稳定ID，任务办理归属不变。

### URB-014 [P2] 内容批准时间实际在最终文控完成时才写入

- 触发条件：批准完成后经过较长培训/分发等待，最终文控审核通过。
- 代码依据：B1:2831/2983处理MATRIX_APPROVAL完成只更新状态，无approvedTime写入；B3:373-380在最终文控完成时用LocalDateTime.now写approvedTime，B2:68实际更新该列；F4:4015将其显示为已批准时间。
- 影响：批准至培训/分发之间的时间证据无法从approvedTime还原，批准时间被推迟成最终审核时间。与 `docs/dcc-three-workflows/technical-design.md` 第149行内容批准/最终文控时间分离要求不一致。
- 修复边界：在内容批准完成事务记录批准时间，最终审核/生效使用独立时间；重试不能重写已完成事实。
- 验收：Given三个不同时间点分别批准、分发、文控，When最终生效和重试，Then批准时间始终是第一个时间点，最终审核/发布时间各自可追溯。

### URB-015 [P2] 已完成分发在后续驳回或生效失败时被投影成未完成

- 触发条件：分发已经通过，最终文控驳回，或最终生效失败进入FINALIZATION_FAILED。
- 代码依据：B5:3133-3156 `populateNativeWorkflowExecutionProjection/isNativeWorkflowDistributionCompleted` 仅将待文控、ACTIVE、SUPERSEDED、OBSOLETE视为完成；B1:1269可写REJECTED，B9:60可写FINALIZATION_FAILED；F4:1181/1198据此显示分发完成提示或暂无分发记录。
- 影响：当前状态变化抹去了已发生的分发事实，历史详情在最需要排查失败时给出错误摘要。
- 修复边界：按冻结的分发完成事件/节点历史投影，不用少数终态枚举替代真实完成证据。
- 验收：Given已完成分发，When后续驳回或生效失败，Then分发仍显示完成且可查当时操作者/时间，当前失败原因另外展示。

### URB-016 [P2] 检入已成功但列表刷新失败被报成检入失败

- 触发条件：checkin请求成功返回新版本，随后getList失败或新筛选下找不到原逻辑文件。
- 代码依据：F3:2190检入，2200关闭弹窗，2201刷新，2202合并结果，都在同一个try；2205统一提示检入失败；2218 `mergeCheckinResult`也可能在成功后抛错。
- 影响：后端已生成版本并清除检出，但用户得到失败反馈，容易重试或认为更改未保存；新版本ID也未先作为成功结果固定呈现。
- 修复边界：区分写成功和读刷新失败，保留新版本身份并提供刷新恢复；不把刷新错误吞掉。
- 验收：Given检入成功，When刷新失败，Then明确提示已检入及新版本、仅刷新失败；重试刷新不再次检入。

### URB-017 [P1] 已受控 NEW 文件升小版本后仍走上传流程

- 触发条件：首次上传生成的ACTIVE文件changeType=NEW，申请人对其进行MINOR检入，再提交新工作稿。
- 代码依据：B5:731允许ACTIVE检入；615-620仅majorRevision分支设置REVISION；945/974 `copyForCheckin`其他情况直接继承来源changeType；B1:682只按file.changeType选REVISION或UPLOAD，因此新小版仍选UPLOAD及NEW路线配置。
- 影响：上传和升版虽然节点顺序相同，但用户要求独立三套流程；新小版错误消费上传路线，可能使用不同审批人/配置，统计归属也错误。首次尚未生效的返工继续NEW则是正确场景，不能一律改所有检入为REVISION。
- 修复边界：根据正式现行/历史受控来源与版本关系确定业务动作，保留首次上传返工语义；同时核对revisionBaseActiveControlledFileId。
- 验收：Given已生效NEW的A/1，When MINOR产生A/2并送审，Then使用REVISION定义/路线；Given尚未首次生效的NEW返工，Then仍使用UPLOAD。依据同一technical-design.md第42行。

## 条件风险与待确认项

### URB-R01 培训证据失效后的前后端门禁不同

- 已确认代码：B5:3141查询真实FileDO，缺记录时trainingRecordAvailable=false且3123禁止分发；B3:284只检查trainingRecordFileId非null，B1:1171也阻止再次上传。
- 条件：既有绑定文件后来失效/丢失。未检查运行态是否存在此类数据，未认定删除入口必然允许这种状态。
- 风险：页面阻止而命令仍可能推进，且缺少补证路径；需要核对正式文件删除保护、存储丢失处置及恢复权限。
- 修复/验收：门禁共用真实证据校验；证据失效时不得推进，授权修复留审计后才能继续。不得把非空ID视为实际证据。

### URB-R02 检入没有继承普通附件，也没有附件维护入口

- 已确认代码：B5:615-626创建新版本只继承relatedFiles，不调用attachmentService；B6按controlledFileId查附件；F3检入载荷只有源文件/图纸PDF等，不含普通附件。
- 待确认：普通附件属于单版本专属，还是应随文件包继承。若应继承，连仅改备注的小版本也会丢失附件可见性；若不继承，页面应明确并提供新版本附件维护。
- 修复/验收：先明确生命周期；用含两普通附件的文件分别升大小版，检查新旧版附件集合及权限/证据归属，不默认复制未审批证据。

### URB-R03 三流程训练记录在详情仅显示文件名，无法从该区域查阅

- 已确认代码：B8只投影trainingRecordAvailable/trainingRecordFileName；B1:1192只绑定独立trainingRecordFileId；F4:1561-1568是文本提示，没有培训记录预览/下载按钮；普通附件集合不包括该记录。
- 待确认：文控是否必须在审核页查看培训记录正文。若需要，现有“已上传”提示不足以完成证据审核；本轮未把其他管理模块可能存在的文件入口当正式替代。
- 修复/验收：提供与受控文件及审核资格绑定的培训证据只读入口，覆盖无权拒绝、失效错误和历史记录；不得暴露裸存储链接绕过授权。

### URB-R04 浏览分页前全量取候选，数据增长存在性能风险

- 已确认代码：B5:313-347、350-363、389-426先selectWorkflowList/selectBrowserSummaryList，再Java权限过滤/版本聚合，最后sliceRows；有分页参数不等于数据库只查一页。
- 条件：大租户、广目录范围、版本数多。尚无本轮性能测量，不能声称已超时。
- 修复/验收：结合授权与逻辑文件聚合设计有界候选或数据库分页；用符合真实授权分布的数据量测量SQL行数、耗时、内存和结果完整性，禁止先分页再权限过滤造成漏项。

### URB-R05 “培训/分发完成”的业务口径与早期开发文档存在差异

- 已确认代码：B1:1164上传训练记录后直接触发TRAINING；B3:283分发按一个授权操作者触发DISTRIBUTION，普通受控文件在B3:591-593发布时不创建通用人员分发计划。
- 需求边界：用户明确了节点顺序和培训checkbox，但早期文档D03/D04把逐人阅读确认/签收列为待业务核对默认值，不能当成已确认需求。
- 风险：现有五条顺序路径PASS只证明节点走通，不能证明逐人培训或签收已实现。
- 修复/验收：先确定采用上传记录+人工确认还是逐人完成门禁，统一设计/验收矩阵；若要求逐人，必须验证未完成任何一人不得进入文控。

## 已排除的误报与覆盖边界

- 检入copyForCheckin保留originalFileId看似引用旧原件，但当前WORKING/待审预览优先sourceFileId，不能仅凭该字段就声称审批预览必然是旧正文。
- 当前普通受控文件transferTask/returnTask/createSignTask公开入口明确拒绝；不能把外来文件内部转办实现的问题直接记为本轮普通上传/升版必现缺陷。
- Long ID的Number转换按实际用途分类：下载入口虽然转换loading标记，但实际下载仍传原ID，未记为下载错ID。
- 会签负责人ID被冻结、同人多部门义务已建模，并不等于姓名也被冻结；URB-013只针对历史展示身份，不声称运行中负责人被配置变更替换。
- URB-001的缺字段不能以现有浏览页默认回传相同值直接作为真实失败复现；需测试旧/新值不同的合同场景。
- `IntRuoyiFronted/src/layout/components/AppView.vue` 第32/61行按route.path设组件key，详情路由noCache=true；因此排除了“直接切不同文件必然复用同一详情实例”的初稿推导。URB-011仅保留同页真实存在的请求竞争；普通刷新失败保留旧详情作为剩余设计风险，不单列确认缺陷。
- 本轮没有扫描所有DCC周边模块、租户配置和真实存储完整性。URB-012是特定读入口的代码授权缺口，不代表已验证实际越权。OnlyOffice部署连通性、SQL迁移是否实际执行也不在静态结论内。

## 后续处理顺序

1. 修复URB-001/002/003/012/017，补流程、授权和持久化集成回归。
2. 修复上传完整性与重试URB-004/005/006/007，再处理URB-016的成功反馈。
3. 修复身份和异步展示URB-008/009/010/011。
4. 修复历史审计投影URB-013/014/015，确认URB-R01至R05的处置边界。
5. 用户恢复E2E要求后，再按每项验收通过真实页面验证；不沿用历史五条正常路径PASS关闭上述问题。
