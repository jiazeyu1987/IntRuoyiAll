# 任务：展厅后端 B1 缺失表结构与持久化基础补齐

## 目标

按设计数据模型补齐 showroom 缺失的持久化表、DO、Mapper 与基础契约，为后续审批、指派、讨论、讲解、预览资产整改提供数据库基础。

## 里程碑

- [x] 记录 BDD 与 TDD 目标
- [x] 补齐缺失表结构与 DO / Mapper
- [x] 运行测试
- [x] 更新任务记录并提交

## 范围

- 补齐或新增：
  - `showroom_product_revision_relation`
  - `showroom_change_request`
  - `showroom_change_request_item`
  - `showroom_field_assignment`
  - `showroom_product_comment`
  - `showroom_narration_version`
  - `showroom_preview_asset_version`
- 对应 DO / Mapper / 测试 SQL / 基础约束

## 非范围

- 不修改前端代码
- 不修改内容查询、审批业务逻辑、前台 display 组装逻辑
- 不改 DCC/MES/AI 其他模块

## 写入边界

- `yudao-module-showroom/src/main/java/**/dal/**`
- `yudao-module-showroom/src/test/resources/sql/**`
- `yudao-module-showroom/src/test/java/**` 中与你本任务直接相关的 schema / mapper 测试
- `doc/tasks/20260519-showroom-remediation-b1-schema-persistence/**`

## 依赖

- 无前置代码依赖
- 设计基准：
  - `D:\ProjectPackage\Int\IntRuoyi\doc\tasks\20260518-showroom-product-doc-package\docs\system\data-model.md`

## 预期验证

- `mvn -pl yudao-module-showroom -Dtest=ShowroomFoundationContractTest "-Dsurefire.failIfNoSpecifiedTests=false" test`

## 完成定义

- 缺失业务表全部有测试建表与 DO/Mapper 落地。
- 字段最少覆盖设计文档中的必需字段。
- 不越权修改 controller/service 业务逻辑。

## 当前状态

completed: B1 持久化缺口已补齐，schema / mapper 契约测试与指定 foundation 测试均已通过，任务记录已更新并准备按任务边界提交。

## 验证结果

- PASS: `mvn -pl yudao-module-showroom "-Dtest=ShowroomSchemaMapperContractTest,ShowroomFoundationContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- PASS: `mvn -pl yudao-module-showroom -Dtest=ShowroomFoundationContractTest "-Dsurefire.failIfNoSpecifiedTests=false" test`

## Current Status

completed

## 无上下文 LLM 提示词

```text
你在仓库 D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro 工作。

先读：
1. D:\ProjectPackage\Int\IntRuoyi\AGENTS.md
2. 当前任务文档：
   D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260519-showroom-remediation-b1-schema-persistence\task.md
3. 设计文档：
   - D:\ProjectPackage\Int\IntRuoyi\doc\tasks\20260518-showroom-product-doc-package\docs\system\data-model.md

目标：
- 只补齐 showroom 缺失的持久化表、DO、Mapper、测试 SQL。

写入边界：
- yudao-module-showroom/src/main/java/**/dal/**
- yudao-module-showroom/src/test/resources/sql/**
- 相关测试
- 你的 task 目录

要求：
- 严格 TDD。
- 不改 controller/service 业务逻辑。
- 不引入 fallback。
- 缺前置条件就失败并记录。

完成后运行：
- mvn -pl yudao-module-showroom -Dtest=ShowroomFoundationContractTest "-Dsurefire.failIfNoSpecifiedTests=false" test
```
