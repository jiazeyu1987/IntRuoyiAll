# 完整审批发布、引用读取与作废撤销：真实UI验收

最终结论：PASS。任务自有PUBLIC文件4584通过真实审批、五次有效签名、盖章发布并生效为A/1；仅有目标项目EDIT的普通Editor从准确固定引用读取真实PDF和本人水印，未获得源文件编辑、下载或打印入口。保留引用的独立作废申请40经四次有效签名后完成，原成功预览URL及刷新均拒绝正文，目标列表保留已作废引用且浏览禁用。

所有创建、授权、上传、审批、签名、引用与作废均由协作Agent使用真实Playwright页面完成。本文记录本Agent独占Editor与任务Signer的操作；未登录admin、Reviewer或文控账号。自然页面请求响应仅用于只读核验，没有直接调用业务API或写数据库。

## 实际对象与冻结轮次

| 对象 | 准确身份 | 正式结果 |
| --- | --- | --- |
| PUBLIC初始上传 | 2054545668044084584 / WORD1106-MATRIX-REVIEWER-V3 / A/1 | 公共矩阵V4；BPM 05a6b3cc-dab6-11f1-9ac1-b082e25ec548；ACTIVE后独立作废 |
| 目标固定引用 | WORD1106-REF-TARGET / DCC241016C22F1 | 来源SOURCE/F1、准确4584 A/1；作废后仍保留 |
| 普通Editor | word1106Editor | 仅目标项目EDIT和受控浏览/预览；无源项目或全局编辑授权 |
| 任务Signer | 910539 / word1107Signer / 部门910384 | 本人签名图片V1已启用；真实指派本人并另行独立会签 |
| PUBLIC独立作废 | 申请40 / BPM 4fdeed99-dabd-11f1-9ac1-b082e25ec548 | 主管→负责人指派→实际会签→批准；批准后OBSOLETE，无文控节点 |
| CUSTOM独立升版 | 新源4589 A/1→工作4590→正式4591 A/2，同master2054545668044079398 | BPM808424ff-dab7-11f1-9ac1-b082e25ec548；独立申请配置，新版本ACTIVE，旧4589自动OBSOLETE |

完整PUBLIC上传签名1701–1705、CUSTOM签名1706–1710及发布状态由admin/Requester真实UI重新读取，见协作报告`output/word1106-e2e/issue3-fullflow/issue3-fullflow-result.md`和`doc/tasks/20261106-word3-fixes/issue2-fullflow-result.md`。编号文字含历史“-V3”，实际冻结与执行的是V4，不能据文件名推断矩阵版本。

## 本Agent真实签名

| 轮次/节点 | 签名ID | 服务器时间2026-11-07 | 结果 |
| --- | --- | --- | --- |
| PUBLIC负责人指派本人 | 1702 | 22:26:10 | VALID |
| PUBLIC独立实际会签 | 1703 | 22:28:36 | VALID，摘要4f9f44c113c1 |
| CUSTOM负责人指派本人 | 1707 | 22:44:32 | VALID |
| CUSTOM独立实际会签 | 1708 | 22:45:11 | VALID，摘要d7444bdcfb22 |
| 作废40负责人指派本人 | 1712 | 23:21:35 | VALID |
| 作废40独立实际会签 | 1713 | 23:23:37 | VALID，摘要2b8159c0bb88 |

Signer首次改密、自助上传本人任务PNG并自动启用、刷新读回版本1/hash b8f7aeb7b3fa均由UI完成。每次指派与实际会签是独立密码签名动作。作废从本人审批中心真实待办进入，URL带准确4fde作废轮次与task047eb4cb-dabe-11f1-9ac1-b082e25ec548，没有混用已完成上传05a6轮次。作废会签响应nextStatus=ACTIVE只表示最终批准前文件仍生效；最终批准由admin签名1714 VALID（23:26:40），之后才开始权限撤销验收。

## Editor权限正反向结果与证据

证据根目录：`output/word1106-e2e/issue1-fullflow/`。

| 验收动作 | 实际断言 | 证据 |
| --- | --- | --- |
| 正式发布后从TARGET/F1浏览引用 | 准确4584/A1；PDF搜索命中“WORD1106 REF SOURCE Task-owned controlled document for U”；本人word1106Editor水印 | 04-approved-active-reference-row、05-approved-active-reference-body、editor-approved-active-reference-trace.zip |
| 从查看器点击真实“查看详情” | 只读详情，准确ACTIVE/A1及五签名1701–1705；无下载正文、受控打印、修改基础信息、更多操作、编辑、检出、升版按钮 | 06-active-reference-readonly-details-no-edit-download、editor-active-readonly-details-trace.zip |
| 引用可读时查看同源未引用4578 | 明确无权、没有正文工具 | 07-active-reference-does-not-grant-other-source-file |
| 新CUSTOM4591发布前的真实审批预览URL | Editor明确无权 | 12-custom-working-before-publication-denied |
| 新CUSTOM历史源4589真实UI返回地址 | 未引用历史文件仍明确无权，归档权限未扩展 | 13-new-custom-unreferenced-history-source-denied |
| 作废最终生效后原成功URL | 明确无权，reload仍拒绝；本人已重新登录且账号header存在，排除登录失效 | 14-editor-authenticated-before-obsolete-check、15-obsolete-original-active-preview-url-denied、16-obsolete-original-preview-reload-denied |
| 作废后刷新TARGET/F1 | 准确A1引用仍在，显示“源文件已作废”“仅文控可查询归档及下载”；浏览disabled=true | 17-obsolete-reference-row-retained-disabled、editor-obsolete-revocation-trace.zip |

机器断言汇总：`final-permission-result.json`；原成功URL精确保存在`public4584-active-reference-facts.json`，作废后使用同一字符串，不替换上下文或取消引用。

Signer配置、三个轮次的指派与独立会签证据在`signer/`、`signer/custom4591/`；自然UI响应与完整trace保留。页面request响应仅从实际Playwright trace/响应事件读取，未承担业务动作。

## 沿实际业务链核对

- 详情审批按钮/签名弹框：`IntRuoyiFronted/src/views/dcc/controlled-file/detail/index.vue:7209`、`:7306`，真实密码和签名意见提交；页面处理失败会保留明确错误，不模拟成功。
- 接口入口：`DccControlledFileController.java:871`，`approve-task`使用登录身份和正式权限调用workflow。
- 任务与签名：`DccControlledFileWorkflowServiceImpl.java:2350`先验证实际任务/本轮/阶段；MATRIX_APPROVAL明确选择编制人，文控阶段要求生效安排与产物readiness；`:2376`创建真实签名后再推进BPM。
- 签名配置：`DccSignatureVerificationServiceImpl.java:97`要求签名授权启用、有效用户部门岗位快照、`requireActiveSnapshot`签名图片、真实密码；保存签名与统一证据并标记图片已引用。任一缺项须通过正式UI配置或报告阻塞。
- 正式发布：`DccControlledFileFinalizationServiceImpl.java:405`验证完成事件及本轮绑定合同；`:449`核对冻结阶段签名完整性和历史任务；`:1145`将正式发布artifact交给`DccControlledFileLifecycleService.completeControl`。后者`:237`记录CONTROLLED，按明确生效安排在`:349`激活，`:405`写ACTIVE并更新master执行定位；关闭较低WORKING小版本时转SUPERSEDED；新正式受控版生效时，较低ACTIVE/待生效受控版由`obsoleteLowerControlled`（`:417`、`:439`）转OBSOLETE。本轮4589 A/1实际已自动作废、后继4591 A/2，不能混称SUPERSEDED。READY_TO_PUBLISH是内部过渡，不等价于已发布完成。
- 引用入口：`ProjectBrowserPanel.vue`及`browser/project-browser.ts`以持久化引用身份请求`getProjectReferencePermissions`；`DccControlledFileQueryServiceImpl.java:1544`校验租户/目标项目/文件夹/准确file与master引用，`:1560`投影可预览而源编辑另行验证。
- 正文实际读取：`DccControlledFileDetailAuthorizationGuard.java:67`先认证和文件上下文；`:30`验证正式master身份。`DccProjectReferenceReadAccess.java:18`仅ACTIVE、已受控已生效且有正式发布artifact，再查准确引用和目标项目EDIT/OWNER。
- 作废执行：`DccControlledFileObsoleteServiceImpl.java:358`锁master/file并要求已批准的真实作废轮次，`:379`请求归档，`:382`写OBSOLETE、保留审计并清空相应master当前生效定位。此时引用helper不再放行；正文guard优先归档判定，`DccArchiveAccessService.java:25`要求有效本租户用户、doc_control角色和archive:query权限，Editor不满足。

代码节点用于解释实际结果；本报告PASS以以上真实页面动作、正式签名和最终读回为依据。

## 保留的失败历史与验收界限

旧4587/4588因历史受控导入缺正式current关系集合而提交失败，原证据保留；新建事务初始化修复后通过UI重新导入4589，不直接修库或修旧fixture冒充成功。既有导入数据的正式迁移资格/阻断策略见`controlled-import-relation-review.md`，本轮未执行历史迁移。

`signer/06-department-self-assignment-saved`是指派后reload瞬态空投影，不能作成功依据；最终采用已加载的06b及准确签名1702。上传本人签名图片会自动启用，额外点已禁用“启用图片”的脚本等待曾超时，最终以刷新后的已启用状态为准。直接没有bodyReadContext的工作详情跳转受控浏览，不算授权拒绝断言。早期取消引用4577的拒绝证据01–03保留，当前最终结论以上述公共4584整链为准。

历史SUPERSEDED/OBSOLETE继续使用原文控归档规则，未授予普通目标编辑归档正文资格；本轮未扩大到目标VIEW角色或源项目所有文件。
