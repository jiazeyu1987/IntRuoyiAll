# Execution Log: DCC 上传目录下钻到叶子目录

BDD: 上传时必须选到叶子目录 -> Given 文件类别已经绑定到包含多层子目录的目录树根节点 / When 用户打开受控文件上传页并继续选择目录 / Then 系统必须要求用户一直选到最后一层叶子目录后才能提交。

BDD: 浏览页汇总子孙目录文件 -> Given 用户在目录浏览页选择某个父目录 / When 查询受控文件列表 / Then 页面应显示该目录及其子孙目录下的受控文件，而不是只显示当前目录本身。

RED: `mvn -pl yudao-module-dcc -am test "-Dtest=DccControlledFileWorkflowServiceImplTest,DccControlledFileQueryServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false"` -> FAIL，测试编译阶段缺少 `directoryId`、`includeDescendantDirectories`、上传目录树 VO 与服务方法。

RED: `mvn -pl yudao-module-dcc -am test "-Dtest=DccControlledFileWorkflowServiceImplTest,DccControlledFileQueryServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false"` -> FAIL，首轮实现后根目录分组逻辑对 `null parentId` 处理错误，且目录管理用户浏览仍被类别 `VIEW` 权限误伤。

GREEN: `mvn -pl yudao-module-dcc -am test "-Dtest=DccControlledFileWorkflowServiceImplTest,DccControlledFileQueryServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false"` -> PASS

GREEN: `mvn -pl yudao-server -am -DskipTests package` -> PASS

GREEN: `D:\ProjectPackage\Int\IntRuoyi\restart-ruoyi.bat` -> PASS
