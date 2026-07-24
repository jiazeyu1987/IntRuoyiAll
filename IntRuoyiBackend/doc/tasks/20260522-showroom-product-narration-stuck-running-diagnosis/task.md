# 任务：展厅产品一键讲解长时间停留执行中排障（后端）

## Goal

定位 `showroom/product` 一键讲解任务长时间显示“执行中（剩171）”且两小时无进展的根因；若问题落在后端任务状态、锁释放、异常持久化或任务线程卡住行为，补齐最小修复并留下回归证据。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-ai\src\main\java\cn\iocoder\yudao\module\ai\framework\ai\core\model\codexcli\CodexCliChatModel.java`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-ai\src\test\java\cn\iocoder\yudao\module\ai\framework\ai\core\model\codexcli\CodexCliChatModelTest.java`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\main\java\cn\iocoder\yudao\module\showroom\controller\ShowroomApiRuntime.java`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\main\java\cn\iocoder\yudao\module\showroom\controller\admin\ShowroomAdminController.java`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\test\java\cn\iocoder\yudao\module\showroom\integration\ShowroomProductNarrationRegressionTest.java`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260522-showroom-product-narration-stuck-running-diagnosis\**`

## Non-Scope

- 不修改一键讲解补齐规则本身。
- 不引入 fallback、mock 成功或静默跳过。
- 不扩大到一键语音、一键封面任务模型，除非排查证据证明共用逻辑有问题。

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260522-showroom-product-narration-current-product-status\task.md`
- Status before this task: `Completed`
- Impact: 上一任务已完成，不阻塞本次继续排查真实运行态卡住问题。

## Repository Status Check

- Repository: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`
- Current state: 仓库存在并行未提交改动。
- Impact: 本任务仅允许修改一键讲解任务状态与回归测试相关代码和任务文档，不覆盖无关改动。

## Milestones

1. 复现并记录卡住现象，核对状态接口、持久化配置、日志与线程行为。
2. 先补 RED，锁定“任务实际未推进却持续 running”的回归行为。
3. 实施最小修复并验证状态会正确推进、失败暴露或回落。
4. 更新证据并执行 closeout preview。

## Expected Verification

- `mvn -pl yudao-module-ai "-Dtest=CodexCliChatModelTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `mvn -pl yudao-module-showroom "-Dtest=ShowroomProductNarrationRegressionTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `mvn -pl yudao-server -am -DskipTests package`
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260522-showroom-product-narration-stuck-running-diagnosis --mode preview`

## Current Status

Completed.

## Completed Work

- 通过数据库状态、JVM 线程栈和子进程树确认根因：一键讲解批任务真实卡在 `CodexCliChatModel -> codex.cmd exec` 子进程上，导致后台锁长期不释放，页面持续显示 `执行中（剩171）`。
- 在 `CodexCliChatModel` 的 `codex exec` 命令行补上 `--ephemeral`，让讲解稿链路与封面链路一致，避免复用持久 app-server 会话。
- 补充 AI 模块回归测试，锁定“必须以 ephemeral 模式调用 Codex CLI”契约。
- 手工清理了数据库里这条卡住任务的 `active/running` 状态，并保留显式失败说明，避免页面继续显示旧的执行中。
- 重新打包 `yudao-server.jar`，并重启本地 `48081/8081` 运行态；手工健康检查确认前后端均返回 `HTTP 200`。

## Verification Result

- PASS: `mvn -pl yudao-module-ai "-Dtest=CodexCliChatModelTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- PASS: `mvn -pl yudao-module-showroom "-Dtest=ShowroomProductNarrationRegressionTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- PASS: `mvn -pl yudao-server -am -DskipTests package`
- PASS: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260522-showroom-product-narration-stuck-running-diagnosis --mode preview`
- PASS: real login + status API -> `POST /admin-api/system/auth/login` with `tenant-id=122`, then `GET /admin-api/showroom/product/batch-generate-narration-script/status` returned `active=false`, `running=false`, `currentProduct=null`, and explicit `SYSTEM_RESET` failure context.

## Remaining Blockers

- 当前工作区仍有大量与本任务无关的并行未提交改动；虽然本任务相关文件可单独提交，但不适合顺手夹带其他在途文件。
