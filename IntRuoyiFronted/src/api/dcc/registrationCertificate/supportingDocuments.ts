import request from '@/config/axios'
export const uploadSupportingDocument = (certificateId: string | number, data: FormData, key: string) =>
  request.upload({ url: `/dcc/registration-certificates/${certificateId}/supporting-documents`, data,
    headers: { 'Idempotency-Key': key } })
