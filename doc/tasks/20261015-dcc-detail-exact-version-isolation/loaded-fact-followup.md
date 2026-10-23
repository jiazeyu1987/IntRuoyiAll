# L01..L03 已加载事实后的动作边界

## Scope And Baseline

int_qms/f06833f06。Root上一轮200/200、types/build及已关闭C01-C06作为历史基线，不重开原问题、不冒充本轮执行。当前新增触发条件是**旧身份已经成功加载数据后**再切身份并发起下一动作；不同于前轮“请求在途身份改变”的回包守卫。

沿用本任务目录，仅修改owner详情/引用源码与专属tests。不推断缺失原文第2尾/第6/8业务规则，不写附件组件、viewer/reading、其他owner宿主或Java/Query。

## Actual RED

- `node --test --test-name-pattern='loaded R05|close clears current|unmounted R05' tests/unit/dcc-reference-usage-authority.test.cjs` -> 5FAIL：已加载后actor/baseTenant/visitTenant变化仍发metadata；close遗留old prompt；unmount后捕获的load仍发page请求。
- `node --test --test-name-pattern='loaded history rounds|loaded fixed relations' tests/unit/dcc-detail-authority-late-response.test.cjs` -> 2FAIL：History选择和Relations目标预览沿用旧loaded集合发新请求。
- `node --test --test-name-pattern='loaded detail cached' tests/unit/dcc-detail-authority-late-response.test.cjs` -> 实际cached-current已prime且rawTenant改变，四种导航/预览仍执行（4 != 0）；同用例进一步确认旧审批弹框会打开。之后补主详情legacy附件/工作浏览等入口，同样实际RED，未改附件组件。
- `node --test --test-name-pattern='loaded historical controlled-browser' tests/unit/dcc-detail-authority-late-response.test.cjs` -> 实际wire丢失为DAILY，原APPROVAL_HISTORY的raw A/1、OBSOLETE BPM/task丢失。该入口与前轮openPreview/正文→详情不同，是未覆盖的受控浏览portal。
- 清空旧page/rounds/relations的额外断言先实际RED，再修复；不是仅添加字符串断言。所有RED均为实际SFC/handler业务输出断言，不是编译或依赖装配失败。

## Minimal GREEN

- Dialog：成功page绑定loadedAuthority；preview请求前比较loaded scope与当前actor/baseTenant/visitTenant，错误时清旧page并明确要求刷新。disposed/closed门禁阻止关闭/卸载后捕获的刷新句柄发请求，close清旧提示，显式重开可加载原固定版本。
- History：正式轮次映射绑定加载身份，selectRound在API前比较，变化即清rounds/evidence/key并emit null。Relations：完整固定集合/未记录证据绑定加载身份，预览前比较，变化即清旧集合/来源，保留服务端精确目标权限复核。
- index：新增ensureCurrentDetailContext，在读入口每次实时调用已有isCurrentDetailLoad，不以cached computed布尔值作为操作许可。正文、历史、BPM、trace详情、工作浏览、legacy附件预览等只允许当前已加载身份。审批仅对openActionDialog与actionDialogRequestIsCurrent复用既有上下文门禁，未改节点、签名、payload或任何业务授权规则。
- controlled-browser portal沿用resolveDetailBodyReadContext与canonical wire，不自动DAILY或当前版本。
- 合法显式refresh、新actor同固定版重新获权、close→reopen、真实当前权限拒绝/网络错误、Long精度拒绝与原raw版本/wire不放宽。

## Actual GREEN / Regression

最终命令：

```powershell
node --test tests/unit/dcc-reference-usage-authority.test.cjs tests/unit/dcc-reference-usage-dialog.test.cjs tests/unit/dcc-detail-authority-late-response.test.cjs tests/unit/dcc-selected-application-context.test.cjs tests/unit/dcc-selected-version-relations.test.cjs tests/unit/dcc-content-detail-split.test.cjs
```

exit0，6文件72/72PASS、0fail/skip/abort。包含本轮新增11项（9个负向缺口用例及2个刷新/重开正向控制）；不与前轮78/200或8/58/20混加。过程中69/71是中间计数，不作额外通过总数。

定向ESLint四个改动生产Vue -> exit0/0error，仅index.vue两条既有属性换行warning。

仅自有partial-template测试漏载新ensureCurrentDetailContext时有3条ReferenceError，随后补载实际声明/当前context端口，原断言不改；该装配失败不作产品RED。没有更改其他owner测试宿主。

## Integration Notes / Untouched Areas

- 本轮不执行Maven/sharedtarget/fulltypes/build/E2E/实库/迁移/服务/remote/Git提交推送，亦未运行78/200全集；Root按最终新输入选择集成验证。
- Root如运行其partial-handler宿主（例如独立viewer-trace-entry、working-browser-navigation或approval-dialog-context），应加载新增ensureCurrentDetailContext及原isCurrentDetailLoad的真实声明或明确当前context端口。本线程未修改这些其他owner宿主，不能用缺fixture helper错误判成产品授权回归。
- 附件组件`ControlledFileAttachments.vue`只读：当前downloadFingerprint在**点击时**捕获actor/tenant，能阻断点击后的变化；但没有明确绑定props.file事实最初加载身份。已加载旧canDownload后切身份再点击，仍可能按新fingerprint进入confirm/请求；preview亦只校文件归属。这是本轮源码推导，未执行该组件新RED，不是已关闭C05在途下载问题重开；交Root/现有附件owner核定，本线程不写其生产/tests。主详情legacy附件入口的实时门禁已在本owner内修复，但不代表独立附件组件已覆盖。
- viewer/reading保持其owner归属，未修改。所有后续内容仍由既有服务端精确版本/申请/任务授权决定，本轮没有用UI门禁代替服务器检查。

## Handoff

修改生产：index.vue、DetailApplicationHistory.vue、DetailRelationsPanel.vue、DccReferenceUsageDialog.vue；BrowseRecords与detail-read-context.ts未改。测试：dcc-detail-authority-late-response、dcc-reference-usage-authority、dcc-content-detail-split。最终raw与UTF-8/LF SHA256、命令和冻结声明见loaded-fact-handoff.json。
