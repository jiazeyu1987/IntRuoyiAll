# DCC 严重缺陷修复验证报告

## 结论

AC-01..AC-18 的生产实现、定向测试和相关模块回归均通过；三份数据库迁移已通过政策门禁并在本地运行库验证。全后端构建、分支运行健康检查、前端类型检查和 8 项 DCC 静态合同通过。

真实 Playwright 没有被伪造成完整发布成功：当前测试租户不存在一条同时具备正式文件类型分类树绑定和有效审批路线的标准类别，脚本在任何上传/提交写请求前明确阻断，`writeRequests=[]`。这证明新前置门禁生效，但完整发布成功路径仍有环境数据前置未满足。

## 逐项验收

| AC | 结果 | 核心实现证据 | 自动化证据 |
| --- | --- | --- | --- |
| AC-01 | PASS | 清理前检查全部正式 artifact 引用；正式源所有权隔离 | `DccUploadTicketServiceTest`、`DccControlledFileTemporaryCleanupControllerTest`、source ownership/migration tests |
| AC-02 | PASS | 详情/列表/版本/预览统一检查 `infra_file` 与对象可读性，前端仅消费能力投影 | `DccControlledFileQueryServiceTest`、`DccControlledFilePreviewProtectionTest`、preview frontend contracts |
| AC-03 | PASS | 路线、会签、转办和加签统一使用岗位参与人验证器 | `DccApprovalParticipantPostValidatorTest`、assignee resolver/workflow tests |
| AC-04 | PASS | 独立错误码 1080000199，消息“审批人未配置系统岗位”，签名前失败 | participant validator、workflow、HTTP contract tests；前端错误映射合同 |
| AC-05 | PASS | 聚合路线 readiness 同时验证岗位、阶段权限、签名授权和签名图片 | `DccControlledFileRouteReadinessServiceTest`、workflow tests、readiness frontend contract |
| AC-06 | PASS | 摘要、逐条验证和导出共用完整证据验证结果与稳定原因 | `DccElectronicSignatureManagementServiceTest`、signature service/finalization tests |
| AC-07 | PASS | 发布事件将全部签名绑定受控 PDF fileId/SHA-256，篡改阻断导出 | `DccControlledFileSignatureBindingServiceTest`、`DccSignatureBindingSchemaTest`、publication/finalization tests |
| AC-08 | PASS | 有效电子分发收件人纳入详情、预览、下载授权；纸质/回收/跨租户不授权 | `DccControlledFileQueryServiceTest` 分发矩阵 |
| AC-09 | PASS | 预上传前校验类别存在、启用、生命周期和租户可用性，失败零存储副作用 | `DccControlledFileUploadApiTest`、`DccUploadTicketServiceTest` |
| AC-10 | PASS | upload-preview/status/cleanup 强制显式正整数且匹配认证租户的 `tenant-id` | `DccExplicitTenantRequestValidatorTest`、`DccUploadEndpointHttpContractTest`、frontend header contract |
| AC-11 | PASS | 租户/用户/session/purpose 活动槽位唯一；相同内容复用，不同内容 409 | `DccUploadTicketServiceTest`、`DccUploadSlotSchemaTest` |
| AC-12 | PASS | `ACTIVE -> CLEANING -> CLEANED` CAS；物理删除与状态恢复可重试 | `DccUploadTicketServiceTest`、temporary cleanup controller test |
| AC-13 | PASS | 目标 DCC 端点将认证、权限、参数、404、409、500 映射真实 HTTP 状态 | `DccApiHttpContractFilterTest`、upload endpoint HTTP contract、frontend Axios contract |
| AC-14 | PASS | 通用上传在存储前拒绝危险扩展及 DOS/PE 内容伪装，返回 HTTP 400 | `FileUploadSecurityPolicyTest`、`FileControllerTest`、`InfraFileUploadHttpContractFilterTest` 17/17 |
| AC-15 | PASS | 最终批准弹窗和服务端一次聚合盖章 PDF、目录、培训与分发缺项 | workflow/finalization tests、readiness frontend contract |
| AC-16 | PASS | 当前阶段 snapshot 与 Flowable assignee/candidate 双重校验；漂移不越权 | `DccApprovalTaskAdapterTest`、workflow/route readiness tests |
| AC-17 | PASS | 提交前解析提交人部门/负责人，明确组织映射 blocker 且零正式副作用 | route readiness/workflow tests；真实 Playwright 返回 `SUBMITTER_ORG_MAPPING_INVALID` |
| AC-18 | PASS | 新正式记录独占 source；raw fileId 先复制验 hash；历史共享源可重入迁移 | ownership/migration/mapper/schema tests，三份正式 SQL 中的 ownership migration |

## 回归汇总

- DCC：排除在 `int_main` 精确复现的五个既有失败类后，163 份报告、1165 tests、0 failures、0 errors。
- infra 任务范围：17 tests、0 failures、0 errors。
- 前端：`pnpm ts:check` PASS；8/8 相关静态合同 PASS。
- 后端构建：30 个 Maven reactor module 全部 SUCCESS。
- 运行态：48094 backend health `UP`；8094 frontend HTTP 200；验证后已释放端口。
- 数据库：三份迁移已执行；活动上传槽位重复组为 0，ownership/signature binding/upload slot 目标结构均存在。
- 代码卫生：`git diff --check` PASS。
- 技能证据：backend API、database schema、frontend feature 三个 evidence validator 均 PASS；关键结论已归档到本报告，临时 evidence 可安全清理。

## 已知基线与残余阻塞

- DCC 五个既有失败类、infra 四个 runtime ops 失败、前端一个 access-rule 静态合同失败均已在未含本任务改动的 `int_main` 精确复现，不是本任务回归。
- 真实完整发布成功路径缺少测试租户正式数据前置：至少一条标准类别必须同时绑定文件类型分类树和有效审批路线，并补齐路线所需部门负责人/岗位数据。按照 fail-fast 与真实 E2E 规则，本任务未直接改库或伪造基线绕过。
- 本次真实 E2E 在阻断前没有业务写请求，不需要回收业务测试数据。

## 主线集成基线复验

- 集成基线：`int_main` 提交 `8f82cea4b`，任务提交 `5932be504` 线性位于其后；任务路径与该期间主线已提交路径无交集。
- DCC：本轮新生成 163 份 Surefire 报告，1165 tests、0 failures、0 errors、0 skipped。
- infra：任务范围 17 tests、0 failures、0 errors、0 skipped。
- 前端：`pnpm ts:check` PASS；8/8 DCC 静态合同 PASS。
- 融合判定：代码与核心任务记录具备 `ff-only` 条件；主工作区并发 dirty 内容不在任务提交中，两份长期经验文档按独立 hunk 保留并发内容后落位。

## 最终收尾

- `int_main` 已通过 `ff-only` 融合至 `688afee83`；未 push。
- 任务 worktree Git 注册和物理目录均已删除，8094/48094 无监听，slot 13 登记已释放。
- `task-closeout-cleanup` preview/apply 均通过，最终仅保留三份核心任务记录。
- 主工作区其它任务的 dirty 内容未被本任务暂存、提交、删除或覆盖。
- 最终判定：任务实现与可执行验证完成；真实完整发布成功路径的测试租户数据前置仍需后续正式配置后复验，当前阻断证据不能替代成功路径 PASS。
