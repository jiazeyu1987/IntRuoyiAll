# 任务：展厅整改工作拆分索引

## 目标

把当前“展厅设计文档 vs 当前实现”的整改工作拆成 10 份可以由无上下文 LLM 独立接手的任务包，明确每份任务的目标、边界、依赖、验证和提示词。

## 里程碑

- [x] 盘点当前前后端设计偏差
- [x] 拆分后端独立任务
- [x] 拆分前端独立任务
- [x] 为每份任务写入自包含提示词

## 预期验证

- 前端仓库存在 5 份独立整改 task 文档
- 后端仓库存在 5 份独立整改 task 文档
- 每份 task 文档包含目标、范围、写入边界、依赖、验证与无上下文 LLM 提示词

## 拆分结果

- 当前仓库仅承接后台与业务层任务。
- `D:\ProjectPackage\Website` 承接 APP 展示层任务。

- 后端 5 份：
  - `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260519-showroom-remediation-b1-schema-persistence`
  - `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260519-showroom-remediation-b2-content-display-contract`
  - `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260519-showroom-remediation-b3-approval-version-workflow`
  - `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260519-showroom-remediation-b4-assignment-comment-collaboration`
  - `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260519-showroom-remediation-b5-narration-preview-assets`
- 后台前端 3 份：
  - `doc/tasks/20260519-showroom-remediation-f1-admin-company-dashboard-history`
  - `doc/tasks/20260519-showroom-remediation-f2-admin-product-hall-operability`
  - `doc/tasks/20260519-showroom-remediation-f3-admin-workflow-workbenches`
- APP 展示层 4 份：
  - `D:\ProjectPackage\Website\doc\tasks\20260519-showroom-app-w1-home-company-navigation`
  - `D:\ProjectPackage\Website\doc\tasks\20260519-showroom-app-w2-gallery-audio-text`
  - `D:\ProjectPackage\Website\doc\tasks\20260519-showroom-app-w3-multi-device-app-shells`
  - `D:\ProjectPackage\Website\doc\tasks\20260519-showroom-app-w4-app-integration-e2e`

## 依赖原则

- `B1` 是后端持久化基础层，优先。
- `B2/B3/B4/B5` 可并行，但都应以 `B1` 的目标表结构为准。
- `F1/F2` 主要依赖 `B2`。
- `F3` 主要依赖 `B3/B4/B5`。
- `W1` 主要依赖 `B2`。
- `W2/W3` 主要依赖 `B2/B5`。
- `W4` 是 Website 仓库中 APP 展示层的集成与 E2E 收口任务，放在最后。

## 当前状态

已完成拆分。

## 推荐启动批次

- 批次 0：总控
  - 1 个总控 LLM
  - 负责按本文档分发、跟踪阻塞、确认前置完成、做最终集成放行
- 批次 1：基础后端
  - `B1`
- 批次 2：核心后端并行
  - `B2`
  - `B3`
  - `B4`
  - `B5`
- 批次 3：核心前端并行
  - `F1`
  - `F2`
  - `F3`
  - `F4`
- 批次 4：前端集成与 E2E
  - `F5`

## 并行规则

- `B1` 完成前，不启动任何依赖数据库新表的后端任务。
- `B2/B3/B4/B5` 可以并行，但如果出现相同 Java 文件写入冲突，应由总控改成串行。
- `F1/F2/F3/F4` 可以并行，但必须严格遵守各自 `task.md` 中的写入边界。
- `F4` 不得碰当前进行中的 `showroom-frontstage-shell-wave-b` 目录。
- `F5` 只能在 `F1/F2/F3/F4` 和关键后端契约都稳定后启动。

## 推荐派工顺序

1. 启动 `B1`
2. `B1` 通过后，同时启动 `B2/B3/B4/B5`
3. 当 `B2` 稳定后启动 `F1/F2`
4. 当 `B3/B4/B5` 稳定后启动 `F3`
5. 当 `B2/B5` 稳定后启动 `F4`
6. 所有前端任务稳定后启动 `F5`

## 总控 LLM 提示词

```text
你是展厅整改总控 LLM。你不负责直接实现业务代码，负责派工、追踪依赖、检查边界、汇总阻塞和安排收口。

先读：
1. D:\ProjectPackage\Int\IntRuoyi\AGENTS.md
2. D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260519-showroom-remediation-worksplit-index\task.md
3. D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260519-showroom-doc-implementation-audit\task.md

你的职责：
- 按 worksplit index 的推荐批次启动各 worker
- 每个 worker 只拿它自己的 task.md，不共享上下文
- 检查是否存在写入边界冲突
- 如果发现前置未完成，暂停后续任务并记录 blocker
- 前端最终由 F5 做集成收口，后端最终由你做契约回归确认

你必须强制执行：
- 一任务一独立 workspace / worktree
- 严格 TDD
- 不允许 mock 成功、静默降级、隐藏 blocker
- worker 只能改自己 task.md 指定的写入边界

你的输出格式：
- 当前应启动的 task 列表
- 每个 task 的仓库、路径、前置、负责人状态
- 已阻塞项
- 下一批次启动条件
```

## Worker 启动模板

对任意一个独立 LLM，统一使用下面模板，只替换 `TASK_MD_PATH`：

```text
你是一个无上下文 worker LLM。

先读：
1. D:\ProjectPackage\Int\IntRuoyi\AGENTS.md
2. TASK_MD_PATH

严格按 TASK_MD_PATH 执行：
- 只在 task.md 允许的写入边界内工作
- 先补 execution-log.md 里的 BDD / RED，再最小实现，再 GREEN
- 缺依赖就失败并记录 blocker
- 不要修改未授权路径
- 不要依赖主线程额外上下文

完成后必须：
- 更新 task.md
- 更新 execution-log.md
- 运行 task.md 中要求的验证命令
- 只提交本任务直接相关文件
```
