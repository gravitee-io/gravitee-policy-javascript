# [2.0.0-alpha.5](https://github.com/gravitee-io/gravitee-policy-javascript/compare/2.0.0-alpha.4...2.0.0-alpha.5) (2025-11-14)


### Features

* enable for LLM & MCP Proxy API ([#49](https://github.com/gravitee-io/gravitee-policy-javascript/issues/49)) ([63f97f4](https://github.com/gravitee-io/gravitee-policy-javascript/commit/63f97f4564b21b39f19ffec3f2eff10a57bccffb))

# [2.0.0-alpha.4](https://github.com/gravitee-io/gravitee-policy-javascript/compare/2.0.0-alpha.3...2.0.0-alpha.4) (2025-10-09)


### Bug Fixes

* override version of central-publishing-maven-plugin with 0.9.0 ([2d28389](https://github.com/gravitee-io/gravitee-policy-javascript/commit/2d283893cda4ab8a0ee31449ef969a05aa19bdff))

# [2.0.0-alpha.3](https://github.com/gravitee-io/gravitee-policy-javascript/compare/2.0.0-alpha.2...2.0.0-alpha.3) (2025-10-02)


### Bug Fixes

* few project changes ([71b8ddc](https://github.com/gravitee-io/gravitee-policy-javascript/commit/71b8ddc16badb5d72ab0fd59290fa77df65c1935))

# [2.0.0-alpha.2](https://github.com/gravitee-io/gravitee-policy-javascript/compare/2.0.0-alpha.1...2.0.0-alpha.2) (2025-09-19)


### Bug Fixes

* rewrite documentation to doc-gen ([#46](https://github.com/gravitee-io/gravitee-policy-javascript/issues/46)) ([90f09f4](https://github.com/gravitee-io/gravitee-policy-javascript/commit/90f09f4ec6d934eb01a41a933f8be1fc177cb615))

# [2.0.0-alpha.1](https://github.com/gravitee-io/gravitee-policy-javascript/compare/1.4.0...2.0.0-alpha.1) (2025-09-16)


### Features

* add v4 messaging support ([#45](https://github.com/gravitee-io/gravitee-policy-javascript/issues/45)) ([6ea67fe](https://github.com/gravitee-io/gravitee-policy-javascript/commit/6ea67fe1e3b441f0ca7bb588355314e4a7be592d))


### BREAKING CHANGES

* requires APIM 4.8+

Co-authored-by: Michal Balinski <michal@incubly.com>

# [1.4.0](https://github.com/gravitee-io/gravitee-policy-javascript/compare/1.3.3...1.4.0) (2025-04-01)


### Features

* enable policy for v4 proxy API ([f85cabf](https://github.com/gravitee-io/gravitee-policy-javascript/commit/f85cabf3fed61aa74ff680b0a3abe2bed80c3506))

## [1.3.3](https://github.com/gravitee-io/gravitee-policy-javascript/compare/1.3.2...1.3.3) (2023-07-20)


### Bug Fixes

* update policy description ([e055cc5](https://github.com/gravitee-io/gravitee-policy-javascript/commit/e055cc5ba4b79be5ffd94875270feef1ed6eb4b8))

## [1.3.2](https://github.com/gravitee-io/gravitee-policy-javascript/compare/1.3.1...1.3.2) (2023-07-11)


### Bug Fixes

*  Protect the engine property from being deleted every time a script is evaluated ([16446ed](https://github.com/gravitee-io/gravitee-policy-javascript/commit/16446ed5b2214bfda97a4750c7690aa811433da3))

## [1.3.1](https://github.com/gravitee-io/gravitee-policy-javascript/compare/1.3.0...1.3.1) (2023-06-27)


### Bug Fixes

* add policy result key to readme ([f37613e](https://github.com/gravitee-io/gravitee-policy-javascript/commit/f37613ede529eaa18f39fddcebfc77f4390461ed))

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
