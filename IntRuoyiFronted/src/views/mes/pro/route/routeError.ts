type RouteErrorLike = {
  message?: unknown
  msg?: unknown
  response?: {
    data?: {
      message?: unknown
      msg?: unknown
    }
  }
}

const pickText = (value: unknown) =>
  typeof value === 'string' && value.trim() ? value.trim() : undefined

export const isRouteConfirmCancel = (error: unknown) => error === 'cancel' || error === 'close'

export const resolveRouteOperationErrorMessage = (error: unknown, fallback: string) => {
  if (error instanceof Error) {
    return pickText(error.message) || fallback
  }
  if (typeof error === 'string') {
    return pickText(error) || fallback
  }
  if (!error || typeof error !== 'object') {
    return fallback
  }

  const errorLike = error as RouteErrorLike
  return (
    pickText(errorLike.response?.data?.msg) ||
    pickText(errorLike.response?.data?.message) ||
    pickText(errorLike.msg) ||
    pickText(errorLike.message) ||
    fallback
  )
}
