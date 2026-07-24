# 展厅整改开工清单

## 迁仓说明

- 当前文档中 `F4/F5` 属于当前仓库里的历史前台整改任务。
- 新的 APP 展示层需求已迁移到 `D:\ProjectPackage\Website`。
- 如果后续要继续做“公司主页 / 展厅分类入口 / 产品图片墙 / 音频播放 / 讲解文字 / 多设备 APP 展示层”，请直接使用 Website 仓库里的 `W1-W4` 任务，不再从这里派 `F4/F5`。

## 统一规则

- 每个 LLM 只拿两样东西：
  - `D:\ProjectPackage\Int\IntRuoyi\AGENTS.md`
  - 它自己的 `task.md`
- 一任务一独立 `worktree/branch`
- 必须严格 TDD
- 缺前置就失败并记录 blocker
- 不得越过 `task.md` 写入边界
- 不得自行扩 scope

## 建议命名

- 总控：`codex/showroom-remediation-supervisor`
- `B1`：`codex/showroom-rem-b1-schema`
- `B2`：`codex/showroom-rem-b2-contract`
- `B3`：`codex/showroom-rem-b3-workflow`
- `B4`：`codex/showroom-rem-b4-collab`
- `B5`：`codex/showroom-rem-b5-narration`
- `F1`：`codex/showroom-rem-f1-company`
- `F2`：`codex/showroom-rem-f2-product-hall`
- `F3`：`codex/showroom-rem-f3-workbenches`
- `W1`：`codex/showroom-app-w1-home-company`
- `W2`：`codex/showroom-app-w2-gallery-audio`
- `W3`：`codex/showroom-app-w3-device-shells`
- `W4`：`codex/showroom-app-w4-integration`

## 批次 0

### 总控

启动条件：
- 立即启动

任务路径：
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260519-showroom-remediation-worksplit-index\task.md`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260519-showroom-remediation-worksplit-index\launch-plan.md`

发给总控 LLM 的提示词：

```text
你是展厅整改总控 LLM。

先读：
1. D:\ProjectPackage\Int\IntRuoyi\AGENTS.md
2. D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260519-showroom-remediation-worksplit-index\task.md
3. D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260519-showroom-remediation-worksplit-index\launch-plan.md

你的职责：
- 按 launch-plan.md 分批启动 worker
- 每个 worker 只发 AGENTS.md + 它自己的 task.md
- 检查前置依赖和写入边界冲突
- 发现 blocker 时暂停后续批次
- 最后安排 F5 收口并做总体验收

必须遵守：
- 一任务一独立 worktree/branch
- 严格 TDD
- 不允许 mock 成功、静默降级、隐藏 blocker
```

## 批次 1

### B1

启动条件：
- 总控已就位

任务路径：
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260519-showroom-remediation-b1-schema-persistence\task.md`

发给 B1 LLM 的提示词：

```text
你是 B1 worker。

先读：
1. D:\ProjectPackage\Int\IntRuoyi\AGENTS.md
2. D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260519-showroom-remediation-b1-schema-persistence\task.md

你没有额外上下文。严格按 task.md 执行。
- 只改 task.md 允许的写入边界
- 先 RED，再最小实现，再 GREEN
- 缺依赖就明确失败并记录 blocker
- 完成后更新 task.md / execution-log.md，并只提交本任务文件
```

## 批次 2

### B2

启动条件：
- `B1` 完成并提交

任务路径：
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260519-showroom-remediation-b2-content-display-contract\task.md`

提示词：

```text
你是 B2 worker。

先读：
1. D:\ProjectPackage\Int\IntRuoyi\AGENTS.md
2. D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260519-showroom-remediation-b2-content-display-contract\task.md

你没有额外上下文。严格按 task.md 执行。
核心目标：对齐内容查询与前台 display 契约。
禁止修改审批/指派/讨论/讲解主流程。
```

### B3

启动条件：
- `B1` 完成并提交

任务路径：
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260519-showroom-remediation-b3-approval-version-workflow\task.md`

提示词：

```text
你是 B3 worker。

先读：
1. D:\ProjectPackage\Int\IntRuoyi\AGENTS.md
2. D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260519-showroom-remediation-b3-approval-version-workflow\task.md

你没有额外上下文。严格按 task.md 执行。
核心目标：补齐审批与版本工作流。
禁止顺手实现指派/讨论/讲解页面需求。
```

### B4

启动条件：
- `B1` 完成并提交
- 若 `B3` 正在改相同 workflow 文件，总控需先串行化

任务路径：
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260519-showroom-remediation-b4-assignment-comment-collaboration\task.md`

提示词：

```text
你是 B4 worker。

先读：
1. D:\ProjectPackage\Int\IntRuoyi\AGENTS.md
2. D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260519-showroom-remediation-b4-assignment-comment-collaboration\task.md

你没有额外上下文。严格按 task.md 执行。
核心目标：补齐 assignment 与 comment collaboration。
必须真实处理 notify linkage，不得用占位成功。
```

### B5

启动条件：
- `B1` 完成并提交

任务路径：
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260519-showroom-remediation-b5-narration-preview-assets\task.md`

提示词：

```text
你是 B5 worker。

先读：
1. D:\ProjectPackage\Int\IntRuoyi\AGENTS.md
2. D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260519-showroom-remediation-b5-narration-preview-assets\task.md

你没有额外上下文。严格按 task.md 执行。
核心目标：补齐 narration 与 preview assets 持久化和读取链路。
不得伪造音频生成成功。
```

## 批次 3

### F1

启动条件：
- `B2` 契约稳定

任务路径：
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260519-showroom-remediation-f1-admin-company-dashboard-history\task.md`

提示词：

```text
你是 F1 worker。

先读：
1. D:\ProjectPackage\Int\IntRuoyi\AGENTS.md
2. D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260519-showroom-remediation-f1-admin-company-dashboard-history\task.md

你没有额外上下文。严格按 task.md 执行。
核心目标：实现 Dashboard / 公司工作台 / 公司版本历史。
不要改 router，不要改 showroom-admin/index.vue。
```

### F2

启动条件：
- `B2` 契约稳定

任务路径：
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260519-showroom-remediation-f2-admin-product-hall-operability\task.md`

提示词：

```text
你是 F2 worker。

先读：
1. D:\ProjectPackage\Int\IntRuoyi\AGENTS.md
2. D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260519-showroom-remediation-f2-admin-product-hall-operability\task.md

你没有额外上下文。严格按 task.md 执行。
核心目标：产品详情/历史入口与展厅映射入口。
不要改 router，不要改 showroom-admin/index.vue。
```

### F3

启动条件：
- `B3/B4/B5` 契约稳定

任务路径：
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260519-showroom-remediation-f3-admin-workflow-workbenches\task.md`

提示词：

```text
你是 F3 worker。

先读：
1. D:\ProjectPackage\Int\IntRuoyi\AGENTS.md
2. D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260519-showroom-remediation-f3-admin-workflow-workbenches\task.md

你没有额外上下文。严格按 task.md 执行。
核心目标：审批/指派/讨论/讲解工作台。
不要改产品页、展厅页和前台页。
```

### F4

启动条件：
- `B2/B5` 契约稳定

任务路径：
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260519-showroom-remediation-f4-frontstage-experience-alignment\task.md`

提示词：

```text
你是 F4 worker。

先读：
1. D:\ProjectPackage\Int\IntRuoyi\AGENTS.md
2. D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260519-showroom-remediation-f4-frontstage-experience-alignment\task.md

你没有额外上下文。严格按 task.md 执行。
核心目标：前台体验对齐设计文档。
绝对不要修改：
- src/views/showroom-frontstage/screen/**
- src/views/showroom-frontstage/pad/**
- src/views/showroom-frontstage/mobile/**
- src/views/showroom-frontstage/shared/components/**
```

## 批次 4

### F5

启动条件：
- `F1/F2/F3/F4` 全部完成并提交

任务路径：
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260519-showroom-remediation-f5-frontend-route-integration-e2e\task.md`

提示词：

```text
你是 F5 worker。

先读：
1. D:\ProjectPackage\Int\IntRuoyi\AGENTS.md
2. D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260519-showroom-remediation-f5-frontend-route-integration-e2e\task.md

你没有额外上下文。严格按 task.md 执行。
核心目标：把前面独立交付的页面接进真实路由、真实入口和真实 E2E。
只做集成收口，不要重写前置任务的业务页面。
```

## 总控验收口径

- `B1` 完成后，检查表结构是否齐。
- `B2/B3/B4/B5` 完成后，检查后端接口契约是否闭环。
- `F1/F2/F3/F4` 完成后，检查是否都遵守了写入边界。
- `F5` 完成后，跑最终路由和 E2E。
