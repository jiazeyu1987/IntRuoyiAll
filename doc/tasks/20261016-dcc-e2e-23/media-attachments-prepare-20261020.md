# E23 音视频普通附件专项：PREPARE/HOLD

## 目标、缺口与当前终态

仅补 E23/E24 尚未执行的“音视频作为普通附件”权限和下载字节验证。本文件是静态计划，不是 E2E PASS，也不改 E24 的覆盖结论。没有启动浏览器、恢复旧 runner、上传或提交任何业务。

原 E23 round4 保留 14 PASS / 1 FAIL / 0 BLOCKED，整体 FAIL/HOLD；六主分支、交互与迟到隔离不重跑。后续 A→B 单分支为已接受 PASS，其独立审计见 `runs/2026-10-20T08-27-03-769Z-709e78bd/trace-privacy-crc-audit.json`。原结果记录 context/browser 已关闭、错误 0；本次只读快照确认旧 PID 1624 不存在、活动 switch-only runner 为 0。旧夹具记录 A/1（2054545668044084233）OBSOLETE、B/1（2054545668044084247）ACTIVE；本阶段未打开页面，不能把历史记录称作新的在线终态核验。新对象尚未创建，ID/BPM/附件 ID 均未产生。

## 短 BDD

Given 原 E23 租户、Author/ProjectReviewer 与三个独立阅读账号、原项目/文件夹/分类/权限，及两个离线格式已校验的本地媒体；When 在一个独立新编号 A/1 申请的“普通附件”上传 WAV 与 WebM，经原审批路线正式生效后由原三个阅读账号分别通过受控浏览进入；Then ViewOnly 能实际播放两附件且无下载入口，ViewDownload 能播放并各确认下载一次、保存字节与本地输入 SHA256 完全相同，NoAccess 稳定拒绝且不能取得两个媒体内容或下载。每一媒体/身份分别记录结果，不以弹框标题或 HTTP 200 判通过。

## 为什么需要一个新申请

原正式详情的 `ControlledFileAttachments.vue` 只有查看/下载入口，无补传入口；附件随上传申请冻结。不能向原 A/B 正式版本追加文件。采用一个新逻辑编号 `DCC241016C23MEDIA20261020`、初始 A/1，复用原项目和分类；源码以“项目 + 分类叶子 + 文件编号”识别逻辑文件，新编号应走新建而非原文件升版。提交前必须由真实页面确认“未查询到同编号受控版本”；撞号或提示已有逻辑文件则 BLOCKED，不改成检出/换版，不自动另造编号。该新建路径目前只有静态依据，尚未在线验证。

## 页面入口与对象来源

| 资源 | 复用来源与约束 |
| --- | --- |
| 租户/账号 | 芋道源码；DCC241016C23Author、DCC241016C23ProjectReviewer、DCC241016C23ViewOnly、DCC241016C23ViewDownload、DCC241016C23NoAccess。来源为本任务 fixture-manifest.json/roleChecks 与既有 DPAPI 文件；本阶段只检查加密文件存在，不解密、不登录。 |
| 项目/文件夹 | DCC241016C23PROJ / DCC241016C23项目，既有项目 #309、DCC241016C23Folder #50，目录 DCC241016C23Dir #913932。来源为 fixture-manifest.json 已有页面回读；执行前重新以页面确认。 |
| 文件分类/类别 | 技术文档 → 设计和开发策划阶段 → DCC241016C23其他 → DCC241016C23章节 → DCC241016C23正文；类别 DCC241016C23附件培训类别 #908766。保持原 VIEW/DOWNLOAD 和审批矩阵。 |
| 源文件/培训支持文件 | 原本地 fixtures/DCC241016C23-source-A.docx 作为本次普通正文输入；沿原需要培训路线时上传 fixtures/DCC241016C23-training-A.pdf 至新申请。二者只读复用，不更改原正式文件，也不以媒体作 SOURCE。上传前分别固化 SHA。 |
| 两个媒体 | 只读复用 E24 的 fixtures/DCC241016C24AUDIO.wav 与 DCC241016C24VIDEO.webm。来源均为仓库任务脚本本地合成；不下载外部媒体，不执行旧生成器。执行获准后复制至 E23 自有 run/inputs，并命名 DCC241016C23MEDIA20261020-audio.wav / -video.webm，复制前后 SHA 必须与准备证据相同。原 C24 内容标记保留并在 manifest 明确来源。 |

Author：文控中心 → 文件上传（`/dcc/controlled-file/upload`），选择原项目/文件夹/分类；`dcc-upload-single-page-workbench`、`dcc-upload-preflight-panel`、`dcc-upload-attachment-files`、`dcc-upload-section-submit`、`dcc-upload-submit-confirmation` 是现有页面锚点。Reviewer 从审批中心新对象待办自然进入各节点，后续培训由 Author 工作台进入。阅读账号从文控中心 → 受控浏览（`/dcc/controlled-file/browser`）筛选新编号，点目标行“追溯”，定位 `dcc-detail-attachments` 内精确文件名。详情 URL/对象 ID 必须由这次真实导航产生。

静态依据：upload/index.vue 的普通附件 multiple、handleAttachmentFileChange、attachmentUploadPurpose=ATTACHMENT、existingUploadIdentityBlockReason；detail/ControlledFileAttachments.vue 的 preview/download；view/index.vue 的 AUDIO/VIDEO 原生媒体元素；后端 DccControlledFileUploadTypePolicy 将 ATTACHMENT 与 SOURCE 分开。静态路径存在不等同本人页面入口已验。

## 所需单次业务动作清单（仅供 Root 放行审核）

1. 新 runner/new context 登录 Author，通过页面确认身份、原项目/分类、原审批路线和新编号未占用。任何必需角色、签名资格、路由或权限缺失即停，不建角色/部门、不改公共或任务权限矩阵。
2. 一次选择一个合法正文和两个普通附件；等三项真实上传完成。表单指定新编号、A/1，保留原直属主管/会签/批准/文控人员及需要培训设置，预检 ready 后检查确认框恰含两个媒体，最终提交一次。立即保存页面产生的新 ID/BPM；提交超时先页面查终态，禁止重提。
3. 由原 ProjectReviewer 按新对象待办完成直属主管审核、会签指派/会签、批准、文控标准化各正式签名一次；各节点以页面状态确认。由 Author 为新对象上传一次线下培训 PDF 并确认一次，沿原路线等待受控/ACTIVE、发布落位和 master 指向 A/1。必需培训是夹具生效前置，不重新验收旧培训分支，不改培训开关规避流程。
4. ViewOnly 本人会话：新对象两媒体行用途为普通附件、上传人/时间/轮次/类型/大小可见；分别在线查看实际 AUDIO/VIDEO，原生控件播放，DOM `readyState >= 2`、无 media error、duration 有效、currentTime 前进；视频尺寸 800×400 且画面内容标记对应来源。附件表无下载按钮，控件保留 nodownload，download 事件 0。读取 DOM 属性只用于断言，不调用 play() 或注入业务。
5. ViewDownload 本人会话：同样分别确认可播放，然后每个附件只点击一次下载并确认一次；合计恰 2 个 Playwright download 事件。保存 suggestedFilename、大小、SHA256、离线格式，逐文件与 inputs 字节 SHA 精确一致。普通下载确认计入本次必要动作，不重跑取消/连续点击/离开/迟到分支。
6. NoAccess 本人新会话：受控浏览元数据行可见时记录内容按钮禁用；使用上述正例真实 UI 产生的该新对象详情原址做独立导航（不拼接口或附件地址），等待稳定拒绝，断言无媒体/附件内容/下载入口、download 事件 0。不能仅因列表无行就把两媒体拒绝判 PASS；若原生地址无法获得则 BLOCKED。

## 最终停点与证据

成功停在该独立新文件 A/1 已受控生效、三个身份附件断言完成、2 个真实下载字节匹配、context/browser/runner 关闭且脱敏 trace CRC/隐私审计完成。保留新文件与待办终态为任务自有证据，不做删除、撤回、换版或数据清理。若任一节点失败/阻塞，立即记录已创建对象、当前节点及副作用，停止剩余写动作，不重复签名或新建替代对象。原 A/B、旧计数、旧 release/gate 和旧审计全部保留。

新证据另存 `runs/media-attachments-<UTC-time>/`：inputs/hash manifest、新编号/对象/BPM、每身份每媒体 DOM/截图/播放属性、自然网络 HTTP 状态、下载事件与文件 hash、finally 关闭证明、脱敏 trace 与独立隐私/CRC报告。登录和密码签名阶段停 trace/截图，凭据仅 DPAPI 内存；任何自然响应不得解析 JSON 作为 oracle，不用 fetch/APIRequest/DB，不导出 storageState/token。NoAccess 不测试媒体作源文件、正文搜索或媒体翻页；本例不扩生产、不构建/Maven/重启/Git/DB/cleanup。

## 离线准备结果与剩余门禁

准备快照、两个媒体大小/SHA/来源/格式及限制见 `media-attachments-prepare-20261020.json`。WAV 全部 PCM 已读且匹配 880Hz/2秒；WebM 的 EBML 边界、VP8 track/尺寸、18 帧块和关键帧头通过结构校验。首次检查器仅考虑 SimpleBlock 而失败，识别实际 BlockGroup/Block 后复核通过，文件未改；这不是产品 FAIL。未发现 ffmpeg/ffprobe/PyAV，因此未做视频完整离线解码，也未验证浏览器解码/播放；这些不能冒充 PASS，实际播放留待放行后的正式页面断言。

当前 PREPARE/HOLD，E2E/业务写入仍 NOT_RUN。Root 需审核这一份单申请动作清单，明确新的单次执行放行和本轮完整源码指纹快照后，才能准备新 runner；旧 A→B gate/release 不复用、不修改。本阶段文档做 UTF-8/JSON/路径/指纹结构验证；任务整体保持 blocked，不提交、不清理、不标 completed。
