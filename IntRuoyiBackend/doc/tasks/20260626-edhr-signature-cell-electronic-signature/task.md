# 任务：批记录签名格后端禁止普通字段写入

## 任务目标

- 后端拒绝把 `SIGNATURE` 单元格作为普通字段值写入字段审计链。
- 签名格必须通过现有电子签名接口生成签名记录，不能通过保存字段变更伪造签名人或签名时间。

## 当前状态

COMPLETED

## 上一任务检查

- 上一个后端任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260626-mes-replan-optional-reason-operation-log\task.md`
- 状态：`COMPLETED`
- 处理说明：上一任务已完成；本次仅修改 MES eDHR 字段审计签名值校验、单元测试和任务文档。

## 经验门禁

- 来源：`D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- 命中文档：无后端本机单元测试专属经验文档。
- 适用强制门禁：
  - 本轮不操作服务器、不改真实数据库 schema、不执行发布或远端联调。
  - 若后续进入真实 E2E 或登录后写入验证，必须先补登录预检记录。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。发现 `SIGNATURE` 普通字段值直接失败。
- `是否从根因和长期维护角度解决`：是。写入通道增加正式校验，防止绕过前端伪造签名。
- `是否存在临时补丁或绕过`：否。不新增兼容旧手填签名路径。

## BDD 场景

- `BDD: 后端拒绝签名格普通字段值 -> Given 请求保存字段变更且 valueType 为 SIGNATURE / When 后端校验字段变更 / Then 返回明确错误，不写字段审计、不生成签名记录。`
- `BDD: 普通字段变更保持原电子签名链路 -> Given 请求保存文本或数字字段变更 / When 用户提供密码签名 / Then 字段审计和 FIELD_CHANGE 签名继续按现有流程保存。`

## 里程碑

1. M1：创建任务文档、执行日志和 RED 单测。`COMPLETED`
2. M2：实现后端 `SIGNATURE` 字段值拒绝校验。`COMPLETED`
3. M3：运行后端目标验证。`COMPLETED`
4. M4：回写证据、收尾预览并按验证结果提交。`COMPLETED`

## 预期验证

- `mvn -pl yudao-module-mes -Dtest=MesProBatchRecordExecutionFieldAuditServiceImplTest test`
- `mvn -pl yudao-module-mes -Dtest=MesProBatchRecordExecutionServiceImplTest,MesProBatchRecordExecutionFieldAuditServiceImplTest test`

## 最终验证结果

- `mvn -pl yudao-module-mes -Dtest=MesProBatchRecordExecutionServiceImplTest#pageGetLegacyDraftGateAndSubmit_followDraftOnlyRules test`：PASS。
- `mvn -pl yudao-module-mes -Dtest=MesProBatchRecordExecutionFieldAuditServiceTest#saveChanges_rejectsSignatureCellAsOrdinaryFieldValue test`：PASS。
- `mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordExecutionServiceImplTest,MesProBatchRecordExecutionFieldAuditServiceTest" test`：PASS，90 个测试通过。
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc\tasks\20260626-edhr-signature-cell-electronic-signature\backend-api-evidence.md`：PASS。

## Cleanup Keep

- `doc/tasks/20260626-edhr-signature-cell-electronic-signature/backend-api-evidence.md`

## 阻塞与影响

- 已解除：用户已通过 `继续` 恢复本任务，本轮完成后端实现、验证和证据回写。
