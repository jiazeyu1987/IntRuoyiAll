BDD: 双共享目录与文件进入同一成功快照 -> Given 质量体系文件 与 生产部 两个共享都可遍历 / When 运行刷新任务 / Then 成功快照必须同时包含两个共享根下的目录和文件。
BDD: 生产部共享中不可读目录仅跳过该目录 -> Given 生产部 共享存在 access denied 子目录且用户已批准 readable-only 范围 / When 刷新继续遍历 / Then 仅跳过不可读子目录并保留其它共享内容。
BDD: 状态接口展示双共享范围 -> Given 当前租户查询 NAS定位 状态 / When 返回状态对象 / Then `scopeShare` 必须展示双共享而非旧单共享字符串。
RED: `mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-srm -am "-Dtest=SrmNasLocatorServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，新增双共享测试、下载路径反解测试与状态范围断言在旧实现上不通过。
GREEN: `mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-srm -am "-Dtest=SrmNasLocatorServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS
RED: `真实 NAS定位 刷新复验` -> FAIL，上一轮后台刷新在后端重启后遗留 `RUNNING` 任务，后续刷新被并发门禁拦截。
GREEN: `stale RUNNING 任务自动收口回归` -> PASS，真实数据库记录 `id=11` 已在下一次刷新触发时被更新为 `FAILED / stale running task: previous refresh did not finish before timeout`。
