# Verification Report

## Scope

- 当前任务删除本机 `ruoyi-vue-pro` 库、当前页面租户 `tenant_id=1` 下项目为 `批记录` / `文控` 的测试管理测试项。
- 删除语义对齐后端服务：直属检查点物理删除，测试项软删除。
- 未访问远端测试服、正式服、备用服或共享存储。

## Preflight Evidence

- 本地前端端口 `8081` 和后端端口 `48081` 均在监听，后端 health 返回 `UP`。
- 页面路径尝试结果：登录页提示“后端服务响应超时”，脚本未进入删除动作，因此没有通过页面删除任何测试项。
- 数据库 schema 核对通过：`system_codex_test_case.project`、`system_codex_test_checkpoint.case_id`、`deleted`、`tenant_id` 字段均存在。
- 删除前当前租户目标项：`批记录` 6 个 / 24 个检查点，`文控` 4 个 / 8 个检查点。

## Deletion Evidence

- 删除范围：`tenant_id=1 AND deleted=b'0' AND project IN ('批记录','文控')`。
- 删除测试项：ID `2-7`（批记录）和 `14-17`（文控），共 10 个。
- 删除检查点：32 行。
- 测试项处理：10 行软删除，`deleted=b'1'`，`updater='codex'`。

## Verification

- 当前租户 `批记录` / `文控` 未删除测试项：0。
- 当前租户 `批记录` / `文控` 直属未删除检查点：0。
- 当前租户非目标测试项仍保留 8 个：`工艺路线` 4 个、`智能排产` 4 个。
- 跨租户数据未删除：`tenant_id=122` 下仍有 `文控` 2 个，原因是不属于当前 `芋道源码/admin` 页面上下文。

## Artifacts

- Summary: `doc/tasks/20260727-delete-codex-test-items-batch-dcc/artifacts/delete-batch-dcc-codex-test-items-summary.json`
- Cleanup: 任务临时脚本和失败截图已删除，仅保留可审计 summary。
