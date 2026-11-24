# 目录模板与生产负责人隔离：独立回归结果

2026-11-24，仅本地源码和测试。未访问服务器数据库、未改测试租户数据、未部署。

## 后续干净基线复核（最终补充）

主线程在`D:/ProjectPackage/IntRuoyi/worktrees/group-leader-scope-fix`以测试58运行commit `21f7528`建立隔离工作树，只带本任务15文件，未带主工作区其他未提交修复。本人执行：

`mvn -o -pl yudao-module-mes -am compile '-DskipTests' '-Dmaven.test.skip=true' '-Dstyle.color=never' '-Dfile.encoding=UTF-8'`

27个reactor项目全部编译成功，用时2分00秒，MES编译3270源文件。之前单module依赖旧jar的编译阻塞在真实reactor依赖构建下消除。日志：`workspace/group-leader-scope-clean-reactor.log`。

随后测试使用该干净树25个`target/classes`输出优先的classpath，完全不引主目录编译类；9类首次共149项全部通过（干净基线TreeService旧测试比主目录少2项，不能写151）。日志：`workspace/group-leader-scope-clean-regression.log`。这些结果支持本任务目录负责人污染、继承/发布授权隔离、GROUP不参与授权及FLOW严格拒绝的范围行为；不代表所有SOP序列化行为通过。

额外直接检查显式未绑定SOP字段时，在新建目录回归中补了`containsKey("sopFileUrl")`与值为null断言，随后该类5项中4通过、1失败：`copiedGroupClearsTemplateLeaderBeforeProjectionAndLeavesSourceUntouched`的负责人清空检查通过，但随后SOP字段检查失败。源码中的`source.toJSONString()`会丢弃null字段，干净基线本已有此行为；主工作区另一个未提交修复使用保留null的序列化方法，主线程已明确不带入当前包。直接SOP断言保留在测试中，失败日志`workspace/group-leader-scope-clean-null-sop-regression.log`，未通过放宽断言掩盖。

随后主线程根据实际失败，将同一目录复制/继承路径改为复用仓库已有`MesProRouteVersionSnapshotIdentityWriter.toSnapshotJson`：TreeService创建处2次调用、TreePatch clone/copy处2次调用保留null；未引入新框架或其他发布校验，也未覆盖主目录其他改动。这是本次复制目录数据保真的必要依赖，SOP问题已处理，不再作为未处理缺陷。

本人对干净树TreeService和TreePatch增量javac编译成功，再以该工作树25个reactor输出重编9测试类并全部重跑，**最终149/149通过，失败0、跳过0、终止0**；新建目录测试中的`containsKey("sopFileUrl")`及null直接断言均通过，断言未放宽。日志：`workspace/group-leader-scope-clean-regression-final.log`；JUnit XML在干净树`IntRuoyiBackend/yudao-module-mes/target/group-leader-scope-regression/reports`。先前失败日志保留为修复前证据，不代表当前结果。

最终结论：完整reactor compile通过（随后两类增量编译通过）、149项范围及null保真增强回归全部通过。未执行package产物验收、未启动服务、未改数据库、未发布或部署；主线程反馈正式发布另受Spug缺已登录会话阻塞。

干净树测试脚本：`workspace/test-group-leader-scope-clean-reactor.py`，可按测试类名参数仅运行相应类。已保留结果和实际失败。

## 结果

后端9类151项通过，失败0、错误0、跳过0。初轮149项全通过；随后FlowConfig新增GROUP显式版本归属验证，增量重编该service和对应测试，11项再次通过（替代初轮同类9项），其余已通过项未重复运行。前端8项全部通过。

| 测试类 | 通过数量 | 保护的行为 |
| --- | ---: | --- |
| MesProRouteTreeLeaderScopeRegressionTest | 5 | 创建目录清空复制负责人；继承保留子流程自身授权；未配置子流程保持未配置；父级发布不覆盖或删除子授权；历史输入不被原地修改 |
| MesProRouteTreePatchTest | 23 | 既有目录继承/发布工序差异合并行为 |
| MesProRouteTreeServiceTest | 9 | 既有目录创建、移动、重命名与删除约束 |
| MesProRouteVersionPublishProjectionServiceImplTest | 14 | 既有正式投影行为 |
| MesRouteStartProductionLeaderAuthorizationServiceTest | 21 | GROUP不污染授权，FLOW有效人员/角色授权，错scope或无权限仍拒绝 |
| MesProRouteProductionLeaderNodeTypeTest | 11 | GROUP空GET/禁保存，显式version归属仍校验；未知类型拒绝，FLOW严格校验 |
| MesFrontlineDeviceAccountGroupTemplateTest | 9 | 一线第二消费者与包装分支排除GROUP，坏FLOW和未知类型仍失败 |
| MesProRouteFlowConfigServiceImplTest | 49 | 既有配置服务行为 |
| MesFrontlineDeviceAccountContextServiceTest | 10 | 既有一线设备账号上下文行为 |

前端新增 `route-group-leader-scope.test.cjs` 5项直接执行SFC真实handler：GROUP不发送负责人/负责范围/人员选项/保存请求；missing或unknown节点类型不按FLOW处理；FLOW正常保存且无人/错范围拒绝；正式FLOW禁写；SFC模板可编译且父级传入真实nodeType。既有`route-start-production-leader-id.test.cjs` 3项更新为真实FLOW prop和实际computed定义后通过，继续验证超长ID一致性。

## 验证方式与限制

首次用临时专用POM执行Maven整module编译，因与本任务无关的`MesProAdmissionListQuery.java:81/96`调用本机旧框架jar不存在的`ListFilterQueryUtils.applyMatching`失败，未到测试。未修改该无关代码或放宽检查。

随后只将本次5个生产类与9测试类用javac `--release 17`编译进隔离目录`IntRuoyiBackend/yudao-module-mes/target/group-leader-scope/focused-classes`和`focused-test-classes`，依赖现有module classes和Maven真实依赖，再用Maven Surefire运行。该本机测试依赖图缺Byte Buddy核心，临时POM显式加本机已有`net.bytebuddy:byte-buddy:1.17.8`测试依赖；未改共享POM或源码依赖配置。

**本结果是相关行为回归通过，不是整包构建通过，也不是服务器真实业务E2E通过。** 主线程另在运行服务器commit对应干净工作树中统筹构建验证。

日志：`workspace/group-leader-scope-maven.log`（整模块阻塞）、`group-leader-scope-focused-compile.log`、`group-leader-scope-focused-test.log`（149项）、`group-leader-scope-version-ownership-test.log`（新增后11项）。JUnit XML在隔离目录`surefire-reports`，最新9类合计151项。

## 本代理文件

新增：后端`MesProRouteTreeLeaderScopeRegressionTest.java`；前端`tests/unit/route-group-leader-scope.test.cjs`。

更新：前端`tests/unit/route-start-production-leader-id.test.cjs`只补实际FLOW节点fixture与computed依赖，未改变原断言。

测试辅助为task-only：`workspace/run-group-leader-scope-tests.py`、`workspace/compile-group-leader-scope-focused.py`、模块内`pom.group-leader-scope-tests.xml`。共享POM未改；其他代理生产实现与既有非任务改动均保留。
