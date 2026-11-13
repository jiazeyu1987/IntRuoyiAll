import * as FileApi from '@/api/infra/file'
import { authContextRevision } from '@/utils/auth'
import {
  UploadRequestOptions,
  UploadProgressEvent
} from 'element-plus/es/components/upload/src/upload'
import axios, { AxiosProgressEvent } from 'axios'

/**
 * 获得上传 URL
 */
export const getUploadUrl = (): string => {
  return import.meta.env.VITE_BASE_URL + import.meta.env.VITE_API_URL + '/infra/file/upload'
}

export const useUpload = (directory?: string) => {
  // 后端上传地址
  const uploadUrl = getUploadUrl()
  // 是否使用前端直连上传
  const isClientUpload = UPLOAD_TYPE.CLIENT === import.meta.env.VITE_UPLOAD_TYPE
  // 重写ElUpload上传方法
  const httpRequest = async (options: UploadRequestOptions) => {
    const revision = authContextRevision.value
    const isRequestContextCurrent = () => authContextRevision.value === revision
    const assertCurrentContext = () => {
      if (!isRequestContextCurrent()) {
        throw new Error('登录或租户已切换，请在当前租户重新上传文件')
      }
    }
    // 文件上传进度监听
    const uploadProgressHandler = (evt: AxiosProgressEvent) => {
      if (!isRequestContextCurrent()) return
      const upEvt: UploadProgressEvent = Object.assign(evt.event)
      upEvt.percent = evt.progress ? evt.progress * 100 : 0
      options.onProgress(upEvt) // 触发 el-upload 的 on-progress
    }

    // 模式一：前端上传
    if (isClientUpload) {
      // 1.1 生成文件名称
      const fileName = options.file.name || options.filename
      // 1.2 获取文件预签名地址
      const presignedInfo = await FileApi.getFilePresignedUrl(
        fileName,
        directory,
        isRequestContextCurrent
      )
      assertCurrentContext()
      // 1.3 上传文件（不能使用 ElUpload 的 ajaxUpload 方法的原因：其使用的是 FormData 上传，Minio 不支持）
      await axios.put(presignedInfo.uploadUrl, options.file, {
        headers: {
          'Content-Type': options.file.type || 'application/octet-stream'
        },
        onUploadProgress: uploadProgressHandler
      })
      assertCurrentContext()
      // 元数据登记成功后才允许表单使用该文件。
      await FileApi.createFile(
        {
          configId: presignedInfo.configId,
          url: presignedInfo.url,
          path: presignedInfo.path,
          name: fileName,
          type: options.file.type || 'application/octet-stream',
          size: options.file.size
        },
        isRequestContextCurrent
      )
      assertCurrentContext()
      return { data: presignedInfo.url }
    } else {
      // 模式二：后端上传
      const res = await FileApi.updateFile(
        { file: options.file, directory },
        uploadProgressHandler,
        isRequestContextCurrent
      )
      assertCurrentContext()
      if (res.code !== 0) throw res
      return res
    }
  }

  return {
    uploadUrl,
    httpRequest
  }
}

/**
 * 上传类型
 */
enum UPLOAD_TYPE {
  // 客户端直接上传（只支持S3服务）
  CLIENT = 'client',
  // 客户端发送到后端上传
  SERVER = 'server'
}
