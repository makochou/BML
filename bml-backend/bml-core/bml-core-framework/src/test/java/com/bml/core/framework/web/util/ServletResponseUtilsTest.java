package com.bml.core.framework.web.util;

import com.bml.core.common.enums.GlobalErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServletResponseUtilsTest {

    @Test
    void shouldWriteUnifiedFailurePayload() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        ObjectMapper objectMapper = new ObjectMapper();

        ServletResponseUtils.writeFailure(
                response,
                objectMapper,
                401,
                GlobalErrorCode.UNAUTHORIZED);

        JsonNode jsonNode = objectMapper.readTree(response.getContentAsString());

        assertEquals(401, response.getStatus());
        assertEquals(GlobalErrorCode.UNAUTHORIZED.getCode(), jsonNode.get("code").asInt());
        assertEquals(GlobalErrorCode.UNAUTHORIZED.getMessage(), jsonNode.get("message").asText());
        assertTrue(jsonNode.has("timestamp"));
    }
}
