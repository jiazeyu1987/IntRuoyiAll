BDD: unified ops launcher should group actions by intent -> Given the root launcher now exposes publish, restart, and status routes / When an operator opens the menu / Then related actions should be shown under grouped headings so publish actions, restart actions, status actions, and cancel are visually separated.
- RED: `python -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_publish_int_ruoyi_to_test_tooling.py -q` -> FAIL, the unified launcher menu still listed actions flatly without grouped headings.
- GREEN: refined `运维工具.bat` from a flat single-layer menu into a two-level menu:
  - root menu: `Publish / Restart / Status / Cancel`
  - submenus: `Test / Production / Cancel`
  while preserving all existing direct route arguments such as `test`, `prod`, `test-restart`, `prod-status`, and `cancel`.
- GREEN: `python -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_publish_int_ruoyi_to_test_tooling.py -q` -> PASS, `12 passed`.
- GREEN: `cmd /c D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\运维工具.bat cancel` -> PASS, the grouped menu still exits safely with `[INFO] Ops launcher cancelled.`.
