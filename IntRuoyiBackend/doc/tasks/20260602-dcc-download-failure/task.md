# 任务：修复 DCC 文件下载失败

## 任务目标

修复 DCC 文件浏览页点击“下载”后提示“下载失败，请查看错误提示后重试”的问题，确保用户在有权限的情况下可下载现行受控文件，并保留下载审计与加密证据。

## 上一任务检查

- 上一个后端任务 `20260602-dcc-other-category-local-apply` 已标记 `completed`。
- 后端仓库存在其他未提交改动，本任务只修改与本缺陷直接相关的文件，避免纳入无关变更。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，先定位下载请求、审计请求号和后端下载策略的正式契约。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

BDD: 文件浏览页下载现行文件 -> Given 用户在 DCC 文件浏览页看到现行受控文件 / When 点击该行“下载” / Then 前端必须携带唯一下载请求号调用后端下载接口，后端返回文件内容、文件名和下载审计响应头。

BDD: 缺失下载请求号时失败 -> Given 下载接口未收到有效下载请求号 / When 后端处理下载请求 / Then 后端必须明确拒绝并返回业务错误，不得创建下载记录或返回未审计文件。

## 里程碑

- [x] M1：建立任务文档，确认上一后端任务已完成，并隔离前端旧阻塞任务。
- [x] M2：复现或以回归测试锁定“下载失败”的请求契约问题。
- [x] M3：按 RED -> GREEN 最小修复下载链路。
- [x] M4：运行后端/前端相关验证并记录证据。
- [x] M5：收尾清理预览并提交本任务相关改动。

## 预期验证

- 后端目标单测：覆盖 `/dcc/controlled-files/{id}/download` 必填下载请求号和成功响应头。
- 前端目标测试：覆盖下载 API/按钮必须生成并传递 `downloadRequestId`。
- 真实路径验证：在 `http://localhost:8081` 使用测试租户进入 DCC 文件浏览页点击“下载”。

## 当前状态

completed

## 已完成工作

- 已确认上一后端任务 `20260602-dcc-other-category-local-apply` 为 `completed`。
- 已将前端仓库上一任务 `20260602-dcc-nas-transfer-confirm-layer` 标记为 `blocked`，避免任务范围混杂。
- 已复现运行时缺陷：后端下载接口返回 200 和文件字节，但响应缺少 `Content-Disposition`，且 `Access-Control-Expose-Headers` 未暴露 `Content-Disposition`，导致前端下载契约校验失败。
- 已确认当前源码的 DCC 下载控制器契约已经包含 `Content-Disposition` 暴露逻辑，旧运行包 `output/runtime/backend-20260602-200530.jar` 落后于源码。
- 已重新打包并启动本机 48081 到当前源码 jar：`output/runtime/int_main/backend-runtime-control-20260603-085757.jar`。
- 已恢复本机前端依赖缺失前置条件，使用 `pnpm install --frozen-lockfile` 补齐 `node_modules`，并重启 8081。
- 已通过真实前端路径验证：测试租户从左侧菜单进入 `DCC文控中心 -> DCC受控浏览`，点击现行文件 `CODEX-E2E-FOURTH-5662414` 的“下载”，浏览器实际下载 `codex-e2e-stamped.pdf.dcc`，未出现“下载失败，请查看错误提示后重试”。

## 最终验证结果

- RED: `node doc/tasks/20260602-dcc-download-failure/verify-download-response-headers.mjs` -> FAIL，旧运行包返回文件字节但 `Access-Control-Expose-Headers` 未包含 `Content-Disposition`。
- GREEN: `node doc/tasks/20260602-dcc-download-failure/verify-download-response-headers.mjs` -> PASS，测试租户 `aoteman` 下载真实 DCC 文件 `CODEX-E2E-FOURTH-5662414`，响应包含并暴露 `Content-Disposition`。
- GREEN: `mvn --% -pl yudao-module-dcc -Dtest=DccControlledFilePreviewDownloadApiTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS，7 tests。
- GREEN: Playwright 真实前端路径 -> PASS，`http://localhost:8081` 登录测试租户，点击 `DCC文控中心 -> DCC受控浏览 -> 下载`，下载响应 `200`，浏览器建议文件名 `codex-e2e-stamped.pdf.dcc`，下载字节 `1143`，失败 toast 数量 `0`。
- GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260602-dcc-download-failure\bug-regression-evidence.md` -> PASS。
- GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260602-dcc-download-failure --mode preview` -> PASS，delete `<none>`、blocked `<none>`、warnings `<none>`。

## Cleanup Keep

- `doc/tasks/20260602-dcc-download-failure/verify-download-response-headers.mjs`
- `doc/tasks/20260602-dcc-download-failure/bug-regression-evidence.md`

## 阻塞记录

- 无。
