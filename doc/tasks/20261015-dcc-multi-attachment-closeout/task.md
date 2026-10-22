# DCC 升版/检入普通附件闭环复核

## Current Status

blocked — loadedAuthority 首次点击窗口已修复并冻结；真实 RED 1 PASS/2 FAIL → GREEN 3/3，受影响组件组 22/22 PASS、0 skip，结构/指纹及 cleanup preview/apply 均通过。仅组件与专属测试变更，新 file 对象接收时绑定加载身份，auth/fingerprint 不重绑旧事实。工程收尾因用户禁止 Git 提交/推送而 blocked，无本轮定向待修项。旧 19、Root 独立 36 是历史回执；Root 唯一决定更广验证。

## 本轮 loadedAuthority BDD

- Given file 事实和 canDownload 在 T1 身份首次接收，props.file 对象随后未刷新；When actor/base tenant/visit tenant 在首次点击前变为 T2；Then preview/download 必须清旧选择并拒绝，不能打开下载确认或发送 API。
- Given 同一 T1 身份且事实未变；When 合法点击；Then 原预览/单次下载保持。
- Given 身份已切为 T2，父级显式提供新的 file 对象；When 点击；Then 以新对象首次接收时的 T2 绑定允许正常操作；auth/fingerprint 变化本身不得重新绑定旧 file。
- 本轮仅组件、专属测试、原任务记录；不改父页/viewer/API，不扩展扫描或重跑其它套件。缺失原文不猜测。
- 生产最小实现：单独 watch props.file 对象（immediate/flush sync）绑定 readAuthority；现有 fingerprint/watch 只负责旧操作失效，不能重写 loadedAuthority。preview/download 入口 ensureLoadedAuthority 不匹配即清选择/预览/下载状态、使 generation 失效并显示刷新提示，确认/API 前返回。
- 最终行为对照：actor/base tenant/visit tenant 分别切换后旧事实拒绝；继续原地改附件名触发 fingerprint 仍不重绑；同身份操作正常；显式新 file 对象接收于新身份后恢复正常。

## 最新轮次：同页状态变化和下载确认并发

- 独占组件内增加附件清单/版本/下载权限/actor/两个 tenant 缓存的事实指纹；响应式事实变化使 generation 失效，确认前/后及回包后直接重读指纹，避免非响应式缓存读被旧 computed 值掩盖。
- 下载原子锁从网络请求前移到打开确认框前；重复点击直接返回，旧回包不能清除新 generation 的锁。
- BDD: 身份缓存变化 -> Given file props 未变且下载确认或响应仍等待；When actor/base tenant/visit tenant 改变；Then 旧确认零请求、旧响应零保存，不扩大服务端权限。
- BDD: 新状态与旧回包 -> Given 同 ID 附件事实原地变化并已开始当前下载；When 旧二进制随后到达；Then 旧预览已关闭、旧回包不保存且不能释放当前下载锁。
- 实际 RED：新清单变化/双击 7 项中 5 PASS/2 FAIL；补身份用例后 8 项中 5 PASS/3 FAIL。最终增加同 ID 新对象、旧二进制与权限对照，3 文件 19/19 GREEN/受影响回归。
- 原共享组件专属回归宿主只补 auth/store 的显式测试端口两行，原测试名/断言完整保留；不改其他 owner 宿主。首次回归未补端口时 5 项因依赖缺失失败，明确为 fixture 接入问题，最终 canonical 命令已通过。
- 上传未保存/取消重试只读：CheckinAttachmentsPanel 的 ready/cleanupAll/retryCleanup/markSaved 与服务票据事务边界均保留，无新缺口，不重复既有套件。
- 当前验证仅上述 3 文件 Node 定向与任务结构/指纹；Maven/shared target/fulltypes/build 仍由 Root 唯一执行。原文缺失和冲突条目不猜测。

## 本次指定边界与验证范围

- 当前分支/基线已核对：int_qms / f06833f06。沿用原任务目录，不新建线程、子 Agent 或重复套件。
- 只读公共 Query/browser/DetailApplicationPanel/SelectedDetailReadService，不改其他 owner 文件和测试宿主；Maven/shared target/fulltypes/完整构建均由 Root 唯一执行。
- Root 当前 Vite 构建输入冻结：生产前端全部只读，专属测试、BDD 和任务文档允许继续。
- BDD: 卸载期间取消旧下载请求 -> Given 版本 A 有两普通附件、下载确认尚未完成且附件组件因刷新/离开被卸载；When 旧确认随后被接受；Then 不再发送 A 的下载请求，不保存文件，也不影响新页面。
- BDD: 卸载期间忽略旧下载回包 -> Given A 的下载请求已开始但响应未到达，组件被卸载；When A 的旧文件响应到达；Then 不调用浏览器文件保存，不把旧结果作为当前版本操作。
- BDD: 同页清单原地变化 -> Given 当前详情对象 ID 不变、附件数组/权限事实被原地替换或清单内容变化且旧确认仍等待；When 旧确认随后接受；Then 旧附件请求和保存必须拒绝，不得把旧清单结果写入当前状态。
- BDD: 下载并行点击 -> Given 同一附件的第一个下载确认尚未完成；When 用户再次点击下载；Then 只保留一个确认/请求，第二次不发送下载，不覆盖第一次状态。
- 该公共组件位于当前独占范围之外；如取得真实 RED，先写 integration note 交 Root 分配，禁止自行修改生产或旧测试宿主。
- 后续 Root 已确认新 RED 并独占转交 `ControlledFileAttachments.vue` 和专属测试，且解除生产前端冻结。仅允许卸载失效最小改动；其它 owner 文件/测试宿主仍保持只读。Root 构建 exit 0/2635 输入零漂移/3239 产物属于修改前回执，不是本次修复后新构建。
- 本轮新边界仍只允许修改同一附件组件及专属测试；若需接入 actor/tenant auth，先以专属 RED 证明旧身份请求可继续，再在同一 owner 范围内最小接入。

## 本轮相邻合并复核授权

- 沿用当前目录；可写自有任务/测试、AttachmentService、RevisionService、CheckinAttachmentsPanel；browser/index.vue 和 DetailApplicationPanel 若需改动先报告 Root；view/index.vue 由阅读线程独占，只读。
- 禁止并行 Maven/shared target/fulltypes/build，由 Root 统一验证票；禁止 E2E、实库、服务、Git 提交/推送、远程和子 Agent。
- 已通过的附件/检入/升版套件不重复运行。无新产品缺口时只更新复核报告、指纹和实际边界。
- BDD: 相邻计时隔离 -> Given 双普通附件及独立正文/培训事实；When 附件进入统一预览器；Then 文件 ID 与附件 ID 不变，普通附件不进入正文阅读计时，也不改写保存清单或培训事实。
- BDD: 精确详情隔离 -> Given 来源指定原文件版本/申请/任务；When 精确详情读取及切换；Then 从原精确 ID 读取完整附件，错误版本拒绝，旧附件预览/下载不带入新版本，保存/确认/重提和普通附件冻结仍使用原正式契约。
- 预期验证：当前源码差异、方法/文件指纹、既有测试覆盖与 Root 回执只读交叉检查；本轮只运行任务文档/指纹结构验证。确有新缺口才追加自有测试 RED/GREEN，越出本轮写范围前先报告。

## 任务目标

在当前 `int_qms` 分支复核并补齐 DCC 新建上传、检入工作稿、正式升版、驳回重提和确认送审的多个普通附件闭环。附件必须使用正式上传票据和精确版本归属，全部上传及保存成功后才允许提交；确认摘要必须展示完整清单；历史培训附件不得混入新版本；失败事务必须回滚并可按同一完整载荷重试。

## 里程碑

1. 读取规约、核对分支/HEAD/既有任务与并行改动。
2. 建立 BDD，静态核对前后端正式链路和现有测试覆盖。
3. 对实际缺口先写 RED，再做最小生产修复并取得 GREEN。
4. 运行定向 REGRESSION，记录未运行的 Maven、服务、数据库和 E2E 边界。
5. 已标记 `ready_for_closeout`，cleanup preview/apply 均通过，临时日志清理；因用户禁止提交/推送，最终记录为 `blocked`。

## BDD

- BDD: 多附件检入 -> Given 当前检出工作稿与两个独立 ATTACHMENT 上传票据均属于同一租户、类别、精确工作版本和 CHECKIN 会话；When 检入；Then 两票均成功绑定到新工作版本，响应返回完整有序清单，任一票失败则正文、检出状态、附件和票据绑定全部回滚。
- BDD: 正式升版 -> Given 选定工作稿已保存普通附件清单且来源/目标版本身份精确；When 提交 INITIAL/PARTIAL/REPLACEMENT 升版；Then 目标版本保存本次附件副本及稳定事实摘要，历史培训记录不复制，清单变更或版本错配明确拒绝。
- BDD: 驳回重提 -> Given 驳回或返工工作稿；When 再次检入/送审；Then 必须重新上传源正文，普通附件仍按独立票据清单校验并归属当前工作版本，不能复用旧正文或旧版本附件事实。
- BDD: 确认摘要与阻断 -> Given 任一附件仍上传中、失败、清理失败、保存响应缺清单或清单摘要变化；When 用户确认提交；Then 展示/校验失败原因并零提交；全部就绪且清单未变化时才提交完整期望 ID 与 manifest hash。
- BDD: 重提验证夹具隔离 -> Given 自定义驳回重提测试复用父类真实服务夹具并继承其测试；When 执行该测试类的全部用例；Then 自定义流程只初始化于子类声明的用例，父类用例使用原夹具且原断言完整执行。

## 预期验证

- 当前门禁：同组件 loaded T1/same authority 三项实际 RED/GREEN；原 dcc-attachment-download-disposal.test.cjs、dcc-shared-attachments-component.test.cjs、dcc-attachment-download.test.cjs 受影响组共 22 项；文档/JSON/最终自有指纹与收尾检查。旧 19 项仅作历史阶段证据。
- 当前独占范围外的公共服务只读；Maven/shared target/fulltypes/完整构建只能由 Root 唯一执行，本线程不运行。Root 后续决定是否需要新增集成验证。
- 历史 211/129 的 Maven/Node 定向命令保留于历史报告，不作为当前重复执行门禁；Root 上轮 125/181/完整类型及修改前 Vite 构建不重写为本次新执行。
- 用户明确禁止：E2E、服务重启、真实业务数据库写入、Git 提交/推送、远程操作和子 Agent。

## 设计约束检查

- 不新增平行上传 API、fallback、默认成功或吞异常。
- 普通附件、源正文和培训记录保持独立事实链；附件票据必须携带精确 session、用途和目标版本。
- 检入 requester、正式升版 OWNER、审批矩阵、版本策略和租户权限沿用现有正式服务。
- 不回滚或覆盖工作树中的并行改动；仅修改当前任务实际缺口及任务记录。

## Cleanup Keep

- doc/tasks/20261015-dcc-multi-attachment-closeout/task.md
- doc/tasks/20261015-dcc-multi-attachment-closeout/execution-log.md
- doc/tasks/20261015-dcc-multi-attachment-closeout/verification-report.md
- doc/tasks/20261015-dcc-multi-attachment-closeout/verification-results.json
- doc/tasks/20261015-dcc-multi-attachment-closeout/validation-sources.json
- doc/tasks/20261015-dcc-multi-attachment-closeout/adjacent-integration-review.json
- doc/tasks/20261015-dcc-multi-attachment-closeout/integration-note-download-disposal.md
- doc/tasks/20261015-dcc-multi-attachment-closeout/focused-boundary-review.json
- doc/tasks/20261015-dcc-multi-attachment-closeout/retained-state-review.json
- doc/tasks/20261015-dcc-multi-attachment-closeout/loaded-authority-review.json
