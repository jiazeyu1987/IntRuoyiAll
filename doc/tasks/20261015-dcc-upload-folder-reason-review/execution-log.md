# Execution Log

## RED

- 复核既有任务 `doc/tasks/20261014-dcc-gap-optional-folder-reason/`：记录了原始必填行为以及 G12 需求、前后端 RED 证据。
- 需求/协调依据：`doc/tasks/20261014-dcc-gap-fix-coordination/coordination.md` 的 G12 条目明确普通上传不因未填写文件夹登记说明被拒绝；说明如保留则为可选且仍校验长度。
- 当前工作区代码检查确认 HEAD 已包含可选实现：页面提示“选填”，`submitter.ts` 将空白说明规范化为 `undefined`，`workflow.ts` 仅对非空值做类型和 500 字校验，后端策略包含 `dcc.project-file-placement.upload-bind` / `OPTIONAL_TEXT`。

## 首轮 GREEN（历史执行，本轮未重跑）

- `git branch --show-current` -> `int_qms`。
- `git rev-parse --verify a3b8234ec` -> `a3b8234ecfba6bbafff2e8aa4dc3d1e60eccb1d2`；当前 HEAD 为 `f06833f06c868dc3a451eb63d01381a467ecfe92`，且 `a3b8234ec` 是 HEAD 祖先。
- `node --test tests/unit/dcc-upload-g12-optional-folder-reason.test.cjs tests/unit/dcc-upload-g12-api-optional-folder-reason.test.cjs tests/unit/dcc-upload-project-folder-context.test.cjs tests/unit/dcc-upload-single-project-folder.test.cjs tests/unit/dcc-upload-actual-attributes.test.cjs` -> PASS，34/34，0 fail/0 skip。
- 覆盖：缺省/空/空白说明、有效说明与 500 字边界、501 字拒绝、工作稿保存、正式提交、页面冻结、跨项目/缺失/失效/加载中文件夹拒绝、项目属性/上传票据校验、正常 NEW 存储目录身份校验。
- `git diff --check`（G12 相关源码）-> PASS。

## 首轮 Regression（历史执行，本轮未重跑）

- 定向相邻上传、项目文件夹上下文、单项目文件夹和实际属性测试均纳入上述 34 项回归并通过。
- 工作区仍有既存并行改动：`IntRuoyiBackend/LOG_FILE_IS_UNDEFINED`、`LOG_FILE_IS_UNDEFINED`、`DccControlledFileQueryServiceImpl.java`、`IntRuoyiFronted/src/views/dcc/controlled-file/view/index.vue`；未修改、未回滚。
- 未执行 E2E、服务重启、数据库写入、Git 提交或推送，符合当轮限制。

## 2026-10-15 继续复核：范围与结果

- 依用户本轮要求沿用本目录，仅只读复核当前源码并更新三份报告；34 项与 Root 77 项通过套件均不重复执行。Maven/fulltypes/build/E2E/实库/服务/Git 写操作/remote/subagents 全未执行。
- 已重读 AGENTS.md、docs/task-closeout-rules.md，复核 frontend-development.md 上传/草稿/确认约束和 backend-development.md DCC/统一审计约束。
- 当前 int_qms / f06833f06c868dc3a451eb63d01381a467ecfe92；工作区已有多处其他 owner 改动，包括 workflow.ts。未写任何生产源码、tests 或其他任务文件。
- 前端 index.vue:79 控件仍为选填，formRules:1075 未登记此字段必填；freezeUploadApplicationDraft:2599 在 2609 仅检查 trim 后长度 >500。草稿 :2686 与上传 :2820 都调用该函数。
- submitter.ts:512 placementReason 对缺省/空白返回 undefined，非空 trim 并限制 500；buildUploadPayload:586 通过该函数构建说明，两种 payload builder :605/:609 共用。附件票据 :576 和矩阵选择 :551 为独立字段，不引入原因校验。
- 确认摘要 buildUploadConfirmationSummary:92 的公共/自定义矩阵分支及附件行未要求登记说明；index.vue:2651 的 confirmUploadApplication 只渲染摘要，:2827 构造摘要，:2845 确认后将同一冻结 draft 和附件 :2859 传入提交。
- workflow.ts:1925 仍只对存在值校验类型及 trim 后 500 字上限；createWorkingControlledFile:2113 / submitControlledFile:2118 共享该校验，未恢复必填。
- 后端 VO DccControlledFileSubmitReqVO:68 无该字段的 NotBlank/NotNull；DccPublicUploadPlacementService#create:87 以空串表达缺省并检查 500，:107 调 bindDerived；DccProjectFilePlacementService:43 区分严格 bind 与派生绑定，:47 派生空串/长度检查，:85 选择独立 upload-bind。
- gxp-audit-policy.yaml:473 的 upload-bind 仍 OPTIONAL_TEXT；:460 严格 bind 与 :434/:447/:486 手动目录新增/修改/删除仍 REQUIRED_CATEGORY_AND_TEXT。DccProjectFolderMaintenanceService#save:74 / #delete:36 调用 validateReason；DccProjectConfigurationAuditService#validateReason:12 拒绝 null、空白及超过 500 原因。
- 结论：本次无新增源码缺口；未发现多附件/自定义矩阵恢复必填。仅静态复核，无新增 RED/GREEN；原实现主任务为 doc/tasks/20261014-dcc-gap-optional-folder-reason/，本次不复制其测试为新执行结果。
- 文档结构验证（PowerShell 只读检查）exit 0：三份报告非空，任务必需章节、当前范围/证据边界和 blocked 状态齐全，输出 REPORT_STRUCTURE_OK。页面、submitter、wrapper、两后端 placement 服务、policy 六份输入复核前后 SHA256 相同，输出 SOURCE_STABILITY_OK；这是静态证据稳定性检查，不是套件执行 PASS。

## 最新追加复核：确认完整性、取消与切换原值保护

- 当前 int_qms / f06833f06；已读 AGENTS.md、task-closeout-rules.md、frontend-development.md 确认/草稿/切换规则与原协调需求。Root 125 后端/181 前端/完整类型及 29 项指纹无漂移为上轮或 Root 提供的证据，本线程未执行这些命令。旧 34 项未重跑。
- 确认完整性：submitter.ts#buildUploadConfirmationSummary:92-163 对照已收到需求，包含名称、编号、初始版本、项目/文件夹/类型、编制人/主管、生效方式、三个属性、图纸及所有普通附件名、固定关联版、会签/下发部门、批准人/规则、培训及全员下发说明。没有新增登记说明必填；第 6/8 项缺失内容不猜测。
- 取消业务确认：index.vue#submitForm:2845-2848 在 cancel/close 返回，正式 uploadSubmitterService.submit 位于 :2859，取消分支没有业务提交或任务创建调用；finally:2884 释放 loading，不清空原填写。矩阵差异确认 :2133 同样取消返回 false，:2818 在最终确认前退出。确认后 :2850-2858 复核上下文与票据；此为静态控制流结论，非 E2E/新软件 PASS。
- 原值保护：项目切换 :1627 确认后才清理，:1657-1663 取消恢复旧项目，登记说明清空 :1673 位于成功接受之后；属性组件 state.ts#applyProjectDefaults:81 对 dirty 编辑先确认，取消不替换属性。类型切换 :1806-1815 取消恢复旧类型且清理之前返回；类型 reset 不清除项目属性或登记说明，成功跨项目才清空旧说明避免串项目。
- 新跨边界缺口：只改项目属性、其它输入/附件/部门/自定义账号为空，hasUploadApplicationInput:1610 不计属性；handleFileTypeTaxonomyChange:1806 因 false 跳过提示。已先在 task.md 写 Given/When/Then，随后新增自有 attribute-only-type-switch.red.cjs，AST 抽取实际两个生产函数执行。
- RED（本次新执行）：node --test doc/tasks/20261015-dcc-upload-folder-reason-review/attribute-only-type-switch.red.cjs -> exit 1，1 test/1 fail/0 skip；断言期望仅 confirm，实际 cleanup、reset-name、reset-category、load-category。该失败发生在业务断言，非依赖或夹具加载错误。无 HTTP/数据库访问，无生产修改。
- integration-note.md 已列触发步骤、源码锚点、仅缺确认/取消机会而非属性丢失的边界和 Root 建议。此项不在登记说明范围且 Root 已冻结生产前端，因此不实施 GREEN，不改公共 workflow/自定义矩阵/其他 owner 代码或测试宿主。
- 结论：登记说明仍选填/trim 后 500 字，无新增本域回归；发现一项上传切换状态跨 owner 实际 RED，须交 Root。此前“无新缺口”仅对应上一次较窄复核，不能覆盖本次新增边界。
- 实际文档结构与来源检查 exit 0：REPORT_STRUCTURE_OK，当前四份报告/交接说明、blocked 与 RED 保留路径有效；SOURCE_STABILITY_OK，页面和 submitter 两生产输入与检查前 SHA256 一致。该检查不执行软件套件。
- `.gitignore:103` 命中 doc/tasks/**/*.cjs，新 RED 脚本在本地保留并显式列入 Cleanup Keep；本次 Git 写操作禁止，未 add -f/提交。Root 后续获授权交付时需明确收录该脚本，不能把被忽略脚本误当已入库证据。未运行 cleanup apply 或删除任何并行资产。
- Root 最新只授权本线程修改 upload/index.vue 的 dirty/切换确认最小 hunk 及专属 tests；生产前端构建冻结未解除，生产仍零改动。新增诊断与明确 defaultSource/actual 手改差异后，仅复跑本目录 1 项 RED（不是旧套件）：exit 1，实际 calls=[cleanup,reset-name,reset-category,load-category]、originalType=7/requestedType=8/typeAfterHandler=8、attributesPreserved=true。确认端口配置为取消，但当前 handler 没有调用；原属性没有丢失。

## Root 解除冻结后的最小修复 / GREEN / REGRESSION

- Root 提供前一轮 Vite exit 0、2635 输入无漂移和 3239 产物并明确解除冻结；这是 Root 的修复前构建证据，不表述为本线程执行或修复后的构建。
- 修改前页面 SHA256=3AA769445F758975F07ED980BC74C284212767534930574449C5712F24EEF46F；分支仍 int_qms / f06833f06。已复核页面属性事件原始 actual/defaultSource、组件 dirty 与 getSnapshot 完整性校验，决定复用原属性事件记录手改，避免未完成属性阻断切换取消。
- 先扩展自有 BDD 和专属 AST 行为测试至 7 场景，再运行 RED：node --test doc/tasks/20261015-dcc-upload-folder-reason-review/attribute-only-type-switch.red.cjs -> exit 1，7 tests/4 fail/3 pass/0 skip。失败为 cancel、close、未填完整、确认继续缺少 confirm；默认/恢复/旧项目三项原本通过。
- 唯一生产修复：index.vue 新增 uploadProjectAttributesEdited=ref(false)；已有 captureProjectAttributes 在原项目/动作 guard 后根据 JSON actual/defaultSource 比较更新；hasUploadApplicationInput 增加当前 projectAttributes 存在且编辑标记为真。3 行新增，不写其他 owner 源码/测试宿主。
- GREEN：同一 node --test 专属命令 -> exit 0，7/7，0 fail/0 skip。取消/关闭的实际 calls 只有 confirm，类型回到原 7，属性/空登记说明保留；continue 时确认后才 cleanup/reset/load。未填完整属性可取消，未调用 getSnapshot；默认/恢复默认和跨项目旧事件不误报 dirty。
- REGRESSION：在 IntRuoyiFronted 执行 node --test --test-name-pattern='cancelled type change|older type confirmation|manual project context|late cancelled project selection|confirmation cancel preserves|cancel and close both|only custom account edits|confirmed type and source switch' tests/unit/dcc-upload-w03-safety.test.cjs tests/unit/dcc-upload-application-page.test.cjs -> exit 0，实际 8 tests/8 pass/0 fail/0 skip。仅执行受影响子集，不重跑整套 34/77/181。
- git diff --check 页面 -> exit 0；git diff --numstat 页面=3/0。交回页面 SHA256=01A4EBC05F1894383959070A68F751C946833120AD9333A1280B2A795426914A，自有脚本 SHA256=21C8334BD4E7331978ADA10301F8B36F565CEACCB587BA3A6F7C9E49A3243297；submitter 仍 197A992D3F6D09E5DBC74C10A9B5FBB86A52D2E912C9DF932FEE008A57FAFA36。
- 无新后台行为，不执行 Maven/shared target/fulltypes/完整构建/E2E/实库/服务/remote/Git 写操作或 subagents。完整类型及构建是否必要由 Root 对最终源码决定，不能用其修复前结果覆盖此 3 行变更。
- 本次软件验证合计：专属 GREEN 7/7 + 受影响回归 8/8。文档结构初查仅因日志使用“8 tests/8 pass”而非汇总“8/8”未匹配计数文本，已补此汇总；不涉及代码或测试失败。
- 收尾：先设 ready_for_closeout，报告结构与最终源码/脚本哈希实际检查 exit 0（REPORT_STRUCTURE_OK）；目录只有三份报告、交接说明及 Cleanup Keep 指定脚本，未生成/删除其它产物。因当轮禁止 Git 提交/推送，按收尾规则最终改 blocked；未执行 cleanup preview/apply，不虚称通过。无待修软件事项，最终集成/必要类型构建由 Root 决定。

## 本轮属性 dirty 生命周期：BDD / RED / GREEN / REGRESSION

- int_qms / f06833f06，开始页面 SHA256=01A4EBC05F1894383959070A68F751C946833120AD9333A1280B2A795426914A。重读 AGENTS、closeout 与前端确认/状态规则；保持属性组件、submitter、矩阵及其他 owner 测试宿主只读。Root 200/200、types/build 为上一轮有效证据，未重跑，原 C01-C06 不重开。
- 只读确认新缺口：captureProjectAttributes:1609 用原始 JSON 字符串比较，会将对象键顺序、市场选择顺序、缺省/空条件文本误判 dirty；handleProjectCodeChange:1652 清理失败只恢复 formData，Q 默认事件把 dirty 置 false 后未恢复 P 的手改保护。先写 task.md 新 BDD，再新增 attribute-dirty-lifecycle.test.cjs，AST 提取当前页面真实 capture/input/project handler 执行。
- RED：node --test doc/tasks/20261015-dcc-upload-folder-reason-review/attribute-dirty-lifecycle.test.cjs -> exit 1，9 tests/5 fail/4 pass/0 skip；5 个业务断言对应字段顺序、市场顺序、空条件文本、语义恢复默认与清理失败 dirty 回滚。原项目身份/属性/说明回滚断言已通过，未虚构原值丢失。
- 最小修复：仅 upload/index.vue 新增 uploadProjectAttributesKey（固定业务字段，复制市场数组再排序，条件文本仅比较时 trim/缺省为空），capture 用语义 key 比较；清理失败在恢复旧项目身份后调用 captureProjectAttributes(previousSnapshot)。不改原始保存 actual，不宽松化提交校验，不调用 getSnapshot 验证未填完整输入，不恢复说明必填。
- GREEN：同一命令真实 exit 0，9/9。随后增加 1 项条件文本负向以验证必填文本被清空或不适用字段出现实质文字仍 dirty，最终同命令 exit 0、10/10、0 fail/0 skip；仅为新增范围定向执行。
- REGRESSION：在 IntRuoyiFronted 执行 node --test --test-name-pattern='A32 accepted project switch clears|cancelled project switch retains|late cancelled project selection' tests/unit/dcc-upload-application-page.test.cjs -> exit 0，实际 3/3、0 fail/0 skip。未跑无变化旧 7 项；只在其自有 loader 加入 uploadProjectAttributesKey 声明，保证新 helper 依赖齐全。
- 生命周期只读依据：恢复默认事件 @restore-defaults 走 capture 重新计算；成功 Q 默认加载 emit 后 dirty=false，取消项目切换先回 P 且未 select/cleanup；新一份 resetCompletedUploadDraft:1455 初始化清空属性，hasUploadApplicationInput 的属性存在 guard 避免旧 ref 构成新输入，后续项目默认事件重算。保存草稿 :2693 仅标已保存并导航，DetailApplicationPanel#load:413/:426 读取 saved.defaultSource 与保存 actual，不复用上传 dirty。未写详情、属性组件或后端，不以该静态复核冒充 E2E/持久化验收。
- git diff --check 页面 exit 0。最终页面 SHA256=D06FA211D32ECA71BBBBB808BBA2E91070E28C71ED952E9A54628049EED3976A，新生命周期测试 SHA256=5ED1392012BBD71690BBD2523381EAC39F1BA95C3C8E84C2FC7FC03D396AB7B4，旧专属 loader SHA256=9AE9F29AFF26153647C356B832DE402CAD29A4A25718B469704467A5A3B1D68A；submitter 仍 197A992D3F6D09E5DBC74C10A9B5FBB86A52D2E912C9DF932FEE008A57FAFA36。
- 禁止项均未执行。新 .cjs 同样命中 .gitignore:103，已纳入 Cleanup Keep；未 add/提交，后续 Root 正式交付需显式收录。未推断原文第 2 尾/6/8。
- 最终结构/来源核对：先设 ready_for_closeout，PowerShell 实际 exit 0，REPORT_STRUCTURE_OK，四份报告最终 10/10、3/3、指纹与两脚本 Keep 路径齐全，四份来源哈希无漂移；两个自有脚本 node --check exit 0，未执行旧 7 项；页面 git diff --check exit 0。任务仅保留四份文档及两份指定脚本，无本轮临时产物。Git 提交/推送禁止，最终状态 blocked（工程收尾），cleanup preview/apply 未执行，无虚构 PASS。
