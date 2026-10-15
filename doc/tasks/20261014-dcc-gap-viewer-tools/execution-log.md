# G09 Execution Log

## 2026-10-14 初始核对
- 实际分支：int_qms；共享工作区已有大量并行改动，仅处理本任务范围。
- 已读根 AGENTS.md、docs/task-closeout-rules.md、docs/frontend-development.md、docs/powershell-encoding.md、docs/test-release-preflight.md，以及本轮协调 requirements.md / coordination.md。
- 用户本轮明确禁止 Git 提交推送，优先于 closeout 文件默认提交要求；不得标工程 completed。
- 现状：PDF 所有页 canvas 预渲染，已有缩放/旋转/水印和 renderVersion；没有 getTextContent、页码导航、正文搜索。
- Office 现有 permissions 全部只读、mode=view，无保存回调；需实际行为验证及新正文加载重置状态。
- 任务自身为唯一 G09/viewer-tools 记录，不复用旧报告 PASS。

## 本轮授权边界补充
- Root 后续明确：浏览时间和音视频范围已向用户异步询问，尚无答复；先完成明确 PDF 分页/文本搜索及只读/水印，不把本阶段当 G09 全项关闭。详情父页依旧没有写授权。

## RED / GREEN / REGRESSION
- 初始 SFC 行为 RED：6 项，5 FAIL / 1 PASS，缺搜索/页码/无文本状态；PDF helper RED：3 FAIL（文件不存在）。
- Office 切版 RED：110% 未重置；新增 3 行 reset 后 4/4 GREEN。
- 渲染中切版 cleanup RED：旧文档未销毁；改直接 destroy 后 GREEN。
- 搜索真实 watch 调度 RED：新命中在 nextTick 被清空；flush=sync 后 GREEN。
- 最终开发行为 19/19 PASS；含真实 vendor PDF.js 读取多页文本和纯 raster PDF，实际 SFC script/template 驱动导航/变换/逐页水印与隔离。完整命令和能力边界见 verification-report.md。
- Office 相邻静态 7/7；common preview 源接线静态 exit 0；追溯入口最终 3/3。追溯入口中途并行窗口 1 FAIL，未改受保护父页，之后当前源码重跑通过，未认领为本任务修复。
- helper TypeScript 0 diagnostics；实际 SFC ViewerCanvas 与 PDF.js 声明的局部边界 0 diagnostics；定向 ESLint 三生产文件 errors=0 / warnings=0；diff --check exit 0。
- 两次验证探索失败未作为产品失败：直接 Node 导入当前 PDF.js 缺 DOMMatrix，后改使用现有真实 canvas 模块；简化类型实验与仓库不等价，最终针对正式实际 ViewerCanvas 接口核验通过。未改 vendor/依赖。

## 第一阶段交付
- 新增 changed-files.md、integration-notes.md、review-ready.md、verification-report.md；生产只动授权 3 文件，presentation.ts 保持进场 hash。
- ready_for_closeout 仅表示已明确范围可 review；浏览时间/媒体、真实页面、统一构建验证及 Git 门禁未关闭。

## 清理与状态
- task-closeout-cleanup preview：在 ready_for_closeout 状态执行，exit 0；7 个任务文件全部 keep，delete 为空，warnings none。
- 依 docs/task-closeout-rules.md 的“当轮禁止 Git 提交推送”规则改为 blocked，不标工程 completed。
- cleanup apply：明确报 `BLOCKED task status is blocked; expected ready_for_closeout or completed`，当前 shell exit 1；未删除文件，不能记作 PASS。
- 没有向其他聊天发消息；报告由 Root 主动读取。等待后续接入授权。

## 2026-10-15 C04 新授权启动
- 已再次读取 AGENTS、closeout、backend/frontend/database 对应规则和协调 C04；实际分支 int_qms。
- C04 明确：PREVIEW 原事件持久化读回并核对完整身份/时间；VO/公共在线预览只读字段；Office 视图旋转/复原。其余共享文件给 hunk，不实施。
- 当前 AccessAudit 仅 processInstanceId 非空做读回，且未校验 occurredAt/type/result；普通 PREVIEW 无持久成功证明。本轮只补 PREVIEW 分支并保护其它审计语义。
- 当前 issuedAt 使用 UTC LocalDateTime 纳秒，而现有 H2 fixture occurred_at 为 DATETIME；将核对正式迁移秒精度再锁定同一秒值，禁止新 schema。
- 保存 5 个新授权生产文件的 baseline-c04-sha256.json；沿用唯一 G09 主目录，不新建重复任务。

## C04 本轮验证与交还
- 后端RED：当前源码私有javac/JUnit、真实H2事务/HMAC 6项/5失败；GREEN6/6。mapping受控移除RED10/9通过/1失败，再恢复正式UTCoffset映射。
- 完整定向regression首轮24/22通过/2失败：旧PreviewAccess测试tenant31请求与BaseDb默认tenant1不一致；只在自有相邻setUp显式tenant31（改前hash已保存），保留所有原断言；regression2当前源24/24 PASS、0skip、SOURCE_STABILITY PASS。
- Office旋转真实SFC测试RED，容器height100%单独RED，实施后Office5+相邻7共12/12 PASS，定向lint0errors/0warnings。
- C04-hunks候选decoder/Shanghai与成功世代状态2项PASS，不能称共享view接线验收。
- Root主动读取hunk并正式转G07 Query/viewer与G01decoder，随后回收冻结5生产文件和测试；本线程不再写共享范围。
- Root修正unavailable：无PREVIEW事件/时间合法，原具体错误优先；Online已有原事件后unavailable保留真实time但不叫正文成功；offline成功正文显示未记录。只追加本轮Online受影响5/5与hunk/当前正式decoder无event分支3/3 PASS，无生产改动。
- c04-verification-report和四交付主报告顶部均同步24+12与增补5+3，不再只报旧19项。完整源码reactor/E2E/部署未运行。
- 当前无活跃Java/Node验证句柄；等待Root审查，不自行发消息。
- C04 cleanup preview在ready_for_closeout时exit0，keep14/delete139，逐项delete只落在本任务c04-*验证产物；记录完整证据后按Git禁止与待集成门禁改blocked。apply实际BLOCKED shell exit1，无删除；不是PASS，不标completed。

## 2026-10-15 A20 Office错误事件补齐
- Root明确追加OnlyOffice子组件精确URL/generation的error({documentUrl,message})事件；父G07已监听，禁止写父页。
- 已重新核对int_qms及AGENTS/frontend/closeout/A20分派；保存组件和测试改前baseline-a20-sha256。
- 当前onError仅修改子组件message与销毁editor，未emit error；且未核props URL变化/watch执行前窗口。以真实SFC脚本回执测试覆盖后再实施。
- RED node --test tests/unit/dcc-g09-office-readonly.test.cjs：8项/6通过/2失败，afteropened缺父error，URL变动watch前旧错误污染。
- 最小实现typed error事件、当前精确URL守卫和正式onError emit；发出前原generation失效，message保留显示。只改2授权文件，不写父view/业务API。
- GREEN/REGRESSION最终Office实际SFC9+相邻只读/缩放7共16/16 PASS；实际子callback→当前父正式handler组合清时间/保留message；无E2E宣称。定向ESLint0/0，SFC编译与diff检查PASS。
- 一次apply_patch写入失败，确认文件内容未变/有权限及空间，顺序重试最小hunk成功，未改ACL或环境；不是代码验证失败。
- baseline-a20与final-a20保存组件/测试SHA；C04旧同两条仅历史，当前以A20为准。后端测试及全量类型未重复运行。
- A20状态先ready_for_closeout，cleanup preview exit0 keep16/delete139（沿用本任务C04私有产物）；按未授权Git收尾改blocked，apply实际BLOCKED shell exit1、零删除，不记PASS。已交还请求，后续不写2文件，待Root review。
