# G55 详情投影、路由与签名说明最终独立复核

Status: ready_for_closeout — 2026-10-05 12:42 +08:00，仅本轮有限源码/已有测试证据复核；未发现当前范围新的P1。未修改生产/测试，未运行Maven、types、build、DB、UI、服务或Git。真实页面重新验收由Root执行。

## 正式项目事实

`DccControlledFileQueryServiceImpl#getControlledFile`（527–535）先执行原`canAccessDetail`授权，之后才组装包含route snapshots的详情。`toRespVO`（2874–2880）继续保存文件版本的真实productSource、各产品ID、productCode和productName；只在授权详情分支调用新增`populateDetailProjectIdentity`（2988–2998）。后者只按文件正式dccProjectCodeId查询项目，核文件tenant、项目精确ID、当前tenant及未删除；绑定项目缺失/外租户/删除明确拒绝，不从产品名称或代码补项目事实。

历史文件没有dccProjectCodeId时返回未绑定项目且可沿原授权读取。正式项目停用不被等同删除，历史详情依原资格可读。新增projectName/projectCode为当前正式项目事实，未修改产品冻结事实、正文资格、文件行、历史签名或项目身份。前端`currentDccProjectCodeText`（3875–3876）只格式化这两个项目字段，产品显示及按实际projectID跳转保持原合同；未绑定历史显示“-”，没有借产品名伪装项目。

## 当前详情路由与迟到响应

`detail/index.vue#isActiveControlledFileDetailRoute`（4716–4719）要求标量字符串正LongID、范围不溢出，当前params ID、精确detail路径及完整fullPath匹配。`reloadAll`（4947–4970）递增generation并捕获ID/fullPath；非详情或非法ID清理原读取状态、失效签名读取序列，不发详情、纸质分发或权限说明请求。

`loadData`（4727–4768）所有file/paper请求使用捕获的requestedId，接收后再核当前generation/route/ID与响应fileID。`loadAccessExplanationOnly`（4849–4867）同样在请求前、成功和错误后核当前上下文；离页后的旧失败不能读取新的空ID或在新页面报旧错。`reloadAll`的当前真实读取错误仍显示，不吞权限拒绝。原详情合法同ID刷新、切换另一合法ID/带合法query和后续审批读取仍被测试覆盖；没有以离页守卫禁用正常刷新。

## 签名说明与业务时点

`actionDialogSubmitFlowText`（3809–3817）明确上传/升版为“会签→批准→培训（如需）→文控审核→受控→下发”，保留预设生效日期及“新版生效时旧版自动作废”；作废为“会签→批准，批准通过即结束”。这与HTML第09项及已确认日期口径一致，没有再宣称文控审核后直接生效/自动下发。驳回文案与实际动作API未改，本次只纠正文案，不声称改了审批节点。

Root已执行真实受控与下发，r4/050受控时间11:51:36、r4/066下发时间12:11:29且预设生效日期2026-10-05保持。新文案及项目投影是否已加载到实际运行包/页面，应以Root后续package及页面证据核定，离线验证不能替代实际UI。

## 冻结证据

BE `g55-project-projection-fingerprints.json` SHA `dd66e487679e9cf7a10e7d7d159b350fa6f713b7e9e9ea34732bdd359f79a12e`：2生产、1测试全部bytes/rawSHA匹配。实际归档XML与原日志SHA匹配，13 tests、0失败/错误/跳过；含正式项目与产品不同名、未绑定历史、绑定项目缺失/外tenant/删除、name-only不可读及原详情日期/版本/检出等必要邻接场景。有效RED1失败0错误；编译夹具错误不冒业务RED。不是完整Query类回归或真实MySQL/API验收。

FE `g55-detail-fingerprints-r3.json` SHA `2c514df78912f9ea0988ec9b77bb17809e11f1cfe7ee8e9219de34dc6205e365`：2生产、3测试全部匹配，7份原日志bytes/rawSHA一致，最终31 tests/31 pass/0 fail/0 skipped、2源lint空日志与收据一致。实际computed/load函数和Vue子树离线执行，目录/网络为显式隔离端口，不当作真实页面验收。

| 当前生产源码 | SHA256 |
|---|---|
| DccControlledFileQueryServiceImpl.java | 069d9041576faa69915fcc386994415bcf9d855810920fe21a483341ca5f33c9 |
| DccControlledFileRespVO.java | 244f4a95b2cb4874eedfe2c622f8d53ae6e1335b804bbc3a2d239cb30a7002f3 |
| detail/index.vue | b61d3be83136637768167d8368c281355c68ceaa999f1de1bc79804abb77e2a4 |
| controlledFile/workflow.ts | 91628ac1833ee857201f77fa55e2c952fbf2b02652d35d94672bc8b04d322d2a |

上述已授权增量明确覆盖g46/g48原Query/VO及workflow字段、detail旧指纹；原封存记录保留，不把合法后继变更称无关漂移。G54适用范围源码未修改。

## 批准驳回实际验收进度

保护目录`C:/IntRuoyiBackups/20261005-dcc-four-direction-runtime/g53-real-ui-r4/`中，074实际申请9审核通过进入PENDING_APPROVAL；076实际approve/reject后显示REJECTED并保留由8重提的申请链。080批准驳回站内消息、081阅读、082准确原9记录、086修改后确认均已页面观察。

087出现真实“登录超时，请重新登录”，没有提交新10。该阶段是会话过期，既不能宣称重提成功，也不能宣称项目重提代码失败。本review未自动调用API重试，Root重新登录后继续真实页面。已同步四方向矩阵，将批准驳回通知/打开原申请标为已验，最终重提新记录仍待新证据。

证据锚点：076 SHA `0222320a86d63e724fac9cfa6c2d0290b6caa80cbdb8a98ba99689f454072764`；082 SHA `63bf5a1f849044ef3dedceda3f690a6e1f88d4e440d61883171f75a056ba0521`；087 SHA `2e046efff1ab1826560a4ad310d36222bc075755e8cac41d4b0fd13046f25b44`。同admin账号多角色验收仍不外推为分离账号权限已验。

## 12:54 +08:00 后继真实重提已通过

Root在已核新包`381754e50a21922e3c106b3dd6e69cec663b9ff9a52ae4cdae2e00f43b1b706c`、所属进程6604/48061健康UP后，通过真实页面重新办理原9。`g53-real-ui-r6/008.json`观察准确`POST /dcc/project-product-requests/9/resubmit`后，列表显示新的PENDING_REVIEW和两条旧REJECTED；Root只读佐证新10.previousRequestId=9、旧8/9保持REJECTED。该快照SHA为`e2ce7e0c17f0c8464147789b1e9ffd35b9ce792ec4a8c096394251cb32ccc8c2`。批准驳回→消息→准确原申请→修改→新申请进入审核的动作闭环已实际通过；新10统一待办及正文popup仍由Root补证。

前一次`g53-real-ui-r5/008.json`真实重提失败保留，SHA `87650dc989f20f79a776648452d572646dea490006c6a80c6f7321eabd3d7223`；HTTP200时列表仍为旧REJECTED，不能冒成功。Root运行诊断确认`NoClassDefFoundError: FolderTemplateStructure$Node`：原进程仍运行时target Jar被重新打包替换造成加载损坏；旧9保持REJECTED，新10未创建。Root先停止所属14104再启动已核新包，之后才由用户真实页面再次提交，没有源级fallback、吞异常或API补数据。此失败与r4/087认证过期均留作历史证据，不再覆盖后继r6真实成功。

## 12:58 +08:00 最终页面补证与冻结

Root在同一已核新包下完成余下有限验收，本review读取保护文件核实：r6/010.json实际统一审批中心显示G53-R-20261005的新审核待办、12:52:23及准确项目产品摘要；新10的列表待办入口已验，不再列待补。013/014实际详情显示正式项目`G53文控主流程项目 / G53-P-20261005`，产品仍为`G53文控验证产品 / G53PRODUCT`。015/016返回browser，自然响应记录无三类空ID详情/纸质分发/权限说明GET，pageErrors为空，没有“请求不存在”。

018实际popup viewer显示同文件、A/1和PDF第1/1页，并观察真实预览GET；019及`actual-visible-canvas-proof.json`进一步给出可见canvas 685×969，采样26578、非白2208。因此正文已实际渲染，结论不只来自HTTP200。canvas proof rawSHA `8ca4aeb7adafe44ef91ef524f535256e1bcfd0fb3bbdbb307083781aad1d1e80`；010 SHA `4865c42a77b7354406503cc0274a59334cfa8e4ed115acf58906847df0bb0dbc`；018 SHA `0ac124276f0e3283395218a0b56e8e00ad040ddb3bb8a64a29b6c7c6aa7ffdda`。旧pending段仅为原轮次记录，以此段为最新闭合。

四方向本轮同admin正向、审核驳回和批准驳回→原申请→修改重提→新待办的必要主流程证据齐备。Root报告25源码/测试本地实现提交`ee4d9b4e0136885deb28ac9e9ca83bf68b0026a9`，尚未push，3保护资产保持；本review没有执行或核造Git动作。至此两份G55独立报告冻结，不新增其它审查/测试；完整HTML及分离账号边界仍按四方向矩阵限定。
