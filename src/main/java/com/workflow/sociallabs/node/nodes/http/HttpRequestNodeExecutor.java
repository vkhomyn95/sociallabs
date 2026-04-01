package com.workflow.sociallabs.node.nodes.http;

import com.workflow.sociallabs.model.NodeDiscriminator;
import com.workflow.sociallabs.node.base.AbstractNode;
import com.workflow.sociallabs.node.core.ExecutionContext;
import com.workflow.sociallabs.node.core.NodeResult;
import com.workflow.sociallabs.node.core.WorkflowItem;
import com.workflow.sociallabs.node.nodes.http.parameters.HttpRequestParameters;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Executor для HTTP Request ноди.
 *
 * <p>Особливість: нода підтримує batching (batchSize > 1) і пагінацію.
 * Тому вона безпосередньо override executeInternal, а не лише processItem.
 */
@Slf4j
@Component
//@RequiredArgsConstructor
public class HttpRequestNodeExecutor extends AbstractNode {

    private final HttpRequestService httpService;

    public HttpRequestNodeExecutor() {
        super(NodeDiscriminator.HTTP_REQUEST);
        // Spring ін'єктує через @RequiredArgsConstructor, але для super() потрібен no-arg
        // → вирішується через field injection нижче
        this.httpService = null;
    }

    // Правильний конструктор — Spring використає цей
    public HttpRequestNodeExecutor(HttpRequestService httpService) {
        super(NodeDiscriminator.HTTP_REQUEST);
        this.httpService = httpService;
    }

    @Override
    public boolean requiresCredentials() {
        return false; // Auth вбудована в параметри
    }

    @Override
    protected NodeResult executeInternal(ExecutionContext context) throws Exception {
        HttpRequestParameters params = context.getParameters(HttpRequestParameters.class);

        List<WorkflowItem> inputItems = context.getInputItems();
        if (inputItems == null || inputItems.isEmpty()) {
            // Запустити один раз без input
            inputItems = List.of(new WorkflowItem(Map.of(), null));
        }

        List<WorkflowItem> outputItems = new ArrayList<>();
        int batchSize = params.getBatchSize() != null ? params.getBatchSize() : 1;
        int batchInterval = params.getBatchInterval() != null ? params.getBatchInterval() : 0;

        // Process in batches
        for (int i = 0; i < inputItems.size(); i += batchSize) {
            List<WorkflowItem> batch = inputItems.subList(i, Math.min(i + batchSize, inputItems.size()));

            for (WorkflowItem item : batch) {
                try {
                    List<Map<String, Object>> results = httpService.execute(params, item.json());
                    results.forEach(r -> outputItems.add(new WorkflowItem(r, null)));
                } catch (Exception e) {
                    if (Boolean.TRUE.equals(params.getContinueOnFail())) {
                        log.warn("HTTP node failed for item, continuing: {}", e.getMessage());
                        Map<String, Object> errItem = new java.util.LinkedHashMap<>(item.json());
                        errItem.put("_error", e.getMessage());
                        outputItems.add(new WorkflowItem(errItem, null));
                    } else {
                        throw e;
                    }
                }
            }

            // Batch interval between batches (not after last)
            if (batchInterval > 0 && i + batchSize < inputItems.size()) {
                Thread.sleep(batchInterval);
            }
        }

        return NodeResult.success(outputItems);
    }
}
