BDD: showroom cover image from provided facts only -> Given the supplied product facts and image constraints / When one native image generation call is executed / Then a single square PNG showroom cover image is produced and its absolute local path is returned

GREEN: native image generation -> PASS
GREEN: `Get-ChildItem -Path 'C:\Users\BJB110\.codex\generated_images\019e4ece-4587-7571-b557-5b0817314404' -File | Select-Object -ExpandProperty FullName` -> PASS
