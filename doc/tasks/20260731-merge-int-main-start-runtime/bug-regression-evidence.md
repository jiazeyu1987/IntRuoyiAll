# Bug Regression Evidence

## Bug

After merging `origin/int_main` into `int_batch`, the backend first failed in `yudao-module-bpm` with missing Lombok-generated members and then, under Java 17, failed in `yudao-module-dcc` because four `NasRecursive*` dependencies were absent.

## Expected

The merged backend must compile under the repository Java 17 baseline before a new executable jar is used for runtime verification.

## Reproduction

Run the backend Maven compile with the previously active JDK 21, then run the full package with Java 17:

`mvn.cmd -pl yudao-module-bpm -DskipTests -Dmaven.compiler.proc=full -Dmaven.compiler.useIncrementalCompilation=false clean compile`

Representative failures included `ApprovalTaskReviewContext#setLoginUserId`, `BusinessApprovalRequest#toBuilder`, `BusinessApprovalPolicyDO#builder`, and `BpmTaskRespVO.OperationButtonSetting`.

The Java 17 package then reported missing `NasRecursiveScanHandler`, `NasRecursiveScanService`, `NasRecursiveScannedFile`, and `NasRecursiveSkippedDirectory`.

## Root Cause

Two independent root causes were confirmed:

1. The local environment had JDK 21 active while the project compiler baseline is Java 17. The apparent Lombok-generation failures were caused by the incompatible build environment.
2. Merge commit `2c277c09` had both expected parents but omitted 1922 additions and 483 modifications from the normal three-way result. This left newly merged DCC callers without their formal infra dependencies.

## Regression Test

No new production behavior was authored in this task. The Maven clean compile and full reactor package are the regression checks for the environment and merge-content defects; the normal merge also restores the tests that belong to the merged implementation.

## RED:

`mvn.cmd -pl yudao-module-bpm -DskipTests -Dmaven.compiler.proc=full -Dmaven.compiler.useIncrementalCompilation=false clean compile` with the pre-existing JDK 21 -> FAIL, missing Lombok-generated members were reported.

`mvn.cmd -pl yudao-server -am -DskipTests -Dmaven.compiler.proc=full -Dmaven.compiler.useIncrementalCompilation=false package` with Java 17 -> FAIL in `yudao-module-dcc`, missing formal `NasRecursive*` dependencies.

## GREEN:

`JAVA_HOME=C:\Users\BJB110\.jdks\jdk-17.0.20+8 mvn.cmd -pl yudao-module-bpm -DskipTests -Dmaven.compiler.proc=full -Dmaven.compiler.useIncrementalCompilation=false clean compile` -> PASS, 382 source files compiled and Maven reported `BUILD SUCCESS`.

Normal three-way merge recomputation and restoration -> PASS, resolved tree `d4fa4b4d9e5ed6e2d87a1e84d5a56062093fdd65` restored 2405 omitted or incorrect paths while preserving four pre-existing ignored ERP sources.

`JAVA_HOME=C:\Users\BJB110\.jdks\jdk-17.0.20+8 mvn.cmd -pl yudao-server -am -Dmaven.test.skip=true package` -> PASS, all 30 reactor modules succeeded and the executable Jar was generated.

`python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_branch_runtime_profile.py -q` -> PASS, `12 passed`; the backend startup script now runs an independent runtime Jar with stable logs.

## Verification

The Java 17 archive was downloaded through the Microsoft Build of OpenJDK official `aka.ms` entry point and SHA-256 verified as `e46fd292317c6bb0a8fe9dc63115021329f3a63caeba791c185f89f3666a68e5`. Merge-tree reconstruction reproduced only the two expected documentation conflicts.

Final verification completed: backend health is `UP` on `48041`, frontend returns HTTP `200` on `8041`, and rebuilding the Maven target Jar did not replace or interrupt the independent running Jar.

## Blockers

No blocker remains for the requested merge and local runtime startup.
