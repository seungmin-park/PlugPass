import axios, { type AxiosRequestConfig } from 'axios'
import { ApiError, toApiError } from './apiError'
export const httpClient = axios.create({ baseURL: '/api/v1', timeout: 10000, responseType: 'json', transitional: { silentJSONParsing: false } })

export async function getValidatedJson<T>(path: string, validate: (value: unknown) => value is T,
  options: AxiosRequestConfig = {}): Promise<T> {
  try {
    const response = await httpClient.get<unknown>(path, options)
    if (!validate(response.data)) throw new ApiError('invalid_response', '서버 응답 형식이 올바르지 않습니다')
    return response.data
  } catch (error: unknown) {
    throw toApiError(error)
  }
}
