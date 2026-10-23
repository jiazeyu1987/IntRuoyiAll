# G66 配置、权限与名称完成证明审查

Status: ready_for_closeout — 一次有界只读审查冻结。逐需求区分正式实现、有效测试、实际页面及剩余证明；本范围无新增确定生产差异。本Agent不改生产/测试、不运行Maven/build、DB/API/UI、服务或Git。HTML v1.6 SHA `53e9afc74cafd1539cd48d9f2384d364511dd4802c4d686431b444b549930222`。

## 条款与证明边界

方法相对 `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/`；测试相对同模块 `src/test/java/.../service/`。这里的缺证明不等于未实现，也不只以“没发现P1”作为完成结论。

| 条款literal | 准确实现 / 具体断言 | 当前证明和最小补齐 |
|---|---|---|
| AC01/flow03：后台审核账号、提交冻结，改配置不改旧申请 | `productcreate/CreateService.createPendingRequest`83/110–112保存配置账号ID/username/nickname；review193调用`ReviewerConfiguration.assertFrozenReviewer`，不取后来配置替换。ReviewerTest实际先创建first、configuration.save改8、旧first仍1/admin、8review旧申请拒、1review旧申请，新submission冻结8且8实际review成功；缺配置请求/identityclaim均0、停用/foreign账号拒。 | service/test当前源均匹配p05/p07 pin，但**这一专属原XML未找到**，项目7/10同admin正向不能代配置变化分支。唯一必要新补证为下节现有单类顺序运行，不加测试/生产。 |
| AC01：项目/产品可用、模板目录和三属性，负责人一条USER/OWNER、其他人不默认授权、失败整回滚 | `CreateWriteService.writeApprovedRequest`56–119：同tenant WRITING→project→`AccessService.initializeApprovedProjectLeaderOwner` MANDATORY→frozenfolders→同tenant catalog/ACTIVErelation→COMPLETED/audit；Access55–80核真实selectedleader/启用/tenant、原rules空、只插一USER OWNER/rowcount1。`G46ApprovedProductIdentityTest`继承7条OWNER测试：leader7读新folder/项目成功、一规则7/tenant1/OWNER；applicant9/reviewer8/approver1均不可读；lateaudit/folder/zeroowner/relation/completion失败全部资产空、request仍WRITING；COMPLETED重放拒且后来VIEW规则JSON不变。 | G46原13 XML实际含7 inheritedcase且all0，Writer及G46测试current源pin仍匹配；G49属性申请13 XML有“项目defaults/负责人/冻结申请时目录”实际断言，服务/test仍match G49 pin。早G39Access/Owner基类pin后来改变，不能假称G39整体current；Root项目申请7→271/614/OWNER、申请10→272/615已真实批准及可读，构成主正向。补上述reviewer单类即可，非所有角色排列。 |
| AC16：只有target项目leader，引用橙色/文字，按project去重 | `ReferenceController`只LoginUserId；`ReferenceService.create/createBatch`锁project/folder/Master→真实LeaderService，无OWNER/admin替代。FEProjectBrowser.canReference、DccProjectReferences双核；badge独立“引用/来源”，source `.is-referenced`样式橙色。FormalAuthorityTest105–112源leader99/另一leader9/成员8/admin1即使menu=true也create/cancel拒、reference仍1/Gxp仍1；119–127两个项目三个folder计数2。 | **G64 current13正式Controller/Leader/H2/Gxp XML全0、sourcepin当前匹配**；74总证明不依旧统计。RootG56实际单folder引用0→1成立。尚未真实不同账号/两项目三folder，但BE规则已实际测，不造API E2E或改DOM触发隐藏操作。 |
| AC17：targetleader取消、单/项目最后/global最后计数及源色恢复、只移入口 | `ReferenceService.cancel`75–90核leader/confirmed/expectedID，仅DELETE引用+audit；usage DISTINCT；FEchanged刷新source，count0普通色，存活引用仍有独立标识。FormalAuthorityTest119–127 count2→首folder取消仍2→改leader旧7拒→新9删项目最后变1→global最后变0；136–158 source/File/current/frozen两关系仍保留。 | 同G64 current13强证明；Root真实0→1→0/二确认/入口移除已办，源只有class/render证明，不夸offscreen像素。无需重复旧取消回归。 |
| AC18/flow04：模板只permission不审批；修改不自动改旧项目；used模板停用；非空目录保护 | `FolderTemplateService.save/delete`28–67+assertEditor121仅project-code:update，直接history/audit无审批；used禁止delete可disable。TemplateTest70–79无权8拒/table空，有权7直接保存editedBy7/history2旧新JSON；81–107 frozen旧结构生成旧目录、新结构生成新项目，useddelete拒、disable后capture拒/旧folders仍2。FolderMaintenance.delete39–54当前锁tenant/project/folder并拒mapping/child/placement/reference，空folder确认logicaldelete。 | 原模板8 XML all0，**归档与current生产/test编译字节均相同**，支持同方法语义。旧delete34 test字节同、生产字节不同（后加G48 mappingguard），不标current34跑过。G48 sourceguard/test/currentpin相同、保存XML5 all0含mappedfolder拒；Rootfolder3 create/edit/delete/历史保留是真正当前正向。映射guard已足，无需泛重跑；如果要全页面AC18，最少用自有模板做有权save/无权无保存、一次有内容folder拒，勿停共享目录。 |
| AC18/flow05：文件类型新增/改/未用删，useddelete拒且能disable保历史 | TaxonomyController category:manage CRUD；`TaxonomyAdmin.deleteTaxonomy`87–99拒child/category/file/template引用，update69–82允许停用并保持typeID；activepath/uniquecategory拒停用/歧义。Test64–76 actualmapper有templateitem→disablefalse保存→item仍1→activepath INACTIVE→delete REFERENCED；124–152 parentchild/useddelete拒而disable成功。 | 保存13 XML all0，**current生产/test编译字节和归档一致**。这是模板使用分支，不声称每一种usage都专门运行；file/category生产guard明确。如补页面只在自有type新建→改→unused删、used删拒→disable保历史即可，不改908710。 |
| flow06：三动作矩阵各自CRUD、不能借其他动作、修改只影响以后申请、历史保留 | 正式RouteForm/RouteAdmin保存准确category/action新版本，只停同action已生效旧route；typed resolver精确action，OBSOLETE只两阶段。LEGACY审阅Admin save/import/delete仅同scope，submit保存route/assignee snapshot。RouteTest445–468 actual NEW后旧LEGACY仍active、DEPT/codes精确；687–725 exactroute/node删除并历史version2。G34新增889–902三typed原JSON不改、resolver仍active、节点数量不改。 | Root上传/升版路线及合法作废两节点配置已实际使用；旧Route29/XML及Matrix17不能当G34新增case当前XML，缺该直接历史binding。**不因此开新回归范围**；最小实际配置证明可在自有category改/停一个action，另两action及已提交snapshot不变。不动主流程共享route、不借LEGACY；多人ANY批准政策仍D02未定，不把现代码ANY补成用户确认。 |
| AC21：全tenant新链精确完整名、case/ext不同，换project/folder/number不能绕 | `NameClaim.preflight/claimIdentity`以完整UTF8 binary名查询tenant，不lower/trim/去后缀；Workflow从真实SOURCEticket取名。UploadPreflightTest真实multipart：project9占SOP.pdf、project5新同名拒、ticket0/storage0；sop.pdf/SOP.PDF/SOP.docx各返回实际名和ticket；保留期冲突不泄漏另项目编号。 | **G64 current15 XML all0/source匹配**；RootG64实际已作废原名NEW预览负向成立，不把它说成三个合法变体都实际完整提交。有限剩余页面仅三个case/ext合法变体，需求规则后端已强测。 |
| AC24：实际作废起20calendar年占原name/number，同逻辑文件自身新版本例外 | ObsoleteRetention.plusYears20→NameClaim retention；EXISTING_MASTER_VERSION必须actualselected同tenant/Master/project/type/number/name，NEW不能raw sameMaster旁路。LegacyOccupancy141–147自身actualselected成功且原NULL行不改、foreign拒；214–239原number不同Master拒、截止前一秒releasefalse、一个owner释放后name仍占、allowner实际截止才release，旧claim.deleted仍0。 | **G64 current46 XML全0/source匹配**；Root4033真实作废name/number保留到2046-10-06；4026→4032同Master合法修订沿用名号已真实。actualNEW原名拒已补，原number不同名NEW页面仍是最少补证。编号沿既有tenant/project/type身份，不跨scope假判重；期满publicMaster复用/释放权限未定，不据手造Master测试扩成自动释放。 |

## 当前绑定结论（不合并历史数量）

G64 final receipt `3c4bf26f…1304b`：7 source+3 XML逐byte复查0漂移，15+46+13=74；唯一test DTO修正pin `5e46e7e…3309`，生产0改。三个XML完整SHA在该receipt及G64审查报告，所有case fail/error/skip0。

模板/类型原XML分别 `40cc0413…a5eb46` / `3dd83646…55aa6`；原目录删除XML `239f07cf…5f926`。归档base是 `C:/IntRuoyiBackups/20261004-dcc-single-trunk-r2/retired-residual-b/IntRuoyiBackend/yudao-module-dcc/target/`；与当前对应class的准确比较：

- Template production：`77c4f93c44e8a460a9f36411aff83c411cbf12a7a6d9d95849e161271dfc213f`；test：`36a532670c4f99fdc8a3ab3bd5ba105462059027568027f8ce5617f730a1fc4e`，两边分别完全相同。
- Type production：`c275a514d03e9ecf5b158228012c521ad4d30733b9a14f76ad94aaf8bc4b83db`；test：`b286761142a9d001eb19852be78a2e133e36c6841a064ce8f0c631dc31312651`，两边分别完全相同。
- Delete test仍同 `53740835…4ad317`，production旧 `2cc071ed…947cbb` / current `84dac8a3…46fc59b`，因此旧34只能原运行。G48 guard/FolderMaintenance/test当前源与manifest一致，保存XML5 SHA `1d7a222de4aec9d550f3fe44b2dd2c4544f5884b199af344918fccfd53f07bde`；具体mapped保护case已执行。

G46原13 XML SHA `78c370f728546c173c84082bfe3b3c4c1bf6ffbeb115bcebcb7ba0fa22218974`与原receipt相同，Writer/G46源pin相同；G49属性13保存XML `eba3f46b…b775db`及sourcepin相同。这些旧执行不写成新的current回归或总74以外的“本轮全部通过”。Root项目7/10、folder3、引用0/1/0、4033保留2046与同名预览失败只按各自真实动作解释，未用同admin冒多用户证明。

## 唯一建议新增执行补证：审核配置变化

实际类：`cn.iocoder.yudao.module.dcc.service.projectcode.DccProjectReviewerConfigurationTest`。

文件：`IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/projectcode/DccProjectReviewerConfigurationTest.java`，current/p05-p07 SHA均 `18434f79775c56116cfd1e33f273f2ba8c17e3f3628c9f823e75d6cd2932155e`；对应Service SHA均 `e1c74d6b2640b6a5b257b79ff9c4886fbb0e47e6248abafc091e7713b64a1c0d`。关键方法 `configurationChangesDoNotReplaceFrozenReviewerAndNewSubmissionUsesCurrentAccount`；其他3本类方法已明确缺配置、失效/跨tenant与审计失败回滚。它继承DecisionPersistenceTest，真实执行数量须按新XML算，不能猜4。

工作目录main `IntRuoyiBackend`，Root/唯一BE Owner在现组终态后顺序执行：

`mvn.cmd -o -pl yudao-module-dcc -am '-Dtest=DccProjectReviewerConfigurationTest' '-Dsurefire.failIfNoSpecifiedTests=false' test`

只运行既有类，保原XML/CLI/log和current源hash；夹具依赖错误准确报、不降源权限，不加SQL/角色/审批平台。本文不运行它；不再扩矩阵CRUD或非空全量回归。剩余实际页面补证与唯一必要XML由Root排顺序；未定D02多人批准、D10引用followlatest、期满处置不擅补。
