# Release Review Report

logic_status: fail
usability_status: fail
ui_status: pass
final_decision: fail

## Task Summary

静态审查了可信时间采集与解析、巡检聚合、证据 ZIP 导出、运行控制台展示，以及 eDHR 正式签名时间与业务发生时间的后端持久化和前端展示边界。`RuntimeTrustedTimeParser` 对命令失败、证据缺失、同步/NTP/Leap/Last/RMS/UTC 异常采用失败关闭；证据导出从指定已保存巡检读取并固定生成三文件；当前三个 eDHR 页面已直接以 `signedAt` 展示正式签名时间。归档渲染路径仍存在阻塞性边界遗漏，因此本轮不能放行。

## Logic Review

- Status: fail
- Blocking Issues:
  - **[P1] 最终 eDHR 归档仍允许业务发生时间覆盖正式签名时间。** `MesProEdhrBatchArchivePrintablePdfRenderer.formatSignatureDateTime` 在 `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrBatchArchivePrintablePdfRenderer.java:777` 依次选择 `signatureDisplayAt`、`selectedSignedAt`、`signedAt`，然后将结果作为“签名时间”输出；同文件 `:897` 的最新签名排序也使用相同优先级。历史签名的 `signatureDisplayAt` 可能等于用户选择的业务时间，因此新生成的最终可打印归档仍会展示错误的正式签名时间，并可能因用户选择了更晚/更早的业务时间而选择错误的签名记录。即使是本次修改后生成的新签名，`:782` 仍使用属于业务发生时间的 `selectedTimeZone` 标注服务器 `signedAt`；当用户浏览器/业务时区不是 `Asia/Shanghai` 时，归档会把服务端墙钟值标成错误时区。`PdfExecutionArchiveRenderer.java:200` 也仍以同样的三段回退和业务时区生成 `DisplaySignedAt`。这违反 PRD R2/R3 与 AC-01，且现有 P1 测试只覆盖持久化和三个页面，没有覆盖归档消费者。
- Notes:
  - 两条新增签名持久化主路径已令 `signatureDisplayAt = signedAt`，前端正式签名列表和签字格也已直接读取 `signedAt`；阻塞点是遗漏的归档消费者和历史数据展示语义。
  - 可信时间失败关闭、可选偏差阈值、固定目标主机、原始证据保存和 ZIP 校验和实现未发现另一项足以单独阻塞当前需求的静态逻辑缺陷。

## Usability Review

- Status: fail
- Blocking Issues:
  - 最终可打印归档把业务发生时间或业务时区呈现为“签名时间”，用户无法从最终受控记录中可靠判断实际服务器签署时刻；该错误信息直接影响审查和追溯，属于阻塞性误导。
- Notes:
  - 运行控制台的巡检、无巡检提示、下载禁用态、下载失败反馈和异常巡检可导出路径清楚。
  - eDHR 页面将用户输入改称“业务发生时间”，并把正式签名时间固定展示为 `signedAt`，页面主流程语义清楚。

## UI Review

- Status: pass
- Blocking Issues:
  - 无。
- Notes:
  - 本轮按 packet 要求仅做静态 UI 审查。可信时间表格包含环境、节点、时间源、Last/RMS、Leap、检查时间、状态和说明；动作有加载/禁用状态，移动端头部动作区会换行。未发现静态代码层面的遮挡、不可读或关键布局破损。

## Blocking Issues

1. eDHR 归档渲染和签名选择仍以可由用户业务时间影响的 `signatureDisplayAt/selectedSignedAt` 为优先值，并用业务时区标注正式服务器时间，导致最终归档中的正式签名时间不可信。

## Non-Blocking Suggestions

- 运行控制台可明确展示 `maxOffsetMillis` 是否启用，避免数值阈值关闭时仅凭 `PASS` 被误解为偏差已满足正式阈值。
- 可补充 Stratum 合法范围以及服务器 UTC、数据库 UTC、检查 UTC 的合理时间差/新鲜度校验；当前只验证 Stratum 可解析和 UTC 字符串格式。
- 时间证据下载响应可增加 `Cache-Control: no-store` 等缓存策略，降低共享终端或中间缓存保留运维证据的风险。

## Required Changes

1. 修正所有正式归档展示消费者：正式签名时间必须仅取 `signedAt`，不得回退到 `signatureDisplayAt` 或 `selectedSignedAt`；正式签名时区应使用明确的服务器时区（当前合同为 `Asia/Shanghai`）或独立持久化的正式签名时区，不能复用 `selectedTimeZone`。
2. `selectedSignedAt`、`selectedTimeZone`、`selectedTimeReason` 只能以单独的“业务发生时间证据”字段展示；如需保留历史 `signatureDisplayAt`，必须明确标为历史证据，不能再作为正式签名时间或选择顺序依据。
3. `MesProEdhrBatchArchivePrintablePdfRenderer` 的最新签名选择必须按 `signedAt`（并以稳定 ID 处理同秒并列）排序/选择，缺失 `signedAt` 时失败关闭，不得用用户业务时间补齐。
4. 先增加 RED 回归，至少覆盖：历史 `signatureDisplayAt != signedAt`、`selectedSignedAt != signedAt`、业务时区不是 `Asia/Shanghai`、两条同动作签名的业务时间顺序与正式签名顺序相反；同时覆盖 `PdfExecutionArchiveRenderer` 和最终可打印批归档。修复后运行对应 MES 定向回归，并确认前端静态合同继续通过。

## Final Decision

- final_decision: fail
- Reason: 正式签名时间边界在最终归档路径未闭合，存在可被用户业务时间影响的错误审计展示，必须修复并由独立 reviewer 复审后才能放行。
