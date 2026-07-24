# 任务：展厅后端 B4 指派与讨论协作链路

## 目标

补齐补充指派与产品讨论两类协作能力：包括 assignment create/get/page/complete-and-submit，以及 product comment create/page/reply/resolve 的持久化和接口。

## 里程碑

- [x] 记录 BDD 与 TDD 目标
- [x] 实现 assignment 全链路
- [x] 实现 comment 全链路
- [x] 运行测试并提交

## 范围

- 指派相关 service / controller / mapper / model
- 讨论相关 service / controller / mapper / model
- `system_notify_message` 真实引用或创建链路

## 非范围

- 不实现审批中心主流程
- 不实现讲解资产
- 不改前端页面

## 写入边界

- `yudao-module-showroom/src/main/java/**/workflow/**assignment**`
- `yudao-module-showroom/src/main/java/**/content/**comment**`
- 对应 controller / mapper / test
- `doc/tasks/20260519-showroom-remediation-b4-assignment-comment-collaboration/**`

## 依赖

- 建议先合并 `B1`
- assignment auto-submit 与 comment anchors 依赖 `B3` 的 change request 契约稳定

## 预期验证

- `mvn -pl yudao-module-showroom -Dtest=ShowroomAssignmentWorkflowTest,ShowroomDiscussionContentTest,ShowroomHttpApiIntegrationTest "-Dsurefire.failIfNoSpecifiedTests=false" test`

## 完成定义

- assignment 不再只有 create/complete 两个裸动作，而是有 get/page
- comment 不再只有 create/page，而是有 reply/resolve
- notify linkage 真实可追踪

## 当前状态

completed: 已在当前主工作区完成 assignment/comment 协作链路的最终回归，且 B4 指定命令已重新通过。

## 验证结果

- PASS: `mvn -pl yudao-module-showroom '-Dtest=ShowroomAssignmentWorkflowTest,ShowroomDiscussionContentTest,ShowroomHttpApiIntegrationTest' '-Dsurefire.failIfNoSpecifiedTests=false' test`

## 执行备注

- 本次执行目录：`D:\ProjectPackage\Int\IntRuoyi\worktrees\showroom-remediation-b4`
- 前置确认：B3 已完成并提交，`approval/get`、reject 路由与共享审批契约可作为 B4 前置使用。
- 交付摘要：
  - assignment 新增 `create/get/page/complete-and-submit`
  - product comment 新增 `reply/resolve`，并补齐 `status` 过滤
  - `showroom_field_assignment.notify_message_id` 现通过真实 `system_notify_message` 持久化链路生成并可回查
- 运行时修复：
  - 先同步安装了当前工作区的 `yudao-module-ai` 与 `yudao-module-infra`，清掉了 TTS 编译 blocker
  - 补齐了 `ShowroomHttpApiIntegrationTest` 的上下文依赖与旧断言
  - 最终再次通过了 B4 指定测试

## 无上下文 LLM 提示词

```text
你在仓库 D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro 工作。

先读：
1. D:\ProjectPackage\Int\IntRuoyi\AGENTS.md
2. 当前任务文档：
   D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260519-showroom-remediation-b4-assignment-comment-collaboration\task.md
3. 设计文档：
   - D:\ProjectPackage\Int\IntRuoyi\doc\tasks\20260518-showroom-product-doc-package\docs\system\backend-api-design.md
   - D:\ProjectPackage\Int\IntRuoyi\doc\tasks\20260518-showroom-product-doc-package\docs\system\data-model.md
   - D:\ProjectPackage\Int\IntRuoyi\doc\tasks\20260518-showroom-product-doc-package\docs\product\user-flows.md

目标：
- 实现 assignment 与 comment collaboration 全链路。

写入边界：
- assignment / comment 相关后端代码
- 相关 controller / test
- 你的 task 目录

要求：
- 严格 TDD。
- 必须使用真实 notify linkage，不得用占位成功。
- 缺 B1/B3 前置时失败并记录 blocker。
- 不要碰前台 display 和讲解工作流。

完成后运行：
- mvn -pl yudao-module-showroom -Dtest=ShowroomAssignmentWorkflowTest,ShowroomDiscussionContentTest,ShowroomHttpApiIntegrationTest "-Dsurefire.failIfNoSpecifiedTests=false" test
```
