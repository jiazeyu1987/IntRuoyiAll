BDD: unified ops launcher should expose a command quick reference -> Given operators may want to call the launcher directly from `cmd` with preset arguments / When they run `运维工具.bat help` or choose `Help` from the root menu / Then the launcher should print the supported publish, restart, status, and cancel commands without triggering any runtime action.
- RED: `python -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_publish_int_ruoyi_to_test_tooling.py -q` -> FAIL, the unified launcher had no `help` route, no `/?` alias, and no help page content.
- GREEN: added `help` and `/?` routes plus a `Help` menu item to repository-root `运维工具.bat`.
- GREEN: added a direct command quick reference section showing publish, restart, status, and cancel command forms.
- GREEN: `python -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_publish_int_ruoyi_to_test_tooling.py -q` -> PASS, `13 passed`.
- GREEN: help page refinement -> PASS, the direct command examples now expand `%~nx0` and print the real launcher filename `运维工具.bat`.
- GREEN: `cmd /c "\"D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\运维工具.bat\" help"` -> PASS, the launcher printed the help page and returned without triggering any remote action.
- GREEN: `cmd /c D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\运维工具.bat cancel` -> PASS, the launcher still exits safely after the help-page enhancement.
