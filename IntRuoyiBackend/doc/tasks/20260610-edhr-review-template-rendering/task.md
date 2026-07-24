# eDHR 复盘模板渲染后端任务

## 目标

在 eDHR 批次复盘接口中补齐单表复盘所需的模板布局数据，使前端可以按电子批记录原始模板网格展示已填写结果，而不是只能展示字段清单。

## 里程碑

- [x] RED：确认当前复盘响应的 `formViewModel` 未显式返回 `sheetLayoutJson` / `metaJson`，无法稳定承载模板渲染数据。
- [x] GREEN：扩展响应 VO 和服务组装逻辑，返回执行记录保存的模板布局、元数据、快照和值。
- [x] REGRESSION：运行目标单元测试，确认复盘接口仍返回批次任务、签名、审批、归档和已填写记录。

## 预期验证

- `MesProEdhrBatchExecutionServiceTest#getReviewTimeline_returnsBatchTasksSignaturesAndArchives` 通过。
- 接口不新增数据库表，不改现有状态机，不影响 eDHR 填写、签名、审批、归档流程。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，复盘接口显式返回模板渲染所需数据，由前端按模板模型渲染。
- `是否存在临时补丁或绕过`：否。

## 当前状态

已完成。目标测试通过，未改数据库表和状态机。
