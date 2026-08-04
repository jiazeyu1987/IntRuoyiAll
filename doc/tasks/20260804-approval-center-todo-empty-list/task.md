# 审批中心待办列表空态修复

## Task Goal

修复审批中心待办页签中左侧徽标显示存在待办数量，但列表区域显示为空或数量为 0 的问题，确保待办列表首屏、筛选状态和待办徽标使用同一正式聚合口径。

## Milestones

- [x] 创建任务记录并记录既有脏工作区基线提交。
- [x] 复现并用回归测试锁定“总数大于 0 时列表不得为空 / 隐藏筛选不得导致空态”的缺陷。
- [x] 从根因修复审批中心待办分页、筛选和模块加载链路。
- [x] 运行定向 RED/GREEN 和相关回归验证。
- [ ] 完成 cleanup、收尾提交和推送；当前因共享分支存在其它任务未推送提交，需先确认推送边界。

## Expected Verification

- 后端定向 JUnit：`mvn.cmd -pl yudao-module-bpm -am "-Dtest=ApprovalCenterServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`。
- 前端静态合同：`node tests/e2e/approval-center-route-filter-visible-static.spec.js`。
- 前端相邻回归：`approval-center-pagination-preserve-page-static.spec.js`、`approval-center-fill-list-area-static.spec.js`、`approval-center-pagination-event-payload-static.spec.js`。
- 证据收口：`bug-regression-evidence.md` 通过 validator，并将可保留结论复制到 `verification-report.md`。

## Current Status

blocked

实现、定向验证、cleanup 和经验沉淀已完成。本任务代码和初始任务文档被共享分支基线提交 `1bd808f30 chore: baseline int_main remaining before rrm M6 merge` 带入 HEAD，该提交同时包含其它任务文件，未做历史重写或回滚。为避开主工作区并行改动和 Maven target 卡顿，已创建 task-owned detached verification worktree `D:\IntRuoyiWorktree\approval-center-todo-verify-20260804`，在干净目录通过目标 BPM JUnit 后删除，`Test-Path=False`。最终收尾提交/推送被阻塞：当前 `origin/int_main..HEAD` 包含其它任务 ahead 提交 `b59f5baf4` 和 `e9388400e`，本任务不能擅自推送 unrelated commits。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。后端 provider 首屏 total/list 不一致时 fail fast；前端模块加载失败继续抛出，不再静默覆盖为 0 个模块。
- `是否从根因和长期维护角度解决`：是。修复 provider 分页一致性、URL 查询状态与可见快速筛选控件同步、模块加载错误传播。
- `是否存在临时补丁或绕过`：否。临时 detached worktree 仅用于隔离验证，已删除。

## Experience Gate

- 适用 `docs/frontend-development.md#前端静态契约隔离门禁`：本任务用专用静态合同覆盖隐藏路由筛选导致空列表的 RED/GREEN。
- 适用 `docs/frontend-development.md#前端服务端分页排序链路门禁` 的分页原则：服务端分页列表不得只修当前页视觉状态，必须保证分页请求和后端聚合结果一致。
- 适用 `docs/powershell-memory.md#共享分支并发基线提交门禁`：当前任务文件被 `1bd808f30` 混入共享基线提交，后续不得 amend/reset，只能记录事实并选择性处理本任务收尾文件。
- 适用 `docs/worktree-memory.md#主工作区-maven-target-冲突时的隔离验证-worktree-门禁`：主工作区 Maven/Javac 卡顿后，用 task-owned detached worktree 做干净 JUnit 验证，不启动服务、不登记端口，验证后删除。
