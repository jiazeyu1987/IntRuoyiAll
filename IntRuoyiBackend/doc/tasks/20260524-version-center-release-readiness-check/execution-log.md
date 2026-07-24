# 执行日志：版本中心合并后发布就绪性核查

## 2026-05-24

- BDD: 发布前核查 -> Given 版本中心已合入后端 `int_main` / When 核对工作树、验证结果和发布前置条件 / Then 明确输出是否可直接发布及阻塞项
- GREEN: `cmd /c script\deploy\show-int-ruoyi-test-status.bat` -> PASS，测试服 runtime/container/health 正常
- GREEN: `cmd /c script\deploy\show-int-ruoyi-prod-status.bat` -> PASS，正式服 runtime/container/health 正常
- GREEN: `python -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py -q` -> PASS（18 tests）
- GREEN: `mvn -pl yudao-module-showroom -DskipTests compile` -> PASS
- GREEN: `mvn -f pom.xml -pl yudao-server -am -DskipTests package` -> PASS
