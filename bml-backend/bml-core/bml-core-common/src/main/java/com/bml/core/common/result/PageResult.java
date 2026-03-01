package com.bml.core.common.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * 统一分页结果对象。
 * <p>
 * 该对象不依赖具体 ORM 框架，仅表达分页接口对外返回的最小稳定协议，
 * 便于各业务模块在保持统一响应结构的前提下复用。
 * </p>
 *
 * @param <T> 列表记录类型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 当前页记录列表。
     */
    private List<T> records;

    /**
     * 总记录数。
     */
    private long total;

    /**
     * 当前页码，从 1 开始。
     */
    private long pageNum;

    /**
     * 每页大小。
     */
    private long pageSize;

    public static <T> PageResult<T> empty(long pageNum, long pageSize) {
        return PageResult.<T>builder()
                .records(Collections.emptyList())
                .total(0L)
                .pageNum(pageNum)
                .pageSize(pageSize)
                .build();
    }

    public static <T> PageResult<T> of(List<T> records, long total, long pageNum, long pageSize) {
        return PageResult.<T>builder()
                .records(records)
                .total(total)
                .pageNum(pageNum)
                .pageSize(pageSize)
                .build();
    }
}
