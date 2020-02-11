package com.sap.cloud.security.token.validation;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.GregorianCalendar;
import java.util.Map;

import com.sap.cloud.security.config.OAuth2ServiceConfigurationBuilder;
import com.sap.cloud.security.json.DefaultJsonObject;
import com.sap.cloud.security.json.JsonObject;
import com.sap.cloud.security.token.SapIdToken;
import com.sap.cloud.security.token.TokenClaims;
import com.sap.cloud.security.util.HttpClientTestFactory;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.sap.cloud.security.config.OAuth2ServiceConfiguration;
import com.sap.cloud.security.config.Service;
import com.sap.cloud.security.config.cf.CFConstants;
import com.sap.cloud.security.token.Token;
import com.sap.cloud.security.token.XsuaaToken;
import com.sap.cloud.security.token.validation.validators.JwtValidatorBuilder;

public class IntegrationTest {

	public static final Instant NO_EXPIRE_DATE = new GregorianCalendar(2190, 11, 31).getTime().toInstant();

	CloseableHttpClient httpClientMock;

	@Before
	public void setup() throws IOException {
		httpClientMock = Mockito.mock(CloseableHttpClient.class);

		CloseableHttpResponse response = HttpClientTestFactory
				.createHttpResponse(IOUtils.resourceToString("/jsonWebTokenKeys.json", UTF_8));
		when(httpClientMock.execute(any(HttpGet.class))).thenReturn(response);
	}

	@Test
	public void validationFails_withXsuaaCombiningValidator() throws IOException {
		String vcapServices = IOUtils.resourceToString("/vcapXsuaaServiceSingleBinding.json", UTF_8);
		JsonObject serviceJsonObject = new DefaultJsonObject(vcapServices).getJsonObjects(Service.XSUAA.getCFName())
				.get(0);
		Map<String, String> credentialsMap = serviceJsonObject.getJsonObject(CFConstants.CREDENTIALS).getKeyValueMap();

		OAuth2ServiceConfiguration configuration = OAuth2ServiceConfigurationBuilder.forService(Service.XSUAA)
				.withProperties(credentialsMap)
				.build();

		CombiningValidator<Token> tokenValidator = JwtValidatorBuilder.getInstance(configuration)
				.withHttpClient(httpClientMock)
				.build();

		Token xsuaaToken = spy(new XsuaaToken(
				IOUtils.resourceToString("/xsuaaUserAccessTokenRSA256.txt", StandardCharsets.UTF_8)));

		ValidationResult result = tokenValidator.validate(xsuaaToken);
		assertThat(result.isValid()).isTrue();
	}

	@Test
	public void xsaTokenValidationSucceeds_withXsuaaCombiningValidator() throws IOException {
		String XsaVcapServices = IOUtils.resourceToString("/vcapXsuaaXsaSingleBinding.json", UTF_8);
		JsonObject serviceJsonObject = new DefaultJsonObject(XsaVcapServices).getJsonObjects(Service.XSUAA.getCFName())
				.get(0);
		Map<String, String> credentialsMap = serviceJsonObject.getJsonObject(CFConstants.CREDENTIALS).getKeyValueMap();

		OAuth2ServiceConfiguration configuration = OAuth2ServiceConfigurationBuilder.forService(Service.XSUAA)
				.withProperties(credentialsMap)
				.runInLegacyMode(true)
				.build();

		CombiningValidator<Token> tokenValidator = JwtValidatorBuilder.getInstance(configuration)
				.withHttpClient(httpClientMock)
				.build();

		XsuaaToken xsaToken = spy(new XsuaaToken(
				IOUtils.resourceToString("/xsuaaXsaAccessTokenRSA256_signedWithVerificationKey.txt", UTF_8)));
		when(xsaToken.getExpiration()).thenReturn(NO_EXPIRE_DATE);

		ValidationResult result = tokenValidator.validate(xsaToken);
		assertThat(result.isValid()).isTrue();
	}

	@Test
	public void iasTokenValidationSucceeds_withIasCombiningValidator() throws IOException {
		CloseableHttpResponse oidcResponse = HttpClientTestFactory
				.createHttpResponse("{\"jwks_uri\" : \"https://application.auth.com/oauth2/certs\"}");
		CloseableHttpResponse tokenKeysResponse = HttpClientTestFactory
				.createHttpResponse(IOUtils.resourceToString("/iasJsonWebTokenKeys.json", UTF_8));

		when(httpClientMock.execute(any(HttpGet.class)))
				.thenReturn(oidcResponse)
				.thenReturn(tokenKeysResponse);

		String vcapServices = IOUtils.resourceToString("/vcapIasServiceSingleBinding.json", UTF_8);
		JsonObject serviceJsonObject = new DefaultJsonObject(vcapServices).getJsonObjects(Service.IAS.getCFName())
				.get(0);
		Map<String, String> credentialsMap = serviceJsonObject.getJsonObject(CFConstants.CREDENTIALS).getKeyValueMap();

		OAuth2ServiceConfiguration configuration = OAuth2ServiceConfigurationBuilder.forService(Service.IAS)
				.withProperties(credentialsMap)
				.build();

		CombiningValidator<Token> tokenValidator = JwtValidatorBuilder.getInstance(configuration)
				.withHttpClient(httpClientMock)
				.build();

		SapIdToken iasToken = new SapIdToken(
				IOUtils.resourceToString("/iasOidcTokenRSA256.txt", StandardCharsets.UTF_8));

		ValidationResult result = tokenValidator.validate(iasToken);
		assertThat(result.isValid()).isTrue();
	}

	@Test
	public void xsuaaTokenValidationFails_withIasCombiningValidator() throws IOException {
		String vcapServices = IOUtils.resourceToString("/vcapIasServiceSingleBinding.json", UTF_8);
		JsonObject serviceJsonObject = new DefaultJsonObject(vcapServices).getJsonObjects(Service.IAS.getCFName())
				.get(0);
		Map<String, String> credentialsMap = serviceJsonObject.getJsonObject(CFConstants.CREDENTIALS).getKeyValueMap();

		OAuth2ServiceConfiguration configuration = OAuth2ServiceConfigurationBuilder.forService(Service.IAS)
				.withProperties(credentialsMap)
				.build();

		CombiningValidator<Token> tokenValidator = JwtValidatorBuilder.getInstance(configuration)
				.withHttpClient(httpClientMock)
				.build();

		XsuaaToken xsuaaToken = spy(new XsuaaToken(
				IOUtils.resourceToString("/xsuaaUserAccessTokenRSA256.txt", UTF_8)));
		when(xsuaaToken.getClaimAsString(TokenClaims.XSUAA.CLIENT_ID)).thenReturn("T000310");

		ValidationResult result = tokenValidator.validate(xsuaaToken);
		assertThat(result.isValid()).isFalse();
		assertThat(result.getErrorDescription()).startsWith(
				"Issuer is not trusted because 'iss' 'http://auth.com' does not match host 'myauth.com' of the identity provider");
	}
}
