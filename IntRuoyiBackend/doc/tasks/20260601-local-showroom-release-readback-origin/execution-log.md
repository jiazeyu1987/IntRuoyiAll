# 执行日志：修复本机展厅发布读回缺少 origin 配置（后端回归）

BDD: 根目录重启脚本注入发布读回 origin -> Given 使用 `restart-ruoyi.bat` 启动本机后端 / When 执行展厅发布 / Then 后端具备 `showroom.release.public-website-origin`，不会因缺配置失败。

BDD: 本机读回使用后端公开发布 API -> Given 本机发布读回在同一机器执行 / When 配置 public readback origin / Then 指向 `http://127.0.0.1:48081` 对应的后端公开发布 API。

RED: python -m pytest script/tests/test_restart_ruoyi_script.py -q -> FAIL, `test_restart_script_sets_showroom_public_release_readback_origin` 断言缺少 `$showroomPublicReleaseOrigin` 和 `--showroom.release.public-website-origin`。

GREEN: python -m pytest script/tests/test_restart_ruoyi_script.py -q -> PASS, 3 passed。

实现记录：回归测试锁定根目录 `restart-ruoyi.bat` 必须注入本机发布读回 origin，防止后续再次漏配。
