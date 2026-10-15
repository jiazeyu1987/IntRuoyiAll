# G09 正文浏览工具补齐

## 任务目标
在受控分页正文查看器补齐 PDF 真实文本搜索、无文本提示、页码导航；保持缩放、旋转、水印、精确版本隔离和原件只读。复核 Office 只读能力。媒体工具及浏览时间口径等待用户明确，不据此声称全项完成。

## Current Status
blocked — A20子组件error事件与实际父handler组合已ready_for_closeout并清理preview，定向16/16 PASS；2文件申请归还Root。等待review/集成，Git/E2E/大构建未授权，不关闭G09全项。

## 里程碑
1. 独立复核现有代码和相邻测试，保存改前 SHA256。
2. BDD / 可执行行为测试 RED。
3. 在授权范围实现并取得 GREEN / REGRESSION。
4. 输出 review-ready、changed-files、integration-notes、verification-report，交 Root 复核。

## Expected Verification
- 独立 Node 测试：真实 PDF 文本提取、多页命中和扫描件空态；实际 SFC 行为测试：页码/翻页/变换、切版/卸载、防护及只读 Office 配置。
- 定向 SFC 编译、TypeScript 工具检查、相邻静态合同和 git diff --check。
- 全量类型/构建仅 Root 唯一票据后运行；E2E 未授权不执行。

## 设计约束检查
- 已核对实际分支 int_qms；不切分支，不提交推送，不创建 worktree，不启用子 Agent。
- 只允许写 IntRuoyiFronted/src/views/dcc/controlled-file/view/index.vue、OnlyOfficeReadOnlyViewer.vue、presentation.ts 及本任务专属 helper/tests。
- 首轮不修改 vendor/pdf.*、详情父页、公共 API 或后端；C04仅后文明确授权4个后端文件与Office，详情父页/公共API/vendor仍禁止。
- 搜索只读取本次 PDF 的真实 textContent；无文本明确提示不可搜索，不 OCR、不猜测正文。
- 任何异步结果必须绑定加载世代；切版/卸载清空页码和搜索状态。
- 缩放/旋转只改变视图；PDF.js 使用源 bytes 的副本，禁止保存或回写原件。
- 水印、防复制/打印/下载和权限请求契约保持。
- 首轮浏览时间未明确，仅交设计；C04后按Root公开合理假设，只读既有PREVIEW原事件登记时间，不创造累计时长或新会话。

## BDD
- BDD: PDF 正文工具 -> Given 多页含真实文本 PDF / When 搜索并跳转命中页、上一页下一页或输入页码、缩放旋转 / Then 页码和画面对应、所有页仍带水印、源 bytes 保持不变。
- BDD: 无文本 PDF -> Given 扫描件或纯图 PDF / When 提取正文 / Then 明确提示当前 PDF 无可搜索文本，搜索不产生假命中。
- BDD: 加载隔离 -> Given 旧 PDF 文本或渲染仍在进行 / When 切换版本或卸载 / Then 旧结果不更新新页码、搜索、画布或 opened 事件。
- BDD: 明确失败 -> Given 文本提取或渲染失败 / When 查看正文 / Then 展示真实错误，不伪装为无文本或成功。
- BDD: Office 只读 -> Given Office 正文 / When 初始化和切换 / Then edit/comment/review/download/print/copy 全为 false、view 模式、不配置保存回调，旧加载不能发 opened。

## Cleanup Keep
- doc/tasks/20261014-dcc-gap-viewer-tools/changed-files.md
- doc/tasks/20261014-dcc-gap-viewer-tools/integration-notes.md
- doc/tasks/20261014-dcc-gap-viewer-tools/review-ready.md
- doc/tasks/20261014-dcc-gap-viewer-tools/baseline-sha256.json
- doc/tasks/20261014-dcc-gap-viewer-tools/baseline-c04-sha256.json
- doc/tasks/20261014-dcc-gap-viewer-tools/c04-integration-hunks.md
- doc/tasks/20261014-dcc-gap-viewer-tools/c04-verification-report.md
- doc/tasks/20261014-dcc-gap-viewer-tools/run-c04-java.ps1
- doc/tasks/20261014-dcc-gap-viewer-tools/G09TestLauncher.java
- doc/tasks/20261014-dcc-gap-viewer-tools/baseline-c04-adjacent-test.json
- doc/tasks/20261014-dcc-gap-viewer-tools/final-c04-sha256.json
- doc/tasks/20261014-dcc-gap-viewer-tools/baseline-a20-sha256.json
- doc/tasks/20261014-dcc-gap-viewer-tools/final-a20-sha256.json

## C04 追加目标与 BDD（2026-10-15）

先只读显示本次浏览登记时间：正式 PREVIEW 访问事件 occurredAt 与 issuedAt 同一 UTC 服务器时刻，响应 previewRegisteredAt 是 ISO offset String，Shanghai 展示；不是累计时长、水印时间或客户端 now。只在成功正文画面后显示，切版清空旧时间。

- Given 普通或精确审批轮次 PREVIEW / When 原事件 insert/readback / Then 同 id/code/tenant/user/file/version/process/type/purpose/result/time 全部一致才发 token；零行/错行/错时/异常真实拒绝，晚失败原事务回滚、不加事件流水。
- Given 既有 DATETIME 秒精度 / When 生成服务时间 / Then 先规范到秒后同值写入并读回，禁止以不一致时刻修补读回；响应明确 Z/UTC offset。
- Given 已核验 accessResult / When 公共在线预览映射 / Then previewRegisteredAt 来自 verified issuedAt，缺失拒绝；原 business/sourceByTenant 权限不变。
- Given Office 只读正文 / When 旋转、缩放、复原、切版 / Then 仅 CSS 视图几何改变、原 document URL/只读权限不变、水印仍覆盖、复原与切版归零；原生分页/搜索不改。
- Query 映射、workflow metadata 类型/decoder、view 成功画面状态由 G07/G01 独占，只提供精确补丁与验收说明，不写这些生产文件。
- 独立 Java 源码/JUnit 在私有目录运行；不写共享 target，不执行 Maven。真实数据库/服务/E2E 仍禁止。

## A20 BDD（2026-10-15）
- Given 当前Office已opened / When DocsAPI真实onError / Then 子组件保留正式message并emit error({documentUrl,message})一次；打开回执不可重发，父G07可清登记时间；事件无业务写入。
- Given 文档URL已变但watch未执行、或新generation已创建（含同URL重挂） / When 旧onError迟到 / Then 不emit旧错误、不覆盖新message、不销毁新editor。
- Given 组件已卸载 / When 旧DocsAPI onError / Then 无事件、无状态回写。
- 先SFC行为RED，再最小production GREEN，回归Office及相邻配置/缩放合同和定向lint；不跑已冻结后端或全量类型。
