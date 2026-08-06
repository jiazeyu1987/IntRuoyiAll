# QA 规程 PDF 字段对齐修复

## Task Goal

修复 QA 规程“工序检验方法与抽样方案”列表中与 `PQC-IDI-001（B 0）按压式球囊扩充压力泵组装过程检验规程--2026.01.04生效.pdf` 不一致的字段，重点确保气密性高压/低压检测的“检验方法”按 PDF 原文完整呈现。

## Milestones

- [ ] M1 建立任务记录、保存进入本任务前的脏工作区基线
- [ ] M2 用静态合同复现 PDF 与 QA 列表字段不一致
- [ ] M3 最小修改 QA 压力泵规程模板字段
- [ ] M4 运行聚焦回归验证并记录证据
- [ ] M5 更新收尾记录、沉淀经验并完成提交推送

## Expected Verification

- `node tests/e2e/qa-regulation-pressure-pump-pdf-field-alignment-static.spec.cjs`
- `node tests/e2e/role-matrix-qa-regulation-original-excerpt-real.e2e.cjs`
- `node tests/e2e/role-matrix-qa-regulation-tab-static.spec.cjs`
- `git diff --check`

## Current Status

in_progress

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，直接修正正式 QA 规程模板与回归合同。
- `是否存在临时补丁或绕过`：否。

## Experience Gate

- 待读取 `docs/experience-index.md` 后补齐适用门禁摘要。

