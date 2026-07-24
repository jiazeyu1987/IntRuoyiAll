import axios from 'axios'
import request from '@/config/axios'
import { config as axiosConfig } from '@/config/axios/config'
import {
  CONTROLLED_FILE_PREVIEW_WATERMARK_HEADER,
  DCC_ACCESS_EVENT_CODE_HEADER,
  DCC_VIEWER_TOKEN_HEADER,
  DCC_VIEWER_TOKEN_ID_HEADER,
  DCC_VIEWER_TOKEN_NONCE_HEADER,
  DCC_WATERMARK_TRACE_CODE_HEADER,
  buildControlledFileBinaryHeaders,
  decodePreviewWatermark,
  getAxiosHeader,
  getControlledFilePreviewMetadata,
  parseControlledFilePreviewMetadata,
  previewControlledFileWithWatermark,
  type ControlledFilePreviewMetadataVO,
  type ControlledFilePreviewWithWatermark
} from '@/api/dcc/controlledFile/workflow'

export type OnlineFilePreviewSource =
  | {
      type: 'DCC_CONTROLLED_FILE'
      controlledFileId: number | string
    }
  | {
      type: 'EDHR_SPECIAL_NODE_ATTACHMENT'
      fileId: number | string
    }

export const buildDccControlledFilePreviewSource = (
  controlledFileId: number | string
): OnlineFilePreviewSource => ({
  type: 'DCC_CONTROLLED_FILE',
  controlledFileId
})

export const buildEdhrSpecialNodeAttachmentPreviewSource = (
  fileId: number | string
): OnlineFilePreviewSource => ({
  type: 'EDHR_SPECIAL_NODE_ATTACHMENT',
  fileId
})

export const getOnlineFilePreviewMetadata = async (
  source: OnlineFilePreviewSource
): Promise<ControlledFilePreviewMetadataVO> => {
  if (source.type === 'DCC_CONTROLLED_FILE') {
    return getControlledFilePreviewMetadata(source.controlledFileId)
  }
  return parseControlledFilePreviewMetadata(
    await request.get({ url: `/dcc/file-preview/files/${source.fileId}/preview-metadata` })
  )
}

export const previewOnlineFileWithWatermark = async (
  source: OnlineFilePreviewSource,
  metadata?: ControlledFilePreviewMetadataVO
): Promise<ControlledFilePreviewWithWatermark> => {
  if (source.type === 'DCC_CONTROLLED_FILE') {
    return previewControlledFileWithWatermark(source.controlledFileId, metadata)
  }
  const resolvedMetadata = metadata || (await getOnlineFilePreviewMetadata(source))
  const response = await axios.get<Blob>(
    `${axiosConfig.base_url}/dcc/file-preview/files/${source.fileId}/preview`,
    {
      headers: buildControlledFileBinaryHeaders({
        [DCC_VIEWER_TOKEN_HEADER]: resolvedMetadata.viewerToken,
        [DCC_VIEWER_TOKEN_ID_HEADER]: resolvedMetadata.viewerTokenId,
        [DCC_VIEWER_TOKEN_NONCE_HEADER]: resolvedMetadata.viewerTokenNonce,
        [DCC_ACCESS_EVENT_CODE_HEADER]: resolvedMetadata.accessEventCode,
        [DCC_WATERMARK_TRACE_CODE_HEADER]: resolvedMetadata.watermarkTraceCode
      }),
      timeout: axiosConfig.request_timeout,
      responseType: 'blob'
    }
  )
  const watermark = decodePreviewWatermark(
    getAxiosHeader(response.headers, CONTROLLED_FILE_PREVIEW_WATERMARK_HEADER)
  )
  return {
    blob: response.data,
    watermark: {
      ...watermark,
      traceCode: resolvedMetadata.watermarkTraceCode
    }
  }
}
