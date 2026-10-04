export class TestDiagnostics {
  private readonly messages: string[] = []

  record(kind: string, message: string): void {
    this.messages.push(`[${kind}] ${message}`)
  }

  assertEmpty(): void {
    if (this.messages.length > 0) throw new Error(this.messages.join('\n'))
  }
}
