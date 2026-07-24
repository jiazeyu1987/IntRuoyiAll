# 执行日志：测试服发布后端堆内存防回归

BDD: 测试服发布保留 2G 后端堆 -> Given 运维人员通过脚本或运行控制台发布测试服, When 脚本生成测试服 `.env`, Then `JAVA_OPTS` 必须为 `-Xms1g -Xmx2g -Djava.security.egd=file:/dev/./urandom`。

BDD: 测试服发布不得回退到 512m -> Given 展厅手动发布需要处理图片和音频资产, When 测试服发布脚本生成后端运行参数, Then 不得写入 `-Xms512m -Xmx512m`。

RED: `python -m pytest script\tests\test_publish_int_ruoyi_to_test_tooling.py -q` -> FAIL, expected publish script does not contain `JAVA_OPTS=-Xms1g -Xmx2g -Djava.security.egd=file:/dev/./urandom` and still contains the 512m runtime risk.

GREEN: `python -m pytest script\tests\test_publish_int_ruoyi_to_test_tooling.py -q` -> PASS, 23 tests passed.

GREEN: `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260525-test-publish-backend-heap-guard\bug-regression-evidence.md` -> PASS.

GREEN: `rg -n "JAVA_OPTS=.*Xmx" script\deploy\publish-int-ruoyi-to-test.ps1 script\deploy\int-ruoyi-test\docker-compose.yml script\tests\test_publish_int_ruoyi_to_test_tooling.py` -> PASS, publish script contains `-Xmx2g`; regression test asserts `-Xmx512m` is absent.

GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260525-test-publish-backend-heap-guard --mode preview` -> BLOCKED for apply gates, preview completed and listed only `bug-regression-evidence.md` as task artifact delete candidate; backend main worktree dirty and linked worktree cannot fast-forward merge into `int_main`.
