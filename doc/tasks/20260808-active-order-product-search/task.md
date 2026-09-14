# 新增活跃订单支持按产品搜索

## Task Goal

在生产组长工作台的“新增活跃订单”弹窗中，保留按订单号选择正式生产工单的提交契约，同时允许用户输入产品编码或产品名称检索可加入的已确认生产工单候选。

## Milestones

- [x] 记录活跃订单产品搜索的 BDD 场景和接口契约。
- [x] 后端候选接口支持同一 `keyword` 同时匹配生产工单号、产品编码和产品名称。
- [x] 前端弹窗文案清楚表达“订单号/产品”均可搜索，仍只提交选中的 `workOrderId`。
- [x] 完成后端聚焦测试、前端静态合同和证据校验。
- [ ] 追加真实页面只读 E2E，验证产品关键词可触发候选下拉且不发起新增写请求。

## Expected Verification

- `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderActiveOrderServiceTest,MesProcessPoolTeamLeaderControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `node tests/e2e/production-leader-active-order-pool-tab-static.spec.js`
- `node tests/e2e/team-leader-workbench-static.spec.cjs`
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260808-active-order-product-search/backend-api-evidence.md`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260808-active-order-product-search/frontend-feature-evidence.md`
- `node --check tests\e2e\team-leader-active-order-product-search-real.e2e.js`
- `node tests\e2e\team-leader-active-order-product-search-real.e2e.js`

## Current Status

blocked - 真实页面只读 E2E 已跑到“新增活跃订单”弹窗和候选接口，但当前 48081 运行态仍未加载本次产品搜索 mapper 方法；本轮已完成后端聚焦测试并生成仅替换本任务 class 的补丁 runtime Jar，直接停止/启动 48081 被本地安全策略拦截，暂不能把产品搜索实现加载进本机运行态复验。

## Experience Gate Summary

- `docs/backend-development.md#零排产活跃订单必须使用发布态正式路线`：候选资格和新增写入必须复用同一个正式路线来源解析契约；不得以默认路线、任取第一条绑定或前端文案放宽替代正式门禁。
- `docs/frontend-development.md#复合输入控件交互保留门禁`：修改远程下拉时必须保留原有远程搜索、候选 label/value、清空和正式提交身份，不得替换成纯输入或本地过滤。
- `docs/frontend-development.md#前端静态契约隔离门禁`：若大合同存在无关失败，必须使用当前需求的最小合同记录 RED/GREEN。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。本次仅扩展正式候选搜索字段，不改变加入活跃订单的正式前置门禁。
- `是否从根因和长期维护角度解决`：是。产品搜索在后端候选来源中完成，前端继续消费正式候选数组。
- `是否存在临时补丁或绕过`：否。
