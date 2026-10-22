# Verification Report

## 用户追加：E2E用例设计与当前检查点

- e2e-test-cases.md已整理24个业务场景，每个包含Given/When/Then与预期结果，编号01—24唯一连续，结构/UTF-8检查PASS；本次未运行E2E，所有案例状态为设计。
- 最新相邻Maven回执：13类388/388通过；两类fixture最小适配后复跑177项，W08 100/100通过，Native77项仍有3failure/6error。已交原owner继续核对Native，不能把当前后台回归称为全部通过。核心来源绑定84/84及前端247/247为此前已执行且仍有效的不同范围证据，不与重跑中的重叠结果混加。
- 最终前端Vite构建exit0，3239产物/35304144字节，完整2635输入0漂移；完整ts:check已exit0。当前仍在修复验证阶段，未收尾、未执行真实E2E、未提交推送或部署。

## 本轮继续检查中（R2）

用户再次要求继续原六线程。已恢复各线程并限定为已显示旧身份数据的后续动作、失败/暂停恢复、草稿恢复及SDK错误收口等未覆盖分支。当前各线程只对确认新缺陷补实际BDD/RED/GREEN；下方200/类型/构建和C01至C06是前一轮已通过的历史检查点，不冒充本轮最终结果。本轮独立证据使用R2编号，最终交还后由Root统一必要验证。

不扩展E2E、实库/服务/部署或Git操作，不使用冲突旧需求填补缺失条款。

## 本次续办结果

六个原线程本次续修已交还。Root当前实际执行28个不同测试文件共**200/200 PASS**，0fail/skip/cancel；完整`pnpm run ts:check`及最终完整Vite `--mode test`构建均exit0。2635源码/配置输入前后无漂移，49项定向生产/测试输入仍匹配。正式命令、分组、源码哈希和原始失败摘要归档在`continuation-verification-results.json`，完整冻结输入清单保留为`continuation-final-inputs-before.json`。

|本次问题|修复后的行为|验证|
|---|---|---|
|C01 附件卸载下载竞态|旧确认不再发送下载，旧二进制回包不再保存；合法当前下载与取消不变|真实RED后专属5/5、相邻8/8，Root合并组覆盖|
|C02 仅属性编辑后切类型|出现确认；取消/关闭保留原类型和属性；未修改默认值不误报|真实RED后专属7/7、相邻8/8，Root合并组覆盖|
|C03 Office服务与销毁错误|核对SDK真实服务来源；同步/异步销毁失败显式报告，旧源和卸载回执不污染新源|独立实际RED/GREEN，Root查看器94覆盖；旧G09宿主只补来源字段后9/9|
|C04 阅读暂停及会话重放|pagehide持续暂停到pageshow；拒绝已结束start重放，不安装心跳|新增3条RED转GREEN，Root查看器94覆盖|
|C05 详情身份与BPM回跳|实时复核账号、基础/访问租户；旧成功/失败/签名/关系回包失效；回跳保留原wire申请|新增8项及相邻58范围，Root详情78覆盖|
|C06/R05 独立引用弹窗|旧账号/租户分页和预览的成功/失败回包失效；保留固定版本及Long/权限校验|真实RED12FAIL/1PASS后新增15+原5共20PASS，Root详情78覆盖|

草稿线程只读复核DAILY、Office和阅读会话权限链未发现新放行路径，没有重跑无变化套件。本次生产增量仅10个前端文件；相关后端10项指纹与前次正式证据一致，本次未重复Maven，也未把旧125项算作新执行。

Root实际分组为查看器94、附件/属性20、上传取消保护8、详情/引用78，共200；各组测试文件无重复。完整类型采用仓库正式`tsconfig.relaxed.json`脚本，不称strict检查。最终构建于19:41:37至19:46:32实际exit0，3239产物、35299297字节，index SHA256 `816747704749f9d6f8cb933852dac8ee83729d895c6db64078664d0278977c4d`。修改前的首次构建另存历史，不用来覆盖这10个修改后的文件。

Root亲跑旧Office宿主先exit1（来源缺失导致8项不能建立editor），只给注入SDK补匹配的来源字段，原权限、URL、generation、水印与几何断言全部保留；同9项exit0。此为测试适配问题，未冒充新业务RED。实际代码RED分别由各owner原任务记录保留。13个相关代码/经验路径`git diff --check` exit0；换行通知不是差异错误。

本次没有E2E、外部Office/媒体实际页面验收、实库迁移/写入、服务启停、部署或Git新提交推送。最新需求第2项尾部及第6/8项仍未提供，桌面旧文档与当前延迟确认规则冲突，未用旧规则补齐新要求；音频/图片等天然格式边界不以伪工具绕过。软件通过不等于运行系统已经生效。

已先置ready_for_closeout，再实际执行规范cleanup preview/apply，均exit0；逐项核对全部删除目标均在本Root任务目录内，keep6/delete6498、71148832字节。仅删除两次本任务构建产物、临时日志及中间回执，所需退出码/计数/哈希与失败摘要已汇入正式JSON。完整2635输入清单和五份其它正式文档/证据仍在，所有原线程资产与被忽略但需保留的属性测试脚本均保留。最终状态blocked只表示本轮未获Git新提交推送授权，软件缺陷与定向门禁无未决项；没有标成已提交或已部署。

## 前一轮结果（历史）

六个现有线程的本轮软件修复与Root review已完成。实际最终证据：后端125/125、前端181/181，全部0failure/error/skip；完整前端类型检查exit0，2429个源码输入零漂移。基线int_qms/f06833f06，交付包含当前未提交源码，未宣称已部署。

## 六线程结论

|范围|当前结果|
|---|---|
|草稿日常入口|保留列表/版本选项限制；本轮额外修复显式DAILY详情重新带出同链WORKING预览动作。审批、编制及历史上下文不受DAILY过滤误伤。|
|升版多附件|本轮只读复核未发现新缺口，保留检入保存、正式升版冻结、清单确认、重提及历史培训隔离；未重复无变化的旧211/129套件。|
|全格式只读查看|Office改为固定原生逻辑视窗+滚动占位，修复切源/构造失败/旧尺寸回执污染；保留PDF/文本工具、图片视频变换、音频时间轴及各格式水印。|
|阅读时长|已从真正正文ready路径接入start/heartbeat/stop/summary，显示登记时间、首次/最近打开及累计有效秒数；隐藏/失焦/卸载/切版/迟到结果/UUID失败/过期均有行为覆盖。|
|可选登记说明|当前仍为选填，非空最多500字；人工目录维护和严格绑定原因规则不变。本轮无需新生产修改。|
|历史版本精确详情|wire/plain及null上下文严格拒绝；真实loader、详情、阅读请求保持同一精确身份。后端新增明确context的详情投影入口，保留原generic查询合同。|

## 本轮新增缺口与 RED/GREEN

1. 完整Duration门槛：旧逻辑将15秒加1微秒取整为15秒。真实Clock六场景中两条边界在RED实际累计19而应4；恢复完整Duration比较后29项完整服务测试PASS。
2. 阅读接口审批身份：旧query菜单门禁阻止仅review/approve身份。真实方法安全代理四参数均AuthorizationDenied，证明为业务门禁拒绝而非装配失败；四个controlled路径恢复与正文一致的认证门禁，真实Guard/token/租户/任务校验保留，统一存储四路原权限不变。完整Controller12项PASS。
3. DAILY详情投影：真实reader→Query→历史/动作投影的RED中WORKING仍有canPreview=true。传入显式context并仅对DAILY复用既有过滤后，新6项完整用例PASS，合法AUTHORING/APPROVAL_TASK/APPROVAL_HISTORY/VERSION_HISTORY均保留。
4. 详情前端：线程真实RED为混合/null context未拒绝，GREEN新增4项；Root用实际wrapper/loader/reading组合与相邻详情用例统一回归。
5. Office与阅读接线：对应线程记录真实SDK/SFC/generation/几何和正文ready/隐藏/卸载/统计行为RED/GREEN。Root适配旧Office反向缩放尺寸合同后9/9PASS，原只读/URL/不重建实例断言保留。
6. 完整类型检查先因Summary漏processInstanceId字段TS2339失败；核对后端正式字段和真实setter后补可选string/null声明，再次完整检查PASS。实际转译确认新旧运行时代码逐字节相同，没有any或类型断言绕过。

Root对后端RED只临时恢复三份已冻结生产文件的精确旧hunk，11项产生3断言失败、4实际旧权限拒绝，0skip。随后按原始字节SHA恢复，三个文件零差异；没有覆盖并行改动。

## 最终验证命令

后端cwd为IntRuoyiBackend；依赖模块无指定测试时用surefire.failIfNoSpecifiedTests=false，十个目标类均有本次freshXML：

```powershell
mvn -q -pl yudao-module-dcc -am '-Dtest=DccOnlineFilePreviewReadingSessionServiceTest,DccPreviewReadingSessionControllerTest,DccSelectedDailyDetailProjectionTest,DccSelectedDetailReadServiceTest,DccSelectedDetailReadHttpTest,DccDailyBrowserBodyProjectionTest,DccControlledFileBodyReadCurrentSourceTest,DccControlledFileDetailAuthorizationGuardTest,DccControlledFileBodyReadContextTest,DccOnlyOfficePreviewTokenServiceTest' '-Dsurefire.failIfNoSpecifiedTests=false' test
```

结果exit0，125/125。逐类29/12/6/7/3/11/27/19/3/8，0failure/error/skip。报告哈希及真实执行时窗核对在verification-results.json。

前端cwd为IntRuoyiFronted，viewer组82项。真实PDF.js文本提取使用本机已安装的@napi-rs/canvas，通过G09_CANVAS_MODULE指定，不新增依赖、不替换PDF实现：

```powershell
pnpm exec node --test tests/unit/dcc-preview-reading-viewer.test.cjs tests/unit/dcc-preview-reading-api.test.cjs tests/unit/dcc-preview-reading-lifecycle.test.cjs tests/unit/dcc-full-format-viewer-contract.test.cjs tests/unit/dcc-full-format-office-review.test.cjs tests/unit/dcc-full-format-post-reading.test.cjs tests/unit/dcc-g09-viewer-tools.test.cjs tests/unit/dcc-g09-office-readonly.test.cjs tests/unit/dcc-g09-pdf-text.test.cjs tests/unit/dcc-g07-preview-registration.test.cjs scripts/dcc-onlyoffice-readonly-config.test.mjs scripts/dcc-onlyoffice-zoom-controls.test.mjs
```

详情组99项：

```powershell
pnpm exec node --test tests/unit/dcc-detail-reading-context-boundary.test.cjs tests/unit/dcc-detail-exact-read-context.test.cjs tests/unit/dcc-detail-route-read.test.cjs tests/unit/dcc-selected-application-context.test.cjs tests/unit/dcc-content-application-selection.test.cjs tests/unit/dcc-detail-application-round-context.test.cjs tests/unit/dcc-content-detail-split.test.cjs tests/unit/dcc-detail-integration.test.cjs tests/unit/dcc-selected-version-relations.test.cjs tests/unit/dcc-shared-attachments-component.test.cjs tests/unit/dcc-g07-body-read-context.test.cjs tests/unit/dcc-body-read-wrapper-g01.test.cjs
pnpm run ts:check
```

两组181/181、0fail/skip；类型GREEN exit0。类型检查与最终归档2429输入零漂移；阅读10份及详情6份交还源码哈希匹配。相关十份生产文件lint exit0、0error，仅详情两条既有换行warning；tracked diff检查exit0。

## Evidence Boundary

后端为当前源码、真实业务方法/方法安全/Guard及Mockito外部端口，前端为真实SFC/API/helper、Vue renderer与可控DOM/HTTP端口；不是Playwright真实业务验收。没有运行MySQL迁移、外部Office/媒体解码/浏览器像素、多连接数据库并发、服务启停、远端发布、Git提交推送或生产构建。运行环境仍需正式迁移与后续另行授权的运行验收；不能把当前软件通过写成线上已生效。

## Closeout

已归档核心RED/GREEN和稳定性证据，先置ready_for_closeout；规范cleanup preview/apply实际均exit0，keep4/delete28，只清理本Root任务临时日志、输入清单、脚本与RED备份，三报告和verification-results.json保留。六个线程均已结束本轮、源码/tests冻结，最新报告已移除当前待验证状态。未获Git提交推送授权，工程状态按规则记blocked，不标completed；不影响本轮软件验证结论。
