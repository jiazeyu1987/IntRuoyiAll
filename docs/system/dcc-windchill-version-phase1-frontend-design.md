# DCC 第一阶段 Windchill 版本前端设计

## Purpose and Scope

本文定义 DCC 第一阶段的页面、组件、状态和交互设计。前端继续使用现有文件上传、受控浏览、文件详情和统一审批中心入口，不新建一套平行版本菜单。

页面必须清楚区分“当前正式版本”和“当前工作版本”。普通用户只看到正式版本；编制人员在同一文件详情中管理 Revision、Iteration 和检出会话。所有动作是否允许以后端 `actionProjection` 为准，前端不得仅根据版本字符串或按钮权限自行推断。

## Evidence Reviewed

- 第一阶段 PRD、用户流程、验收标准、数据模型和后端 API 设计。
- 当前 `/dcc/controlled-file/upload`、`/dcc/controlled-file/browser`、`/dcc/controlled-file/detail/:id` 路由。
- 当前上传页中的手工 `changeType/versionNo`、修订候选和一次提交审批交互。
- 当前受控浏览中的版本历史、修改中标识、检出/检入按钮和动作投影。
- 当前详情页的审批、发布、签名、分发、培训和生命周期展示。
- Vue 3、TypeScript、Element Plus 以及项目统一列表和动态菜单规则。

## Pages and Routes

### `/dcc/controlled-file/upload`

保留现有路由，页面职责改为“创建逻辑文件并生成 A/1”。

- 保留项目、分类叶子、类别、目录、文件编码、文件名称、源文件和关联文件输入。
- 删除用户可编辑的 `changeType` 和 `versionNo`。
- 页面明确显示“创建后版本：A/1；当前不会提交审批”。
- 创建成功后跳转到该 Master 的工作版本详情。

### `/dcc/controlled-file/browser`

保留现有路由，列表一行代表一个逻辑文件，不再由多个 Iteration 直接占用主分页行。

- 默认版本列显示当前正式版本。
- 存在开放 Revision 时显示“工作版本 B/3”摘要。
- 存在 ACTIVE 检出时显示“由某人检出”。
- 普通用户不返回工作版本摘要和检出操作。
- 项目 EDIT 可从行操作进入版本工作区；只有项目 OWNER 显示“新建修订版”和“提交审批”。
- 正式版本不能直接检出，必须先由项目 OWNER 创建下一 Revision。

### `/dcc/controlled-file/detail/:id`

保留现有 Iteration ID 路由以兼容审批中心和签名详情跳转，但详情页通过返回的 `masterId` 加载完整版本上下文。

页面分为：

- `正式版本`：当前正式文件、状态、发布时间和受控操作。
- `工作版本`：开放 Revision、最新 Iteration、检出信息和编制动作。
- `版本历史`：按大版本折叠展示全部小版本。
- `审批与发布`：沿用现有路线、任务、签名和发布信息。

直接打开无权访问的工作 Iteration 时，显示权限错误，不跳回正式版本掩盖问题。

### 迁移盘点入口

第一阶段在现有 DCC 管理页面增加“版本迁移预检”页签，权限为 `dcc:controlled-file:migration:query`。

- 只显示统计、问题列表和导出按钮。
- 不提供一键修复、一键迁移或默认映射按钮。
- 页面明显标记“只读盘点，不修改业务数据”。

### 项目访问配置

在现有 DCC 基础数据的项目详情中增加“项目访问”页签。

- 支持按用户、部门、角色或岗位配置 OWNER、EDIT、VIEW。
- 同一主体同一项目只显示一条当前生效规则。
- 页面明确展示 OWNER 包含 EDIT/VIEW、EDIT 包含 VIEW。
- 项目负责人文本不自动转换为 OWNER；需要用户从正式账号中明确选择。

## Components

### `ControlledFileVersionContextHeader`

固定展示文件编码、名称、项目、分类叶子、当前正式版本、当前工作版本和整体状态。正式与工作版本不能只靠颜色区分。

### `ControlledFileWorkingRevisionPanel`

展示开放 Revision、最新小版本、来源版本、创建时正式基线、检出人和可执行动作。

### `ControlledFileVersionHistory`

按 Revision 分组展示 Iteration：

```text
B  工作中
  B/3 最新工作版本
  B/2 历史迭代
  B/1 来源于 A/2
A  已发布
  A/3 当前正式版本
  A/2 历史迭代
  A/1 历史迭代
```

每行显示版本、状态、修改人、时间、修改说明、来源、哈希摘要和审批发布结果。

### `CheckoutDialog`

- 显示将要检出的版本。
- 修改原因必填。
- 提交期间固定幂等键并禁用重复点击。
- 成功后刷新 Master 上下文；失败保留原因文本。

### `CheckinDialog`

- 显示基础版本和系统预计生成的版本，例如 `A/1 -> A/2`。
- 使用分段模式选择“修改内容”“仅改信息”“内容和信息”。
- 修改内容时上传新的源文件，图纸类继续使用现有配套 PDF 规则。
- 仅改信息时允许修改文件名称、类别、目录、培训标识、备注和关联文件，不允许修改项目、分类叶子或文件编码。
- 仅改信息时明确显示“文件内容不变，仍会形成新的小版本”。
- 修改说明必填。
- 用户不能编辑预计版本号。
- 上传成功但检入响应不确定时，进入“结果待确认”，先重新读取 Master/Checkout 回执，不重新提交。

### `CancelCheckoutDialog`

- 撤销原因必填。
- 明确提示不会生成新小版本，未检入工作副本将被丢弃。

### `CreateRevisionDialog`

- 列出当前大版本全部可选择小版本。
- 每项显示是否当前正式、修改时间、修改说明和哈希摘要。
- 选择非当前正式小版本时展示强提醒，并要求填写选择理由。
- 确认区明确显示“将从 A/2 创建 B/1”，不得使用模糊的“升版”文案。
- 只有项目 OWNER 可以打开和提交该对话框。

### `SubmitLatestIterationPanel`

- 只展示当前最新工作小版本的提交入口。
- 存在 ACTIVE 检出、路线未就绪或版本漂移时，显示后端阻塞原因。
- 历史小版本行不显示提交按钮。

### `VersionMigrationPreflightPanel`

- 展示冻结边界、扫描时间、主档数、版本数和阻塞分类。
- 支持按缺项目、缺分类叶子、身份混合、重复身份、多正式版本、文件缺失、生命周期漂移筛选。
- 导出前显示当前 snapshotMaxMasterId，避免把两次不同快照的统计混为一份证据。

### `ProjectAccessRulePanel`

- 按主体类型选择用户、部门、角色或岗位。
- 访问级别使用 OWNER、EDIT、VIEW 单选，不允许同一主体叠加多条当前规则。
- 保存后重新读取后端正式规则；前端不在本地合并权限。

## State and Data Flow

1. 浏览页加载 Master 摘要分页；后端已经完成权限过滤和当前正式/工作版本投影。
2. 用户进入详情后，并行读取 Master 摘要、版本历史和当前 Iteration 详情；任一关键合同缺失都显示错误，不用其他响应补算身份或版本。
3. 创建文件先通过现有上传预览获得 SOURCE ticket，再调用 Master 创建接口；成功后 ticket 已绑定 A/1。
4. 检出成功后，以后端返回的 Checkout 为唯一编辑会话，不在浏览器本地推断检出所有权。
5. 内容变更先上传源文件获取 ticket；元数据-only 直接提交结构化字段差异；响应统一返回新 Iteration。
6. 创建 Revision 时，前端提交用户实际选择的 sourceIterationId；响应中的来源不一致视为合同错误并阻止成功提示。
7. 提交审批前重新读取 Master 动作投影；后端最终确认 latest Iteration 和无 ACTIVE Checkout。
8. 审批和发布状态通过现有详情刷新和统一审批中心进入，不在前端模拟成功状态。
9. 普通用户的浏览和预览始终使用 `currentReleasedIteration`；工作版本信息不会通过隐藏 DOM 方式下发。

页面状态至少区分：

```text
currentReleasedIteration
openRevision
latestWorkingIteration
submittedIteration
activeCheckout
actionProjection
pendingWriteReceipt
```

`currentReleasedIteration` 和 `latestWorkingIteration` 不得合并成单一 `currentVersion`。

## Error States

- 逻辑文件身份冲突：保留已填字段，提示打开现有文件，不自动改走修订流程。
- 项目停用或分类不是叶子：阻止创建并明确指出失效前置。
- 文件被他人检出：显示检出人和时间，不隐藏失败或覆盖锁。
- 非检出人检入：保留上传 ticket 状态并提示所有权错误，不显示成功。
- 空操作检入：保留检出锁并提示至少修改内容或一项允许信息。
- 元数据检入包含项目、分类叶子或文件编码：阻止提交并说明这些字段属于逻辑文件身份。
- 检入响应不确定：锁定当前表单，读取幂等回执和版本历史后决定结果，不盲目重放。
- 目标不是最新工作小版本：刷新版本历史并指向最新版本。
- 已存在开放 Revision：跳转到现有工作版本，不创建第二条。
- 项目 EDIT 或 VIEW 尝试创建 Revision：隐藏正式入口；直链或接口拒绝时显示项目 OWNER 权限不足。
- 非当前正式来源缺少理由：表单原地校验，不发请求。
- 送审期间尝试编辑：显示“该版本已锁定审批”，不提供检出按钮。
- 发布失败：旧正式版本继续显示为当前，工作版本显示发布失败和正式重试入口。
- 迁移盘点存在阻塞：列表显示具体原因，不提供默认修复。
- 后端缺少 actionProjection：动作区进入只读错误态并提示合同缺失，不按状态字符串猜按钮。

## Accessibility and Responsive Behavior

- 正式、工作、审批、驳回和历史状态必须使用文本标签，不能只依赖颜色。
- 版本号采用等宽数字显示区域，但不通过缩小字体隐藏长版本代码。
- 历史表格在窄屏横向滚动，版本、状态和操作列保持可定位；不把完整历史塞入嵌套卡片。
- 检出、检入、撤销检出和新建修订版按钮使用清晰文字与对应图标，危险动作需要确认。
- 对话框初始焦点位于第一个必填字段，错误信息与字段关联，支持键盘提交和取消。
- 上传进度、提交中、结果待确认和失败状态均提供可读文本。
- “B/1 来源于 A/2”使用正文展示并提供来源版本跳转，不能仅放在 hover tooltip。
- 迁移历史存在旧标签时并列显示“当前身份 A/1，原签名版本 V1.0”，不能把旧签名页面中的 V1.0 静默替换为 A/1。
- 版本列为 AA、AZ、BA 等多字符 Revision 预留稳定宽度，不因标签增长挤压状态或操作列。

## Open Questions

本阶段前端口径已收口：默认展开开放 Revision；无开放 Revision 时展开当前正式 Revision。非当前正式来源第一阶段展示元数据和哈希差异，不建设正文可视化 diff。

## Design Blockers

- 后端 Master 摘要与动作投影合同未实现前，前端不能仅靠现有 `ControlledFileVO` 拼装版本工作区。
- 旧上传页同时创建并提交审批，必须完成接口拆分后才能删除手工版本号和 changeType。
- 当前详情路由以 Iteration ID 进入，后端必须稳定返回 masterId 才能加载完整版本上下文。
