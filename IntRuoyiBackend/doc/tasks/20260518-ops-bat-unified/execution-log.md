BDD: unified ops launcher should route to existing wrappers -> Given the repository already has verified `publish-int-ruoyi-to-test.bat` and `publish-int-ruoyi-to-prod.bat` wrappers / When the operator launches `运维工具.bat` / Then it should present a simple menu or preset routing that delegates to those wrappers instead of duplicating publish logic.
BDD: unified ops launcher should support safe cancellation -> Given launcher validation should not accidentally deploy / When the operator runs `运维工具.bat cancel` / Then the launcher should exit cleanly without calling the test or production publish wrappers.
- RED: `python -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_publish_int_ruoyi_to_test_tooling.py -q` -> FAIL, no repository-root `运维工具.bat` unified launcher existed yet.
- GREEN: added repository-root `运维工具.bat` with:
  - fixed routing to `script\deploy\publish-int-ruoyi-to-test.bat`
  - fixed routing to `script\deploy\publish-int-ruoyi-to-prod.bat`
  - no-argument menu mode
  - direct `test` / `prod` routing mode
  - safe `cancel` path
- GREEN: `python -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_publish_int_ruoyi_to_test_tooling.py -q` -> PASS, `7 passed`.
- GREEN: `cmd /c D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\运维工具.bat cancel` -> PASS, wrapper returned `[INFO] Ops launcher cancelled.` without triggering a release.
