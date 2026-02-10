tools.deps.edn
========================================

Reader for deps.edn

# Rationale

This is a small library for reading and manipulating deps.edn files and data structures.
It can be used in scenarios where the full [tools.deps](https://github.com/clojure/tools.deps)
library is not needed for dep expansion (which pulls in many large libraries).

* [deps.edn Reference](https://clojure.org/reference/deps_edn)

# Release Information

Latest release: 0.9.22

* [All released versions](https://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22org.clojure%22%20AND%20a%3A%22tools.deps.edn%22)

[deps.edn](https://clojure.org/reference/deps_edn) dependency information:

```
org.clojure/tools.deps.edn {:mvn/version "0.9.22"}
```

[Leiningen](https://github.com/technomancy/leiningen/) dependency information:

```
[org.clojure/tools.deps.edn "0.9.22"]
```

[Maven](https://maven.apache.org) dependency information:

```xml
<dependency>
  <groupId>org.clojure</groupId>
  <artifactId>tools.deps.edn</artifactId>
  <version>0.9.22</version>
</dependency>
```

# API 

## Reading, validating, and canonicalization

Usually, you should use the `read-deps` function to read a deps.edn file, validate, and canonicalize it:

* ([read-deps](https://clojure.github.io/tools.deps.edn/#clojure.tools.deps.edn/read-deps) f & opts) - coerce f to a File, then read, validate, canonicalize and return a deps.edn map

However, these component functions may also occasionally be useful:

* ([read-edn](https://clojure.github.io/tools.deps.edn/#clojure.tools.deps.edn/read-edn) r & opts) - reads a single edn value from a Reader r
* ([validate](https://clojure.github.io/tools.deps.edn/#clojure.tools.deps.edn/validate) deps-edn & opts) - validate a deps.edn map against the specs and throw or return the valid map
* ([canonicalize](https://clojure.github.io/tools.deps.edn/#clojure.tools.deps.edn/canonicalize) deps-edn & opts) - canoncialize a deps.edn map and return it

## Deps chain

These functions are available to get individual or multiple of the standard deps.edn maps in the chain:

* ([root-deps](https://clojure.github.io/tools.deps.edn/#clojure.tools.deps.edn/root-deps)) - return the root deps read as a resource
* ([user-deps-path](https://clojure.github.io/tools.deps.edn/#clojure.tools.deps.edn/user-deps-path)) - calculate the path to the user deps.edn
* ([user-deps](https://clojure.github.io/tools.deps.edn/#clojure.tools.deps.edn/user-deps)) - use `user-deps-path`, then read and return it (or nil if none exists)
* ([project-deps-path](https://clojure.github.io/tools.deps.edn/#clojure.tools.deps.edn/project-deps-path)) - calculate the path to the project deps.edn, using the dir context as the current directory
* ([project-deps](https://clojure.github.io/tools.deps.edn/#clojure.tools.deps.edn/project-deps)) - use `project-deps-path`, then read and return it (or nil if none exists)
* ([create-edn-maps](https://clojure.github.io/tools.deps.edn/#clojure.tools.deps.edn/create-edn-maps)) ([create-edn-maps](https://clojure.github.io/tools.deps.edn/#clojure.tools.deps.edn/create-edn-maps) params-map) - takes optional map of location sources and returns a map of the root, user, project, and extra deps.edn maps

## Data manipulation

These functions can be used to modify or extract information from a deps.edn map:

* ([merge-edns](https://clojure.github.io/tools.deps.edn/#clojure.tools.deps.edn/merge-edns) deps-edn-maps) - merge multiple deps.edn maps in a chain
* ([merge-alias-maps](https://clojure.github.io/tools.deps.edn/#clojure.tools.deps.edn/merge-alias-maps)) - like merge-with, for merging alias maps with per-key rules
* ([combine-aliases](https://clojure.github.io/tools.deps.edn/#clojure.tools.deps.edn/combine-aliases)) - find, read, and combine alias maps identified by alias keywords from
  a deps edn map into a single args map.

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
