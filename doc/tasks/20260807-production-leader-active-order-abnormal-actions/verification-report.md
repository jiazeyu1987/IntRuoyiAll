# Verification Report

## Result

实现、运行态修复和真实页面 E2E 均通过；生产订单 `980028` 已按用户输入原因 `123123` 成功标记异常。

## Passed

- 后端相关单元测试：4 个类，38/38 PASS。
- 后端 P0 回归：21 项中 20 项 PASS；唯一失败未进入本次异常订单链路。
- 前端聚焦合同：异常行操作、活跃订单池、页签删除、工作台、路线名称/版本显示等 7 个脚本 PASS。
- 前端类型检查：`pnpm ts:check` PASS。
- 任务文件空白检查：`git diff --check` 无错误，仅有仓库既有 LF/CRLF 提示。
- frontend-feature-delivery 与 backend-api-delivery evidence validator 均 PASS，两个 validator self-test 均 PASS；关键结论已归档到本报告。
- 旧运行 Jar 字节码 RED：请求 VO 仍有 3 个字段，`abnormalReasonCode` 带 `@NotBlank`。
- 新运行 Jar 字节码 GREEN：请求 VO 只剩 2 个字段；任务类组通过 JVM `-Xverify:all`。
- 本任务新稳定 Jar 内嵌 MES 条目唯一且未压缩，首次切换后 `48081` health=`UP`；随后并发任务以包含同一正式合同的更新 Jar 接管运行态，最终 health 仍为 `UP`。
- Playwright 真实页面 GREEN：订单 `980028` 上报成功，刷新后 ID 为红色、原因标题为 `123123`、按钮禁用，页面错误和失败请求均为 0。

## Behavior Verified

- 活跃订单行操作为“移除”和“报异常”。
- 报异常锁定当前生产订单，仅要求异常原因。
- 未关闭异常投影到活跃订单列表，生产订单 ID 使用红色显示。
- 未关闭异常订单从 FIFO 和前端候选中排除，手工分配由后端明确拒绝。
- 生产组长独立“异常”页签和旧异常表单已删除。

## Residual Failures

- `MesP0TeamLeaderReviewSignatureServiceTest#reviewSubmissionShouldPersistStructuredReviewSignature` 在既有事件根路径校验失败，与本次异常状态及分配逻辑无关。
- `mes-process-pool-team-leader-static.spec.js` 在并发任务的生产报工修订接口断言失败，聚焦异常合同测试通过。
- 标准 Maven/Surefire 未取得稳定结果：同模块存在其他 Maven 进程并发写入共享 `target`；隔离 javac/JUnit 补充验证通过，但不将其表述为 Maven PASS。

## Real E2E

- 使用本机 `http://127.0.0.1:8081` 和默认 `芋道源码/admin` 身份，从生产组长页面点击订单 `980028` 行“报异常”，填写 `123123` 并确认。
- 刷新后 `/active-order/list` 返回 `code=0`、`abnormal=true`、`abnormalReason=123123`。
- DOM/CSS 实测生产订单 ID 颜色为 `rgb(245, 108, 108)`，异常按钮禁用；settled 页面无可见错误消息或通知。
- settled 截图已完成视觉检查，确认红色 ID、按钮状态和布局正常；截图按收尾规则作为临时产物清理，关键 DOM/CSS 数值已归档在本报告。
- 非目标 `/team-device/list` 请求返回业务 404，但未影响活跃订单链路，settled 页面无残留提示；该并行功能不在本任务范围。
- 收尾期间并发任务切换到 `backend-latest-20260807-1919-team-device-list.jar`（PID `2396`）；该 Jar 请求 VO 字节码仍为 2 个字段并保留本次修复，最终真实页面复核同时确认 `/team-device/list` 不再产生可见错误。

## Cleanup

- 首轮 task-closeout-cleanup preview/apply 已完成，keep 3、blocked 0、warnings 0；并发新运行态复核产生的最小临时检查文件将在最终 cleanup 中删除。
- 当前 PID `2396` 属于并发任务接管后的正式本机运行态，已确认包含本次请求合同，不纳入临时清理。
- 最终 apply 已删除并发复核产生的临时内嵌 Jar 和 Playwright 会话产物；任务目录只剩 3 个核心记录。

## Final Status

completed：实现、运行态合同、真实页面行为、并发运行态复核和任务清理全部通过。
