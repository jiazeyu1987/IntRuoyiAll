# 20260726-route-start-batch-record-attachments

## Task Goal

在工艺路线流程图“工序开始”节点左侧增加固定页签“批记录附件”，右侧展示 4 个批记录附件负责人配置，并为 4 个默认上传角色提供当前租户启用用户范围内的幂等初始化能力。

## Milestones

- [x] 建立 BDD/TDD 任务证据与最小 RED 测试。
- [x] 实现后端角色初始化、租户启用用户筛选、路线级附件负责人配置保存/读取。
- [x] 实现前端“工序开始”节点“批记录附件”入口与 4 项负责人配置 UI。
- [x] 运行目标后端、前端静态合同与回归验证。
- [ ] 完成收尾文档、经验沉淀与提交推送。

## Expected Verification

- 后端目标测试覆盖角色创建、当前租户启用用户过滤、2-4 人授权、少于 2 人失败、配置保存/读取。
- 前端静态合同覆盖“批记录附件”仅出现在“工序开始”节点，且 4 个默认配置项和角色名称正确。
- 相关 Maven/Node 目标验证通过；若环境前置缺失，记录 fail-fast blocker。

## Current Status

blocked_e2e_credentials

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，按路线级配置与租户级角色授权建模，不复用工序级临时字段。
- `是否存在临时补丁或绕过`：否。

## 经验门禁

- 前端静态契约隔离门禁：本任务需要新增聚焦静态合同，若全量检查先失败于无关历史问题，必须记录无关 blocker 并以任务专用合同证明当前行为 RED/GREEN。
- 后端 eDHR 批次任务配置来源门禁：涉及路线发布快照与批记录配置时，不得把发布快照作为通用 fallback；必须明确当前配置与发布快照边界。
- 数据库门禁：涉及 schema 或迁移时必须以现有迁移/DO/Mapper 证据核对字段，不凭记忆写 SQL。
- PowerShell 门禁：含中文内容的文档和脚本读写必须使用 UTF-8；Maven `-D` 参数在 PowerShell 中整体加双引号。
- 收尾门禁：实现和验证完成后先标记 `ready_for_closeout`，再执行 cleanup preview/apply，最后才能标记 `completed`。

## Closeout Blocker

- 当前工作区存在多个非本任务脏改动和 `int_main...origin/int_main [ahead 20]`，包括 `.runtime/`、`system/codex-test-management`、其它 `doc/tasks/*` 与 `docs/frontend-development.md` 等并行任务文件。
- 为避免混入或提交并行任务改动，本任务已完成实现与验证，但未执行 cleanup apply、实现提交、收尾提交和 push；状态保持 `ready_for_closeout`。

## Runtime Blocker

- 用户复现 `请求地址不存在: admin-api/mes/pro/route/flow-config/batch-record-attachment-owners`。
- 本项目 `E:\IntRuoyi\IntRuoyiFronted` 的 `8081` 前端通过 `.env.local` 代理到 `http://127.0.0.1:48081`。
- 当前监听 `48081` 的后端不是 `E:\IntRuoyi\IntRuoyiBackend`，而是 `D:\IntRuoyiWorktree\codex-test-run-monitor-runtime\IntRuoyiBackend\yudao-server\target\yudao-server-exec.jar`，PID `53560`。
- 该 worktree 的 `MesProRouteFlowConfigController.java` 只有基础流程配置、排产保存和批记录保存接口，不包含 `batch-record-attachment-owners` 三个新增映射。
- 按本地运行态规则，`48081` 应归属 `E:\IntRuoyi` 的 `int_main` 后端；当前端口被其他 worktree Jar 占用，不能静默强停或改端口，需要用户确认后停止 PID `53560` 并加载本项目后端 Jar。

## 2026-07-26 E2E Retry Status

- 已创建任务专用隔离 worktree：`D:\IntRuoyiWorktree\route-start-batch-record-attachments-e2e`，端口 `8087/48087`。
- 已将修复后 Jar 加载到任务专用后端 `48087`，当前健康检查 `UP`，前端 `8087` 返回 HTTP 200。
- 真实 Playwright E2E 已启动并调用登录接口，但 `测试租户/aoteman` 使用本地默认密码来源返回“登录失败，账号密码不正确”。
- 当前阻塞前置：缺少本地可用的 `测试租户/aoteman` E2E 密码，或需要用户明确授权临时重置并恢复该本地测试账号密码。
