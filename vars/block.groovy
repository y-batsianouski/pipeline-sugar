def call(String name = '', Closure body) {
  def config = [
    post: [
      always: {},
      success: {},
      unstable: {},
      failure: {},
      aborted: {}
    ],
    agent: [
      type: false
    ],
    when: true,
    steps: {},
    parallel: false,
    ignoreFailure: false,
    credentials: [],
    wrappers: [],
    tools: [],
    environment: []
  ]
  if (!binding.hasVariable('blockOptions')) {blockOptions = []}
  if (!binding.hasVariable('blockTriggers')) {blockTriggers = []}
  if (!binding.hasVariable('blockParameters')) {blockParameters = []}

  def delegate = [:]
  delegate.scm          = scm
  delegate.currentBuild = currentBuild
  delegate.env          = [:]
  env.each {it -> delegate.env[it.key] = it.value}
  delegate.env.scm          = scm
  delegate.env.currentBuild = currentBuild
  delegate.when = {it -> config['when'] = it()}
  delegate.post          = {}
  delegate.agent         = {}
  delegate.steps         = {}
  delegate.parallel      = {}
  delegate.ignoreFailure = {}
  delegate.options       = {}
  delegate.parameters    = {}
  delegate.triggers      = {}
  delegate.credentials   = {}
  delegate.wrappers      = {}
  delegate.tools         = {}

  body.resolveStrategy = Closure.DELEGATE_FIRST
  body.delegate = delegate
  body()

  def blockWhen = config['when']
  if (blockWhen) {
    def postDelegate          = [:]
    postDelegate.currentBuild = delegate.currentBuild
    postDelegate.scm          = delegate.scm
    postDelegate.env          = delegate.env
    postDelegate.always       = {v -> config['post']['always']   = v}
    postDelegate.unstable     = {v -> config['post']['unstable'] = v}
    postDelegate.failure      = {v -> config['post']['failure']  = v}
    postDelegate.success      = {v -> config['post']['success']  = v}
    postDelegate.aborted      = {v -> config['post']['aborted']  = v}

    def agentDelegate = [:]
    agentDelegate.label = {v ->
      config['agent']['type']   = 'label'
      config['agent']['config'] = v
    }
    agentDelegate.podTemplate = {v ->
      config['agent']['type']   = 'podTemplate'
      config['agent']['config'] = v
    }

    def environmentDelegate          = [:]
    environmentDelegate.scm          = scm
    environmentDelegate.currentBuild = currentBuild
    environmentDelegate.env          = env

    delegate.post = {it ->
      it.resolveStrategy = Closure.DELEGATE_FIRST
      it.delegate = postDelegate
      it()
    }
    delegate.agent = {it ->
      it.resolveStrategy = Closure.DELEGATE_FIRST
      it.delegate = agentDelegate
      it()
    }
    delegate.steps = {it ->
      config['steps'] = it
    }
    delegate.wrappers = {it ->
      config['wrappers'] = it
    }
    delegate.tools = {it ->
      config['tools'] = it
    }
    delegate.credentials = {it ->
      config['credentials'] = it
    }
    delegate.when  = {it -> config['when']  = it}
    delegate.environment = { it ->
      it.resolveStrategy = Closure.DELEGATE_FIRST
      it.delegate = environmentDelegate
      it()
    }

    delegate.parallel = { String parallelName, Closure code ->
      config['parallel'] = { parallel(parallelName, code) }
    }
    delegate.ignoreFailure = {it -> config['ignoreFailure'] = it}
    delegate.options = { opts -> blockOptions = blockOptions.plus(opts) }
    delegate.parameters = { params -> blockParameters = blockParameters.plus(params) }
    delegate.triggers = { trgrs -> blockTriggers = blockTriggers.plus(trgrs) }

    body()

    def blockPost  = config['post']
    blockPost.each {postItem ->
      blockPost[postItem.key].resolveStrategy   = Closure.DELEGATE_FIRST
      blockPost[postItem.key].delegate          = this
    }

    def blockSteps = config.get('steps', {})
    blockSteps.resolveStrategy = Closure.DELEGATE_FIRST
    blockSteps.delegate        = this

    def blockParallel = config['parallel']
    if (blockParallel) {
      blockParallel.resolveStrategy = Closure.DELEGATE_FIRST
      blockParallel.delegate        = this
    }

    def blockIgnoreFailure = config['ignoreFailure']

    environmentDelegate.each {var ->
      config['environment'] << "${var.key}=${var.value}"
    }

    def blockExecutable = [:]
    if (blockParallel) {
        blockExecutable['steps'] = blockParallel
    } else {
        blockExecutable['steps'] = blockSteps
    }

    if (config['environment']) {
        blockExecutable['environment'] = { withEnv(config['environment'], blockExecutable['steps']) }
    } else {
        blockExecutable['environment'] = blockExecutable['steps']
    }

    if (config['credentials']) {
      blockExecutable['credentials'] = { withCredentials(config['credentials'], blockExecutable['environment']) }
    } else {
      blockExecutable['credentials'] = blockExecutable['environment']
    }

    if (name == '') {
      blockExecutable['stage'] = blockExecutable['credentials']
    } else {
      blockExecutable['stage'] = { stage(name, blockExecutable['credentials']) }
    }

    if (config['wrappers']) {
      blockExecutable['wrappers'] = []
      blockExecutable['wrappers'] << blockExecutable['stage']
      config['wrappers'].eachWithIndex { item, index ->
        blockExecutable['wrappers'] << { wrap(item, blockExecutable['wrappers'][index]) }
      }
      blockExecutable['wrappersFinal'] = blockExecutable['wrappers'].last()
    } else {
      blockExecutable['wrappersFinal'] = blockExecutable['stage']
    }


    blockExecutable['final'] = blockExecutable['wrappersFinal']

    def blockError = false
    def localProperties = []
    if (blockParameters) { localProperties << parameters(blockParameters) }
    if (blockTriggers) { localProperties << pipelineTriggers(blockTriggers) }
    blockOptions.each {opt -> localProperties << opt}
    properties(localProperties)

    def blockCode = {
      try {
        config['tools'].each { tul ->
          tool(tul)
        }
        blockExecutable['final']()
      } catch(org.jenkinsci.plugins.workflow.steps.FlowInterruptedException | hudson.AbortException e) {
        blockPost.aborted(error: e)
        currentBuild.result = "ABORTED"
        blockError = e
      } catch (e) {
        blockPost.failure(error: e)
        currentBuild.result = "FAILURE"
        blockError = e
      } finally {
        if (currentBuild.currentResult == "UNSTABLE") {
          blockPost.unstable()
        }
        if (currentBuild.currentResult == "SUCCESS") {
          blockPost.success()
        }
        blockPost.always()
        if (blockError && !blockIgnoreFailure) {
          throw blockError
        }
      }
    }
    blockCode.resolveStrategy = Closure.DELEGATE_FIRST
    blockCode.delegate = this

    switch(config['agent']['type']) {
      case 'label':
        node(config['agent']['config']) {
          blockCode()
        }
        break
      case 'podTemplate':
        podTemplate(config['agent']['config']) {
          node(config['agent']['config']['label']) {
            blockCode()
          }
        }
        break
      default:
        blockCode()
        break
    }
  } else {
    echo "Skipping block due to when condition"
  }
}
