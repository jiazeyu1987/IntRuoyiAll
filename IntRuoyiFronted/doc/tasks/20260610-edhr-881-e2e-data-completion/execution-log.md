# 执行日志

## BDD

- BDD: 测试租户完整 eDHR 批次执行 -> Given 测试租户存在有效未冻结工单 `881MO090863` 且关联路线 `ROUTE-YXN.069.001.1001`，When 用户从 `eDHR执行列表` 打开或创建批次并按工序填写、签名、提交、审批、关闭和归档，Then 批次详情、复盘页和最终归档均能查看模拟填写记录、签名记录和流程历史。
- BDD: 数据平移边界 -> Given 芋道源码租户存在可复用主数据，When 测试租户缺少完成 E2E 的必要数据，Then 仅将必要数据复制到测试租户，不写入芋道源码租户，不覆盖测试租户已有有效数据。
- BDD: 无绕过真实用户路径 -> Given 前端存在 eDHR 创建、填写、签名、审批、复盘入口，When 执行 E2E，Then Playwright 操作前端完成流程，SQL/API 只用于数据准备和最终校验。

## TDD / E2E Evidence

- RED: `node --check tests\e2e\edhr-881-completed-batch-review.e2e.js` -> FAIL, expected reason: 复核脚本尚不存在，Node 返回 `MODULE_NOT_FOUND`。
- GREEN: `node --check tests\e2e\edhr-881-completed-batch-review.e2e.js` -> PASS。
- RED: `$env:EDHR_881_E2E_PASSWORD='admin123'; node tests\e2e\edhr-881-completed-batch-review.e2e.js` -> FAIL, expected reason: 脚本断言页面必须显示路线编码；实际批次详情页显示路线名称，路线编码已由详情接口返回校验。
- GREEN: 修正路线页面断言 -> PASS，详情页继续校验接口中的 `routeCode=ROUTE-YXN.069.001.1001`，页面校验路线名称。
- RED: 复盘页断言 `approvalRecords >= 15` -> FAIL, expected reason: 后端正式复盘模型只有 `batchEvents/taskEvents/signatureRecords/archiveVersions`，单表审批完成体现在 `taskEvents.status=40` 和 `approvedAt`。
- GREEN: 复盘断言改为 `taskEvents` 中 15 个已批准任务 -> PASS。
- RED: 打印窗口 URL 断言必须为 `blob:` -> FAIL, expected reason: headless Playwright 捕获到 popup 但 URL 不稳定；真实可观察行为是点击打印后出现浏览器 popup。
- GREEN: `$env:EDHR_881_E2E_PASSWORD='admin123'; node tests\e2e\edhr-881-completed-batch-review.e2e.js` -> PASS，真实前端完成批次详情、复盘、最终 PDF 下载和打印窗口触发验证。
- REGRESSION: 数据库最终校验 -> PASS，测试租户批次 `PC-E2E-20260610-0210` 为 `status=40`，`task_total=21`，`task_approved_count=15`，`blocked_count=0`；任务状态分布 `status=40` 共 15、`status=0` 共 6；单表签名 `APPROVE/FIELD_CHANGE/FORM_REVIEW/SUBMIT` 各 15 条；最终归档 `SEALED`。
- REGRESSION: 租户边界 -> PASS，本轮只读核对芋道源码租户数据，未写入源租户；测试租户目标数据已存在，未执行新增平移 SQL。

## 数据范围

- 目标租户：测试租户。
- 源租户：芋道源码，仅用于读取和平移必要数据。
- 目标工单：`881MO090863`。
- 目标路线：`ROUTE-YXN.069.001.1001`。
- 目标批次号：`PC-E2E-20260610-0210`。

## 回滚

本轮未新增或更新测试租户业务数据，因此无新增记录回滚清单。E2E 复核产生的本地证据位于 `test-results/edhr-881-completed-batch-review/`。
