# Task: 批记录 .doc 解析粒度修复

## Goal

修复 `.doc` 批记录模板导入时拆分粒度过粗的问题，使样本
`C:\Users\BJB110\Desktop\2\2\RE-PP-ID-01（A 1）球囊扩张压力泵生产记录(1).doc`
能够按逻辑工序表拆分，而不是只按当前过粗的物理结果返回少量候选模板。

## Scope

- 仅处理 `yudao-module-mes` 内批记录 `.doc` 模板解析链
- 优先修复 `.doc -> candidate templates` 的拆分粒度
- 不改 Phase 1/Phase 2 前端功能边界
- 不引入 silent fallback

## Milestones

- [x] M1: 确认前一项后端任务已完成。
- [x] M2: 在改代码前创建任务文档与执行日志。
- [x] M3: 记录 BDD 与 RED 证据。
- [x] M4: 修复 `.doc` 解析链并补 focused tests。
- [x] M5: 用真实样本做直接 HTTP 验证。
- [ ] M6: 更新状态并提交当前任务改动。

## Expected Verification

- 同一份压力泵 `.doc` 通过系统接口解析时返回的候选模板数显著增加
- 解析结果应至少覆盖 `产品信息 + 各工序记录` 的逻辑拆分
- focused tests 通过

## Current Status

Completed. `.doc` 解析链已改为稳定的 Python + win32com 转换后再走现有 docx 表格解析，真实样本接口返回 10 个候选表单。

## Final Verification

- `mvn -f D:\wt\rbt-be-clean\yudao-module-mes\pom.xml "-Dtest=MesProBatchRecordWordParserTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS
- Direct HTTP verification on `http://127.0.0.1:48083/admin-api/mes/pro/batch-record-template/import/parse` with the real pilot `.doc` -> PASS, `tableCount = 10`
