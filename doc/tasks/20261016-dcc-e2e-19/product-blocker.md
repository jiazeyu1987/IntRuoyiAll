# E2E-19 前置复核：目录范围已澄清，类别待续验

当前按Root重启指令暂停。配置账号DCC241016C19Config（910403）及本例角色DCC241016C19DirectoryConfig（991290）已经通过真实前端创建并独立登录。菜单授权仅6801/6803/6807，已保存后重开回读；没有admin或审计权限，没有创建项目指派。

## 本例新增真实页面结论

- 原目录913890的名称查询命中1行；本人点击该行“编辑”自然读取913890，表单编码DCC241016C19Storage、名称DCC241016C19存储目录均正确。未再次创建目录，未修改admin现有指派。目录回读前置PASS，不能再作为目录代码缺陷。
- 类别列表仍出现系统异常，但此次类别表单尚未发出保存请求。实际停止点是默认文件分类的级联浮层仍打开，遮挡绑定目录控件；这是脚本交互问题，不能记为schema写入失败或类别保存业务RED。
- 已离线补充Escape关闭级联浮层及隐藏断言。Root要求暂停后未启动浏览器复测，因此修正仅语法检查，尚无GREEN。
- Root五份SQL迁移仍待独立授权；本例没有执行迁移或改后端。恢复后须分别记录类别列表/保存的真实页面结果，不能将公共待办告警混入目录结论。

本轮证据：runs/config-actor-2026-10-16T11-09-44-275Z/result.json；01-minimal-role-readback.png、02-configuration-user-role.png、03-original-directory-found.png、04-original-directory-details.png、error-stop.png；admin-before-password.zip、admin-final.zip、configuration.zip。

配置账号保持本轮已授权的最小权限用于恢复，未在Root停止写入要求后再做停用或其它业务写入。此前三个业务账号仍按上一轮清理状态停用。完整业务fixture仍未就绪，23分支仍未产生业务PASS。

## 2026-10-16 根因更正

Root的directory-scope-inspection.json已只读确认本例目录913890存在且active；admin有4条ACTIVE项目指派。现有实现与测试明确让指派范围优先于目录管理权限，因此admin看不到范围外新目录符合现有规则。E2E-17的root-directory-diagnosis.json进一步区分了目录children/search/tree正常与公共待办/类别缺字段错误。撤回此前“需要修改生产目录可见性”的推断；以下旧RED仅保留实际页面现象，不作为目录代码缺陷结论。

当前动作是创建或复用本例无项目指派的配置账号，使用最小目录维护及类别绑定所需权限复核原目录；不重复创建目录，不修改admin指派、共享权限或后端。类别迁移由Root独立处理，本例不执行SQL。新的真实页面结果随后更新。

这是本轮真实页面复核结果，未修改生产代码。授权及脚本不再被prepare_only或共享fixture=false限制。

## BDD

Given 配置账号在已确认测试租户通过真实“文控中心 -> 文档目录 -> 新建目录”填写本例编码DCC241016C19Storage、名称DCC241016C19存储目录，保存后新增弹框关闭；本例P1/P2项目及目录模板已由独立审核账号完成正式审批。

When 同一配置身份在目录维护页按完整名称查询，再从“文控权限 -> 类别列表 -> 新增类别 -> 绑定目录”打开最新目录树。

Then 预期可以找到本例目录并为本例专用类别选择绑定，继续准备三份可辨识受控正文。

## RED

- 实际 `node doc/tasks/20261016-dcc-e2e-19/prepare-file-config.cjs` 在选择绑定目录时停止；T1类别尚未保存，T2类别尚未创建，没有以其它共享目录补齐。
- 独立复核 `node doc/tasks/20261016-dcc-e2e-19/diagnose-directory.cjs` 实际输出 `DIRECTORY_NOT_VISIBLE`。
- 目录维护：本例完整名称查询后目标可见行0，并出现“系统异常”。这一错误空表不能证明数据库无记录。
- 类别绑定树：已实际展开；可见树中没有本例前缀，精确名称定位0。未用隐藏值、组件实例、API或SQL补选。
- 诊断只读范围内写请求0；真实目录创建发生在此前独立准备脚本，不能把诊断的0写请求当作本轮全程零写入。

证据：runs/file-config-2026-10-16T02-44-11-356Z/result.json、error-stop.png、trace.zip；runs/directory-diagnostic-2026-10-16T03-04-31-595Z/result.json、01-maintenance-name-search.png、02-binding-directory-tree.png、trace.zip。

## 源码核对与边界

- `IntRuoyiFronted/src/views/dcc/controlled-file/categories/index.vue` 的openForm在打开类别表单前调用正式getDirectoryTree；不是沿用旧任务缓存来认定缺目录。
- `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/directory/DccDirectoryAdminServiceImpl.java` 的getDirectoryTree先处理有效项目分配目录，再判断目录管理权限。该顺序是待Root复核的静态线索，尚未通过数据库、接口或业务页面证明此次根因就是该分支。
- 不修改现有账号权限、既有项目分配、共享角色、共享矩阵或目录过滤来掩盖问题。未把新建弹框关闭当作目录最终可见/可绑定成功。

## GREEN / REGRESSION

NOT_RUN：本线程仅维护测试资产，生产修复不在授权范围。Root修复后应先让本例目录在真实维护页和绑定树中可见，再恢复本例账号、补齐类别/查看规则/审批签名和三份ACTIVE正文，最后运行23个原业务分支。不可仅凭health UP或节点源码存在关闭此阻塞。

## 保留及清理

三个本例新账号已通过真实页面停用并刷新确认；P1/P2已完成项目、目录模板、专用分类树、两专用查询角色/分类和当前不可见目录保留用于Root review。账号停用不等于业务fixture全部删除；完整cleanupCompleted=false。保护的本机凭据缓存仅用于后续正式页面恢复，不属于提交或共享证据。
