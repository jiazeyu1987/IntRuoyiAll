# 验证报告

## Current Status
blocked

## 验证结论

- PASS：后端 `48081` health `UP`，PID `57192`，运行 `backend-runtime-control-20260920-025034-dcc-version-fix.jar`。
- PASS：审批人通过真实审批中心页面驳回并填写原因，原因 `E2E 驳回原因 20260919183402 - 请重新上传修正文档`。
- PASS：申请人通过真实审批中心已办页面查看驳回结果。
- PASS：申请人通过真实受控文件浏览页面检出驳回版本，上传新文件并检入新版本 A/5。
- PASS：申请人通过真实页面重新提交审批，待办重新出现，当前节点为 `文控审核`。
- PASS：已办历史记录保留 `已驳回`、驳回意见、`签名`、`审计`、`证据账本`入口；最终只读 Playwright 验收无 console/page error。
- PASS：最终只读验收 run `20260920191030`，证据 JSON、截图和 trace 在 cleanup 前位于 `doc/tasks/20260920-dcc-reject-withdraw-resubmit-e2e/e2e-artifacts/`。

## 范围边界

产品在“已驳回”状态没有独立“撤回”按钮，因此本次按真实产品支持的“驳回后检出修订、检入新版本、重新提交审批”链路验收撤回/重新提交；未用 API 或数据库代替任何被验收业务动作。

## 收尾

- cleanup preview/apply 均 PASS，已删除本任务临时截图、trace、JSON、诊断脚本和启动辅助脚本，保留核心任务记录与主 E2E 脚本。
- E2E 验证结论为 PASS；未标记 `completed` 的唯一原因是本轮没有 Git 提交/推送授权。
