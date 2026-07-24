# 20260724 merge recordbook from jiluben

## Task Goal

将 `D:\IntRuoyiWorktree\jiluben_20260722_clean` 中与记录本相关的改动筛选并融合进 `int_main`，同时保留主分支现有未提交改动。

## Milestones

- [x] 建立任务记录并读取 worktree 限制
- [x] 检查 `int_main` 与来源 worktree Git 状态
- [x] 识别记录本相关变更边界
- [ ] 在不覆盖主分支未提交改动的前提下迁移记录本改动
- [ ] 运行后端/前端目标验证
- [ ] 收尾清理与经验沉淀

## Expected Verification

- `docs\worktree-restrictions.md` 已读取并记录。
- `int_main` 同文件脏改冲突清零后，再执行代码融合。
- 融合后运行记录本相关后端测试，例如记录本同步迁移契约、字段审计记录本模式测试、路线动态表单绑定相关测试。
- 融合后运行记录本相关前端静态/E2E 契约测试。
- 不覆盖、不回退、不暂存无关任务改动。

## Current Status

in_progress

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，计划按记录本功能边界筛选迁移，而不是整包覆盖。
- `是否存在临时补丁或绕过`：否。

## Experience Gate

- 用户在阻塞报告后明确回复“继续”，授权在记录风险并避免覆盖同文件并行改动的前提下继续选择性融合。
- `docs\experience-index.md` 已恢复可读，并命中 SQL、E2E、PowerShell 编码与 worktree 高风险门禁。
- 已读取 `docs\database-rules.md`，本任务 SQL 迁移只按现有迁移/测试夹具证据编写，不连接或修改真实数据库。
- 已读取 `docs\e2e-rules.md`，真实 E2E 仅在入口、租户、账号和运行服务前置条件确认后执行；否则只运行静态契约并记录阻塞。
- 已读取 `docs\powershell-encoding.md`，中文文档读写使用 UTF-8；PowerShell 命令不使用 `&&`。
- `docs\powershell-memory.md` 缺失；本任务采用当前可读的 `docs\powershell-encoding.md` 与项目 AGENTS PowerShell 规则作为执行门禁。
- 已读取 `docs\worktree-restrictions.md`，确认合并/清理 worktree 前必须执行该限制文件。

## Merge Boundary

- 来源 worktree 的大量改动是未提交工作区差异；本任务不整包导入，只迁移“记录本批记录受控同步”相关 SQL、模型字段、服务语义、前端类型/UI契约和对应测试。
- `int_main` 当前有未提交改动，且包含本次记录本融合高度可能触碰的同文件：
  - `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrBatchExecutionServiceImpl.java`
  - `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrWorkTaskServiceImpl.java`
  - `IntRuoyiFronted/src/api/mes/pro/edhr/batchExecution.ts`
  - `IntRuoyiFronted/src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue`
- 对这些同文件只做逐 hunk 比对和人工补丁，不执行整文件复制、stash、reset 或 checkout。
