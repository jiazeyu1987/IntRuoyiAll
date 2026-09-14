# QA 规程 PDF 字段对齐修复

## Task Goal

核对并锁定 QA 规程“工序检验方法与抽样方案”列表与 `PQC-IDI-001（B 0）按压式球囊扩充压力泵组装过程检验规程--2026.01.04生效.pdf` 的列归属一致性，防止把 PDF “接受标准”栏内容误搬到“检验方法”栏。

## Milestones

- [x] M1 建立任务记录、保存进入本任务前的脏工作区基线
- [x] M2 复查 PDF 扫描页与 QA 模板字段归属
- [x] M3 添加压力泵气密性字段对齐静态合同
- [x] M4 运行聚焦回归验证并记录证据
- [x] M5 执行 cleanup、沉淀经验并完成提交推送

## Expected Verification

- `node tests/e2e/qa-regulation-pressure-pump-pdf-field-alignment-static.spec.cjs`
- `node tests/e2e/role-matrix-qa-regulation-tab-static.spec.cjs`
- `git diff --check -- IntRuoyiFronted/tests/e2e/qa-regulation-pressure-pump-pdf-field-alignment-static.spec.cjs doc/tasks/20260806-qa-regulation-pdf-field-alignment/task.md doc/tasks/20260806-qa-regulation-pdf-field-alignment/execution-log.md doc/tasks/20260806-qa-regulation-pdf-field-alignment/verification-report.md`

## Current Status

completed

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，复查 PDF 原始列归属后以静态合同锁定，避免错误修改正式 QA 模板。
- `是否存在临时补丁或绕过`：否。

## Experience Gate

- 适用门禁：`docs/frontend-development.md#前端静态契约隔离门禁`，本任务使用专用静态合同锁定当前 PDF 字段归属，避免扩大到无关 QA 页面逻辑。
- 适用门禁：`docs/powershell-memory.md#共享分支并发基线提交门禁` 与 `#同文件并行改动选择性暂存门禁`，当前分支存在并行提交和残余改动，本任务仅选择性提交专用测试与任务记录。
- 经验沉淀：已调用 `project-experience-consolidation` 技能并检查既有归宿；`docs/frontend-development.md` 与 `docs/experience-index.md` 当前已有并行非本任务改动，为避免同文件混入，本次不追加长期经验文档，经验保留在本任务记录。
