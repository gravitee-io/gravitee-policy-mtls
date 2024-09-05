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

/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.gateway.reactive.api.ExecutionFailure;
import io.gravitee.gateway.reactive.api.context.HttpExecutionContext;
import io.gravitee.gateway.reactive.api.policy.SecurityPolicy;
import io.gravitee.gateway.reactive.api.policy.SecurityToken;
import io.gravitee.policy.mtls.configuration.MtlsPolicyConfiguration;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.DigestUtils;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
public class MtlsPolicy implements SecurityPolicy {

    public static final String CLIENT_CERTIFICATE_MISSING = "CLIENT_CERTIFICATE_MISSING";
    public static final String CLIENT_CERTIFICATE_INVALID = "CLIENT_CERTIFICATE_INVALID";
    public static final String FAILURE_MESSAGE = "Unauthorized";
    public static final String SSL_SESSION_REQUIRED = "SSL_SESSION_REQUIRED";
    private final MtlsPolicyConfiguration configuration;

    public MtlsPolicy(MtlsPolicyConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public String id() {
        return "template-policy";
    }

    @Override
    public Maybe<SecurityToken> extractSecurityToken(HttpExecutionContext ctx) {
        final SSLSession sslSession = ctx.request().sslSession();
        if (sslSession == null) {
            return Maybe.empty();
        }

        final Certificate[] peerCertificates;
        try {
            peerCertificates = sslSession.getPeerCertificates();
        } catch (SSLPeerUnverifiedException e) {
            return Maybe.empty();
        }

        if (peerCertificates == null || peerCertificates.length == 0) {
            return Maybe.empty();
        }

        final String clientCertificate;
        try {
            clientCertificate = DigestUtils.md5DigestAsHex(peerCertificates[0].getEncoded());
        } catch (CertificateEncodingException e) {
            return Maybe.empty();
        }
        return Maybe.just(SecurityToken.forClientCertificate(clientCertificate));
    }

    @Override
    public boolean requireSubscription() {
        return true;
    }

    /**
     * Order set to -100 to make sure it will be executed before all other security policies.
     *
     * @return -100
     */
    @Override
    public int order() {
        return -100;
    }

    @Override
    public Completable onRequest(HttpExecutionContext ctx) {
        final SSLSession sslSession = ctx.request().sslSession();
        if (sslSession == null) {
            return interruptWith401(ctx, SSL_SESSION_REQUIRED);
        }
        final Certificate[] certificates;
        try {
            certificates = sslSession.getPeerCertificates();
        } catch (SSLPeerUnverifiedException e) {
            return interruptWith401(ctx, CLIENT_CERTIFICATE_INVALID);
        }
        if (certificates == null || certificates.length == 0) {
            return interruptWith401(ctx, CLIENT_CERTIFICATE_MISSING);
        }
        return Completable.complete();
    }

    private static Completable interruptWith401(HttpExecutionContext ctx, String errorKey) {
        return ctx.interruptWith(new ExecutionFailure(HttpStatusCode.UNAUTHORIZED_401).key(errorKey).message(FAILURE_MESSAGE));
    }
}
