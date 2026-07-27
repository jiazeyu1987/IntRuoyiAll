# Execution Log

## 用户意图

- 用户确认当前分支两个 LFS 文件均可删除。
- 实施口径：下线 Win7 客户端下载能力；奖项回导 Excel 保持测试运行时可生成，但不再作为仓库资产保留。

## 基线

- 分支：`int_main`。
- Remote：`origin=https://github.com/jiazeyu1987/IntRuoyiAll.git`。
- BASELINE: `f18927b9e3682a8a66d44d535b24c75b824b40e2` -> `chore: baseline pre-existing dirty worktree`。
- 基线文件清单：80 个既有脏文件，使用 `git show --name-status --format= f18927b9` 可完整复验。
- 基线后并行任务继续修改 Codex 测试管理相关文件；这些文件不属于本任务，后续不得暂存。
- 基线 `git diff --cached --check` 曾报告既有 `index.vue` 全文件尾随空白和部分 Markdown EOF 空行；为保持既有脏改动原样，未在基线中擅自修复。

## BDD / TDD

- BDD: 下线 Win7 客户端下载 -> Given Win7 客户端资产已批准删除 / When 用户进入公司工作台或调用展厅客户端下载接口 / Then 仅保留 Android 下载能力且不存在 Win7 下载入口。
- BDD: 导出回导文件不进入仓库 -> Given 奖项导出回导 E2E 需要同一 Excel 完成导入 / When 测试执行结束 / Then Excel 临时文件被清理且仓库不再跟踪该文件。

## Milestone Status

- M1: completed。用户批准、变更影响、LFS 现状和脏工作区基线已记录。
- M2: completed。已增加后端 JUnit 退休契约和跨前后端静态契约。
- M3: in_progress。
- M4: pending。
- M5: pending。

## RED Evidence

- RED: `node tests\e2e\showroom-client-download-retirement-static.spec.js` -> FAIL，首个预期原因是前端 API 仍包含 `SHOWROOM_DESKTOP_CLIENT`；同一契约还覆盖后端 Win7 映射、两个资产、两个 LFS 属性文件和 Excel 临时清理。
- RED ATTEMPT: `mvn -pl yudao-module-showroom -am "-Dtest=ShowroomClientDownloadControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> BLOCKED，工作区同时存在多路 Maven 构建，目标进程运行超过 6 分钟仍未生成 surefire 报告；已仅停止本任务启动的 PID `60176`，未操作其他任务进程。该命令将在 GREEN 阶段重新执行。
