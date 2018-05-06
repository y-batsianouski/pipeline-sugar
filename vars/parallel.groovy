def call(name = '', Closure body = null) {
  if (name instanceof Map) {
    steps.parallel(name)
  } else {
    if (name instanceof Closure) {
      body = name
      name = "parallel"
    }
    def config = [:]
    def parallelDelegate = [:]
    parallelDelegate.scm          = scm
    parallelDelegate.currentBuild = currentBuild
    parallelDelegate.env          = [:]
    env.each {it -> parallelDelegate.env[it.key] = it.value}
    parallelDelegate.env.scm          = scm
    parallelDelegate.env.currentBuild = currentBuild
    parallelDelegate.stage    = { String stageName, Closure code ->
      if (config.containsKey(stageName)) {
        config["${stageName}-${org.apache.commons.lang.RandomStringUtils.randomAlphanumeric(5)}"] = { stage(stageName, code) }
      } else {
        config[stageName] = { stage(stageName, code) }
      }
    }
    parallelDelegate.parallel = { String parallelName, Closure code ->
      if (code == null) {
        def ss = "${name}-${org.apache.commons.lang.RandomStringUtils.randomAlphanumeric(5)}"
        config[ss] = { parallel(ss, parallelName) }
      } else {
        if (config.containsKey(parallelName)) {
          config["${parallelName}-${org.apache.commons.lang.RandomStringUtils.randomAlphanumeric(5)}"] = { parallel(parallelName, code) }
        } else {
          config[parallelName] = { parallel(parallelName, code) }
        }
      }
    }
    parallelDelegate.block = { blockName, code ->
      if (blockName instanceof Closure) {
        def ss = "${name}-${org.apache.commons.lang.RandomStringUtils.randomAlphanumeric(5)}"
        config[ss] = { block(blockName) }
      } else {
        if (config.containsKey(blockName)) {
          config["${blockName}-${org.apache.commons.lang.RandomStringUtils.randomAlphanumeric(5)}"] = { block(blockName, code) }
        } else {
          config[blockName] = { block(blockName, code) }
        }
      }
    }
    parallelDelegate.failFast = { ff ->
      config['failFast'] = ff
    }

    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = parallelDelegate
    body()
    body.delegate = this
    steps.parallel(config)
  }
}
