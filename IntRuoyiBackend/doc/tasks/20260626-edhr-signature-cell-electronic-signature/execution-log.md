# Execution Log：批记录签名格后端禁止普通字段写入

BDD: 后端拒绝签名格普通字段值 -> Given 请求保存字段变更且 valueType 为 SIGNATURE / When 后端校验字段变更 / Then 返回明确错误，不写字段审计、不生成签名记录。
BDD: 普通字段变更保持原电子签名链路 -> Given 请求保存文本或数字字段变更 / When 用户提供密码签名 / Then 字段审计和 FIELD_CHANGE 签名继续按现有流程保存。

READONLY: 已读取 `docs/experience-index.md`；本轮不执行服务器、真实库 schema 或真实登录写入动作。

RED: `mvn -pl yudao-module-mes -Dtest=MesProBatchRecordExecutionFieldAuditServiceTest#saveChanges_rejectsSignatureCellAsOrdinaryFieldValue test` -> FAIL，预期失败；`PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_SIGNATURE_CELL_VALUE_FORBIDDEN` 错误码尚未实现，后端尚未拒绝 `SIGNATURE` 普通字段值。
RED: `mvn -pl yudao-module-mes -Dtest=MesProBatchRecordExecutionServiceImplTest#pageGetLegacyDraftGateAndSubmit_followDraftOnlyRules test` -> FAIL，预期失败；旧 `save-draft` 入口收到 `valueType=SIGNATURE` 时仍先返回字段审计基线缺失，没有给出签名格专属错误。

GREEN: implementation -> PASS，新增错误码 `PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_SIGNATURE_CELL_VALUE_FORBIDDEN`，错误文案为“签名格必须通过电子签名完成，不能作为普通字段保存”。
GREEN: implementation -> PASS，字段审计 `saveChanges` 在命令形状校验阶段拒绝 `MesProBatchRecordExecutionFieldAuditValueType.SIGNATURE`，并验证不调用 `recordFieldChangeSignature`。
GREEN: implementation -> PASS，旧 `save-draft` 入口在 baseline 缺失错误之前先检查 `cellValues.valueType=SIGNATURE` 并失败，防止绕过字段审计链路提交签名值。

GREEN: `mvn -pl yudao-module-mes -Dtest=MesProBatchRecordExecutionServiceImplTest#pageGetLegacyDraftGateAndSubmit_followDraftOnlyRules test` -> PASS。
GREEN: `mvn -pl yudao-module-mes -Dtest=MesProBatchRecordExecutionFieldAuditServiceTest#saveChanges_rejectsSignatureCellAsOrdinaryFieldValue test` -> PASS。
GREEN: `mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordExecutionServiceImplTest,MesProBatchRecordExecutionFieldAuditServiceTest" test` -> PASS，90 个测试通过。
GREEN: `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc\tasks\20260626-edhr-signature-cell-electronic-signature\backend-api-evidence.md` -> PASS。
GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260626-edhr-signature-cell-electronic-signature --mode preview --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro` -> PASS，无删除项、无阻塞、无警告。

Verification: 本轮执行 MES 后端目标单测、相关回归单测和 backend evidence 校验；未执行真实库 schema 修改、服务器操作或真实登录写入 E2E。
