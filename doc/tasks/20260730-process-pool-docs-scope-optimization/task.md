# 工序池文档口径优化任务

## Task Goal

优化本次线程沉淀的生产一线报工/工序池/批记录需求文档，消除“班组长生产工单可见范围”的口径冲突：员工提交按负责范围过滤，生产班组长和 PQC 班组长可查看所有生产工单用于异常标记和上报。

## Milestones

- [x] 读取任务、编码、项目构想、BDD/TDD 和独立验证门禁。
- [x] 识别现有文档冲突点和适用经验门禁。
- [x] 更新 inception 与 acceptance 文档，去除开放问题中的已确认事项。
- [x] 补充 BDD/TDD/E2E/测试数据断言。
- [x] 运行文档结构、UTF-8 和关键口径验证。
- [x] 更新验证报告和任务状态。

## Expected Verification

- `python C:\Users\BJB110\.codex\skills\project-inception-docs\scripts\validate_inception_docs.py --root E:\IntRuoyi`
- `python C:\Users\BJB110\.codex\skills\bdd-tdd-acceptance-planner\scripts\validate_acceptance_plan.py --root E:\IntRuoyi`
- UTF-8 读取修改过的 Markdown 成功。
- 关键口径搜索确认不再把“班组长是否能看所有生产工单”保留为开放问题。
- `git diff --check` 对本次文档范围通过。

## Applicable Gates

- 本任务只优化需求与验收文档，不修改生产代码、数据库、运行态或排产系统。
- 不引入 fallback、默认成功、默认审核、默认员工、默认设备或静默降级。
- 批记录表单、表单槽位 `formBindings`、工序开始配置三条链路不得混用。
- 员工提交列表按班组长负责范围过滤；生产工单列表按用户已确认口径对班组长可见全部生产工单。
- 现有工作区已有大量并行脏改动，本任务不得回滚、覆盖或删除无关改动；如进入提交阶段，必须按项目 Git 门禁先处理脏工作区基线或明确记录阻塞。

## Current Status

ready_for_closeout

## Closeout Notes

- Cleanup preview/apply 已通过，删除项为空，保留 `task.md`、`execution-log.md`、`verification-report.md`。
- Git 提交/推送未执行：当前 `int_main` 工作区在本任务开始前已有大量源码、测试、文档和其它任务目录脏改动；为了不把无关并行改动混入本次文档优化提交，本任务停留在 `ready_for_closeout`。
- 后续若需要提交，需先按项目 Git 门禁处理既有脏工作区基线，或在可区分 hunks 后只暂存本任务文档范围。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，直接修正文档事实口径和验收断言，不用开放问题掩盖已确认需求。
- `是否存在临时补丁或绕过`：否。
