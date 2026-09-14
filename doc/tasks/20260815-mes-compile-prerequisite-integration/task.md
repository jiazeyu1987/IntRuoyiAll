# MES 编译前置正式接入

## Task Goal

把用户已授权且已提交验证的 20 个前置源码中的剩余 18 个 MES 文件，从正式提交 `2810aec91fa55eedea3e1a0fd1b5e1195371ad26` 接入最新 `int_main` 基线，解除已提交 MES 消费代码的缺类编译阻塞。

## Scope

- 只接入 `2810aec91` 中 18 个 MES 源文件。
- 两个 DCC 候选 DTO 已由独立前置任务合入，不重复修改。
- 不复制主工作区未跟踪文件，不接入其差异，不修改 SQL、测试、前端或业务数据。
- 主工作区 `MesProSchedulerWorkbenchRuntimeStatusService.java` 的更晚未跟踪差异属于独立排产任务，只做 patch 备份并保持未应用。

## Milestones

- [x] M1：确认 20 文件正式来源、用户授权、原始 clean compile PASS 和当前缺类 RED。
- [x] M2：从正式提交精确恢复剩余 18 个 MES 文件并验证范围/哈希。
- [x] M3：运行 clean compile、DF06 前置命令、diff/UTF-8/风险扫描；DF06 前置已越过 main compile，下一阻塞为独立 INT12 测试辅助类。
- [x] M4：独立复验通过后提交并接入 `int_main`；正式前置已由 `254bb6181` 集成并通过 INT12 扩展回归。

## Expected Verification

- 当前缺少 10 个类型导致的 MES main compile 错误全部消失。
- `mvn.cmd -pl yudao-module-mes -am "-DskipTests" compile` 从干净隔离输出成功。
- DF06/C00 的定向 Maven 命令能够进入目标测试阶段，不再停在缺类编译错误。
- 18 个文件逐一与 `2810aec91` blob 相同；新增范围没有 SQL、测试、前端或其它任务文件。
- `git diff --check`、UTF-8/冲突标记/风险词扫描和 branch runtime guard 通过。

## Experience Gate

- 使用隔离 worktree，避免主工作区损坏的 `target_corrupt*` 和并发 Maven 输出。
- Maven 使用 `-am`；PowerShell 的 `-D` 参数逐项加引号。
- 提交前复验当前 blocker，不使用历史 Maven PASS 代替。
- 主工作区未跟踪文件即使大部分与正式提交相同，也不得作为本次复制来源。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；补齐已提交消费者对应的正式源码，而不是绕过模块编译。
- `是否存在临时补丁或绕过`：否；主工作区差异仅留 patch 备份，不进入本任务交付。

## Current Status

`completed`

18 个源码逐一与 `2810aec91` blob 相同，MES reactor clean compile 已 PASS，并已作为提交 `254bb6181` 接入 `int_main`。INT12 正式测试辅助类恢复后，扩展七类回归进入 Surefire 并通过 44/44，证明原缺类编译阻塞已正式解除。
