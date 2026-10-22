# G08 Changed Files

## A36 当前唯一测试增量

- 仅IntRuoyiFronted/tests/unit/dcc-detail-application-ic05.test.cjs的宿主/依赖setup：当前isPublicMatrixExecution声明加入执行列表；实际production版本/附件helper替代旧显示stub并接入附件期待；隔离saved文件响应补两份附件和固定合法hash。
- 原28个test声明/所有断言逐字保持（规范LF AST对照PASS）；没有改任何G03 Panel/API/矩阵/model生产源。Vue依赖沿用真实...vue，不伪造markRaw。
- 改前完整副本doc/tasks/20261014-dcc-gap-draft-presentation/baselines/dcc-detail-application-ic05-a36.before.cjs.txt及前后hash已归档；A35测试未再改。

## A35 当前唯一测试增量

- 仅写IntRuoyiFronted/tests/unit/dcc-revision-target-confirmation.test.cjs：加载当前实际production helper、两份saved普通附件及合法隔离serverhash；保留原3个target/late测试并按已接收草稿显示改可见文案，原raw身份另锁定；新增2个manifest缺省/漂移场景。
- 任务自有task/log/review-ready/verification/changed-files同步；改前/后指纹见execution-log。
- 本次Panel/model/shared helper三份只读production前后hash相同；未写API/其它owner生产或测试文件。

## P01 / R08 当前增量

- 只读review新增doc/tasks/20261014-dcc-gap-draft-presentation/review-followup.md；记录实际请求/行为、源码校验后果、复现步骤、owner最小差异/验收及覆盖限制。
- R08仅改IntRuoyiFronted/src/views/dcc/controlled-file/revision/DccRevisionPanel.vue一处form-item label为“生效方式”；不改revision-model（G03唯一owner）/命令/身份/授权。
- 自有IntRuoyiFronted/tests/unit/dcc-version-display-a09.test.cjs添加两项实际helper/SFC模式测试和夹具mode参数；R08定向2/2PASS。
- 未写upload/Query/DetailApplicationPanel/main API/A33组件，也未重写已交还browser/detail/workbench父页。

## A19 最后增量与归还

- IntRuoyiFronted/src/views/dcc/controlled-file/workbench/index.vue：仅import getWorkbenchImpactVersionLabel及影响升版radio文字插值；整页已归还Root，不再写。
- IntRuoyiFronted/tests/unit/dcc-version-display-workbench-a17.test.cjs：扩真实radio label/model选择、完整SFC绑定编译，当前4/4PASS；保留formatter2项。
- 新增归还前基线doc/tasks/20261014-dcc-gap-draft-presentation/baselines/workbench-index-a19.before.vue，前/后hash和精确2锚点对照见execution-log。
- A17旧“index未授权/未改/模板RED”段落为当时历史；A19已获授并GREEN，当前状态见review-ready/verification。

## A09/A12/A17 当前增量

- A09：IntRuoyiFronted/src/views/dcc/controlled-file/logs/index.vue（只版本结构格/helper import）；revision/DccRevisionPanel.vue（只只读label及本地可空字段形状，含真实数字timestamp）；新增 tests/unit/dcc-version-display-a09.test.cjs，真实7项。
- A12：detail/index.vue（仅generic结构版本/历史/提示及HTML版本格），新增 tests/unit/dcc-version-display-detail-a12.test.cjs；已有审批/完成摘要/日期3个测试宿主注入真实helper，审批增加工作稿确认文案与精确原请求回归。本轮页面已正式交还Root→G07→G01，归还指纹以a12-handback为准，不再写入。
- A17：workbench/presentation.ts 新增只读候选label formatter，新增 tests/unit/dcc-version-display-workbench-a17.test.cjs。workbench/index.vue未授，仍未改；formatter两项通过，实际模板仍RED不可假称全部GREEN。
- 所有短路径均在 IntRuoyiFronted/src/views/dcc/controlled-file（生产）或 IntRuoyiFronted（测试）下；精确17个A03及当前阶段指纹见execution-log，不能把共同工作区其它改动归给本线程。
- 新增任务记录：a12-handback.md、a17-workbench-wiring.md、reachable-coverage.md、baselines/detail-index-a12.before.vue；旧记录继续保留，不覆盖旧证据或删掉失败。

## A03 增量（当前）

生产目录前缀 `IntRuoyiFronted/src/views/dcc/controlled-file/`：

| Modified path | Own difference |
|---|---|
| browser/presentation.ts | versionText 调唯一显示 helper |
| browser/index.vue | 草稿选项真实时间/说明+ID；检入提示；精确选择失败不默认首稿；保留原始校验和 merge |
| browser/ProjectFolderFileTable.vue | 版本列 slot 显示 helper |
| browser/ProjectFileRelationsDialog.vue | 源版本可见文字 helper，精确响应比对不改 |
| detail/presentation.ts | 所选版与历史版时间线显示 helper |
| revision/DccRevisionPanel.vue | 版本文本/helper，草稿 label 配精确 ID；命令与选项 value 不改 |
| detail/DetailRelationArrangements.vue | 审阅版本列显示 helper |
| detail/DetailRelationStageEvidence.vue | 冻结 versionNoSnapshot 列显示 helper；原快照不改 |
| relations/DccFileRelations.vue | 固定关联版列显示 helper |
| relations/DccProjectReferences.vue | 固定引用版本列显示 helper |

测试新增 `IntRuoyiFronted/tests/unit/dcc-version-display-wiring.test.cjs`。旧测试变化仅匹配本轮显示与真实 helper 依赖：

- IntRuoyiFronted/tests/unit/dcc-browser-selected-version-display.test.cjs
- IntRuoyiFronted/tests/unit/dcc-browser-checkin-minor-only.test.cjs
- IntRuoyiFronted/tests/unit/dcc-relation-stage-evidence.test.cjs
- IntRuoyiFronted/tests/unit/dcc-e2e-findings-fix.test.cjs
- IntRuoyiFronted/tests/unit/dcc-nc-e2e-findings.test.cjs
- IntRuoyiFronted/scripts/dcc-relations-vue-runtime.test.mjs

任务文档新增 a03-review-notes.md、baselines/DetailApplicationHistory.before.vue（自有归还证据，不是生产副本）。DetailApplicationHistory.vue 在任何 G08 写入前归还 G11。所有既有文件改前 SHA256 及 A03 最终指纹见 execution-log.md，工作区已有其它差异不能认作本线程全部所有。

## 第一阶段新增（保留记录）

| Path | Purpose | Before |
|---|---|---|
| IntRuoyiFronted/src/views/dcc/controlled-file/shared/version-display.ts | 唯一纯显示 helper；工作迭代仅“草稿”，原始正式/历史标签不改 | ABSENT |
| IntRuoyiFronted/tests/unit/dcc-version-display.test.cjs | 真实生产 helper/既有升版命令/既有导航的 8 个行为测试 | ABSENT |
| doc/tasks/20261014-dcc-gap-draft-presentation/task.md | 目标、授权、BDD、里程碑、验证、状态、设计约束 | ABSENT |
| doc/tasks/20261014-dcc-gap-draft-presentation/execution-log.md | 改前指纹及本轮 RED/GREEN/回归证据 | ABSENT |
| doc/tasks/20261014-dcc-gap-draft-presentation/verification-report.md | 实测结果与证据边界 | ABSENT |
| doc/tasks/20261014-dcc-gap-draft-presentation/integration-notes.md | 最小显示合同、具体接线锚点、依赖及页面 RED 设计 | ABSENT |
| doc/tasks/20261014-dcc-gap-draft-presentation/review-ready.md | 第一阶段交付及 Root 审查/转交请求 | ABSENT |

## 第一阶段边界及当前继续保护

第一阶段未写既有生产文件。A03 仅改上述授权清单；package.json、公共 API/VO/后端/数据库、G10 三文件/DetailApplicationPanel/DetailRelationsPanel/view/presentation 仍未由本线程改动。无需新增依赖或 package script。
