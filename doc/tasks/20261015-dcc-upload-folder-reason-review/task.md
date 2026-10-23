# DCC 上传文件夹登记说明需求核对

## Task Goal

核对 DCC 上传说明与属性切换保护。登记说明选填及原“仅属性手改切类型确认”已关闭，不重开；最新授权仅针对属性 dirty 生命周期、等价快照误报及清理失败回滚，以 upload/index.vue 最小状态/确认 hunk 和自有测试修复。

## Milestones

1. 读取仓库规则、DCC 产品/后端规则及既有 G12 任务记录。
2. 核对 `a3b8234ec`、当前 HEAD、工作区并行改动和相关前后端链路。
3. 首轮及 Root 旧套件不重复；本次生命周期边界先取得真实 RED，再最小修复，最终新增专属 GREEN 10/10、受影响项目切换回归 3/3；先前 7/8 仅保留为历史证据。
4. 形成验证报告；由于当轮禁止 Git 提交/推送，任务状态保留为 `blocked`（仅工程收尾门禁阻塞）。

## Expected Verification

- 需求依据明确说明普通上传不因登记说明为空被拒绝，登记说明如保留则为可选且仍有长度上限。
- 前端表单、草稿冻结、确认摘要/载荷和 API wrapper 不把登记说明当必填，不生成默认说明。
- 后端上传派生绑定使用可选原因策略；项目、文件夹、租户、操作者、存储和审计事实校验保持有效。
- 首轮及已关闭问题通过仅作为历史证据；本次验证为生命周期专属 10 项、项目切换受影响回归 3 项和报告结构检查，不把 Root 200/200/types/build 或原 7 项写成本轮新执行。
- 本轮明确取消重复 34 项/Root 77 项执行；不执行 Maven、fulltypes、build、E2E、实库、服务、Git 写操作、remote 或 subagents。

## BDD

- BDD: 普通上传缺省登记说明 -> Given 项目、有效项目文件夹和其它规定必填项完整，When 登记说明为空、缺省或仅空白，Then 保存/提交载荷构建成功且不注入虚假说明。
- BDD: 登记说明长度边界 -> Given 有效项目文件夹，When 说明规范化后长度为 500，Then 保留真实说明；When超过 500，Then 在传输前明确拒绝。
- BDD: 归属与其它必填项不放宽 -> Given 登记说明可选，When 项目文件夹缺失、跨项目、失效或项目属性/上传票据无效，Then 仍明确拒绝。

## 设计约束检查

- 仅核对 G12 相关源码和文档，不回滚或覆盖工作区其它并行改动。
- 保留人工文件夹维护/严格绑定路径的必填审计原因规则；仅普通 NEW 上传派生绑定使用可选原因策略。
- 不执行 E2E、服务操作、数据库写入、Git 提交或推送。
- 初轮只读复核未修改生产；最新追加已获 Root 明确授权，仅修改 upload/index.vue 的 3 行属性编辑/输入判断及自有专属测试。本节以最新授权范围为准，不写其他 owner 文件。

## 2026-10-15 本轮只读复核

- 用户已明确 Root 确认普通上传说明可选，上一轮“额外必填”的误判已更正；不重新推翻已正确的生产实现。
- 当前分支 int_qms，HEAD f06833f06c868dc3a451eb63d01381a467ecfe92。复核包含当前未提交工作区，特别是其他 owner 已改动的 workflow.ts，只读不覆盖。
- 当前锚点：页面登记说明控件 / formRules / freezeUploadApplicationDraft / saveUploadDraft / submitForm / confirmUploadApplication；submitter 的 placementReason / buildUploadPayload / buildUploadConfirmationSummary；wrapper 的 assertControlledFileSubmitRequest；后端 create / bindDerived / 私有 bind 及独立 upload-bind 策略。
- 多附件与 APPLICATION_CUSTOM 自定义矩阵仍通过上述共用冻结、载荷、确认链路；未重新引入登记说明必填，手动目录维护原因仍严格必填。详细行号与方法见 verification-report.md。
- 本次无新增源码缺口；只读结论不冒充新单测、后端运行或 E2E 验收。原实现任务仍为 doc/tasks/20261014-dcc-gap-optional-folder-reason/，本目录是复核记录。

## Current Status

blocked

最新生命周期修复已完成：新增专属 RED 9 项中 5 fail/4 pass，GREEN 9/9；补充条件必填负向后最终 GREEN 10/10，受影响项目切换回归 3/3。原 7 项及 Root 200/200/types/build 未重跑。待 Root 最终集成复核；Git 提交/推送禁止，不标 completed。历史冻结/3 行/旧哈希结论以最新生命周期章节为准。

本轮已先设 ready_for_closeout，完成报告结构、保留路径、脚本语法及最终指纹检查后，按禁止 Git 提交/推送收尾规则记 blocked；无未修软件失败。cleanup preview/apply 未执行，不记通过。

## 本次追加范围：确认完整性与切换原值保护

- 仍沿用本目录；只读生产源码，不改公共 workflow/矩阵、其他 owner 文件或测试宿主。Root 构建冻结期间也不修改生产前端。
- 125 后端 / 181 前端 / 完整类型及既有 34 项均为历史证据，本次不重跑、不声称本次通过。Maven/shared target/fulltypes/完整构建全部由 Root 唯一执行；不执行 E2E、实库写入/迁移、服务、remote、Git 提交推送或 subagents。
- BDD: 只有属性编辑时切换类型 -> Given 已选择项目及 APPLICATION_CUSTOM 类型，仅手动修改本次项目属性，文件夹、名称、编号、生效方式、部门、关联、附件和自定义账号均未填写，When 改选另一启用类型并取消切换确认，Then 必须弹确认、恢复原类型、保留属性且不清理临时上传/重置联动。
- 静态发现 hasUploadApplicationInput 未计入 projectAttributes，类型 handler 仅用该函数决定弹框；项目 handler 另有属性组件 dirty 检查。此为跨上传状态 owner 边界，只新增专属复核 RED 和 integration note，不修改生产。
- RED: node --test doc/tasks/20261015-dcc-upload-folder-reason-review/attribute-only-type-switch.red.cjs -> exit 1，1 fail/0 pass/0 skip；预期 confirm，实际 cleanup/reset-name/reset-category/load-category，确认缺少取消机会。
- GREEN/REGRESSION：未执行，等待 Root 决定所属修复及前端解冻；不以旧 125/181/完整类型或 34 项通过代替此新 RED。
- Root 最新明确授予本线程 upload/index.vue 中 dirty/切换确认最小 hunk 及专属 tests；不授予属性组件、submitter、公共矩阵或其他 owner 文件。生产前端构建冻结尚未解除，先保留当前真实 RED，收到明确解除后再实施/GREEN。
- Root 已明确解除构建冻结。计划仅在 upload/index.vue 利用现有 captureProjectAttributes 收到的 actual/defaultSource 记录手改并纳入已有输入判断，不调用会校验完整性的 getSnapshot 作为切换前提。
- BDD: Given 仅属性手改（包含未填完整的属性），When 切换另一类型后 cancel/close，Then 保留原类型、属性、空登记说明，不清理/重置；When 确认继续，Then 按现有流程接受新类型且保留属性。
- BDD: Given 只有默认属性或已恢复默认值，When 无其它输入切换类型，Then 不误报属性手改；Given 当前项目 P，When 收到其他项目旧属性事件，Then 忽略而不制造 dirty 提示。

## 最新实现与验证（解除冻结后）

- 生产改动仅 IntRuoyiFronted/src/views/dcc/controlled-file/upload/index.vue：新增 uploadProjectAttributesEdited ref；在已有 captureProjectAttributes 身份 guard 后比较 actual/defaultSource；将手改且当前属性仍存在纳入 hasUploadApplicationInput。全部 3 行新增，不改类型 handler、属性组件、submitter、公共 workflow/矩阵或其他 owner 文件。
- 专属场景先 RED 7 项中 4 fail/3 pass（均业务断言），随后同一脚本 GREEN 7/7，exit 0；受影响既有测试按 name pattern 仅执行 8 项，8/8 exit 0。未运行旧 34 项、125/181 或全类型/构建。
- GREEN 覆盖 cancel/close 保留类型和属性、未填完整属性可取消、默认值无误报、恢复默认、确认继续、跨项目旧事件忽略。登记说明为空保持选填，原 500 字处理没有改动。
- 页面交回 SHA256=01A4EBC05F1894383959070A68F751C946833120AD9333A1280B2A795426914A；submitter 未变；git diff --check 页面 exit 0。历史冻结/待 GREEN 条目仅为过程记录，以本节为最新结论。

## Cleanup Keep

- doc/tasks/20261015-dcc-upload-folder-reason-review/integration-note.md
- doc/tasks/20261015-dcc-upload-folder-reason-review/attribute-only-type-switch.red.cjs
- doc/tasks/20261015-dcc-upload-folder-reason-review/attribute-dirty-lifecycle.test.cjs

## 本轮追加：属性 dirty 生命周期与语义比较

- 当前 int_qms/f06833f06；上一轮 200/200、types/build 由 Root 取得，仍为历史证据，不重开 C01-C06、不重跑已修切类型 7 项。范围仅 upload/index.vue 最小属性状态/确认 hunk 及本目录专属测试；属性组件、submitter、矩阵和其他 owner 宿主只读。
- BDD: 等价快照不误报 -> Given 本次实际属性与默认业务值相同，When 字段键顺序、目标市场勾选顺序或缺省条件字段的空字符串/空白表示不同，Then dirty=false，输入对象不被比较逻辑修改；有效业务值变化或清空必填值仍为 dirty。
- BDD: 清理失败回滚 dirty -> Given 项目 P 的属性已有真实手改且有临时上传，When 确认切项目 Q、Q 默认属性已加载，但临时清理返回失败，Then 恢复 P 身份、原实际属性及对应 dirty，保留原文件夹/说明，不执行成功重置/任务创建。
- BDD: 取消与成功 -> Given P 属性手改，When cancel/close 项目切换，Then 原身份/属性/dirty 保留；When 切换成功并加载 Q 默认值，Then dirty=false，旧关联/说明按原成功路径清理。
- BDD: 下一份隔离 -> Given 上份提交/保存成功后重新进入上传，When 初始化 draft 不再携带旧属性，Then 旧 dirty 标记不构成新申请输入；下一项目默认事件会重新计算 dirty。草稿详情重开读取已保存快照，不复用上传页 dirty。
- 本轮预期验证：先运行新增生命周期专属测试 RED，再最小修复并 GREEN；仅按影响选择相关既有取消/切换子集。禁 Maven/sharedtarget/fulltypes/build/E2E/实库/迁移/服务/remote/Git 提交推送/subagents。

## 本轮最新实现与验证：生命周期

- 仅 index.vue：增加 uploadProjectAttributesKey，固定业务字段顺序、复制后排序市场集合、以空字符串比较缺省/空白条件文本；仍保留原始 actual，不用严格提交校验阻止未填完整编辑；清理失败恢复旧项目后复用 captureProjectAttributes 同步原属性和 dirty。
- 最终专属 10/10、受影响回归 3/3，均真实 exit 0；未重跑原仅属性切类型 7 项。其自有 loader 仅增加新 helper 提取，供 Root 后续复核依赖，不宣称该旧套件本次通过。
- 恢复默认/成功切项目/取消/清理失败/下一份无属性隔离见新专属测试；保存草稿重开走 DetailApplicationPanel 的 saved.defaultSource/actual，不读取上传页 dirty，未发现该只读边界新缺陷，不写其他 owner。
- 页面最新 SHA256=D06FA211D32ECA71BBBBB808BBA2E91070E28C71ED952E9A54628049EED3976A；新测试 SHA256=5ED1392012BBD71690BBD2523381EAC39F1BA95C3C8E84C2FC7FC03D396AB7B4。完整类型/构建留 Root 唯一执行，前轮 200/200 等为历史证据。
