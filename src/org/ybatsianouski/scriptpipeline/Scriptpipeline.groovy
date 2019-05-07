package org.ybatsianouski.scriptpipeline

import org.ybatsianouski.scriptpipeline.Agent

class Scriptpipeline implements Serializable {
  private body
  private p
  private pipeline
  private wrap_steps = [:]
  public config = {}
  Scriptpipeline(pipeline, Closure body) {
    this.body     = body
    this.config   = new Config(pipeline)
    this.p        = pipeline
    this.pipeline = pipeline
  }
  
  def run() {
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = this.config
    body.call()
    pipeline.properties(config.options.options + [pipeline.parameters(config.parameters)])
    config.agent.run {
      if (config.agent.config.type != config.agent.config.TYPE_NONE) {
        if (config.options.skipDefaultCheckout != true) {
          wrap_steps['checkoutScm'] = {
            if (config.options.checkoutToSubdirectory != ".") {
              pipeline.dir(config.options.checkoutToSubdirectory) {
                pipeline.checkout pipeline.scm
              }
            } else {
              pipeline.checkout pipeline.scm
            }
            config.steps.call()
          }
        } else {
          wrap_steps['checkoutScm'] = config.steps
        }
        wrap_steps['steps'] = [wrap_steps['checkoutScm']]
        config.options.wrappers.eachWithIndex { v, k -> wrap_steps['steps'] << { pipeline.wrap(v, wrap_steps['steps'][k]) } }
        wrap_steps['wrapLast'] = wrap_steps['steps'].last()
        
        wrap_steps['wrapLast'].call()
      } else {
        config.steps.call()
      }
    }
  }
  
  class Config implements Serializable {
    private agent_configured = false
    private steps_configured = false
    private p
    private pipeline
    /* need to be able to specify agent none in pipeline */
    private none = { none() }
    /* need to be able to specify agent any in pipeline */
    private any  = { node { label "" } }
    private parameters
    
    public options
    public agent      = false
    public steps      = { it -> false }
    Config(pipeline) {
      this.pipeline   = pipeline
      this.p          = pipeline
      this.agent      = new Agent(pipeline, none)
      this.parameters = new Parameters(pipeline)
      this.options = new Options(pipeline)
    }
    
    def steps(Closure body) {
      if(steps_configured == false) {
        steps = body
        steps.resolveStrategy = Closure.DELEGATE_FIRST
        steps.delegate        = pipeline
        steps_configured = true
      } else {
        pipeline.error("Multiple occurrences of the steps section")
      }
    }
    
    def agent(Closure body) {
      if(agent_configured == false) {
        agent = new Agent(this.pipeline, body)
        agent_configured = true
      } else {
        pipeline.error("Multiple occurrences of the steps section")
      }
    }
    
    def parameters(Closure body) {
      body.resolveStrategy = Closure.DELEGATE_FIRST
      body.delegate = parameters
      body.call()
    }
    
    def getParameters() {
      parameters.parameters
    }
    
    def options(Closure body) {
      body.resolveStrategy = Closure.DELEGATE_FIRST
      body.delegate = options
      body.call()
    }
    
    class Parameters implements Serializable {
      private p
      private pipeline
      public parameters = []
      Parameters(pipeline) {
        this.p        = pipeline
        this.pipeline = pipeline
      }
      
      def string(Map conf) {
        parameters << pipeline.string(conf)
      }
      
      def text(Map conf) { parameters << pipeline.text(conf) }
      
      def choice(Map conf) {
        if (conf["choices"] instanceof Collection) {
          conf["choices"] = conf["choices"].join("\n")
        }
        parameters << pipeline.choice(conf)
      }
      
      def file(Map conf) { parameters << pipeline.file(conf) }
      
      def booleanParam(Map conf) { parameters << pipeline.booleanParam(conf) }
      
      def password(Map conf) { parameters << pipeline.password(conf) }
      
      def generic(conf) { parameters << conf }
    }
    
    class Options implements Serializable {
      private p
      private pipeline
      public options  = []
      public wrappers = []
      public checkoutToSubdirectory = "."
      public skipDefaultCheckout = false
      public skipStagesAfterUnstable = false
      public timeout = false
      public parallelsAlwaysFailFast = false
      public retry = false
      Options(pipeline) {
        this.p        = pipeline
        this.pipeline = pipeline
      }
      
      def checkoutToSubdirectory(String path) {
        checkoutToSubdirectory = path
      }
      
      def disableConcurrentBuilds() {
        options << pipeline.disableConcurrentBuilds()
      }
      
      def overrideIndexTriggers(Boolean flag) {
        options << pipeline.overrideIndexTriggers(flag)
      }
      
      def preserveStashes(Map conf) {
        options << pipeline.preserveStashes(conf)
      }
      
      def skipDefaultCheckout(Boolean flag) {
        skipDefaultCheckout = flag
      }
      
      def skipStagesAfterUnstable() {
        skipStagesAfterUnstable = true
      }
      
      def parallelsAlwaysFailFast() {
        parallelsAlwaysFailFast = true
      }
      
      def retry(num) {
        retry = num
      }
      
      def timestamps() {
        wrappers << [$class: 'TimestamperBuildWrapper']
      }
      
      def timeout(conf) {
        timeout = conf
      }
      
      def ansiColor(String scheme = 'xterm') {
        wrappers << [$class: 'AnsiColorBuildWrapper', colorMapName: scheme]
      }
      
      def genericOption(conf) {
        options << conf
      }
      
      def genericWrapper(conf) {
        wrappers << conf
      }
    }
  }
}