# 表单解析生产批记录 JSON 输出修正

## Task Goal

定位并修复“表单解析 -> 生产批记录”上传 Word 后提示 `Template schema rows are missing` 的问题；随后按用户澄清，将下载 JSON 从 Jimu 表单版式 JSON 调整为类似 `E:\IntRuoyi\resource\按压式球囊扩充压力泵IDI-001\批记录总对应.json` 的生产批记录业务映射 JSON，并使用 `E:\IntRuoyi\resource\按压式球囊扩充压力泵IDI-001\RE-PP-IDI-01（A 1） 按压式球囊扩充压力泵生产记录--2026.02.02生效.doc` 做验证。

## Milestones

- [x] M1: 复现 `Template schema rows are missing` 并定位异常来源
- [x] M2: 对照表单中心 Word 解析 JSON 的正式链路，确认差异
- [x] M3: 增加回归测试锁定给定 `.doc` 文件解析行为
- [x] M4: 按最小范围修复解析链路
- [x] M5: 运行定向验证并记录 RED/GREEN
- [x] M6: 将生产批记录下载 JSON 调整为批记录业务映射格式
- [x] M7: 运行变更后的定向回归、证据校验和收尾清理

## Expected Verification

- Targeted backend regression test for production batch record parse-only JSON
- Backend regression test proving parse-only output matches `product/schemaVersion/processes` batch-record mapping shape
- Frontend static contract proving the downloaded JSON payload is the batch-record mapping content, not the wrapper response or Jimu schema
- Frontend TypeScript relaxed type check for the updated parser page and API wrapper
- `python C:\Users\BJB110\.codex\skills\change-request-triage\scripts\validate_change_request.py --evidence docs/changes/20260908-form-parser-production-batch-record-mapping-json.md`
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260908-form-parser-template-schema-rows/backend-api-evidence.md`
- `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc/tasks/20260908-form-parser-template-schema-rows/bug-regression-evidence.md`
- Task-scoped `git diff --check`

## Current Status

ready_for_closeout

实现、验证和 task-closeout-cleanup 已完成：生产批记录按钮已改为下载 `product/schemaVersion/processes` 批记录总识别 JSON；真实 `.doc` 与 `批记录总对应.json` 语义等价回归、前端静态合同、类型检查、evidence validator、task-scoped diff check 和 cleanup preview/apply 均已通过。因本轮未获 Git 提交/推送授权，当前不标记 completed。

## 设计约束检查

- 解析方式必须复用表单中心 Word 解析 JSON 的正式链路，不新增硬编码模板结构或默认成功 JSON。
- 生产批记录下载 JSON 必须是从上传 Word 解析出的 `product/schemaVersion/processes` 业务映射结构，不得直接读取样例 JSON 当返回值。
- 缺少真实 schema、依赖或源文件时 fail fast，不吞异常、不降级。
- 不执行数据库写入、远程操作、服务重启或 Git 提交。
- `.doc` 验证必须使用用户指定的真实文件路径。
