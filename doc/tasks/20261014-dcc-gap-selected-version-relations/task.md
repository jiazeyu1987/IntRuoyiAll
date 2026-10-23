# G11 历史详情关联不串版

## Goal
所选版本/申请的关联仅显示自身冻结记录，固定目标版本；保留上传、后补、整改证据。引用来源及去重项目数量按正式持久 selectedControlledFileId 记录过滤所选版，允许后补引用；不要求新增整个引用列表的申请提交快照。

## Authorization
初始 WRITE_GRANTED：IntRuoyiFronted/src/views/dcc/controlled-file/detail/DetailRelationsPanel.vue、relation-contract.ts、专属测试及本任务目录。其他初始共享仅只读，后续追加以A04/A07为准。禁止子 Agent、Git 提交/推送、切分支、worktree、E2E、重启、远程及真实业务库写入。无 Root 唯一验证票，不执行 Maven、全类型检查或大构建。

A04 追加唯一写入：detail/DetailApplicationHistory.vue、detail/browse-records.ts、api/dcc/controlledFile/relations.ts、relations/reference-usage.ts、relations/DccReferenceUsageDialog.vue、相关专属及混合旧测试必要断言/加载器。父页 index/BrowseRecords 仍 G10，applicationRead/Query、G08 DccProjectReferences.vue 未授。BE Reference Service/Store/Contracts/Controller 与所属测试预授但 M01 未释放，当前不得 Java/test 写入。

A07已释放M01源码冻结，BE预授生效，可在上述明确范围写当前源与所属测试；使用任务私有javac/H2隔离验证，不写共享target。Maven/全类型票仍Root独占未授。

最新只读交叉review授权：G11原生产/测试继续冻结；只读当前custom冻结source/Bound/Resolver/Workflow、History事件/未记录空态和正文附件引用所选版边界，可运行3-5个已有独立Node/handler场景，不跑旧全量/Maven/type/E2E；发现仅写review-followup.md供Root分发，不修他owner文件、不跨线程发消息。

A33回授：DetailRelationsPanel.vue、DetailRelationStageEvidence.vue、DccReferenceUsageDialog.vue及必要专属/相邻测试，修RF11-01精确目标VERSION_HISTORY/raw版/ID，metadata/binary同源与late清理，不借父BPM、不放宽DAILY、不跟current。另授原ReferenceUsagePageDatabaseTest两失败的正式Guard依赖诊断/适配；若生产Query错误只交Root/G06 hunk。RF11-02 Bound归G03独占，不写。M09已结束，可私有明编，仍禁Maven/fulltype/build。

A33最新接管：Root独立FE34/34且review接收，三FE/相关tests冻结；PageDB整16参数runner已真实完成，等待Root下一正式票覆盖整类。无新授权不再改接管资产或重复测试。

## Milestones
1. 核对 int_qms、当前源码、相邻测试及正式读取合同，保存改前 SHA256。
2. 先写 BDD 和真实组件行为 RED；修复授权生产代码，完成 GREEN。
3. 运行独立 Node 回归，核对结构/差异，产出 Root 可复核报告及精确共享接入合同。

## Expected Verification
- Node 隔离执行实际 Vue SFC，记录调用身份和实际渲染的冻结字段。
- 历史与当前文件不一致、目标升级、错误租户/项目/主文件、请求失败、切版/切申请/卸载晚响应、真实固定正文身份。
- 选定申请上传/后补/整改证据及相邻 stage/history 单测。
- 专属 SFC 编译及定向 lint、git diff --check；不将隔离单测称为运行态验收。
- 任务文档 UTF-8 和必需结构检查；cleanup preview 按规则核对。

## BDD
- Given 历史 A/1 关联 X/1，当前 A/2 关联 Y/1，When 打开 A/1 详情，Then 仅读取 A/1 冻结记录，不读取 A/2 权限/当前关系且不显示 Y/1。
- Given X 已升为 X/2，When 查看历史 A/1，Then 显示冻结 X/1 名称、编号、版本并按 X/1 精确权限预览。
- Given 父页明确提供所选文件、申请类型、BPM 轮次及属性轮次，When 查看其关联，Then 按该申请读取完整冻结集合和上传/后补/整改证据，拒绝错文件/租户/项目/主文件/轮次。
- Given 切版、切申请或卸载前的异步请求尚未完成，When 旧结果返回，Then 不覆盖新上下文、不打开旧正文。
- Given 正式响应或权限缺失/失败，When 读取，Then 明确报错，不以当前关系或空集合冒充成功。
- Given A/1 和 A/2 同 Master 且各有正式持久引用，When 读取 A/1 引用来源/项目/数量，Then 只取 selectedControlledFileId=A/1 记录、按目的项目去重，并允许后补引用；不新造提交快照。
- Given History 切换正式申请或文件，When 请求未完成/失败/身份错配或卸载，Then selection-change 同步发 null；只有当前 generation 对应且文件/类型/BPM/属性轮次完整验真才发正式选择，晚回包不得重新发旧选择。
- Given 引用页/摘要返回其他版本或租户/主文件，When model/loader 接收，Then 明确拒绝而不默认数目/客户端过滤；合法同项目多文件夹保持总行数和去重项目数。
- Given 关联/阶段证据/引用固定目标X/1为SUPERSEDED或OBSOLETE，When三入口实际handler预览，Then正式previewSource或导航wire为VERSION_HISTORY+目标raw版与精确ID，metadata/binary使用相同canonical context且无父申请BPM；待生效/错版拒绝，关闭/切换/卸载后旧响应不能挂载旧source。
- Given PageDB真实名称权限和正文权限独立，When按新正式Guard依赖构造fixture，Then名称通过不授正文，增加真实正文目录权才canPreview，去名称读取权/错误租户/坏分页仍拒绝；不得删除/减弱旧断言。
- Given 同所选ID的正式引用行master错配或目的项目/文件夹缺失，When 调用getUsage摘要，Then 沿page相同正式identity guard准确失败，禁止COUNT过滤坏行后伪成功；Given 只有其他版本的坏行，When 读本版摘要，Then 计数正常且不读目的项目名称/授予正文。

## 设计约束检查
- ID 保持精确 Long 十进制字符串，禁止数字化丢精度。
- 正式所选身份优先于 currentActiveControlledFileId/latestControlledFileId；不得随目标升级。
- 名称/编号/版本/来源/整改取冻结事实；权限取精确目标当前只读权限投影。
- 申请选择必须由父页正式身份转交，不能在组件猜最新申请或按顺序选择。
- 不改组织、权限、版本写入、签名、事务、重放合同。
- 不写共享生产或无关并行改动；工程完成需 Root 接入和另行 Git 授权。

## Current Status
blocked — G11软件域已获Root接收：A33独立FE34PASS，M05精确引用/事务29正式PASS，M11 PageDB16+authority13正式PASS/0skip且3066路径零漂移；源码/tests继续冻结不重复套件。仅项目最终freeze/T04结果/Git收尾及历史cleanup审批阻塞仍由Root处理，Git未授权不标工程completed。

## Cleanup Keep
- doc/tasks/20261014-dcc-gap-selected-version-relations/task.md
- doc/tasks/20261014-dcc-gap-selected-version-relations/execution-log.md
- doc/tasks/20261014-dcc-gap-selected-version-relations/verification-report.md
- doc/tasks/20261014-dcc-gap-selected-version-relations/changed-files.md
- doc/tasks/20261014-dcc-gap-selected-version-relations/integration-notes.md
- doc/tasks/20261014-dcc-gap-selected-version-relations/review-ready.md
- doc/tasks/20261014-dcc-gap-selected-version-relations/reference-schema-evidence.md
- doc/tasks/20261014-dcc-gap-selected-version-relations/run-private-reference-tests.py
- doc/tasks/20261014-dcc-gap-selected-version-relations/maven-validation-request.md
- doc/tasks/20261014-dcc-gap-selected-version-relations/review-followup.md
- doc/tasks/20261014-dcc-gap-selected-version-relations/run-private-page-review.py
- doc/tasks/20261014-dcc-gap-selected-version-relations/PageReviewJUnit.java
- doc/tasks/20261014-dcc-gap-selected-version-relations/a33-checkpoint.json
