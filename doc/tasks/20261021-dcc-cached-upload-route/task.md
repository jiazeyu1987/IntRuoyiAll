# 缓存上传页跨页签误发请求

## Goal
修复进入 DCC 项目代码及其他页签时，缓存上传页继续发起产品校验导致错误提示的问题。

## Milestones
- 已定位上传页全局 route.fullPath watcher 与 KeepAlive 路由 path 缓存键。
- RED/GREEN：执行真实 watcher 回调，验证离开页面不请求、返回和自身查询参数变化仍刷新。
- 定向回归、清理并记录运行态边界。

## BDD
Given 上传页已选项目且被缓存，When 切到项目代码或其他页面，Then 不执行上传页产品、审批、会签和路由就绪刷新。
Given 返回原上传页或改变其查询参数，When 路由变化，Then 正常执行上述刷新，外来评审仍不加载会签。

## Expected Verification
Node 定向单元测试 RED/GREEN；已有产品身份合同回归；SFC 编译检查；git diff --check。

## 设计约束检查
仅限制缓存页面监听范围；缓存键与 AppView 的 route.path 一致；不吞异常、不改产品校验、不写数据库、不执行 E2E、不重启服务、不提交推送；保留其他任务改动。

## Current Status
blocked

实现、22 项定向验证及 cleanup preview/apply 通过；仅因当轮未授权 Git 提交推送，未标记 completed。
