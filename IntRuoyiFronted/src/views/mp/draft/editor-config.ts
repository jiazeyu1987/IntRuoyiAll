import { IEditorConfig } from '@wangeditor-next/editor'
import { getAccessToken, getTenantId } from '@/utils/auth'

type UploadImageConfig = NonNullable<NonNullable<IEditorConfig['MENU_CONF']>['uploadImage']>
type UploadFiles = Parameters<NonNullable<UploadImageConfig['onBeforeUpload']>>[0]

export const createEditorConfig = (
  server: string,
  accountId: number | undefined
): Partial<IEditorConfig> => {
  const message = useMessage()
  const uploadImageConfig: UploadImageConfig = {
    server,
    base64LimitSize: 0,
    maxFileSize: 5 * 1024 * 1024,
    maxNumberOfFiles: 10,
    allowedFileTypes: ['image/*'],
    meta: {
      accountId,
      type: 'image'
    },
    metaWithUrl: true,
    headers: new Headers({
      Accept: '*',
      Authorization: `Bearer ${getAccessToken()}`,
      'tenant-id': getTenantId()
    }),
    withCredentials: true,
    timeout: 5 * 1000,
    fieldName: 'file',
    onBeforeUpload(files: UploadFiles) {
      console.log(files)
      return files
    },
    onProgress(progress: number) {
      console.log('progress', progress)
    },
    onSuccess(file, res: any) {
      console.log('onSuccess', file, res)
    },
    onFailed(file, res: any) {
      message.alertError(res.message)
      console.log('onFailed', file, res)
    },
    onError(file, err: any, res: any) {
      message.alertError(err.message)
      console.error('onError', file, err, res)
    },
    customInsert(res: any, insertFn) {
      insertFn(res.data.url, undefined, 'image', res.data.url)
    }
  }

  return {
    MENU_CONF: {
      uploadImage: uploadImageConfig
    }
  }
}
