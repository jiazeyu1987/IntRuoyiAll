# 加载第三方报工导入修复到本地运行态

## Task Goal

确认用户在 `芋道源码/admin` 本地路径测试第三方报工导入后列表仍不更新的原因，并将已实现的第三方报工导入修复加载到当前 `E:\IntRuoyi` 本地后端 `48081` 运行态。

## Milestones

1. 核对当前 `48081` Jar 是否包含修复。`completed`
2. 停止归属明确的旧后端运行态，重新打包 `yudao-server`。`completed`
3. 重新启动本地后端并验证 health。`completed`
4. 运行真实导入验证，确认报工列表与排产工单进度更新。`completed`
5. 记录验证证据并收尾。`completed`

## Expected Verification

- 当前旧 Jar 缺少 `ThirdPartyFeedbackImportServiceImpl$DirectWorkstationResolution.class` 的原因证据。
- 新打包 Jar 包含 `ThirdPartyFeedbackImportServiceImpl$DirectWorkstationResolution.class`。
- `http://127.0.0.1:48081/actuator/health` 返回 `UP`。
- 使用真实前端路径导入 `C:\Users\BJB110\Desktop\文档\李萍.xlsx` 后，正式报工列表和排产工单进度更新。

## Current Status

ready_for_closeout

实现和本机真实导入验证已完成；当前仓库存在大量非本任务脏改动与本地 ahead 提交，未执行最终提交/推送式 closeout，避免混入并行任务改动。

## Applicable Experience Gates

- `docs/backend-development.md#第三方报工直报正式链路门禁`：导入成功必须落到正式报工，不得用导入记录直接进度、前端假新增、默认成功或空列表刷新替代正式报工持久化链路。
- `docs/local-runtime.md#2026-07-24-隔离构建-Jar-加载门禁`：本机 E2E 前必须核对运行中 Jar 是否加载本次修复，不能只看源码或 health。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，本任务加载已实现的正式报工链路修复到当前本地运行态。
- `是否存在临时补丁或绕过`：否。
