import axios, { type AxiosInstance, type AxiosRequestConfig, type AxiosResponse } from 'axios';
import type { ApiResponse } from '../types';

/**
 * 创建 Axios 实例
 */
const createClient = (): AxiosInstance => {
  const instance = axios.create({
    baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
    timeout: 30000,
    headers: {
      'Content-Type': 'application/json',
    },
  });

  // 请求拦截器
  instance.interceptors.request.use(
    (config) => {
      // 可以在这里添加 token 等认证信息
      // const token = localStorage.getItem('token');
      // if (token) {
      //   config.headers.Authorization = `Bearer ${token}`;
      // }
      return config;
    },
    (error) => {
      return Promise.reject(error);
    }
  );

  // 响应拦截器
  instance.interceptors.response.use(
    (response: AxiosResponse<ApiResponse>) => {
      const data = response.data;

      // 检查业务状态
      if (data.success === false) {
        const error = new Error(data.message || '请求失败');
        (error as any).code = data.code;
        (error as any).response = response;
        return Promise.reject(error);
      }

      return response;
    },
    (error) => {
      // 处理 HTTP 错误
      if (error.response) {
        const { status, data } = error.response;
        let message = data?.message || '请求失败';

        switch (status) {
          case 400:
            message = data?.message || '请求参数错误';
            break;
          case 401:
            message = '未授权，请登录';
            // 可以在这里跳转到登录页
            break;
          case 403:
            message = '权限不足';
            break;
          case 404:
            message = '资源不存在';
            break;
          case 500:
            message = data?.message || '服务器内部错误';
            break;
          default:
            message = data?.message || `请求失败 (${status})`;
        }

        error.message = message;
      } else if (error.code === 'ECONNABORTED') {
        error.message = '请求超时';
      } else if (!error.response) {
        error.message = '网络错误';
      }

      return Promise.reject(error);
    }
  );

  return instance;
};

/**
 * API 客户端实例
 */
export const apiClient = createClient();

/**
 * 通用请求方法
 */
export const request = {
  get<T = unknown>(url: string, config?: AxiosRequestConfig): Promise<AxiosResponse<ApiResponse<T>>> {
    return apiClient.get(url, config);
  },

  post<T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<AxiosResponse<ApiResponse<T>>> {
    return apiClient.post(url, data, config);
  },

  put<T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<AxiosResponse<ApiResponse<T>>> {
    return apiClient.put(url, data, config);
  },

  delete<T = unknown>(url: string, config?: AxiosRequestConfig): Promise<AxiosResponse<ApiResponse<T>>> {
    return apiClient.delete(url, config);
  },

  patch<T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<AxiosResponse<ApiResponse<T>>> {
    return apiClient.patch(url, data, config);
  },
};

export default apiClient;
