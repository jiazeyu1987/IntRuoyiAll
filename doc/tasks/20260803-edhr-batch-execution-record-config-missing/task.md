# eDHR 批次执行批记录配置缺失修复

## Task Goal

修复 eDHR 批次执行读取历史批次/提交内容时出现 `eDHR 批次执行缺少工艺流程批记录配置流程配置或默认批记录` 的问题，确保正式批记录表单来源可追溯、配置缺失时 fail fast，不使用 `formBindings`、默认 `MAIN`、工序开始配置或前端文案替代正式逐工序批记录绑定。

## Milestones

- [ ] 定位触发该错误的后端配置读取链路和现有回归测试。
- [ ] 先补最小失败回归，证明缺失场景的正式预期。
- [ ] 实施最小后端修复，保持批记录表单、表单槽位、工序开始三类来源分离。
- [ ] 运行定向 GREEN 和相关回归验证。
- [ ] 记录 bug evidence、verification report，并按 closeout 规则收尾。

## Expected Verification

- `mvn -pl yudao-module-mes -am "-Dtest=MesProEdhrBatchExecutionServiceTest#<target>" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- 相关相邻 eDHR 批次任务配置来源回归。
- 如本机运行态、登录、夹具满足，再执行真实只读页面/E2E 验证；若缺前置，记录明确 blocker，不用 API-only 冒充通过。
- `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc/tasks/20260803-edhr-batch-execution-record-config-missing/bug-regression-evidence.md`

## Applicable Gates

- 工艺路线三类配置术语契约：`批记录表单` 只来自工序设置逐工序正式绑定；`formBindings` 只属于表单槽位；`工序开始` 只属于开始节点动作，三者不得互相替代。
- eDHR 批次任务配置来源门禁：当前 BATCH 工序配置存在时必须使用当前配置并校验绑定归属；当前配置整体缺失时才读取已发布冻结快照；禁止用发布快照作为通用 fallback 或用空绑定/默认 MAIN 掩盖损坏。
- eDHR 管理员主区域已提交内容门禁：历史样本 `review-timeline` 返回本任务错误时必须阻塞并修根因，不得用历史 execution 直连、API-only 或旧样本截图替代页面验证。

## Design Constraint Check

- `是否引入 fallback/降级/吞异常`：否；只允许正式配置来源选择，缺失正式来源必须显式报错。
- `是否从根因和长期维护角度解决`：是；修复目标为后端批次任务配置来源/快照恢复链路，保持数据来源可追溯。
- `是否存在临时补丁或绕过`：否；不使用前端硬编码、直接 SQL、mock、默认成功或静默兜底。

## Current Status

in_progress

- 已创建任务目录并读取 `docs/task-closeout-rules.md`、`docs/backend-development.md`、`docs/frontend-development.md`、`docs/e2e-rules.md`、`docs/experience-index.md` 和 bug-regression-fix-loop 技能门禁。
