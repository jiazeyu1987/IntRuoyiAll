# Execution Log

## BDD Scenario

BDD: critical deviation transfers to QA nonconformance review -> Given a task-owned active-order test batch exists, When an authorized user signs and initiates a critical deviation and manually starts its QA nonconformance review, Then the deviation is automatically closed, linked to the review, and visible as a pending QA review; after QA records a signed disposition, the batch gates reflect the disposition.

## Evidence

- Preflight: frontend at `http://127.0.0.1:8094` returned HTTP 200; backend health probe reported `UP`. No service restart was performed.
- Real UI path: active order pool -> `复制测试单` -> batch execution `打开/创建` -> deviation management `发起偏差` -> `签名并发起` -> deviation detail `发起不合格审批` -> `签名并发起` -> linked QA review page.
- Task-owned batch created: `EDHRB-1790591237468`, batch number `E2E-20260928-KEY-NCR`.
- Critical deviation created: `PC-202609-0009`; UI displayed level `重大（关键）`, status `未处理`, linked batch, and initiation electronic signature `已签署 #12604`.
- Manual QA NCR initiation succeeded. Deviation UI then displayed status `已处理`, close mode `转不合格审批关闭`, linked review `EDHR-NCR-20260928184110-900000001163`, and review disposition `pending_review`.
- QA review page displayed `1 个待评审` and listed the same review and batch marker.
- QA disposition was not submitted. The review page labels `评审材料` as required (`* 评审材料`) and the upload list is empty. Per the task's stop-at-prerequisite instruction, execution stopped without inventing or attaching review evidence.
- Post-disposition gate assertions were not reached because disposition could not be completed.
- Playwright console reported one existing console error during the run; no page errors or warnings were reported in page snapshots. The exact console log is preserved with the artifacts.
- No API/fetch/business endpoint or database write was used; no other deviation or batch was operated on.

## Artifacts

- `IntRuoyiFronted/output/playwright/key-ncr-full-20260928/trace-1790590500560.trace`
- `IntRuoyiFronted/output/playwright/key-ncr-full-20260928/trace-1790590500560.network`
- Screenshots `page-2026-09-28T10-43-04-588Z.png`, `page-2026-09-28T10-46-41-408Z.png`, and `page-2026-09-28T10-47-14-910Z.png`
- `console-2026-09-28T10-13-57-523Z.log`

The trace's test signature password and the network log's cookies, authentication values, and request bodies were redacted before preserving the artifacts.

## Current Status

blocked
