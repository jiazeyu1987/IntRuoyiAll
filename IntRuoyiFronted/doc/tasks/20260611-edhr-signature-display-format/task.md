# 20260611-edhr-signature-display-format

## 任务目标

修正 eDHR 签名记录页显示：动作列展示签名动作对应的中文映射，签名时间按年月日格式展示，不再直接显示动作编码和毫秒时间戳。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。未知动作按原始编码显式展示，时间缺失保持空值，不伪造默认成功值。
- 是否从根因和长期维护角度解决：是。新增统一展示函数，页面和测试围绕用户可见表格行为验证。
- 是否存在临时补丁或绕过：否。本次仅修改签名记录页显示和对应测试/证据。

## BDD 场景

- BDD: 签名记录动作显示中文 -> Given 后端返回 actionType 为 `APPROVE`、`SUBMIT`、`FORM_REVIEW`、`FIELD_CHANGE` 的真实签名记录 / When 用户打开 eDHR 签名记录页 / Then 动作列显示 `审批通过`、`提交审批`、`表单复核`、`字段变更`，不直接显示英文动作编码。
- BDD: 签名记录时间显示年月日 -> Given 后端返回 `signedAt` 为毫秒时间戳或可解析时间 / When 用户查看签名记录列表 / Then 签名时间显示为 `YYYY年M月D日` 格式，不显示原始毫秒值。

## 里程碑

- [x] M1：补充 RED 测试，证明当前表格仍展示英文动作和原始毫秒时间戳。
- [x] M2：实现签名记录动作中文映射和年月日日期格式。
- [x] M3：运行静态/类型/证据验证，更新任务文档并完成 closeout preview。
- [x] M4：按当前任务范围提交前端仓库改动。

## 预期验证

- `node tests/e2e/edhr-tracking-signature-real-flow.e2e.js --static-display-format`
- `pnpm ts:check`
- `node --check tests/e2e/edhr-tracking-signature-real-flow.e2e.js`
- `node tests/e2e/edhr-tracking-signature-real-flow.e2e.js`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260611-edhr-signature-display-format/frontend-feature-evidence.md`
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260611-edhr-signature-display-format --mode preview --worktree-closeout off`

## 当前状态

- 状态：已完成，待随本次提交入库。
- 已完成：确认前端最近任务已完成；锁定签名记录页和既有真实 E2E；完成 RED 静态检查、页面显示实现、静态 GREEN、类型检查、脚本语法检查、真实 Playwright E2E、证据校验和 closeout preview。
- 阻塞：无。

## 完成记录

- RED: `node tests\e2e\edhr-tracking-signature-real-flow.e2e.js --static-display-format` -> FAIL，确认旧页面缺少动作中文映射。
- GREEN: `node tests\e2e\edhr-tracking-signature-real-flow.e2e.js --static-display-format` -> PASS。
- GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS。
- GREEN: `node --check tests\e2e\edhr-tracking-signature-real-flow.e2e.js` -> PASS。
- GREEN: `node tests\e2e\edhr-tracking-signature-real-flow.e2e.js` -> PASS，真实测试租户 Playwright 路径验证签名记录页展示与动作筛选。
- GREEN: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc\tasks\20260611-edhr-signature-display-format\frontend-feature-evidence.md` -> PASS。
- CLOSEOUT: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260611-edhr-signature-display-format --mode preview --worktree-closeout off` -> PASS，blocked/warnings 均为 `<none>`；证据文件为本次正式验收证据，未执行删除。
