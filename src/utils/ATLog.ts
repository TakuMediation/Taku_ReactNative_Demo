/**
 * RN 层 debug 联调日志。受 `ATSDK.setNetworkLogDebug` 开关控制——关闭时 0 输出。
 * tag = `ATRN-JS`（**与原生侧 `ATRNBridge` 区别开**，测试时一眼分清是 RN 层还是侨联层日志），
 * 调用方只传 scope（模块）+ 方法/回调名 + 参数，不重复写 tag。
 */
class ATLogger {
  enabled = false;

  /** RN 层统一 tag——刻意与原生 `ATRNBridge` 不同，便于按层过滤。 */
  private static readonly TAG = 'ATRN-JS';

  /** 调用日志：`[ATRN-JS] scope.method  args`。 */
  call(scope: string, method: string, ...args: unknown[]): void {
    if (!this.enabled) {
      return;
    }
    const head = `[${ATLogger.TAG}] ${scope}.${method}`;
    if (args.length > 0) {
      console.log(head, ...args);
    } else {
      console.log(head);
    }
  }

  /** 回调日志：`[ATRNBridge] scope ← callbackName  payload`。 */
  callback(scope: string, callbackName: string, payload?: unknown): void {
    if (!this.enabled) {
      return;
    }
    const head = `[${ATLogger.TAG}] ${scope} ← ${callbackName}`;
    if (payload !== undefined) {
      console.log(head, payload);
    } else {
      console.log(head);
    }
  }
}

export const ATLog = new ATLogger();
