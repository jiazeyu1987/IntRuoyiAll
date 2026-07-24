# 任务：展厅运行时 fallback 清理

## 目标

清理 showroom 当前遗留的旧内存态运行时入口，去掉 `ShowroomApiRuntime` / `ShowroomDisplayController` 中不再需要的 fallback 构造、静态共享实例、内存审批缓存与内存预览图回退路径，保留当前持久化链路行为。

## 里程碑

- [x] 记录 BDD 与 TDD 目标
- [x] 删除旧 runtime fallback 入口
- [x] 运行验证测试
- [x] 更新任务记录并提交

## 范围

- `ShowroomApiRuntime`
- `ShowroomDisplayController`
- 与 runtime fallback 清理直接相关的测试
- `doc/tasks/20260519-showroom-runtime-fallback-cleanup/**`

## 非范围

- 不实现 B4 assignment/comment 业务
- 不实现 B5 narration/preview asset 新功能
- 不改前端页面
- 不改 showroom 持久化审批主契约

## 写入边界

- `yudao-module-showroom/src/main/java/**/controller/ShowroomApiRuntime.java`
- `yudao-module-showroom/src/main/java/**/controller/display/ShowroomDisplayController.java`
- 直接相关测试
- `doc/tasks/20260519-showroom-runtime-fallback-cleanup/**`

## 依赖

- B3 持久化审批主契约已落地
- B4/B5 旧 blocker 任务单已存在记录，本任务只清理其共同依赖的 runtime fallback

## 预期验证

- `mvn -pl yudao-module-showroom '-Dtest=ShowroomRuntimeStructureTest,ShowroomHttpApiIntegrationTest,ShowroomWorkflowApprovalTest' '-Dsurefire.failIfNoSpecifiedTests=false' test`

## 完成定义

- `ShowroomApiRuntime` 不再暴露静态 shared 单例或无参 fallback 构造
- `ShowroomDisplayController` 不再依赖默认构造 + shared runtime
- 运行时不再保留内存审批缓存和内存预览图回退分支
- B2/B3 相关回归测试保持通过

## 当前状态

completed: 已移除 `ShowroomApiRuntime` / `ShowroomDisplayController` 中的旧 fallback 入口与内存审批/预览回退分支，并保持 B2/B3 相关回归测试通过。

## Current Status

completed

## 验证结果

- PASS: `mvn -pl yudao-module-showroom '-Dtest=ShowroomRuntimeStructureTest' '-Dsurefire.failIfNoSpecifiedTests=false' test`
- PASS: `mvn -pl yudao-module-showroom '-Dtest=ShowroomRuntimeStructureTest,ShowroomHttpApiIntegrationTest,ShowroomWorkflowApprovalTest' '-Dsurefire.failIfNoSpecifiedTests=false' test`
