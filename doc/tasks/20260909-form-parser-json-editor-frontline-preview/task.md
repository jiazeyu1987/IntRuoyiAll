# 表单解析 JSON 编辑与一线生产预览

## Goal

在 `jiexi123` worktree 中把表单解析生产批记录结果区改为左侧 JSON 编辑模块、右侧一线生产样式预览。用户可直接修改解析出的 `product/schemaVersion/processes` 业务映射 JSON，点击“应用”后刷新右侧预览；右侧按产品、工序、输入物料、输出物料、设备、设备参数控件展示当前 JSON 内容。

## Milestones

- [x] M1: 确认 worktree、规则、页面入口、API 合同和当前静态合同
- [x] M2: 写入 BDD 与 RED 静态合同
- [x] M3: 实现 JSON 编辑、应用校验、下载当前 JSON 和一线生产预览
- [x] M4: 完成静态合同、类型检查和差异检查验证
- [x] M5: 记录验证报告和剩余收尾状态
- [x] M6: 合并本轮 worktree 经验到长期经验文档
- [x] M7: 在 `jiexi123` worktree 启动独立运行态并执行真实 Playwright E2E
- [x] M8: 融合到 `int_main` 工作区并在 `int_main` 运行态执行真实 Playwright E2E

## Expected Verification

- `node tests/e2e/form-parser-json-download-static.spec.cjs`
- `pnpm ts:check`
- `git diff --check`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260909-form-parser-json-editor-frontline-preview/frontend-feature-evidence.md`
- Worktree runtime: registered frontend/backend ports, frontend HTTP 200, backend `/actuator/health` UP
- `int_main` runtime: frontend `8081` HTTP 200, backend `48081` `/actuator/health` UP
- Playwright real E2E: login as `芋道源码/admin`, enter `表单解析` through visible menu or route, upload the provided production batch Word, observe parse/download, edit JSON, apply it, verify the right-side frontline preview updates and process selector/controls are interactive.

## Design Constraints Check

- 仅修改表单解析页前端展示、静态合同、本任务文档和项目经验文档；不修改后端接口、解析器、数据库、权限和菜单。
- 生产批记录仍调用 MES parse-only 接口 `/mes/pro/batch-record-report/production-batch-record/total-recognition-json`，下载和预览的内容仍是 `product/schemaVersion/processes` 批记录业务映射 JSON，不使用 Jimu schema。
- 左侧 JSON 编辑失败必须显示具体错误并保持右侧上一次成功应用的预览，不吞异常、不伪造成功。
- 右侧预览只表达当前 JSON 内容；没有正式来源的真实生产提交、工单号、员工和电子签名不接入后端、不写入数据。
- 未经本轮明确授权，不启动服务、不执行数据库写入、不提交或推送。

## Current Status
ready_for_closeout

Merged from `jiexi123` into the `int_main` working tree. Static contract, TypeScript check, diff check and real Playwright E2E all passed on `int_main`; commit, push, cleanup and worktree removal are not performed in this turn.

## Cleanup Keep

- doc/tasks/20260909-form-parser-json-editor-frontline-preview/form-parser-frontline-real-e2e.cjs
