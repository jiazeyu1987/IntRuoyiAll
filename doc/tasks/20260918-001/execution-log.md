# Execution Log

## Scope

本任务实现 DCC 普通上传的不可编辑必选版本、可编辑可选版本，以及两个独立下载权限。

## BDD

- BDD: 必须上传不可编辑版本 -> Given 用户提交受控文件 / When 不可编辑版本缺失 / Then 提交失败且不创建受控文件。
- BDD: 可编辑版本可选 -> Given 有效不可编辑 PDF / When 无可编辑版本 / Then 提交成功且浏览使用不可编辑 PDF。
- BDD: 双版本保存 -> Given 两个版本均上传 / When 提交成功 / Then 两个文件分别保存，浏览只读取不可编辑 PDF。
- BDD: 下载权限独立 -> Given 用户只拥有一个下载权限 / When 请求两个下载接口 / Then 仅对应接口成功。
- BDD: 浏览权限独立 -> Given 只有在线浏览权限 / When 浏览并尝试下载 / Then 浏览成功且下载被拒绝。

## Verification Timeline

- RED: `node --test tests/dcc-dual-version-upload-download-contract.test.cjs` in `IntRuoyiFronted` -> FAIL，3 个契约失败：提交 VO 缺少 `readOnlyUploadTicket`，上传策略缺少 `PURPOSE_READ_ONLY_VIEW` / `PURPOSE_EDITABLE_SOURCE`，后端和前端缺少 `/download/read-only`、`/download/editable` 与两项下载权限。
- GREEN: 已完成双版本字段持久化、不可编辑浏览解析、双下载接口与权限隔离，并收紧旧 `/download` 入口为不可编辑下载权限。
- REGRESSION: 前端双版本静态契约测试 4/4 通过，前端 ESLint 通过，`git diff --check` 通过；`scripts\preflight\branch-runtime-port-guard.ps1` 通过。
- REGRESSION: 高内存 `pnpm exec vue-tsc --noEmit` 首次发现 DCC 浏览页、API 类型和上传页的本任务相关类型问题；已修复后重跑，全量类型检查仅剩 Form/MES/Workorder 既有错误，无 DCC 相关错误。
- FIX: 恢复 `GET /dcc/controlled-files/{id}` 详情接口为 `dcc:controlled-file:query`，避免在线浏览/查询被不可编辑下载权限误阻断；静态契约已新增防回归断言。
- TOOLING: `where.exe mvn`、`where.exe mvn.cmd`、`where.exe java` 均未找到；随后下载仓库外便携 Java 17.0.20.1 与 Maven 3.9.11 到 `C:\IntRuoyi\.tool-cache` 用于本地验证。
- REGRESSION: 首次 `mvn -pl yudao-module-dcc -am -DskipTests compile` -> FAIL，`DccControlledFileVersionHistoryRespVO` 缺少 `setCanDownloadReadOnly` / `setCanDownloadEditable`。
- FIX: 为 `DccControlledFileVersionHistoryRespVO` 增加 `canDownloadReadOnly`、`canDownloadEditable` 字段。
- REGRESSION: `mvn -pl yudao-module-dcc -am -DskipTests compile -rf :yudao-module-dcc` -> PASS。
- REGRESSION: `mvn -pl yudao-module-dcc -am -DskipTests compile` -> PASS。
- TOOLING: 本机未找到既有 `task_closeout.py` / cleanup skill；已在 `C:\Users\D01020\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py` 补齐本地 cleanup preview/apply 脚本，仅清理当前任务目录内非保留文件。
- CLOSEOUT: cleanup preview 通过，保留 task.md、execution-log.md、verification-report.md 和契约测试；cleanup apply 通过，无需删除中间文件。
- CLOSEOUT: 任务状态已更新为 completed，待提交、推送并融合到 `int_main`。
- COMMIT: implementation commit `96145b5b9`, pushed as `origin/codex/20260918-001`; staged file list was limited to the DCC implementation, SQL, frontend contract test, design document, and task records.

## Design Constraints

- 不自动转换，不将一个上传 ticket 复制到两个版本。
- 普通浏览固定解析不可编辑文件。
- 下载权限在 Spring Security 注解和前端按钮上同时隔离。
- 不执行真实 E2E，不写真实数据库。
