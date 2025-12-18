package com.ql.qlexpress.web.liteflow.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.ql.qlexpress.web.liteflow.model.QLComponent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * QLExpress 组件仓储
 * 内存存储 + JSON 文件持久化
 */
@Slf4j
@Repository
public class QLComponentRepository {

    /**
     * 组件存储 (componentId -> QLComponent)
     */
    private final Map<String, QLComponent> componentStore = new ConcurrentHashMap<>();

    /**
     * 分类索引 (category -> Set<componentId>)
     */
    private final Map<String, Set<String>> categoryIndex = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper;

    @Value("${qlexpress.component.storage-path:data/components.json}")
    private String storagePath;

    @Value("${qlexpress.component.auto-persist:true}")
    private boolean autoPersist;

    public QLComponentRepository() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    @PostConstruct
    public void init() {
        loadFromFile();
        log.info("组件仓储初始化完成，加载了 {} 个组件", componentStore.size());
    }

    // ==================== CRUD 操作 ====================

    /**
     * 保存组件
     */
    public QLComponent save(QLComponent component) {
        String componentId = component.getComponentId();

        // 如果是更新，先从旧分类索引中移除
        QLComponent existing = componentStore.get(componentId);
        if (existing != null && existing.getCategory() != null) {
            Set<String> oldCategorySet = categoryIndex.get(existing.getCategory());
            if (oldCategorySet != null) {
                oldCategorySet.remove(componentId);
            }
        }

        // 保存组件
        componentStore.put(componentId, component);

        // 更新分类索引
        if (component.getCategory() != null) {
            categoryIndex.computeIfAbsent(component.getCategory(), k -> ConcurrentHashMap.newKeySet())
                    .add(componentId);
        }

        // 自动持久化
        if (autoPersist) {
            persistToFile();
        }

        log.debug("保存组件: componentId={}, category={}", componentId, component.getCategory());
        return component;
    }

    /**
     * 批量保存组件
     */
    public List<QLComponent> saveAll(List<QLComponent> components) {
        for (QLComponent component : components) {
            save(component);
        }
        return components;
    }

    /**
     * 根据 ID 查找组件
     */
    public Optional<QLComponent> findById(String componentId) {
        return Optional.ofNullable(componentStore.get(componentId));
    }

    /**
     * 检查组件是否存在
     */
    public boolean existsById(String componentId) {
        return componentStore.containsKey(componentId);
    }

    /**
     * 获取所有组件
     */
    public List<QLComponent> findAll() {
        return new ArrayList<>(componentStore.values());
    }

    /**
     * 根据分类查找组件
     */
    public List<QLComponent> findByCategory(String category) {
        Set<String> componentIds = categoryIndex.get(category);
        if (componentIds == null || componentIds.isEmpty()) {
            return Collections.emptyList();
        }
        return componentIds.stream()
                .map(componentStore::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 根据组件类型查找
     */
    public List<QLComponent> findByComponentType(QLComponent.ComponentType componentType) {
        return componentStore.values().stream()
                .filter(c -> c.getComponentType() == componentType)
                .collect(Collectors.toList());
    }

    /**
     * 根据启用状态查找
     */
    public List<QLComponent> findByEnabled(boolean enabled) {
        return componentStore.values().stream()
                .filter(c -> c.isEnabled() == enabled)
                .collect(Collectors.toList());
    }

    /**
     * 删除组件
     */
    public void deleteById(String componentId) {
        QLComponent removed = componentStore.remove(componentId);
        if (removed != null && removed.getCategory() != null) {
            Set<String> categorySet = categoryIndex.get(removed.getCategory());
            if (categorySet != null) {
                categorySet.remove(componentId);
            }
        }

        if (autoPersist) {
            persistToFile();
        }

        log.debug("删除组件: componentId={}", componentId);
    }

    /**
     * 删除所有组件
     */
    public void deleteAll() {
        componentStore.clear();
        categoryIndex.clear();

        if (autoPersist) {
            persistToFile();
        }

        log.info("已删除所有组件");
    }

    /**
     * 统计组件数量
     */
    public long count() {
        return componentStore.size();
    }

    // ==================== 分类相关 ====================

    /**
     * 获取所有分类
     */
    public Set<String> getAllCategories() {
        return new HashSet<>(categoryIndex.keySet());
    }

    /**
     * 统计各分类的组件数量
     */
    public Map<String, Integer> countByCategory() {
        Map<String, Integer> result = new HashMap<>();
        for (Map.Entry<String, Set<String>> entry : categoryIndex.entrySet()) {
            result.put(entry.getKey(), entry.getValue().size());
        }
        return result;
    }

    // ==================== 批量查询 ====================

    /**
     * 根据多个 ID 查找组件
     */
    public List<QLComponent> findByIds(Collection<String> componentIds) {
        return componentIds.stream()
                .map(componentStore::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 搜索组件 (按名称或描述模糊匹配)
     */
    public List<QLComponent> search(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return findAll();
        }
        String lowerKeyword = keyword.toLowerCase();
        return componentStore.values().stream()
                .filter(c -> {
                    String name = c.getComponentName();
                    String desc = c.getDescription();
                    return (name != null && name.toLowerCase().contains(lowerKeyword))
                            || (desc != null && desc.toLowerCase().contains(lowerKeyword));
                })
                .collect(Collectors.toList());
    }

    // ==================== 持久化 ====================

    /**
     * 持久化到文件
     */
    public synchronized void persistToFile() {
        try {
            File file = new File(storagePath);
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            List<QLComponent> components = new ArrayList<>(componentStore.values());
            objectMapper.writeValue(file, components);
            log.debug("组件持久化完成: {} 个组件 -> {}", components.size(), storagePath);
        } catch (IOException e) {
            log.error("组件持久化失败: {}", storagePath, e);
        }
    }

    /**
     * 从文件加载
     */
    public synchronized void loadFromFile() {
        File file = new File(storagePath);
        if (!file.exists()) {
            log.info("组件存储文件不存在，跳过加载: {}", storagePath);
            return;
        }

        try {
            List<QLComponent> components = objectMapper.readValue(file, new TypeReference<List<QLComponent>>() {});
            componentStore.clear();
            categoryIndex.clear();

            for (QLComponent component : components) {
                componentStore.put(component.getComponentId(), component);
                if (component.getCategory() != null) {
                    categoryIndex.computeIfAbsent(component.getCategory(), k -> ConcurrentHashMap.newKeySet())
                            .add(component.getComponentId());
                }
            }

            log.info("从文件加载组件: {} 个 <- {}", components.size(), storagePath);
        } catch (IOException e) {
            log.error("从文件加载组件失败: {}", storagePath, e);
        }
    }

    /**
     * 强制刷新到文件
     */
    public void flush() {
        persistToFile();
    }
}
