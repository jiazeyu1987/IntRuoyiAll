# 执行日志：带数据发布补齐 DCC 对象清单门禁

- BDD: 带数据发布必须包含 DCC 对象清单 -> Given 构建发布包选择 `publishScope=with-data` / When 生成 release package / Then 发布包必须包含 DCC 对象清单或索引，缺失时失败。
- BDD: 缺少 DCC 引用对象必须阻断 -> Given DCC 数据库记录引用的对象在源环境中不存在 / When 构建带数据发布包 / Then 构建必须失败并报告缺失对象。
- RED: `python -X utf8 -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py -k with_data_release_package_requires_dcc_object_inventory -q` -> FAIL, expected reason: 发布脚本尚无 `Write-DccObjectInventoryForReleasePackage`、`manifest\dcc-object-inventory.json` 和 DCC 对象缺失门禁。
- GREEN: `python -X utf8 -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py -k with_data_release_package_requires_dcc_object_inventory -q` -> PASS, `1 passed, 58 deselected`。
- GREEN: `python -X utf8 -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py -q` -> PASS, `59 passed`。
- GREEN: PowerShell `[scriptblock]::Create(...)` 解析 `script/deploy/publish-int-ruoyi.ps1` -> PASS, `POWERSHELL_PARSE_OK`。
- REGRESSION: `python -X utf8 -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py script/tests/test_publish_int_ruoyi_deploy_services.py -q` -> FAIL, existing unrelated blocker: `test_onlyoffice_readiness_is_waited_only_when_compose_declares_it` 与另一测试文件对 OnlyOffice 服务发现条件存在冲突，本任务未修改 OnlyOffice 发布策略。
