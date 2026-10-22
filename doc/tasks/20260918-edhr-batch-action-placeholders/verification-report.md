# Verification Report

## Scope

验证 eDHR 批次执行列表“上市放行”按钮的管理者代表权限、二次确认、负责人电子密码、最终放行 API、历史追溯跳转，以及 `admin` 绑定既有管理者代表角色的迁移合同。

## Results

- FRONTEND STATIC: `node tests/e2e/edhr-batch-action-placeholders-static.spec.cjs` -> PASS。
- SQL STATIC: `python -m pytest script/tests/test_mes_management_representative_admin_permission_sql.py` -> PASS，2 passed。
- BACKEND TARGET TEST: `mvn -pl yudao-module-mes -am '-Dtest=MesProEdhrReleaseServiceImplTest,MesReleaseFinalizationValidatorTest,MesReleaseAuthoritativeContextPortImplTest' '-Dsurefire.failIfNoSpecifiedTests=false' test` -> PASS，48 tests。
- TYPECHECK: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm exec vue-tsc --noEmit --pretty false -p tsconfig.relaxed.json` -> PASS。
- DIFF: `git diff --check` -> PASS；仅有 CRLF warning，无 whitespace error。

## Behavioral Evidence

- 前端“上市放行”按钮绑定 `mes:pro-edhr-release:approve`，无该权限不可见。
- 点击“上市放行”打开确认弹窗，必须输入负责人电子密码；空密码不调用放行接口。
- 提交后调用 `/mes/pro/edhr-release/approve`，请求携带幂等键和 `password`。
- 后端在任何放行写入前调用 `adminUserApi.reauthenticateForSignature(actorUserId, password)`。
- 电子密码认证成功后，最终放行直接进入 `RELEASED`，关闭批次，记录放行决策、事件和操作审计。
- 资料上传状态不再作为最终上市放行前置门禁；权限、状态、冻结、幂等和权威上下文仍保留。
- 旧审批中心签名证据不再作为最终上市放行硬门禁；负责人电子密码是本路径确认凭证。
- SQL 迁移复用 `MES_MANAGEMENT_REPRESENTATIVE`，确保 `mes:pro-edhr-release:query` 与 `mes:pro-edhr-release:approve`，并按 `tenant_id + username = 'admin'` 精确绑定。

## Notes

- 真实 Playwright E2E：已执行登录、权限按钮显示和二次确认。目标批次 `EDHRB-1789703187501` 当前为“冻结中”，没有正式放行事务，前端按正式门禁阻止调用最终放行接口；因此密码认证成功、`RELEASED` 和历史追溯跳转尚未能在真实数据上完成。
- E2E 证据截图：`output/playwright/edhr-after-login.png`、`output/playwright/edhr-release-confirm.png`、`output/playwright/edhr-release-result.png`、`output/playwright/edhr-work-task.png`。
- 后端运行时已切换为包含本任务实现的构建产物；本次阻塞来自测试数据的正式放行事务缺失，不是旧编译产物。
- task-closeout-cleanup preview/apply 已通过，仅删除本任务临时 `bug-regression-evidence.md`。
- 本地实现提交已完成：`fbf46743a`。
- 本轮未执行 Git push：用户仅补充授权提交，本轮未明确要求推送远端。
