import type { StationMapFactory } from './stationMap'

export async function loadStationMap(): Promise<StationMapFactory> {
  const kakaoKey = import.meta.env.VITE_KAKAO_JS_KEY
  if (!kakaoKey) throw new Error('카카오 지도 키가 설정되지 않았습니다.')
  const sdk = await import('./kakaoStationMap')
  await sdk.loadKakaoMaps(kakaoKey)
  return sdk.createKakaoStationMap
}
