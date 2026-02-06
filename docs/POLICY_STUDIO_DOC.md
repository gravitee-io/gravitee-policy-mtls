## Overview
You can use the `Mtls`  policy to verify a client certificate exists as part of the request.

This policy does not ensure that certificates are valid, since it is done directly by the server.



## Errors
These templates are defined at the API level, in the "Entrypoint" section for v4 APIs, or in "Response Templates" for v2 APIs.
The error keys sent by this policy are as follows:

| Key |
| ---  |
| CLIENT_CERTIFICATE_MISSING |
| CLIENT_CERTIFICATE_INVALID |
| SSL_SESSION_REQUIRED |


