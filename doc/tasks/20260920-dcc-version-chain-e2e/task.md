# 20260920 DCC 版本链完整 E2E

## Task Goal

使用真实 Playwright 页面，在当前 `int_qms` 运行态完成任务自有 DCC 版本链：

1. 当前正式版本 `A/1` 通过真实页面检出并以小版本检入生成 `A/2`。
2. 从指定历史小版本 `A/2` 通过真实页面发起大版本，生成 `B/1`。
3. 完成 `B/1` 的真实审批，确认其替代旧正式版本并成为当前有效版本。
4. 在版本历史真实页面核验来源版本、来源/目标哈希、操作者、审批结果和审批/发布状态。

API/数据库仅允许被动网络证据和最终只读核验，不得承担任何业务写入、检出、检入、审批、发布或状态变更动作。

## BDD

BDD: DCC A/1 to A/2 minor then historical A/2 to B/1 major ->
Given 当前租户存在任务自有的 `A/1` 当前正式版本，且页面可进入检出、检入、审批和版本历史
And Playwright 使用真实账号 `芋道源码/admin` 完成登录
When 用户从 `A/1` 真实页面检出并选择小版本检入
Then 页面生成 `A/2`，`A/2` 成为当前正式版本且保留 `A/1` 历史
When 用户从指定历史 `A/2` 真实页面发起大版本并完成审批
Then 页面生成 `B/1`，`B/1` 成为当前正式版本并替代 `A/2`
And 版本历史真实显示来源版本、前后哈希、操作者、审批结果和发布/生效结果

## Milestones

- [x] M1 任务记录、运行态、入口和账号前置确认
- [x] M2 任务自有 `A/1` 数据和指定历史小版本入口确认
- [x] M3 真实页面完成 `A/1 -> A/2` 小版本
- [x] M4 真实页面从指定 `A/2` 完成 `B/1` 大版本创建
- [x] M5 真实页面完成 `B/1` 审批并确认替代旧正式版本
- [x] M6 版本历史页面核验来源、哈希、操作者、审批结果
- [x] M7 最终只读核验、证据整理和任务收尾

## Expected Verification

- 分支和运行态保持 `int_qms`：前端 `8061`、后端 `48061`，不使用 `8081/48081`。
- 所有被验收业务动作由 Playwright 真实页面完成。
- 页面可见 `A/1 -> A/2 -> B/1` 版本链，且 `B/1` 审批后替代旧正式版本。
- 版本历史页面可见来源版本、来源/目标哈希、操作者、审批结果和发布时间/生效结果。
- 记录真实页面 URL、请求方法/状态/业务码、版本号、文件标识和截图路径。
- 失败区分 `BLOCKED`（入口、账号、服务或数据缺失）与 `FAIL/RED`（前置齐备但真实页面行为失败）。

## Current Status

ready_for_closeout

## Design Constraints Check

- `是否引入 fallback/降级/吞异常`：否。
- `是否用 API/数据库代替真实页面写入`：否。
- `是否修改生产代码`：否，当前任务只执行和记录真实 E2E。
- `是否清理非本任务数据`：否。
- `是否记录敏感凭据`：否。

## Cleanup Keep

- doc/tasks/20260920-dcc-version-chain-e2e/create-initial-version-real.e2e.cjs
- doc/tasks/20260920-dcc-version-chain-e2e/minor-version-real.e2e.cjs
- doc/tasks/20260920-dcc-version-chain-e2e/submit-minor-approval-real.e2e.cjs
- doc/tasks/20260920-dcc-version-chain-e2e/artifacts/initial-version-chain-real.json
- doc/tasks/20260920-dcc-version-chain-e2e/artifacts/minor-version-real.json
- doc/tasks/20260920-dcc-version-chain-e2e/artifacts/submit-minor-approval-real.json
- doc/tasks/20260920-dcc-version-chain-e2e/artifacts/a2-approval-real.json
- doc/tasks/20260920-dcc-version-chain-e2e/artifacts/major-version-real.json
- doc/tasks/20260920-dcc-version-chain-e2e/artifacts/final-detail.png
