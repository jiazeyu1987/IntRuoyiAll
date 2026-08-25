# 流程1-11代码融合到 xiufu20260826

## 任务目标

以当前 `int_main` 提交 `2faf0f332` 为基线，在 `D:/IntRuoyiWorktree/xiufu20260826` 中选择性融合流程1-11尚未进入主干的代码提交。已经进入 `int_main` 的代码不重复融合；旧设计分支、历史文档和无关并行改动不纳入。

## 里程碑

1. 核对目标 worktree、主线基线和流程分支提交关系。
2. 为每个流程建立候选提交、已在主线提交和排除提交清单。
3. 选择性融合未进入主线的流程代码，处理冲突并保留冲突证据。
4. 运行后端编译、流程定向测试、差异检查和 branch runtime guard。
5. 记录融合提交、文件清单、剩余阻塞和最终状态。

## 预期验证

- `git status --short --branch`
- `git diff --check`
- `git log --oneline --decorate`
- 流程相关 Maven compile 和定向测试
- `scripts/preflight/branch-runtime-port-guard.ps1`

## Current Status

in_progress

代码已经选择性应用并提交，当前 HEAD 为 `9c696db6d45ef3dda1a64e262c14cda2934ae106`；已通过定向编译/测试、独立后端启动和 runtime guard。真实数据库迁移、Tx-C outbox、写入型 E2E 和主干融合仍未完成。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否；本任务只做 Git 选择性融合，不修改业务降级策略。
- 是否从根因和长期维护角度解决：是；以提交祖先关系和文件归属为依据，避免重复融合历史分支。
- 是否存在临时补丁或绕过：否；不使用 `--no-verify`、不修改 hook、不清理其它 worktree。

## 范围边界

- 不修改 `E:/IntRuoyi` 主工作树。
- 不整体提交目标 worktree 原有改动；目标基线初始应保持干净。
- 不自动融合仅有文档的设计分支。
- 融合成功不等于流程1-11全链路验收完成；每个未解除的业务或环境阻塞必须保留。
