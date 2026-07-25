# eDHR 损耗单打开动作修复

## Task Goal

修复 eDHR 批次执行详情页选择“损耗单”时错误触发跳过逻辑，导致提示“必填路线表单不允许跳过”的问题。预期在关闭前、可填写状态下，点击损耗单应打开填写表单，而不是调用跳过接口。

## Milestones

- [x] 复现并定位损耗单卡片点击动作分派错误
- [x] 先补充静态回归测试覆盖必填动态表单不得走跳过接口
- [x] 实施最小前端修复，保持现有批次详情交互契约
- [x] 运行目标回归验证并记录证据
- [ ] 完成收尾检查和任务文档更新

## Expected Verification

- `node tests\e2e\<focused-static-test>.spec.js` 先 RED 后 GREEN
- 相关 eDHR 批次详情静态回归测试通过
- `git diff --check` 通过

## Current Status

ready_for_closeout

## 经验门禁

- `eDHR 详情回填门禁`：动态表单/损耗单必须核对正式详情任务字段和后端规则来源，不得只改前端显示文案或用前端兜底掩盖缺失来源。
- `静态合同与真实 E2E 同步门禁`：窄缺陷优先新增聚焦静态合同；读取源码时归一化 CRLF，不用宽合同失败牵动无关逻辑。
- `eDHR 单据填写人显示值门禁`：本次不修改填写人展示；若后续真实 E2E 涉及卡片填写人，必须以详情接口 `fillableUsers` 为准。
- `eDHR 路线表单跳过口径门禁`：只有 `requiredPolicy === 'OPTIONAL'` 且后端 `allowedActions` 包含 `SKIP` 时，前端才允许跳过表单；损耗单等必填路线表单点击应进入打开填写路径。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，目标是修正点击动作路由，不绕过后端必填校验。
- `是否存在临时补丁或绕过`：否。

## Cleanup Keep

- `doc/tasks/20260725-edhr-loss-form-open-action/task.md`
- `doc/tasks/20260725-edhr-loss-form-open-action/execution-log.md`
- `doc/tasks/20260725-edhr-loss-form-open-action/verification-report.md`
