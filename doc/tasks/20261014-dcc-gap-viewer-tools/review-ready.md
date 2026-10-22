# G09 Review Ready — 最新 A20 Office error（2026-10-15）

## A20 最新交付

已补子组件 `error: [event: { documentUrl: string; message: string }]`。仅真实 DocsAPI onError 且当前 generation/精确URL/未卸载才发；保留子组件真实可见错误、使该generation失效，旧URL/watch前窗口/同URL旧generation/卸载回执不通知父页或覆盖新画面。无业务写入、未改父view或其他冻结生产。

当前真实SFC与相邻定向 **16/16 PASS**，含实际子onError→当前父handleOfficeError组合后清登记时间/保留错误消息。初始RED8项/2失败（缺error事件、URL已变watch前迟到错误污染）；GREEN后8/8，添加实际父handler组合再最终16/16。定向lint0errors/0warnings、SFC编译与diff检查通过。A20最终2文件hash见 final-a20-sha256.json，覆盖旧C04同两文件指纹。

请求Root接收组件与自有测试并重新冻结。父页原已订阅无需新增hunk；精确事件签名和证据见 integration-notes.md / verification-report.md 顶部。未运行真实Office UI、E2E、后端重复回归或全类型；累计时长/媒体分页仍未关闭。

## 最新 C04 交付

后端当前源码私有编译/真实 H2 事务及 HMAC/metadata 回归 **24/24 PASS、0skip**；Office 当前实际 SFC script/template 和相邻配置/缩放合同 **12/12 PASS**。Root不可用修正后追加OnlineMetadata **5/5 PASS**、hunk/正式decoder不可用分支 **3/3 PASS**（与前述测试有重叠，不算页面接线通过）。

已实现 PREVIEW 原事件持久化读回与 id/code/tenant/user/file/version/process/type/purpose/result/occurredAt 核对；签发前固定 UTC 秒，与现有 DATETIME 精度相同；VO readonly previewRegisteredAt 及 OnlinePreview UTC ISO offset 映射；Office 纯可视旋转/复原。未新增表/端点/重复访问事件，非 PREVIEW 分支保持原检查。完整本轮证据见 c04-verification-report.md，生产/测试最终 hash 见 final-c04-sha256.json。

Root 已主动读取并正式转交 c04-integration-hunks.md：G07 接 Query/viewer、G01 接 metadata decoder。G09 的 AccessAudit/PreviewAccess/OnlineMetadata/VO/Office 与专属测试现按 Root 指令冻结等待 review；不再实施共享接线或重复验证。累计时长、媒体分页、Office 真实 UI 仍未关闭。

不可用metadata可以没有原事件/登记时间，decoder保留原具体失败而不是missingTime；成功metadata必须有正式时间。离线blob成功正文显示未记录。上述hunk已修正，不写共享生产。

以下为已被 A05 接收的首轮历史记录，不是本轮 C04 验证统计。

## 当前交付

PDF 已补真实正文搜索/处数/正文片段/命中页跳转、上一页下一页/输入页码、滚动页码同步、扫描件无文本明确状态；现有缩放/旋转/水印及源文件只读保持。切版/卸载隔离文本与渲染；Office 新正文加载重置视图状态并完成只读配置行为复核。

自有开发测试最终 19/19 PASS，Office 相邻 7/7、追溯相邻 3/3、common preview 静态合同 PASS；SFC 编译、helper 严格类型、定向 lint、diff 检查通过。真实 RED 和准确边界见 verification-report.md。

## 尚未完成

- 浏览时间定义和音视频工具范围待用户答复，已交最小后端合同建议。
- 未运行真实页面/E2E、Office 服务能力验收、全量类型/构建；没有 Root 唯一票据，也没有 E2E 授权。
- Git 提交推送/工程 completed 未授权。G09 全项不能关闭。

## 文件归还请求

请求 Root 复核并接收 `view/index.vue`、`view/OnlyOfficeReadOnlyViewer.vue`、`view/pdf-tools.ts` 和专属 3 测试；`view/presentation.ts` 保持原 hash。本阶段之后本线程不继续写这些资产，等待 Root 后续接入授权；不自行写共享详情父页、公共 API 或后端。

进场前查看器已有 opened/Office 世代保护；只依据本目录 baseline-sha256 与本轮最小差异认领成果，不能把相对 HEAD 的全部 diff 算入 G09。
