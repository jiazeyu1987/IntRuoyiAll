# 工序池 Open Questions / Blockers 初始定义任务

## Task Goal

基于当前系统业务和本次线程已确认需求，为生产一线报工工序池文档中的 Open Questions / Blockers 给出第一版可执行初始定义，减少后续设计和开发的悬空项，同时保留仍需业务最终确认的边界。

## Milestones

- [x] 读取项目构想、BDD/TDD 和任务/编码门禁。
- [x] 建立任务文档并记录当前工作区状态。
- [x] 梳理当前 Open Questions / Blockers。
- [x] 写入初始定义、默认阻塞策略和仍需确认边界。
- [x] 同步 acceptance 文档中的测试阻塞说明。
- [x] 运行文档结构、UTF-8 和关键定义验证。
- [x] 更新验证报告和任务状态。

## Expected Verification

- `python C:\Users\BJB110\.codex\skills\project-inception-docs\scripts\validate_inception_docs.py --root E:\IntRuoyi`
- `python C:\Users\BJB110\.codex\skills\bdd-tdd-acceptance-planner\scripts\validate_acceptance_plan.py --root E:\IntRuoyi`
- UTF-8 读取修改过的 Markdown 成功。
- 关键初始定义搜索确认覆盖：工序池维度、PQC 失败规则、生产工单 `plannedStartTime` 空值、班组长负责范围、异常上报、上下限规则、原始记录修改、测试数据前置。
- `git diff --check` 对本次文档范围通过。

## Applicable Gates

- 本任务只修改需求/验收文档，不修改生产代码、数据库、运行态或测试数据。
- 初始定义必须来自当前系统业务和本次线程需求，不能编造已确认事实；未确定项必须标注为“初始定义/待业务确认”。
- 不引入 fallback、默认成功、默认审核、默认员工、默认设备或静默降级；缺少正式前置条件仍按 blocker 处理。
- 批记录表单、表单槽位 `formBindings`、工序开始配置三条链路不得混用。
- 当前工作区已有大量并行脏改动，本任务不得回滚、覆盖或删除无关改动。

## Current Status

ready_for_closeout

## Closeout Notes

- 初始定义已写入 inception、evidence 和 acceptance 文档。
- 验证已通过；cleanup preview/apply 已通过，删除项为空，保留 `task.md`、`execution-log.md`、`verification-report.md`。
- Git 提交/推送暂不执行：当前工作区存在大量本任务外脏改动，不能把无关并行任务混入本任务提交。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，目标是把已悬空问题转成明确初始口径和阻塞条件。
- `是否存在临时补丁或绕过`：否。
