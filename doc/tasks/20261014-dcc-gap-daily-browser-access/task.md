# G07 日常入口禁草稿正文

## Current Status
in_progress — Root M09正式接收G07原软件域（Query272、新BodyRead23/Context3/Guard19/Office8/HTTP2及登记相关PASS），G07生产/测试保持冻结；整体M09仍有引用DB2FAIL，三历史关系入口VERSION_HISTORY由G11 A33接线。公共helper保护，Git及工程completed未授，后续仅Root明确转交再改。

## 目标

日常目录/搜索只允许读取正式生效正文；工作稿与待生效正文必须被后端拒绝。编制/检入及审批/历史专门入口继续按精确业务上下文和既有权限读取对应版本。

## 里程碑

- [x] 复核分支、规则、任务分派和源码基线
- [x] 完成正规入口、服务器证明与端到端文件锚点设计；新增 Query 服务 RED 用例（后续已真实执行）
- [x] 真实私有源码 RED 4FAIL，再实现 Query/DetailAuthorizationGuard 正文意图与服务器证明
- [x] GREEN：日常入口拒绝工作稿/待生效，办理入口保留精确读取；Query全270PASS/Guard19PASS/Office8PASS/HTTP2PASS/canonical3PASS
- [x] 当前检查点回归与 review-ready 证据；剩余依赖待Root接入与统一验证

## 预期验证

- 专属 DccControlledFileQueryServiceTest / DccControlledFileDetailAuthorizationGuardTest 定向测试（由 Root 发 Maven 验证票）
- 独立静态/单元证据覆盖四条 BDD，不执行 E2E、服务、数据库写入或 Git。

## 设计约束检查

- C03 已追加接口/正文 Controller 两锚点/Office token/actor/唯一 context 后端范围；M02 期间仍不得写 Java/后端测试。
- FE 授权 bodyReadContext.ts、自有测试、filePreview 的 DCC 正文分支、viewer-navigation 与 BPM dcc-business-identity/index.vue 正文 source；workflow.ts 由 G01 接精确 hunk，G09/G10/G08 父页待归还。版本状态和办理写接口保持。
- 不依赖前端隐藏；租户、正文权限、精确版本身份和办理入口上下文必须由后端验证。
- 不把审批/历史/编制上下文退化为全局放行；未知上下文 fail fast。
- 保持当前分支并行改动，不回滚或格式化无关文件。

## BDD

BDD: M04后新增延迟确认source护栏复核 -> Given 当前正式延迟确认状态、已绑定真实任务轮次和原编制人，When 使用APPROVAL_TASK读metadata/binary，Then 只读该版本sourceFileId或该图纸当前配套PDF，日常入口与错任务拒绝；缺source不得回退original/stamped/current。
BDD: M04后Office实际source复核 -> Given 真实签名Office token已绑定本版本source与context/event，When callback前source引用改变，Then 在业务文件授权和存储读取前明确拒绝；原source未变合法callback仍读精确正文。
BDD: 四出口当前资格复核 -> Given metadata/binary/Office callback/实际DCC infra provider预先合法可读，When 账号停用、任务改派、scope撤销或未核验HTTP忽略租户，Then 当前请求拒绝且零存储正文读取/新viewer token；Office TenantIgnore callback必须先真实验签、使用token租户重新授权并恢复外部context，停用仍零正文。

BDD: 日常目录/搜索拒绝工作稿 -> Given 申请人拥有工作稿且具正文权限，When 以 DAILY_BROWSE 读取正文，Then 后端拒绝正文。
BDD: 合法编制入口保留 -> Given 同一申请人拥有工作稿及正文权限，When 以 EDITOR_WORKFLOW 精确版本读取，Then 允许对应稿。
BDD: 审批精确版本保留 -> Given 审批人具任务授权，When 以 APPROVAL_TASK 精确版本读取，Then 允许任务对应版本。
BDD: 日常拒绝待生效 -> Given 文件处于 READY_TO_PUBLISH/待生效，When 以 DAILY_BROWSE 读取，Then 后端拒绝正文。
BDD: 严格前端正文合同 -> Given 所选文件、raw version 和正规任务轮次，When 构造/序列化/还原正文上下文，Then 保留精确身份并拒绝未知意图、混合字段和跨文件身份。
BDD: 正文 source 透传 -> Given APPROVAL_TASK/HISTORY 正文 source，When 加载 metadata 与 binary，Then 两请求使用同一规范化上下文且失败继续抛出。
BDD: 审批包装页精确历史 -> Given 所选已办 task 与当前 todoTask 不同，When 包装页构造正文 source，Then 只使用所选历史 task 与原版本，不改读当前任务。

## 证据边界

本任务报告只陈述当前源码和本线程私有定向测试事实；只读旧target/dependency编译依赖，当前授予源码重新编译至任务自有输出，未运行 Maven/写target、大构建、E2E、服务或真实库验收。完整限制见verification-report。

## Cleanup Keep

- doc/tasks/20261014-dcc-gap-daily-browser-access/task.md
- doc/tasks/20261014-dcc-gap-daily-browser-access/execution-log.md
- doc/tasks/20261014-dcc-gap-daily-browser-access/verification-report.md
- doc/tasks/20261014-dcc-gap-daily-browser-access/review-ready.md
- doc/tasks/20261014-dcc-gap-daily-browser-access/changed-files.md
- doc/tasks/20261014-dcc-gap-daily-browser-access/integration-notes.md
- doc/tasks/20261014-dcc-gap-daily-browser-access/workflow-wrapper-hunks.md
- doc/tasks/20261014-dcc-gap-daily-browser-access/run-private-tests.ps1
- doc/tasks/20261014-dcc-gap-daily-browser-access/G07DevelopmentTestRunner.java
