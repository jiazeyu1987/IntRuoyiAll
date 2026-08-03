# DCC 未受控文件本地下载与自动归类 BDD 场景

## Purpose and Scope

本文件定义 `NAS 管理 -> 统计未受控文件 -> 选择下载并归类` 的可观察行为。范围覆盖 NAS 未受控文件扫描、明细选择、本地目录写入、DCC 项目代码 item 识别、文件分类识别、正式归档和 `未分类/待处理` 状态。设计要求尽量复用当前 DCC/NAS、项目代码、文件分类树和 NAS 转移链路；无法唯一识别项目代码、item 或分类时必须进入 `未分类/待处理`。

## Evidence Reviewed

- `IntRuoyiFronted/src/views/system/nas/index.vue`：已有 `统计未受控文件` 按钮、确认弹框、轮询、报告下载和 NAS 转移/导入文件夹弹框。
- `IntRuoyiFronted/src/api/system/nas/index.ts`：已有 `nas-control-audit/start`、`/{taskId}`、`/{taskId}/download` API wrapper。
- `IntRuoyiBackend/yudao-module-dcc/src/main/java/.../DccNasControlAuditServiceImpl.java`：已有固定根目录扫描、未受控文件统计和 Excel 报告。
- `IntRuoyiBackend/yudao-module-dcc/src/main/java/.../DccControlledFileNasTransferServiceImpl.java`：已有 NAS 文件读取、目录/分类复用、原始文件创建、受控文件提交和 NAS 来源映射。
- `IntRuoyiBackend/sql/mysql/20260730_dcc_nas_control_audit.sql`：已有 NAS 来源映射和 audit task 表。
- `IntRuoyiBackend/sql/mysql/20260731_dcc_file_category_match_rule.sql`：已有 DCC 文件分类规则表。
- `docs/frontend-development.md#DCC 基础条目关联文档分类树门禁`：文件分类必须来自正式 DCC 文件分类树，自动归类不得写回未分类桶。
- `doc/tasks/20260728-dcc-nas-product-code-unified/verification-report.md`：DCC/NAS 新写入已经统一使用 DCC 项目代码口径。
- `doc/tasks/20260731-dcc-file-category-rules/verification-report.md`：分类规则 seed 与后端规则匹配已有可复用验证。
- `doc/tasks/20260801-dcc-project-code-list-auto-classify-unclassified/verification-report.md`：项目代码列表级按文件名归类未分类已有前端合同。

## Feature Scenarios

### Scenario 1: 扫描完成后显示可选择的未受控文件明细

Given 用户具备 `infra:nas:query` 和 `dcc:controlled-file:query` 权限，且 NAS 配置可连接
When 用户点击 `统计未受控文件` 并确认扫描固定根目录
Then 系统后台扫描 `1. QMS documents`、`2.DHF`、`3.DMR`，保存 audit task 和未受控文件明细
And 页面显示任务状态、扫描统计、未受控文件列表、待确认数量和来源缺失数量
And 新增明细初始为 `classificationStatus=PENDING_RECOGNITION`、`downloadStatus=NOT_SELECTED`、`archiveStatus=NOT_STARTED`
And Excel 报告仍可下载，但页面不再只依赖 Excel 作为后续处理来源

### Scenario 2: 用户选择已匹配文件下载并归档

Given 统计任务已完成，未受控明细中存在可唯一识别项目代码 item 和文件分类的文件
When 用户勾选这些文件并点击 `下载并归类`
Then 页面显示每个文件的项目代码、item、文件分类和本地相对目录预览
And 用户选择本地根目录且相对路径校验通过后，后端才创建 `NAS_UNCONTROLLED_IMPORT` 任务
And 浏览器将文件写入对应相对目录并回写 `LOCAL_WRITTEN`
And 后端复用正式 DCC/NAS 写入链路创建受控文件、写入 `dccProjectCodeId`、文件分类路径和 NAS 来源映射
And 处理结果显示本地写入成功、DCC 归档成功和新受控文件编号

### Scenario 3: 无法唯一识别的文件进入未分类待处理

Given 未受控明细中存在路径和文件名无法唯一识别项目代码 item 或文件分类的文件
When 用户选择这些文件下载并归类
Then 未命中文件标记为 `classificationStatus=UNCLASSIFIED_PENDING`，多候选文件标记为 `classificationStatus=AMBIGUOUS`
And 页面统一展示 `未分类/待处理` 和具体原因，例如项目代码未命中、项目代码歧义、分类未命中或分类歧义
And 后端不得创建默认受控文件、不得写入随机项目代码、不得写入 `未分类文件类型` 作为成功分类
And 若用户仍选择下载，文件只写入本地 `_未分类待处理` 相对目录并保留原 NAS 路径结构

### Scenario 3A: 用户选择仅查看不下载不产生写入

Given 统计任务已完成且页面展示未受控文件明细和识别预览
When 用户选择“不下载/仅查看”或关闭处理确认
Then 系统保留 audit 明细和 Excel 报告下载入口
And 后端不创建 `NAS_UNCONTROLLED_IMPORT` 任务、不读取 NAS 文件内容、不写本地结果、不创建 DCC 受控文件
And 页面不得把仅查看状态展示为本地下载成功或归档成功

### Scenario 3B: 用户取消本地目录选择不产生处理任务

Given 用户已勾选未受控文件并打开下载归类预览
When 用户在本地目录授权窗口中取消选择
Then 页面保持 audit 明细和识别预览
And 后端不创建 `NAS_UNCONTROLLED_IMPORT` 任务
And 不调用 content 下载接口、不回写 `LOCAL_WRITTEN`、不创建 DCC 受控文件

### Scenario 3C: 识别结果必须持久化候选摘要和期望本地路径

Given 统计任务已完成，且未受控明细仍为 `PENDING_RECOGNITION`
When 用户或页面触发确定性预识别
Then 后端只更新当前任务的 audit 明细识别快照，不读取文件内容、不创建 import task
And `MATCHED` 明细必须持久化唯一项目代码、唯一文件分类、`classificationReason=MATCHED` 和 `expectedLocalRelativePath`
And `UNCLASSIFIED_PENDING` 或 `AMBIGUOUS` 明细必须持久化稳定原因码、候选摘要和 `_未分类待处理` 下的期望相对路径
And 候选摘要不得包含文件内容、NAS 凭据、本地绝对路径或其它租户数据

### Scenario 4: 已受控文件不会重复进入处理列表

Given 某 NAS 文件已经存在唯一 `EXACT` 当前 ACTIVE 受控来源映射
When 用户重新执行未受控文件统计
Then 该文件计入已受控数量，不出现在可选择未受控明细中
And 用户不能通过旧 audit 明细重复创建受控文件

### Scenario 5: 处理完成后项目代码详情可见归档结果

Given 用户处理的文件已成功归档到某个 DCC 项目代码 item 和文件分类
When 用户打开 `基础数据 / DCC项目代码` 的对应详情
Then 关联文档三栏中目标文件出现在正式文件分类阶段和文件类型下
And 关联文件数量更新
And 文件元数据中的 `fileTypeTaxonomyId`、`fileTypeLevel1-3`、`dccProjectCodeId` 与处理结果一致

## Failure Scenarios

### Scenario 6: 浏览器不支持本地目录写入时阻塞

Given 用户浏览器不支持 `window.showDirectoryPicker` 或等价受控目录写入能力
When 用户点击 `下载并归类`
Then 页面显示“当前浏览器不支持选择本地目录，无法下载到本地对应目录”
And 后端不得标记 `LOCAL_WRITTEN` 或 `LOCAL_WRITE_FAILED`
And 后端不得创建 `NAS_UNCONTROLLED_IMPORT` 任务
And 不得降级为 ZIP、默认下载目录或静默跳过本地写入

### Scenario 7: NAS 文件在统计后发生变化

Given 用户选择的未受控文件在统计完成后被移动、改名、大小变化或修改时间变化
When 后端读取文件内容准备下载或归档
Then 当前文件处理失败并记录 `NAS_SOURCE_CHANGED`
And 页面提示需要重新统计
And 同任务其它未变化文件可继续按真实结果处理

### Scenario 7A: 本地写入失败不触发 DCC 归档

Given 未受控文件已唯一匹配项目代码 item 和文件分类
When 浏览器读取内容成功但写入本地目录失败
Then 前端回写 `LOCAL_WRITE_FAILED` 和真实失败原因
And 后端不得调用受控文件创建链路，不得写入 NAS 来源映射
And 页面分别显示本地写入失败和 DCC 归档未开始

### Scenario 7B: 本地相对路径非法或冲突时阻塞

Given 选中文件的目标本地相对路径包含盘符、`..`、非法字符、Windows 保留名或与另一文件规范化后冲突
When 用户确认下载并归类
Then 当前文件标记为 `downloadStatus=LOCAL_WRITE_FAILED`，并写入 `localWriteErrorCode=LOCAL_PATH_COLLISION` 或对应路径校验错误码
And 系统不得覆盖本地已有文件、不得自动改名、不得取第一条继续归档
And 页面展示阻塞原因并允许用户重新选择或重新统计

### Scenario 7D: 本地预检失败发生在 import task 之前

Given 用户已完成识别并选择本地根目录
When 前端在调用 `import-selected` 前发现目标文件已存在、相对路径过长、规范化冲突或非法路径
Then 页面展示具体阻塞原因
And 后端不创建 `NAS_UNCONTROLLED_IMPORT` 任务、不改变 audit 明细下载状态、不调用 content、不回写 local-write-result
And 若 import task 创建后才发生写入竞争或句柄写入失败，前端才回写 `LOCAL_WRITE_FAILED`

### Scenario 7C: 大文件下载使用二进制传输并保持状态可追踪

Given 未受控文件体积超过前端单次 JSON 安全承载范围但仍在产品允许处理范围内
When 用户选择本地目录并开始下载归类
Then content 接口以二进制响应、流式读取或明确分块方式返回文件内容
And 前端不得要求后端把文件内容放入 JSON/base64 字段
And 任一分块或流式写入失败必须回写 `LOCAL_WRITE_FAILED` 和真实错误码，不得展示为下载成功

### Scenario 8: 分类规则缺失或歧义

Given DCC 文件分类树缺失、规则表未配置或多个分类同分命中
When 用户执行下载并归类
Then 相关文件进入 `未分类/待处理`
And 页面展示分类缺失或歧义原因
And 后端不得吞掉异常、不得归入默认分类、不得把 `UNCLASSIFIED_PENDING` 计入归档成功

### Scenario 9: 权限不足

Given 用户只能查询 NAS 或下载报告，但没有 DCC 受控文件创建或元数据更新权限
When 用户尝试处理未受控文件
Then 页面禁用或隐藏 `下载并归类` 处理入口
And 后端在直接请求时返回明确权限错误
And 不得用当前登录人或 admin 代理完成归档

### Scenario 9A: 未完成识别或签名不匹配不能创建处理任务

Given 用户提交的 audit file 仍为 `PENDING_RECOGNITION`、不属于当前 audit task 或携带过期 `sourceSignature`
When 用户直接调用 `import-selected`
Then 后端返回明确业务错误
And 不创建 `NAS_UNCONTROLLED_IMPORT` 任务、不读取 NAS 文件、不写本地结果、不创建 DCC 受控文件
And 页面展示需要重新识别或重新统计的原因

### Scenario 9B: 选中集合任一无效时整体拒绝

Given 用户一次提交多个 audit file，其中至少一个存在跨任务、签名过期、未识别、已绑定 import task、已归档或本地相对路径不匹配
When 用户调用 `import-selected`
Then 后端拒绝整个请求并返回明确错误码
And 不创建部分 import task、不创建部分任务项
And 所有选中 audit 明细的 `downloadStatus`、import 绑定和归档状态保持不变

## Boundary Scenarios

### Scenario 10: 选中文件中同时包含可归档和待处理文件

Given 用户勾选的文件包含 `MATCHED`、`UNCLASSIFIED_PENDING` 和 `AMBIGUOUS` 三类识别结果
When 用户确认处理
Then `MATCHED` 文件执行本地写入和 DCC 归档
And `UNCLASSIFIED_PENDING` 文件只进入待处理状态和待处理本地目录
And `AMBIGUOUS` 文件不得创建 DCC 受控文件
And 页面统计必须分别显示成功、待处理、失败和歧义数量

### Scenario 11: 重复提交同一选择

Given 用户因为网络重试重复提交同一批 audit file ids 和相同 `idempotencyKey`
When 后端收到重复请求
Then 返回已创建的处理任务
And 不新增重复任务项，不创建重复受控文件，不重复写 NAS 来源映射
And 若第二次请求只是 `selectedFiles` 顺序不同，规范化 `request_hash` 仍与第一次一致并返回原任务

### Scenario 11C: 相同幂等键但请求内容变化必须冲突

Given 用户已用某个 `idempotencyKey` 创建 import task
When 用户再次使用相同 `idempotencyKey` 但更换 audit file、sourceSignature 或 localRelativePath
Then 后端返回幂等键请求哈希不一致的明确冲突
And 不复用原任务处理新文件、不覆盖原任务快照、不创建新的受控文件

### Scenario 11D: 前端提交的本地相对路径必须匹配后端预览

Given 某 audit file 已完成识别并存在后端生成的 `expectedLocalRelativePath`
When 前端或直接请求把 `localRelativePath` 改成其它项目代码、其它分类目录、错误 `_未分类待处理` 前缀或本地绝对路径
Then 后端拒绝 `import-selected`
And 不创建 import task、不读取 NAS 内容、不写本地结果、不创建 DCC 受控文件

### Scenario 11E: 重复本地写入回写不重复归档

Given 某个 `MATCHED` audit file 已在 import task 中完成 `LOCAL_WRITTEN` 回写并触发归档
When 浏览器或网络重试再次提交相同 `local-write-result`
Then 后端幂等返回当前处理项和归档状态
And 不再次调用正式 DCC 归档链路
And 不创建第二个受控文件、不创建第二条 ACTIVE NAS 来源映射
And 若已有 `LOCAL_WRITTEN` 后又提交 `LOCAL_WRITE_FAILED` 或已有失败后提交成功，后端返回明确冲突，除非另有显式 retry 入口和测试覆盖

### Scenario 11A: 不同幂等键不能重复归档同一 audit file

Given 某 audit file 已在任一 import task 中完成 `ARCHIVED`
When 用户使用不同 `idempotencyKey` 再次提交同一 audit file
Then 后端返回已处理或冲突状态
And 不创建第二个受控文件、不覆盖原 NAS 来源映射、不把重复提交展示为新成功

### Scenario 11B: 并发处理同一 audit file 时只能有一个归档结果

Given 两个请求几乎同时提交同一租户、同一 audit file、不同 `idempotencyKey`
When 后端创建或执行 import task
Then 只有一个请求能获得可处理任务或已存在任务
And 另一个请求必须返回明确并发冲突或已处理状态
And 后端不能依赖前端按钮禁用作为唯一保护，不能创建重复受控文件

### Scenario 12: 大量文件分页处理

Given 未受控文件数量超过一页
When 用户按筛选条件全选并处理
Then 首版页面只能处理显式勾选并提交的 `auditFileId` 列表，必须在文案中说明是当前选择数量
And 后端只按入参 `auditFileId` 处理，不得根据当前筛选条件隐式扩大范围
And 若后续支持“处理当前筛选条件下全部结果”，必须先新增服务端 selection snapshot/token 及对应 BDD/TDD

### Scenario 13: 本地写入成功但 DCC 归档失败时状态分离

Given 某个 `MATCHED` 文件已成功写入本地目录
When 后端正式归档受控文件失败
Then 系统保留 `downloadStatus=LOCAL_WRITTEN`
And 同一文件记录 `archiveStatus=FAILED` 和归档失败原因
And 页面不得把本地成功合并展示成整体成功，必须允许用户看到归档失败并重试或人工处理

### Scenario 14: content 下载必须绑定当前任务和租户

Given 用户知道一个其它任务或其它租户的 `auditFileId`
When 用户尝试通过当前 `importTaskId` 调用 content 下载接口
Then 后端返回明确权限或绑定关系错误
And 不返回文件内容、不推进 `CONTENT_READY`、不产生 local-write-result 或 DCC 归档
And 安全日志不得泄露 NAS 服务器凭据、本地绝对路径或其它租户数据

## Open Questions

- 当前证据未发现独立的 DCC item 表；本设计将 `dcc_project_code.id` 作为 item。实施前必须再次检索是否存在更近的正式 item 模型，若存在需先更新本文档和测试。
- 真正写入用户本地目录依赖浏览器目录写入能力。若产品需要支持不具备该能力的浏览器，必须由用户另行批准 ZIP 或服务器暂存方案，因为这属于行为替代。

## Test Blockers

- 缺少可写测试租户、账号、NAS 样本目录或清理授权时，写入型真实 E2E 阻塞。
- 本机前端 `8081`、后端 `48081`、NAS 连接、Redis、数据库或 MinIO 不可用时，真实路径 E2E 阻塞。
- DCC 文件分类树、项目代码、分类规则或项目代码别名数据缺失时，归档成功路径测试阻塞；待处理路径仍可通过合成数据验证。
