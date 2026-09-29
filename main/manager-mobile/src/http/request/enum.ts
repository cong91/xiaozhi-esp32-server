export enum ResultEnum {
  Success = 0, // 成功
  Error = 400, // 错误
  Unauthorized = 401, // 未授权
  Forbidden = 403, // 禁止访问（原为forbidden）
  NotFound = 404, // 未找到（原为notFound）
  MethodNotAllowed = 405, // 方法不允许（原为methodNotAllowed）
  RequestTimeout = 408, // 请求超时（原为requestTimeout）
  InternalServerError = 500, // 服务器错误（原为internalServerError）
  NotImplemented = 501, // 未实现（原为notImplemented）
  BadGateway = 502, // 网关错误（原为badGateway）
  ServiceUnavailable = 503, // 服务不可用（原为serviceUnavailable）
  GatewayTimeout = 504, // 网关超时（原为gatewayTimeout）
  HttpVersionNotSupported = 505, // HTTP版本不支持（原为httpVersionNotSupported）
  MixedContent = 600, // 混合内容错误（HTTPS页面请求HTTP接口）
}
export enum ContentTypeEnum {
  JSON = 'application/json;charset=UTF-8',
  FORM_URLENCODED = 'application/x-www-form-urlencoded;charset=UTF-8',
  FORM_DATA = 'multipart/form-data;charset=UTF-8',
}
export function ShowMessage(status: number | string, translate: (key: string, params?: Record<string, string | number>) => string): string {
  return translate('message.httpStatusError', { status })
}
