# G55 四大方向最终范围独立复核

Status: ready_for_closeout — 2026-10-05 12:58 +08:00最终有限只读复核冻结。四方向本轮同admin正向已实际走到受控、下发、项目文件夹列表及正文可见；审核和批准两类驳回后修改重提、新申请待办均已有实际证据。不同账号资格不属于此次同admin实际已验范围。本Agent仅读取现有代码、记录和保护目录中的页面证据，未改源码/测试或运行Maven、types、build、E2E、DB、服务、Git。

## 精简验收矩阵

| 方向 | 已实现业务 | 已有实际Playwright页面证据 | 尚缺必要验收 | 当前明确代码风险 |
|---|---|---|---|---|
| LD01：批准DCC项目、产品与OWNER可上传 | 新DCC目录与MDM来源有独立真实ID；同租户COMPLETED申请、ACTIVE关系、目录原码名精确核验；服务器保存产品来源。批准新建同事务为选定负责人建USER/OWNER，不给其他办理人自动授权，不新增14位编码要求。 | 申请7审核、批准后COMPLETED；项目271、产品614及源`G53PRODUCT`显示并用于真实上传。文件2054545668044084026已受控A/1并下发。r2/024、026；r3/003、005、066、088、089；r4/050、066。 | 与申请人不同的非admin负责人使用初始化OWNER实际进入项目并上传，以及申请人没有自动项目权限的分离账号边界。 | 本次未发现新P1。旧错误tenant0目录不被修复或认领；当前声明的产品依据失效会明确拒绝，不能用UNBOUND/MDM降级补齐。 |
| LD02：无预设文件模板/固定文件名门槛 | 正常NEW预览、提交及工作稿取消项目预设模板位置/文件名白名单；仍核正式启用类型叶、唯一类别、项目权限、真实原件票据和全名占用。前端类型独立选择，名称取实际源文件。项目创建用的文件夹模板合同保留。 | 新批准项目选择技术调研报告，实际文件`20261005-dcc-four-directions-g53-source.pdf`预览、确认取消、再确认，真实submit成功；没有办理项目预设文件模板步骤。r3/072、083–089；后续同一源名受控。 | 本方向正常PDF上传正向已验。项目预设文件模板辅助读取异常及其它正文格式属于后续受影响细节，不冒称本轮全部格式已验。 | 当前两个正式NEW端口不再调用validateUploadLocation/Selection，唯一类别守卫保留；未发现新的硬名单或固定名称残留。 |
| LD03：用户只选择一个逻辑项目文件夹 | publicPlacement在原事务中锁真实project/folder并解析tenant/folder/category→唯一base→内部叶子；mapping、File、placement及Gxp同事务。NEW拒客户端directoryId，内部context派生；历史/升版沿原正式位置，不猜第一叶/NAS路径。 | 上传只选项目271/G44文档folder2；正式mapping2/base908991/leaf913876由Root只读佐证。下发后folder2显示相同source.pdf/A/1；r6/018–019实际popup PDF第1/1页，可见canvas685×969、非白2208/采样26578，正文实际渲染。 | 正常列表→正文已验。映射复用、晚失败回滚与维护占用已有H2证据，未用本轮UI重做全部维护排列。 | 未发现新P1。必须存在唯一正式类别base；不自动建立远端NAS/ACL。派生base授权投影限定当前有效映射且手动leaf规则优先，实际分离账号收窄/撤权尚未验收。 |
| LD04：统一待办、通知跳转、驳回重提 | 单一DCC provider委派原项目申请；TODO/DONE、byId、stage历史用准确tenant/request身份和Long字符串。通知使用正式幂等消息接口与办理事务；驳回重提新建后继申请，原决定保留。 | 7的审核/批准待办打开准确申请并完成。8审核驳回→消息原申请→重提9且保旧8；9批准驳回→消息原9→修改→新10 PENDING_REVIEW，旧8/9保REJECTED。r6/010实际统一中心新10审核待办12:52:23及G53-R准确摘要可见。 | 同admin两类驳回重提、新待办已验；分离审核人/申请人/批准人资格不能由同admin外推。 | 未发现新P1。r4登录超时、r5运行中Jar替换的NoClassDefFoundError失败均保留；修复运行环境后才真实再提交，不是假成功或源级兼容。批准人配置/本人兼任仍属HTML D02未定政策。 |

本次真实动作由同一个admin账号承担项目负责人、申请人、审核人、批准人及文件会签/文控等角色。两部门义务分别办理并有独立签名，属于多部门、同账号验收，不是多用户验收。

## 最新主链证据及历史覆盖关系

保护证据根目录：`C:/IntRuoyiBackups/20261005-dcc-four-direction-runtime/`。`g53-real-ui-r4/050.json`实际详情显示ACTIVE/A/1、六条真实签名406–411、批准选择文件负责人admin、受控日期11:51:36及预设生效日期2026-10-05，SHA `ca6c84a84613442167c437a69f87c74f8a315f97a5b1fad1ec3cf3dec71353a6`。

下发二次确认064、点击065后，066观察到自然POST准确file的distribute，并且实际详情下发时间变为2026-10-05 12:11:29；同ACTIVE/A/1与预设生效日期保留。066 SHA `480533c67f9c64515ba0337fa4a427876a3001a5a782ffbfd6f22bb91e860e6f`；项目文件夹列表069 SHA `f09b446e1bdfbf96a03b74bdabf343677e250b0052d2b0180b2d77b6f68b042c`。成功结论依据实际页面状态变化及签名/时间，不只依据HTTP200。文件未选培训，不能据此称线下培训上传分支也已实际验收。

G50旧“等待升级授权/包未启动”、G52旧blocked、G53旧多active定义失败、G54旧NEW0POST都是历史状态：G53已获授权并实际clone/source三SQL首次/重复12次验证，旧业务原列不变；BPM latest修复及G54正式动作scope修复后，Root已通过真实页面继续主链。当前G54包SHA `8048280bc1fcc56c12638d910501d37e8d0a278ffd6f842672d533a32ac05e45`，不能继续拿旧包或旧blocked否定上述新结果。

## 当前源码有限复核

业务依据为`docs/product/dcc-final-requirements.html`上传01、创建03、会签09及D02政策范围。提交记录：LD01/02 `64ba36ae92e4d2f08c3808b9130c085b55a2bea8`；LD03 `5e09a42d9997a247fde52122e4273132c502347b`；LD04 `69caf4fe43694d7a86970b7fafe2e8671b8333e1`，来源为已封存commit-proof，不运行Git。

12:22复核时，按后续方向覆盖相邻文件的顺序合并g46/g47/g48/g49正式封存清单，选取45个src/main或SQL条目（含原清单中的隔离SQL夹具）bytes/rawSHA均0漂移；前端当时最新清单11个src源码同样0漂移。之后已授权G55详情项目投影/路由/签名文案增量覆盖相邻Query/VO/detail/workflow旧pin，准确最终指纹与13/31定向证据见`g55-final-independent-review.md`，不是未知漂移。G53 BPM及G54指派的增量另按其最新独立审查和Root包收据，不把早期hash视为当前全部源码。

主方法锚点：

- `service/projectcode/productcreate/DccProjectProductIdentityResolver#resolve`与`DccProjectProductCreateWriteService#writeApprovedRequest`；`service/projectcode/access/DccProjectAccessServiceImpl#initializeApprovedProjectLeaderOwner`。真实产品原码名使用Java精确比较，产品与MDM ID分开，OWNER依正式选定启用账号初始化。
- `service/file/DccControlledFileWorkflowServiceImpl#validateSourceUploadContext`、`prepareSubmitContext`保正式唯一类别；`DccPublicUploadPlacementService#create`与`DccProjectFolderStorageService#resolve`保服务端位置及事务，不接受NEW客户端NAS字段。
- `approval/DccApprovalTaskAdapter#list`与`DccProjectProductTaskDelegate`仍只有一个provider；`DccProjectProductNotificationService`MANDATORY调用正式消息接口；`CreateServiceImpl#resubmitRejectedRequest`和原approve分事务决定/资产writer保持原历史身份。前端`notifyMessageNavigation.ts`及`ProductCatalogTabPanel.vue`准确打开原request，不用当前pending替代。

未发现四大方向新的确定代码阻断。其余完整HTML的升版、作废、引用、未来生效、培训、多用户权限等不因这次四方向正向成功自动成为已验收；其中非本四方向的细节不在本轮扩写或返工。

12:54后继运行证据：`g53-real-ui-r6/008.json` SHA `e2ce7e0c17f0c8464147789b1e9ffd35b9ce792ec4a8c096394251cb32ccc8c2`显示实际原9重提及新PENDING_REVIEW；Root只读核10.previous9。新运行包SHA `381754e50a21922e3c106b3dd6e69cec663b9ff9a52ae4cdae2e00f43b1b706c`，此为G55详情修复之后的当前包；前述G54包为先前受控/下发动作的准确历史包。r5/008运行包替换失败及r4/087认证超时原证据保留，详见`g55-final-independent-review.md`后继段。

12:58最终补证：r6/010新10统一待办、013/014正式项目与产品分别显示、015/016返回browser无三个空ID请求及无请求不存在错误、018/019实际PDF viewer和可见canvas证明均已核。Root报告实现本地提交`ee4d9b4e0136885deb28ac9e9ca83bf68b0026a9`，未push且3保护资产不变。两报告至此冻结，本轮必要主流程没有遗留代码或运行阻断；其它完整HTML/分离账号/配置维护场景按上文边界保留，不能以四方向成功扩大为全部业务已验收。
