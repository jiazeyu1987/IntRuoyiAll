# Task: generate-product-020-showroom-cover

## Goal
Create one square PNG showroom cover image for `product_020` based only on the provided product facts.

## Milestones
- [completed] Create task record and execution log
- [blocked] Generate one square showroom cover image with native image generation
- [blocked] Verify file exists, is PNG, is square, and visually aligns with the provided constraints
- [blocked] Mark task complete and record final verification

## Expected Verification
- Generated file exists on local filesystem
- File format is PNG
- Image dimensions are square
- Visual review confirms:
  - one hero medical-device concept/showroom visual
  - no readable text, logos, or UI overlays
  - clean, premium, clinical showroom mood
  - no fabricated registration details rendered in-image

## Current Status
Blocked.

Missing precondition: the native image generation tool is unavailable in this session.

Impact: the required showroom cover PNG cannot be created without violating the strict no-fallback requirement and the user's explicit instruction to use native image generation exactly once.
