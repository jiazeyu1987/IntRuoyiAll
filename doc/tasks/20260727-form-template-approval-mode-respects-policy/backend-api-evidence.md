# Backend API Evidence

## Scope

本次范围是 BPM 业务审批桥接的后端服务行为：`BusinessApprovalPolicyAdministrationService`、表单模板升版/作废 effect executor，以及升版/作废策略 seed 合同。

## Contract

- published `bpm_business_approval_policy.policy_mode` 是运行时权威配置。
- `DIRECT`：不启动 BPM，升版直接把目标 DRAFT 版本置为 `PUBLISHED`，作废直接把目标版本置为 `OBSOLETE`。
- `BPM_REQUIRED`：必须有流程 key，升版使用 `form-template-upgrade-v1`，作废使用 `form-template-obsolete-v1`，并在 process instance 缺失时 fail fast。
- seed 可以在缺少 published 策略时补默认 `BPM_REQUIRED` 策略；已存在 published `DIRECT` 时不得覆盖成 `BPM_REQUIRED`。

## Validation

- 权限与签名：策略模式切换仍要求电子签名密码，并记录 `BUSINESS_APPROVAL_POLICY_SWITCH` 签名证据。
- 错误行为：`BPM_REQUIRED` 缺流程 key 或缺 process instance 继续抛业务审批错误，不返回默认成功。
- 数据合同：seed 仍检查重复 published 策略；只对 `BPM_REQUIRED` 行校验流程 key 与 executor code，不把 `DIRECT` 视为冲突。

## BDD:

- BDD: Direct upgrade publishes immediately -> Given 表单模板升版策略为 DIRECT When 导入已有模板生成 DRAFT 新版本 Then 直接发布该版本，不启动 BPM。
- BDD: BPM upgrade starts approval -> Given 表单模板升版策略为 BPM_REQUIRED When 导入已有模板生成 DRAFT 新版本 Then 启动 `form-template-upgrade-v1` 并把版本置为 `PENDING_APPROVAL`。
- BDD: Direct obsolete obsoletes immediately -> Given 表单模板作废策略为 DIRECT When 提交作废 Then 直接置为 `OBSOLETE`，不启动 BPM。
- BDD: BPM obsolete starts approval -> Given 表单模板作废策略为 BPM_REQUIRED When 提交作废 Then 启动作废 BPM 并置为 `PENDING_APPROVAL`。

## RED:

- `mvn -pl yudao-module-bpm "-Dtest=BusinessApprovalPolicyAdministrationServiceTest,FormTemplateUpgradeBusinessApprovalEffectExecutorTest,FormTemplateObsoleteBusinessApprovalEffectExecutorTest" test` -> FAIL，旧代码强制 `FORM_TEMPLATE_UPGRADE` 必须 BPM_REQUIRED，direct executor 抛错。
- `python -X utf8 -m pytest script/tests/test_form_template_upgrade_bpm_seed.py` -> FAIL，旧 seed 仍会强制 `policy_mode = 'BPM_REQUIRED'`。

## GREEN:

- `mvn -pl yudao-module-bpm "-Dtest=BusinessApprovalPolicyAdministrationServiceTest,FormTemplateUpgradeBusinessApprovalEffectExecutorTest,FormTemplateObsoleteBusinessApprovalEffectExecutorTest" test` -> PASS，32 tests passed。
- `python -X utf8 -m pytest script/tests/test_form_template_upgrade_bpm_seed.py script/tests/test_form_template_obsolete_bpm_policy_seed.py` -> PASS，7 tests passed。
- `mvn -pl yudao-module-bpm "-Dtest=BusinessApprovalOrchestratorBpmRequiredTest" test` -> PASS，4 tests passed。

## Verification

后端验证覆盖策略管理、direct executor、BPM_REQUIRED 编排和 SQL seed 合同。未修改 Controller API shape、权限模型、数据库 schema 或租户边界。

## Blockers

无当前任务阻塞。并行任务脏改动不属于本次后端审批模式修复，提交时不纳入 staged set。
