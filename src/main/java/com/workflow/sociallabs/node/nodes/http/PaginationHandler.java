package com.workflow.sociallabs.node.nodes.http;

import com.workflow.sociallabs.node.nodes.http.parameters.HttpRequestParameters;
import com.workflow.sociallabs.node.nodes.http.parameters.HttpRequestParameters.PaginationMode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;

/**
 * Handles multi-page API calls.
 *
 * <p>Підтримує два режими:
 * <ul>
 *   <li>UPDATE_PARAM — інкрементує числовий параметр (page, offset, skip…)</li>
 *   <li>RESPONSE_URL — бере наступний URL зі SpEL-виразу над тілом відповіді</li>
 * </ul>
 */
@Slf4j
@Component
public class PaginationHandler {

    private final ExpressionParser spelParser = new SpelExpressionParser();

    /**
     * Виконати paginated запит.
     *
     * @param params      параметри ноди
     * @param pageRunner  функція (urlOverride, paramValue) → List<Map> (одна сторінка)
     * @return            всі записи з усіх сторінок
     */
    public List<Map<String, Object>> paginate(
            HttpRequestParameters params,
            PageRunner pageRunner
    ) throws Exception {

        if (params.getPaginationMode() == PaginationMode.OFF) {
            return pageRunner.run(null, null);
        }

        List<Map<String, Object>> allItems = new ArrayList<>();
        int page = 0;
        int maxPages = params.getPaginationMaxRequests() != null
                ? params.getPaginationMaxRequests()
                : 100;

        String currentUrl = null;
        Object currentParamValue = params.getPaginationStartValue() != null
                ? params.getPaginationStartValue()
                : 0;

        while (page < maxPages) {
            List<Map<String, Object>> pageItems = switch (params.getPaginationMode()) {
                case UPDATE_PARAM -> pageRunner.run(null, currentParamValue);
                case RESPONSE_URL -> pageRunner.run(currentUrl, null);
                default -> List.of();
            };

            if (pageItems == null || pageItems.isEmpty()) {
                log.debug("Pagination: empty page at page={}, stopping", page);
                break;
            }

            allItems.addAll(pageItems);
            page++;

            // Calculate next param / URL
            if (params.getPaginationMode() == PaginationMode.UPDATE_PARAM) {
                int increment = params.getPaginationIncrement() != null
                        ? params.getPaginationIncrement()
                        : 1;
                currentParamValue = ((Number) currentParamValue).intValue() + increment;
            } else if (params.getPaginationMode() == PaginationMode.RESPONSE_URL) {
                // Evaluate SpEL against last page's first item
                currentUrl = evaluateNextUrl(params.getNextUrlExpression(), pageItems.get(0));
                if (currentUrl == null || currentUrl.isBlank()) {
                    log.debug("Pagination: no next URL in response, stopping");
                    break;
                }
            }

            if (Boolean.TRUE.equals(params.getPaginationStopOnEmpty())
                    && pageItems.isEmpty()) {
                break;
            }
        }

        log.debug("Pagination: fetched {} total items across {} pages", allItems.size(), page);
        return allItems;
    }

    /**
     * Evaluate SpEL expression over last response body.
     * Expression example: "$response.body.meta.next_cursor"
     * Accessible root variables: body (Map), statusCode (int), headers (Map)
     */
    private String evaluateNextUrl(String expression, Map<String, Object> responseItem) {
        if (expression == null || expression.isBlank()) return null;
        try {
            StandardEvaluationContext ctx = new StandardEvaluationContext();
            ctx.setVariable("response", responseItem);
            ctx.setVariable("body", responseItem.get("body"));
            ctx.setVariable("statusCode", responseItem.getOrDefault("statusCode", 200));
            ctx.setVariable("headers", responseItem.getOrDefault("headers", Map.of()));
            Object result = spelParser.parseExpression(expression).getValue(ctx);
            return result != null ? result.toString() : null;
        } catch (Exception e) {
            log.warn("Failed to evaluate pagination expression '{}': {}", expression, e.getMessage());
            return null;
        }
    }

    /**
     * Функціональний інтерфейс для однієї сторінки.
     * Реалізується всередині HttpRequestService.
     */
    @FunctionalInterface
    public interface PageRunner {
        List<Map<String, Object>> run(String urlOverride, Object paramValue) throws Exception;
    }
}
