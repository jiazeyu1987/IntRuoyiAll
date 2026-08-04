# Verification Report

## Scope

- DCC 审批中心待办点击“打开”进入同一路由的上传审批视角。
- 上传审批视角只展示提交范围、文件信息、审批要求、附件预览和当前审批处理区。
- 上传审批视角隐藏详情页全量追溯、历史、分发、打印、培训和签核留痕区块。

## Results

- `node tests/e2e/dcc-approval-upload-view-static.spec.js`：PASS。
- `pnpm e2e:dcc:approval-center-handling-entry:static`：PASS。
- `pnpm e2e:dcc:detail-retired:static`：PASS。
- `pnpm e2e:dcc:detail-lifecycle-timeline:static`：PASS。
- `node tests/e2e/dcc-traceability-ux-static.spec.js`：PASS。
- `pnpm ts:check`：PASS。
- `node --check tests/e2e/dcc-approval-upload-view-real.e2e.js`：PASS。
- `pnpm e2e:dcc:approval-upload-view:real`：PASS。
- `pnpm e2e:dcc:approval-upload-view:static`：PASS。

## Real E2E Evidence

- 入口：`http://127.0.0.1:8081/approval-center/todo?moduleCode=DCC&viewType=TODO`，身份标签 `芋道源码/admin`。
- 跳转结果：`/dcc/controlled-file/detail/2054545668044070311?handling=approval&from=approval-center&processInstanceId=c1cd2ae6-8fbf-11f1-a00f-00155d2984a0&taskId=c1d6eef8-8fbf-11f1-a00f-00155d2984a0`，未携带 `viewer=1` 或 `traceability=1`。
- 页面断言：`dcc-approval-upload-view` 可见，提交范围/审批阶段可见，`dcc-approval-upload-file-preview` 内嵌预览 shell 可见且无预览错误。
- 处理区断言：`dcc-approval-upload-action-panel` 显示当前任务按钮 `审核通过`、`驳回`、`回退`、`转办`、`加签`，不再出现空任务提示。
- 追溯区断言：生命周期、路线快照、版本历史、分发、受控打印、培训、签核追溯、签名留痕、受控浏览 linkage、发布完成摘要均不可见。
- 证据文件：`E:\IntRuoyi\output\playwright\20260804-dcc-approval-upload-view\dcc-approval-upload-view-real-evidence.json`；截图：`E:\IntRuoyi\output\playwright\20260804-dcc-approval-upload-view\dcc-approval-upload-view-real.png`。

## Notes

- 文件预览复用 `ProtectedPdfViewer :controlled-file-id="controlledFileId"`，没有在前端猜测预览文件 ID。
- 未新增后端 API，未修改 `DccApprovalTaskAdapter` 查询字段契约。
- 本轮 E2E 还修正了真实脚本门禁：必须断言当前任务按钮可见，并同时排除 `暂无待处理审批任务` 与 `当前没有待处理审批任务`，避免处理区空态误判为通过。
