# 批次执行已发布工艺路线快照运行态更新

## Task Goal

确保 eDHR 创建批次执行时仅使用创建时最新已发布（ACTIVE）的工艺路线版本，并持久化冻结路线快照；运行态不得再读取当前草稿配置。

## Milestones

- [x] 核查已实现的发布版本快照和冻结任务生成逻辑
- [x] 验证目标回归测试及当前本地运行态版本
- [x] 在隔离且可确认归属的运行环境中打包并更新后端
- [x] 通过真实创建批次路径验证草稿变更不影响新批次
- [ ] 完成验证、清理和收尾

## Expected Verification

- `openOrCreate` 在创建批次对象时写入 ACTIVE 路线版本 ID、版本号和路线快照。
- 批次任务从该批次的冻结路线快照解析，不读取当前草稿配置。
- 已创建批次保留创建时的路线版本和快照，不受后续草稿修改影响。
- 本地运行态更新前确认 `48081` 监听进程和工作区改动归属，避免部署其他并行任务改动。

## Current Status

ready_for_closeout

## 经验门禁

- Trigger: 本地后端重启、`48081`、eDHR 真实路径验证。
- Preflight check: 读取 `docs/local-runtime.md` 和 `docs/worktree-restrictions.md`，确认端口监听 PID、进程命令行、工作区归属和前端根目录。
- Blocker: `48081` 被无法确认归属的进程占用，或当前构建输入包含其他并行任务未提交改动时，不得停止进程、不得重启、不得换端口；2026-07-24 该阻塞已通过可确认 PID 和隔离构建 Jar 解除。
- Verification: 定向冻结快照回归通过；已加载隔离构建 Jar，`http://127.0.0.1:48081/actuator/health` 返回 `{"status":"UP"}`，真实创建批次路径通过。
- Forbidden action: 不强杀 PID `39264`，不从当前脏工作区构建并部署其他任务改动。
- Evidence: `docs/local-runtime.md`、`docs/worktree-restrictions.md`、本任务 `verification-report.md`。

## E2E Data Boundary

- 入口：仅 `http://127.0.0.1:8081`。
- 身份：必须在执行写入前确认测试租户和测试账号；不得假定默认 `芋道源码/admin` 是可写测试身份。
- 数据：新建数据必须带本任务标识；本次使用测试租户工单 `925555 / TESTERPA9ED2D417434` 与批次 `BRS20260724195134`，不得使用工单 `881MO090935` 或其他真实业务工单。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否
- `是否从根因和长期维护角度解决`：是，批次创建显式持久化 ACTIVE 路线快照，任务生成只读该快照。
- `是否存在临时补丁或绕过`：否

## Cleanup Keep

- `doc/tasks/20260724-batch-execution-published-route-runtime-update/bug-regression-evidence.md`
- `doc/tasks/20260724-batch-execution-published-route-runtime-update/real-e2e-evidence.md`

## Closeout Blockers

- 2026-07-24 cleanup preview：主工作区仍包含其他任务脏改动；隔离 worktree 分支 `e2e/batch-route-snapshot-20260724` 当前不能 fast-forward 合并到 `int_main`，且 worktree 中存在非本任务改动 `MesProRouteFlowConfigServiceImpl.java`，因此暂不执行自动合并、删除 worktree 或提交无关文件。
