package com.ql.qlexpress.web.liteflow.context;

import com.yomahub.liteflow.context.ContextBean;

import java.util.concurrent.ConcurrentHashMap;

/**
 * LiteFlow 通用上下文
 * 基于 Map 实现，支持动态存取任意数据
 * QLExpress 脚本通过 flowContext 访问
 */
@ContextBean("flowContext")
public class FlowContext extends ConcurrentHashMap<String, Object> {

    /**
     * 获取值并转换类型
     */
    @SuppressWarnings("unchecked")
    public <T> T getData(String key) {
        return (T) get(key);
    }

    /**
     * 获取值，不存在时返回默认值
     */
    @SuppressWarnings("unchecked")
    public <T> T getData(String key, T defaultValue) {
        Object value = get(key);
        return value != null ? (T) value : defaultValue;
    }

    /**
     * 设置值
     */
    public FlowContext setData(String key, Object value) {
        put(key, value);
        return this;
    }

    /**
     * 获取 String
     */
    public String getString(String key) {
        Object value = get(key);
        return value != null ? String.valueOf(value) : null;
    }

    /**
     * 获取 String，带默认值
     */
    public String getString(String key, String defaultValue) {
        Object value = get(key);
        return value != null ? String.valueOf(value) : defaultValue;
    }

    /**
     * 获取 Integer
     */
    public Integer getInt(String key) {
        Object value = get(key);
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).intValue();
        return Integer.parseInt(String.valueOf(value));
    }

    /**
     * 获取 Integer，带默认值
     */
    public int getInt(String key, int defaultValue) {
        Integer value = getInt(key);
        return value != null ? value : defaultValue;
    }

    /**
     * 获取 Long
     */
    public Long getLong(String key) {
        Object value = get(key);
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).longValue();
        return Long.parseLong(String.valueOf(value));
    }

    /**
     * 获取 Long，带默认值
     */
    public long getLong(String key, long defaultValue) {
        Long value = getLong(key);
        return value != null ? value : defaultValue;
    }

    /**
     * 获取 Double
     */
    public Double getDouble(String key) {
        Object value = get(key);
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).doubleValue();
        return Double.parseDouble(String.valueOf(value));
    }

    /**
     * 获取 Double，带默认值
     */
    public double getDouble(String key, double defaultValue) {
        Double value = getDouble(key);
        return value != null ? value : defaultValue;
    }

    /**
     * 获取 Boolean
     */
    public Boolean getBoolean(String key) {
        Object value = get(key);
        if (value == null) return null;
        if (value instanceof Boolean) return (Boolean) value;
        return Boolean.parseBoolean(String.valueOf(value));
    }

    /**
     * 获取 Boolean，带默认值
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        Boolean value = getBoolean(key);
        return value != null ? value : defaultValue;
    }

    /**
     * 检查 key 是否存在
     */
    public boolean hasKey(String key) {
        return containsKey(key);
    }

    /**
     * 移除并返回值
     */
    @SuppressWarnings("unchecked")
    public <T> T removeData(String key) {
        return (T) remove(key);
    }
}
