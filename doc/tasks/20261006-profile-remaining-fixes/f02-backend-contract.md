# F02 backend contract（实施交接）

2026-10-08；后端源码已完整落盘，等待主 Agent 集中编译及业务链审查。仅静态证据，不代表 SQL、RR、真实数据或容量 PASS。

## HTTP

- GET `/system/profile-workbench-todo/page`（使用项目既有 admin-api 前缀）：pageNo 默认1且>=1，pageSize默认10且10..100，visibility默认visible、只允许visible/hidden。
- enabledSources 使用重复同名参数，例如 `enabledSources=DCC_DISTRIBUTION&enabledSources=EDHR_WORK_TASK`；不使用逗号列表、JSON或数组下标。省略表示空集合，去重后按固定五源顺序处理。
- 五源枚举顺序：DCC_DISTRIBUTION、DCC_TRAINING、EDHR_WORK_TASK、WORK_ORDER、SHOWROOM_ASSIGNMENT。
- taskType可省略，允许文控/批记录/排产/展厅/行政；行政合法空集合。
- QuickFilter 使用Spring点号绑定：`quickFilter.fieldKey=source|detail|statusLabel`、`quickFilter.operator=contains|eq`、`quickFilter.value=文字`。整体省略或三个字段完整提供；value最长1000、按JS trim字符集合去首尾空白，空值不筛选。contains为字面包含，不解释SQL通配符。
- Sort整体省略或两个字段完整提供：`sort.key=taskType|source|detail|statusLabel`、`sort.order=asc|desc`。不是ascending/descending。未知参数、标量重复、未知枚举和残缺嵌套对象均失败。
- GET `/system/profile-workbench-todo/count`：只接受enabledSources，返回未受隐藏和页面筛选影响的businessTotal。
- 返回CommonResult，page.data为`{list,total,businessTotal,hiddenTotal,effectivePageNo,pageSize,readAt}`；count.data为`{businessTotal,readAt}`。readAt是本次读取结束时间，不是跨请求快照版本。

## Canonical row / navigation

公开行字段：sourceId/taskKey/businessId/taskType/source/detail/statusLabel/createdAt/dueAt/navigation。businessId及所有导航ID均为十进制字符串；内部numericId与联表导航原始列通过JsonIgnore不公开。navigation为字符串键值映射，可选字段缺失时省略。createdAt/dueAt/readAt使用项目Jackson现行LocalDateTime序列化规则：epoch毫秒数，空日期为null。

| sourceId | navigation正式字段 |
| --- | --- |
| DCC_DISTRIBUTION | controlledFileId、distributionId、recipientId（必有） |
| DCC_TRAINING | controlledFileId、progressId（必有） |
| EDHR_WORK_TASK | id、taskType（必有）；actionUrl、batchExecutionId、batchTaskId、executionId、businessScopeType、businessScopeId（原数据存在则传）；actionUrl可缺，保留原结构化FILL正式navigator |
| WORK_ORDER | code（原值非null则传，包括原blank值） |
| SHOWROOM_ASSIGNMENT | assignmentId（必有） |

taskKey原文：文控分发:recipientId、文控培训:progressId、eDHR工作任务:id、待排产工单:id、展厅补充指派:assignmentId。导航仍由原页面正式导航函数验证/办理，不新增后端办理动作。

## 排序、查询、事务

自选文本按UTF-8 unsigned binary asc/desc，同值继续默认排序。默认due非null优先、due升序；无due按created降序（null为1970）；再按上述source序号、numericId降序。单源SQL去掉常量source序号，其余tuple与Java完全同序。每源100缓冲、SQL取101判hasNext、严格after；五路归并只消费offset+实际页行数前缀。准确count钳制有效末页，不截断或估算total。

QueryService固定master；标准PermissionApi先检查动态权限，保留临时权益USE审计；拒绝已有外层事务，之后进入独立bean代理的只读REPEATABLE_READ事务。source没有切库、异步和新事务。请求开始计10秒预算，count/chunk前后及每次归并检查；事务和SQL同时timeout10。任何源、count、导航投影、chunk、排序/游标/计数合同或预算失败均整请求失败。

## 实施清单及静态证据

- system `api/profileworkbench/`：7个DTO/SPI/排序/Chunk文件；`mapper/profileworkbench/ProfileWorkbenchTodoSql.xml`统一投影、hidden身份关联、全范围文本过滤、排序和keyset。
- server `profileworkbench/`：6个VO/QueryService/ReadTransaction文件；`controller/admin/profileworkbench/`的Controller匹配项目admin-api前缀包规则，固定参数白名单，标准鉴权，准确三种total，RR事务和bounded merge。
- dcc：DccWorkbenchTodoMapper及XML、DccDistributionWorkbenchSource/DccTrainingWorkbenchSource。分发保留PUBLIC_FOLDER、可签收状态；培训不套文件预览状态，600秒默认和待学习/待确认保持。
- mes：MesWorkbenchTodoMapper及XML、MesEdhrWorkbenchSource/MesWorkOrderWorkbenchSource。eDHR TODO/OVERDUE，owner OR精确候选token，ARCHIVE批次30例外；工单共享CONFIRMED/SELF/未冻结，关联正式item.name，不加本人所有条件。
- mes公共谓词：MesOpenWorkTaskVisibility，旧MesProEdhrWorkTaskMapper的本人/候选及开放批次wrapper与新XML共享固定SQL；XML动态替换只有服务端固定常量，无用户SQL。
- showroom：ShowroomWorkbenchTodoMapper及XML、ShowroomAssignmentWorkbenchSource，显式tenant/user/OPEN，原notify_message联表，缺正式通知整请求失败；旧数组/20上限接口不动。
- 各SQL都显式tenant/deleted；框架租户和数据权限插件继续生效，无ignore注解。派生detail按JS trim字符集合处理，DCC标题及eDHR任务编号用实际字节长度区分空字符串和空格。

## 六AC交接

AC1：五源完整base/count/chunk与canonical投影；AC2：base之后tenant/user/taskKey的hidden EXISTS，不造历史行；AC3：SQL与Java全tuple、strict after、准确有效页；AC4：整请求错误/timeout/非法chunk，前端generation/epoch由frontend worker负责；AC5：标准权限、正式来源ID、原接口保持；AC6：单bean只读RR、master同线程、100/101缓冲，深页前缀成本保持。

静态结构检查：四个新增mapper XML由XmlDocument（XmlResolver=null）解析通过；git diff --check通过，仅CRLF提示。没有自己运行Maven，编译由主 Agent集中执行。

NOT RUN/UNVERIFIED：SQL实际执行、实际部署引擎/主库一致性、RR快照/并发、全集精确结果、每源1万/10万首中末页10秒容量、真实页面。算法末页仍需消费完整前缀，无法以静态检查证明容量。
