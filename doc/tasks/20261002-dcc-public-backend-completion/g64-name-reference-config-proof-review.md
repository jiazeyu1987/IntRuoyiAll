# G64 名称、引用与基础配置证明审查

Status: ready_for_closeout — 有界只读审查完成，本组未发现新的确定源码差异。具体断言、原XML、真实页面分别记录；缺证据不当未实现，旧测试总数不当current PASS。本Agent不改生产/测试、不运行Maven/build、DB/API/UI、服务或Git。HTML v1.6 SHA `53e9afc74cafd1539cd48d9f2384d364511dd4802c4d686431b444b549930222`。

## 正式入口及具体测试断言

BE简称位于 `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/`，测试相对该模块 `src/test/java/.../service/`。以下均读到实际执行语句及assert，而非仅名称。

| 范围 | 当前接线/守卫 | 实际断言与不足 |
|---|---|---|
| flow01/AC21 完整名称 | FE actualFile.raw.name→NEW_UPLOAD/SOURCE票据；UploadService预览preflight，Workflow2370再claim。NameClaim40–82用tenant+完整UTF8 binary键，不含project/folder/type/number绕过；normalize294返回原名。Mapper与正式SQL generated VARBINARY/unique保大小写及后缀，不trim/lower/去后缀。 | UploadNamePreflightDatabaseTest真实H2 Upload/NameClaim/Ticket：另project9预占SOP.pdf后actualmultipart同名拒、ticket0/bytes空/storage零调用；sop.pdf/SOP.PDF/SOP.docx逐个有ticket且返回原名；保留期冲突不泄露N-OTHER。**原XML未找到，p14/p15统计不能替current执行证明。** |
| AC24 20年/自身链 | ObsoleteRetention实际obsoleteAt.plusYears(20)→NameClaim保modern+verifiedlegacy；立即release禁止。EXISTING_MASTER_VERSION145–150核真实selected同tenant/Master/project/type/number/原名，NEW不能只raw sameMaster放行；仅实际未提交NEW票据正文可draft replay。编号沿正式tenant/project/type/number，名字全tenant。 | LegacyOccupancy141–147 actualclaimExistingVersion旧NULL行不变/foreign selected拒；214–239实际mapper不同Master原number+other.pdf拒、截止前一秒releasefalse、一owner释放name仍占、allowner截止才释放，旧claim.deleted仍0。**XML未找到。**手造第二Master不证明期满publicNEW可复用。LogicalIdentityConcurrencyTest仅SQL contains，不是实际并发约束测试。 |
| flow11/AC16 leader引用 | ProjectBrowser.canReference与DccProjectReferences双FE守卫；Controller只取LoginUserId；ReferenceService24–72锁project/folder/Master并正式LeaderService核当前targetleader，无admin/OWNER/源leader替代；reference选定受控版不复制正文。 | FormalAuthorityIntegrationTest46–50导入真实Controller/Authority/LeaderService/resolver/store/Gxp；105–112源leader99、另一leader9、成员8、非targetleader admin1即使menu=true仍create/cancel拒，reference保持1/Gxp保持1。**原XML未找到**，ui05统计不替证明；ApiPersistenceTest客户端actor伪造拒但authority是mock，不能替真实leader测试。 |
| flow12/AC17 计数/取消/颜色 | Store COUNT(DISTINCT project_id)，同folder/master精确selected重放不重复插；cancel75–90同leader/二确认/expectedID，只DELETE此reference+audit；源File/signature/relations不删。FE badge isReference或count>0橙色，引用/来源文字独立；changed刷新usage，count0源恢复。 | FormalAuthorityIntegrationTest119–127：7在project2两folder、9在project3引用；count2→首folder取消仍2→改leader旧7拒→新9删首项目最后变1→global最后变0、源File数仍2；136–158 obsolete源不可执行，cancel后current/frozen关系各仍1。TransactionTest49–56也核1/1/2→2/1/0但authoritymock。**两组原XML未找到。** |
| flow04/AC18 模板/目录 | FolderTemplateLibraryEditor→/dcc/folder-templates PUT/DELETE；Controller/Service.assertEditor121仅project-code:update，直接history+Gxp无审批。used不可delete可disable，提交冻结结构，旧项目不自动同步。FolderMaintenance.delete锁准确tenant/project/folder拒mapping/child/placement/reference，空目录二确认logicaldelete保原row。 | TemplateTest70–79 user8拒/table空，7新增/修改editedBy7/history2/旧新json；81–107旧snapshot生成旧目录/new生成新目录、useddelete拒/disable后capture拒/旧folders仍2。**原XML8全0。**FolderDeletionCombination实际断言引用/子/placement拒、cancel后空目录logicaldelete、源完整JSON及旧audit不变、晚audit失败恢复；**原XML34全0（含继承cases）**。它们是retired原执行，缺current源/compiled绑定。 |
| flow05/AC18 类型CRUD | 正式taxonomy FE→Controller category:manage；Service87–99拒child或category/file/template引用delete，69–82允许disable保ID/history，正式activepath/unique category拒失效/歧义。审批路线经category绑定type，不是folder身份。 | TaxonomyAdminTest64–76真实mapper templateitem存在→disablefalse保存→item仍1→activepath INACTIVE→delete REFERENCED；124–152有child拒、used模板delete拒而disable成功。**保存G47 XML13全0且case确实执行，target与其SHA相同。**file/category生产guard已读，但这两个usage分支没有本组独立断言，不假称全部排列已测。 |
| flow06 三动作矩阵CRUD | 正式“上传审批”RouteForm明确NEW/REVISION/OBSOLETE；samecategory/action新版本只停同action旧有效route，作废仅会签批准，缺配置不借其它动作。旧审阅Admin仅LEGACY；submit正式route/assignee snapshot不由以后global配置替旧任务。 | RouteAdminTest445–468真NEW保存后LEGACY仍active、codes/DEPT/IDs精确；687–725准确delete route/node、重建version2不复用历史，**targetXML29全0包含这些case**。G34新增889–902三typed原JSON不变/active resolver/nodecount断言存在，但targetMatrix XML17未含G34新case，原receipt68总数不能替XML。**三typed隔离current执行证明不足，不是源未实现。** |

## 原XML核验

检查所有testcase的failure/error/skipped，不仅suite计数；不相加为本轮回归。

| 原文件 | tests/fail/error/skip | SHA256 |
|---|---|---|
| `C:/IntRuoyiBackups/20261004-dcc-single-trunk-r2/retired-residual-b/IntRuoyiBackend/yudao-module-dcc/target/surefire-reports/TEST-cn.iocoder.yudao.module.dcc.service.projectcode.DccFolderTemplateServiceTest.xml` | 8/0/0/0 | `40cc041370f33bd32d74912a5a0770b733f817aff64e43c0452e59cee5a5eb46` |
| 同保护retired目录，`TEST-cn.iocoder.yudao.module.dcc.service.projectcode.DccProjectFolderDeletionCombinationTest.xml` | 34/0/0/0 | `239f07cf1abf4d061ad1e283b0c70468e015aa834204db611aec310f5655f926` |
| 本child `g47-DccFileTypeTaxonomyAdminServiceImplTest.xml` | 13/0/0/0 | `3dd83646f1b135c6bff707fc2a19d7dfe2d3213651fa217e6e18e067eaa55aa6` |
| main dcc target `TEST-cn.iocoder.yudao.module.dcc.service.route.DccApprovalRouteAdminServiceImplTest.xml` | 29/0/0/0 | `23ec7588cecf11e6a49d6c849fd6eed700b8f604c69a97b5abafc20677ee2caa` |
| main dcc target `TEST-cn.iocoder.yudao.module.dcc.service.category.DccCategoryApprovalMatrixAdminServiceImplTest.xml` | 17/0/0/0，缺G34新增case | `4c85bdb3ead2be8f562a077832fb60260274ee73c0d058a63895935a4820090c` |
| main dcc target `TEST-cn.iocoder.yudao.module.dcc.service.file.DccLogicalIdentityConcurrencyTest.xml` | 1/0/0/0，仅SQL文本 | `b32b574609d5f51e77e8163c24475c88dda4be87d69584f4a79f71fc4e18c7c3` |

NameClaim/UploadNamePreflight/Reference重点组XML在当前doc/main target及本次只读枚举Backups未找到；旧count/receipt追踪原执行，不够独立复核current运行。Root若补有限current回归，须唯一BE Owner按真实账号fixture执行并永久归档XML，本Agent不重跑或造统计。旧G27 NameClaim pin也与current不同，旧579总数不能冒为该源fresh结果。

## 最新真实证据与最少页面补证

G56真实project272/folder4/source4026引用/取消0→1→0，source有class/render色记录但offscreen非像素证明；未覆盖不同leader/非leader、多folder去重。G63 file4033实际签名会签/批准结束、00:43:39作废；原名 `20261005-dcc-full-html-g61-independent-training.pdf`、原号 `G61-DOCTRAIN-20261005-01` 保留到 `2046-10-06 00:43:39`，sharedOBSOLETE双flagNULL、原BPM/签名仍在。**这证明20年保留事实，不证明另一NEW已被拒。**G62自然A2执行/旧A1自动作废、G61/G62 currentA2/historyA1已实际分别读取，不用旧NOT_RUN覆盖。

1. **名字**：以task-owned4033实际原名为基准，在另一合法project/folder选同名原件及新编号，SOURCE正式预览应拒/File与BPM不新增。三个自有合法文件仅改名字大小写、后缀大小写、实际另一合法后缀，分别正式NEW并读回完整名。避免全库通用SOP.pdf或伪后缀正文绕格式guard。
2. **编号/自身链**：4033原project/type用不同完整新名、原number发起NEW应拒，不能换project/type冒编号scope。已4032同Master合法修订沿用自身名号且旧4026保留，可复用真实证据并只读核准确Master/name/number，不造20年时钟。
3. **引用**：两个自有target项目由各自正式leader，首项目两folder/次项目一folder同源观察1/1/2；取消依次2/1/0并看源色恢复。只有targetVIEW且非leader普通账号应看不到引用/取消；缺可读target前提时明确缺条件，不借admin/log/OWNER冒nonleader。UI隐藏只证FE，BE拒仍需上面真实authority组合执行，不能直接API/改DOM代办E2E。
4. **模板/类型**：update账号改自有模板直接完成无新approval，query-only无保存；旧项目目录不跟变。自有folder有file/reference/child时delete拒，空folder二确认删；used task-ownedtype delete拒、disable保历史且新上传不选。不要停共享908710阻并行主线。
5. **路线**：复用Root已实际NEW/REVISION及新增合法OBSOLETE配置；需CRUD时在自有category对一action新增/改/删，另两action/已提交snapshot不变。勿动主流程共享路线或借LEGACY。

本轮无新增确定P1/P2，余下是有限实际行为或原执行证明。D10“引用也跟latest”未定（引用固定真实selected，关联才latest）；期满释放权限未定，不声称publicMaster自动复用或删证据。

读时源SHA：NameClaim `909e6a7f6086f61c8aa98c5928ff660d519eca484a9fb6b01d997169a559fa99`；Reference `37668a9af33bb69d2525b2f64b705a6b51027833f1780205c46d066965b2a7a3`；Leader `534c51ec04a11fde06ca34263eb1b04a0a5ce300328b8022ea21824f67ee8f91`；FolderTemplate `19e9f4136635943e54dbcfac353bf0ebd09bd4c109b01a7a617834973d476b18`；FolderMaintenance `2d4efa63434e64eb585e9e4096b960e6210cbb64d13b86cf59cb5269736e71f2`；Taxonomy `cbffc52b531ee828bcb158eb761bdbca89404306d0a535fc6d7c1fcff561a2ee`；LegacyMatrix `aecb27be22e8a28d7ceaa8a83b0e107807947b911b80f1a8a726b13412c11d75`；RouteAdmin `694e63848bb06ce432913aa7bf32031d14a8aabc4a081d1c3e07ad258de7646a`。

## 后继Root授权补原执行证明：2026-10-06 01:57:59

Root授权运行原三类而不改生产。实际R1名称15、legacy46全0，引用13因旧AdminUserApi目录DTO缺tenant/name而7失败2错误；R2普通正向9通过，并发override旧DTO仍4错误。两失败日志/XML原样保留，不算产品RED，也不松生产认证。Root授权仅这测试目录fixture补明确tenant1、dcc-d-ID、正式账号，与现有LoginUser一致；seed及并发override均补，权限/leader/锁/assert/case数量0改。

最终R3引用13全部0、CLI0；current三组证据15+46+13=74/3，原names61未重复，原引用13失败/重跑次数不累加。原R1三XML/R2一/R3一共5raw文件已保护归档，current有效descriptor见 `g64-name-ref-current-final-receipt.json`，测试说明 `g64-name-ref-current-runtime-tests.md`。唯一测试SHA `5e46e7e908c438d345980cbc90d0b9b43d1f75b98b0e804104de61293c3d3309`；生产4项和未改tests2项前像SHA保持。**本报告前文NameClaim/UploadPreflight/Reference缺原XML属于补证前历史，当前该不足已关闭**；G34/其他配置组未重跑，不悄扩大结论。

当前3有效XML：名称 `fd6164d5e75f1d89f312221e1f9d7c2d8fa9c3516d040fc2274e873dd6506e74`、legacy `1f6d3978554d14349e2985776f8dd728245d9e0b384b377f8e936e05d482e781`、正式引用 `4b3f68459ec6673d50ffb8d1f2f783f01b6859c227dd681b96ea0e60b0f673f7`。每case failure/error/skipped均0。这里只证明真实H2/正式service和既定isolatedports，不替实际多用户前端或全部HTML目标完成。Maven/source/target已释放，不再run/package。
