# G09 接入与依赖

## A20 已实现的 Office error 事件签名

```ts
const emit = defineEmits<{
  opened: [event: { documentUrl: string }]
  error: [event: { documentUrl: string; message: string }]
}>()
```

仅 DocsAPI `events.onError` 在当前generation、未卸载且捕获documentUrl===当前props.documentUrl时发出；message与子组件保留显示的正式服务错误完全相同。发出前generation失效，拒绝重复error/opened；旧URL即使watch尚未执行也不发。没有新增业务写入或API。

G07父页已经`@error="handleOfficeError"`，现有同URL/未卸载/OFFICE守卫下clearRegisteredPreview并显示event.message。无需修改父页；本轮实际子回调与只读提取的正式父handler组合测试已证明清登记时间/保留原错误，后续真实Office服务UI由Root统一授权验收。

## C04 当前接入（2026-10-15）

Root 已读并转交 c04-integration-hunks.md：G07 接 Query/viewer，G01 接正式 metadata 类型/decoder。原“浏览时间最小合同待定义”段落为历史背景；当前按 C04 合理假设实现“本次浏览登记时间”，不是累计时长。G09 已完成 AccessAudit/PreviewAccess/VO/OnlineMetadata/Office 授权范围、24+12 定向验证，文件冻结待 Review；前端成功画面后的显示/切版 clear 尚由共享 owner 接入验证，不能称已完成。

## 已接入的明确范围

现有 ProtectedPdfViewer 入口自动使用 PDF 工具；不要求父页/API 变更。源数据仍通过既有受权 metadata / preview source 取得，controlledFileId、distribution 身份、精确版本参数不改；worker 只接收源 bytes 副本。搜索和视图变换没有原件保存路径或额外接口。

局部 `ViewerCanvasPageProxy extends PdfTextPage` 声明正式 PDF.js getTextContent 能力，未写 vendor declaration；真实 vendor 提取测试通过。搜索只索引本次 PDF 自有文字，不索引 UI 片段、水印或其他版本。逐页 key 含 renderVersion；文本 await 后复核同一世代。先渲染后提取，避免与逐页渲染并发 cleanup。

## 浏览时间最小合同（待用户明确后 Root 冻结，尚未实施）

- 当前来源：`IntRuoyiFronted/src/api/dcc/controlledFile/workflow.ts` 的 `ControlledPreviewWatermark.timestamp` 是水印时间；不能未经确认当累计时长或正文访问成功时间。
- 当前正式事件：`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/dal/dataobject/protection/DccControlledFileAccessEventDO.java` 有 `controlledFileId/fileVersionNo/processInstanceId/userId/occurredAt`；`service/audit/DccControlledFileAccessAuditService.java#createAccessEvent` 已记录事件。只复核，不修改。
- 若用户要“访问时间”：Root 应先确认展示哪种既有成功事件，再将该事件的服务器 occurredAt 与精确版本、用户和 accessEventCode 一并作为只读投影交前端；无正式事实则显示未记录，不用客户端当前时间或水印时间补齐。
- 若用户要“累计阅读时长”：需 Root 设计正式会话和后端持久合同，按租户/用户/文件版本/轮次绑定，会话起止、可见性/闲置口径、重复 heartbeat 幂等、上限与失联处理须明确；不能把前端倒计时当正式审计事实。接口、迁移、服务均未获 G09 写授权。
- 当前 Office `opened` 事件在进场前已存在，其正式 distribution 保存路径有精确票据校验；不得挪作全格式计时成功，或为计时重复写业务库。

## 媒体与 Office

音视频支持格式、播放和水印保持原实现；是否要求分页类缩放/旋转/搜索和浏览时间等待用户答复。本阶段不删除支持格式、不伪造分页或搜索。

Office 本阶段复核配置只读、加载错误、切版和卸载回执，修复新正文保留旧缩放问题；原生编辑器分页和正文搜索是否可见取决于真实服务，未取得真实 UI 证据，不能宣称该部分已验收。

## 需要 Root review 的文件与事项

只读/水印/精确上下文及异步切换可直接 review 当前 3 个生产文件及 3 个测试。详情父页由 G10 独占，本线程没有写入或接入请求。全量类型及构建仅由 Root 统一发验证票调度；真实 E2E 仍需人类当轮授权。
