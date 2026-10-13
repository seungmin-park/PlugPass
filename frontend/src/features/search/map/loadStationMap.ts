import type { StationMapFactory } from './stationMap'

export async function loadStationMap(): Promise<StationMapFactory> {
  const kakaoKey = import.meta.env.VITE_KAKAO_JS_KEY
  if (kakaoKey) {
    const sdk = await import('./kakaoStationMap')
    await sdk.loadKakaoMaps(kakaoKey)
    return sdk.createKakaoStationMap
  }
  const sdk = await import('./leafletStationMap')
  return sdk.createLeafletStationMap
}
