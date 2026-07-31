# Execution Log

## 2026-07-24

- USER INTENT: D-Main must use ports distinct from every other local IntRuoyi base directory.
- BDD: unique five-directory ports -> Given five base directories, When each runtime profile resolves, Then D-Main is 8101/48101, E-Main is 8081/48081, Batch is 8041/48041, Shedule is 8021/48021, and QMS is 8061/48061.
- RED: existing contract -> FAIL, D-Main and E-Main both used 8081/48081.
- IMPLEMENTED: Added path-bound int_main_d profile, branch-main-d frontend environment, and v2 guard/document contract.
