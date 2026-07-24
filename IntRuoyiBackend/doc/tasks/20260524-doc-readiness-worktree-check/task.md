# 20260524 doc readiness worktree check

## 任务目标

- 在独立 worktree 中检查正式发布相关文档是否齐全。
- 检查是否可以仅依赖新 worktree 内的文件完成发布门禁 review。
- 不执行正式发布、不触发回滚/恢复、不发送真实通知。

## 里程碑

1. 确认新 worktree 分支和工作区状态。
2. 运行现有文档契约测试。
3. 检查 root-level 发布文档、服务仓库文档和任务证据是否齐备。
4. 输出 verification report，给出 PASS/BLOCKED。

## 预期验证

- 新 worktree 中 `test_release_go_no_go_contract_docs.py` 可运行。
- 必需文档清单逐项有证据路径。
- 若 root-level `docs/` 不在 worktree 中，必须区分“可通过真实 docs root 检查”和“worktree 自包含文档包缺失”。

## 当前状态

- 状态：completed
- 当前阶段：新 worktree 文档齐备性检查完成。
- 结论：发布文档契约测试已支持 `DOCS_ROOT` 和向上查找真实文档根目录，并在新 backend worktree 内通过；worktree 根目录已建立 `docs` junction 指向主根文档目录，acceptance validator 已通过。该入口不是独立复制件，若要求完全离线自包含副本，仍需另行制作快照。
