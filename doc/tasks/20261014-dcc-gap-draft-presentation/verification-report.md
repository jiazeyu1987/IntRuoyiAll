# G08 Verification Report

## 最终软件验收已接受（2026-10-15，以此回执为准）

按Root本轮明确的最终验收回执同步，本线程没有重跑下列验证：

- M14实际exit0，23 reactor全部SUCCESS；G48声明18项及原父类27项合计45/45，0 failures/errors/skips。
- M12其余45类963项PASS且无生产差异；最终有效47份class报告共1008/1008通过，11865输入零漂移。
- F02前端联合38/38，0skip、exit0；T04完整类型与B01构建均通过，2429生产输入零漂移。
- 全软件域已被Root正式接收，生产/tests继续冻结。本线程早前的P01、R08、A35/A36及其它阶段失败/阻塞条目为当时诊断历史，不能再作为当前软件失败或未接收状态。

唯一剩余工程项为Git提交/推送收尾未获授权，任务保持blocked，不标completed。未要求的E2E或部署不是软件验收失败；最终清理和总报告由Root统一办理。本次仅更新三份自有记录，不新增代码、测试、提交或跨线程消息。

## A36 当前唯一宿主适配结果

`pnpm --dir IntRuoyiFronted exec node --test tests/unit/dcc-detail-application-ic05.test.cjs`：原28tests/10PASS/18FAIL，大量ReferenceError isPublicMatrixExecution导致handler提前返回、原断言未到；属于宿主依赖缺失，不是产品RED。仅补当前实际production声明/helper+明确saved附件事实后，canonical原命令28/28PASS，0FAIL/skip，exit0。

实际修改仅test fixture setup：抽取并执行当前Panel的isPublicMatrixExecution声明；使用当前version-display与readApplicationOrdinaryAttachmentExpectation；宿主文件响应补两份ATTACHMENT有序LongID和合法隔离serverhash，不生成生产hash。原...vue已包含真实markRaw，保留真实Vue实现，没有新fake函数。

原28个test declaration文本及所有旧矩阵/组变更/取消/IC21/精确定义/实际值断言与改前snapshot规范LF后逐字相同，TypeScript AST对照PASS。Node --check与限定diff-check exit0；原文件untracked可见，另直接UTF-8和前后指纹核验，不把空diff当全部内容校验。Panel/model/approval-matrix/applicationRead四份production输入前后规范hash相同。

加载补齐后该suite未发现新增产品RED；仅证明上述28个当前fixture行为，并非整体主链/真实页面验收。未重跑A35/43/118、Maven/fulltype/E2E；测试交还Root。

## A35 当前唯一测试适配结果

`pnpm --dir IntRuoyiFronted exec node --test tests/unit/dcc-revision-target-confirmation.test.cjs`：适配前3tests/1PASS/2FAIL，ReferenceError缺实际附件helper，原目标断言未到，不算产品RED；适配后5/5PASS、0FAIL/skip，使用canonical命令，未手工stdin注入。

测试真实加载当前revision-model.ts的readApplicationOrdinaryAttachmentExpectation和version-display.ts，不stub恒成功，不生成生产hash。明确隔离响应fixture含两份ATTACHMENT：One.pdf/Two.docx、各自Long字符串ID、排序0/1及固定合法serverhash；确认清单和正式payload有序ID/hash精确一致。保留正式来源A/1、目标B/1、缺目标确认前零写、确认期间目标改C/1零写；可见草稿文本符合已接收合同，原file.versionNo仍A/1-1和请求sourceID11独立锁定。

新增两场景：serverhash缺失/明确null均确认前拒绝且附件保留；确认期间hash改变后拒绝、零write/原两附件仍在。无生产hash计算或缺省空列表。

单测试node --check exit0；限定git diff --check exit0（文件为untracked时另记可见性及前后指纹）；UTF-8核验PASS；观察Panel/model/version-helper三份production本轮前后规范hash相同。没有写生产，没有重复43/118大suite、Maven/fulltype/E2E。

## P01 / R08 当前增量

只读5个现有定向场景4PASS/1旧宿主import ERROR，后只重开场景用现有ic05一项PASS补证。stdin复用现有真实handler验证稳定DELAYED/null+exact sourceID/hash、缺hash阻断、确认中hash漂移零write；当前publicV2open真实请求丢ID/hash已复现（Java后果仅静态严格校验依据）。详见review-followup.md，未以字符串存在称通过。

Root R08后，G03唯一写helper、G08只改面板label。`pnpm --dir IntRuoyiFronted exec node --test --test-name-pattern='R08' tests/unit/dcc-version-display-a09.test.cjs` 实际1PASS/1FAIL→2PASS/0FAIL；覆盖3正式mode/null/unknown实际helper/SFC和DELAYED无预设日期。面板eslint与限定diff-check exit0。不重跑118/43/11旧组合，不写其他owner源；RF08-01(P1)仍待分派。

## A19 当前最终软件结果

A19按Root新授权只改workbench/index.vue的两个锚点。fresh专属4项先2PASS/2FAIL，最小接入后同专属4项全部GREEN；连必要工作台process-entry/section-permissions合计11/11 PASS，exit0。准确命令见a17-workbench-wiring.md，未重复118/43大回归。

真实控件label仍原LongID；model在第一/第二稿之间实际触发onUpdate后仍精确；raw versionNo不改。全SFC按真实script bindings编译通过，ESLint index/presentation exit0无告警，限定diff --check exit0。严格基线对照只有import+插值两处差异，默认首稿和值/key/排序/handler/其它布局均保留。

workbench/index.vue已整页归还Root，hash f3228ed707ced753314341d33e9f110253b160c110e8bb6a822951b76482431a；不再写。下面旧A17“模板仍RED/未授”的历史状态已解除。G08已授可达显示范围均有当阶段证据，完整覆盖/不证明可达项见reachable-coverage；G01 owner后续验证仍须Root接收，不标全工程完成。

A19归档preview exit0，13份记录/基线全keep，delete空、warnings none；UTF-8结构和交还hash核验PASS。最终blocked因Git禁止及整体owner接收范围未收口；cleanup apply实际exit1明确拒绝blocked，无删除，准确记录BLOCKED。

## A09/A12/A17 最新结果（优先于以下历史阶段）

- A09日志+revisionlabel：初次真实行为RED 5tests/1PASS/4FAIL；实现后5/5。新增正式label保留RED→GREEN，随后Jackson实际number时间戳真实挂载RED(TypeError trim)→GREEN。最终新7/7 PASS；logs/revision ESLint exit0无错误。
- A09当时与helper8+wiring12组合26/26 PASS；加入number用例后只重跑新A09 7项，不用旧118/43冒充新验证。日志旧离线static合同exit0。
- A12：真实generic模板/4种版本列/历史源格/identity/helper/结果prompt/print HTML 6tests全部RED→GREEN；必要正文9、selection8、审批弹框13、完成摘要3、日期4合计43/43 PASS。该结果已获Root接收，父页交还，不再写入。
- A12 AST比较除8个含显示字符串的声明外500顶层语句与改前规范LF相同，仅最小diff证据，不是全工程证明。ESLint0errors/父页既有1条style warning；原始audit/processJSON不替换。
- A17：presentation formatter真实行为初RED后2/2 GREEN；专属实际radio模板目前仍RED，当前未授index.vue。准确hunk见a17-workbench-wiring.md；若未接入不得记A17完成。
- G07/G01后续已交createTime/changeDescription正式投影与decoder，本线程再次按新源码只跑A09新7项exit0，其中字符串与数字均使用真实wrapper→SFC；不声称Java/HTTP运行验收。
- 当前可达覆盖与未证明页面见reachable-coverage.md，G01确认框hunk见a12-handback.md。未运行Maven/全类型/大构建/E2E。

本轮精准命令：

```powershell
pnpm --dir IntRuoyiFronted exec node --test tests/unit/dcc-version-display-a09.test.cjs
pnpm --dir IntRuoyiFronted exec node --test tests/unit/dcc-version-display-detail-a12.test.cjs tests/unit/dcc-content-detail-split.test.cjs tests/unit/dcc-content-application-selection.test.cjs tests/unit/dcc-approval-dialog-context.test.cjs tests/unit/dcc-native-completion-summary.test.cjs tests/unit/dcc-upload-effective-activation-times.test.cjs
pnpm --dir IntRuoyiFronted exec node --test --test-name-pattern='presentation' tests/unit/dcc-version-display-workbench-a17.test.cjs
```

## 结论与边界

A03 已接入 10 个授权生产文件，真实 Vue/handler 定向回归 118/118 PASS；第一阶段 helper 8/8 另获 Root 独立复测。G08 全项仍未完成：升版逐稿时间/说明合同与受保护页、日志扩展待 Root 核准。没有真实运行验收、E2E、构建、Maven、全量类型检查或实库操作。

## A03 当前实际执行（覆盖第一阶段“页面未接线”状态）

- 初始 RED：`pnpm --dir IntRuoyiFronted exec node --test tests/unit/dcc-version-display-wiring.test.cjs` -> exit 1，9/9 FAIL，实际可见模板/真实 presentation 显示 A/1-1、A/1-2。增补签名阶段快照和检入提示后 30 tests 中 19 PASS / 11 FAIL；各 FAIL 为工作号仍可见。没有模块缺失假 RED。
- GREEN：接线后同新增接线+检入命令 30/30 PASS（升版挂载夹具显式提供 applicationReady=true，符合真实父页合同；没有改生产判定）。
- 追加初始化 RED：仅 `--test-name-pattern='actual browser initialization'` 跑接线文件 -> exit 1，实际取第一份 ID 而不是本行响应第二份 ID；去除首份猜测后同接线+working-browser-navigation 25/25 PASS。
- 最终新增接线文件 12 项：11 个真实业务行为 + 8 个授权 Vue SFC 按真实 script binding metadata 编译检查。
- 最终组合命令（仓库根目录）如下，exit 0，118 tests / 118 PASS / 0 FAIL / 0 skipped：

```powershell
pnpm --dir IntRuoyiFronted exec node --test tests/unit/dcc-version-display.test.cjs tests/unit/dcc-version-display-wiring.test.cjs tests/unit/dcc-browser-selected-version-display.test.cjs tests/unit/dcc-browser-checkin-minor-only.test.cjs tests/unit/dcc-exact-working-browser-scope.test.cjs tests/unit/dcc-working-browser-navigation.test.cjs tests/unit/dcc-relation-stage-evidence.test.cjs tests/unit/dcc-relation-history-read-contract.test.cjs tests/unit/dcc-selected-version-relations.test.cjs tests/unit/dcc-e2e-findings-fix.test.cjs scripts/dcc-relations-vue-runtime.test.mjs
```

- 独立日期格式摘要：`pnpm --dir IntRuoyiFronted exec node --test --test-name-pattern='actual browser summary' tests/unit/dcc-nc-e2e-findings.test.cjs` -> exit 0，1/1 PASS。
- ESLint：`pnpm --dir IntRuoyiFronted exec eslint` 加 helper 和 A03 10 个授权生产路径（changed-files.md 列表）-> exit 0，0 errors，DccProjectReferences.vue 旧第5/11行有 2 个 first-attribute-linebreak warnings；未自动改无关样式。
- Node 文件名中 e2e 只是既有离线单元/编译测试名称；未启动 Playwright/浏览器/服务或访问真实 API。

## A03 行为证据强度

真实编译并挂载项目文件夹表/关系弹框/DccRevisionPanel/历史关联/固定项目引用/签名阶段快照，派发可见控件的真实事件；将调用边界记录为精确 IDs 后检查，不用 fake API 充当真实业务运行验收。浏览选择和整改表使用当前 SFC 中实际模板，真实正式 handler/declaration；生命周期直接运行生产 presentation。

两个同名“草稿”点击第二份后真实 buildRevisionCommand 的 selectedIterationId 为第二份原字符串；初始化也按精确响应 ID；无效已选 ID 报错。旧 raw 可见断言改为草稿，但原始版本/当前 master 正式 A/1/精确 Long/late success/error/权限等回归保留。历史预览和关系读取仍固定 ID，快照版本没有改写。

## A03 仍失败/未运行的门禁

- `scripts/dcc-revision-component.test.mjs` -> exit 1，18 tests 中 10 PASS / 8 FAIL；其旧 application fixture 缺 selectedDistributionDepartmentIds/effectiveMode，独立 model 用例已失败，不依赖本次显示代码。单独只跑两个 model 用例仍 0/2 FAIL。revision-model.ts hash 与改前相同：5cc077dc987fef916fcb0b57f265c28a8dd3e07ad42859ae9cd8f1bd22311267。准确保留，交 Root/G01 处理，没有改弱合同或伪造 PASS。
- `dcc-nc-e2e-findings.test.cjs` 另两项详情 loader 旧测试宿主缺 viewerMode，失败源为 G10 持有父页的新上下文；本线程未改该父页或这两测试。受 helper 影响的 browser summary 用例已独立通过，不称全文件 PASS。
- Maven/全量类型/大构建仍禁止；E2E 未授权且未运行。
- DetailApplicationHistory 未写即归还，G10/DetailApplicationPanel/G11 主关系父组件/公共 API 均未接管；G11/G10 自行接线结果不纳入本线程完成声明。
- 升版 options 当前只有精确 ID 可供区分，尚未满足真实编制时间/说明，后端/前端字段缺口及最小方案见 a03-review-notes.md。

## 第一阶段执行记录（历史证据）

工作目录：仓库根目录。pnpm --dir 自动将 Node 测试运行目录设为 IntRuoyiFronted。以下均为本轮实际运行，未复用旧报告。

| Phase | Command | Actual Result |
|---|---|---|
| RED | pnpm --dir IntRuoyiFronted exec node --test tests/unit/dcc-version-display.test.cjs | exit 1；8 tests，2 pass / 6 fail。helper 初始返回 raw A/1-1、A/1-2、A/1/2-3，期望草稿失败；非字符串没有明确合同错误。不是模块缺失。 |
| GREEN | 同上 | exit 0；8/8 pass。 |
| REGRESSION | pnpm --dir IntRuoyiFronted exec node --test tests/unit/dcc-version-display.test.cjs tests/unit/dcc-browser-selected-version-display.test.cjs tests/unit/dcc-browser-checkin-minor-only.test.cjs tests/unit/dcc-exact-working-browser-scope.test.cjs | exit 0；34/34 pass。旧浏览测试仍要求 raw 工作版本可见，只证明当前未接线页面未被影响，不代表新需求已验收。 |
| Helper type | pnpm --dir IntRuoyiFronted exec tsc --noEmit --strict --skipLibCheck --types node --target ES2022 src/views/dcc/controlled-file/shared/version-display.ts | exit 0；仅 helper 严格类型检查，skipLibCheck 仅跳依赖声明检查。不冒充全量。 |
| Helper format | pnpm --dir IntRuoyiFronted exec prettier --check src/views/dcc/controlled-file/shared/version-display.ts | exit 0；All matched files use Prettier code style。 |

## 8 个真实行为用例

- 工作 A/1-1、A/1-2、B/3-19、AA/2-2147483647 只显示“草稿”。
- 同一工作身份在 WORKING/审批中/驳回/撤回/历史状态仍只显示“草稿”；原对象冻结不改写。
- 多段配置 A/1/2-3、AA/2/3/4-5 以及后端政策可规范化的大小写/空白输入不泄漏尾号。
- 正式 A/1、A/2、AA/9、多段正式及历史 V1.0 原样显示。
- versionNo 缺失/空白明确“未记录”，不编造版本。
- 非字符串输入明确抛“版本显示值必须为字符串”，不强转、不吞异常。
- 两个同名“草稿”调用既有真实 buildRevisionCommand，仍保留各自 selectedIterationId、基线原始 Long ID；把“草稿”当 ID 传入会被既有身份校验拒绝。
- 既有真实正文和历史/签名路径 builder 保留各自所选 Long ID 和来源，不因显示文案改变。

## 第一阶段依赖（已由 A03 部分覆盖）

- G10 交还 detail/index.vue、BasicInfoPanel、BrowseRecords 后由 Root 核准接入。
- Root 明确浏览 G07/G06 顺序后转交 browser；新增升版/申请/历史及其它用户页面需按 integration-notes.md 精确路径授予。
- 页面真实 Vue 渲染和 handler/正式 wrapper 精确请求回归尚未执行，需接线后 TDD；当前 8 tests 不宣称覆盖所有真实模板。
- 全量类型检查与大构建需 Root 唯一验证票；当前未申请并运行。
- Git 收尾被当轮明确禁止；任务不能标 completed。

## Cleanup and Structural Validation

A03：按阶段先标 ready_for_closeout，cleanup preview exit 0，8 个当前任务文件全 keep（含 G11 转交副本），delete 空、warnings none。17个 A03 源码/测试文件 UTF-8 与规范 LF 指纹已保存；限定 git diff --check exit 0。工程最终仍 blocked，不标 completed。以下是第一阶段历史清理证据。

A03 在 blocked 状态实际执行 cleanup apply：exit 1，输出 `BLOCKED task status is blocked; expected ready_for_closeout or completed`，没有删除文件；结果记 BLOCKED。

阶段曾标 ready_for_closeout 后运行规则指定 cleanup preview，exit 0，六份任务文档全部 keep，delete 为空，warnings none。限定 git diff --check exit 0；因为文件均为新增 untracked，另做直接 UTF-8/空白/结构检查。

独立 Python 结构验证 exit 0：8 份自有文件 UTF-8 no-BOM/换行/尾空格/冲突检查通过；六份任务文档、目标/状态/里程碑/BDD/验证/约束标题齐全；23 个具体接线文件路径与详情方法锚点存在。新增源码的规范 UTF-8/LF SHA256 与 execution-log 中原始字节 SHA256 相同。

最终状态 blocked（等待共享接线、Git 当轮禁止），不标 completed。按 blocked 状态执行 cleanup apply 实际被脚本拒绝：tool observed exit 1，输出 `BLOCKED task status is blocked; expected ready_for_closeout or completed`；没有删除文件，准确记录 BLOCKED，不写成 PASS。
