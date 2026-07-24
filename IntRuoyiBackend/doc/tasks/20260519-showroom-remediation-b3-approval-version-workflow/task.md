# 任务：展厅后端 B3 审批与版本工作流对齐

## 目标

把审批中心与版本工作流补齐到设计文档要求：包括持久化 change request / items、reject 接口、审批详情、版本差异、状态链路与 BPM 挂接位。

## 里程碑

- [x] 记录 BDD 与 TDD 目标
- [x] 实现持久化审批与版本工作流
- [x] 运行测试
- [x] 更新任务记录并提交

## 范围

- 审批与版本相关 controller / service / model / mapper / test
- reject 接口与审批详情接口
- 字段级 old/new diff 数据
- BPM 过程实例关联字段与业务状态

## 非范围

- 不实现指派/讨论/讲解逻辑
- 不改前台 display
- 不改前端页面

## 写入边界

- `yudao-module-showroom/src/main/java/**/workflow/**`
- 新增或调整审批相关 controller
- 审批相关测试
- `doc/tasks/20260519-showroom-remediation-b3-approval-version-workflow/**`

## 依赖

- 建议先合并 `B1`

## 预期验证

- `mvn -pl yudao-module-showroom -Dtest=ShowroomWorkflowApprovalTest,ShowroomHttpApiIntegrationTest "-Dsurefire.failIfNoSpecifiedTests=false" test`

## 完成定义

- 有 supervisor reject / gaoxin reject
- approval get 返回详情而不是裸 change request
- change request / item 状态字段覆盖设计文档核心要求

## 当前状态

completed: 已在 B3 写入边界内完成 change request 主契约持久化、approval/get 详情契约、supervisor-reject / gaoxin-reject，并通过指定测试清除 B4 对审批主契约的阻塞。

## Current Status

completed

## 阻塞记录

- 历史阻塞背景：当前主干实现仍依赖内存态 `ShowroomWorkflowService` / `ShowroomApiRuntime.changeRequests`，`approval/get` 与 reject 路由未形成稳定可追踪契约。
- 本次处理：已在 B3 范围内落持久化审批工作流与详情契约，作为 B4 的前置清障；未实现 B4 自身 assignment/comment 业务。

## 验证结果

- PASS: `mvn -pl yudao-module-showroom '-Dtest=ShowroomWorkflowApprovalTest,ShowroomHttpApiIntegrationTest' '-Dsurefire.failIfNoSpecifiedTests=false' test`

## 无上下文 LLM 提示词

```text
你在仓库 D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro 工作。

先读：
1. D:\ProjectPackage\Int\IntRuoyi\AGENTS.md
2. 当前任务文档：
   D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260519-showroom-remediation-b3-approval-version-workflow\task.md
3. 设计文档：
   - D:\ProjectPackage\Int\IntRuoyi\doc\tasks\20260518-showroom-product-doc-package\docs\system\backend-api-design.md
   - D:\ProjectPackage\Int\IntRuoyi\doc\tasks\20260518-showroom-product-doc-package\docs\system\data-model.md
   - D:\ProjectPackage\Int\IntRuoyi\doc\tasks\20260518-showroom-product-doc-package\docs\product\user-flows.md

目标：
- 补齐审批与版本工作流。

写入边界：
- workflow/**
- 审批 controller
- 相关测试
- 你的 task 目录

要求：
- 严格 TDD。
- 使用真实持久化，不允许内存态充当正式实现。
- 缺 B1 基础表则失败并记录 blocker。
- 不碰指派/讨论/讲解接口。

完成后运行：
- mvn -pl yudao-module-showroom -Dtest=ShowroomWorkflowApprovalTest,ShowroomHttpApiIntegrationTest "-Dsurefire.failIfNoSpecifiedTests=false" test
```
