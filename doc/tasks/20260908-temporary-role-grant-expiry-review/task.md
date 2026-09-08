# 临时角色授权到期自动撤销、到期提醒与权限审查归集

## Task Goal

在既有 1.10「临时权限管理失控」最小闭环上补齐三个控制点：到期自动撤销、到期前提醒、定期权限审查列表归集。实现仍保持临时授权与永久角色分离，不通过长期 `system_user_role` 兜底。

## Milestones

1. BDD/TDD：已补充到期自动撤销、到期提醒、权限审查归集场景和失败/通过证据。
2. 数据模型与迁移：已新增 `remind_time`、提醒索引、提醒 Job 与站内信模板，审查分类按当前时间归集。
3. 后端：已补齐提醒扫描、审查归集查询接口和测试；既有到期扫描测试保持通过。
4. 前端：已在临时角色授权页面提供审查归集筛选、统计卡片、提醒时间列。
5. 验证：后端定向测试、SQL pytest、前端静态合同、本任务前端文件级类型/ESLint、前端全量 ts:check 已完成。

## Expected Verification

- `mvn.cmd -pl yudao-module-system '-Dtest=TemporaryRoleGrantServiceImplTest,PermissionServiceTest' '-Dsurefire.failIfNoSpecifiedTests=false' test`
- `python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_system_temporary_role_grant_sql.py -q`
- `node IntRuoyiFronted\tests\e2e\system-temporary-role-grant-static.spec.js`
- `pnpm --dir IntRuoyiFronted ts:check`
- 相关 evidence validator PASS。

## Current Status

completed：实现、全部预期验证、evidence validator、cleanup preview/apply、经验沉淀和 Git closeout 已完成；本次仅提交任务记录，代码文件在当前 `int_main` 已无未提交 diff。

## 设计约束检查

- 不修改 admin，admin 仍只作为测试身份。
- 临时授权继续独立于永久 `system_user_role`。
- 到期自动撤销必须产生审计事件。
- 到期提醒必须可追踪，不允许静默吞掉通知失败。
- 权限审查归集必须能区分仍有效、即将到期、异常逾期。
