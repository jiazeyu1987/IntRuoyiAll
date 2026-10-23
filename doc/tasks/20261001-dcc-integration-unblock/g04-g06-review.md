# G04–G06 整合审查记录

2026-10-03。沿用主任务。当前仍是独立整合 worktree，未本地提交、未合入int_qms、未执行实库迁移或真实Playwright业务动作。此记录不把离线通过作为全部目标完成。

## 本轮已验证改动

### 会签部门实际预检与上传控件

Root新增 `DccSelectedDepartmentReadinessTest` 与 `DccRouteDepartmentPreviewHttpTest`：首轮4项中2失败/1错误，证明HTTP丢部门字段、先解析已移除默认部门导致有效替代部门被拒。保留 `g04-department-preview-red.log`。

后端修复：route-preview接受独立selectedSignoffDepartmentIds；五参数公共service保留用户与部门语义分开；显式部门集在resolver读取默认部门leader之前替换，只修改会签节点。上传/升版正式evaluateDepartments及作废resolveRoute使用同一选择规则；null用正式默认，空/重复/非法集拒绝。后端6类先204后206次回归通过，真实Controller→Workflow→resolver组合、旧legacy预览、作废两节点/岗位检查保留。

上传新增可编辑部门控件与目录读，实际preview返回的默认集带入；选择/类别变化重预检；旧响应不标新申请ready；确认冻结精确Long和实际集。先1/6通过、5失败的父handler行为RED，GREEN后结合既有上传29项通过。正式提交wrapper另加非空/唯一/精确部门ID验证，有独立RED→GREEN。原父页5失败来自新增真实依赖未配置夹具，已补实际helper和合法部门/预检上下文，不用空值绕守卫。

### 真实申请轮次与关系能力读接口

新增只读application-rounds和relation-permissions接口，分别验证当前详情/名称可见权限；普通权限拒绝返回false能力，仅明确访问拒绝转换，基础设施异常传播。关系集当前source必须同Master；历史快照不回写。

先3项失败：缺读API与跨Master当前source未拒绝；其中申请轮次夹具漏建映射表是测试设置失败，修正后记录 `g04-history-relations-read-red-refined.log`，两个真实接口缺失和source守卫缺口成立。随后4类35次全部通过。没有新增DB写入型API或DDL。

### 子 Agent 后端 Review 扩展

backend_closure正式续跑后复核并修复：较早本人工作正文的属性只读需准确Master/project/type/number/hard scope；已不可送审仍可读原始CE/FDA。作废正式创建流程补真正岗位/权限/签名授权/图片就绪守卫。部门列表序列化精确字符串。历史申请轮次核验真实BPM的定义/租户/业务键/申请人或独立作废FormCenter对象，而非只信映射行。

发现OWNER可换版非本人稿的Query/读取/公共提交合同不一致，新增精确replacement上下文读，显式REPLACEMENT生成独立OWNER申请，原稿本人保存/检出限制保留；各历史行返回实际processInstanceId。其正式交付日志 `g04-p10-p13-final-regression.log` **731次/19类，零失败/错误/跳过**，主应用compile通过。此数含继承回归，不与各批历史数字累加成独立场景。

该交付随后进入P14/P15继续修复，G04资产指纹仅证明当时批次，当前共享文件可能变化；最终收口须对最新交付重新核对，不假称旧hash证明最终源码。

### 公共详情与上传/工作台

detail_closure交付当前关联/历史快照、真实申请轮次选择/本轮签名、尝试号/前驱/各BPM及所选工作稿CE/FDA读；20项详情、18项升版、23项关系Vue运行时与lint通过。与后端新replacement/轻量关系metadata合同还有进一步接线，已正式续跑，不能将初次ready报告当最终闭环。

upload_closure G05交付完整逐项确认汇总、MATRIX_APPROVAL真实启用账号展示、取消零提交/上下文变化拒绝、受控版本与检入/正式变更文案、移除默认今天及“允许历史补录”假承诺：**42项定向测试**，3文件lint0/0。

G06交付公共工作台PendingWorkflowDistributionList挂载，读取正式日期/提醒排序，按当前账号真实doc_control与下发权限显示；缺配置/请求失败可见，不用旧PENDING_MANUAL_DISTRIBUTION查询伪造当前结果；39项工作台/工作流回归、3文件lint0/0和旧静态合同通过。实际路由守卫仍阻断其跳转，在Root Review中修复：

- `g06-management-route-red.log`：真实Vue Router memory history与实际remaining.ts guard拒绝两正式来源。
- `g06-management-route-green.log`：3项路由行为通过，工作台mode=manage和project-browser management=1均进入准确Long详情；旧viewer/approval/trace/browser及未知来源拒绝保持。
- `g06-route-lint.log`：完整remaining.ts ESLint exit0。

Root组合验证 `g06-root-combined-ui.log`：9文件**71项全部通过**；项目正式types `g06-root-types.log`，进程42422 exit0；env.local构建 `g06-root-build-local.log`，进程35399 exit0、Build successful。构建覆盖G06当时前端源码，后续detail/P14/P15接线有变须另做最终检查，不能偷换最终范围。

## 当前真正待收口

1. 后端P14/P15已正式FINAL交付。Root重读9资产hash全部一致；474主回归/21最终HTTP/H2均BUILD SUCCESS且零失败，补充21含重叠，不当作独立新增场景。实际Multipart全名早期预检/精确own-draft replay和轻量metadata已完成；首次组合fixture漏source/original列以及零ticket insert的失败证据保留，正式submit最终占名/并发守卫不变。
2. detail Owner第二次正式FINAL交付：按REPLACEMENT intent读取上下文、不写他人原稿；关系Parent使用轻量正式投影、只读无目录请求、当前失败保留独立历史。其26详情/18升版/23关系回归通过。Root最新10文件组合85项全部通过；最终types/build复合进程30916已核实exit0，原项目types和env.local构建均通过，日志Build successful；没有放宽类型。
3. 自动生效多版本链和Job实际注册/运行状态、默认latest浏览/状态筛选、引用橙色及全部原12项需求继续按 `goal-acceptance-matrix.md` 做审计，不缩小到已有绿测试。
4. 本机环境只读复查仍无Docker/MySQL23306/Redis26379及整合端口，共享依赖恢复授权问题待答。迁移16项/依赖42项已有静态包，实库schema/既有迁移/候选policy/时区提醒等未执行或核实，真实UI仍未通过。
5. 最终Git纳入范围及无关改动保护见 `goal-git-scope.md`；主目录AGENTS和infra字节已冻结保护，不纳入DCC提交。

所有任务仍以完整目标为完成标准。三子 Agent 的实际运行/终态必须用正式collaboration接口和交付证据核实，不按mtime推定，也不以写反馈文件冒充消息送达。

运行复验：主目录和整合树当前HEAD同为a801dc8b91579241e221d129ab34343997673f40；没有将验证中间代码带入主目录。共享环境恢复权限问题仍待原答复，另已提出时区/提醒提前天数/渠道的业务配置问题；等待答复期间保留可独立开发与Review，不标整个目标blocked。后续多待生效版本切换、默认latest/状态浏览、实际文件名橙色等审查仍待源码/行为验证，不能用本轮全绿替代。
