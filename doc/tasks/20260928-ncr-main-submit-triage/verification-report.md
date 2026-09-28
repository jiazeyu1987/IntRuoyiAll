# 提交甄别结果

- 基线 HEAD：0ece51c1cdb000986c23c0ef72ebe5c31db4d8e7；本地相对已有 origin/int_main 跟踪引用领先两个提交，未在本轮 fetch，非远端实时同步结论。
- 800b93a91 与 0ece51c1c 属于 ToDo_反查 的反查开发/融合，不能作为 NCR 实现提交。
- 原三份 UU 测试由 ToDo_反查 与 主流程优化 协调恢复并登记；本轮 git ls-files -u 为空。本任务未重复处理其索引。
- 现有五个暂存文件为上述恢复产物，保留原状。
- 主流程优化任务仍 in_progress；其 red-wave8.log 实际编译失败：MesProEdhrFrozenProductionWriterReaderR3Test 调用 readSourceSnapshotHash，现行接口已变更。所属任务正在协调修复。
- NCR 服务直接注入 MesActiveOrderReworkCycleService 并在返工调用 start；后者属于主流程优化的返工周期修改。不能只按 NCR 文件名独立提交当前完整文件，否则遗漏依赖；一并纳入则会发布尚在修复中的并行任务。
- 本轮不执行编译，避免与该任务共享 Maven target 并发；历史 61/61、7/7 和 ts:check 不是当前混合源码验证证据。
- 旧 NCR 全量 E2E 仍未执行，见 doc/tasks/20260927-ncr-three-findings/execution-log.md。

## 文件处理

file-classification.tsv 为初筛清单，明确标注“候选”不代表已审核可提交。四类处理：NCR及关联依赖待联合验证；其他主流程源码交原任务完成；AOCI/规则/审计文档保留归原任务；临时文件删除、备份、演示文件不纳入本次提交、不删除实体。

## 恢复提交的条件

1. 主流程优化完成共享源码修复，最新编译/定向回归通过，并提供稳定版本。
2. 按 diff 核验 NCR 与返工依赖整套文件，保留其他任务暂存内容，不使用 git add -A。
3. 核对原任务剩余验收条件后再提交推送。用户授权仍有效，无需重复请求 Git 授权。

## 经验核对

已使用 project-experience-consolidation，读取 docs/worktree-memory.md 的共享目录分类/选择性提交规则（约418行）及并行 dirty 保护（约440行），本次情形已有覆盖，无需新建长期经验或改写并行任务文档。

## 最终核验

本轮只完成甄别并记录；没有提交或推送，没有修改业务代码、索引或运行服务。
## 用户再次要求立即提交推送
用户已获知未完成验证、并行修改和依赖风险，仍再次明确要求“提交和推送”。本轮按当前业务代码快照提交，不声明业务已验收，不部署、不执行SQL、不更改其他任务状态。此前的等待完整业务验收再提交改为记录真实验证限制后提交快照；业务验收要求仍保留在原任务。

选择范围：MES源码与测试、前端源码与测试、五份相关前向SQL（详见commit-paths.txt）。包括完整主流程依赖，排除临时删除、AGENTS/规则/AOCI、演示、备份及其他文档。既有暂存docs/backend-development.md保留，不纳入。

CHECK：未合并索引为空；选定跟踪文件 git diff --check HEAD 通过。最新编译失败后的测试适配已由原任务落盘，尚无本轮整体GREEN，不以旧测试结果替代。快照不代表可发布版本。
## 提交推送结果
- 业务快照提交：8c50567aa（102个业务/测试/SQL文件及4个任务记录，共106文件）。完整文件列表见commit-paths.txt，Git提交为准确版本。
- git push origin int_main -> PASS，远端从840b9ef0d推进至8c50567aa；同时包含原有反查两个提交。
- Git提交/推送端口钩子均通过。git diff --cached --check通过。
- 原暂存docs/backend-development.md保持暂存且未纳入；其余排除项保持工作区原状，未清理并行文件。
- 本轮未运行构建、E2E、SQL或部署。业务验证仍在原任务进行，不能视为全量验证通过。