// @ts-ignore
/* eslint-disable */
import { request } from '@umijs/max';

/** doDailyCheckIn POST /api/dailyCheckIn/doCheckIn */
export async function doDailyCheckInUsingPost(options?: { [key: string]: any }) {
  return request<API.BaseResponseBoolean_>('/api/dailyCheckIn/doCheckIn', {
    method: 'POST',
    ...(options || {}),
  });
}

/** doDailyCheckIn1 POST /api/dailyCheckIn/doCheckIn1 */
export async function doDailyCheckIn1UsingPost(options?: { [key: string]: any }) {
  return request<API.BaseResponseBoolean_>('/api/dailyCheckIn/doCheckIn1', {
    method: 'POST',
    ...(options || {}),
  });
}
