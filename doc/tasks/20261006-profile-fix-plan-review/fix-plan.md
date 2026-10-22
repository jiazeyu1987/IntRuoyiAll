# 个人中心四项问题修复方案

## 2026-10-08 用户批准的 F02 验收变更（当前生效，优先于下方历史 MySQL 门禁）

用户明确同意 F02 只做静态代码评审，不做真实数据验证；保留编译、类型检查和 lint。此变更仅调整 F02 的验收方式，不缩减原 F02-AC1～AC6 功能范围，不更改来源、权限、租户、排序、隐藏、事务、错误和共享 badge epoch 合同；F01 已融合，不重新实施，F03/F04 原定向行为及临时内存 H2 验证保持。

- 保留：主 Agent 与隔离 reviewer 沿页面/角标→wrapper→Controller→标准权限→来源适配器→SQL/Mapper→聚合/事务→返回和导航逐节点评审，逐项记录文件/行号/方法锚点；原六 AC 均需对应静态推导和未验证边界。编译（包含目标模块依赖闭包）、前端类型检查、目标文件 lint、差异/范围检查为放行门槛，既有基线错误须实际对照并明确区分。
- 移出本轮门槛：F02 原 S1 来源真实集合运行对照、MySQL 双连接 RR/快照、H2/HTTP/数据库读写测试、真实容量首中末页与 EXPLAIN/耗时、前端真实数据/E2E 和 F02 行为测试执行。无测试库、无 MySQL 配置不再阻塞 F02 实施及本轮静态放行，不建立任何替代数据验证环境。
- S1 改为来源 SQL / count-chunk 同谓词 / keyset 与比较器同序 / 标准权限和租户 / master 选择与 RR 调用链的静态评审及编译；通过此门槛后允许迁移工作台和角标。S2/S3 以静态链条及编译/类型/lint证据放行 F02，不再引用下方历史运行门槛否决本轮。
- 仍需记录：SQL 实际执行、实际部署表引擎/数据源一致性、RR 快照和并发行为、精确结果全集、大数据量性能、真实页面全部 NOT RUN/UNVERIFIED，不能记录为 PASS 或“已证实容量”。10秒错误预算、100/101缓冲、完整排序/游标和正常容量设计目标保持；不能通过提高 pageSize、取首批、部分成功或算法 fallback 回避缺陷。
- F02 六 AC 静态检查映射：AC1 五源完整 base/投影/过滤及准确计数；AC2 hidden EXISTS 的 tenant/user/taskKey 身份和筛选前顺序、恢复刷新；AC3 SQL ORDER BY/keyset/Java比较器全tuple同序、尾键和有效页钳制；AC4 失败整请求、非法chunk/timeout与页面 generation/badge epoch 的所有写入守卫；AC5 标准动态权限、eDHR OR、展厅显式 tenant/assignee、旧接口合同；AC6 数据源/只读RR bean代理链、同线程/同事务无切库、缓冲上界和深页成本推导（实际快照/性能未验证）。
- 本轮仅授权任务代码和必要本地提交/主干融合；不推送、不发布、不重启主干服务、不连接或写入真实数据库。共享权限审计在生产标准路径保持，不因本轮未连接数据库而删改。

## 目标、状态与证据边界

目标：修复 F01–F04，令仍可办理的逾期待办可见、所有符合条件的来源任务可被分页查询和隐藏恢复、缺联系方式账号可以只改昵称、修改密码单次提交并在成功后清空。

当前状态：文档编制；**未实现、未运行构建或业务测试、未做真实复现**。方案放行以同目录 `review-report.md` / `task-state.json` 为准，由主Agent维护；作者不自行判断。本文的结论来自工作区源码静态检查，不代表修复或业务验收通过。基线 HEAD 为 `f68e333e418e0873759a943ff681f8410b5aabb1`；工作区可能含并行修改，实施前重新核对下列方法，不能只凭提交号复用结论。

已读规则：根 `AGENTS.md`、本任务 `task.md`、`docs/task-closeout-rules.md`、`docs/frontend-development.md` 的入口/标准列表/局部错误/筛选合同，以及 `docs/backend-development.md` 的模块边界和个人待办悬空关系门禁。最近根规则覆盖旧默认 BDD/TDD、E2E、提交要求。本轮仅编写本任务文档；实施、测试、数据库写入、上线各自需要相应授权，方案评审不等于其中任何一项已完成。

不在范围：交接、消息和审批中心迁移；ERP/基础配置迁移；共享排产归属变化；隐藏即完成或减少业务角标；个人信息卡片或原始时间行恢复；分散权限校验重构；密码强度、历史和会话策略改变。

## 四项问题及静态依据

| 编号 | 触发条件与业务影响 | 源码及真实方法锚点 |
| --- | --- | --- |
| F01 / P1 | eDHR 正式状态转为 OVERDUE，当前用户仍是办理人/正式候选人，批次状态仍允许办理。工作台漏行，角标漏数，用户失去个人入口。 | `IntRuoyiFronted/src/views/Profile/components/ProfileWorkbench.vue`：`loadEdhrRows` 未传 status；`IntRuoyiFronted/src/store/modules/profileWorkbenchTodoBadge.ts`：`loadEdhrWorkTaskTodoTotal` 只传 TODO；`IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrWorkTaskServiceImpl.java`：`getMyPage` 默认 TODO，仅接受 TODO/OVERDUE，`isProcessableStatus` 接受两者；该模块 `dal/mysql/pro/batchrecord/MesProEdhrWorkTaskMapper.java`：`selectMyPage`、`applyMyTaskVisibility`、`applyOpenWorkTaskBatchVisibility`。 |
| F02 / P1 | 任一正式来源任务超过首批、目标筛选行位于首批以后，或隐藏任务已移出来源首批。工作台本地分页/筛选并非全范围查询，隐藏恢复入口漏行。 | `ProfileWorkbench.vue`：`TODO_PAGE_SIZE=50`、五个 `load*Rows` 固定 pageNo=1，`filteredRows`/`pagedRows`/`hiddenRows` 只消费首批；`loadEnabledSource` 失败返回 []，其余来源仍合并。`IntRuoyiFronted/src/views/showroom-admin/assignment/contracts.ts`：`normalizeAssignmentPage` 只接受数组。展厅 `workflow/service/ShowroomAssignmentService.java`：`pageAssignments` 从 `selectList()` 过滤后内存切页，`page` 把有效页大小封顶20；`controller/admin/ShowroomAdminController.java#getAssignmentPage` 返回 List。角标 `loadShowroomAssignmentTodoTotal` 在20条时明确拒绝缺total。 |
| F03 / P2 | 手机/邮箱缺失的账号只修改昵称。前端 required 拦截；简单去 required 后，空字符串手机号又会被后端长度校验拒绝，空邮箱存在误清空风险。 | `IntRuoyiFronted/src/views/Profile/components/BasicInfo.vue`：`rules`、`submit`、`init`；`IntRuoyiFronted/src/api/system/user/profile.ts`：`UserProfileUpdateReqVO`、`updateUserProfile`；后端 system 模块 `controller/admin/user/vo/profile/UserProfileUpdateReqVO.java`：mobile `@Length(min=11,max=11)`，email `@Email`，均没有非空要求；`service/user/AdminUserServiceImpl.java#updateUserProfile` 使用 `BeanUtils.toBean` + `userMapper.updateById`，`validateEmailUnique`/`validateMobileUnique` 对blank跳过。 |
| F04 / P2 | 校验尚未结束或改密请求尚未结束时重复点击；成功后旧/新/确认密码仍留在表单。可能产生多次请求和重复错误，敏感值滞留。 | `IntRuoyiFronted/src/views/Profile/components/ResetPwd.vue`：`submit` 直接 `formEl.validate(async callback)`，无锁或loading，成功只提示；`reset` 调用 resetFields；`IntRuoyiFronted/src/components/XButton/src/XButton.vue`：loading 默认false，`handleClick` 仅emit，无Promise锁；`profile.ts#updateUserPassword`；system `AdminUserServiceImpl#updateUserPassword` 保持旧密码、强度、历史和凭据逻辑。 |

上表中后端模块内路径均位于 `IntRuoyiBackend/yudao-module-<模块>/src/main/java/cn/iocoder/yudao/module/<模块>/`；以下列表列出实施目标完整相对路径或完整前缀，便于独立定位。

## 修复覆盖与完成判定

本次仅复核确认F01–F04四项缺陷。已撤回的设计建议、非缺陷历史需求及前述范围外意见不计为此次漏修；若实施中发现新的问题，单独记录并提交范围评审，不加进这15项AC冒充已授权工作。

| 缺陷与根因 | 应实际完成的改动（当前均未实现） | 生效依赖 | 原AC与完成证据 | 尚未验证边界 |
| --- | --- | --- | --- | --- |
| F01：默认TODO使OVERDUE漏行/漏数 | 新eDHR来源显式纳入TODO/OVERDUE，保持本人/候选及批次谓词；工作台和角标使用该集合，正式导航不变 | 与F02聚合一起交付；来源合同、权限及MySQL预验证通过后才迁移入口 | F01-AC1～AC3的mapper、聚合、前端导航行为报告，且F02完整性报告通过 | 当前仅静态调用链证据；真实办理页面未验证，不能据此称办理成功 |
| F02：多来源首批后本地筛选/分页/hidden交集 | 来源SQL准确count/分块、server全局归并、新page/count、服务端hidden筛选、共享epoch；旧接口不变 | 同主库同快照、来源完整集合对照、首中末页容量与并发预验证；page与badge范围各自守卫 | F02-AC1～AC6实际报告，包括真实MySQL、前端生产函数行为及定向回归 | 深页前缀成本、SQL投影/权限/租户/快照尚无运行证据 |
| F03：前端required及空值请求合同不匹配 | 前端可选但非空仍校验，空值省略、已有值清空提示零请求；service显式set-if-nonnull，原共享校验/unique/blank合同不变 | 前端真实校验/序列化、后端真实Validator/HTTP/Mapper与OAuth2回归；可独立实施 | F03-AC1～AC3实际报告及持久化readback，不依赖F02完成 | 运行时更新策略及两个HTTP入口边界未验证；真实页面未验证 |
| F04：校验/请求期间无锁，成功不清空 | 校验前取得提交锁、loading/disabled、失败保留可重试、成功清三字段并clearValidate、finally解锁 | 真实SFC异步校验/请求行为与现有密码策略回归；可独立实施 | F04-AC1～AC3实际报告，验证每个结束路径及连续点击 | 真实页面与实际改密请求未验证，不改变安全/历史/会话策略 |

四项修复完成必须有原15项AC对应的实际报告且零未决阻塞，主Agent复核后才可下结论。文档结构通过、方案放行或部分F03/F04通过均不是四项修复完成。E2E仅后续当轮明确要求时执行；未做真实页面验证须明确标NOT RUN，不能把合成行为报告称E2E，但不自行加默认E2E门禁。实施与上线授权仍分别确认。

## 开发前提检查表（实施轮执行，本轮未检查环境）

每项由实施负责人记录检查时间、源码指纹、脱敏证据和PASS/FAIL/BLOCKED；以下动作是计划，当前环境全部未证实就绪。不得输出连接串、密码或令牌值。

| 前提 | 可执行检查动作与通过条件 | 缺失影响及停止范围 |
| --- | --- | --- |
| 当前源码及规则 | 用`rg`重新定位本方案方法/接口、最近AGENTS及适用docs，与原静态依据逐项比较并记录改动指纹；生产行为有变化时更新对照，交主Agent确认 | 被改变合同对应的实现/验收停止；不沿旧结论继续。只影响某项时其它独立项可继续 |
| 五来源完整集合、投影、权限/租户/导航 | 按test-plan来源对照表，在同身份、租户和静止合成夹具上执行现行正式查询完整遍历与新mapper；按源namespace比较ID集合、canonical字段和正式导航，记录实际权限入口。展厅按有效20条遍历并在静止夹具最后追加空页，不用请求50判尾；这仅为测试对照 | 任一来源无法形成正确完整对照时停止F01/F02入口迁移及整体验收，不以mock source结论替代 |
| 单主库同快照 | 查dynamic主库及事务bean装配；核对所有来源/关联/visibility表属于同master且支持同快照（如InnoDB）；mapper不得切库/REQUIRES_NEW。实际MySQL两连接测试证明同请求counts/chunks保持RR快照，记录连接和事务边界 | 跨库、不支持快照或切事务则F01/F02预验证BLOCKED；先修前提/复审，不能声称total/list一致 |
| JDK17/Maven/pnpm与现有依赖 | 后续分别执行`java -version`、`mvn -version`、`pnpm --version`，核对JDK17、package/lockfile及测试编译依赖；按本计划定向命令验证可装配 | 缺JDK/Maven阻塞后端对应验证，缺pnpm/前端依赖阻塞前端验证；只记录环境问题，不绕过或自动安装替代 |
| 任务测试Redis | 实读各目标模块unit-test配置，确认测试Redis目标归属；只读`Test-NetConnection -ComputerName localhost -Port 16379`核对可达，并由定向测试证明协议连接。缺失时不重启共享服务 | 阻塞依赖该Redis的测试报告；其它不依赖测试可继续，但相应AC不能PASS |
| MySQL任务专有库及写入授权 | 先核对本轮写入授权和任务专有数据库归属；只检查三项PROFILE_WORKBENCH_TEST_MYSQL环境变量存在性，不输出值。获授权后核对脱敏库身份、MySQL版本/引擎/DDL与部署差异，再创建合成夹具 | 无授权/归属不明/配置缺失时停止所有MySQL夹具写入及F01/F02容量/隔离预验证。F03/F04可独立推进，但四项整体仍BLOCKED |

## F01：正式 TODO 与 OVERDUE，保持办理和来源身份

实施与 F02 同一交付完成。新的 eDHR 来源查询显式取 `status IN ('TODO','OVERDUE')`；旧 `my-page` 的单status合同及默认 TODO 不变。不得加入 DOING/DONE/CANCELED，也不使用权限更窄的 stats 接口补数。

把 `MesProEdhrWorkTaskMapper#applyMyTaskVisibility` 和 `applyOpenWorkTaskBatchVisibility` 的现行条件复用为来源查询公共谓词，旧my-page与新count/chunk都调用；保留 assignee OR 精确候选快照token、同租户、逻辑删除和批次终态条件。同一任务命中assignee及候选时仍只有一个DO行，不join候选展开重复计数。特别保留 ARCHIVE 在批次30时仍可见、其它任务排除30/40/50/60，ARCHIVE排除40/50/60；不能把所有终态统一删除。

eDHR行的正式 `id/taskType/actionUrl/batchExecutionId/batchTaskId/executionId/businessScopeType/businessScopeId` 由该任务及现行VO投影返回，身份为十进制字符串。前端继续调用 `normalizeEdhrWorkTaskRoute` / `navigateToEdhrWorkTask`，不按工单号、批号或列表索引猜路由；进入后仍由原接口检查当前办理资格。计数使用完全相同的权限/来源状态谓词，隐藏不改变业务总数。

## F02：选定后端聚合分页，不再用客户端首批本地分页

### 模块与接口位置

默认方案为**来源模块数据库查询 + yudao-server 五路归并**。不是提高pageSize、逐页调用现有内存分页接口直到拉完，也不维护新增业务投影表。

静态依赖依据：`IntRuoyiBackend/yudao-server/pom.xml` 已直接依赖 system/dcc/mes/showroom；dcc、mes、showroom 的pom已依赖system，mes还依赖dcc，showroom依赖dcc/bpm。不能把聚合服务直接加到system并反向依赖这些业务模块，形成循环。server现有 `YudaoServerApplication` 扫描 `.server` 和 `.module`。因此system只定义领域无关查询DTO/SPI，五个固定来源适配器在各领域模块内实现，聚合编排在server；不新增模块依赖边。

新增 API：

- `GET /system/profile-workbench-todo/page`：入参 `pageNo>=1`、`pageSize=10..100`、`visibility=visible|hidden`、`enabledSources`、可选 `taskType`、可选 `quickFilter(fieldKey,operator,value)`、可选 `sort(key,order)`。来源白名单为 DCC_DISTRIBUTION、DCC_TRAINING、EDHR_WORK_TASK、WORK_ORDER、SHOWROOM_ASSIGNMENT；taskType保留现有五类，行政当前无来源，合法空结果。
- 响应：`list`、当前筛选/visibility下准确 `total`、未受筛选/visibility影响的 `businessTotal`、未受筛选影响的 `hiddenTotal`、`effectivePageNo`、`pageSize`、`readAt`。`total/businessTotal/hiddenTotal` 必须为非负整数，不能用当前list.length代替。`readAt`是读取时间提示，不冒充跨请求冻结快照版本。
- `GET /system/profile-workbench-todo/count`：同一 `enabledSources`，仅返回 `businessTotal/readAt`；与page使用同一个来源base谓词。菜单角标改用本接口，移除展厅20条启发式计数。隐藏仍计入角标，筛选仍不改变角标。
- 原各来源公开分页、展厅数组返回、hidden-keys/hide/restore接口保持合同；其他调用方不被强制迁移。新count/chunk是内部SPI，不为任意用户ID开放HTTP查询。

`enabledSources` 由各调用方现有权限/入口判断生成。工作台仍保留 `checkPermi` 和展厅 `super_admin || hasCachedRouteName('ShowroomAdminAssignment')`；角标仍保留现行permissionStore路线判断，不把菜单可见性当服务器授权。服务端对请求的DCC/MES来源重新按现有正式权限判断：分发query、培训mine、eDHR工作任务query OR 批次query、工单query，包含当前有效动态权益，不能退化为静态角色预筛。请求包含无权来源时整体403而不是返回删减来源成功页。来源未启用不查询、不计数，属于请求选择范围，响应必须保持该范围。

展厅当前 `getAssignmentPage` 没有 `@PreAuthorize`，服务只做当前租户过滤；不能虚构一个现存展厅权限码。新来源沿用登录可读的服务授权边界，且固定 `assigneeUserId=登录人`，绝不接受外部传入负责人/租户。其前端入口gate继续保留。新查询的同租户必须显式条件：`ShowroomFieldAssignmentDO` 带 `@TenantIgnore`，不能假设框架自动加租户。所有来源均由登录上下文确定租户/用户；缺上下文明确失败。

### 固定来源合同及新的SQL投影

适配器接口为 `count(query)` 与 `readChunk(query, afterKey, limit=100)`；返回投影行、末行游标、`hasNext`。count和chunk必须复用同一个base/筛选/visibility SQL片段。先在SQL内投影下述canonical字段，再在派生查询外层做筛选/排序，最后LIMIT；禁止先分页再做Java过滤。只有被选中显示的行才补导航数据，不为计数做N+1详情读取。

| 来源 | 保持的base合同、稳定身份、投影及现行排序依据 |
| --- | --- |
| 分发 | `DccDistributionTaskServiceImpl#getMyDistributionTaskPage/buildTaskRowOrNull`：当前user收件人、acknowledgedAt为空、关联distribution存在且medium=PUBLIC_FOLDER、关联file存在且status属于ACTIVE/SUPERSEDED/OBSOLETE。以recipientId唯一；联表有每表tenant/deleted条件。显示详情取fileNumber、title/fileName、版本，状态待签收，createdAt取publishedTime，due为空；旧排序publishedTime desc、recipientId desc。 |
| 培训 | `DccTrainingTaskServiceImpl#getMyTrainingTaskPage/buildTaskRowOrNull/fillCommonTaskFields/resolveProgressStatus`：当前user进度、关联file存在、acknowledgedAt为空；唯一progressId。实际列表buildTaskRowOrNull仅过滤缺file，**不擅自添加预览入口 `loadTrainingVisibleFile` 的file状态/publishedFile条件**。status按已有累计秒数规则：required>0取required否则600，accumulated null取0，达标READY_TO_ACKNOWLEDGE，否则PENDING_VIEW。这属于保留现行业务算法，不新增fallback。两状态合为一源，不重复progress。详情、createdAt沿现行VO，due为空；旧排序publishedTime desc、progressId desc。 |
| eDHR | F01的现行可见谓词；唯一taskId；detail取taskCode、workOrderCode、batchCode、processName、taskType现行文字映射，缺taskCode显示现行任务编号文字。due取dueTime否则overdueAt，createdAt取createTime。原my-page按id desc，但新来源必须按统一sort投影排序，不能依赖原顺序归并。 |
| 工单 | `MesProWorkOrderServiceImpl#getWorkOrderPage`、`MesProWorkOrderMapper#selectPageByProductIds`：CONFIRMED + SELF + temporaryFrozen=false，保持框架现有数据权限/租户条件，仍是共享可见待排产工单，不改为本人所有。唯一workOrderId。`MesProWorkOrderController#buildWorkOrderRespVOList` 用itemService的物料name填充productName；新轻量查询join同源item名称，缺item时productName保持原VO的null，不从工单name或另一产品表补齐。详情code/productName/batchCode/非零quantity；createdAt=requestDate，due=plannedStartTime，状态已确认待排产。 |
| 展厅 | `ShowroomAssignmentService#pageAssignments/toDetail`：当前tenant、登录assignee、OPEN，唯一assignmentId。现行详情的notifyContent来自关联system NotifyMessage的templateContent，不能从指派备注或当前模板猜。新轻量投影显式join该通知，关联通知缺失/错租户时失败，不inner join静默丢行。targetType/targetId/fieldCode沿指派正式字段，字段文字映射与前端contracts逐项合同锁定；不需要返回currentDraftValue。当前工作台映射没有createdAt/dueAt，保持两者为空，不借assignment.createdAt改变优先级。旧公开数组接口仍按createdAt desc、id desc，封顶20，与新内部query无关。 |

canonical row字段为 `sourceId/taskKey/businessId/taskType/source/detail/statusLabel/createdAt/dueAt/navigation`。taskKey保持 `文控分发:<recipientId>`、`文控培训:<progressId>`、`eDHR工作任务:<id>`、`待排产工单:<id>`、`展厅补充指派:<assignmentId>`，避免已有隐藏记录失效。businessId和所有导航ID使用十进制字符串。system公共navigation只定义字符串字段集合，由sourceId区分；DCC携带原controlledFileId/distributionId/recipientId或progressId，工单原code，展厅assignmentId，MES携带F01列出的正式任务导航字段。无任意外部URL，也不将domain VO类型反向引入system。

详情字符串的SQL表达式遵守当前 `compactJoin`：trim各段、空段省略、分隔符为 ` · `；title/fileName、任务类型/字段名/状态名映射与原前端一致。quantity按原JS数值显示去无意义小数零，0段省略，需用明确数值格式合同校验，不直接CAST得到10.00而前端原来显示10。前端直接显示canonical文本，避免SQL筛选的是一套字符串而UI又格式化成另一套。SQL标签参数/CASE由来源映射集中定义，不能引入业务身份推断。

来源、详情和statusLabel快速过滤只支持现有contains/eq，trim检索值，空值不筛选，未列入白名单的field/operator返回400。大小写、百分号/下划线、中文及首尾空白与现行 `String.includes`/严格等值语义一致：使用参数化binary比较和字面子串查找，不能把用户 `%` 当SQL通配符。taskType是独立eq条件，多个已正式映射的条件为AND；不额外解锁当前hook禁止的多个未映射quickFilter条件。

隐藏通过SQL `EXISTS`/`NOT EXISTS` 关联 `system_profile_workbench_task_visibility`，条件必须包含tenantId、登录userId和原taskKey。先应用来源base，再判断隐藏；不从隐藏记录的旧detail造业务行。完成、消失或无权限的历史隐藏键不进入hiddenTotal/list，记录保持不动；隐藏不等于完成。恢复调用原restore接口成功后重新查询当前tab/页，并更新page业务总数；若最后一行消失，服务按total回到有效末页。页面不再靠本地Set把未完整加载的业务集合切两半。

### 全局排序与分页算法

默认排序保留现行业务优先级并补确定性尾键：

1. 有due的行先于无due，due升序；相同due按固定来源顺序、businessId数值降序，不插入新的created时间优先级。
2. 无due的行按createdAt降序，缺created按现行0语义放后；相同created按固定来源顺序、businessId数值降序。
3. 固定来源顺序为分发、培训、eDHR、工单、展厅。尾键由source命名空间+正式唯一ID确定；同号不同来源不是同一任务。

标准模板当前有custom sort事件但工作台未绑定sortState。为避免服务端分页后变成页内排序，新工作台绑定 `v-model:sort-state`/`sort-change`；支持taskType/source/detail/statusLabel文本字段升降序，statusTime列显式 `sortProp=statusLabel`，动作列不可排序。自选排序使用canonical文本binary顺序，null文本作空字符串；同值用默认排序和唯一尾键。清除排序回默认，筛选/tab/sort改变重置page1。不改公共标准模板源码或列持久化key。

聚合每次请求先取五来源各项count，计算businessTotal、hiddenTotal、筛选后的total，钳制effectivePageNo。令offset=(effectivePageNo-1)*pageSize。每个已启用、筛选后非空来源读取首个100行块，块内SQL顺序与全局比较器完全一致。维护最多五个源头的最小堆，取最前行，推进该源；块耗尽且hasNext时按末行完整sort tuple做严格keyset `after` 查询。弹出offset行后收集pageSize行即停，禁止按原本不同的source默认id/时间排序直接归并。只消费所需的全局前缀，不拉全部任务到客户端或后端内存；总数来自SQL count，不取已遍历条数估算。

chunk显式hasNext通过内部limit+1查询得出，不用 `list.length < HTTP请求pageSize` 推断结束；游标包含当前sort完整tuple与ID，等值边界不得跳过或重复。读取少于count预测的前缀、重复taskKey、非法数量/ID、chunk乱序、末尾游标不推进都属于合同错误，整请求失败；不能去重后继续标完整。

### 一致性、容量与错误合同

量化成本推导：五源各100,000匹配行、无筛选且全部处于所查visibility时total=500,000；pageSize=100，末页pageNo=5,000，offset=499,900，归并需消费约500,000行前缀、约5,000次100行chunk SQL，另有准确COUNT、limit+1及来源预读。10秒完整服务请求端到端预算除以5,000，平均每块约2ms，尚未给COUNT、归并CPU、导航投影及响应处理分配时间。这是算法推导，**不是benchmark，也不能据此断言一定失败或已可用**；预算不是每条SQL各10秒。必须在前端迁移前用下面阶段门禁实际预验证，保留算法、预算和目标规模。

server在同一主数据源、同一线程、只读 `REPEATABLE_READ` Spring事务里顺序执行counts、隐藏EXISTS和chunks，避免多线程丢租户/权限/事务上下文。主数据源配置在application-local/dev的dynamic.primary=master；通过独立外层 `@DS("master")` 编排bean再调用事务bean，保证先选数据源再取得连接；来源适配器不得切库或REQUIRES_NEW。使用真实mapper，不自调用Controller绕过权限，也不发内部HTTP。部署若来源迁到异库、事务管理器不支持同一读快照，明确缺前提阻塞，不能声称total一致。

单次请求各来源缓冲100行，最多约500个轻量行（limit+1查询临时多1行）；HTTP list上限100。令N=offset+pageSize、S为启用非空来源数，内存归并CPU成本O(N log S)、缓冲内存O(S*100+P)。为跳过offset仍须消费前缀，chunk SQL调用约O(S+N/100)，并非深页只调用一次SQL；每块都会重新执行来源联表、筛选和keyset条件，缺适合索引时还会重复扫描及排序。canonical派生文本筛选/排序可能不能直接利用基础列索引；base/hidden/filtered的准确COUNT也可能多次扫描。上述数据库成本不包含在单纯堆归并CPU估计内。

10秒是拟定的完整服务请求端到端技术预算（包含COUNT、全部chunk、归并/导航及响应处理，不是每条SQL各10秒），**没有已有benchmark或业务SLO证据支持**。超时必须整请求明确失败、无list/total成功载荷，且不自动换算法或取缓存；这只证明错误处理，不能算任务可达性或容量通过。正常性能门禁为每来源10,000及100,000匹配行规模下的首/中/末页、筛选和排序均在该技术预算内返回完整准确结果；详细组合见test-plan。任一正常组合超时/错误/结果缺失，则该规模性能为FAIL；环境/授权缺失则BLOCKED，不能以明确失败替代通过。深页prefix和准确count成本是当前架构待实证的主要风险。若正常规模不达标，先停止F01/F02迁移及整体验收，评估实际EXPLAIN、索引/查询调整并重新评审方案和验证，不自动切算法、降低规模或容忍部分结果；F03/F04可独立推进但四项整体仍BLOCKED，本轮不猜DDL、不执行建索引。

同一page响应count与list属于同一读快照；独立count及后续page刷新各自新建事务，反映当前业务状态。不承诺跨请求冻结任务生命周期，也不承诺同范围的两个独立新事务businessTotal在业务变化后恒等；相等断言只适用于相同读取快照或无并发变化的固定夹具。前端递增页面requestGeneration，捕获查询、来源gate、用户/租户及visibility；仅当前generation能原子替换页面rows/totals/error/loading。新查询开始清空当前rows和total有效标记，失败显示局部“加载失败/请重试”，不显示“没有待办”或0总数。页面generation只保护页面，不能单独保护共享角标。

角标store统一拥有单调递增的badgeUpdateEpoch和提交令牌 `{epoch,userId,tenantId,badgeSources}`（集合去重并规范排序）。每次新page查询、独立count查询、hide/restore操作开始，以及身份/来源集合变化，都先调用同一beginBadgeUpdate使epoch递增、loaded=false并隐藏旧数；epoch不因登出重置。page/count捕获各自令牌，成功/失败/finally对角标total、loaded、error、loading的任何写入都经过同一tryCommit，要求令牌epoch等于store当前epoch且身份/来源集合仍匹配。旧请求即使自己的generation仍有效也不得提交角标。pendingPromise只允许复用同epoch、同身份、同badgeSources的查询；新page意图不得复用旧count Promise。

确切顺序：旧count C开始取得epoch=1 → 新page P开始取得epoch=2 → P成功，仅epoch=2提交businessTotal → C迟到，其epoch=1被拒绝，不覆盖total/loaded/error/loading；C迟到失败也不能抹去P成功数。反向次序P开始→新C开始时P仍可按页面generation提交行，但不能用旧角标令牌覆盖C。hide/restore开始先使旧C/P令牌失效，同时递增页面generation使旧page不能更新行；写成功后再启动新page刷新取得新令牌。身份或来源gate变化也先递增页面generation、失效全部旧角标令牌再发新读。取消请求仅节省资源，不能替代令牌守卫。

工作台与角标现行展厅gate不同：page的enabledSources与当前badgeSources相同才有提交businessTotal资格，还必须持有最新令牌；不相同时page不能提交角标，在page开始使旧令牌失效后按角标自身范围启动新count并取得更新令牌。范围不同的page失败只写页面错误，不污染该count的角标状态。写入hide/restore成功但随后读取失败时明确分别显示“已隐藏/恢复；列表刷新失败”，不能报告写入失败并诱导重写；写入失败也不允许已失效的旧读重新提交。保留taskKey动作锁直到写及读取结束。非启用源不查询；已启用源任意失败时page/count整体失败，局部错误区域接管提示，不重复全局toast，不能只展示成功来源。

## F03：可选联系方式与明确更新语义

静态序列化依据：`IntRuoyiFronted/src/api/system/user/profile.ts#updateUserProfile` 直接put对象；`src/config/axios/index.ts#request/put` 不转换数据，service对PUT没有把空字符串改null的逻辑；标准JSON省略undefined但保留null及空字符串。现有个人资料与OAuth2两个VO均为email @Email/@Size(max=50)、mobile @Length(min=11,max=11)，没有手机号正则或NotBlank；Controller各自@Valid，OAuth2转换后调用同一service。mobile空字符串长度0拒绝；任意非数字11字符及11个空格不被Length额外拒绝，blank的unique跳过。email空字符串按现有Email/Size空值语义允许，空白及非空非法邮箱仍按现有Email校验，不因unique跳过而合法。service没有trim/blank拒绝，非null空邮箱会进入updateById；DO无联系字段策略覆盖，本轮未运行Mapper确认配置，但方案不得把历史允许的空邮箱改为新增拒绝。

本前端入口选定合同为局部更新：**联系方式未提供或null表示保持已有值；本入口仅提交非空有效值，不新增清空能力**。共享API保留原注解允许的直接请求输入，包括原有blank边界；不把前端入口限制扩展成共享后端的新业务强制要求。

前端 `init` 保存mobile/email原始基线。移除通用required，保留非空邮箱格式、长度≤50及手机号现有 `^1[3-9]\d{9}$` 校验。原值缺失(null/undefined/空白)且输入为空白时，payload白名单组装nickname/sex/avatar和有值联系方式，空联系方式直接省略；不把整个formModel（包含id/dept/roles等）直接put。输入非空先trim再校验，不能因可选跳过非法格式。原有联系方式非空、用户清空/只留空白时，在该字段明确提示当前修改入口不支持清空，请保留或填写有效值；阻止请求，不把原值偷偷补回后提示修改成功。正常昵称必填保持。

后端不修改 `UserProfileUpdateReqVO`、`OAuth2UserUpdateReqVO` 的Email/Size/Length，不添加手机号正则、NotBlank或服务blank拒绝，不删除原mobile Length。只将 `AdminUserServiceImpl#updateUserProfile` 的持久化改为显式update wrapper：联系方式非null时set，null/缺失不set；保持原unique调用和其它字段现有非null局部更新行为。不trim、不把blank变null、不使用isNotBlank决定是否set，因此原注解允许的直接请求空邮箱仍可写空；mobile空字符串仍由原Length拒绝，原来Length允许的11字符手机号不会被新增正则拒绝。该改动只明确null/省略不误删除，不改变其它用户管理保存服务。

现有 `OAuth2UserController#updateUserInfo` 通过BeanUtils转换对象调用updateUserProfile，需要回归null/省略保留、有效更新、空email允许写空、空mobile按Length拒绝、11字符非标准mobile维持既有Length边界、非空非法邮箱仍按Email拒绝；不变更授权或旧API有效输入合同。前端已有但非法的非空值不得当缺失跳过：显示并要求用户纠正再提交，不自动抹去。前端阻止清空时零请求、明确提示，因此不会以保存成功掩盖未被接受的清空操作。

## F04：校验开始即锁，失败可重试，成功清空

`ResetPwd.vue`增加submitting状态；submit入口先检查form存在及submitting，立即置true，覆盖完整表单校验和请求期；在finally统一false。采用await表单校验，已知字段校验失败由表单错误呈现、无请求；程序性异常及API失败明确呈现/记录，不能空catch。保存XButton绑定loading和disabled，重置按钮及密码输入在提交期disabled，避免校验/请求期间变更载荷。请求使用本次已验证字段快照，只调用原updateUserPassword一次。

API wrapper增加可选 `ignoreErrorMessage` 参数供本页本地接管错误，默认调用方行为不变；本页catch只对当前请求展示正式错误，失败保留字段、无成功提示，finally释放按钮，可修正后再次提交。验证失败也释放。成功时三字段显式设空、clearValidate，再提示成功；不调用会回填旧密码的初始化，不等重新进入页面才清空。用户普通reset仍清空并清理验证；提交期不允许reset。保留 `systemPasswordRule`、确认密码一致、原后端旧密码/强度/历史/凭据更新/会话逻辑；不增加自动登出、不绕过密码历史。

## 每项可观察验收（实施后才可记录PASS）

| AC | 观察标准 |
| --- | --- |
| F01-AC1 | 同权限/本人范围、可办理批次下，TODO和OVERDUE都在正确全局页出现，角标含两者；DOING/DONE/CANCELED不出现。 |
| F01-AC2 | 非本人且非正式候选不出现；本人也是候选只计一次；终态及ARCHIVE特例与原my-page一致；仅批次query动态权益仍可读本人任务。 |
| F01-AC3 | OVERDUE行从原正式导航办理入口进入；刷新/隐藏/恢复不改变原业务任务状态与办理守卫。 |
| F02-AC1 | 每来源分别超过旧50/展厅20，且五源混合超过多页时，准确total和全局页无首批遗漏、无重复；全范围筛选命中旧首批以后任务。 |
| F02-AC2 | 任意页任务隐藏后，可在完整已隐藏页查询并恢复，刷新仍正确；已结束/无权限历史hidden key不造行；角标业务总数不因隐藏减少。 |
| F02-AC3 | 默认优先级、同due/同created稳定尾键和四个文本列升降序在所有来源的全局页一致；翻页、改页大小、筛选重置、末页缩短都正确。 |
| F02-AC4 | 启用来源/隐藏查询/count/chunk任一失败、超时、合同错误时没有成功部分列表/伪0；错误可见；迟到响应、恢复后的旧响应、换用户/租户不覆盖新状态。 |
| F02-AC5 | 旧展厅数组接口、20页上限及其他来源公开分页合同不变；所有来源权限、正式ID及租户边界保持；同范围page/count仅在相同快照或静止数据夹具上断言businessTotal一致，独立事务可反映不同业务时刻。 |
| F02-AC6 | 同一次count/行读取并发业务更新时响应内保持一致；流式缓冲上界成立；两种计划规模的首/中/末页、筛选、排序均在拟定技术预算内返回完整准确结果，否则容量FAIL/BLOCKED。显式超时负例只属于AC4错误合同，不算本项通过。 |
| F03-AC1 | 缺一种/两种联系方式、null/undefined/空字符串基线，只改昵称成功且请求省略空联系方式，数据库现有联系方式无误清空。 |
| F03-AC2 | 前端非空有效联系方式仍能修改且unique仍拒绝冲突；前端非空非法格式/长度不能提交；共享API原Email/Size/Length和blank输入边界不变，null/缺字段不删除已有值。 |
| F03-AC3 | 原值非空而用户清空，字段明确提示且无更新请求；用户恢复或填写有效值可继续，不出现假成功；OAuth2调用的局部更新语义兼容。 |
| F04-AC1 | 在验证Promise未结束、请求未结束时连续点击，只执行一次验证周期/最多一次改密请求，按钮显示loading且禁止重置/改值。 |
| F04-AC2 | 校验失败零请求；业务/网络失败保留字段、无成功提示且按钮恢复，修正后可重试；错误明确可见且不重复全局提示。 |
| F04-AC3 | 成功三字段为空、验证提示清空、按钮恢复；新一轮需要重新填写；现行强度、历史、旧密码及会话策略不变。 |

## 预计改动文件与依赖顺序

以下全是未来实施目标，本轮没有编辑这些文件。

| 顺序 | 文件/接口改动 |
| --- | --- |
| 1：公共查询合同 | 新增 `IntRuoyiBackend/yudao-module-system/src/main/java/cn/iocoder/yudao/module/system/api/profileworkbench/` 下 `ProfileWorkbenchTodoSource.java`、`ProfileWorkbenchTodoQueryDTO.java`、`ProfileWorkbenchTodoRowDTO.java`、`ProfileWorkbenchTodoChunkDTO.java`、`ProfileWorkbenchTodoSortKey.java`；固定白名单、游标、字段/比较器、null与身份合同。system只提供接口/DTO，不调用业务模块。 |
| 2：领域来源 | DCC `service/file/` 新增 `DccDistributionWorkbenchSource.java`、`DccTrainingWorkbenchSource.java`，该模块 `dal/mysql/file/` 新增 `DccWorkbenchTodoMapper.java` 和 `src/main/resources/mapper/profileworkbench/DccWorkbenchTodoMapper.xml`；MES `service/pro/batchrecord/` 新增 `MesEdhrWorkbenchSource.java`，`service/pro/workorder/` 新增 `MesWorkOrderWorkbenchSource.java`，该模块 `dal/mysql/pro/` 新增 `MesWorkbenchTodoMapper.java` 和对应 `src/main/resources/mapper/profileworkbench/MesWorkbenchTodoMapper.xml`，复用现有eDHR可见谓词；showroom `workflow/service/` 新增 `ShowroomAssignmentWorkbenchSource.java`，`dal/mysql/workflow/` 新增 `ShowroomWorkbenchTodoMapper.java` 和对应XML。每个Java目标均在其模块现有包前缀下。 |
| 3：server装配 | 新增 `IntRuoyiBackend/yudao-server/src/main/java/cn/iocoder/yudao/server/profileworkbench/` 下 `ProfileWorkbenchTodoController.java`、`ProfileWorkbenchTodoQueryService.java`、`ProfileWorkbenchTodoReadTransaction.java` 及page/count VO。同源只读事务、准确计数、五路归并、10秒预算；所需测试依赖若未由现有依赖传递则仅补server/pom.xml的test scope，不反向添加system依赖。 |
| 4：工作台/角标 | 新增 `IntRuoyiFronted/src/api/system/profileWorkbenchTodo/index.ts`；修改 `src/views/Profile/components/ProfileWorkbench.vue` 和 `src/store/modules/profileWorkbenchTodoBadge.ts`：服务端page/count、total有效状态、排序、页面generation及共享badgeUpdateEpoch/提交令牌、局部错误、隐藏动作后刷新。旧来源wrapper及展厅contracts保留。 |
| 5：联系方式 | 修改 `IntRuoyiFronted/src/views/Profile/components/BasicInfo.vue`、`src/api/system/user/profile.ts`；后端仅system `service/user/AdminUserServiceImpl.java` 的显式set-if-nonnull。前端字段白名单、空字段省略及清空提示；两共享ReqVO和原校验/unique/blank边界不变。 |
| 6：密码 | 修改 `IntRuoyiFronted/src/views/Profile/components/ResetPwd.vue` 及同profile.ts错误选项；不修改XButton及后端密码实现。 |
| 7：测试与验收 | 按 `test-plan.md` 新增行为/mapper/事务/HTTP契约测试，同步原个人中心静态合同中过时的旧API/请求次数/本地分页断言。不放宽原用户入口、权限、单表布局和错误可见性。 |

领域来源数值和排序合同先完成再接聚合；聚合与F01状态同时完成后再替换前端。F03/F04可独立实施，但共用profile.ts需同一提交阶段整合冲突。该任务涉及有限结构重构，若下一实施轮按根规则启动子Agent，必须由主Agent明确分配文件归属；本轮作者不再启动Agent。

## 实施阶段门禁与文件归属

| 阶段 | 应完成的工作/证据 | 进入下一阶段条件与失败停止范围 |
| --- | --- | --- |
| S0：前提与来源合同 | 执行前提检查表；锁定五源base/投影/身份/权限/租户/导航对照及旧接口合同，形成test-plan来源证据记录 | 合同不清或源码变化未复核则停止受影响项；F02环境前提缺失不妨碍独立F03/F04，但整体不能完成 |
| S1：最小mapper/聚合原型预验证 | 先实现来源count/chunk及最小聚合链路，不替换工作台/角标；运行真实mapper对照、MySQL双连接RR并发、两规模首/中/末页与筛选/排序容量预验证 | 必须有准确集合、同快照和完整结果/10秒端到端报告。未达标即停止F01/F02入口迁移及整体验收，分析EXPLAIN和各count/chunk成本；修订方案交主Agent复审后再试，不全部前端完成后才暴露风险 |
| S2：替换工作台与角标 | S1通过后接新page/count，保留正式入口，验证全局筛选/隐藏恢复、页面generation及共享badge epoch，更新受影响静态合同 | 前端真实生产函数行为与接口合同通过再进入全套验收；单独静态通过不足。F01与F02同批交付 |
| S3：全套定向验收 | 按test-plan执行原15AC及所列现有回归，确认原型之后最终代码未破坏容量/隔离证据；改变SQL/索引/事务后重跑相关预验证 | 每AC有实际报告、无失败/未决阻塞才由主Agent确认四项修复完成；未运行真实页面单列NOT RUN。上线仍另需授权 |

F03/F04不依赖S1，可在相应前提满足后各自实施和定向验证，部分结果须单列。`IntRuoyiFronted/src/api/system/user/profile.ts` 由主Agent在实施开始时指定一名文件整合负责人，BasicInfo与ResetPwd实施者提交所需改动，由该负责人合并并同时跑F03/F04回归；未明确归属前停止该共享文件的并发编辑。本轮不分派或启动Agent。

## 风险、验证和放行区别

主要风险是新SQL投影与现行Java/前端显示算法不一致、跨模块绕过数据权限、展厅显式租户遗漏、事务选库顺序错误、同排序值游标遗漏、深页和准确count的扫描成本。测试必须比较真实现行查询结果及新查询base的身份集合，不能只用source mock证明权限一致；H2可验证mapper绑定/租户SQL，MySQL隔离/二进制文本/性能需独立数据库合同验证，不能互相冒充。未获得数据库写入授权时不得运行这些需要建夹具/插入的验证。

方案放行仅由主Agent复核本文和test-plan、确认无未决核心合同后决定；作者不自判放行。实施放行意味着可以按明确范围改代码，不等于验证通过。实施验收需实际运行定向验证并记录输入与报告；本轮所有计划命令均未运行。E2E不属于本轮或下一轮默认门禁，后续仅在用户当轮明确授权时按真实页面路径执行。上线仍需当轮授权及部署MySQL计数/性能/隔离证据；未验证的运行条件必须保留为阻塞，不以静态文档通过记作业务PASS。
