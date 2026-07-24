BDD: operations docs should be scannable from top to bottom -> Given the repository already has a working launcher and a complete `OPS.md` guide / When a maintainer opens the docs / Then the key entrypoints, environments, and commands should be visible in compact tables instead of long scattered lists.
- GREEN: rewrote the key `OPS.md` sections into compact tables for main entry, menu shape, direct commands, environments, wrappers, safety notes, and evidence pointers.
- GREEN: refined the `README.md` operations summary into a compact table linking to `OPS.md` and `运维工具.bat`.
- GREEN: `Select-String -Path D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\OPS.md -Pattern '|'` -> PASS.
- GREEN: `Select-String -Path D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\README.md -Pattern 'Operations','OPS.md','运维工具.bat'` -> PASS.
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260519-ops-doc-table-format --mode preview` -> PASS.
