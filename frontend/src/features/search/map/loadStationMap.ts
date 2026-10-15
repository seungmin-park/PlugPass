import type { StationMapFactory } from './stationMap'
import { appPlatform } from '../../../shared/platform/runtime'

export async function loadStationMap(): Promise<StationMapFactory> {
  if (appPlatform !== 'web') return (await import('./nativeStationMap')).createNativeStationMap
  const kakaoKey = import.meta.env.VITE_KAKAO_JS_KEY
  if (!kakaoKey) throw new Error('카카오 지도 키가 설정되지 않았습니다.')
  const sdk = await import('./kakaoStationMap')
  await sdk.loadKakaoMaps(kakaoKey)
  return sdk.createKakaoStationMap
}
