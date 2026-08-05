# AC-M11 生产报工数量与损耗边界校验验证报告

## Scope

- 本报告仅覆盖 AC-M11 的数量/损耗主边界：输出数量、损耗数量、`loss <= output` 和拆分器合格数量计算。
- 不覆盖 AC-M11 其它未闭合项：设备参数规则、结构化损耗原因、签名快照、原始事实不可覆盖、详情/导出完整回读和 FIFO 损耗分片隔离。

## Code Changes Verified

- `MesProFrontlineFeedbackSubmitServiceImpl`：新增提交前数量校验，拒绝 `output <= 0`、`loss < 0`、`loss > output`。
- `MesProFrontlineFeedbackErrorCodeConstants`：新增 `PRO_FRONTLINE_FEEDBACK_QUANTITY_INVALID`。
- `MesProFrontlineFeedbackPayloadSplitter`：移除 `.max(BigDecimal.ZERO)` 截断，合法报工按 `outputQuantity - lossQuantity` 计算合格数量。
- `MesProFrontlineFeedbackSubmitServiceTest`：新增非法数量回归测试，要求非法数量在授权、幂等查询和写入前 fail-fast。

## Verification

- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesProFrontlineFeedbackSubmitServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL；损耗大于产出时仍创建正式报工并将合格数量截断为 0。
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProFrontlineFeedbackSubmitServiceTest,MesProFrontlineFeedbackPayloadSplitterTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS；9 tests, 0 failures, 0 errors。
- Final regression: 同一目标 Maven 命令于 2026-08-05 15:20:16 复跑 PASS；9 tests, 0 failures, 0 errors。
- Diff check: `git diff --check -- <AC-M11 task-owned paths>` PASS；仅 LF/CRLF warning，无 whitespace 错误。
- Cleanup: `task-closeout-cleanup --mode preview/apply` PASS；keep `task.md`、`execution-log.md`、`verification-report.md`，delete/blocked/warnings 均为 `<none>`。

## Result

AC-M11 数量/损耗主边界已代码级修复，非法数量不会进入正式报工、记录本或工序池。AC-M11 整体仍为不完全符合，后续仍需修复设备、参数、原因、签名和原始事实不可覆盖相关缺口。
