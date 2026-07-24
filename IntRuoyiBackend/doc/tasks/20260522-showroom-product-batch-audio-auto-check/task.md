# 任务：展厅产品一键语音定时续跑（后端）

## Goal

为 `showroom/product/batch-generate-narration-audio` 增加“首轮执行后每 10 分钟自动检查并续跑”的后端能力，保持当前筛选条件语义不变，并确保：

- 已有双语音频的产品跳过；
- 缺中文或英文讲解稿的产品跳过；
- 只对已发布且当前发布版本讲解稿齐全但缺音频的产品补齐语音；
- 若处理中断，重启后仍可根据已持久化状态继续扫描同一批次；
- 当所有可处理产品都完成或只剩缺稿产品时，自动检查关闭。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\main\java\cn\iocoder\yudao\module\showroom\controller\ShowroomApiRuntime.java`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\main\java\cn\iocoder\yudao\module\showroom\controller\admin\ShowroomAdminController.java`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\main\java\cn\iocoder\yudao\module\showroom\job\**`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\test\java\cn\iocoder\yudao\module\showroom\integration\**`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260522-showroom-product-batch-audio-auto-check\**`

## Non-Scope

- 不把批量语音范围改成“全量已发布产品”。
- 不新增 fallback、mock 成功或静默吞错逻辑。
- 不修改产品详情单条生成语音入口的现有语义。
- 不为 E2E 额外增加前端测试控件。

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-yingtai-showroom-narration\task.md`
- Status before this task: `Completed on 2026-05-21`
- Impact: 前一任务已完成，不阻塞本次 showroom 产品批量语音续跑后端交付。

## Repository Status Check

- Repository: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`
- Current state: 仓库存在 showroom、MES、AI 等多组未提交改动。
- Impact: 本任务仅允许修改 showroom 批量语音续跑相关代码、定向测试与本任务文档，不覆盖无关在途改动。

## Milestones

1. 建立任务文档并锁定批量语音自动检查状态持久化字段、跳过规则与停止条件。
2. 先补 RED，锁定“已有双语音频跳过”“缺稿跳过”“首轮失败后仍保持 enabled”“自动关闭条件”“重启后续跑”的可观察行为。
3. 最小实现共享批次执行器、自动检查状态持久化、10 分钟调度器与并发闸门。
4. 扩展批量接口返回值与状态查询接口。
5. 跑定向回归、更新证据并执行 closeout preview。

## Expected Verification

- `mvn -pl yudao-module-showroom "-Dtest=ShowroomProductNarrationRegressionTest,ShowroomHttpApiIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260522-showroom-product-batch-audio-auto-check --mode preview`

## Current Status

Blocked on 2026-05-22.

## Completed Work

- 已实现 showroom 后端批量语音自动检查状态持久化，使用隐藏 `ConfigService` 配置保存：
  - `enabled`
  - 锁定筛选条件
  - 最近一次汇总结果
  - 最近一次失败信息/时间
- 已把产品批量语音改为“首轮执行 + 共享批次执行器 + 10 分钟调度续跑”结构。
- 已实现批次规则：
  - 已发布产品才进入处理判断
  - 当前发布版本双语音频都齐全时跳过
  - 缺中文或英文讲解稿时跳过
  - 缺任一语音时仅补齐缺失语音并直接发布对应 narration
  - 失败后保留 `enabled=true`，等待下轮自动检查重试
  - 无剩余可处理产品时自动关闭
- 已新增 `GET /showroom/product/batch-generate-narration-audio-state`。
- 已新增 10 分钟自动检查调度器。
- 已兼容当前仓库在途的批量封面后台任务返回契约，避免破坏已有未提交改动。

## Final Verification Result

- PASS: `mvn -pl yudao-module-showroom "-Dmaven.test.skip=true" compile`
- BLOCKED: `mvn -pl yudao-module-showroom "-Dtest=ShowroomProductNarrationRegressionTest,ShowroomHttpApiIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
  - Blocker: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\test\java\cn\iocoder\yudao\module\showroom\integration\ShowroomProductNarrationRegressionTest.java`
  - Impact: 该文件已存在一组与本任务无关的“批量讲解稿定时续跑”测试，直接引用当前仓库尚未实现的 `startBatchGenerateNarrationScript` / `getProductBatchGenerateNarrationScriptStatus` / `runScheduledProductBatchNarrationScriptAutoCheck` 等方法，导致 `testCompile` 阶段失败，无法完成本次后端整套 Maven 测试放行。
- PASS: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260522-showroom-product-batch-audio-auto-check --mode preview`
