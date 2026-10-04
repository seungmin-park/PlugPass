import type { AxiosResponse, InternalAxiosRequestConfig } from 'axios'
export function jsonResponse(config: InternalAxiosRequestConfig, data: unknown, status = 200): AxiosResponse<string> {
  return { config, data: JSON.stringify(data), status, statusText: String(status), headers: { 'content-type': 'application/json' } }
}
