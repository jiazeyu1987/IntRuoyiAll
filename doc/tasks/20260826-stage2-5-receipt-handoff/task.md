# Stage2.5 正式回执交接

## Task Goal

修复 Stage2.5 模拟流程在活跃订单完成回执已经生成后再次调用旧 dossier 三 writer 的问题。模拟流程必须读取正式 Flow4 completion receipt，调用流程6创建/复用批次执行，并只用 receipt 中的结果 ID 形成展示快照。

## Milestones

1. 复制并核对 Stage2.5 task-owned 文件和正式 receipt/Flow6接口。
2. 删除旧 dossier writer 依赖，改为 receipt evidence 映射。
3. 执行 RED/GREEN/REGRESSION、MES编译和差异检查。
4. 提交独立分支，等待主线程选择性融合。

## Expected Verification

- Stage2.5源码不再引用 `MesPqcReleaseDossierPort`、`MesPqcReleaseDossierPlan` 或 `MesPqcReleaseDossierWriteResult`。
- batch request 只使用正式 completion receipt 字段。
- snapshot 的批记录、过程检验和损耗链接来自 receipt，不产生二次写入。
- Stage2.5定向测试、MES编译、`git diff --check` 和 runtime guard 通过。

## Current Status

ready_for_closeout

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，移除重复写入边界。
- 是否存在临时补丁或绕过：否。
