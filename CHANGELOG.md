# [1.3.0](https://github.com/gravitee-io/gravitee-policy-javascript/compare/1.2.1...1.3.0) (2023-06-27)


### Features

* allow to add response template key in policy result ([b0ffc3a](https://github.com/gravitee-io/gravitee-policy-javascript/commit/b0ffc3a2988376d1e2810e5693eff2bab4ac6666))

## [1.2.1](https://github.com/gravitee-io/gravitee-policy-javascript/compare/1.2.0...1.2.1) (2023-06-22)


### Bug Fixes

* **engine:** Protect the engine property from being deleted ([b0cae0f](https://github.com/gravitee-io/gravitee-policy-javascript/commit/b0cae0fc3c4764809f508689fd7fcfc89e69741b))

# [1.2.0](https://github.com/gravitee-io/gravitee-policy-javascript/compare/1.1.1...1.2.0) (2023-04-12)


### Bug Fixes

* add `getMetrics` method to be consistent with other fields ([123d585](https://github.com/gravitee-io/gravitee-policy-javascript/commit/123d585489967c4a9eac4da33cc9c8aae8117fcd))
* fix `scheme` getter that was returning local address instead ([bb10890](https://github.com/gravitee-io/gravitee-policy-javascript/commit/bb1089056ab6974faabea3e9ba2ae9908eb1c921))


### Features

* add getter for `host` ([84bc68c](https://github.com/gravitee-io/gravitee-policy-javascript/commit/84bc68cd8aa21bb832b9a08a49a5a3f8c68e71ea))
* expose `properties` just like it's done for `dictionaries` ([2e4f8fa](https://github.com/gravitee-io/gravitee-policy-javascript/commit/2e4f8faa03d215e0730faba849b1d38754a58a88))

## [1.1.1](https://github.com/gravitee-io/gravitee-policy-javascript/compare/[secure]...1.1.1) (2022-02-21)


### Bug Fixes

* allow error on request and response content phases ([#17](https://github.com/gravitee-io/gravitee-policy-javascript/issues/17)) ([d1c6be9](https://github.com/gravitee-io/gravitee-policy-javascript/commit/d1c6be912c03e544e3e6a6b0173a38f2b37f5b33)), closes [gravitee-io/issues#7173](https://github.com/gravitee-io/issues/issues/7173)

# [[secure]](https://github.com/gravitee-io/gravitee-policy-javascript/compare/1.0.0...[secure]) (2022-01-24)


### Features

* **headers:** Internal rework and introduce HTTP Headers API ([f5354c4](https://github.com/gravitee-io/gravitee-policy-javascript/commit/f5354c4282abffa53b0c184f911e6db0ac49638f)), closes [gravitee-io/issues#6772](https://github.com/gravitee-io/issues/issues/6772)
* **perf:** adapt policy for new classloader system ([b70c9c8](https://github.com/gravitee-io/gravitee-policy-javascript/commit/b70c9c89013ca20b7064c9ac37f6f460446dbf27)), closes [gravitee-io/issues#6758](https://github.com/gravitee-io/issues/issues/6758)
