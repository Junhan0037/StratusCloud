package com.stratuscloud.api.system

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SystemPingControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    // 테스트가 외부 의존성 없이 컨트롤러 동작만 검증하도록 서비스는 목킹한다.
    @MockitoBean
    private lateinit var pingService: PingService

    @Test
    fun `ping API는 pong 과 traceId를 반환해야 한다`() {
        val response = PingResponse(
            message = "pong",
            traceId = "0123456789abcdef0123456789abcdef"
        )

        org.mockito.BDDMockito.given(pingService.ping()).willReturn(response)

        mockMvc.get("/v1/system/ping")
            .andExpect {
                status { isOk() }
                jsonPath("$.message") { value("pong") }
                jsonPath("$.traceId") { value("0123456789abcdef0123456789abcdef") }
            }
    }
}
