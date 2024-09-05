/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.policy.mtls;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.gateway.tests.sdk.AbstractPolicyTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.configuration.GatewayConfigurationBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.EndpointBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.EntrypointBuilder;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.node.api.certificate.KeyStoreLoader;
import io.gravitee.plugin.endpoint.EndpointConnectorPlugin;
import io.gravitee.plugin.endpoint.http.proxy.HttpProxyEndpointConnectorFactory;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPlugin;
import io.gravitee.plugin.entrypoint.http.proxy.HttpProxyEntrypointConnectorFactory;
import io.gravitee.policy.mtls.configuration.MtlsPolicyConfiguration;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.core.buffer.Buffer;
import io.vertx.rxjava3.core.http.HttpClient;
import io.vertx.rxjava3.core.http.HttpClientRequest;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class MtlsPolicyIntegrationTest {

    @Nested
    @GatewayTest
    @DeployApi({ "/apis/v4/api.json" })
    class SecuredGateway extends AbstractPolicyTest<MtlsPolicy, MtlsPolicyConfiguration> {

        @Override
        public void configureEntrypoints(Map<String, EntrypointConnectorPlugin<?, ?>> entrypoints) {
            entrypoints.putIfAbsent("http-proxy", EntrypointBuilder.build("http-proxy", HttpProxyEntrypointConnectorFactory.class));
        }

        @Override
        public void configureEndpoints(Map<String, EndpointConnectorPlugin<?, ?>> endpoints) {
            endpoints.putIfAbsent("http-proxy", EndpointBuilder.build("http-proxy", HttpProxyEndpointConnectorFactory.class));
        }

        @SneakyThrows
        @Override
        protected void configureGateway(GatewayConfigurationBuilder config) {
            config
                .httpSecured(true)
                .set("http.ssl.clientAuth", "request")
                .set("http.ssl.keystore.type", KeyStoreLoader.CERTIFICATE_FORMAT_SELF_SIGNED)
                .set("http.ssl.truststore.path", getUrl("ca.pem").getPath())
                .set("http.ssl.truststore.type", "pem")
                .set("http.ssl.truststore.password", "secret");
        }

        @Test
        protected void should_be_unauthorized_if_no_certificate_on_request(Vertx vertx) {
            wiremock.stubFor(get("/endpoint").willReturn(ok("backend response")));

            createTrustedHttpClient(vertx, false)
                .rxRequest(HttpMethod.GET, "/test")
                .flatMap(HttpClientRequest::rxSend)
                .flatMap(response -> {
                    assertThat(response.statusCode()).isEqualTo(HttpStatusCode.UNAUTHORIZED_401);
                    return response.body();
                })
                .test()
                .awaitDone(10, TimeUnit.SECONDS)
                .assertComplete()
                .assertValue(Buffer.buffer(MtlsPolicy.FAILURE_MESSAGE))
                .assertNoErrors();

            wiremock.verify(0, getRequestedFor(urlPathEqualTo("/endpoint")));
        }

        @Test
        protected void should_call_the_api_with_a_certificate_on_request(Vertx vertx) {
            wiremock.stubFor(get("/endpoint").willReturn(ok("backend response")));

            createTrustedHttpClient(vertx, true)
                .rxRequest(HttpMethod.GET, "/test")
                .flatMap(HttpClientRequest::rxSend)
                .flatMap(response -> {
                    assertThat(response.statusCode()).isEqualTo(HttpStatusCode.OK_200);
                    return response.body();
                })
                .test()
                .awaitDone(10, TimeUnit.SECONDS)
                .assertComplete()
                .assertValue(Buffer.buffer("backend response"))
                .assertNoErrors();

            wiremock.verify(1, getRequestedFor(urlPathEqualTo("/endpoint")));
        }

        HttpClient createTrustedHttpClient(Vertx vertx, boolean withCert) {
            var options = new HttpClientOptions().setSsl(true).setTrustAll(true).setDefaultPort(gatewayPort()).setDefaultHost("localhost");
            if (withCert) {
                options =
                    options.setPemKeyCertOptions(
                        new PemKeyCertOptions().addCertPath(getUrl("client.cer").getPath()).addKeyPath(getUrl("client.key").getPath())
                    );
            }

            return vertx.createHttpClient(options);
        }

        URL getUrl(String name) {
            return getClass().getClassLoader().getResource(name);
        }
    }

    @Nested
    @GatewayTest
    @DeployApi({ "/apis/v4/api.json" })
    class UnsecuredGateway extends AbstractPolicyTest<MtlsPolicy, MtlsPolicyConfiguration> {

        @Override
        public void configureEntrypoints(Map<String, EntrypointConnectorPlugin<?, ?>> entrypoints) {
            entrypoints.putIfAbsent("http-proxy", EntrypointBuilder.build("http-proxy", HttpProxyEntrypointConnectorFactory.class));
        }

        @Override
        public void configureEndpoints(Map<String, EndpointConnectorPlugin<?, ?>> endpoints) {
            endpoints.putIfAbsent("http-proxy", EndpointBuilder.build("http-proxy", HttpProxyEndpointConnectorFactory.class));
        }

        @Test
        protected void should_be_unauthorized_if_no_certificate_on_request(HttpClient client) {
            wiremock.stubFor(get("/endpoint").willReturn(ok("backend response")));

            client
                .rxRequest(HttpMethod.GET, "/test")
                .flatMap(HttpClientRequest::rxSend)
                .flatMap(response -> {
                    assertThat(response.statusCode()).isEqualTo(HttpStatusCode.UNAUTHORIZED_401);
                    return response.body();
                })
                .test()
                .awaitDone(10, TimeUnit.SECONDS)
                .assertComplete()
                .assertValue(Buffer.buffer(MtlsPolicy.FAILURE_MESSAGE))
                .assertNoErrors();

            wiremock.verify(0, getRequestedFor(urlPathEqualTo("/endpoint")));
        }
    }
}
