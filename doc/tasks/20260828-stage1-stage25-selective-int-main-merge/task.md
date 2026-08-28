# Stage1 / Stage2.5 选择性融合到 int_main

## Task Goal

只从 `D:\IntRuoyiWorktree\stage25-stage5-stage6-e2e-20260826` 抽取与 stage1、stage2.5 直接相关的代码和测试，融合到 `E:\IntRuoyi` 的 `int_main`，明确排除 stage4、stage5、stage6、DCC、BPM、系统登录等其它差异。

## Milestones

- [x] 读取后端、PowerShell/Git、worktree、任务收尾和编码规则。
- [x] 核对主干目标路径是否存在同文件未提交冲突。
- [x] 融合 stage1/stage2.5 最小后端代码与合同测试。
- [x] 运行 stage1/stage2.5 定向验证。
- [x] 记录最终状态和剩余阻塞。

## Expected Verification

- `git diff --name-status -- <selected paths>` 只显示 stage1/stage2.5 相关路径。
- Stage2.5 静态合同通过。
- Stage2.5 后端合同测试通过；若 Maven 环境阻塞，记录精确失败和影响。

## Current Status

ready_for_closeout

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，stage2.5 继续走正式完成回填与 Flow6 handoff，不恢复旧 dossier writer。
- 是否存在临时补丁或绕过：否。
