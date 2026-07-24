# 任务：Smart Release Phase 1 实现与本地真实 E2E 门禁

## Goal

按已放行的 Smart Release 长期方案，开始实现 Phase 1：Manifest v1 校验、build-release intake report-only、deploy-release precheck report-only，并补齐本地真实 E2E 验收门禁。

## 任务目标

- 更快打包与稳定发布的第一阶段基础能力先落地为可执行代码和测试。
- 构建侧必须能发现 schema/data/resource 变化并输出结构化 report-only 报告。
- 部署侧必须能按逻辑环境和 target config 做本地 deploy-precheck report-only，不写死测试服、正式服、备份服 IP。
- 每个功能点必须有可执行测试；涉及 Runtime Control 或用户路径的验收必须用 Playwright 登录本机真实租户。
- 编写、调试、会产生数据写入的 E2E 只能使用本机测试租户 `测试租户/aoteman`。
- 最终复核必须使用本机 `芋道源码/admin` 登录验证；失败后回到本机测试租户修复，再回到 `芋道源码/admin` 复核。
- 本任务不得访问或修改外部测试服、正式服、备份服。

## Scope

- 新增或更新 `script/release/**` 的 Phase 1 工具。
- 新增或更新 `script/tests/test_release_*.py` 测试。
- 必要时新增 Runtime Control 本地 report-only 展示/API/E2E 验收路径。
- 更新本任务 `execution-log.md`、`test-report.md`、`task-state.json`。

## Non-Scope

- 不部署到外部服务器。
- 不连接或修改外部测试服、正式服、备份服。
- 不执行真实数据库 migration。
- 不同步、删除或覆盖 MinIO/NAS 对象。
- 不实现 artifact cache 正式复用、resource delta 执行、manifest executor 接管、rollback 执行。
- 不允许用 mock success、接口绕过真实用户路径、全库覆盖、全量资源 mirror、自动 URL 改写或外网 fallback 来通过验收。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；缺配置、缺本地后端、缺测试租户、缺真实 E2E 前置条件时必须记录 blocker，不得改用 mock 或接口绕过。
- `是否从根因和长期维护角度解决`：是；本任务按 Smart Release Phase 1 契约先建立构建/部署 report-only 基座和 E2E 门禁。
- `是否存在临时补丁或绕过`：否；任何为了当前构建成功而跳过 schema/data/resource 检查的改动都不放行。

## Milestones

- [x] M0：确认本地验证前置条件、子 agent 写入边界和阻塞项。
- [x] M1：Manifest v1 schema、validator、fixture、RED/GREEN 测试。
- [x] M2：build-release intake report-only、schema/data/resource 报告、RED/GREEN 测试。
- [x] M3：deploy-release precheck report-only、target config、artifact/resource gate、RED/GREEN 测试。
- [x] M4：发布脚本可选 report-only 接入，不改变 legacy build/deploy 返回码。
- [x] M5：Runtime Control 本地真实 E2E 门禁，测试租户调试，芋道源码/admin 复核。
- [x] M6：主 agent 严格 review，独立测试通过后才放行。

## Expected Verification

- `python -X utf8 -m pytest script/tests/test_release_manifest_validator.py -q`
- `python -X utf8 -m pytest script/tests/test_release_intake_report_only.py -q`
- `python -X utf8 -m pytest script/tests/test_release_deploy_precheck_report_only.py -q`
- `python -X utf8 -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py -q`
- Playwright 本地测试租户真实路径 E2E。
- Playwright 本机 `芋道源码/admin` 最终复核路径 E2E。

## Current Status

completed

## Final Verification

- `python -X utf8 -m pytest script/tests/test_release_manifest_validator.py script/tests/test_release_intake_report_only.py script/tests/test_release_deploy_precheck_report_only.py script/tests/test_publish_int_ruoyi_to_test_tooling.py -q` -> PASS，114 passed。
- `mvn -pl yudao-module-infra -Dtest=RuntimeControlServiceImplTest test` -> PASS，49 tests。
- `node yudao-ui-admin-vue3\tests\e2e\runtime-control-smart-release-report-only.e2e.js` -> PASS，测试租户 `测试租户/aoteman/admin123` 真实路径。
- `node yudao-ui-admin-vue3\tests\e2e\runtime-control-yudao-admin-readonly.e2e.js` -> PASS，本机 `芋道源码/admin/admin123` 只读复核。
- 独立 reviewer 子 agent `019e9805-4b9c-7152-8cf1-62dddf22c082` -> PASS，`final_decision=pass`。

## 当前前置条件记录

- 本地前端 `http://localhost:8081` 可访问。
- 本地后端 `http://localhost:48081` 健康检查可访问；`48080` 不作为当前 worktree 的后端端口。
- Playwright E2E 需使用 `RUNTIME_CONTROL_E2E_BASE_URL=http://localhost:8081` 与 `RUNTIME_CONTROL_E2E_ACTION_ORIGIN=http://127.0.0.1:48081`。
