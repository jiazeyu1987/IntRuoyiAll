# 任务：展厅后端 B5 讲解与预览资产持久化

## 目标

补齐讲解稿、讲解音频、静态预览图资产的持久化、审批、读取与前台使用链路，去掉当前仅靠内存和空预览图 URL 的状态。

## 里程碑

- [x] 记录 BDD 与 TDD 目标
- [x] 实现 narration 持久化与读取
- [x] 实现 preview asset 持久化与读取
- [x] 运行测试并提交

## 范围

- narration version 持久化
- preview asset version 持久化
- admin narration get/draft/generate/submit 契约
- preview asset live URL 组装
- display 侧 narration / preview image 读取所需后端能力

## 非范围

- 不改后台审批主流程
- 不改前端页面
- 不做知识库/问答

## 写入边界

- `yudao-module-showroom/src/main/java/**/narration/**`
- `yudao-module-showroom/src/main/java/**/asset/**`
- 讲解/预览资产相关 controller
- 相关测试
- `doc/tasks/20260519-showroom-remediation-b5-narration-preview-assets/**`

## 依赖

- 建议先合并 `B1`

## 预期验证

- `mvn -pl yudao-module-showroom -Dtest=ShowroomNarrationLifecycleTest,ShowroomAudioGenerationContractTest,ShowroomPreviewAssetLifecycleTest,ShowroomHttpApiIntegrationTest "-Dsurefire.failIfNoSpecifiedTests=false" test`

## 完成定义

- `generate-audio` 不再因为 runtime 缺 adapter 就天然失败
- 讲解 live 数据与预览资产 live 数据都能持久化读取
- display payload 可带出真实 preview image URL

## 当前状态

completed: 已在干净 worktree `D:\ProjectPackage\Int\IntRuoyi\worktrees\showroom-remediation-b5`、分支 `codex/showroom-remediation-b5`、基线提交 `2ead7a55c0` 上重启并完成 B5；narration latest/get、preview asset 持久化与 display preview URL 已补齐，`generate-audio` 仍保持显式失败语义。

## 验证结果

- PASS: `mvn -pl yudao-module-showroom clean '-Dtest=ShowroomNarrationLifecycleTest,ShowroomAudioGenerationContractTest,ShowroomPreviewAssetLifecycleTest,ShowroomHttpApiIntegrationTest' '-Dsurefire.failIfNoSpecifiedTests=false' test`
- PASS: `mvn -pl yudao-module-showroom '-Dtest=ShowroomNarrationLifecycleTest,ShowroomAudioGenerationContractTest,ShowroomPreviewAssetLifecycleTest,ShowroomHttpApiIntegrationTest,ShowroomPersistentNarrationServiceTest' '-Dsurefire.failIfNoSpecifiedTests=false' test`

## 无上下文 LLM 提示词

```text
你在仓库 D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro 工作。

先读：
1. D:\ProjectPackage\Int\IntRuoyi\AGENTS.md
2. 当前任务文档：
   D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260519-showroom-remediation-b5-narration-preview-assets\task.md
3. 设计文档：
   - D:\ProjectPackage\Int\IntRuoyi\doc\tasks\20260518-showroom-product-doc-package\docs\system\backend-api-design.md
   - D:\ProjectPackage\Int\IntRuoyi\doc\tasks\20260518-showroom-product-doc-package\docs\system\data-model.md
   - D:\ProjectPackage\Int\IntRuoyi\doc\tasks\20260518-showroom-product-doc-package\docs\product\prd.md

目标：
- 补齐 narration 与 preview assets 的持久化和读取链路。

写入边界：
- narration/**
- asset/**
- 讲解/预览资产相关 controller
- 相关测试
- 你的 task 目录

要求：
- 严格 TDD。
- 不允许 fallback 音频或空成功。
- 若外部音频适配器契约未就绪，必须明确保留失败语义并记录 blocker，而不是伪造生成成功。
- 不要改前端页面。

完成后运行：
- mvn -pl yudao-module-showroom -Dtest=ShowroomNarrationLifecycleTest,ShowroomAudioGenerationContractTest,ShowroomPreviewAssetLifecycleTest,ShowroomHttpApiIntegrationTest "-Dsurefire.failIfNoSpecifiedTests=false" test
```
