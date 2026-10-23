# Verification Report

## Conclusion

登记说明仍为选填、非空限制 500 字，原已关闭问题未重开。本次新增生命周期边界修复仅在 upload/index.vue：等价属性快照不误报 dirty，项目切换清理失败同步恢复原 dirty；新增专属 10/10、受影响回归 3/3。以下保留原登记说明及历次修复证据，当前最终指纹和范围以末节“属性 dirty 生命周期追加修复”为准。

## Formal Basis

- `doc/tasks/20261014-dcc-gap-fix-coordination/coordination.md` 的 G12 明确写出“普通上传不因未填写文件夹登记说明而被拒绝；若登记说明保留则是可选且长度仍校验”。
- `doc/tasks/20261014-dcc-gap-optional-folder-reason/task.md` 的 BDD 和设计约束与上述口径一致，并区分普通 NEW 上传派生绑定与人工/严格绑定路径。
- DCC 产品验收文档要求项目和逻辑身份等正式业务边界，但未将 `projectFolderChangeReason` 列为普通上传必填字段；因此不能以通用审计原因规则推导出该字段必填。

## Source Findings

- `IntRuoyiFronted/src/views/dcc/controlled-file/upload/index.vue`：登记说明控件标为“选填”；冻结逻辑只保留 500 字长度校验，仍验证当前项目文件夹和项目属性。
- `IntRuoyiFronted/src/views/dcc/controlled-file/upload/submitter.ts`：空白说明返回 `undefined`，非空说明 trim 后传递，超过 500 字拒绝；项目文件夹身份和上传票据校验不变。
- `IntRuoyiFronted/src/api/dcc/controlledFile/workflow.ts`：`assertControlledFileSubmitRequest` 不再调用该字段的必填校验，仅对存在的值执行字符串类型和 500 字上限校验。
- `IntRuoyiBackend/config/gxp-audit-policy.yaml`：`dcc.project-file-placement.upload-bind` 使用 `OPTIONAL_TEXT`；既有人工/严格绑定策略仍保持必填原因约束。
- `DccPublicUploadPlacementService` 与 `DccProjectFilePlacementService`：普通上传派生绑定保留租户、操作者、项目、文件夹、存储和类别归属校验，并以真实空串表达未填写原因，不编造默认说明。

## Test Evidence

首轮曾在 `IntRuoyiFronted` 执行定向 Node 单测，结果 34/34 PASS，覆盖 G12 专属测试及相邻上传回归。这是历史执行证据。本轮用户要求只读复核，不重复 34 项或 Root 77 项套件，未运行新单测；Root 77 项仅为用户提供的既有通过信息，不表述为本线程独立执行。

## 2026-10-15 当前工作区只读复核

本轮分支 int_qms，HEAD f06833f06c868dc3a451eb63d01381a467ecfe92。复核对象包含当前未提交源码，不仅是 HEAD。Root 已确认普通上传说明可选，上一轮“额外必填”误判已更正；本轮未发现新增源码缺口，未修改生产或测试。

下列路径以前缀缩写表达：FE=`IntRuoyiFronted/src`，BE=`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc`。

| 当前源码锚点 | 只读发现 |
| --- | --- |
| FE/views/dcc/controlled-file/upload/index.vue:79、:1075、freezeUploadApplicationDraft:2599 | 控件提示选填；formRules 不要求登记说明；冻结仅在 :2609 拒绝 trim 后超过 500 字，仍校验文件夹和属性归属。 |
| 同页草稿 :2686、提交 :2820 | 草稿和上传都调用共用冻结逻辑，无独立登记说明必填分支。 |
| FE/views/dcc/controlled-file/upload/submitter.ts，placementReason:512、buildUploadPayload:586、builders :605/:609 | 空白/缺省返回 undefined，不制造原因；非空 trim 后上限 500。保存与提交共用载荷构造。 |
| 同文件 buildUploadConfirmationSummary:92、自定义分支 :142、附件行 :159；页面 confirmUploadApplication:2651、:2827/:2845/:2859 | 确认摘要未要求登记说明；确认框只渲染摘要，同一冻结 draft/附件随后交给提交器。自定义矩阵核对正式账号/来源和部门，多附件核对票据与文件名，均未恢复说明必填。 |
| FE/views/dcc/controlled-file/upload/approval-matrix.ts，requireApprovalMatrixSelection:125；submitter.ts:551/:576 | 自定义矩阵与附件身份分别校验，不把登记说明混入矩阵或附件必填合同。 |
| FE/api/dcc/controlledFile/workflow.ts，assertControlledFileSubmitRequest:1898/:1925，wrappers :2113/:2118 | 当前其他 owner 已修改此文件，但原因锚点仍仅验证存在值的类型和 trim 后长度，无必填。 |
| BE/controller/admin/file/vo/DccControlledFileSubmitReqVO.java:68 | 登记说明字段没有 NotBlank/NotNull 必填注解。 |
| BE/service/file/DccPublicUploadPlacementService.java#create:87/:107；BE/service/projectcode/folder/DccProjectFilePlacementService.java#bindDerived:29、私有 bind:43/:47/:85 | 派生上传缺省原因记真实空串，仅限制规范化后 500 字；必须有服务器派生上下文，继续校验项目、文件夹、租户、权限、文件/存储归属，写独立 upload-bind 审计。 |
| IntRuoyiBackend/config/gxp-audit-policy.yaml:473/:479，严格 bind :460/:466，目录 :434/:447/:486 | 上传派生绑定仍 OPTIONAL_TEXT；人工严格绑定和目录新增、修改、删除仍 REQUIRED_CATEGORY_AND_TEXT。 |
| BE/service/projectcode/folder/DccProjectFolderMaintenanceService.java#save:74 / #delete:36；BE/service/projectcode/attributes/DccProjectConfigurationAuditService.java#validateReason:12 | 人工目录维护仍调用原原因校验，拒绝 null/空白及超过 500 原因，未被上传可选规则放宽。 |

本轮取消重复套件是用户明确限定的范围；保留只读源码复核与任务文档结构验证。未执行 Maven/fulltypes/build/E2E/实库/服务/Git 写操作/remote/subagents。上述结论是静态复核，不宣称当前工作区新增测试或运行验收 PASS。原实现及历史 RED/GREEN 以 `doc/tasks/20261014-dcc-gap-optional-folder-reason/` 为主任务，本目录仅记录复核。

本轮文档结构验证已实际执行：PowerShell exit 0，三份报告、必需章节、范围/证据边界和 blocked 状态检查通过（REPORT_STRUCTURE_OK）；页面、submitter、wrapper、两后端 placement 服务及 policy 共六份生产输入复核前后 SHA256 一致（SOURCE_STABILITY_OK）。仅报告结构与静态输入稳定性验证通过，无新软件套件执行。

## Status Boundary

登记说明范围无新源码缺口；追加的类型切换确认缺口已在 Root 解除冻结和明确授权后最小修复，专属 GREEN 7/7、受影响回归 8/8。当前仅等待 Root 最终收尾与决定类型/构建验证；Git 提交/推送仍禁止，不标 completed。历史待修复/冻结描述保留为过程记录，以末节为最新结论。

## 最新追加复核：确认与切换边界

本次没有修改任何生产源码/测试宿主；125 后端、181 前端、完整类型及旧 34 项均未重跑。登记说明前端两文件 SHA256 与前次一致（页面 3AA769445F758975F07ED980BC74C284212767534930574449C5712F24EEF46F；submitter 197A992D3F6D09E5DBC74C10A9B5FBB86A52D2E912C9DF932FEE008A57FAFA36）。公共 wrapper 当前原因锚点在 :1926，仍选填，仅类型/trim 后 500 字校验；不据其整体变化推断登记说明回归。

- 已收到需求列明的确认字段在 submitter.ts#buildUploadConfirmationSummary:92-163 均有对应行，多附件 :159-161 逐个列名；本次自定义矩阵分支 :142-154 校验正式账号，未添加原因必填。登记说明仍经 placementReason:512 与页面 freeze:2609 处理。
- index.vue#submitForm:2845-2848 取消/关闭确认即 return，:2859 才调用正式提交。矩阵差异确认取消在 :2133 返回 false，:2818 退出；这些路径没有业务申请/审批任务创建调用，且不清空字段。实际任务落库未验证，不把静态控制流说成真实页面或后端验收。
- 项目切换取消 :1657-1663 返回旧项目，说明清空 :1673 仅在成功接受后执行；属性 dirty 确认由 ProjectApplicationAttributes.vue#selectProject:44 和 state.ts#applyProjectDefaults:81 实施。类型切换取消 :1811-1815 返回旧类型，不执行后续 reset；成功同项目切换保留项目属性/登记说明，成功跨项目才清除旧说明。

发现一项超出登记说明范围的真实控制流缺口：仅修改本次项目属性时，hasUploadApplicationInput:1610 未计入属性编辑，类型 handler :1806 跳过切换确认。专属隔离测试 AST 抽取实际生产函数执行，新增 BDD 在前，实际 RED 在后：

`node --test doc/tasks/20261015-dcc-upload-folder-reason-review/attribute-only-type-switch.red.cjs` -> exit 1，1 fail/0 pass/0 skip，预期 confirm，实际 cleanup/reset-name/reset-category/load-category。

测试没有 HTTP/实库访问，失败属于实际分支行为。现有 reset 保留 projectAttributes，因此只能确认缺少提示/取消机会，不能宣称属性数据丢失。触发步骤、源码依据与建议详见本目录 integration-note.md；Root 生产前端冻结中，本线程未改实现，GREEN/回归未执行，新 RED 未被旧通过结果覆盖。用户缺失第 6/8 项未推断。

最新 Root 已授权本线程实施 upload/index.vue 的 dirty/切换确认最小 hunk 和专属 tests；冻结尚未解除。为明确实际行为，RED 加入不同 defaultSource/actual 和运行后诊断，仅复跑此 1 项仍 exit 1：typeAfterHandler=8（原类型 7），attributesPreserved=true，确认端口未被调用而是执行 cleanup/reset/load。待明确解除冻结后实施/GREEN，不修改属性组件/submitter/矩阵。

## 最终软件结果：冻结解除后已修复

Root 已解除冻结。唯一生产改动为 `IntRuoyiFronted/src/views/dcc/controlled-file/upload/index.vue` 新增 3 行（:1603 编辑 ref、:1609 捕获实际值/默认值差异、:1620 输入判断）：仅修改属性也进入原切换确认，cancel/close 在原 handler 中恢复旧类型并返回；未填完整的属性也能取消。初始默认或恢复默认不会额外提示，跨项目旧事件仍由原身份 guard 拒绝。属性组件、submitter、公共 workflow/矩阵和其他 owner 文件未修改。

先扩展 BDD/专属测试，真实 RED 7 项为 4 fail/3 pass；最小修复后同一命令 `node --test doc/tasks/20261015-dcc-upload-folder-reason-review/attribute-only-type-switch.red.cjs` -> exit 0，7/7，0 fail/0 skip。脚本文件名保留历史 red 标识，实际已验证 GREEN；文件长期保留但被 .gitignore 忽略，Root 正式收录时须显式纳入。

受影响回归在前端目录以 `node --test --test-name-pattern='cancelled type change|older type confirmation|manual project context|late cancelled project selection|confirmation cancel preserves|cancel and close both|only custom account edits|confirmed type and source switch' tests/unit/dcc-upload-w03-safety.test.cjs tests/unit/dcc-upload-application-page.test.cjs` 执行，exit 0，实际 8/8，0 fail/0 skip。旧 34/77/125/181 套件与完整类型/构建未重复执行。

`git diff --check` 页面通过，差异为 3 行新增。最终页面 SHA256=01A4EBC05F1894383959070A68F751C946833120AD9333A1280B2A795426914A；submitter 未变。登记说明仍选填/trim 后 500 字，取消不提交审批的控制流未修改。Root 的 2635 输入/3239 产物构建通过来自本修复之前，仅按其原证据有效；修复后的完整类型/构建由 Root 决定，不在本线程宣称已跑。

收尾已先标 ready_for_closeout，报告结构与最终来源哈希检查实际通过；仅保留指定报告与专属脚本，无其它临时产物。按当轮禁止 Git 提交/推送规则，最终状态 blocked（工程收尾），不标 completed。cleanup preview/apply 未执行，不记 PASS；现有软件修复无未解决失败，交 Root 最终集成复核。

## 最新结果：属性 dirty 生命周期追加修复

原已关闭问题不重开；本次发现并修复两处新的边界：等价属性序列化差异误报 dirty，以及切项目后清理失败恢复原实际属性却未恢复 dirty。仅修改上传页状态/确认范围：增加 uploadProjectAttributesKey，固定业务字段并按市场集合/空条件文本语义比较，完全保留原始实际输入；旧项目恢复路径复用 captureProjectAttributes 同步属性和 dirty。原值没有被宣称丢失。

新 BDD 在前，真实 RED 为新增 9 项中的 5 fail/4 pass；最小修复后 GREEN 9/9，补充“清空条件必填文字仍 dirty”负向后最终 GREEN 10/10。命令：`node --test doc/tasks/20261015-dcc-upload-folder-reason-review/attribute-dirty-lifecycle.test.cjs`，最终 exit 0、0 fail/0 skip。对象键/市场顺序和空字段不会误报，原对象不被排序/改写；真实枚举变化、空必选值、清空有条件必填文本及实质性隐藏文本仍被保护。

受影响回归：在前端目录执行 `node --test --test-name-pattern='A32 accepted project switch clears|cancelled project switch retains|late cancelled project selection' tests/unit/dcc-upload-application-page.test.cjs` -> exit 0，实际 3/3、0 fail/0 skip。原仅属性切类型 7 项未重复，其自有 loader 只补新 helper 提取；Root 上轮 200/200/types/build 保留为历史通过证据，不算本次执行，也不覆盖本次新差异。

生命周期边界与证据：

- 恢复默认通过现有 `@restore-defaults="captureProjectAttributes"` 重新计算语义差异；新测试证明等价默认值清 dirty。
- 项目切换取消在 select/cleanup 前返回，原身份/属性/dirty 保留；成功加载 Q 默认后 dirty=false；清理失败回滚在恢复 P 身份后调用 capture，P 的手改保护复原，未执行成功 reset/load。三条路径由新增实际 handler 测试覆盖。
- 保存草稿成功的上传页只导航到已保存申请，`DetailApplicationPanel.vue:413/:426` 读取该草稿的 saved.defaultSource 和 actual，与上传 dirty ref 独立；无本次新缺口，未修改其他 owner 或调用真实库验证。
- 成功后再进入上传，`resetCompletedUploadDraft:1455` 用初始表单清空 projectAttributes，输入判断已有属性存在 guard，旧 dirty ref 不会构成下一份输入；下一次默认事件重新计算。新专属行为验证空属性隔离，未重复旧 reentry 套件。

页面最终 SHA256=`D06FA211D32ECA71BBBBB808BBA2E91070E28C71ED952E9A54628049EED3976A`；新测试 SHA256=`5ED1392012BBD71690BBD2523381EAC39F1BA95C3C8E84C2FC7FC03D396AB7B4`；旧自有 loader SHA256=`9AE9F29AFF26153647C356B832DE402CAD29A4A25718B469704467A5A3B1D68A`。submitter 未变，说明仍选填/500 字。git diff --check 通过；本次没有 Maven/sharedtarget/fulltypes/build/E2E/实库/迁移/服务/remote/Git 提交推送/subagents，必要的集成验证交 Root。

本轮先标 ready_for_closeout，结构/保留路径/最终来源哈希核对实际 exit 0，脚本语法检查通过但没有执行旧套件。无待修软件失败；因禁止 Git 提交/推送最终状态 blocked，不标 completed。cleanup preview/apply 未执行，不记 PASS；两份指定 .cjs 持续保留，正式收录由 Root 在获 Git 授权后处理。
