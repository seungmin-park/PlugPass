import { getCurrentScope, onScopeDispose, readonly, ref } from 'vue'

export interface LocationCoordinates { latitude: number; longitude: number }

export function useCurrentLocation() {
  const status = ref<'idle' | 'loading' | 'success' | 'error'>('idle')
  const coordinates = ref<LocationCoordinates | null>(null)
  const error = ref<string | null>(null)
  let requestSequence = 0
  let cancelPending: (() => void) | null = null

  function cancel(): void {
    requestSequence += 1
    cancelPending?.()
    cancelPending = null
    status.value = 'idle'
    coordinates.value = null
    error.value = null
  }

  function requestLocation(): Promise<LocationCoordinates | null> {
    cancel()
    status.value = 'loading'
    const sequence = requestSequence
    return new Promise(resolve => {
      const timer = setTimeout(() => complete(null, '10초 안에 위치를 확인하지 못했습니다. 다시 시도하거나 검증용 예시 위치를 선택해 주세요.'), 10000)
      cancelPending = () => { clearTimeout(timer); resolve(null) }
      let completed = false
      function complete(result: LocationCoordinates | null, message: string | null = null): void {
        if (completed || sequence !== requestSequence) return
        completed = true
        clearTimeout(timer)
        cancelPending = null
        coordinates.value = result
        error.value = message
        status.value = result ? 'success' : 'error'
        resolve(result)
      }
      if (typeof navigator === 'undefined' || !navigator.geolocation) {
        complete(null, '현재 위치를 지원하지 않는 브라우저입니다. 검증용 예시 위치를 선택해 주세요.')
        return
      }
      try {
        navigator.geolocation.getCurrentPosition(position => {
          const { latitude, longitude } = position.coords
          if (!Number.isFinite(latitude) || latitude < -90 || latitude > 90
            || !Number.isFinite(longitude) || longitude < -180 || longitude > 180) {
            complete(null, '현재 위치 좌표를 확인할 수 없습니다. 다시 시도하거나 검증용 예시 위치를 선택해 주세요.')
            return
          }
          complete({ latitude, longitude })
        }, failure => {
          const messages: Readonly<Record<number, string>> = {
            1: '위치 권한이 거부되었습니다. 브라우저 권한을 확인하거나 검증용 예시 위치를 선택해 주세요.',
            2: '현재 위치를 확인할 수 없습니다. 다시 시도하거나 검증용 예시 위치를 선택해 주세요.',
            3: '10초 안에 위치를 확인하지 못했습니다. 다시 시도하거나 검증용 예시 위치를 선택해 주세요.',
          }
          complete(null, messages[failure.code] ?? messages[2] ?? '현재 위치를 확인할 수 없습니다.')
        }, { timeout: 10000, maximumAge: 0, enableHighAccuracy: false })
      } catch {
        complete(null, '현재 위치를 요청할 수 없습니다. 브라우저 권한을 확인하거나 검증용 예시 위치를 선택해 주세요.')
      }
    })
  }

  if (getCurrentScope()) onScopeDispose(cancel)
  return { status: readonly(status), coordinates: readonly(coordinates), error: readonly(error), requestLocation, cancel }
}
