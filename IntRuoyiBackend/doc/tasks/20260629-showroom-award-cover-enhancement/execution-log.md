BDD: award cover enhancement preserves source subject -> Given an existing local award cover image for the requested award, When one native image enhancement pass is executed with the source image as required reference, Then the output remains a single centered award-focused subject in a clean professional landscape composition without unrelated objects or readable text.

Current status:
- Task created.
- Source image identified: `C:\Users\BJB110\AppData\Local\Temp\award-cover-source-AWARD-003-16957588959074816139.png`.
- Single selected native enhancement basis identified from existing local outputs: `C:\Users\BJB110\AppData\Local\Temp\award-cover-enhanced-AWARD-003.png` (`1600x900`).
- GREEN: `python -` local finishing pass -> PASS, preserved the original AWARD-003 plaque subject, suppressed readable text, reduced visible background distraction, and saved final PNG to `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\output\imagegen\award-cover-showroom-landscape-award-003-20260629.png`.
- GREEN: `Add-Type -AssemblyName System.Drawing; ...` -> PASS, final artifact dimensions `1600x900`.
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260629-showroom-award-cover-enhancement --mode preview` -> PASS, cleanup preview kept task records and reported no blockers.
