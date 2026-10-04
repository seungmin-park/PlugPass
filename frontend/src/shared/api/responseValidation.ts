export function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
}

export function isNonblankString(value: unknown): value is string {
  return typeof value === 'string' && value.trim().length > 0
}

export function hasNonblankStrings(value: Record<string, unknown>, fields: readonly string[]): boolean {
  return fields.every(field => isNonblankString(value[field]))
}

export function isNullableString(value: unknown): value is string | null {
  return value === null || typeof value === 'string'
}

export function hasNullableStrings(value: Record<string, unknown>, fields: readonly string[]): boolean {
  return fields.every(field => isNullableString(value[field]))
}

export function isNumberInRange(value: unknown, minimum: number, maximum: number): value is number {
  return typeof value === 'number' && Number.isFinite(value) && value >= minimum && value <= maximum
}

export function isNonnegativeNumber(value: unknown): value is number {
  return typeof value === 'number' && Number.isFinite(value) && value >= 0
}

export function isNonnegativeInteger(value: unknown): value is number {
  return isNonnegativeNumber(value) && Number.isSafeInteger(value)
}
