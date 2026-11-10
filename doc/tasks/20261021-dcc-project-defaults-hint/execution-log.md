已核对DEFAULTS_MISSING调用点和实际维护入口；按ponytail最小修改共享错误文案，不改变校验规则。
RED/GREEN：旧文案缺目标市场的断言真实失败；修改共享错误文案后3个定向JUnit方法通过。Maven testCompile因既有system依赖DTO缺失阻塞，改用真实源文件独立javac和直接JUnit方法调用，不伪称Maven通过。测试及args/log保留于output/dcc-project-defaults-hint，未混入任务文档。
cleanup preview/apply PASS：只保留三份任务文档，无待删除项。git diff --check PASS。按当前授权不提交推送、不重启；最终仓库收尾blocked，提示修改已完成。
