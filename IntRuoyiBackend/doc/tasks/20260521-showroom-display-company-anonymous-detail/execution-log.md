BDD: anonymous display company detail returns company-only public detail -> Given the current company live revision and company preview asset are available / When Website requests anonymous `GET /showroom/display/company` / Then the backend must return company title, subtitle, image URL, and public fields without depending on hall/product live data.

BDD: anonymous company detail remains fail-fast on missing company live prerequisites -> Given the current company preview asset or live company revision is missing / When anonymous `GET /showroom/display/company` is requested / Then the endpoint must return the exact company-side blocker instead of fallback data.

BDD: only app-config and company detail become anonymous display routes -> Given showroom display routes still contain home/hall/product/narration / When anonymous access policy is checked / Then only `getAppConfig()` and `getCompany()` may be public.

RED: Pending contract test update.

GREEN: Pending implementation and verification.
