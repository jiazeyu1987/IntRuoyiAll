# 保存球囊扩张压力泵草稿 V15 系统异常修复

## Task Goal

修复在 MES 系统生产管理 > 工艺流程中保存“球囊扩张压力泵”草稿版本 V15 时提示“系统异常”的问题，并确保草稿保存只持久化当前草稿、不隐式提交发布，保存后仍可继续修改同一草稿版本。

## Milestones

- [x] 复现并定位保存异常的前后端根因。
- [x] 先补充失败回归测试并记录 RED 证据。
- [x] 实施最小正式修复，不引入 fallback、降级或吞异常。
- [x] 运行目标回归验证并记录 GREEN / REGRESSION 证据。
- [x] 修复草稿普通保存后被隐式提交发布导致不可继续编辑的问题。
- [x] 定位并修复 2026-07-26 用户复报的普通保存仍提示单条“系统异常”。
- [ ] 完成任务文档、验证报告和必要收尾。

## Expected Verification

- 后端目标 JUnit 或前端静态契约先 RED 后 GREEN。
- 受影响模块的最小回归验证通过。
- 草稿版本普通保存后不弹出“立即提交发布”确认，不调用提交发布流程；只有显式“提交发布”入口会进入发布流程。
- 如具备本地运行态和登录前置条件，再通过真实页面路径验证保存草稿版本 V15；若当前 48081 运行态不属于本任务源码，必须记录未加载原因，不得宣称真实页面已加载本次后端修复。

## Current Status

ready_for_closeout

## 经验门禁

- PowerShell / Git：命令不得使用 `&&`；提交或推送前必须检查 `git status --short --branch`、当前分支、remote 和 staged 文件清单。
- 前端静态契约隔离：若既有大契约或 `pnpm ts:check` 先失败在无关历史问题上，必须用任务专用最小静态契约证明当前保存行为 RED/GREEN。
- 前端保存链路重复错误提示：内部 API 保存若由外层统一 toast，必须传 `ignoreErrorMessage: true`，子组件 rethrow 前不得重复 `message.error`。
- 前端草稿保存与提交发布解耦：普通保存成功处理只能标记已保存和清理退出标记，不得弹立即提交发布确认或调用 submit-publish；提交发布必须由显式入口触发。
- 后端开发：不得用默认成功、空数据、catch 后吞异常来掩盖接口保存失败；缺少数据库、Redis、测试数据或运行配置时必须 fail fast。
- 后端草稿 BATCH 快照读写对称：DRAFT 草稿显式保存过 `batchUseConfigs.formBindings` 后，读取必须优先返回该草稿快照；PENDING_APPROVAL / READY_TO_PUBLISH 相邻场景仍按既有规则读取当前工序设置。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；已修复保存失败三层重复提示、普通保存后隐式提交发布耦合，以及 DRAFT 草稿 BATCH 表单绑定快照保存后读回仍被当前工序设置覆盖的问题。后端通过显式快照标记保持草稿读写对称，同时保留待审批/待发布版本读取当前工序设置的既有规则。
- `是否存在临时补丁或绕过`：否。

## Closeout Blocker

- 当前工作区 `int_main...origin/int_main [ahead 20]` 且存在大量并行脏改动；按项目提交/推送门禁，完成提交前需要先处理或基线提交这些非本任务改动，当前未执行提交/推送。
- Cleanup preview/apply 已在本次复报后端修复文档更新后复跑通过，无删除项、无阻塞、无警告；任务仍未标记 completed，因为提交/推送门禁尚未解除。
- 当前 48081 后端运行态可能来自其他 worktree 或旧 Jar；本次未停止或重启非本任务运行态，因此不能宣称页面运行态已加载本次后端修复。
- 2026-07-26 用户要求 int_main 真实 E2E 时，前置检查确认 48081 被 `D:\IntRuoyiWorktree\edhr-release-dossier-e2e-20260726` 下的 Jar 占用；按当前端口规则已 fail fast，未登录、未写入、未冒充 int_main 验证通过。
- 2026-07-26 用户要求恢复 int_main 后端时再次确认 PID 57744 仍占用 48081；该进程属于非本任务 worktree，按规则不能强停。恢复动作需等待所属任务停止/迁移该进程后才能继续。

## Cleanup Keep

- doc/tasks/20260726-route-flow-v15-save-system-exception/bug-regression-evidence.md
