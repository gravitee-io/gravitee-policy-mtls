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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.gateway.reactive.api.policy.SecurityToken;
import io.gravitee.gateway.reactive.core.context.AbstractRequest;
import io.gravitee.gateway.reactive.core.context.AbstractResponse;
import io.gravitee.gateway.reactive.core.context.DefaultExecutionContext;
import io.gravitee.gateway.reactive.core.context.interruption.InterruptionFailureException;
import io.gravitee.policy.mtls.configuration.MtlsPolicyConfiguration;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.util.List;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@ExtendWith(MockitoExtension.class)
class MtlsPolicyTest {

    private final MtlsPolicy cut = new MtlsPolicy(new MtlsPolicyConfiguration());

    @Nested
    class TokenExtraction {

        @Test
        void should_not_extract_token_when_no_ssl_session() {
            final DefaultExecutionContext ctx = prepareContext(new AbstractRequest() {});

            cut.extractSecurityToken(ctx).test().assertComplete().assertNoValues();
        }

        @Test
        void should_not_extract_token_when_peer_certificate_exception() {
            final DefaultExecutionContext ctx = prepareContext(
                new AbstractRequest() {
                    @SneakyThrows
                    @Override
                    public SSLSession sslSession() {
                        final SSLSession sslSession = mock(SSLSession.class);
                        when(sslSession.getPeerCertificates()).thenThrow(SSLPeerUnverifiedException.class);
                        return sslSession;
                    }
                }
            );

            cut.extractSecurityToken(ctx).test().assertComplete().assertNoValues();
        }

        @Test
        void should_not_extract_token_when_peer_certificate_null_array() {
            final DefaultExecutionContext ctx = prepareContext(
                new AbstractRequest() {
                    @SneakyThrows
                    @Override
                    public SSLSession sslSession() {
                        final SSLSession sslSession = mock(SSLSession.class);
                        when(sslSession.getPeerCertificates()).thenReturn(null);
                        return sslSession;
                    }
                }
            );

            cut.extractSecurityToken(ctx).test().assertComplete().assertNoValues();
        }

        @Test
        void should_not_extract_token_when_peer_certificate_empty_array() {
            final DefaultExecutionContext ctx = prepareContext(
                new AbstractRequest() {
                    @SneakyThrows
                    @Override
                    public SSLSession sslSession() {
                        final SSLSession sslSession = mock(SSLSession.class);
                        when(sslSession.getPeerCertificates()).thenReturn(new Certificate[0]);
                        return sslSession;
                    }
                }
            );

            cut.extractSecurityToken(ctx).test().assertComplete().assertNoValues();
        }

        @Test
        void should_not_extract_token_when_digest_computation_exception() {
            final DefaultExecutionContext ctx = prepareContext(
                new AbstractRequest() {
                    @SneakyThrows
                    @Override
                    public SSLSession sslSession() {
                        final SSLSession sslSession = mock(SSLSession.class);
                        final Certificate certificate = mock(Certificate.class);
                        when(certificate.getEncoded()).thenThrow(CertificateEncodingException.class);
                        when(sslSession.getPeerCertificates()).thenReturn(List.of(certificate).toArray(new Certificate[0]));
                        return sslSession;
                    }
                }
            );

            cut.extractSecurityToken(ctx).test().assertComplete().assertNoValues();
        }

        @Test
        void should_extract_token() {
            final DefaultExecutionContext ctx = prepareContext(
                new AbstractRequest() {
                    @SneakyThrows
                    @Override
                    public SSLSession sslSession() {
                        final SSLSession sslSession = mock(SSLSession.class);
                        final Certificate certificate = mock(Certificate.class);
                        when(certificate.getEncoded()).thenReturn("a-certificate".getBytes());
                        when(sslSession.getPeerCertificates()).thenReturn(List.of(certificate).toArray(new Certificate[0]));
                        return sslSession;
                    }
                }
            );

            cut
                .extractSecurityToken(ctx)
                .test()
                .assertComplete()
                .assertValue(securityToken -> {
                    assertThat(securityToken.getTokenType()).isEqualTo(SecurityToken.TokenType.CERTIFICATE.name());
                    return true;
                });
        }
    }

    @Nested
    class OnRequest {

        @Test
        void should_answer_with_401_when_no_ssl_session() {
            final DefaultExecutionContext ctx = prepareContext(new AbstractRequest() {});

            cut
                .onRequest(ctx)
                .test()
                .assertError(t -> {
                    assertThat(t).isInstanceOf(InterruptionFailureException.class);
                    final InterruptionFailureException exception = (InterruptionFailureException) t;
                    assertThat(exception.getExecutionFailure().key()).isEqualTo(MtlsPolicy.SSL_SESSION_REQUIRED);
                    assertThat(exception.getExecutionFailure().statusCode()).isEqualTo(HttpStatusCode.UNAUTHORIZED_401);
                    return true;
                });
        }

        @Test
        void should_answer_with_401_when_peer_certificate_exception() {
            final DefaultExecutionContext ctx = prepareContext(
                new AbstractRequest() {
                    @SneakyThrows
                    @Override
                    public SSLSession sslSession() {
                        final SSLSession sslSession = mock(SSLSession.class);
                        when(sslSession.getPeerCertificates()).thenThrow(SSLPeerUnverifiedException.class);
                        return sslSession;
                    }
                }
            );

            cut
                .onRequest(ctx)
                .test()
                .assertError(t -> {
                    assertThat(t).isInstanceOf(InterruptionFailureException.class);
                    final InterruptionFailureException exception = (InterruptionFailureException) t;
                    assertThat(exception.getExecutionFailure().key()).isEqualTo(MtlsPolicy.CLIENT_CERTIFICATE_INVALID);
                    assertThat(exception.getExecutionFailure().statusCode()).isEqualTo(HttpStatusCode.UNAUTHORIZED_401);
                    return true;
                });
        }

        @Test
        void should_answer_with_401_when_peer_certificate_null_array() {
            final DefaultExecutionContext ctx = prepareContext(
                new AbstractRequest() {
                    @SneakyThrows
                    @Override
                    public SSLSession sslSession() {
                        final SSLSession sslSession = mock(SSLSession.class);
                        when(sslSession.getPeerCertificates()).thenReturn(null);
                        return sslSession;
                    }
                }
            );

            cut
                .onRequest(ctx)
                .test()
                .assertError(t -> {
                    assertThat(t).isInstanceOf(InterruptionFailureException.class);
                    final InterruptionFailureException exception = (InterruptionFailureException) t;
                    assertThat(exception.getExecutionFailure().key()).isEqualTo(MtlsPolicy.CLIENT_CERTIFICATE_MISSING);
                    assertThat(exception.getExecutionFailure().statusCode()).isEqualTo(HttpStatusCode.UNAUTHORIZED_401);
                    return true;
                });
        }

        @Test
        void should_answer_with_401_when_peer_certificate_empty_array() {
            final DefaultExecutionContext ctx = prepareContext(
                new AbstractRequest() {
                    @SneakyThrows
                    @Override
                    public SSLSession sslSession() {
                        final SSLSession sslSession = mock(SSLSession.class);
                        when(sslSession.getPeerCertificates()).thenReturn(new Certificate[0]);
                        return sslSession;
                    }
                }
            );

            cut
                .onRequest(ctx)
                .test()
                .assertError(t -> {
                    assertThat(t).isInstanceOf(InterruptionFailureException.class);
                    final InterruptionFailureException exception = (InterruptionFailureException) t;
                    assertThat(exception.getExecutionFailure().key()).isEqualTo(MtlsPolicy.CLIENT_CERTIFICATE_MISSING);
                    assertThat(exception.getExecutionFailure().statusCode()).isEqualTo(HttpStatusCode.UNAUTHORIZED_401);
                    return true;
                });
        }

        @Test
        void should_continue_request_if_certificate_exist() {
            final DefaultExecutionContext ctx = prepareContext(
                new AbstractRequest() {
                    @SneakyThrows
                    @Override
                    public SSLSession sslSession() {
                        final SSLSession sslSession = mock(SSLSession.class);
                        final Certificate certificate = mock(Certificate.class);
                        when(sslSession.getPeerCertificates()).thenReturn(List.of(certificate).toArray(new Certificate[0]));
                        return sslSession;
                    }
                }
            );

            cut.onRequest(ctx).test().assertComplete();
        }
    }

    private static DefaultExecutionContext prepareContext(AbstractRequest request) {
        final DefaultExecutionContext ctx = new DefaultExecutionContext(request, new AbstractResponse() {});
        return ctx;
    }
}
