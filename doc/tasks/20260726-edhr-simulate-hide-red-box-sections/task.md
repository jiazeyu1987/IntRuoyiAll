# Task: 隐藏 eDHR 模拟填写页红框区域

## Task Goal

隐藏用户截图红框中的模拟填写页内容：工序与模板标题、模板摘要信息、左侧“模板内填写”说明，以及左侧规则图例；保留返回按钮、右侧“表单显示”、左右模板内容和模拟填写能力。

## Milestones

- [x] 确认截图目标与对应组件边界。
- [x] 建立聚焦静态合同并完成 RED。
- [x] 实现页面级隐藏并完成 GREEN。
- [ ] 完成相关静态合同、类型检查和构建回归。
- [ ] 完成清理、提交和推送。

## Expected Verification

- `node tests/e2e/edhr-batch-template-simulate-red-box-hidden-static.spec.js`
- `node tests/e2e/edhr-batch-template-simulate-static.spec.js`
- `node tests/e2e/edhr-batch-template-simulate-return-static.spec.js`
- `pnpm ts:check`
- `pnpm build:local`

## Current Status

blocked

阻塞项：`pnpm build:local` 在加载 Vite 配置时找不到 `@babel/helper-validator-identifier`；锁文件有该依赖，但现有 `node_modules/.pnpm/@babel+helper-validator-identifier@7.25.9/node_modules/@babel/helper-validator-identifier` 目录为空。影响：无法完成本地生产构建验证；聚焦静态合同和类型检查已通过。

## 经验门禁

- 已读取 `docs/experience-index.md`。
- 命中 `docs/frontend-development.md#前端静态契约隔离门禁`：使用聚焦静态合同锁定本次隐藏范围，不修改无关页面逻辑。
- 命中 `docs/e2e-rules.md#静态合同与真实-e2e-同步门禁`：同步调整模拟填写页既有静态合同，避免保留已废弃可见文案断言。
- 已读取统一前端样式规范；本次遵循用户明确截图要求，不引入额外视觉体系。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，通过共享组件显式展示开关限定模拟页行为，避免页面深层 CSS 覆盖。
- `是否存在临时补丁或绕过`：否。
