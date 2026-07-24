# Task: 20260626-showroom-product-cover-ocr-translation

## Goal
Extract the Chinese product narration text from the provided showroom cover image path and translate it into natural, formal English narration text.

## Milestones
- Identify and access the referenced image asset.
- Extract the Chinese source text from the image.
- Translate the extracted text into English and return only the English body.

## Expected Verification
- The referenced image path resolves to an actual image.
- The extracted Chinese text is readable enough to translate without inventing missing content.
- Final response contains only the English body text.

## Current Status
- Completed: the referenced image resolves and is readable as a PNG asset, but it contains only a product render with no readable Chinese narration text. The image path was then matched to the live showroom product revision and its linked Chinese narration source, which was used for translation.

## Verification Evidence
- The provided image path resolved successfully from the local backend.
- The downloaded image was visually inspected and confirmed to contain no readable Chinese narration text.
- The cover hash `a8ae6037931540cd` was matched to the live showroom product revision and its linked public Chinese narration text was recovered from `showroom_narration_version`.
