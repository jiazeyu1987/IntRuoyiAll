# Execution Log: DCC 测试根目录 3.DMR 彻底删除

BDD: 删除 DCC 测试根目录及其测试文件 -> Given 真实运行态 `DCC目录管理` 中存在根目录 `3.DMR` 及其子目录和测试文件 / When 执行本次彻底删除 / Then `3.DMR` 子树、目录内受控文件记录、关联运行态记录与底层文件存储引用均应被删除，页面和底层校验都不再返回这些测试数据

RED: live MySQL audit on `jdbc:mysql://127.0.0.1:23306/ruoyi-vue-pro` -> FAIL, 当前存在两棵同名根目录 `3.DMR`；`900003` 子树含 `2699` 个目录、`96` 条分类绑定和 `30` 条测试/联调受控文件，`902702` 子树含 `2699` 个目录但无绑定无文件，直接删除整个根目录会超出“仅清理测试文件”范围

GREEN: `python -X utf8 D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260522-dcc-test-root-hard-delete\scripts\dcc_test_root_cleanup.py --mode cleanup` -> PASS, 已真实删除 `5398` 个 `dcc_file_directory` 节点、`96` 条 `dcc_category_directory_binding`、`30` 条 `dcc_controlled_file`、`30` 个 Flowable `process_instance_id` 及其关联运行态/历史态记录，并通过 `infra/file/delete-list` 删除 `5` 个底层文件对象后再硬删 `infra_file` 元数据。

GREEN: `python -X utf8 D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260522-dcc-test-root-hard-delete\scripts\dcc_test_root_cleanup.py --mode verify` -> PASS, 删除后 `root_name_matches=0`、`remainingTotal=0`，目标 DCC/Flowable/infra 表残留全部归零。

GREEN: `npx.cmd --yes --package @playwright/cli playwright-cli --session dcc-test-root-hard-delete open http://127.0.0.1:8081/login?redirect=%2Fdcc%2Fcontrolled-file%2Fdirectories` + `npx.cmd --yes --package @playwright/cli playwright-cli --session dcc-test-root-hard-delete run-code --filename D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260522-dcc-test-root-hard-delete\scripts\verify-dcc-root-delete-e2e.mjs` -> PASS, 真实页面标题为 `瑛泰管理系统 - DCC目录管理`，实时目录树接口返回 `checkedDirectoryCount=0`，页面正文与接口都不再出现 `3.DMR`。

GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260522-dcc-test-root-hard-delete --mode preview` -> PASS, closeout preview 返回 `status: ready`，仅提示删除任务临时脚本；本次收尾已按预览结果执行清理。
