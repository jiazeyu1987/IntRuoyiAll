# 表单模板填写配置辅助映射模式

## Task Goal

让“表单模板”页签下的“填写配置”逻辑与“批记录表单”的填写配置逻辑保持一致，支持切换到辅助表单映射模式，并继续使用表单模板自身的数据链路保存填写配置。

## Milestones

- [x] 记录 BDD 场景和严格 TDD 验证路径。
- [x] 增加表单模板填写配置辅助映射模式的 RED 静态契约。
- [x] 实现最小前端修复，保证模板填写配置可切换辅助表单映射模式。
- [x] 运行相关静态契约、类型检查和前端特性证据校验。
- [x] 更新任务文档和验证报告。

## Expected Verification

- `node tests/e2e/form-template-fill-config-assist-mode-static.spec.js`
- `node tests/e2e/form-template-fill-config-static.spec.js`
- `node tests/e2e/assist-grid-per-user-mapping-static.spec.js`
- `node tests/e2e/form-template-button-interaction-parity-static.spec.js`
- `node tests/e2e/form-template-independent-button-actions-static.spec.js`
- `pnpm ts:check`

## Current Status

completed

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，目标是让表单模板填写配置直接具备批记录填写配置同等的辅助映射入口和交互结构，不用旁路逻辑。
- `是否存在临时补丁或绕过`：否。

## Applicable Experience Gates

- 前端静态契约隔离门禁：本任务使用专用 `form-template-fill-config-assist-mode-static.spec.js` 证明当前缺口 RED/GREEN，不改无关大契约绕过历史失败。
- 表单模板三按钮领域边界门禁：本任务只对齐填写配置交互，不引入 `batchRecordReportId`、MES 批记录 API 或批记录路由依赖，保存仍使用当前模板上下文。

## Cleanup Keep

- `doc/tasks/20260728-form-template-fill-config-assist-mode/frontend-feature-evidence.md`
