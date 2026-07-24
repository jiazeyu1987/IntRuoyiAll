# 展厅整改派工手册

## 迁仓说明

- 本文档中的 `F4/F5` 为当前仓库历史完成记录。
- 自本次拆分调整后，新的 APP 展示层需求统一迁移到 `D:\ProjectPackage\Website`。
- Website 侧请改看：
  - `D:\ProjectPackage\Website\doc\tasks\20260519-showroom-app-remediation-worksplit\task.md`
  - `D:\ProjectPackage\Website\doc\tasks\20260519-showroom-app-w1-home-company-navigation\task.md`
  - `D:\ProjectPackage\Website\doc\tasks\20260519-showroom-app-w2-gallery-audio-text\task.md`
  - `D:\ProjectPackage\Website\doc\tasks\20260519-showroom-app-w3-multi-device-app-shells\task.md`
  - `D:\ProjectPackage\Website\doc\tasks\20260519-showroom-app-w4-app-integration-e2e\task.md`

## 总任务数

- 后端：5
- 当前仓库后台前端：3
- Website APP 展示层：4
- 总控：1
- 总计：13 个 LLM 角色

## 推荐批次

### 批次 0

- 总控

### 批次 1

- `B1`：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260519-showroom-remediation-b1-schema-persistence\task.md`

### 批次 2

- `B2`：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260519-showroom-remediation-b2-content-display-contract\task.md`
- `B3`：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260519-showroom-remediation-b3-approval-version-workflow\task.md`
- `B4`：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260519-showroom-remediation-b4-assignment-comment-collaboration\task.md`
- `B5`：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260519-showroom-remediation-b5-narration-preview-assets\task.md`

### 批次 3

- `F1`：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260519-showroom-remediation-f1-admin-company-dashboard-history\task.md`
- `F2`：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260519-showroom-remediation-f2-admin-product-hall-operability\task.md`
- `F3`：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260519-showroom-remediation-f3-admin-workflow-workbenches\task.md`
- `W1`：`D:\ProjectPackage\Website\doc\tasks\20260519-showroom-app-w1-home-company-navigation\task.md`
- `W2`：`D:\ProjectPackage\Website\doc\tasks\20260519-showroom-app-w2-gallery-audio-text\task.md`
- `W3`：`D:\ProjectPackage\Website\doc\tasks\20260519-showroom-app-w3-multi-device-app-shells\task.md`

### 批次 4

- `W4`：`D:\ProjectPackage\Website\doc\tasks\20260519-showroom-app-w4-app-integration-e2e\task.md`

## 依赖矩阵

- `B1`：无前置
- `B2`：前置 `B1`
- `B3`：前置 `B1`
- `B4`：前置 `B1`，最好等待 `B3` 关键契约稳定
- `B5`：前置 `B1`
- `F1`：前置 `B2`
- `F2`：前置 `B2`
- `F3`：前置 `B3 + B4 + B5`
- `W1`：前置 `B2`
- `W2`：前置 `B2 + B5`
- `W3`：前置 `B2 + B5`
- `W4`：前置 `W1 + W2 + W3`

## 绝对禁止的冲突

- `W1/W2/W3/W4` 一律在 `D:\ProjectPackage\Website` 仓库执行
- `B2/B3/B4/B5` 如同时修改同一个 controller 或 runtime 文件，必须由总控串行化

## 最简分发方式

- 给每个 worker 只发两样东西：
  - `D:\ProjectPackage\Int\IntRuoyi\AGENTS.md`
  - 它自己的 `task.md`

- 不要额外发聊天上下文
- 不要让 worker 自己猜依赖
- 缺依赖时必须让它在 `execution-log.md` 明确记录 blocker
