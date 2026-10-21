import type { MapRect, NativeMapLayout } from './nativeMapBridge'

function documentRect(element: Element): MapRect {
  const rect = element.getBoundingClientRect()
  return { x: rect.x + window.scrollX, y: rect.y + window.scrollY, width: rect.width, height: rect.height }
}
function intersection(left: MapRect, right: MapRect): MapRect {
  const x = Math.max(left.x, right.x)
  const y = Math.max(left.y, right.y)
  return { x, y, width: Math.max(0, Math.min(left.x + left.width, right.x + right.width) - x),
    height: Math.max(0, Math.min(left.y + left.height, right.y + right.height) - y) }
}
export function readNativeMapLayout(container: HTMLElement): NativeMapLayout {
  const frame = documentRect(container)
  let clip = frame
  let visible = container.isConnected && !document.hidden && frame.width > 0 && frame.height > 0
  for (let element: HTMLElement | null = container; element; element = element.parentElement) {
    const style = getComputedStyle(element)
    if (style.display === 'none' || style.visibility === 'hidden') visible = false
    if (element !== container && /(hidden|clip|scroll|auto)/.test(`${style.overflowX} ${style.overflowY}`))
      clip = intersection(clip, documentRect(element))
  }
  for (const dialog of document.querySelectorAll<HTMLElement>('[role="dialog"], dialog[open]')) {
    const style = getComputedStyle(dialog)
    if (!dialog.hidden && style.display !== 'none' && style.visibility !== 'hidden') visible = false
  }
  return { frame, clip, visible, viewportHeight: window.visualViewport?.height ?? window.innerHeight }
}

export function observeNativeMapLayout(container: HTMLElement, changed: (layout: NativeMapLayout) => void): () => void {
  let scheduled: number | null = null
  let closed = false
  const schedule = () => {
    if (closed || scheduled !== null) return
    scheduled = requestAnimationFrame(() => {
      scheduled = null
      if (!closed) changed(readNativeMapLayout(container))
    })
  }
  const resize = new ResizeObserver(schedule)
  resize.observe(container)
  const mutations = new MutationObserver(schedule)
  mutations.observe(document.body, { subtree: true, attributes: true, childList: true, characterData: true })
  window.addEventListener('scroll', schedule, true)
  window.addEventListener('resize', schedule)
  document.addEventListener('visibilitychange', schedule)
  window.visualViewport?.addEventListener('resize', schedule)
  window.visualViewport?.addEventListener('scroll', schedule)
  return () => {
    closed = true
    if (scheduled !== null) cancelAnimationFrame(scheduled)
    resize.disconnect()
    mutations.disconnect()
    window.removeEventListener('scroll', schedule, true)
    window.removeEventListener('resize', schedule)
    document.removeEventListener('visibilitychange', schedule)
    window.visualViewport?.removeEventListener('resize', schedule)
    window.visualViewport?.removeEventListener('scroll', schedule)
  }
}
