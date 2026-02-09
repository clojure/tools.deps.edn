tools.deps.edn
========================================

Reader for deps.edn

# Rationale

This is a small library for reading and manipulating deps.edn files and data structures.
It can be used in scenarios where the full [tools.deps](https://github.com/clojure/tools.deps)
library is not needed for dep expansion (which pulls in many large libraries).

* [deps.edn Reference](https://clojure.org/reference/deps_edn)

# Release Information

Latest release: 0.9.13

* [All released versions](https://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22org.clojure%22%20AND%20a%3A%22tools.deps.edn%22)

[deps.edn](https://clojure.org/reference/deps_edn) dependency information:

```
org.clojure/tools.deps.edn {:mvn/version "0.9.13"}
```

[Leiningen](https://github.com/technomancy/leiningen/) dependency information:

```
[org.clojure/tools.deps.edn "0.9.13"]
```

[Maven](https://maven.apache.org) dependency information:

```
<dependency>
  <groupId>org.clojure</groupId>
  <artifactId>tools.deps.edn</artifactId>
  <version>0.9.13</version>
</dependency>
```

# API 

For info on using tools.deps.edn as a library, see:

* [API Docs](https://clojure.github.io/tools.deps.edn)

# Developer Information

* [GitHub project](https://github.com/clojure/tools.deps.edn)
* [How to contribute](https://clojure.org/community/contributing)
* [Bug Tracker](https://clojure.atlassian.net/browse/TDEPS)
* [Continuous Integration](https://github.com/clojure/tools.deps.edn/actions/workflows/test.yml)

# Copyright and License

Copyright © Rich Hickey, Alex Miller, and contributors

All rights reserved. The use and
distribution terms for this software are covered by the
[Eclipse Public License 1.0] which can be found in the file
LICENSE at the root of this distribution. By using this software
in any fashion, you are agreeing to be bound by the terms of this
license. You must not remove this notice, or any other, from this
software.

[Eclipse Public License 1.0]: https://opensource.org/license/epl-1-0/
