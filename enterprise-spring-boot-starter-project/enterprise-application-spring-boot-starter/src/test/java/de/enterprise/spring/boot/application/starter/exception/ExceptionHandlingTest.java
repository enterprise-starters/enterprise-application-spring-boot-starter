package de.enterprise.spring.boot.application.starter.exception;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.function.Supplier;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.enterprise.spring.boot.application.starter.exception.testapp.ExceptionHandlingTestApplication;
import de.enterprise.spring.boot.application.starter.exception.testapp.ExceptionTestController.DataContainer;

@SpringBootTest(classes = ExceptionHandlingTestApplication.class)
@AutoConfigureMockMvc
public class ExceptionHandlingTest {

	@Autowired
	private MockMvc mvc;
	@Autowired
	private ObjectMapper objectMapper;

	@Test
	public void resourceNotFoundException_withoutCode() throws Exception {
		this.mvc.perform(get("/exceptions?exception=ResourceNotFoundException").with(httpBasic("test", "hallo")))
				.andDo(print())
				.andExpect(status().isNotFound())
				// ResourceNotFoundException without code results in a empty response body
				.andExpect(MockMvcResultMatchers.content().string(Matchers.isEmptyOrNullString()));
	}

	@Test
	public void resourceNotFoundException_withCode() throws Exception {
		String code = "test.code";
		this.mvc.perform(get("/exceptions?exception=ResourceNotFoundException&code=" + code)
				.with(httpBasic("test", "hallo")))
				.andDo(print())
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$").isNotEmpty())
				.andExpect(jsonPath("$.*", Matchers.hasSize(1)))
				.andExpect(jsonPath("$.code", Matchers.is(code)));
	}

	@Test
	public void resourceNotFoundException_withCode_andMessage() throws Exception {
		testExceptionHandling("ResourceNotFoundException", "test.code", "message", () -> status().isNotFound());
	}

	@Test
	public void technicalException() throws Exception {
		testExceptionHandling("TechnicalException", "test.code", "message", () -> status().isInternalServerError());
	}

	@Test
	public void badRequestException() throws Exception {
		testExceptionHandling("BadRequestException", "test.code", "message", () -> status().isBadRequest());
	}

	@Test
	public void validationException() throws Exception {
		testExceptionHandling("ValidationException", "test.code", "message", () -> status().isUnprocessableEntity());
	}

	@Test
	public void accessDeniedException() throws Exception {
		testExceptionHandling("AccessDeniedException", "403", "message", () -> status().isForbidden());
	}

	@Test
	public void runtimeException() throws Exception {
		testExceptionHandling("RuntimeException", "500", "message", () -> status().isInternalServerError());
	}

	private void testExceptionHandling(String exceptionUrlPath, String code, String message,
			Supplier<ResultMatcher> statusMatcher) throws Exception {

		// no accept header -> JSON
		this.mvc.perform(
				get("/exceptions?exception=" + exceptionUrlPath + "&code=" + code + "&message=" + message)
						.with(httpBasic("test", "hallo")))
				.andDo(print())
				.andExpect(statusMatcher.get())
				.andExpect(MockMvcResultMatchers.header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
				.andExpect(jsonPath("$").isNotEmpty())
				.andExpect(jsonPath("$.*", Matchers.hasSize(2)))
				.andExpect(jsonPath("$.code", Matchers.is(code)))
				.andExpect(jsonPath("$.message", Matchers.is(message)));

		// other but application/xml accept header -> JSON
		this.mvc.perform(
				get("/exceptions?exception=" + exceptionUrlPath + "&code=" + code + "&message=" + message)
						.with(httpBasic("test", "hallo"))
						.accept(MediaType.TEXT_HTML))
				.andDo(print())
				.andExpect(statusMatcher.get())
				.andExpect(MockMvcResultMatchers.header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
				.andExpect(jsonPath("$").isNotEmpty())
				.andExpect(jsonPath("$.code", Matchers.is(code)))
				.andExpect(jsonPath("$.message", Matchers.is(message)));

		// application/xml accept header -> XML
		this.mvc.perform(
				get("/exceptions?exception=" + exceptionUrlPath + "&code=" + code + "&message=" + message)
						.with(httpBasic("test", "hallo")).accept(MediaType.APPLICATION_XML))
				.andDo(print())
				.andExpect(statusMatcher.get())
				.andExpect(MockMvcResultMatchers.header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML_VALUE))
				.andExpect(
						content().xml("<ExceptionWrapper><code>" + code + "</code><message>" + message + "</message></ExceptionWrapper>"));

		// application header with comma separated content containing application/xml
		this.mvc.perform(
				get("/exceptions?exception=" + exceptionUrlPath + "&code=" + code + "&message=" + message)
						.with(httpBasic("test", "hallo")).header("Accept", "text/html,application/xml"))
				.andDo(print())
				.andExpect(statusMatcher.get())
				.andExpect(MockMvcResultMatchers.header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML_VALUE))
				.andExpect(
						content().xml("<ExceptionWrapper><code>" + code + "</code><message>" + message + "</message></ExceptionWrapper>"));
	}

	@Test
	public void handleMethodArgumentNotValidException() throws Exception {
		DataContainer dataContainer = new DataContainer(100, DataContainer.Status.NEW);
		String jsonContent = this.objectMapper.writeValueAsString(dataContainer);

		this.mvc.perform(post("/beanValidation").content(jsonContent).contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
				.with(httpBasic("test", "hallo")))
				.andDo(print())
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$").isNotEmpty())
				.andExpect(jsonPath("$.*", Matchers.hasSize(3)))
				.andExpect(jsonPath("$.code", Matchers.is("900")))
				.andExpect(jsonPath("$.message", Matchers.is("dataContainer")))
				.andExpect(jsonPath("$.values.count", Matchers.is("must be less than or equal to 10")));
	}

	// error body should have same structure as above
	@Test
	public void handleConstraintViolationValidationException() throws Exception {
		DataContainer dataContainer = new DataContainer(100, DataContainer.Status.NEW);
		String jsonContent = this.objectMapper.writeValueAsString(dataContainer);

		this.mvc.perform(post("/beanValidation2").content(jsonContent).contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
				.with(httpBasic("test", "hallo")))
				.andDo(print())
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$").isNotEmpty())
				.andExpect(jsonPath("$.*", Matchers.hasSize(3)))
				.andExpect(jsonPath("$.code", Matchers.is("custom code")))
				.andExpect(jsonPath("$.message", Matchers.is("count: must be less than or equal to 10")))
				.andExpect(jsonPath("$.values.count", Matchers.is("must be less than or equal to 10")));
	}

	@Test
	public void handleMissingServletRequestParameterException() throws Exception {
		this.mvc.perform(get("/requireParameter")
				.with(httpBasic("test", "hallo")))
				.andDo(print())
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$").isNotEmpty())
				.andExpect(jsonPath("$.*", Matchers.hasSize(2)))
				.andExpect(jsonPath("$.code", Matchers.is("902")))
				.andExpect(jsonPath("$.message", Matchers.is("Required String parameter 'someParam' is not present")));
	}

	/*
	 * In this case, springs DefaultHandlerExceptionResolver handles HttpMessageNotReadableException with http status 400. Reason:
	 * Unmappable Enum value
	 *
	 * TODO: inconsequent error handling: MethodArgumentNotValidException results in 422 while this case results in 400. But using enum is
	 * just like a pattern on a string...
	 */
	@Test
	public void handleHttpMessageNotReadableException() throws Exception {

		this.mvc.perform(
				post("/beanValidation")
						.content("{\"count\":10,\"status\":\"ABC\"}")
						.contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
						.with(httpBasic("test", "hallo")))
				.andDo(print())
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$").isNotEmpty())
				.andExpect(jsonPath("$.*", Matchers.hasSize(2)))
				.andExpect(jsonPath("$.code", Matchers.is("901")))
				.andExpect(jsonPath("$.message", Matchers.containsString("JSON parse error: Cannot deserialize value of type")));
	}
}