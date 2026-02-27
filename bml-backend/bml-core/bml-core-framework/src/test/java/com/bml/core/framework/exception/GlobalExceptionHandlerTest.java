package com.bml.core.framework.exception;

import com.bml.core.common.enums.GlobalErrorCode;
import com.bml.core.common.exception.BusinessException;
import com.bml.core.common.result.Result;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void shouldReturnBadRequestForDefaultBusinessException() {
        Result<Void> result = handler.handleBusinessException(
                new BusinessException("业务规则不满足"),
                new MockHttpServletRequest());

        assertEquals(GlobalErrorCode.BAD_REQUEST.getCode(), result.getCode());
        assertEquals("业务规则不满足", result.getMessage());
    }

    @Test
    void shouldReturnInternalServerErrorForUnknownException() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/test");

        Result<Void> result = handler.handleException(new RuntimeException("boom"), request);

        assertEquals(GlobalErrorCode.INTERNAL_SERVER_ERROR.getCode(), result.getCode());
        assertEquals(GlobalErrorCode.INTERNAL_SERVER_ERROR.getMessage(), result.getMessage());
    }
}
