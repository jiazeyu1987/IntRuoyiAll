# 通用检验规程与产品绑定开发文档

## Task Goal

为 QA 建立可复用的通用检验规程版本，并维护“正式产品 -> 当前有效通用规程版本”的唯一绑定关系。一个通用规程版本可被多个产品复用；同一产品在同一时间只能有一个启用绑定。本文档只覆盖后台维护与绑定，不覆盖活跃订单冻结和一线 PQC 执行改造。

## Milestones

- [x] 明确范围、业务术语、边界和不变量
- [x] 形成产品需求、用户流程、验收标准
- [x] 形成后端 API、数据模型、前端设计和权限设计
- [x] 形成 BDD/TDD、测试数据和验证命令
- [x] 使用真实 Word 通用包装规程样本补充 A/B 验证口径
- [x] 独立子任务审核文档
- [x] 根据审核意见修订并复审，直到放行

## Expected Verification

- 产品需求、系统设计、验收文档结构校验通过。
- 独立 reviewer 返回 `final_decision=pass`，且无逻辑、易用性或 UI 阻塞项。
- 文档中所有未决问题均已列为 Open Questions/Design Blockers，不以默认值掩盖。

## Current Status

blocked：qa_extra_2026 worktree 内开发验证文档已补齐，用户指定 A/B Word 源路径已完成直接读取、哈希一致性、表 2 解析和文档门禁验证，任务目录 cleanup 已应用；但主工作区 `E:\IntRuoyi` 当前存在无关脏改动且包含 `docs/backend-development.md` 同名文件改动，按 worktree closeout 规则不能执行 ff-only 合并或删除本 worktree。当前分支提交/推送可保存本任务证据，但不能标记 completed。

## 设计约束检查

- 不引入 fallback、按名称猜产品、自动选择最新版本或隐式切换规程。
- 产品绑定主键使用正式 `productId`，不使用产品名称、代际文本或编码前缀推断。
- 已发布版本不可原地修改；修改必须复制为新版本。
- 同一通用版本允许由多份受控 Word 规程组成，版本内容必须保存来源文件、包装阶段和来源行证据。
- 同一产品只能有一个当前有效绑定；历史绑定和历史执行快照不被覆盖。
- 本任务只写开发验证文档，不修改生产代码、数据库、权限数据或运行环境。

## Cleanup Keep

- doc/tasks/20260908-common-qa-regulation-product-binding/task.md
- doc/tasks/20260908-common-qa-regulation-product-binding/execution-log.md
- doc/tasks/20260908-common-qa-regulation-product-binding/verification-report.md
- doc/tasks/20260908-common-qa-regulation-product-binding/prd.md
- doc/tasks/20260908-common-qa-regulation-product-binding/user-flows.md
- doc/tasks/20260908-common-qa-regulation-product-binding/acceptance-criteria.md
- doc/tasks/20260908-common-qa-regulation-product-binding/backend-api-design.md
- doc/tasks/20260908-common-qa-regulation-product-binding/data-model.md
- doc/tasks/20260908-common-qa-regulation-product-binding/frontend-design.md
- doc/tasks/20260908-common-qa-regulation-product-binding/bdd-scenarios.md
- doc/tasks/20260908-common-qa-regulation-product-binding/tdd-plan.md
- doc/tasks/20260908-common-qa-regulation-product-binding/source-docx-validation.md

## Cleanup Candidates

- doc/tasks/20260908-common-qa-regulation-product-binding/source-docx/

## Closeout Blockers

- `E:\IntRuoyi` 主工作区存在其它任务脏改动，且包含本任务同名经验文档 `docs/backend-development.md` 改动；在主工作区清理或合并前，禁止自动 ff-only 合并 `codex/qa_extra_2026`，禁止删除 `D:\IntRuoyiWorktree\qa_extra_2026`。
