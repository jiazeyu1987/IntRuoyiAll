# Execution Log

## 2026-08-15 Baseline

- User authorization：`doc/tasks/20260814-production-release-flow-implementation/execution-log.md` 第 75 行已记录用户明确授权审查、提交并接入这 20 个前置 Java 源码；禁止复制未提交文件、混入无关文件和推送远端。
- Formal source：`2810aec91fa55eedea3e1a0fd1b5e1195371ad26`，原任务对 20 文件执行风险扫描、`git diff --check` 和 MES clean compile，均 PASS。
- Current state：两个 DCC DTO 已在 `int_main`；剩余 18 个 MES 源码未合入，导致 DF06/C00 Maven 在目标测试前 main compile 失败。

BDD: 正式基线可编译 -> Given 已提交消费者引用 18 个缺失 MES 类型；When 从已授权正式提交接入精确源码并执行 reactor compile；Then 编译成功且测试命令能进入目标测试阶段。

BDD: 并发未跟踪改动隔离 -> Given 主工作区同名排产服务存在更晚未跟踪差异；When 接入本前置；Then 只使用正式提交内容，差异单独备份且不自动恢复。

RED: `mvn.cmd -pl yudao-module-mes -am "-DskipITs" "-Dtest=MesQaPqcSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，在目标测试前因 10 个正式类型缺失而 main compile 失败。

GREEN: `mvn.cmd -pl yudao-module-mes -am "-DskipTests" clean compile` -> PASS，24 个 reactor 模块全部 SUCCESS，MES 从干净输出编译 2665 个主源码，`BUILD SUCCESS`，总计 01:19。

## 2026-08-15 Scope And Reachability Verification

- Provenance：对 18 个 MES 路径逐一比较 `git rev-parse 2810aec91:<path>` 与工作树 `git hash-object`，`HASH_MISMATCH=0`。
- Whitespace：`git diff --check -- <18 paths>` -> PASS。
- Risk scan：`fallback|default-success|TODO|FIXME|password|secret` -> 0 hits。
- Preservation：主工作区更晚的未跟踪排产服务差异已保存为 `doc/tasks/20260814-fast-forward-int-main/patch-backups/20260815-mes-scheduler-runtime-untracked-vs-prerequisite.patch`，SHA-256 `3FDCBACF6324152B7B2C9A9612798A28D881440125DED98A594F6AC6ED10D8A5`；未应用到本任务。
- Target reachability：重新运行 `MesQaPqcSchemaTest` 命令后，main compile 已成功；随后停在 testCompile 缺少 `MesProFrontlineFeedbackSubmitSnapshotTestSupport`。该辅助类属于已提交 INT12 修复 `3e0df78fe`，因此本次不是 C00 RED，也不扩大当前 18 源码范围。
- Remaining gate：独立复验、branch runtime guard、精确提交；之后接入 `3e0df78fe` 再恢复 C00/DF06 TDD。
