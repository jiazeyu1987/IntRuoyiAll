# Bug Regression Evidence

## Bug

表单模板升版/作废被上一轮实现硬编码为必须 `BPM_REQUIRED`。即使业务审批策略配置为 `DIRECT`，策略发布/切换会被拒绝，executor 的 direct 路径也会抛出 `Form template upgrade requires BPM approval` 或 `Form template obsolete requires BPM approval`。

## Expected

业务审批策略的 published 配置是权威来源：`DIRECT` 表示不启动 BPM 并直接生效；`BPM_REQUIRED` 表示必须有流程 key、必须启动 BPM，并在流程实例缺失时 fail fast。

## Reproduction

- `mvn -pl yudao-module-bpm "-Dtest=BusinessApprovalPolicyAdministrationServiceTest,FormTemplateUpgradeBusinessApprovalEffectExecutorTest,FormTemplateObsoleteBusinessApprovalEffectExecutorTest" test`
- `python -X utf8 -m pytest script/tests/test_form_template_upgrade_bpm_seed.py`

## Root Cause

- `BusinessApprovalPolicyAdministrationService` 维护了 `BPM_REQUIRED_EFFECT_EXECUTOR_CODES`，导致 `FORM_TEMPLATE_UPGRADE` / `FORM_TEMPLATE_OBSOLETE` 发布或切换到 `DIRECT` 被强制拦截。
- `FormTemplateUpgradeBusinessApprovalEffectExecutor#executeDirect` 与 `FormTemplateObsoleteBusinessApprovalEffectExecutor#executeDirect` 在 direct 路径直接抛 `BUSINESS_APPROVAL_MODE_INVALID`。
- `20260721_form_template_upgrade_bpm_seed.sql` 会把已发布升版策略的 `policy_mode` 强行更新为 `BPM_REQUIRED`，覆盖用户配置。

## Regression Test

- 更新 `BusinessApprovalPolicyAdministrationServiceTest`，覆盖表单模板升版策略可发布 `DIRECT`、可从 `BPM_REQUIRED` 切到 `DIRECT`、也可从 `DIRECT` 切回 `BPM_REQUIRED`。
- 更新 `FormTemplateUpgradeBusinessApprovalEffectExecutorTest`，覆盖 `DIRECT` 升版直接发布 DRAFT 版本。
- 更新 `FormTemplateObsoleteBusinessApprovalEffectExecutorTest`，覆盖 `DIRECT` 作废直接置为 `OBSOLETE`。
- 更新升版/作废 seed 静态合同，防止 seed 覆盖已发布 `DIRECT` 策略。

## RED:

- `mvn -pl yudao-module-bpm "-Dtest=BusinessApprovalPolicyAdministrationServiceTest,FormTemplateUpgradeBusinessApprovalEffectExecutorTest,FormTemplateObsoleteBusinessApprovalEffectExecutorTest" test` -> FAIL，预期失败原因：策略管理层仍报 `Business approval executor requires BPM_REQUIRED policy: FORM_TEMPLATE_UPGRADE`，direct executor 仍报 requires BPM approval。
- `python -X utf8 -m pytest script/tests/test_form_template_upgrade_bpm_seed.py` -> FAIL，预期失败原因：seed 仍包含 `SET policy_mode = 'BPM_REQUIRED'`，会覆盖 direct 配置。

## GREEN:

- `mvn -pl yudao-module-bpm "-Dtest=BusinessApprovalPolicyAdministrationServiceTest,FormTemplateUpgradeBusinessApprovalEffectExecutorTest,FormTemplateObsoleteBusinessApprovalEffectExecutorTest" test` -> PASS，32 tests passed。
- `python -X utf8 -m pytest script/tests/test_form_template_upgrade_bpm_seed.py script/tests/test_form_template_obsolete_bpm_policy_seed.py` -> PASS，7 tests passed。

## Verification

- `mvn -pl yudao-module-bpm "-Dtest=BusinessApprovalOrchestratorBpmRequiredTest" test` -> PASS，4 tests passed，确认 `BPM_REQUIRED` 仍启动流程并保持 fail-fast。
- `rg` 扫描确认生产代码和测试中不再存在 `Business approval executor requires BPM_REQUIRED`、`Form template upgrade requires BPM approval`、`Form template obsolete requires BPM approval` 的旧硬编码错误路径。

## Blockers

无当前任务阻塞。工作区仍有并行任务的未提交文档和 artifact 改动，本任务提交会仅暂存审批模式相关文件与本任务记录。
