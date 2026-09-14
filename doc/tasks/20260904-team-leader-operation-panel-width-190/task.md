# Team Leader Operation Panel Width 190

## Task Goal
生产组长报工列表操作面板固定宽度调整为 190。

## Milestones
- [x] 记录 BDD 与验收边界
- [x] 调整操作列宽度
- [x] 运行定向静态验证
- [ ] 提交并推送

## Expected Verification
- node IntRuoyiFronted/tests/e2e/team-leader-production-report-payload-columns-static.spec.cjs
- pnpm exec vue-tsc --noEmit --pretty false
- git diff --check

## Current Status
blocked

## 设计约束检查
- 仅调整生产组长操作列宽度，不改业务按钮、权限或接口。
- 生产页签操作列直接固定为 190，避免旧用户列宽配置继续生效。
- 保留现有多物料展开详情展示逻辑。

