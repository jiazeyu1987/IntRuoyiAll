# 20260730 eDHR 批记录页面关系图页签

## Task Goal

在现有 eDHR 批记录页签中新增“批记录页面关系图”，使用类似流转关系图的节点/连线视图展示批记录相关操作页面之间的关系；每个节点代表一个页面或业务入口，不代表工艺路线工序，也不写回工艺路线配置。

## Milestones

- [x] 建立任务记录、读取前端与任务门禁
- [x] 增加静态合同覆盖页签、路由和页面节点
- [x] 新增批记录页面关系图页面和共享页签接入
- [x] 运行目标静态验证和相邻页签验证
- [x] 更新验证报告和收尾状态

## Expected Verification

- `node tests/e2e/edhr-batch-page-graph-tab-static.spec.js`
- `node tests/e2e/edhr-frontline-fill-tabs-static.spec.cjs`
- `pnpm ts:check`
- 官方登录预检与 Playwright 真实页面路径验证

## Current Status

ready_for_closeout

## E2E Result

- `批记录页面关系图` 页签、关系图页面、节点和页面路由：PASS。
- 完整业务流程：`GRAPH_PASS_DOWNSTREAM_BLOCKED`。
- 下游 blocker：生产填写与 PQC填写页面缺少设备账号工艺路线绑定来源，页面提示无法加载一线报工上下文。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；新增独立页面关系图，不复用或污染工艺路线流转关系图配置。
- `是否存在临时补丁或绕过`：否。

## Applicable Gates

- 工艺路线三类配置术语契约：本任务只展示“页面关系”，不得把批记录表单、表单槽位或工序开始配置混用。
- 前端静态契约隔离门禁：新增任务专用静态合同覆盖页签、路由、组件和节点关系，不依赖全量前端检查作为唯一证据。
- 当前工作区已有并行任务状态，本任务只修改明确相关的前端文件、测试和任务记录。

## Closeout Blocker

- 实现与定向验证已完成，但当前分支领先 `origin/int_main`，且包含非本任务并行提交。
- 为避免把非本任务提交一起推送，本任务未执行最终 closeout push，状态停在 `ready_for_closeout`。
- 真实 E2E 发现生产填写/PQC填写的下游一线报工上下文前置未接入，因此不能将完整流程记录为 PASS。
