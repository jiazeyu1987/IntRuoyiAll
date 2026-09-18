# eDHR 批次执行列表上市放行负责人电子签名放行

## Task Goal

在 eDHR 批次执行列表的“操作”列中完成正式上市放行业务流程：

- 在黄框位置提供“上市放行”“上传”“驳回”三个按钮，其中“上传”和“驳回”保持占位行为；
- 仅拥有 `mes:pro-edhr-release:approve` 权限的“管理者代表”角色用户可看到“上市放行”按钮；
- 给 `admin` 精确绑定既有 `MES_MANAGEMENT_REPRESENTATIVE` 角色，并确保该角色拥有 `mes:pro-edhr-release:query` 与 `mes:pro-edhr-release:approve`；
- 点击“上市放行”后打开二次确认弹窗，要求输入负责人电子密码；
- 服务端使用当前登录用户重新认证电子密码，认证成功后直接完成 `RELEASED`；
- 放行不以资料上传为前置条件，但必须保留权限、批次状态、事务状态、幂等、冻结、权威上下文、事件和审计门禁；
- 放行成功后跳转 `/mes/pro/feedback/edhr-batch-history` 历史追溯列表。

## Milestones

1. 修订 BDD 场景和 RED 测试，覆盖上市放行按钮权限、二次确认、电子密码、成功路由和资料上传非门禁。
2. 修改 `BatchExecutionListPage.vue` 和前端 API 类型，接入正式上市放行 API。
3. 修改后端最终放行接口，加入负责人电子密码重新认证，取消资料上传门禁，不绕过其它正式门禁。
4. 迁移 `MES_MANAGEMENT_REPRESENTATIVE` 角色权限并精确绑定 `admin`。
5. 修复目标模块编译/测试契约错误，使新电子签名放行路径与旧审批中心签名路径解耦。
6. 运行前端静态合同、后端目标测试、SQL 静态合同、类型检查和差异检查，记录 GREEN 结果。

## Expected Verification

- `node tests/e2e/edhr-batch-action-placeholders-static.spec.cjs`
- `python -m pytest script/tests/test_mes_management_representative_admin_permission_sql.py`
- `mvn -pl yudao-module-mes -am '-Dtest=MesProEdhrReleaseServiceImplTest,MesReleaseFinalizationValidatorTest,MesReleaseAuthoritativeContextPortImplTest' '-Dsurefire.failIfNoSpecifiedTests=false' test`
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm exec vue-tsc --noEmit --pretty false -p tsconfig.relaxed.json`
- `git diff --check`

## Design Constraints

- 复用既有 `MES_MANAGEMENT_REPRESENTATIVE` 角色，不新建重复“上市放行负责人”角色。
- “不用考虑资料上传”仅取消资料上传门禁，不取消权限、状态、冻结、幂等、权威上下文、事件和审计。
- 电子密码为空、错误或用户未配置电子签名时，必须在任何业务写入前失败。
- 相同幂等键重试必须按既有规则返回，不重复关闭批次或重复写入放行事件。
- `admin` 授权必须按 `tenant_id + username = 'admin'` 精确绑定，不使用宽泛用户名或默认 `tenant_admin` 推断。
- 不覆盖工作区中其他任务已有改动。
- 不执行真实 E2E：项目规则要求仅在用户当轮明确要求时执行，本轮未要求真实页面验收。

## Current Status

ready_for_closeout

实现和必需验证已通过。真实 Playwright E2E 未执行；本轮未执行 Git commit/push，因为用户未授权。
