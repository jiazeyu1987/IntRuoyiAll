# 任务：展厅产品管理增加一键卖点（后端）

## Goal

为 `showroom/product` 产品管理提供一个批量 `一键卖点` 能力，允许企宣用户按当前筛选范围补齐产品当前版本缺失的中文核心卖点和英文核心卖点，并返回真实任务状态与失败原因。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\main\java\cn\iocoder\yudao\module\showroom\controller\admin\ShowroomAdminController.java`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\main\java\cn\iocoder\yudao\module\showroom\controller\ShowroomApiRuntime.java`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\main\java\cn\iocoder\yudao\module\showroom\narration\ShowroomProductNarrationCodexService.java`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\test\java\cn\iocoder\yudao\module\showroom\integration\ShowroomProductNarrationRegressionTest.java`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\test\java\cn\iocoder\yudao\module\showroom\integration\ShowroomHttpApiIntegrationTest.java`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260522-showroom-product-batch-selling-points\**`

## Non-Scope

- 不修改数据库 schema。
- 不变更产品审批流规则。
- 不新增 fallback、兼容分支、默认成功结果或假卖点内容。

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260522-product-022-showroom-cover-single-native-rerun\task.md`
- Status before this task: `Blocked on 2026-05-22 due to user priority switch`
- Impact: 上一同仓任务已因用户切换优先级显式暂停，不阻塞本次 showroom 产品卖点能力交付。

## Repository Status Check

- Repository: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`
- Current state: 仓库存在 MES、DCC、showroom 封面与文档等多组并行未提交改动。
- Impact: 本任务仅允许修改 showroom 产品卖点生成链路、定向测试与本任务文档，不覆盖无关在途改动。

## Milestones

1. 建立任务文档并确认当前产品批量能力、卖点字段落点与显示版本契约。
2. 先补 RED，锁定“批量卖点生成补齐中英卖点、已有语言跳过、失败原因可见”的后端行为。
3. 最小实现批量卖点接口、生成逻辑与任务状态返回。
4. 跑定向 Maven 回归并更新证据。
5. 执行 closeout preview。

## Expected Verification

- `mvn -pl yudao-module-showroom -am "-Dtest=ShowroomProductNarrationRegressionTest,ShowroomHttpApiIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `python -X utf8 C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260522-showroom-product-batch-selling-points\backend-api-evidence.md`
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260522-showroom-product-batch-selling-points --mode preview`

## Current Status

Completed on 2026-05-22.

## Completed Work

- 已确认当前后端仅提供产品 `翻译英文 / 生成讲解稿 / 生成语音 / 生成封面 / 批量讲解 / 批量语音 / 批量封面` 能力。
- 已确认产品列表卖点状态读取的是 `displayRevision.fields`，即当前发布版本优先，非独立资产。
- 已确认当前不存在批量卖点生成接口或任务状态契约。
- 已新增 `POST /showroom/product/batch-generate-selling-points` 后端接口与响应契约。
- 已为产品 Codex 服务补齐中文卖点生成能力，并复用现有中译英链路补齐英文卖点。
- 已将批量卖点生成落到“最新产品版本缺失语言补齐后保存为新草稿版本”的最小实现路径。
- 已补齐回归测试并完成零副作用的真实接口终验。

## Verification Result

- PASS: `mvn -pl yudao-module-showroom -am "-Dtest=ShowroomProductNarrationRegressionTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- PASS: 真实接口零命中校验 `POST http://127.0.0.1:48081/admin-api/showroom/product/batch-generate-selling-points`
- PASS: `python -X utf8 C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260522-showroom-product-batch-selling-points\backend-api-evidence.md`
- PASS: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260522-showroom-product-batch-selling-points --mode preview`
