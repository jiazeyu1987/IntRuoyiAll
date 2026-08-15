# IntRuoyi Worktree Restrictions

## 触发场景

- 创建、启动、停止、重启、合并、清理任何 IntRuoyi worktree 前，必须先读取本文件。
- 涉及 worktree 路径、分支命名、端口分配、端口占用处理、端口登记表或 worktree 删除时，必须按本文件执行。
- 本文件是 IntRuoyi worktree 操作的强制限制文件；不得用临时判断、随机端口或旧项目规则替代。

## 固定基线

PORT_CONTRACT_VERSION: 2026-08-15-branch-runtime-v4

- 主工作区：`E:\IntRuoyi`。
- D-Main 工作区：`D:\ProjectPackage\IntRuoyi\IntRuoyiAll`。
- 主分支：`int_main`。
- worktree 根目录：`D:\IntRuoyiWorktree\`。
- 端口登记表：`D:\IntRuoyiWorktree\.ports\worktree-ports.json`。
- `int_main_d` 固定槽位：`slot = 0`。
- `int_main_d` 前端专属端口：`8101`。
- `int_main_d` 后端专属端口：`48101`。
- `int_main` 固定槽位：`slot = 0`。
- `int_main` 前端专属端口：`8081`。
- `int_main` 后端专属端口：`48081`。

## Runtime Profile 端口矩阵

- `int_main_d` profile：基准前端 `8101`，基准后端 `48101`，对应 `D:\ProjectPackage\IntRuoyi\IntRuoyiAll`。
- `int_main` profile：基准前端 `8081`，基准后端 `48081`，对应 `E:\IntRuoyi`。
- `int_batch` profile：基准前端 `8041`，基准后端 `48041`。
- `int_shedule` profile：基准前端 `8021`，基准后端 `48021`。
- `int_qms` profile：基准前端 `8061`，基准后端 `48061`。
- 槽位 `1..19` 的前后端端口继续按所属 profile 基准端口 + slot 计算。
- 槽位 `20..30` 使用本文件定义的独立扩展端口段。
- `slot = 0` 只用于各 profile 的基准工作区；同一 profile 的附加 worktree 必须使用稳定槽位 `1..30`。
- 跨 profile 不共享 slot 语义；例如 `int_batch slot=1` 是 `8042/48042`，`int_qms slot=1` 是 `8062/48062`。
- 分支端口矩阵的权威说明见 `docs\branch-runtime-ports.md`，提交、合并、推送前必须运行 `scripts\preflight\branch-runtime-port-guard.ps1`。

## 创建位置限制

- 所有 IntRuoyi worktree 只能创建在 `D:\IntRuoyiWorktree\` 下。
- 创建前必须解析目标目录的绝对路径，并确认它是 `D:\IntRuoyiWorktree\` 的子路径。
- 禁止在 `E:\IntRuoyi`、`E:\IntRuoyi\IntRuoyiBackend`、`E:\IntRuoyi\IntRuoyiFronted`、`%TEMP%`、用户目录或任何旧项目目录创建 IntRuoyi worktree。
- 如果 `D:\IntRuoyiWorktree\` 不存在或不可写，必须 fail fast，并报告缺失前置条件和影响；不得改用其他目录。

## 端口槽位规则

- `int_main_d` 基准工作区永远使用 `slot = 0`，前端 `8101`，后端 `48101`。
- `int_main` 基准工作区永远使用 `slot = 0`，前端 `8081`，后端 `48081`。
- `int_batch` 基准工作区永远使用 `slot = 0`，前端 `8041`，后端 `48041`。
- `int_shedule` 基准工作区永远使用 `slot = 0`，前端 `8021`，后端 `48021`。
- `int_qms` 基准工作区永远使用 `slot = 0`，前端 `8061`，后端 `48061`。
- 附加 worktree 必须使用稳定整数槽位，`slot = 1..30`。
- `slot >= 31` 必须 fail fast。
- 槽位 `1..19` 的端口按所属 runtime profile 的基准端口计算；槽位 `20..30` 必须使用集中定义的扩展端口段，不得自行推算或随机选择。
- 示例：
  - `int_main_d slot = 1`：前端 `8102`，后端 `48102`
  - `int_main slot = 1`：前端 `8082`，后端 `48082`
  - `int_batch slot = 1`：前端 `8042`，后端 `48042`
  - `int_shedule slot = 1`：前端 `8022`，后端 `48022`
  - `int_qms slot = 1`：前端 `8062`，后端 `48062`
- `int_main_d` 必须使用 `8101/48101`，不得使用保留给 `E:\IntRuoyi` 的 `8081/48081`。
- 非 `int_main` profile 永远不得使用 `8081` 或 `48081`。
- 各 profile 的附加 worktree 可用端口段分别为：
  - `int_shedule`：槽位 `1..19` 为 `8022-8040/48022-48040`，槽位 `20..30` 为 `8121-8131/48121-48131`
  - `int_batch`：槽位 `1..19` 为 `8042-8060/48042-48060`，槽位 `20..30` 为 `8132-8142/48132-48142`
  - `int_qms`：槽位 `1..19` 为 `8062-8080/48062-48080`，槽位 `20..30` 为 `8143-8153/48143-48153`
  - `int_main`：槽位 `1..19` 为 `8082-8100/48082-48100`，槽位 `20..30` 为 `8154-8164/48154-48164`
  - `int_main_d`：槽位 `1..19` 为 `8102-8120/48102-48120`，槽位 `20..30` 为 `8165-8175/48165-48175`

## 端口登记表规则

- 创建任何附加 worktree 后、首次启动前，必须通过 `scripts\runtime\reserve-worktree-slot.ps1` 原子分配并登记槽位。
- 分配脚本必须使用跨进程互斥锁读取和写入 `D:\IntRuoyiWorktree\.ports\worktree-ports.json`，并选择所属 profile 的最低空闲槽位。
- 如果登记表不存在，必须创建空登记表并立即登记本次分配；不得跳过登记。
- 每个登记项至少记录：
  - `name`
  - `path`
  - `branch`
  - `profile`
  - `slot`
  - `frontendPort`
  - `backendPort`
  - `active`
  - `createdAt`
  - `updatedAt`
- worktree 停止服务但目录仍存在时，槽位不得释放。
- 只有确认 worktree 目录已删除、分支/合并状态已处理、任务记录已完成后，才允许将对应槽位标记为可复用。
- 所有 `active = true` 的登记项必须保持 `profile/slot`、前端端口和后端端口全局唯一。
- 不允许因为端口冲突临时随机换端口；必须修正登记表或阻塞。

## 启动和端口占用处理

- 启动 `int_main` 前端前，必须确认 `8081` 的占用情况。
- 启动 `int_main` 后端前，必须确认 `48081` 的占用情况。
- 启动 `int_batch`、`int_shedule`、`int_qms` 基准工作区前，必须确认各自矩阵端口占用情况。
- 如果端口被同一 profile 对应的旧前端/后端进程占用，先停止对应旧进程，再启动新的同 profile 服务。
- 如果端口被其他 profile、未知进程或无关程序占用，必须 fail fast，报告占用进程、端口和影响；不得强杀、不得换端口启动。
- 启动任何附加 worktree 前，必须确认登记端口的占用情况。
- 如果登记端口被同一 worktree 的旧进程占用，先停止对应旧进程，再启动该 worktree。
- 如果登记端口被其他 worktree、未知进程或无关程序占用，必须 fail fast，报告冲突；不得自动换端口。

## 断链快照恢复规则

- 如果旧 worktree 的 `.git` 文件指向缺失的 `.git/worktrees/<name>` 元数据，不要在旧目录内直接修补或重写 `.git`。
- 先从有效主仓库在 `D:\IntRuoyiWorktree\` 下创建干净 worktree，再把旧快照中的源码、测试、文档、SQL 和脚本差异迁移过去。
- 迁移前必须明确旧目录到新仓库目录的映射；例如旧后端快照映射到 `IntRuoyiBackend`，旧前端快照映射到 `IntRuoyiFronted`。
- 迁移时默认排除 `.git`、`node_modules`、运行日志、`.runtime`、`runtime`、`target`、`dist`、环境密钥文件和生成物。
- 迁移后用 `git status --short --branch`、`.git` 指向检查和 `git diff --stat` 验证新 worktree 可追踪；旧断链快照保持只读，不在原地修复。

## 禁止做法

- 禁止在未读取本文件时创建或启动 worktree。
- 禁止非 `int_main` 使用 `8081/48081`。
- 禁止任一 profile 使用其他 profile 的基准端口。
- 禁止附加 worktree 使用 `slot >= 31`。
- 禁止绕过 `scripts\runtime\reserve-worktree-slot.ps1` 手工猜测或并发写入槽位。
- 禁止活动登记项复用其他 worktree 的 `profile/slot`、前端端口或后端端口。
- 禁止随机选择端口或按启动顺序临时分配端口。
- 禁止端口冲突时静默换端口、静默跳过服务或假装启动成功。
- 禁止不检查目标绝对路径就执行 `git worktree add`。
- 禁止删除或清理不属于当前任务的 worktree、进程、端口登记项或任务记录。

## 验证方式

- 创建 worktree 前记录已读取本文件。
- 记录目标路径解析结果，证明目标在 `D:\IntRuoyiWorktree\` 下。
- 记录 `reserve-worktree-slot.ps1` 的分配结果和端口登记表写入结果。
- 记录分配的 `slot`、前端端口、后端端口。
- 启动服务前记录端口占用检查结果。
- 如果停止旧进程，必须记录进程 ID、端口、归属判断依据和停止结果。
- 任务收尾时记录 worktree 是否仍存在、槽位是否保留或释放、端口登记表最终状态。
