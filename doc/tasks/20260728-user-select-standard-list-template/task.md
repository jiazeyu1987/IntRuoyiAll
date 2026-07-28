# 人员选择弹窗标准列表模板

## Task Goal

将人员选择弹窗红框内的用户列表区域改成项目标准列表模板，保留现有用户筛选、部门树、选择和确认行为。

## Milestones

- [x] 创建任务目录并读取前端、任务收尾、PowerShell 编码和前端标准样式门禁。
- [x] 定位人员选择弹窗组件和现有标准列表模板用法。
- [x] 先补充静态合同，锁定人员选择列表必须接入标准列表模板。
- [x] 修改前端组件，使用标准列表模板承载红框内列表区域。
- [x] 运行目标静态合同和相关前端验证。
- [ ] 更新任务文档、验证报告并完成提交推送。

## Expected Verification

- `node IntRuoyiFronted/tests/e2e/user-select-standard-list-template-static.spec.js`
- 受影响范围可行时运行前端类型或相邻静态合同验证。
- `git diff --check`

## Current Status

ready_for_closeout

## 经验门禁

### 前端标准列表样式门禁

- Trigger: 前端页面、表格、列表模板或样式调整。
- Preflight check: 读取 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`，以 IntPP 生产工单列表作为统一列表视觉基线。
- Blocker: 标准样式文件缺失、请求设计与标准样式冲突且未获用户明确覆盖。
- Verification: 静态合同检查列表接入 `UnifiedListTemplate`、列配置、显示字段保存和标准表格结构。
- Forbidden action: 禁止引入一套无关视觉体系、悬浮卡片式列表或隐藏请求失败。
- Evidence: `docs/experience-index.md` 路由“前端页面 / 表格 / 样式”。

### 前端静态合同隔离门禁

- Trigger: 当前任务需要 RED/GREEN 静态合同，但全量前端检查可能被无关历史问题阻塞。
- Preflight check: 使用任务专用最小静态合同覆盖当前人员选择列表模板行为。
- Blocker: 无法证明静态合同稳定 RED/GREEN，或合同未覆盖本次用户可见改动。
- Verification: 记录 RED/GREEN 命令、失败原因和通过结果。
- Forbidden action: 禁止修改无关大契约绕过历史失败，禁止只靠截图宣称完成。
- Evidence: `docs/frontend-development.md#前端静态契约隔离门禁`。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，改用项目已有标准列表模板而不是局部样式补丁。
- 是否存在临时补丁或绕过：否。

## Cleanup Keep

- doc/tasks/20260728-user-select-standard-list-template/frontend-feature-evidence.md
