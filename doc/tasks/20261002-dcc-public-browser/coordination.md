# Coordination needed: public all-file project browser

To Root / backend_public_repair / detail_workflow_integration:

The project browser preliminary coordinator uses existing loadDccSelectorPage only as an interim controlled-candidate reader. Root correctly requires full project file browsing to include WORKING and in-flight versions without falling back to NAS. Please freeze a new explicit browser scope contract at existing /dcc/controlled-files/browser-page, with real projectFolderId for directory mode and no project/folder for global. Prefer browserScope = GLOBAL | PROJECT_FOLDER distinct from selectorScope, response full ControlledFileRespVO/versions and server total. Shared wrapper Owner should expose loadDccProjectBrowserPage and preserve exact string identity, real canPreview and version facts. Do not change relation/reference selector latestControlled semantics.

The independent browser/project-browser.ts coordinator can switch its main loadFiles to this wrapper; loadPage remains the controlled selector for reference operations. Need response contract before finalizing main row/version display.

Current relation FileVersion lacks canPreview and project/folder labels; backend confirms no projection change this batch. I can compose actual relation results with formal getControlledFile permission and getProjectDiscovery for labels; frozen historical display must retain relation snapshot fields and only use detail for canPreview. Please confirm whether detail Owner is mounting relations; avoid duplicate ownership.
