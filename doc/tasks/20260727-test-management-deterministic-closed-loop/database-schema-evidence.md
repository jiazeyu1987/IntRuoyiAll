# Database Schema Evidence

## Data Change Goal

更新本地 Docker MySQL `ruoyi-vue-pro` 中 `system_codex_test_case` 与 `system_codex_test_checkpoint` 的测试说明文本，使测试管理自然语言测试项具备确定性、业务可读和闭环可重复执行能力。

## Database Engine And Tool

- Engine: MySQL in local Docker container `int-ruoyi-mysql`。
- Tool: `docker exec` 调用容器内 `mysql`，通过临时 defaults 文件读取容器环境变量中的 root 密码，不在日志输出明文密码。

## Affected Entities

- `system_codex_test_case`
- `system_codex_test_checkpoint`

## Data Safety Analysis

- 只更新 `tenant_id=1`、`deleted=0`、项目为 `工艺路线`、`批记录`、`智能排产` 的 14 个现有测试项。
- 不新增业务数据、不删除测试项、不修改生产租户数据。
- 检查点按现有 `case_id + sort` 精确更新，保持每个测试项 4 个检查点。

## Rollback Or Recovery Plan

- 写入前读取并记录目标测试项和检查点结构。
- 如验证失败，使用同一目标范围重新写入修正文案；不做破坏性删除。

## BDD Scenarios

- BDD: 测试项确定性闭环 -> Given 当前测试管理 14 项；When 重写方法与目标；Then 结构、闭环和业务可读验证通过。
- BDD: 缺失固定样本 fail-fast -> Given 固定样本不存在；When 执行对应节点；Then 测试项要求停止并记录，不继续执行。

## RED Evidence

- Command intent: scan current `system_codex_test_case` and `system_codex_test_checkpoint` for ambiguous wording before rewrite.
- Result: FAIL as expected, `AMBIGUOUS_CASES=13`, `AMBIGUOUS_CHECKPOINTS=33`.
- First write attempt: FAIL fast on MySQL `ERROR 1267 Illegal mix of collations`; transaction did not commit and target text stayed unchanged.

## GREEN Evidence

- Rewrite command: updated existing tenant `1` rows only, preserving project, case names, row count and checkpoint count.
- Result: PASS, `updated_cases=14`, `updated_checkpoints=56`.
- Structural verification: `CASE_COUNT=14`, `CHECKPOINT_COUNT=56`, `BAD_METHOD_LINE_COUNT=0`, `BAD_CHECKPOINT_COUNT=0`.
- Quality verification: `AMBIGUOUS_CASES=0`, `AMBIGUOUS_CHECKPOINTS=0`, `INTERNAL_CASES=0`, `INTERNAL_CHECKPOINTS=0`.
- Closed-loop wording verification: `STOP_PHRASE_CASES=14`, `CLEANUP_CASES=14`.
- Project distribution: `工艺路线=4`, `批记录=6`, `智能排产=4`.

## Migration Verification

本任务不创建 schema migration；只对现有表做受限文本更新。

## Blockers

- 批记录和电子批记录节点依赖的固定样本当前缺失；测试项文本已明确要求固定样本不存在时停止并记录，不继续执行。
- 本次未执行真实页面 Runner 回归；当前证据为 DB schema、数据范围和文本质量扫描。
- 任务 closeout 未完成提交和推送；工作区开始前已有无关脏改动。
