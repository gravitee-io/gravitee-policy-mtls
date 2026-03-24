
<!-- GENERATED CODE - DO NOT ALTER THIS OR THE FOLLOWING LINES -->
# mTLS

[![Gravitee.io](https://img.shields.io/static/v1?label=Available%20at&message=Gravitee.io&color=1EC9D2)](https://download.gravitee.io/#graviteeio-apim/plugins/policies/gravitee-policy-mtls/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://github.com/gravitee-io/gravitee-policy-mtls/blob/master/LICENSE.txt)
[![Releases](https://img.shields.io/badge/semantic--release-conventional%20commits-e10079?logo=semantic-release)](https://github.com/gravitee-io/gravitee-policy-mtls/releases)
[![CircleCI](https://circleci.com/gh/gravitee-io/gravitee-policy-mtls.svg?style=svg)](https://circleci.com/gh/gravitee-io/gravitee-policy-mtls)

## Overview
You can use the `Mtls` policy to verify a client certificate exists as part of the request.

This policy does not ensure that certificates are valid, since it is done directly by the server.



## Errors
These templates are defined at the API level, in the "Entrypoint" section for v4 APIs, or in "Response Templates" for v2 APIs.
The error keys sent by this policy are as follows:

| Key |
| ---  |
| CLIENT_CERTIFICATE_MISSING |
| CLIENT_CERTIFICATE_INVALID |
| SSL_SESSION_REQUIRED |



## Phases
The `mtls` policy can be applied to the following API types and flow phases.

### Compatible API types

* `PROXY`
* `MESSAGE`

### Supported flow phases:

* Request

## Compatibility matrix
Strikethrough text indicates that a version is deprecated.

| Plugin version| APIM |
| --- | ---  |
|3.x|4.11.x and above |
|2.x|4.10.x |
|1.x|4.9.x and below |


## Configuration options


#### 
| Name <br>`json name`  | Type <br>`constraint`  | Mandatory  | Description  |
|:----------------------|:-----------------------|:----------:|:-------------|
| No properties | | | | | | | 



## Examples



## Changelog

### [3.0.0](https://github.com/gravitee-io/gravitee-policy-mtls/compare/2.0.1...3.0.0) (2026-03-24)


##### Features

* use sha-256 for stronger digests ([4fb89ef](https://github.com/gravitee-io/gravitee-policy-mtls/commit/4fb89ef7c18cc0ca1d4fa01cb896f8f99d7c2d3b))


##### BREAKING CHANGES

* for 4.11 and above only.

#### [2.0.1](https://github.com/gravitee-io/gravitee-policy-mtls/compare/2.0.0...2.0.1) (2026-03-24)


##### Bug Fixes

* revert breaking change commit ([eae6c29](https://github.com/gravitee-io/gravitee-policy-mtls/commit/eae6c29ca01bb87893eec902cfc91009486da5d1))

### [2.0.0](https://github.com/gravitee-io/gravitee-policy-mtls/compare/1.0.0...2.0.0) (2026-03-16)


##### Bug Fixes

* enhance MTLS policy to support Kafka connections and improve certificate validation ([0d09a8f](https://github.com/gravitee-io/gravitee-policy-mtls/commit/0d09a8fb1327a11b1781ffd5e2f77422c6912dcb))


##### Features

* introduce MtlsPolicyException for improved error handling in mTLS validation ([daf6399](https://github.com/gravitee-io/gravitee-policy-mtls/commit/daf6399e9e20cc972153c157fde3c7dbc31c8946))
* update dependencies in order to make it compatible with native kafka ([d6595b9](https://github.com/gravitee-io/gravitee-policy-mtls/commit/d6595b9379c62f1fe601beb6d76c610bd8b6f646))


##### BREAKING CHANGES

* This policy is compatible with APIM 4.10 minimum

### 1.0.0 (2024-09-13)


##### Bug Fixes

* change token validity for invalid cert ([4ae7ada](https://github.com/gravitee-io/gravitee-policy-mtls/commit/4ae7ada01e73340f92cb7ab3a11b1aee3675f70f))


##### Features

* change mtls  logo ([b026a06](https://github.com/gravitee-io/gravitee-policy-mtls/commit/b026a064efe8d7b0fb552cb8d87f3b7043853733))
* complete README ([742f6e9](https://github.com/gravitee-io/gravitee-policy-mtls/commit/742f6e9f25160d376c0e6f4975134b3889d16699))
* implement mTLS policy ([3086876](https://github.com/gravitee-io/gravitee-policy-mtls/commit/30868765e2c8a4873781e0fc81ae6af00a3aad88))
* init no op mtls policy ([bc353ab](https://github.com/gravitee-io/gravitee-policy-mtls/commit/bc353ab7af5989369c94a7e25adab98f5dd5380c))
* rely on tls session to check client certificate ([60b6516](https://github.com/gravitee-io/gravitee-policy-mtls/commit/60b6516321b244b5bd13face7ce685db678ea2e3))

### [1.0.0-alpha.4](https://github.com/gravitee-io/gravitee-policy-mtls/compare/1.0.0-alpha.3...1.0.0-alpha.4) (2024-09-12)


##### Features

* rely on tls session to check client certificate ([479f022](https://github.com/gravitee-io/gravitee-policy-mtls/commit/479f0220663ca78c993aeb6f08b6bcf168a53e89))

### [1.0.0-alpha.3](https://github.com/gravitee-io/gravitee-policy-mtls/compare/1.0.0-alpha.2...1.0.0-alpha.3) (2024-09-09)


##### Bug Fixes

* change token validity for invalid cert ([4719391](https://github.com/gravitee-io/gravitee-policy-mtls/commit/47193918db5099203d65bb79754a78fedd972607))

### [1.0.0-alpha.2](https://github.com/gravitee-io/gravitee-policy-mtls/compare/1.0.0-alpha.1...1.0.0-alpha.2) (2024-09-05)


##### Features

* change mtls  logo ([fc194c8](https://github.com/gravitee-io/gravitee-policy-mtls/commit/fc194c82ffc1369cdf3c3cd5119d35330683e5e7))
* complete README ([f7cf64f](https://github.com/gravitee-io/gravitee-policy-mtls/commit/f7cf64feb535802aece92fd4e67c8969d055447f))
* implement mTLS policy ([1e72fb4](https://github.com/gravitee-io/gravitee-policy-mtls/commit/1e72fb4bc59db702731c0c1cc89b93a55dcc96d1))

### 1.0.0-alpha.1 (2024-08-07)


##### Features

* init no op mtls policy ([9f848e4](https://github.com/gravitee-io/gravitee-policy-mtls/commit/9f848e4f1aa6222740a090fadbe65de254bbd931))

