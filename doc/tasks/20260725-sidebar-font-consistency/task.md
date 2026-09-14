# 20260725 Sidebar Font Consistency

## Task Goal

将前端所有侧边栏页签字体固定为参考图 2 的字体观感与粗细，修复 115 浏览器中菜单字体显示偏细/不一致的问题。

## Milestones

- [x] 创建任务记录并记录 BDD/TDD 约束
- [x] 定位侧边栏菜单字体样式入口
- [x] 先补静态失败验证，再实施最小样式修复
- [x] 运行受影响范围验证并记录证据
- [ ] 收尾记录与最终状态更新

## Expected Verification

- 静态测试覆盖侧边栏菜单标题的字体族与字重约束。
- 受影响前端验证命令通过。
- 样式改动不引入 fallback、降级或吞异常。

## Current Status

ready_for_closeout

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，目标是在侧边栏菜单样式源头固定字体族与字重。
- `是否存在临时补丁或绕过`：否。

## 经验门禁

- Trigger: 前端页面、表格、样式变更命中 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`。
- Preflight check: 样式实现前读取统一前端样式源，保持蓝/中性运营台风格，不做无关视觉重设计。
- Blocker: 用户要求与统一样式冲突且未记录原因，或样式改动引入 mock、fallback、降级、吞异常。
- Verification: 静态契约、`pnpm ts:check`、`pnpm build:local` 通过。
- Forbidden action: 禁止借字体修复调整菜单结构、路由、权限、接口或业务状态。
- Evidence: 本任务 `execution-log.md` 和 `frontend-feature-evidence.md`。

## Cleanup Keep

- `doc/tasks/20260725-sidebar-font-consistency/frontend-feature-evidence.md`
