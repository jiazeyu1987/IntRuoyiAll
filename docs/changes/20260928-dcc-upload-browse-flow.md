# DCC 上传与浏览代码流程

## 范围

本图静态梳理受控文件新建上传、浏览器选择和在线预览。只描述代码实际执行的链路；正式下载、审批、签名等不是在线浏览的替代路径。附件预览及 Office/OnlyOffice 是旁路分支。

## 流程图

```mermaid
flowchart TD
  subgraph U[上传：受控文件新建]
    U0[用户选择主文件] --> U1[upload/index.vue 校验并生成 session]
    U1 --> U2[POST /dcc/controlled-files/upload-preview\npurpose + sessionId + category + uploadContext]
    U2 --> U3[DccControlledFileController 权限门禁]
    U3 --> U4[DccControlledFileUploadServiceImpl\n校验分类、用途、会话、项目上下文、大小与内容]
    U4 --> U5{当前会话是否可复用有效票据?}
    U5 -->|是| U7[复用已存文件与 uploadTicket]
    U5 -->|否| U6[存储临时文件并创建用途绑定票据]
    U6 --> U7
    U7 --> U8[响应文件名、大小、previewKind、ticket/session]
    U8 --> U9{需要上传图纸 PDF 或附件?}
    U9 -->|图纸 PDF| U2
    U9 -->|附件| U2
    U9 -->|否| U10[用户提交表单]
    U10 --> U11[submitter.buildSubmitPayload\nsourceUploadTicket + drawingPdfUploadTicket + attachmentUploadTickets]
    U11 --> U12[POST /dcc/controlled-files/submit]
    U12 --> U13[服务端消费票据并创建受控文件/正式关联]
    U13 --> U14[成功后导航受控文件浏览器]
    U4 -->|格式/大小/权限/会话/上下文错误| UX[记录上传边界失败并向调用方抛错]
    U12 -->|校验或票据失败| US[提交失败反馈；不创建成功结果]
  end

  subgraph B[浏览：列表至在线预览]
    B0[受控文件浏览器加载列表/目录/版本] --> B1[选择目标文件及版本]
    B1 --> B2[openPreview(id) 导航详情页]
    B2 --> B3[详情页加载受控文件详情]
    B3 --> B4[GET /dcc/controlled-files/{id}/preview-metadata]
    B4 --> B5[查询服务校验文件与 PREVIEW 可读权限]
    B5 --> B6[解析正式预览产物及文件记录]
    B6 --> B7{预览产物可用?}
    B7 -->|否| BX[返回明确不可预览/产物缺失状态；不切换成下载]
    B7 -->|是| B8[准备预览访问事件/水印追踪并签发短期 viewer token]
    B8 --> B9[前端携带 viewer token、nonce、访问/水印标识]
    B9 --> B10[GET /dcc/controlled-files/{id}/preview]
    B10 --> B11[再次校验 PREVIEW 权限、访问证据及 token 上下文]
    B11 --> B12[读取正式预览二进制并记录成功审计]
    B12 --> B13[详情页内嵌显示预览]
    B5 -->|无权/对象不存在| BE[返回业务错误并记录拒绝审计]
    B11 -->|证据/token/权限不匹配| BE
    B12 -->|存储读取失败| BE
  end

  subgraph X[预览旁路]
    X0[用户选择附件] --> X1[附件 preview-metadata]
    X1 --> X2[附件 preview 二进制端点\n独立附件 ID 与同类访问证据校验]
    X3[上传响应 previewKind=OFFICE] --> X4[OnlyOffice 短期令牌/回调文件端点]
  end
```

## 代码锚点

| 环节 | 主要代码 |
|---|---|
| 上传页面状态与提交 | `IntRuoyiFronted/src/views/dcc/controlled-file/upload/index.vue`：`handleFileChange`、`handleDrawingPdfChange`、`submitForm`；`upload/submitter.ts`：`buildSubmitPayload` |
| 上传 API 与会话清理 | `IntRuoyiFronted/src/api/dcc/controlledFile/workflow.ts`：`uploadControlledFilePreview`、`cleanupControlledFileUploadSession`、`cleanupControlledFileUploadTicket`、`submitControlledFile` |
| 上传接口和权限 | `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/file/DccControlledFileController.java`：`/upload-preview`、temporary cleanup、`/submit`、preview endpoints |
| 上传服务 | `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileUploadServiceImpl.java`：`uploadPreviewFile`、用途/上下文/内容校验、上传票据创建或复用、OnlyOffice 上传预览 |
| 浏览入口 | `IntRuoyiFronted/src/views/dcc/controlled-file/browser/index.vue`：`openPreview` 与所选版本解析；详情页位于 `views/dcc/controlled-file/detail/index.vue` |
| 浏览 API | `IntRuoyiFronted/src/api/dcc/controlledFile/workflow.ts`：`getControlledFilePreviewMetadata`、`previewControlledFileWithWatermark` |
| 预览授权和二进制读取 | `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileQueryServiceImpl.java`：`getPreviewMetadata`、`readPreviewBinary`、预览文件解析 |

## 关键判读

- `upload-preview` 是取得临时存储文件和用途绑定票据，不等于正式创建受控文件；正式提交才把票据绑定到业务记录。
- 图纸 PDF 和普通附件各有独立用途票据。主源文件票据、图纸 PDF 票据、附件票据在提交请求中分别传递。
- 上传接口按 `dcc:controlled-file:submit` 校验；审批 PDF 用途走 approve 权限分支。具体用途和内容规则由后端 upload type policy 决定。
- 普通在线预览与正式下载使用不同端点和授权语义。预览先取 metadata，再拉二进制；二进制读取会复核 viewer token、访问事件与水印追踪，不能只凭前端拿到文件 ID 直接读取。
- metadata 中正式预览产物不可用时返回不可用状态；读取阶段缺少正式产物时按业务异常处理，不回退到源文件/可编辑文件作为在线预览。
- OnlyOffice 上传预览是特定 OFFICE 类型的短期 token 文件读取分支，不代表普通受控文件在线预览链路。
- 图中的端点和方法按当前工作区源码静态归纳；该工作区当前分支为 `int_qms` 且有既存改动，本图未修改或验证这些改动。
