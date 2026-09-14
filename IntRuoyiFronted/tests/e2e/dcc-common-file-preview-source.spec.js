const fs = require('fs')
const path = require('path')

const root = path.resolve(__dirname, '../..')
const workflowApiPath = path.join(root, 'src/api/dcc/controlledFile/workflow.ts')
const viewerPath = path.join(root, 'src/views/dcc/controlled-file/view/index.vue')
const uploadPath = path.join(root, 'src/views/dcc/controlled-file/upload/index.vue')

const workflowApiSource = fs.readFileSync(workflowApiPath, 'utf8')
const viewerSource = fs.readFileSync(viewerPath, 'utf8')
const uploadSource = fs.readFileSync(uploadPath, 'utf8')

for (const kind of ['VIDEO', 'AUDIO']) {
  if (!workflowApiSource.includes(`'${kind}'`)) {
    throw new Error(`ControlledFilePreviewKind must include ${kind}`)
  }
  if (!viewerSource.includes(`resolvedPreviewKind === '${kind}'`)) {
    throw new Error(`Protected viewer must branch ${kind}`)
  }
}

if (!viewerSource.includes('<video') || !viewerSource.includes('protected-viewer-video')) {
  throw new Error('Protected viewer must render video files with a native video element')
}

if (!viewerSource.includes('<audio') || !viewerSource.includes('protected-viewer-audio')) {
  throw new Error('Protected viewer must render audio files with a native audio element')
}

const unavailableReasonGuardIndex = viewerSource.indexOf('if (resolvedPreviewUnavailableReason.value)')
const binaryPreviewLoadIndex = viewerSource.indexOf('const previewPayload = await resolvePreviewBlob()')
if (unavailableReasonGuardIndex < 0) {
  throw new Error('Protected viewer must short-circuit preview binary loading when previewUnavailableReason is present')
}
if (binaryPreviewLoadIndex < 0 || unavailableReasonGuardIndex > binaryPreviewLoadIndex) {
  throw new Error('previewUnavailableReason must be honored before any binary preview request is made')
}
if (!viewerSource.includes("resolvedPreviewKind.value !== 'OFFICE'")) {
  throw new Error('Non-Office preview kinds must surface previewUnavailableReason through the generic viewer error area')
}

const requiredTransformFragments = [
  'protected-viewer-transform-controls',
  'protected-viewer-transform-controls--sticky',
  'protected-viewer-transform-controls__grid',
  'protected-viewer-transform-controls__status',
  'protected-viewer-frame--transformable',
  'canTransformPreview',
  'handleZoomIn',
  'handleZoomOut',
  'handleRotateRight',
  'handleResetViewerTransform',
  'pdfPreviewTransformStyle',
  'getPdfPageViewportStyle',
  'protected-viewer-page__canvas-viewport',
  'protected-viewer-transform-stage',
  'renderPdfPages(previewPayload.bytes, currentRenderVersion, scale)',
  'MIN_VIEWER_ZOOM_PERCENT',
  'MAX_VIEWER_ZOOM_PERCENT',
  'VIEWER_ZOOM_STEP_PERCENT',
  'currentPdfBytes',
  'clonePdfBytesForWorker',
  'data: clonePdfBytesForWorker(pdfBytes)',
  'max-height: calc(100vh - 180px)',
  'overscroll-behavior: contain',
  "['PDF', 'IMAGE'].includes(resolvedPreviewKind.value)"
]
for (const fragment of requiredTransformFragments) {
  if (!viewerSource.includes(fragment)) {
    throw new Error(`Protected viewer transform controls contract missing: ${fragment}`)
  }
}

if (!viewerSource.includes('<div v-if="pageEntries.length" ref="viewerFrameRef" class="protected-viewer-frame protected-viewer-frame--transformable">')) {
  throw new Error('PDF transform controls must stay inside a bounded protected viewer scroll frame')
}

if (!viewerSource.includes('<div v-if="mediaObjectUrl" class="protected-viewer-frame protected-viewer-frame--media protected-viewer-frame--transformable">')) {
  throw new Error('Image transform controls must stay inside a bounded protected viewer scroll frame')
}

if (!viewerSource.includes('放大') || !viewerSource.includes('缩小') || !viewerSource.includes('旋转') || !viewerSource.includes('复原')) {
  throw new Error('Protected viewer must expose 2x2 zoom, right-rotate, and reset action labels')
}

if (viewerSource.includes('左旋转90°') || viewerSource.includes('右旋转90°') || viewerSource.includes('handleRotateLeft')) {
  throw new Error('Protected viewer compact controls must not expose separate left/right rotation actions')
}

if (viewerSource.includes('applyPdfZoomChange') || viewerSource.includes('rerenderCurrentPdf')) {
  throw new Error('PDF zoom must not rerender the whole pdf.js document on every click')
}

if (viewerSource.includes('baseScale * viewerScale.value')) {
  throw new Error('PDF initial render must use the base canvas scale and leave zoom to CSS transforms')
}

if (!uploadSource.includes('EDITABLE_SOURCE_MESSAGE')) {
  throw new Error('DCC upload tip must state that any single file type is supported')
}

console.log('PASS: DCC common file preview source wiring is present')
