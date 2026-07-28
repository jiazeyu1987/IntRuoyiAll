# 测试管理串行路线筛选本地同步

## Task Goal

将已集成到 `origin/int_main` 的测试管理 `串行路线` 常驻下拉同步到当前运行工作区 `E:\IntRuoyi`，解决页面看不到红框下拉的问题。

## Milestones

- [x] 确认当前运行工作区落后远端主线且缺少红框下拉。
- [x] 更新聚焦静态合同，复现当前工作区缺少常驻下拉。
- [x] 同步最小前端改动，不触碰并行 eDHR 任务文件。
- [x] 运行聚焦验证并记录结果。

## Expected Verification

- `系统管理 > 测试管理` 快速筛选右侧显示 `串行路线` 下拉框。
- 选择串行路线后通过 `queryParams.nodeChainName` 查询对应节点。
- 清空下拉后恢复其他筛选条件下的列表。
- 不影响现有节点串列、项目筛选、测试租户和执行按钮。

## Current Status

ready_for_closeout

## Closeout Blocker

- 当前 `E:\IntRuoyi` 本地 `int_main` 为 `08c3eae0`，`origin/int_main` 为 `fd05b0da`，同时存在大量并行任务脏改动；本次只做本地可见性同步和验证，不提交、不推送、不清理并行文件。
- 正式远端主线已经包含同等生产代码；本地运行态若仍看不到，需要刷新浏览器或等待当前 Vite HMR 完成。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，同步已验证的正式 `nodeChainName` 查询控件。
- `是否存在临时补丁或绕过`：否。
