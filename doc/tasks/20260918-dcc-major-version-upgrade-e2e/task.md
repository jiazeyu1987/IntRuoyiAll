# 20260918 DCC 大版本升级完整 E2E

## Task Goal

使用真实 Playwright 页面，在当前 `int_qms` 本机运行态完整执行 DCC 受控文件大版本升级链路：

ACTIVE A/1 -> 页面检出/检入生成 B/1 大版本 -> 上传新版源文件 -> 页面提交 DCC 审批 -> 真实页面逐节点审核/签名 -> 系统按当前发布策略自动完成受控副本和发布后续 -> 新版 ACTIVE、生效文件落位、旧版 SUPERSEDED -> 受控浏览默认打开新版并从页面查看版本历史与升版原因。

API/数据库只允许用于登录态网络证据和最终只读核验，不得代替任何业务写入、审批、发布或状态变更动作。

## BDD

BDD: DCC controlled file major-version upgrade full chain ->
Given 当前租户存在可作为任务自有数据的 ACTIVE 受控文件，且真实页面可进入检出、检入、审批、发布和受控浏览入口
And Playwright 使用真实测试账号完成登录，密码只通过当前进程环境注入
When 用户在真实页面上传/检入新版源文件，选择“大版本”，提交 DCC 审批，审批人逐节点完成审批/签名
Then 新版受控文件在页面上显示为 ACTIVE 并成为当前有效版本
And 旧版在页面可见为 SUPERSEDED，不再作为当前有效浏览结果
And 受控浏览默认打开新版，版本历史展示旧版/新版及升版原因或变更说明
And 若当前发布策略需要单独生效申请，必须从真实页面完成申请和审批；本次运行若页面无该动作，则以详情页“发布后续已完成”、发布件和盖章件均已生成作为实际结果
And 目标写请求、页面错误和业务错误均按真实运行结果记录

## Milestones

- [x] M1 运行态、账号、浏览器和真实页面入口前置确认（临时 EDHR 运行配置后 `48061` 健康、真实登录通过）
- [x] M2 任务自有数据和当前 ACTIVE 源文件可用于大版本升级确认
- [x] M3 真实页面完成大版本创建、上传和 DCC 审批
- [x] M4 真实页面确认当前发布策略下的发布后续自动完成，无额外生效申请待办
- [x] M5 真实页面确认新版 ACTIVE、旧版 SUPERSEDED、受控浏览和版本历史
- [x] M6 最终只读核验、证据整理和任务收尾准备

## Expected Verification

- 当前分支为 `int_qms`，前端/后端成对运行在 `8061/48061`；不使用 `8081/48081` 混配运行态。
- Playwright 真实页面完成所有业务写入和审批动作；不能用 API、SQL、mock 或历史截图替代。
- 运行记录包含真实页面 URL、目标写请求方法/状态/业务码、审批账号标签、旧版/新版编号和页面截图。
- 最终页面可见新版当前有效、旧版已失效、受控浏览落位新版、版本历史和升版原因可见。
- 本次实际结果记录发布策略未展示单独“生效申请”按钮，详情页“发布后续”显示“已完成”，发布文件和盖章文件均已生成。
- API/数据库若使用，仅记录最终只读状态和不敏感的业务标识；不记录密码、token、Cookie、签名密钥或完整敏感载荷。
- 失败时区分 `BLOCKED`（缺少账号、服务、数据或入口）与 `FAIL/RED`（前置齐备但真实页面行为失败）。

## Current Status

ready_for_closeout

## Design Constraints Check

- `是否引入 fallback/降级/吞异常`：否。
- `是否用 API/数据库代替真实页面写入`：否。
- `是否修改生产代码`：否，当前任务仅执行和记录真实 E2E。
- `是否清理非本任务数据`：否。
- `是否记录敏感凭据`：否。

## Cleanup Keep

- doc/tasks/20260918-dcc-major-version-upgrade-e2e/task.md
- doc/tasks/20260918-dcc-major-version-upgrade-e2e/execution-log.md
- doc/tasks/20260918-dcc-major-version-upgrade-e2e/verification-report.md
- doc/tasks/20260918-dcc-major-version-upgrade-e2e/start-temp-int-qms-backend.ps1
- doc/tasks/20260918-dcc-major-version-upgrade-e2e/approve-initial-version-real.e2e.cjs
- doc/tasks/20260918-dcc-major-version-upgrade-e2e/bind-category-directory-real.e2e.cjs
- doc/tasks/20260918-dcc-major-version-upgrade-e2e/configure-temp-admin-signature-real.e2e.cjs
- doc/tasks/20260918-dcc-major-version-upgrade-e2e/configure-temp-four-stage-route-real.e2e.cjs
- doc/tasks/20260918-dcc-major-version-upgrade-e2e/create-product-project-real.e2e.cjs
- doc/tasks/20260918-dcc-major-version-upgrade-e2e/dcc-major-version-upgrade-real.e2e.cjs
- doc/tasks/20260918-dcc-major-version-upgrade-e2e/export-admin-config-real.e2e.cjs
- doc/tasks/20260918-dcc-major-version-upgrade-e2e/find-template-project-real.e2e.cjs
- doc/tasks/20260918-dcc-major-version-upgrade-e2e/import-temp-upload-permission-real.e2e.cjs
- doc/tasks/20260918-dcc-major-version-upgrade-e2e/prepare-task-owned-project-real.e2e.cjs
- doc/tasks/20260918-dcc-major-version-upgrade-e2e/prepare-temp-upload-permission-package.cjs
- doc/tasks/20260918-dcc-major-version-upgrade-e2e/probe-project-template-ui.e2e.cjs
- doc/tasks/20260918-dcc-major-version-upgrade-e2e/artifacts/category-directory-binding.json
- doc/tasks/20260918-dcc-major-version-upgrade-e2e/artifacts/dcc-approval-pdf-upload-policy-seed.sql
- doc/tasks/20260918-dcc-major-version-upgrade-e2e/artifacts/gxp-audit-policy-seed.sql
- doc/tasks/20260918-dcc-major-version-upgrade-e2e/artifacts/initial-approval-real.json
- doc/tasks/20260918-dcc-major-version-upgrade-e2e/artifacts/initial-approval-step-1.png
- doc/tasks/20260918-dcc-major-version-upgrade-e2e/artifacts/product-project.json
- doc/tasks/20260918-dcc-major-version-upgrade-e2e/artifacts/prepared-project.json
- doc/tasks/20260918-dcc-major-version-upgrade-e2e/artifacts/real-e2e-result.json
- doc/tasks/20260918-dcc-major-version-upgrade-e2e/artifacts/99-final-detail.png
- doc/tasks/20260918-dcc-major-version-upgrade-e2e/artifacts/temp-admin-signature-real.json
- doc/tasks/20260918-dcc-major-version-upgrade-e2e/artifacts/temp-four-stage-route-real.json
- doc/tasks/20260918-dcc-major-version-upgrade-e2e/artifacts/temp-upload-permission-import-real.json
- doc/tasks/20260918-dcc-major-version-upgrade-e2e/artifacts/dcc-file-directory-pre-unclassified-20260919-052100.sql
