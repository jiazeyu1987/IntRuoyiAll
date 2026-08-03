# Execution Log

## User Intent

- 用户反馈：`eDHR 批次执行缺少工艺流程批记录配置流程配置或默认批记录`。
- 初始判断：问题命中 eDHR 批次执行的正式批记录配置来源链路，需用 BDD + RED/GREEN 修复，禁止用 `formBindings`、默认 `MAIN`、工序开始配置或前端文案替代正式批记录绑定。

## Rule And Experience Reads

- Read: `C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\SKILL.md`
- Read: `C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\references\bug-contract.md`
- Read: `docs\task-closeout-rules.md`
- Read: `docs\backend-development.md`
- Read: `docs\frontend-development.md`
- Read: `docs\e2e-rules.md`
- Read: `docs\experience-index.md`
- Applicable gate: 工艺路线三类配置术语契约；eDHR 批次任务配置来源门禁；eDHR 管理员主区域已提交内容门禁。

## BDD

- BDD: 历史批次提交内容读取使用正式批记录配置 -> Given 历史 eDHR 批次的冻结快照缺少完整 `flowGraph.nodes` 或 `batchUseConfigs` 但当前 BATCH 工序配置完整覆盖任务工序, When 批记录管理员读取 `review-timeline` 或批次详情提交内容, Then 后端应使用正式当前 BATCH 配置恢复批记录任务上下文并返回可读内容, And 不得用 `formBindings`、默认 `MAIN`、工序开始配置或空绑定替代正式逐工序批记录表单。
- BDD: 正式批记录配置确实缺失时 fail fast -> Given 冻结快照和当前 BATCH 工序配置都无法完整覆盖批次任务工序, When 批次执行读取任务上下文, Then 后端应返回明确缺失配置错误, And 不得静默返回空任务、默认成功或 mock 内容。

## Command Log

- Command intent: `git status --short --branch` -> observed existing unrelated dirty files before this task; will preserve and avoid unrelated edits.
- Command intent: `rg` search for target error/config keys -> located backend error constant, `MesProEdhrBatchExecutionServiceImpl`, existing `MesProEdhrBatchExecutionServiceTest`, and related eDHR gates.
