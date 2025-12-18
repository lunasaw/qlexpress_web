package com.ql.qlexpress.web.liteflow.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ql.qlexpress.web.liteflow.model.FlowDesign;
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
 * 流程设计仓储
 * 内存存储 + JSON 文件持久化
 */
@Slf4j
@Repository
public class FlowDesignRepository {

    /**
     * 流程存储 (flowId -> FlowDesign)
     */
    private final Map<String, FlowDesign> flowStore = new ConcurrentHashMap<>();

    /**
     * 分类索引 (category -> Set<flowId>)
     */
    private final Map<String, Set<String>> categoryIndex = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper;

    @Value("${qlexpress.flow.storage-path:data/flows.json}")
    private String storagePath;

    @Value("${qlexpress.flow.auto-persist:true}")
    private boolean autoPersist;

    public FlowDesignRepository() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @PostConstruct
    public void init() {
        loadFromFile();
        log.info("流程仓储初始化完成，加载了 {} 个流程", flowStore.size());
    }

    // ==================== CRUD 操作 ====================

    /**
     * 保存流程
     */
    public FlowDesign save(FlowDesign flow) {
        String flowId = flow.getFlowId();

        // 如果是更新，先从旧分类索引中移除
        FlowDesign existing = flowStore.get(flowId);
        if (existing != null && existing.getCategory() != null) {
            Set<String> oldCategorySet = categoryIndex.get(existing.getCategory());
            if (oldCategorySet != null) {
                oldCategorySet.remove(flowId);
            }
        }

        // 保存流程
        flowStore.put(flowId, flow);

        // 更新分类索引
        if (flow.getCategory() != null) {
            categoryIndex.computeIfAbsent(flow.getCategory(), k -> ConcurrentHashMap.newKeySet())
                    .add(flowId);
        }

        // 自动持久化
        if (autoPersist) {
            persistToFile();
        }

        log.debug("保存流程: flowId={}, category={}", flowId, flow.getCategory());
        return flow;
    }

    /**
     * 根据 ID 查找流程
     */
    public Optional<FlowDesign> findById(String flowId) {
        return Optional.ofNullable(flowStore.get(flowId));
    }

    /**
     * 检查流程是否存在
     */
    public boolean existsById(String flowId) {
        return flowStore.containsKey(flowId);
    }

    /**
     * 获取所有流程
     */
    public List<FlowDesign> findAll() {
        return new ArrayList<>(flowStore.values());
    }

    /**
     * 根据分类查找流程
     */
    public List<FlowDesign> findByCategory(String category) {
        Set<String> flowIds = categoryIndex.get(category);
        if (flowIds == null || flowIds.isEmpty()) {
            return Collections.emptyList();
        }
        return flowIds.stream()
                .map(flowStore::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 根据启用状态查找
     */
    public List<FlowDesign> findByEnabled(boolean enabled) {
        return flowStore.values().stream()
                .filter(f -> f.isEnabled() == enabled)
                .collect(Collectors.toList());
    }

    /**
     * 根据部署状态查找
     */
    public List<FlowDesign> findByDeployStatus(FlowDesign.DeployStatus status) {
        return flowStore.values().stream()
                .filter(f -> f.getDeployStatus() == status)
                .collect(Collectors.toList());
    }

    /**
     * 删除流程
     */
    public void deleteById(String flowId) {
        FlowDesign removed = flowStore.remove(flowId);
        if (removed != null && removed.getCategory() != null) {
            Set<String> categorySet = categoryIndex.get(removed.getCategory());
            if (categorySet != null) {
                categorySet.remove(flowId);
            }
        }

        if (autoPersist) {
            persistToFile();
        }

        log.debug("删除流程: flowId={}", flowId);
    }

    /**
     * 删除所有流程
     */
    public void deleteAll() {
        flowStore.clear();
        categoryIndex.clear();

        if (autoPersist) {
            persistToFile();
        }

        log.info("已删除所有流程");
    }

    /**
     * 统计流程数量
     */
    public long count() {
        return flowStore.size();
    }

    // ==================== 分类相关 ====================

    /**
     * 获取所有分类
     */
    public Set<String> getAllCategories() {
        return new HashSet<>(categoryIndex.keySet());
    }

    /**
     * 统计各分类的流程数量
     */
    public Map<String, Integer> countByCategory() {
        Map<String, Integer> result = new HashMap<>();
        for (Map.Entry<String, Set<String>> entry : categoryIndex.entrySet()) {
            result.put(entry.getKey(), entry.getValue().size());
        }
        return result;
    }

    // ==================== 搜索 ====================

    /**
     * 搜索流程 (按名称或描述模糊匹配)
     */
    public List<FlowDesign> search(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return findAll();
        }
        String lowerKeyword = keyword.toLowerCase();
        return flowStore.values().stream()
                .filter(f -> {
                    String name = f.getFlowName();
                    String desc = f.getDescription();
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

            List<FlowDesign> flows = new ArrayList<>(flowStore.values());
            objectMapper.writeValue(file, flows);
            log.debug("流程持久化完成: {} 个流程 -> {}", flows.size(), storagePath);
        } catch (IOException e) {
            log.error("流程持久化失败: {}", storagePath, e);
        }
    }

    /**
     * 从文件加载
     */
    public synchronized void loadFromFile() {
        File file = new File(storagePath);
        if (!file.exists()) {
            log.info("流程存储文件不存在，跳过加载: {}", storagePath);
            return;
        }

        try {
            List<FlowDesign> flows = objectMapper.readValue(file, new TypeReference<List<FlowDesign>>() {});
            flowStore.clear();
            categoryIndex.clear();

            for (FlowDesign flow : flows) {
                flowStore.put(flow.getFlowId(), flow);
                if (flow.getCategory() != null) {
                    categoryIndex.computeIfAbsent(flow.getCategory(), k -> ConcurrentHashMap.newKeySet())
                            .add(flow.getFlowId());
                }
            }

            log.info("从文件加载流程: {} 个 <- {}", flows.size(), storagePath);
        } catch (IOException e) {
            log.error("从文件加载流程失败: {}", storagePath, e);
        }
    }

    /**
     * 强制刷新到文件
     */
    public void flush() {
        persistToFile();
    }
}
