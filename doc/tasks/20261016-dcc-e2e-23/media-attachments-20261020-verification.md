# E23 普通音视频附件补充分支

业务 PASS：6 PASS / 0 FAIL / 0 BLOCKED；Root 已接受业务结果，最终证据也已由Root独立review通过。cleanRun=false，原 HTTP400、六处 SCRIPT 失败及首次审计 FAIL 均保留。未改产品代码，也未重跑原六主分支、交互或迟到分支；原 round4 仍为14 PASS / 1 FAIL / 0 BLOCKED、FAIL/HOLD。

## 对象与单次操作

- 新编号：DCC241016C23MEDIA20261020；file2054545668044084384，A/1，页面明确显示 ACTIVE / 受控（已生效）。
- BPM：be2a4856-cc83-11f1-ada5-b082e25ec548。沿原项目309、文件夹50、类别908766、taxonomy叶168及原权限/审批矩阵。
- Author源件与两普通附件上传成功；一次submit响应200。ProjectReviewer直属主管、会签、批准、文控四次approve-task响应均200；会签指派一次，不重复签名。
- Author培训PDF上传一次、确认一次；training-record请求12:51:55.860Z开始、12:52:16.595Z结束，20.735秒。脚本在12:52:05.867Z记录pending和DOM；当时仍待培训、对话框未关闭。用户可见报告晚于脚本观察，已披露时间差。无pending线程栈，不据此诊断或关闭E13慢因。

## 三身份实际结果

| 身份 | 音频 | 视频 | 下载事实 |
| --- | --- | --- | --- |
| ViewOnly | PASS，currentTime 0→0.217525秒 | PASS，0→0.184231秒 | 附件下载按钮0，原生下载0 |
| ViewDownload | PASS，0→0.382912秒 | PASS，0→0.190117秒 | 两次警示确认、恰两次原生download |
| NoAccess | PASS，拒绝 | PASS，拒绝 | 独立本人会话13次稳定观察，无媒体/附件行/下载按钮或download |

四个可播放正例均由真实原生控件点击，readyState=4、error=null；音频duration=2秒，视频2.023614秒、800×400。两份实际视频截图均显示来源标记DCC241016C24 VIDEO BODY MARKER、Original frame 3及本人水印。未通过JS调用play、修改媒体状态或读取自然响应JSON充当oracle。

两媒体用途明确为普通附件，上传人、时间、首次提交轮次、MIME和大小均在各本人附件表可见。音频与视频保留控件nodownload；仅有下载权限账号通过独立附件按钮及非受控警示确认下载。NoAccess从本人正式受控浏览目标行进入操作面板，页面明确Current user cannot access this controlled file。

## 两份下载

| 文件 | 字节 | 上传输入与下载SHA-256 |
| --- | ---: | --- |
| DCC241016C23MEDIA20261020-audio.wav | 64044 | c62ba5d9ddd4398a1e55b72e0142a85b15a1e981b17eb105375db8fdac5a388e |
| DCC241016C23MEDIA20261020-video.webm | 37195 | 4126a2c95172e2b834def9855669696bd184b8ac50f1d5f5ed9ac100f32d43c8 |

输入为已披露的本地合成C24素材，只读复制；WAV全PCM/结构及WebM VP8/EBML结构准备证据保留。实际浏览器已完成解码播放。正文采用原docx字节的唯一名称副本，SHA27d785b616d7e4698afa9ba1a6c493449a7bc8e98a3873547fd39a290b0e3a5d；媒体未作为SOURCE。

## 原始失败保留与解释

首次run2026-10-20T12-34-24-643Z-709e78bd的原正文名被已受控/保留期文件占用，upload-preview明确400；未创建申请或BPM。后端SOURCE新上传在存储预览之前执行只读name-claim预检，这是既有业务约束，非媒体缺陷。Root授权只纠正源名，复制相同合法docx字节后在当前run执行一次申请，原失败独立保留。

当前run的script-failures.json保留六处SCRIPT_OR_ASSERTION_FAILED：02等待旧详情落点，06等待旧指派提示，07在待办提前找批准按钮，10旧按钮名“批准”与实际“批准通过”不同，15过严URL尾部不接受原生query，16视频点击误落进度条。均仅纠正局部脚本和同对象只读定位；02/06成功写动作未重做，07/10/15无签名业务写入，16未上传/重建媒体。后续原生待办、实际控件与节点终态完成核验；不宣称首次无错连续通过。

## Trace审计、结束门禁与停点

10份原样保留的脱敏ZIP全部读取、CRC通过：3570条目、9858行JSON。5个账号DPAPI及缓存凭据成功在内存比较，3个distinct已知凭据零命中；列明敏感字段/query、真实Bearer和JWT均0。raw0；登录/签名输入期间停trace/截图。

首次审计FAIL保留为trace-privacy-crc-audit-initial.json：01与06的相同JS entry resources/2b53840448687a96c734cbe29986a5f0fae1c41e.js，各4处匹配全部严格等于已脱敏占位符。仅本run审计在所有原始Bearer候选均是已知占位符时排除entry级误报，保留parsed-value/真实Bearer/JWT检查。ZIP未改、digest未变；修正后的trace-privacy-crc-audit.json为PASS。范围限已知凭据和列明模式，未证明未知/图片嵌入秘密；raw已由原sanitize验证后删除，不能回溯审计raw。

结束end-source-gate.json为PASS：33个前端源码SHA、后端Jar SHA、driver SHA均与执行release一致；41508/48061、20976/8061及Jar/Vite归属一致。全部context/browser关闭且关闭错误0，runner28984不存在。执行过的每个片段SHA与stage记录原样保留。

唯一交接入口media-attachments-20261020-handoff.json；当前run目录runs/2026-10-20T12-41-36-535Z-709e78bd包含结果、输入/下载、DOM/截图、分段脚本、原失败、trace、两次审计与结束门禁。准备期md/json保持历史。任务状态blocked，未Git提交/推送、未cleanup apply、未删除新ACTIVE对象及相关媒体/培训/流程；此为有限封存后的最终停点，不再执行后续业务或实验。
