BDD: showroom backend staged changes should compile and satisfy the active public display contract -> Given the repo contains pending showroom backend changes for company fields and public display routes / When the targeted showroom tests run / Then the code must compile, expose the intended anonymous display routes, and keep the app-config company fields contract passing.

BDD: commit should include only the current task's direct backend files -> Given the repo also contains many unrelated untracked task directories / When preparing this commit / Then only the validated showroom backend files and companion task docs may be staged.

RED: `mvn -pl yudao-module-showroom "-Dtest=ShowroomAppConfigCompanyFieldsContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, `ShowroomDisplayController.CompanyPayload` record shape did not match `ShowroomApiRuntime.displayCompany()` constructor usage and the module could not compile.

RED: `mvn -pl yudao-module-showroom "-Dtest=ShowroomAppConfigCompanyFieldsContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, after compile fix the contract test still failed because its permission assertion expected an outdated anonymous surface inconsistent with the active Website public display flow.

GREEN: `mvn -pl yudao-module-showroom "-Dtest=ShowroomAppConfigCompanyFieldsContractTest,ShowroomHttpApiIntegrationTest#publicFrontstageDisplayEndpointsShouldBeAnnotatedPermitAllExceptHome" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS

GREEN: `git diff --cached --stat` -> PASS, staged set reduced to the direct showroom backend code and related task records for this submission.
