package com.sap.cloud.security.spring.adapter;

import com.sap.cloud.security.config.OAuth2ServiceConfiguration;
import com.sap.cloud.security.config.OAuth2ServiceConfigurationBuilder;
import com.sap.cloud.security.config.Service;
import com.sap.cloud.security.config.cf.CFConstants;
import com.sap.cloud.security.token.Token;
import com.sap.cloud.security.token.validation.ValidationResults;
import com.sap.cloud.security.token.validation.Validator;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.security.oauth2.provider.OAuth2Authentication;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;

public class SAPOfflineTokenServicesCloudTest {

	private SAPOfflineTokenServicesCloud cut;
	private String xsuaaToken;

	public SAPOfflineTokenServicesCloudTest() throws IOException {
		xsuaaToken = IOUtils.resourceToString("/xsuaaScopesTokenRSA256.txt", StandardCharsets.UTF_8);
	}

	@Before
	public void setUp() {
		cut = createSAPOfflineTokenServicesCloud((token) -> ValidationResults.createValid());
	}


	@Test
	public void loadAuthentication() throws IOException {
		cut.afterPropertiesSet();
		OAuth2Authentication authentication = cut.loadAuthentication(xsuaaToken);

		assertThat(authentication.isAuthenticated()).isTrue();
		assertThat(authentication.getOAuth2Request()).isNotNull();
		assertThat(authentication.getOAuth2Request().getScope()).contains("openid");
	}

	@Test
	public void loadAuthentication_tokenIsNull_throwsException() {
		assertThatThrownBy(() -> cut.loadAuthentication(null)).isInstanceOf(InvalidTokenException.class);
	}

	@Test
	public void loadAuthentication_tokenIsMalformed_throwsException() {
		assertThatThrownBy(() -> cut.loadAuthentication("a.b.c")).isInstanceOf(InvalidTokenException.class);
	}


	@Test
	public void readAccessToken() {
		assertThatThrownBy(() -> cut.readAccessToken("token")).isInstanceOf(UnsupportedOperationException.class);
	}

	@Test
	public void loadAuthentication_tokenValidationFailed_throwsException() {
		String errorDescription = "just not valid";
		cut = createSAPOfflineTokenServicesCloud((token) -> ValidationResults.createInvalid(errorDescription));
		cut.afterPropertiesSet();

		assertThatThrownBy(() -> cut.loadAuthentication(xsuaaToken)).isInstanceOf(InvalidTokenException.class).hasMessageContaining(errorDescription);

	}

	@Test
	public void afterPropertiesSet() {
		TestTokenValidatorSupplier mockValidator = Mockito.mock(TestTokenValidatorSupplier.class);
		cut =  new SAPOfflineTokenServicesCloud(Mockito.mock(OAuth2ServiceConfiguration.class), mockValidator);

		Mockito.verify(mockValidator, times(0)).get();
		cut.afterPropertiesSet();
		Mockito.verify(mockValidator, times(1)).get();
	}

	@Test
	public void createInstancWithEmptyConfiguration_throwsException() {
		cut =  new SAPOfflineTokenServicesCloud(Mockito.mock(OAuth2ServiceConfiguration.class));
		cut.afterPropertiesSet();
		assertThatThrownBy(() -> cut.loadAuthentication(xsuaaToken)).isInstanceOf(InvalidTokenException.class);
	}


	private static class TestTokenValidatorSupplier implements Supplier<Validator<Token>> {
		@Override public Validator<Token> get() {
			return null;
		}
	}

	private SAPOfflineTokenServicesCloud createSAPOfflineTokenServicesCloud(Validator<Token> tokenValidator) {
		OAuth2ServiceConfiguration configuration = OAuth2ServiceConfigurationBuilder
				.forService(Service.XSUAA)
				.withProperty(CFConstants.XSUAA.APP_ID, "appId")
				.withProperty(CFConstants.CLIENT_ID, "clientId")
				.withProperty(CFConstants.XSUAA.UAA_DOMAIN, "localhost")
				.build();
		return new SAPOfflineTokenServicesCloud(configuration, () -> tokenValidator);
	}

}