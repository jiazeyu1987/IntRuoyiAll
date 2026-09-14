# 20260810-pqc-leader-default-tab

## Task Goal

点击左侧菜单“PQC组长”进入独立工作台时，页面内部默认选中“PQC管理”，而不是“人员管理”。

## Milestones

- [x] 建立任务记录并读取前端、编码、任务收尾规则。
- [ ] 用 BDD/TDD 静态合同锁定 PQC 组长默认页签需求。
- [ ] 修改前端默认页签逻辑并保持相邻 PQC/生产组长工作台隔离。
- [ ] 运行目标验证并记录结果。
- [ ] 更新验证报告和最终任务状态。

## Expected Verification

- node tests/e2e/pqc-leader-module-tabs-static.spec.js
- node tests/e2e/pqc-leader-personnel-tab-static.spec.js
- node tests/e2e/pqc-leader-default-management-tab-static.spec.js
- pnpm ts:check
- git diff --check

## Applicable Experience Gates

### 前端角色内容页签拆分口径门禁

- Trigger: 涉及 PQC组长、TeamLeaderWorkbenchPage、页面内部功能模块 Tab 和默认激活页签。
- Preflight check: 确认本需求是 PQC 组长页面内部 el-tabs 默认页签，不是动态菜单或 Office 工作簿页签；核对包装页、共享组件 props、页签 key、显示 gate 和相邻合同。
- Blocker: 默认页签改到错误角色、只改可见文案、未覆盖重复 tab 组、或相邻生产组长/PQC 组长合同被破坏时停止。
- Verification: 聚焦静态合同同时断言默认页签、tab label/key、包装页 props、显示 gate 和相邻工作台隔离；涉及 Vue/TS 时运行 pnpm ts:check。
- Forbidden action: 禁止用 CSS 隐藏、空数据、路由别名或错误承载物冒充默认进入 PQC 管理。

### 前端静态契约隔离门禁

- Trigger: 当前需求可用最小静态合同先 RED 再 GREEN。
- Preflight check: 新增任务专用静态合同，避免依赖全量历史测试先失败点。
- Blocker: 专用合同不能稳定先 RED 后 GREEN，或全量检查失败无法与当前任务隔离时停止。
- Verification: execution-log.md 记录 RED/GREEN 和全量回归剩余阻塞摘要。
- Forbidden action: 禁止跳过当前默认页签需求的最小 RED/GREEN。

### Element Plus 页签点击门禁

- Trigger: 真实页面验证涉及 Element Plus el-tabs。
- Preflight check: 若执行真实 E2E，应点击可见 role=tab 并断言 aria-selected=true 与目标内容可见。
- Blocker: 页签未选中或目标内容未渲染时停止。
- Verification: 本任务优先用静态合同锁定默认状态；如需真实 E2E，再按该门禁补充页面证据。
- Forbidden action: 禁止对隐藏 pane 强制点击或仅用 API 响应代替页签可见性。

## Current Status

in_progress

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，目标是修改共享组件的正式默认状态并补合同。
- 是否存在临时补丁或绕过：否。
