# 新一轮 actor / tenant / late response 复核

## Baseline And Scope

int_qms / f06833f06；上轮125后端、181前端及完整types仅为上轮证据。本轮未运行这些套件，未写生产代码直到Root解除2635输入Vite构建冻结；后续仅改本线程owner详情区和专属测试。

## Actual RED

命令：`node --test tests/unit/dcc-detail-authority-late-response.test.cjs`。

初始4项为3FAIL/1PASS；追加实际History/Relations SFC和BPM回跳后7项为6FAIL/1PASS。均为实际输出断言失败，没有缺依赖/命令/fixture错误。

- 同用户/同大Long文件/同路由，访问租户缓存31→32后，旧详情成功回包实际发布old-tenant.pdf。
- 同条件旧失败回包实际触发access查询及错误通知。
- 同文件/路由/分页的旧签名请求实际发布一条旧签名。
- History实际SFC接受旧租户申请证据并emit验证选择；Relations实际SFC发布tenant31的selectedSource。
- wire选择old-obsolete-round/old-task，实际openBpmDetail却跳native-upload-round，丢失申请来源。
- 相邻控制：真实同步actor watcher在actor变化时使旧请求失效，新actor详情保持，PASS。

## Source Diagnosis

- auth.ts的getTenantId/getVisitTenantId读取WebStorageCache，不是Vue响应式状态；TenantVisit先setVisitTenantId，再调用tagsView.refreshPage。租户已变化、旧实例尚未完成刷新/卸载时，旧请求仍可能返回。
- index.vue的isCurrentDetailLoad原只看sequence/route/file；签名列表自有守卫也未实时比较请求actor/tenant。单靠包含缓存getter的watch不足以阻断该窗口。
- History原只看generation/selectionGeneration；Relations原只看generation/readSequence；相邻BrowseRecords contextKey是computed缓存，亦须在每次await后的写状态前读取真实请求身份。
- openBpmDetail原未解析bodyReadContext，只有明文processInstanceId/taskId与native file BPM分支。

## Planned Minimal GREEN

详情reload记录发起时actor+基础tenant+访问tenant快照，公共回包守卫实时比较；签名分页自有请求同样捕获并比较。History/Relations/BrowseRecords使用真实tenant读取与父级readAuthority函数比较，不用当前版本补齐或重新定位。

精确wire/Long/rawversion/BPM/task保持原合同；BPM回跳复用既有严格解析器。附件组件归附件线程，不改；DccReferenceUsageDialog宿主归其他owner，不改，其父级引用metadata加载在BrowseRecords内受同样实时身份守卫保护。若子对话框自身还需扩展独立身份门禁，交Root分配，不能借本轮授权写他owner源码。

本轮不涉及服务端授权变化，不运行Maven/shared target/fulltypes/build/E2E/DB/服务/Git提交推送；最终新GREEN与回归结果另记，不将上轮数字写为本次执行。

## GREEN And Targeted Regression

- index.vue新增readDetailAuthority（actor+基础tenant+访问tenant），reload记录快照，isCurrentDetailLoad实时比较。因此详情/失败/权限解释/审批/打印回包沿原守卫失效；签名分页也单独捕获并实时比较。
- 父页向History/Relations/BrowseRecords传readAuthority读取函数；History在rounds/evidence两层、Relations在metadata/snapshot/evidence/preview各await边界、BrowseRecords在metadata/reference摘要写状态前实时比较。readAuthority是额外只读上下文门禁，不授予业务权限；组件原有服务端授权/精确版本合同保留。
- openBpmDetail改用严格resolveDetailReadSelection，wire旧BPM/task不再被native上传轮次替代。
- 新增专属Node最终8/8PASS、0skip。受影响7文件一次58/58PASS、0skip；原route-read宿主补载实际authority声明，partial-template宿主补载实际父级getter，不修改断言或其他owner测试宿主。
- Browse摘要测试起初用plain props导致computed不会形成真实缓存，修正为实际Vue reactive props/route后先复现旧tenant referenceProjectCount=9的真实RED，再GREEN；signature测试一次重复VM声明是测试装配错误，移除重复后正常，不当作产品RED。
- 四个改动生产Vue定向eslint exit0/error0，仅index.vue原2条换行warning。没有运行全量types/构建，由Root按最终修改选择集成验证。

## Cross-owner R05 — historical static discovery (now authorized and closed below)

只读位置：`IntRuoyiFronted/src/views/dcc/controlled-file/relations/DccReferenceUsageDialog.vue` 的load与openPreview。当前只比较sequence/contextGeneration/props.source/context-key；未读取请求时actor/当前raw tenant。父级BrowseRecords的computed contextKey虽包含tenantgetter，但缓存本身并非响应式，不能保证缓存租户写入后、页面刷新/卸载结束前使该子对话框generation立即变化。

因此已打开对话框自己的引用分页/权限回包仍可能在同源props不变时接受T1旧回包；这一点为源码推导，本轮没有执行该其他owner组件行为RED，也没有修改其生产或测试宿主。父级摘要load已修复，不能据此宣称对话框自身缺口已关闭。

建议Root转该owner：为引用对话框接入同一readAuthority函数或自行捕获真实actor/tenant，在引用page/preview的成功与失败写状态前比较；沿用fixed selectedControlledFileId与Long校验，不按当前版重取。共享组件归属未授权，本线程停止于integration note。

## R05 追加授权与实际 RED（当前推进）

Root已将DccReferenceUsageDialog.vue和新增专属测试唯一交本线程，上述“待owner/静态”段落为发现时历史记录，不再是归属阻塞。计划由Dialog独立读取auth/userStore，避免修改已冻结四详情Vue或browser区域，两个现有父入口均由同一组件内的实时actor/baseTenant/visitTenant快照保护。

BDD已记录到task.md。实际命令 `node --test tests/unit/dcc-reference-usage-authority.test.cjs` -> 13项1PASS/12FAIL，0skip；全部失败均为业务断言：actor/baseTenant/visitTenant三种变化×load/preview两动作×成功/失败两结果。props.source/context-key不变；旧page实际发布、旧预览实际window.open、旧失败实际写入error。身份不变的fixed Long/raw版canonical wire与真实权限拒绝控制项PASS。真实SFC脚本被执行，仅HTTP/auth/userStore端口为测试替身，不是页面E2E。

## R05 GREEN / REGRESSION — owner status closed

- Dialog独立使用useUserStore与auth cache getter，readReferenceAuthority与详情相同，记录actor/baseTenant/visitTenant三个原始值。load/preview分别捕获请求时key，成功、失败和finally写状态前实时比较，加上原sequence/contextGeneration/modelValue。
- 未新增父级props、未修改两个父入口或browser其他区域，四个先前冻结详情Vue的UTF-8/LF SHA全部与authority-handoff.json一致。Root原“R05待owner/静态未验证”状态已解除，当前为本线程已授权实际修复。
- 原fixed selectedControlledFileId、保存行优先、referenceIdentity安全Long、精确metadata权限、VERSION_HISTORY raw版wire、关闭/切版/卸载旧请求失效的逻辑未放宽。身份不变的真实page/preview网络失败仍显示，非法Long在metadata请求前拒绝。
- GREEN初次：新增13+旧5共18/18PASS；追加当前错误可见与损失精度Long拒绝两项后，最终两文件20/20PASS、0fail/skip。没有重复58全集。
- 最终命令：`node --test tests/unit/dcc-reference-usage-authority.test.cjs tests/unit/dcc-reference-usage-dialog.test.cjs`。旧Dialog test仅为新auth/userStore依赖补同身份端口，未删改原断言。
- `pnpm exec eslint src/views/dcc/controlled-file/relations/DccReferenceUsageDialog.vue` -> exit0，0error/0warning。没有运行Maven/shared target/types/build/E2E/实库/Git。
- 最终生产/两测试指纹及冻结声明见r05-handoff.json。运行态/全项目类型/构建是否需要追加，由Root依据新输入决定，不能复用上一轮2635输入构建冒称本次验证。
