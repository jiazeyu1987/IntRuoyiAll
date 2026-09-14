# 辅助表格映射预览紧凑显示

## Task Goal

按截图反馈调整辅助表格映射预览：已映射单元格字段名不换行，超出宽度显示省略号；删除格内字段类型圆标和独立“取消映射”按钮；通过双击已映射辅助格取消映射。

## Milestones

- [x] 创建任务目录并读取前端、E2E、任务收尾、PowerShell 编码和经验门禁。
- [x] 记录 BDD 场景和当前脏工作区边界。
- [x] 先更新静态合同并取得 RED。
- [x] 修改两个辅助映射预览组件的 DOM、交互和样式。
- [x] 运行定向 GREEN、相邻回归和类型检查。
- [x] 更新证据、执行经验沉淀与收尾。

## Expected Verification

- `node tests/e2e/assist-grid-per-user-mapping-static.spec.js`
- `node tests/e2e/edhr-visual-fill-config-static.spec.js`
- `node tests/e2e/form-template-fill-config-assist-mode-static.spec.js`
- `pnpm ts:check`

## Applicable Experience Gates

### 前端静态契约隔离门禁

- Trigger: 辅助表格映射预览 DOM 和交互调整。
- Preflight check: 使用聚焦静态合同锁定不换行省略、去除按钮/类型圆标、双击取消映射。
- Blocker: 静态合同无法先 RED 后 GREEN，或必须扩大改动到无关页面才能通过。
- Verification: 逐条运行目标静态合同并记录 RED/GREEN。
- Forbidden action: 禁止只改视觉截图、不覆盖取消映射交互。

### eDHR 辅助模式当前工序 assistRows 路由门禁

- Trigger: 涉及辅助表格预览、`data-assist-grid-cell` 和映射取消。
- Preflight check: 只改变预览显示与取消映射入口，不改变正式 `assistRows` / rowKey 协议。
- Blocker: 如果改动影响保存 payload、rowKey 解析或辅助表格配置来源，必须停止并重新设计。
- Verification: 相邻静态合同仍证明辅助表格可点击、可建立映射、可取消映射。
- Forbidden action: 禁止用默认字段、空布局或额外兼容分支替代正式辅助格映射。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，收敛同一预览 UI 的模板、交互和样式规则。
- `是否存在临时补丁或绕过`：否。

## Current Status

completed

## Cleanup Keep

- doc/tasks/20260729-assist-grid-preview-compact-unmap/frontend-feature-evidence.md
