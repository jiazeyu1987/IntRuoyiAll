# DCC 第一阶段 Windchill 版本配置安全部署设计

## Purpose and Scope

本文定义 DCC 第一阶段版本机制的配置、最小业务安全边界、权限、测试环境部署切换和可观测性。当前交付范围仅为测试环境，不纳入生产密钥治理、生产灾备、生产发布审批或生产 Go/No-Go；业务权限、审计、幂等、文件哈希和数据完整性仍是 DCC 功能要求。

## Evidence Reviewed

- 第一阶段 PRD、数据模型、后端 API 和前端设计。
- 当前 DCC 类别权限、目录访问规则、受控浏览、上传 ticket、签名、下载和水印审计。
- 当前 `controlled_content_version_ref` 单正式版本约束和发布事务。
- `docs/database-rules.md`、`docs/backend-development.md`、`docs/frontend-development.md`、`docs/release-backup-restore.md` 和项目无 fallback 规则。

## Configuration

### Revision 序列

Revision 不使用运行配置表，统一由正整数 `revision_sequence` 通过确定性 Excel 式字母算法生成：

```text
1=A, 2=B, ..., 26=Z,
27=AA, 28=AB, ..., 52=AZ,
53=BA，并按相同规则继续
```

服务端必须同时提供 sequence -> code 和 code -> sequence，并通过边界测试证明互为逆运算。客户端不参与计算。唯一失败边界是 revision_sequence 超出服务端正整数容量，此时明确拒绝，不循环回 A。

### 文件编码规范化

规范化规则作为代码和数据库合同固定：首尾空白删除、字母统一大写、保留内部有效字符。是否折叠内部空白或全角字符必须在实现前基于现有编码数据决定；未批准前不得擅自改写。

### 上传与哈希

继续使用现有 DCC 上传大小策略、ticket 过期策略、文件类型校验和 SHA-256。检入使用新的上传 session/purpose 约束，不允许复用已经绑定其他业务动作的 ticket。

### 迁移模式

不增加“新旧版本模型切换开关”。部署状态由 schema/preflight/postflight 门禁决定；后端启动时检测必需表、列、索引和迁移版本，缺少任一前置即启动失败或禁用整个 DCC 写模块并明确报告，不能回退旧写路径。

### 当前环境范围

- 本阶段只面向本机或已确认测试环境。
- 不配置生产域名、生产账号、生产对象存储或生产数据库。
- 不要求生产安全评审、生产备份恢复责任人或生产维护窗口批准。
- 测试环境仍需区分目标租户和任务数据，不能修改无关并行任务数据。

## Secrets

- Revision 算法、编码规则和迁移状态不是秘密。
- 现有文件存储、数据库、OnlyOffice 和签名密钥继续使用项目现有秘密注入方式，本阶段不复制或迁移明文。
- 上传 ticket、预览 token、下载 token、Cookie 和 Authorization header 不得写入版本事件、迁移导出或普通日志。
- 迁移映射包可以包含业务 ID、版本和哈希，但不得包含文件正文、连接串或账号密码。
- 日志只记录 ticket 的不可逆摘要或业务 requestId，不记录完整 ticket。

## Permissions

### 页面与动作权限

继续使用：

- `dcc:controlled-file:query`
- `dcc:controlled-file:submit`
- `dcc:controlled-file:review`
- `dcc:controlled-file:approve`
- `dcc:controlled-file:print`

新增：

- `dcc:controlled-file:checkout`
- `dcc:controlled-file:checkin`
- `dcc:controlled-file:checkout-cancel`
- `dcc:controlled-file:revise`
- `dcc:controlled-file:migration:query`

类别动作增加 EDIT 和 REVISE。动作最终放行要求同时满足：项目访问权、类别动作权限、全局接口权限和当前版本状态。

项目访问权来自 `dcc_project_access_rule`：

- OWNER：包含 EDIT、VIEW，可以创建 Revision 和提交审批。
- EDIT：包含 VIEW，可以检出、检入和撤销本人检出。
- VIEW：只能查看当前正式版本。
- 主体支持 USER、DEPT、ROLE、POSITION；同一主体同一项目只有一条生效规则。

目录管理、系统管理员或超级管理员身份不能自动获得工作源文件正文权限。管理权限与业务正文权限分别判断。

### 角色边界

- 编制人员：检出、检入、撤销本人检出。
- 文件责任人：创建下一大版本、提交最新工作小版本。
- 审核/批准人员：只访问当前任务锁定的小版本。
- 文控：发布、迁移预检和受控历史管理。
- 普通用户：只访问当前正式版本。
- 管理员强制解锁：第一阶段无此权限和接口。

## Security Controls

- 所有写接口后端重新校验租户、项目、Master、Revision、Iteration 和 Checkout 归属，不能信任客户端 ID 组合。
- 项目访问必须来自 `dcc_project_access_rule`；项目负责人文本、项目修正任务和菜单权限不得补齐项目访问。
- sourceIterationId 必须属于当前 Master 的当前大版本；服务不得按版本号字符串跨 Master 查找。
- 创建 Revision 时同时记录用户选择来源和创建时正式基线，防止事后无法解释内容来源。
- 平台候选 source ref 指向当前正式版本，实际内容来源指向用户选定小版本；二者必须分别校验和审计。
- 检出锁由数据库唯一索引保证，不能仅用前端按钮禁用。
- 内容检入必须校验上传 ticket 的租户、上传人、session、purpose、未绑定状态和哈希；metadata-only 检入必须校验允许字段差异并复制独立源文件记录。
- 工作版本预览/下载使用现有受控业务文件访问服务，只有编制参与人和当前审批参与人可以读取。
- 普通浏览 API 不序列化工作源文件 ID、文件路径或可用 token。
- 送审后源文件、哈希、元数据、关联和路线快照不可修改。
- 发布时重新校验审批 Iteration、文件哈希、Master 当前正式指针和平台 ACTIVE 引用。
- 版本事件写入失败时，检出、检入、修订、送审和发布不能返回成功。
- 迁移预检导出按租户隔离；跨租户导出需要独立平台审计权限。

## Deployment

### 发布包组成

第一阶段至少拆成以下有序迁移与代码交付：

1. schema prerequisite/preflight：校验现有表、列、索引和脏数据，不写业务数据。
2. additive schema：新增 Revision、Checkout、Version Event、发布快照和新字段。
3. approved backfill：只读取批准映射包回填，不产生默认值。
4. constraint cutover：建立新唯一索引、移除旧身份索引和旧检出字段。
5. backend/frontend cutover：只读写新模型。
6. postflight：验证指针、版本、平台生命周期、文件和哈希一致。

每个测试环境 release migration 仍必须有正式 metadata、dependsOn、允许环境和风险级别。回填迁移必须依赖 additive schema 与批准映射包校验。

### 维护窗口

- 回填前关闭 DCC 创建、检出、检入、修订、送审、发布和作废写入口。
- 当前仅测试环境，维护窗口内关闭 DCC 写入口；是否继续只读浏览由本次测试安排决定。
- 不使用双写追平窗口内新增数据。
- postflight 未通过时不恢复 DCC 写入口。

### 部署顺序

```text
当前代码、测试数据库和任务文件快照
-> schema preflight
-> additive schema
-> 进入维护窗口
-> approved backfill
-> constraint cutover
-> 部署新后端
-> 部署新前端
-> postflight 和回归
-> postflight 全部通过后恢复测试写入口
```

### 回滚边界

- additive schema 完成但未回填、未写新模型时，可以回退旧应用。
- 回填开始前保留可定位的测试数据库和任务文件快照，供工程回滚，不作为生产灾备证据。
- 新模型发生首次测试写入后，旧代码不能读取或写入新状态；回退应恢复切换前测试快照或采用向前修复。
- 禁止通过重新启用旧 `version_no` 解析或旧 checkout 列进行临时降级。

## Observability

### 结构化日志

每个版本动作记录：`requestId`、`eventKey`、`tenantId`、`masterId`、`revisionId`、`iterationId`、`checkoutId`、actor、动作、结果和 failureCode。日志不记录文件正文、token 或凭据。

### 指标

- 逻辑文件身份冲突次数。
- ACTIVE 检出数量和最长检出时长。
- 检入成功、失败、幂等重放和基础版本漂移次数。
- 新建 Revision 成功、非当前来源选择和重复开放 Revision 拒绝次数。
- 送审旧小版本拒绝次数。
- 发布成功、失败及旧正式版本保留次数。
- DCC Master 与平台 ACTIVE 引用漂移数量。
- 迁移预检各类 blocker 数量。

### 告警

- 一个 Master 检测到多个正式版本或多个开放 Revision。
- Master 正式指针与平台 ACTIVE 引用不一致。
- ACTIVE Checkout 超过批准时长，只告警，不自动解锁。
- 发布失败或发布事务后新旧指针不一致。
- 版本事件、签名或发布快照写入失败。
- 迁移 postflight 出现任何非零阻塞项。

### 运行看板

文控管理页显示开放 Revision、ACTIVE 检出、审批中版本、发布失败和迁移阻塞摘要。看板只提供定位，不允许绕过正式动作直接改状态。

## Open Questions

本阶段口径已收口：文件编码只执行首尾空白删除和字母大写，不折叠内部空白或改写全角字符；ACTIVE Checkout 在测试环境超过 24 小时告警但不自动解锁；非当前正式来源由项目 OWNER 填写理由后直接进入后续正式审批，不增加预审批。

## Design Blockers

- 现有数据盘点未完成，无法确定维护窗口和回填批次规模。
- 现有审批、签名和平台生命周期是否与迁移映射一致仍需测试库只读盘点证明。
- 生产部署不属于当前范围；未来进入生产必须重新补充安全、备份恢复和发布评审。
