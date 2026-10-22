# 交 Root：仅修改属性后切换文件类型的确认缺口

## 本轮最新交还：生命周期新增边界已修复

upload/index.vue 当前 SHA256=D06FA211D32ECA71BBBBB808BBA2E91070E28C71ED952E9A54628049EED3976A。新范围先 RED 9 项中 5 fail/4 pass，修复后最终生命周期专属 10/10 + 受影响项目切换子集 3/3。只新增语义比较 key，清理失败回滚复用 capture 恢复旧 dirty；不修改其他 owner。原 7 项与 Root 200/200/types/build 未重复。新 test 路径及哈希见 verification-report.md；任务内两份 .cjs 均需后续明确收录，不能默认已入库。以下先前 3 行/7+8/冻结说明仅保留历史过程，以本段最新交还为准。

## 最新结果：已授权并完成最小修复

Root 已解除冻结。本线程仅 upload/index.vue 新增 3 行记录属性 actual/defaultSource 差异并纳入现有输入判断，未改属性组件、submitter、矩阵。扩展专属 RED 4 fail/3 pass 后 GREEN 7/7；受影响取消/项目切换/自定义输入回归子集 8/8。属性本身从未宣称丢失，修复的是确认/取消机会。最终页面 SHA256=01A4EBC05F1894383959070A68F751C946833120AD9333A1280B2A795426914A。以下候选/冻结阶段说明仅为历史证据，当前无需其他 owner 再实现；待 Root 最终集成复核。

## 边界与来源

登记说明选填/500 字无新缺口；本项属于上传切换状态保护，超出本线程登记说明/上传说明范围。依据 `doc/tasks/20261014-dcc-gap-fix-coordination/requirements.md` 明确要求“切项目/类型前对修改过的属性/部门/关联提示确认”。第 6/8 项细节仍未知，本项不依赖补充需求。

## 触发步骤

1. 选择项目和 APPLICATION_CUSTOM 文件类型，项目默认属性读取完成。
2. 只编辑本次项目属性；不选文件夹、不填名称/编号/生效方式，不选部门/关联，不上传附件，不填自定义批准人/文控审核人。
3. 改选另一合法启用文件类型。预期先提示确认，取消后原类型与属性保持；当前 hasUploadApplicationInput 返回 false，直接走清理/重置和类型接受，未提供取消机会。

## 当前源码证据

- `IntRuoyiFronted/src/views/dcc/controlled-file/project-attributes/ProjectApplicationAttributes.vue#edit`（:75）将手改实际属性 emit 给页面；`state.ts#editActual`（:69）标 dirty。
- `IntRuoyiFronted/src/views/dcc/controlled-file/upload/index.vue#hasUploadApplicationInput`（:1610）列入说明、文件夹、部门、关联、附件、有效模式和自定义账号，未列 projectAttributes / 属性 dirty。
- 同页 `handleFileTypeTaxonomyChange`（:1795，弹框条件 :1806）只依赖该布尔判断，不调用属性 dirty 确认；后续 :1817 接受新类型并重置文件/部门/关联。
- 项目切换另由 `ProjectApplicationAttributes.selectProject` / `state.ts#applyProjectDefaults:81` 处理 dirty 属性确认，不能据此推定类型切换也已覆盖。
- 类型 reset 未清空 projectAttributes，因此本项是缺少确认/取消机会，尚无属性原值丢失的运行证据，不能扩大成数据丢失结论。

## 复核方式与建议

新增本目录专属 `attribute-only-type-switch.red.cjs`，用 AST 提取实际生产 hasUploadApplicationInput 与 handleFileTypeTaxonomyChange，在隔离状态下执行；只注入确认/清理/重置端口，期望取消路径保留原值。测试不访问 HTTP/真实服务/数据库，不执行旧套件。

请 Root 或上传状态 owner 决定用正式属性 dirty 状态参与类型切换提示，保持只加载未编辑默认值的行为口径；不要把登记说明改回必填，不改公共矩阵身份，不猜测第 6/8 项。生产前端当前冻结，本线程不实施修复，也不将未执行的 GREEN/回归记为通过。

## 本次实际 RED

执行 `node --test doc/tasks/20261015-dcc-upload-folder-reason-review/attribute-only-type-switch.red.cjs`，exit 1；1 test / 1 fail / 0 skip。预期只调用 confirm 并取消；实际顺序为 cleanup、reset-name、reset-category、load-category。依赖与源码提取成功，失败是业务断言。未更改生产，不执行 GREEN 或旧通过套件。本脚本已纳入 task.md Cleanup Keep。

保留边界：`.gitignore:103` 忽略 doc/tasks/**/*.cjs，故本脚本当前为明确保留的本地证据，未获 Git 写授权也未暂存。Root 后续正式交付须显式收录，不能默认已经入库。

## 最新 owner 授权（冻结尚未解除）

Root 已看到本项并明确授权本线程只改 upload/index.vue 的 dirty/切换确认最小 hunk 和专属 tests；属性组件、submitter、矩阵等保持只读。构建冻结仍有效，当前没有生产修改，GREEN 待明确解除后执行。专属 RED 补充实际前后诊断，明确当前属性保持，缺少的是确认/取消机会与原类型恢复。
