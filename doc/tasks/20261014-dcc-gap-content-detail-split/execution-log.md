# G10 执行记录

## 2026-10-14 初始复核
- 已读根 AGENTS.md、docs/task-closeout-rules.md、docs/frontend-development.md 对应入口/历史快照/静态合同/类型检查门禁、database-rules/login-access 对应权限约束，以及协调 requirements.md/coordination.md。
- git branch --show-current：int_qms。工作区含大量既有并行改动，不动无关资产。
- 当前 viewer 分支挂载完整 BasicInfoPanel 和 ControlledFileBrowseRecords；后者包含审批、附件、关系、历史。原详情跳转保持 ID，但覆盖来源为 browser 并丢失原审批查询参数。
- 原 closeViewerMode returnTo helper 只接受 DCC 来源；需补充站内已注册审批来源返回，不能开放外部地址。
- 当前测试 dcc-browser-html-alignment 有“正文包含完整记录”的旧断言，与本轮需求直接冲突，需要按新业务契约更新所属断言。

## 修改前 SHA256（文件原始字节）
- detail/index.vue：3D5578626A6C44F645FF686BECF8EAC1B49A54F8BA6ADCCA3474575C79168CF4
- detail/ControlledFileBrowseRecords.vue：1D1934A01E0C04415A9E45688DEBC21F0E588538160D3A575C47250C999D8B97
- shared/ControlledFileBasicInfoPanel.vue：D3A176035EACFA4463D6564899C2F6BD82EA0E03C2CFB280C6E94BA66DBACF2B

## 验证边界
仅允许隔离开发测试；没有 E2E/服务/数据库写入/Git 提交推送授权，没有 Maven/全类型/大构建唯一票。任何开发测试 PASS 均不等于真实运行验收。

## TDD 第一轮
- RED: `node --test tests/unit/dcc-content-detail-split.test.cjs`（cwd IntRuoyiFronted）exit 1，5项中4项失败：正文缺少规定摘要、缺少独立只读详情分支、导航丢失原查询合同、审批来源被改成浏览页。陈旧/错误 ID 禁导航原已通过。
- GREEN 同命令 exit 0，5/5，SFC真实编译渲染和正式handler执行。
- 后续补充完整基本信息面板保留于详情、指定审批轮次锁定以及无可用详情不挂载正文/记录，待定向回归。

## 相邻所属测试修改前 SHA256
- tests/unit/dcc-browser-html-alignment.test.cjs：AE323563F6B4151BF601B7F7FBEFEECE5D62AF5E41BC420CCB252052E1FAD2AA
- tests/unit/dcc-viewer-trace-entry.test.cjs：7653A21B71F5109E8DB384A35B0492081B07D408849F91FDD9333979F7C4ABDC
- tests/unit/dcc-readonly-metadata-gate.test.cjs：1EC633798334A121301F913F69079562E8BACE291A17BC3330A464586AE0FE1A

## TDD 第二轮 / Root 转交
- Root 明确转交 G08 shared/version-display.ts 显示合同，允许本线程三文件直接接线；不写该 helper，raw versionNo/Long ID保持。
- 正文已送审工作稿 RED → helper 接线 GREEN；BrowseRecords/BasicInfoPanel 恢复改前展示行实际 RED 2项 → GREEN 2项。
- History 轮次测试升级为真实 Vue 编译 prop 投影，修复 __props 夹具命名冲突后重做正式 RED（latest轮次覆盖请求轮次）→ GREEN。
- 最终定向回归 10份文件，exit 0，70/70；三生产SFC脚本/模板编译通过；定向 ESLint errorCount=0；diff --check exit0。
- G11 History事件尚未冻结，已在integration-notes.md明确两个父入口、正式类型和null清理点，暂不猜事件合同。
- 再读task-closeout-rules.md；Git未授权和cleanup工具不可用如实记为收尾限制，保留当前阶段报告，不标completed。

## 2026-10-15 Root review 修正
- 核对 getControlledFile/query 的 setTitle(file.getTitle()) 与 setFileName(file.getFileName()) 为两个独立正式字段，上传表单 fileName 为用户输入完整名称；不能用 title 冒充 fileName。
- 实际 Vue 渲染新 RED 两项：返回不同 title/fileName 时正文显示了内部简称；缺 fileName 时仍挂载正文并借 title 显示。改为 fileName 并用正式 viewerIdentityError 明确报错、禁止进入详情/挂载正文后 GREEN；主测试现为9项。
- 保护现有陈旧/错 ID guard，缺名称不改其他版本/不假定当前名。BrowseRecords 也去除 fileName || title。
- A04 事件已出现 typed payload；G11 integration-notes 更新但本次读取 review-ready/verification-report 尚无正式 A04 复核结束证据。按 Root 指令继续等待其 review-ready A04，当前不提前接线。G07正文入口context尚待Root共享API接入，本线程不改Protected viewer/API。
- 最新重跑出现共享合同时序变化：BrowseRecords helper要求usage.tenantId/sourceControlledFileId，已更新所属fixture并18/18PASS；History新增relation-contract import导致application-round-context测试6项harness失败，未获此混合测试写权限，不改不称PASS。
- 最新9份G10/相邻定向测试61/61PASS；三Vue SFC再次编译及定向ESLint error0；文档六份存在/章节验证PASS，无临时构建资产。本阶段等待A04接入冻结或Root正式归还父页。

## A04 正式冻结接线轮
- Root已核准G11 review-ready A04回执及6Vue测试，授权立即接两个父入口。本轮重新核对分支int_qms和对应规则。
- 改前SHA256 index.vue：1D77D10EFE520E6D32DD092310ED38F42CC9CFED2D24B1EDE7FBC591CA8CDC3D；BrowseRecords：53C30ACE67748C0C6251EA99D163B9AB78680C1869B9AAAFACCD945A6ADAA481。
- BDD补充申请A→B的同步null隔离/旧事件拒绝，以及明确无BPM任务上下文的版本视图undefined；不猜申请类型/属性轮次，不补冻结snapshot。
- 新真实Vue接线RED 6项4FAIL，申请context下实际relations prop为undefined；GREEN6/6，再扩展锁定BPM和属性轮次拒绝8/8。旧History callback跨context无法发布；同ID新file对象/读取序列触发清null与History重建。
- 组合12份89/89PASS，正文9和A04事件8及G11事件6/关系14全部实跑；不把字符串存在当接线证据。
- Root随后授权两相邻harness：改前round-context SHA256 6554CFAEDB3EF61328DF12914456C70745D53CFBF8AB234906E896333C148F1B；nc findings 7B612BDF9DBD821B7CA9D63082E17525DB50E0AE545727B7901E5D5E39A9894B。补真实module加载及正式非viewer路由宿主；原行为保持，后续只定向25/25PASS。
- 两生产VueSFC再编译PASS，lint error0（仅index第2行既有warning）；授权范围diff-check exit0。已先ready_for_closeout，生产文件归还Root排期，Git未授权按规则最终blocked，不completed。

## Root最终接收 / 本轮仅报告
- 来源为2026-10-15 Root最新分派和协调coordination.md“续派A12：详情父页交还”（216起）；Root独立正文9+父入口selection8共17/17PASS，正式接收G10软件范围及A04identity/null隔离。
- G10源码/tests停止写入；未重复执行回归、没有fulltypescript票。本轮仅修正自有六报告中的当前/历史时点，保留各命令实际记录，不把70/61/89/25转写为后续owner新源码总PASS。
- 冻结交还SHA来自上一实现轮真实Get-FileHash输出（changed-files.md）；本轮不重取后续owner变动的生产指纹冒充G10交还结果。
- 只读目录核对六份Markdown存在，无临时日志/截图/脚本/编译产物；无删除。task-closeout-cleanup preview/apply此前未执行/本轮未执行，缺可用能力；该目录核对仅清理范围证据不是正式preview/apply PASS。
- 生产后续归属index.vue G08→G07→G01，另外两文件Root保护。软件范围accepted，工程状态blocked仅因Git收尾未授权/cleanup不可用，无运行态/E2E扩大。
