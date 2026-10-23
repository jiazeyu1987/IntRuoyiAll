# Root后续脚本Review

以下为静态审查要求，均已交回对应原线程；不作为业务E2E FAIL或PASS。

## E2E-10

Root详细review E2E-10返修：login(page,usernameEnv,passwordEnv,displayNameEnv)所有调用未传displayNameEnv，requiredEnv(undefined)执行必失败；请修身份断言输入。execute分支缺runtimeVerified/roleFixtureVerified强校验。公共矩阵操作当前取editor第一个input追加prefix，既非明确业务字段也可能改共享基线，须精确本例任务类型/规则的页面可恢复配置，不允许随意改第一个主体。C10-C仅查看原矩阵未实际重提，不可称场景完成；D/E明确BLOCKED应独立记录而非BLOCKED_OR_FAIL。各子例结果和清理恢复都持久JSON并保留原错误，trace登录/签名不含秘密。保持prepare_only，不做真实配置写入。

## E2E-22

Root详细review E2E-22返修：execute只检查gate，未检查runtimeVerified/roleFixtureVerified/backendPort=48061；fixture.versions允许仅1版，字段expected空值直接skip可造成缺信息也PASS，需至少2个可区分版本并验证所列必需字段/3实际属性/各时间/状态/编制人/关联引用精确归属；不是ownerName替代编制人。当前关联引用仅detailPages[0]，未逐版隔离，没打开关联固定正文核对所选版本，也没覆盖原提交/后补/整改；缺数据分别BLOCKED。无trace截图/结果文件，stdout PASS不足review；登录waitmain.catch空吞异常须移除并准确归因。只改本例资产、不改生产，保持prepare_only。

## E2E-12

Root补充review E2E-12：context.tracing.stop(...).catch(()=>null)吞证据失败，需记录traceError并阻止完整PASS；trace在login或签名口令填写前启动有泄密风险，须登录后开始、签名段暂停或正式脱敏。跨部门去重和实际部门负责人/会签人区别用精确任务绑定。准备阶段保持prepare_only。

## E2E-14

Root补充review E2E-14：tracing.stop catch undefined吞错误，需报告证据采集失败/保留原错误，不能EXECUTED_REVIEW_REQUIRED exit0掩盖收口失败；登录/签名trace须秘密保护。取消后需同任务仍未签，成功需实际文控签名人与时间、生效模式和实际时间对应，记录本例残留不要空数组。保持prepare_only。

## E2E-18

Root补充review E2E-18：trace stop catch undefined会吞证据失败，请保留traceError/cleanupError并阻止完整成功。至少2版本正文与附件/培训清单可区别，明确字段不能只比列表行/画布存在；登录签名trace保护秘密，保留本例已提交对象残留。保持prepare_only。

## E2E-05

Rootreview E2E-05返修：确认保存/确认提交点击后catch undefined吞异常，后续仅现有snapshot仍可见就记提交PASS，可能从未正式提交。必须精确二次确认按钮及新的主管待办/申请状态，并从详情正式入口重新打开草稿验证实际保存值，不能只首次跳转。公共矩阵不变需要提交前后同一类型配置真实页面对照；新申请私有人员也应全部不继承，不只会签。trace当前开始于login之前会记密码，请登录后开始并保护签名段。保留真实申请残留和清理，不以空residual+PASS结案。当前prepare_only；不改生产/真实数据。

## E2E-06

Rootreview E2E-06返修：页面等待加载/networkidle/项目加载catch undefined吞错误，需具体就绪/错误DOM判据；跨项目残留提交若页面不可制造须独立BLOCKED，不能整体PASS；需要通过详情确认关联固定版本持久化，当前只选中tag不够。trace应登录后启动，网络只记录path/method/status防token/query泄露；真实上传有临时件残留必须清理或准确记录。runtimeVerified/roleFixtureVerified统一强校验，不依赖自造status字面量。只改本例脚本，保持prepare_only。

## E2E-08

Rootreview E2E-08补充：确认脚本trace在登录后开始，不能将填入密码记录进trace；执行里cleanupAttempted/Completed始终false且没有残留标识，实际源件上传会残留，应通过页面清理或逐项记录。项目切换第二层取消、仅属性手改、会签/下发分别手改、关联清空须各有断言，不只总流程PASS。运行门禁按统一runtimeVerified/roleFixtureVerified+精确端口，准备中不执行。

## E2E-21

Rootreview E2E-21返修：三类入口expectedBodyMarker为可选空值，可只见canvas就声称固定正文；必须每类有真实可区分正文标记，并在已存在不同当前版的前提下断言所选旧内容、未取当前内容。现有trace均在login前开始会记录密码，移至登录后；每分支结果/trace/截图应持久文件，不仅stdout，失败也应finally保存trace/关闭context。入口拒绝分支、草稿纯标签不暴露minor及固定状态逐项记录。保持prepare_only。

## E2E-23

Rootreview E2E-23返修：noAccess actor返回history.status=BLOCKED，但actor.status及最终全局status=PASS，属于覆盖假阳性，需按每个计划子例聚合任一BLOCKED→整体BLOCKED非0。实际下载从未点确认、未获取download/save/格式验证，不能验证下载权限；取消/连点/离开应各记录与零download事实，正例需真实下载文件及内容。在线查看仅弹框名不算附件正文渲染，培训记录还需对应版本/原上传人/时间。trace当前login前启动会记录密码，需保护；search等待空表吞异常等移除，明确空表正常判据。新旧目标精确编号+版本不能选first混合。仅本例资产prepare_only。

## E2E-17

Rootreview E2E-17补充：当前所有收件人context trace启动后才login/签名操作，会把账号密码/签名密码写进trace。须登录后开始、签名输入前暂停或正式脱敏并验证无秘密/认证资源；保存每人实际文件编号/版本、签名人/签名时间和未签收追踪，不仅Toast。清理对象残留应明确业务实体，不能把browser.close当业务清理。门禁保持prepare_only。

## E2E-02

检查负例只破坏目标字段，其它输入仍有效；选择缺人员项目后必须补回被项目切换清空的文件/类型/文件夹，否则证明不了人员门禁。搜索词清空不等于选中值清空，页面不可达状态保持BLOCKED。消除确认吞异常、登录trace凭据及上传临时件假清理。

## 未放行业务执行

E2E-01/04/07/09/19的初步脚本覆盖和状态边界已有核对，但仍需当前页面验证、fixture及真实trace复核，暂不签发整例PASS。24例主gate保持prepare_only。E2E-19另获read_only_preflight，用管理员做页面前置盘点，不能替代双账号业务验收。
