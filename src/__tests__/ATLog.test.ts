import { ATLog } from '../utils/ATLog';

describe('ATLog（受 enabled 开关控制）', () => {
  let spy: jest.SpyInstance;
  beforeEach(() => {
    spy = jest.spyOn(console, 'log').mockImplementation(() => {});
    ATLog.enabled = false;
  });
  afterEach(() => {
    spy.mockRestore();
    ATLog.enabled = false;
  });

  it('enabled=false 时 call/callback 不输出', () => {
    ATLog.call('S', 'm', 1);
    ATLog.callback('S', 'cb', { a: 1 });
    expect(spy).not.toHaveBeenCalled();
  });

  it('call 有参 → 打 head + args；无参 → 只打 head', () => {
    ATLog.enabled = true;
    ATLog.call('Scope', 'method', 1, 2);
    expect(spy).toHaveBeenLastCalledWith('[ATRN-JS] Scope.method', 1, 2);
    ATLog.call('Scope', 'noArgs');
    expect(spy).toHaveBeenLastCalledWith('[ATRN-JS] Scope.noArgs');
  });

  it('callback 有 payload → 打 head + payload；无 → 只打 head', () => {
    ATLog.enabled = true;
    ATLog.callback('Scope', 'cbName', { x: 1 });
    expect(spy).toHaveBeenLastCalledWith('[ATRN-JS] Scope ← cbName', { x: 1 });
    ATLog.callback('Scope', 'cbOnly');
    expect(spy).toHaveBeenLastCalledWith('[ATRN-JS] Scope ← cbOnly');
  });
});
