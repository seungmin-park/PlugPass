import { AxiosError, AxiosHeaders, CanceledError, type AxiosResponse } from 'axios'
import { describe, expect, it } from 'vitest'
import { ApiError, toApiError } from './apiError'

describe('HTTP 오류 변환', () => {
  it.each([
    [400, 'validation'], [404, 'not_found'], [429, 'rate_limit'], [500, 'server'], [503, 'server'], [401, 'http'],
  ])('HTTP %s를 %s로 구분한다', (status, kind) => {
    const response = errorResponse(status, { code: 'INVALID_REQUEST', message: '요청 오류', fields: { latitude: '범위 오류' } })
    const error = toApiError(new AxiosError('request failed', undefined, undefined, undefined, response))
    expect(error).toMatchObject({ kind, status, code: 'INVALID_REQUEST', message: '요청 오류', fields: { latitude: '범위 오류' } })
  })
  it('깨진 오류 본문은 구조를 그대로 신뢰하지 않는다', () => {
    const response = errorResponse(400, { code: 1, message: {}, fields: { radiusMeters: 123 } })
    expect(toApiError(new AxiosError('failure', undefined, undefined, undefined, response))).toMatchObject({ kind: 'validation', status: 400, fields: {} })
  })
  it('취소를 네트워크 실패와 구분한다', () => {
    expect(toApiError(new CanceledError())).toMatchObject({ kind: 'cancelled', status: null })
  })
  it.each(['ECONNABORTED', 'ETIMEDOUT'])('%s timeout을 구분한다', code => {
    expect(toApiError(new AxiosError('timeout', code))).toMatchObject({ kind: 'timeout', status: null })
  })
  it('응답 없는 통신 실패를 구분한다', () => {
    expect(toApiError(new AxiosError('offline', 'ERR_NETWORK'))).toMatchObject({ kind: 'network', status: null })
  })
  it('잘못된 JSON을 응답 오류로 구분한다', () => {
    expect(toApiError(new SyntaxError('invalid JSON'))).toMatchObject({ kind: 'invalid_response', status: null })
  })
  it('이미 변환한 오류는 보존한다', () => {
    const error = new ApiError('invalid_response', '응답 오류')
    expect(toApiError(error)).toBe(error)
  })
  it('예상하지 못한 실행 오류를 통신 실패로 오인하지 않는다', () => {
    expect(toApiError(new Error('bug'))).toMatchObject({ kind: 'unexpected', status: null })
  })
})

function errorResponse(status: number, data: unknown): AxiosResponse<unknown> {
  return { status, data, statusText: String(status), headers: new AxiosHeaders(), config: { headers: new AxiosHeaders() } }
}
