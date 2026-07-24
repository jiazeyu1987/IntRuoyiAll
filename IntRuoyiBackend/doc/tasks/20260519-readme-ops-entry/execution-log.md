BDD: README should expose the operations toolkit entry -> Given the repository already contains a unified ops launcher and a dedicated `OPS.md` guide / When a collaborator opens `README.md` / Then they should see a clear operations entry that points them to both the guide and the launcher without hunting through the tree.
- GREEN: added a top-level `## Operations` section near the start of `README.md`.
- GREEN: the section points to `OPS.md` and `运维工具.bat`.
- GREEN: `Select-String -Path D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\README.md -Pattern 'Operations','OPS.md','运维工具.bat'` -> PASS.
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260519-readme-ops-entry --mode preview` -> PASS.
