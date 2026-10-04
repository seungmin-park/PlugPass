import { isAxiosError, isCancel } from 'axios'
import { isRecord } from './responseValidation'

export type ApiErrorKind = 'validation' | 'not_found' | 'rate_limit' | 'server' | 'network' | 'timeout' | 'cancelled' | 'invalid_response' | 'http' | 'unexpected'
export class ApiError extends Error {
  constructor(
    public readonly kind: ApiErrorKind,
    message: string,
    public readonly status: number | null = null,
    public readonly code: string | null = null,
    public readonly fields: Readonly<Record<string, string>> = {},
  ) { super(message); this.name = 'ApiError' }
}
export function toApiError(error: unknown): ApiError {
  if (error instanceof ApiError) return error
  if (isCancel(error)) return new ApiError('cancelled', '요청을 취소했습니다')
  if (error instanceof SyntaxError) return new ApiError('invalid_response', '서버 응답을 읽을 수 없습니다')
  if (!isAxiosError<unknown>(error)) return new ApiError('unexpected', '요청 처리 중 오류가 발생했습니다')
  if (error.name === 'SyntaxError') return new ApiError('invalid_response', '서버 응답을 읽을 수 없습니다')
  if (error.code === 'ECONNABORTED' || error.code === 'ETIMEDOUT') return new ApiError('timeout', '응답 대기 시간을 초과했습니다')
  if (!error.response) return new ApiError('network', '서버에 연결할 수 없습니다')
  const status = error.response.status
  const kind = httpErrorKind(status)
  const body = error.response.data
  if (isErrorBody(body)) return new ApiError(kind, body.message, status, body.code, { ...body.fields })
  return new ApiError(kind, '서버 요청에 실패했습니다', status)
}

function httpErrorKind(status: number): ApiErrorKind {
  if (status === 400) return 'validation'
  if (status === 404) return 'not_found'
  if (status === 429) return 'rate_limit'
  if (status >= 500 && status <= 599) return 'server'
  return 'http'
}

function isErrorBody(value: unknown): value is { code: string; message: string; fields: Record<string, string> } {
  return isRecord(value) && typeof value.code === 'string' && typeof value.message === 'string'
    && isRecord(value.fields) && Object.values(value.fields).every(message => typeof message === 'string')
}
