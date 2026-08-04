# DCC 上传预览 OnlyOffice 文档地址修复

## Task Goal

修复文控中心文件上传页在上传 Office 文件后显示 `OnlyOffice 预览地址未准备好` 的问题。上传预览响应在 OnlyOffice 配置完整时必须同时返回 OnlyOffice 服务地址和带签名 token 的临时上传文件文档地址，前端只展示该正式响应结果，不让用户手工选择或补填预览地址。

## Milestones

- [x] 记录并复现上传 `.xlsx` 后 OnlyOffice 文档地址缺失的回归。
- [x] 后端上传预览响应补齐签名 `onlyofficeDocumentUrl`，且不暴露原始 `fileId` 作为绑定凭据。
- [x] 前端上传预览组件透传并使用 `onlyofficeDocumentUrl`。
- [x] 运行后端、前端静态合同和真实 Playwright E2E 验证。
- [x] 更新任务证据并按 closeout 规则收尾。

## Expected Verification

- 后端 JUnit：上传 Office 源文件时响应包含 `onlyofficeBaseUrl` 与签名 `onlyofficeDocumentUrl`，并继续不暴露 `fileId`。
- 前端静态合同：上传页把响应中的 `onlyofficeDocumentUrl` 传给 `ProtectedPdfViewer`，查看器传给 `OnlyOfficeReadOnlyViewer`。
- 真实 E2E：通过 `http://127.0.0.1:8081` 文件上传页选择真实 `.xlsx` 文件，页面不再出现 `OnlyOffice 预览地址未准备好`。
- 运行态前置：后端 `48081`、前端 `8081`、MinIO、OnlyOffice 容器及 `public-file-base-url` 均真实可用；缺任一项则记录 blocker，不用 API-only 或隐藏错误替代。

## Current Status

ready_for_closeout - 代码修复、后端 JUnit、前端静态合同、真实 Playwright 上传页 E2E、cleanup apply 和隔离构建 worktree 清理均已完成；提交/推送仍需按脏工作区与远端 ahead/behind 状态单独处理。

## Applicable Gates

- DCC 上传预览本机 MinIO 前置门禁：写入型上传 E2E 前必须确认 `docker-minio-1` ready、后端 `48081` UP、前端 `8081` 可访问，并通过真实页面触发 `upload-preview`。
- 本地 OnlyOffice 容器下载地址门禁：OnlyOffice Docker 容器必须能通过 `host.docker.internal:48081` 访问后端下载文件；浏览器加载 OnlyOffice 仍使用 `http://127.0.0.1:8080`。
- 前端静态契约隔离门禁：若全量前端检查被无关历史问题阻塞，使用任务专用最小静态合同证明当前响应字段透传链路。
- 真实 E2E 数据口径：文件分类正式叶子节点为 `技术文档 / 设计和开发输入阶段 / 专利检索与分析报告（如适用）`，自动绑定的文件类别显示为 `专利检索与分析报告`。


## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。本任务不新增默认成功、不静默跳过 OnlyOffice；配置缺失仍显式展示不可预览原因。
- `是否从根因和长期维护角度解决`：是。根因是上传响应缺少 OnlyOffice 文档地址，修复接口契约与前端透传链路。
- `是否存在临时补丁或绕过`：否。
