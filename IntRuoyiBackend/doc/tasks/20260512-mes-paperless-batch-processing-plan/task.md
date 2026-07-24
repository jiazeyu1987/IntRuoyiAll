# Task: MES 无纸化批处理方案收敛

## Goal

把 MES 无纸化批处理方案收敛到“第一期可以直接开始写代码”的质量，且第一期范围固定为模板导入、预览、列表、基础编辑、删除。

## Scope

- 固化第一期与后续阶段边界；
- 明确 `.doc` 为第一期硬要求，且不允许人工前置转换；
- 补齐后端第一期数据模型、API、类清单、解析链职责；
- 校准前端第一期切片，使其与后端第一期完全一致；
- 给出是否可以开始写代码的评审结论与剩余风险。

## Milestones

- [x] M1: 既有任务文档存在且上一后端任务已完成，不阻塞本次方案收敛。
- [x] M2: 在原任务目录内继续记录本次方案收敛工作。
- [x] M3: 识别当前方案中的一期/后续期混淆点与 `.doc` 要求缺口。
- [x] M4: 重写后端第一期方案，固定数据模型、API、类清单和解析链职责。
- [x] M5: 重写前端第一期方案，只保留模板导入、预览、列表、基础编辑、删除。
- [x] M6: 更新评审结论，明确当前是否已经可以开始写代码。

## Expected Verification

- 后端文档明确第一期只做模板导入与模板管理；
- 前端文档第一期与后端第一期完全一致；
- 试点 `.doc` 文件被写为第一期强制验收对象；
- review 文档给出明确的 go / no-go 结论与剩余风险。

## Current Status

Completed. The task record, execution evidence, and backend batch-processing plan remain under `doc/tasks/20260512-mes-paperless-batch-processing-plan/`, and the Phase 1 hardening conclusion explicitly states that coding can begin.
