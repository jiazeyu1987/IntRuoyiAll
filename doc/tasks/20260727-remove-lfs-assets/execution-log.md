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
- CONCURRENT COMMIT: `85afb6fea8e67c0724f117d2da5a86794cc023d8` -> 另一并行任务于 2026-07-27 19:13:36 创建脏工作区基线时一并提交了本任务实现、测试、变更单和初始任务记录。未重写或拆分该提交；后续仅提交本任务收尾文件。

## BDD / TDD

- BDD: 下线 Win7 客户端下载 -> Given Win7 客户端资产已批准删除 / When 用户进入公司工作台或调用展厅客户端下载接口 / Then 仅保留 Android 下载能力且不存在 Win7 下载入口。
- BDD: 导出回导文件不进入仓库 -> Given 奖项导出回导 E2E 需要同一 Excel 完成导入 / When 测试执行结束 / Then Excel 临时文件被清理且仓库不再跟踪该文件。

## Milestone Status

- M1: completed。用户批准、变更影响、LFS 现状和脏工作区基线已记录。
- M2: completed。已增加后端 JUnit 退休契约和跨前后端静态契约。
- M3: completed。已删除两个 LFS 文件、两个 LFS 属性文件、Win7 后端映射、前端 API 和页面按钮；E2E Excel 改为系统临时目录文件并在 finally 清理。
- M4: completed。后端、前端、静态契约和语法验证通过。
- M5: completed。经验沉淀、cleanup、提交与最终推送门禁已执行。

## RED Evidence

- RED: `node tests\e2e\showroom-client-download-retirement-static.spec.js` -> FAIL，首个预期原因是前端 API 仍包含 `SHOWROOM_DESKTOP_CLIENT`；同一契约还覆盖后端 Win7 映射、两个资产、两个 LFS 属性文件和 Excel 临时清理。
- RED ATTEMPT: `mvn -pl yudao-module-showroom -am "-Dtest=ShowroomClientDownloadControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> BLOCKED，工作区同时存在多路 Maven 构建，目标进程运行超过 6 分钟仍未生成 surefire 报告；已仅停止本任务启动的 PID `60176`，未操作其他任务进程。该命令将在 GREEN 阶段重新执行。

## GREEN / REGRESSION Evidence

- GREEN: `node tests\e2e\showroom-client-download-retirement-static.spec.js` -> PASS，Win7 后端/前端入口、两个资产和两个 LFS 属性均已移除，Excel 使用临时目录并执行清理。
- GREEN: `node --check tests\e2e\showroom-award-export-import-roundtrip-real.e2e.js` -> PASS。
- GREEN: `pnpm ts:check` -> PASS。
- GREEN: `mvn -pl yudao-module-showroom "-Dtest=ShowroomClientDownloadControllerTest" test` -> PASS，Tests run: 3, Failures: 0, Errors: 0, Skipped: 0。
- DIAGNOSTIC: `mvn -pl yudao-module-showroom "-Dtest=ShowroomClientDownloadControllerTest" "-Dmaven.compiler.useIncrementalCompilation=false" test` -> FAIL，关闭增量编译后暴露模块全量 Lombok/内部类生成顺序错误；未修改产品代码规避。随后恢复项目标准 Maven 参数，正常全量重编译和目标测试通过。
- REGRESSION: `git diff --check -- <task-owned paths>` -> PASS，仅输出既有 LF/CRLF 转换提示，无空白错误。
- GREEN: `git lfs ls-files -l` -> PASS，无当前分支 LFS 文件。
- GREEN: `git check-attr ...` -> PASS，两个已删除路径的 `filter/diff/merge/text` 均为 `unspecified`。
- GREEN: backend API evidence validator -> PASS。
- EXPERIENCE: 已将 Windows Maven `IncrementalBuildHelper.beforeRebuildExecution` / `WinNTFileSystem.delete0` 卡住诊断门禁合并到 `docs/backend-development.md`，并更新 `docs/experience-index.md`，未新建长期经验文档。

## Commit / Closeout Evidence

- CONCURRENT IMPLEMENTATION CAPTURE: `85afb6fea8e67c0724f117d2da5a86794cc023d8`，并行任务基线提交中包含本任务实现和初始记录。
- TASK EVIDENCE COMMIT: `e8049307` -> `docs: verify LFS asset removal`，包含验证报告、完成证据和长期经验门禁。
- CLEANUP PREVIEW: `task_closeout.py --task-id 20260727-remove-lfs-assets --mode preview` -> PASS，keep 4，delete 0，blocked 0，warnings 0。
- CLEANUP APPLY: `task_closeout.py --task-id 20260727-remove-lfs-assets --mode apply` -> PASS，deleted `<none>`，blocked `<none>`。
- FINAL STATUS: completed。
