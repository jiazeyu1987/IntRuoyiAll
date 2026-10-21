# E2E-17 下发预览缺水印头：只读定位与最小 RED 设计

## 范围与当前结论

只读源码、原任务页面证据及既有运行日志；本轮没有重新请求正文、签收或改变业务数据，没有运行 Maven、修改生产/策略/VIEW，也没有操作已归还的 5627。以下代码位置为当前工作区源码，不能替代 Root 对运行包和真实策略行的核对。

Controller 的普通/下发预览共用成功响应写头代码，尚无证据支持“下发分支漏写水印头”。优先核对下发独有的正文打开审计策略 `dcc.distribution.recipient.body-opened`。它在返回二进制之前执行，缺失/停用会抛异常，Controller 成功写头分支不会执行；前端 raw Axios 把 HTTP 200 错误结果当 Blob 后，首先报必需水印头缺失。这是源码与现场日志支持的首要推断，**运行库该策略状态尚未经本线程核实，不能写成已确认修复点**。

## 原失败身份与证据边界

- 同一原用户 R1：`DCC241016C17R1`，userId `910496`，租户芋道源码/1；文件 `2054545668044084234`，`A/1`，BPM `157cc551-cad1-11f1-a035-b082e25ec548`，distribution `273`，recipient `276`。
- 正式入口：R1 本人站内信的“查看下发文件并签收”；页面 route 的身份已保存在 `distribution-preview-issue.json.result.url/query`。没有手拼业务 URL。
- 原自然请求记录：GET `/admin-api/dcc/controlled-files/2054545668044084234/preview-metadata` → HTTP 200；GET `/admin-api/dcc/controlled-files/2054545668044084234/preview` → HTTP 200。原 observer 只存 path/method/status，**没有原响应头、Content-Type、响应 JSON 或完整 query 抓包**；不能补造这些事实。下发参数来自原页面 route 与正式前端请求构造源码。
- 页面错误：`DCC preview response missing required watermark header`；严格解码器要求的字段名为 `X-DCC-Preview-Watermark`（Axios 按 `x-dcc-preview-watermark` 读取）。这能证明浏览器取到的头值为空，不能独立证明代理、CORS 或线上的响应分支。
- 原截图：`artifacts/acceptance-2026-10-18T07-46-27-330Z/R1-distribution-preview-watermark-error.png`。初始仅容器/标题截图不算正文成功；没有已渲染 PDF 页面证据。
- 同用户资格：原类别查看用户规则、独立登录、启用签名图片/签名授权在 `activation-handoff.json.recipientEligibility`；这些不等于普通 DAILY 正文已读成功。下发 metadata 成功投影及精确签收页表明专用已投递收件人入口可达；本轮未改阅读授权。实际正文 VIEW 连续性仍未通过，六个负例不得据此升级。
- U 的 ACTIVE 详情/正式名单是作者侧事实，不是 R1 的普通预览成功对照。D 全局列表无文件与已办 viewer 禁用也不能当正文时序负例。

## 正式链路与普通预览差异

| 层级 | 普通预览 | 本例下发预览 |
| --- | --- | --- |
| 前端请求 | `workflow.ts#previewControlledFileWithWatermark`，同一个 Axios Blob 请求；带正式 body-read context | 同方法传 distributionId/recipientId，不混入 body-read context |
| Controller | `DccControlledFileController#previewControlledFile` → `readPreviewFile` | 同 Controller，`distributionPreviewTask` 校验精确身份 → `readDistributionPreviewFile` |
| metadata/token | 普通正文上下文、`prepareSourceBoundAccess` | 精确 ACTIVE/受控/启用/下发/原收件人/已发送任务守卫，`prepareAccess`，purpose `DIST_PREVIEW:273:276` |
| 二进制 | 两者均进入 `DccControlledFileQueryServiceImpl#readPreviewBinary`，复核正文/业务文件权限、证据、token，读取正式制品 | 相同主链，但使用专用下发上下文，不借普通 VIEW 或最新主版本补齐 |
| 打开记录 | `recordDistributionBodyOpenedIfScoped` 在无下发上下文时直接 return | 读取成功后、返回 binary 前调用真实 `DccDistributionReceiptServiceImpl#recordDistributionBodyOpened` |
| 审计 | 普通链不写收件人 body-opened 事实 | 事务内写 readAt，append `dcc.distribution.recipient.body-opened`；缺策略须拒绝并回滚 readAt，不得制造签收时间 |
| 成功响应 | 两者共用 Controller 的 `ResponseEntity.ok()`，写 Content-Type/Content-Disposition、Expose-Headers、`X-DCC-Preview-Watermark`、`X-DCC-Access-Event-Code` | 完全同一成功写头分支；Service 抛错时到不了此处 |
| 错误响应与前端 | `GlobalExceptionHandler#serviceExceptionHandler` 返回 CommonResult 错误；无该成功水印头。raw Axios 没有先判业务错误 envelope 就解码必需头 | 因此可能以“缺水印头”表现上游拒绝，不能补假头或忽略审计让它看似成功 |

源码锚点：Controller `previewControlledFile` / `encodePreviewWatermark`；Query `getPreviewMetadataInScope` / `readDistributionPreviewFile` / `readPreviewBinary` / `recordDistributionBodyOpenedIfScoped`；Receipt `recordDistributionBodyOpened`；`GxpAuditServiceImpl#append`；前端 `workflow.ts#decodePreviewWatermark`。

## 关联日志、正式策略与现有测试

- 运行日志 `output/runtime/int_qms/logs/dcc24-categoryuploadmessage/application.log:1245`：`2026-10-18 19:05:19.759` 的业务异常首帧为 `GxpAuditServiceImpl.append(GxpAuditServiceImpl.java:50)`；与 R1 首次进入本次正文时间相邻。日志没有 operationId/requestId，不单靠时间关联下最终根因结论。
- `GxpAuditServiceImpl#append` 的 line 48–50 是 `selectActive(tenantId, operationId)` 为空后抛 `GXP_AUDIT_POLICY_NOT_FOUND`，并非响应编码方法。
- 正式 `IntRuoyiBackend/config/gxp-audit-policy.yaml:70` 已定义 `dcc.distribution.recipient.body-opened`；`:226` 已定义 `signature.dcc.distribution-receipt.create`。YAML 存在不等于 tenant1 运行库已启用；Root 负责精确核对/配置修复。
- `DccControlledFilePreviewDownloadApiTest#previewControlledFile_delegatesToQueryService` 已对普通预览成功响应断言水印头/Expose-Headers，但 Query 是 mock；不能覆盖下发真实审计前置。
- `DccW08DistributionPreviewTest#readsIssuedPublishedBodyAndSavedEventRestoresOnlyThisTask` 验证正式 Query 下发入口，但 `distributionReceiptService` 是 mock。`byteReadFailureAndOpenedWriteFailureCannotProduceSuccessfulReceiptOrBinary` 已证明打开事实失败时不可返回成功 binary。
- `DccDistributionSignatureTransactionTest#actualBodySuccessPersistsHumanReadTimeAndReplayWritesNothing` 显式插入 body-opened 策略才验证真实事务成功；`lateBodyAuditMissingPolicyRollsBackReadAtWithoutSignatureOrNotificationFact` 已验证缺策略时拒绝、readAt/acknowledgedAt 不写入。后者是应保留的守卫回归，不应被“修成通过预览”。

## BDD 与最小 RED/GREEN 设计（待 Root 分配，不冒充已执行）

**Given** 原 R1、tenant1、file4234/A1、distribution273/recipient276、SENT 通知及原有授权保持不变，收件人仍待签；成功普通预览作为响应协议对照，不把别的用户/版本冒充 R1 对照。

**When** R1 从原正式通知进入下发查阅，服务读取精确正式制品后记录真实 body-opened，再由 Controller 返回二进制和真实水印头。

**Then** 应实际渲染 A/1 正文、有真实水印/读取注册，原收件人 readAt 与审计记录一致；不产生签收时间、不改另外两行、不授下载/打印权限。缺失/停用策略或原授权无效时仍明确失败，不能造头、造成功或写假 readAt。

1. **现成 RED**：原 UI 请求 HTTP200 但页面报缺头且正文未渲染，证据已保留。Root 先只读核对 tenant1 的 body-opened 策略是否为启用且与正式 YAML 一致，以及原 readAt/相关审计事实；这是下一步区分配置源头与真实写头问题的最小验证。没有数据库核对前，不新增一个 mock 服务的 Controller 测试声称复现了运行故障。
2. **最小 GREEN 路线**：若 Root 确认缺 body-opened 策略，复用 Root 已授权的精确 test-only 正式配置修复；随后只重走原 R1 通知打开，采集同一二进制自然请求的 path/status 与非敏感 Content-Type、必需头“是否存在”，并以真实 PDF 渲染为通过依据。不得读取认证头/响应 JSON 代替 UI。不需要改 Controller/前端水印协议或 VIEW。
3. **现有定向回归**（由 Root 独占执行）：`DccDistributionSignatureTransactionTest#actualBodySuccessPersistsHumanReadTimeAndReplayWritesNothing+lateBodyAuditMissingPolicyRollsBackReadAtWithoutSignatureOrNotificationFact`；`DccW08DistributionPreviewTest#readsIssuedPublishedBodyAndSavedEventRestoresOnlyThisTask+byteReadFailureAndOpenedWriteFailureCannotProduceSuccessfulReceiptOrBinary`。普通成功响应头沿用 `DccControlledFilePreviewDownloadApiTest#previewControlledFile_delegatesToQueryService`。本线程本轮没有运行这些测试。
4. **仅在策略已正确而原 R1 仍失败时**，再由 Root 分配一个真实 Query→真实 Receipt/GxP 审计端口的预览测试，复用现有 H2 fixture，不另建框架；加入同一正式 binary 经 Controller 的头与 PDF bytes 断言。先得到有效 RED，再确定唯一生产文件写者。单独 mock `readDistributionPreviewFile` 返回 bytes 的新头断言预期现有代码即 PASS，只能作协议回归，不能称根因 RED。

恢复后先正文成功，再 R1/R2/R3 原账号逐人真实签收与 U 逐次未签人数回读。不得触碰 5627、旧文件/旧签名/原计划时间；三行待签事实保持。
