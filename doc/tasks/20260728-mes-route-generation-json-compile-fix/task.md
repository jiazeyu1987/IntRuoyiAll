# Task: MES route generation JSON compile fix

## Task Goal

修复 `MesProBatchRecordRouteGenerationServiceImpl.java` 中 JSON 字符串未转义导致的 MES 后端编译失败，并复跑刚才失败的 MES 目标 Maven 测试。

## Milestones

1. [x] 建立任务记录、读取经验门禁并复现编译失败。
2. [x] 修复 Java JSON 字符串构造语法。
3. [x] 复跑 MES 目标 Maven 测试并记录结果。
4. [x] 更新验证报告和收尾状态。

## Expected Verification

- `mvn -o -pl yudao-module-mes "-Dtest=MesProEdhrBatchExecutionServiceTest#previewTask_returnsUnopenedBatchRecordWithExecutionSnapshotAssistRows,MesProBatchRecordExecutionServiceImplTest#openOrCreateByContext_shouldNormalizeExecutionSnapshotJsonStructure+openOrCreateByContext_freezesAssistRowsInExecutionSnapshot" "-Dsurefire.failIfNoSpecifiedTests=false" test`

## 经验门禁

### PowerShell Maven -D 参数引号门禁

- Trigger: PowerShell 中运行 Maven 且参数包含 `-Dtest` 或 `-Dsurefire.failIfNoSpecifiedTests=false`。
- Preflight check: 每个 `-D...` 参数整体加双引号。
- Blocker: 出现 Maven lifecycle phase 解析错误时按 PowerShell 参数解析问题处理，不改测试范围。
- Verification: 使用整体加引号后的 Maven 命令复验。
- Forbidden action: 禁止移除目标测试或跳过 `surefire.failIfNoSpecifiedTests=false`。
- Evidence: `docs/powershell-memory.md#powershell-maven--d-参数引号门禁`。

### MES 编译门禁

- Trigger: `yudao-module-mes` 编译失败、Java 语法错误或 companion contract 漂移。
- Preflight check: 定位首个编译失败文件和行号，修复源码根因后重新运行目标 Maven。
- Blocker: 编译阶段仍失败时不得宣称后端测试通过。
- Verification: Maven compile/test 返回 `BUILD SUCCESS`。
- Forbidden action: 禁止用静态检查或跳过测试替代 Maven 编译。
- Evidence: `docs/backend-development.md` 与 `docs/experience-index.md` 中 MES compilation failure 路由。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，修复非法 Java 字符串构造。
- `是否存在临时补丁或绕过`：否。

## Current Status

completed
