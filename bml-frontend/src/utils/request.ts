import axios from 'axios';
import { Message } from '@arco-design/web-vue';

const service = axios.create({
    baseURL: import.meta.env.VITE_API_BASE_URL || '/api', // 使用反向代理
    timeout: 10000
});

// Request interceptor
service.interceptors.request.use(
    config => {
        const token = localStorage.getItem('token');
        if (token) {
            config.headers['Authorization'] = `Bearer ${token}`;
        }
        return config;
    },
    error => {
        return Promise.reject(error);
    }
);

// Response interceptor
service.interceptors.response.use(
    response => {
        const res = response.data;
        if (res.code === 401) {
            Message.error('登录失效，请重新登录');
            localStorage.removeItem('token');
            window.location.href = '/admin/login';
            return Promise.reject(new Error('Unauthorized'));
        }
        if (res.code !== 200) {
            Message.error(res.msg || 'Error');
            return Promise.reject(new Error(res.msg || 'Error'));
        }
        return res;
    },
    error => {
        Message.error(error.message || 'Request Error');
        return Promise.reject(error);
    }
);

export default service;
