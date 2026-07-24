# 任务：eDHR 字段级不可篡改审计后端实现

## 任务目标

在本后端 worktree 中实现字段级不可篡改审计链，严格遵循根任务包 `D:\ProjectPackage\Int\IntRuoyi\doc\tasks\20260526-edhr-field-audit-implementation` 与已放行设计文档。

## 里程碑

- [x] T1：后端 schema、DO/Mapper、hash、service core。
- [x] T2：后端 REST、旧 save-draft 门禁、提交/审批/归档绑定。
- [x] T4：配合独立验证修复后端缺陷。

## 预期验证

- Maven 后端单测 GREEN。
- SQL pytest GREEN。
- 无旧 JSON 直写绕过字段审计。
- 无 fallback/mock/静默降级。

## 当前状态

- 状态：completed
- 分支：`task/20260526-edhr-field-audit-implementation`
- 基线：`task/20260526-edhr-approval-tracking-implementation`

## 完成记录

- 完成内容：新增字段审计 SQL、执行表审计投影字段、字段审计 batch/item DO 与 Mapper、typed JSON canonicalization、字段审计 hash、`hashVerification` 状态、`FIELD_CHANGE` 签名绑定方法、字段审计 core service。
- 完成内容：补齐 REST controller/VO、旧 `save-draft` fail-fast 门禁、查询/详情/校验/导出、提交/审批/归档字段审计证据绑定、archive 字段审计投影列、snapshot default old value 与签名时间数据库精度修复。
- 验证结果：SQL 合同 4 tests PASS；后端 Maven targeted regression 68 tests PASS；DB_HASH executionId=19 PASS；详见 `execution-log.md`。
- 剩余阻塞：无。
