# 执行日志：展厅站点 /showroom/sites 代理配置提交

BDD: 展厅站点路径由 Website Nginx 代理到后端 -> Given Website 运行时请求 `/showroom/sites/` scoped release/assets routes, When 测试服发布 Nginx 配置被应用, Then 请求应代理到 `__BACKEND_ORIGIN__`，而不是落到静态站点 history fallback。

BDD: 发布工具配置回归保护 -> Given 发布脚本打包 Website Nginx 配置, When 执行发布工具测试, Then 测试必须断言 `/showroom/sites/`、`/showroom/release/`、`/showroom/assets/` 均被支持。

RED: `python -c "import subprocess; text=subprocess.check_output(['git','show','HEAD:script/deploy/int-ruoyi-test/website.nginx.conf'], text=True, encoding='utf-8'); assert 'location /showroom/sites/' in text"` -> FAIL, `AssertionError`; previous committed `website.nginx.conf` lacks `/showroom/sites/`.

GREEN: `python -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py -q` -> PASS, 31 tests.

GREEN: `git diff --check` -> PASS, no whitespace errors.

GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260528-showroom-sites-nginx-proxy --mode preview` -> PASS, delete none, blocked none, warnings none.

GREEN: `git status --short --branch` after commit -> PASS, branch `int_main` ahead 441; remaining untracked path is excluded local runtime sample `runtime/`.
