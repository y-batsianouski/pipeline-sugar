package org.ybatsianouski.scriptpipeline

class Agent implements Serializable {
  private body
  private delegate
  private p
  private pipeline
  public config = {}
  Agent(pipeline, body) {
    this.config   = new Config(pipeline)
    this.p        = this.pipeline
    this.pipeline = pipeline
    this.body     = body
  }
  
  def run(Closure steps) {
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body.call()
    
    switch(config.type) {
      case Config.TYPE_NODE:
        pipeline.node(config.node.label) {
          if (config.node.customWorkspace) {
            pipeline.ws(config.node.customWorkspace) {
              steps.call()
            }
          } else {
            steps.call()
          }
        }
        break
      case Config.TYPE_POD_TEMPLATE:
        pipelinet.podTemplate(config.podTemplate) {
          parent.node(config.podTemplate['label']) {
            steps.call()
          }
        }
        break
      case Config.TYPE_NONE:
        steps.call()
        break
    }
  }
  
  class Config implements Serializable {
    static final String TYPE_NODE         = 'node'
    static final String TYPE_NONE         = 'none'
    static final String TYPE_POD_TEMPLATE = 'podTemplate'
    
    private configured  = false
    private p
    private pipeline
    public node
    public podTemplate = false
    public type        = TYPE_NONE
    Config(pipeline) {
      this.p        = this.pipeline
      this.pipeline = pipeline
      this.node     = new Node(this.pipeline)
    }
    
    def label(String l) {
      node() { label l }
    }
    
    def podTemplate(Map podTemplate) {
      if (configured == false) {
        podTemplate = podTemplate
        type   = TYPE_POD_TEMPLATE
        configured = true
      } else {
        pipeline.error("Couldn't configure multiple agents")
      }
    }
    
    def none() {
      if (configured == false) {
        type   = TYPE_NONE
        configured = true
      } else {
        pipeline.error("Couldn't configure multiple agents")
      }
    }
    
    def any() {
      node() { label "" }
    }
    
    def node(Closure body) {
      if (configured == false) {
        body.resolveStrategy = Closure.DELEGATE_FIRST
        body.delegate = node
        body.call()
        type   = TYPE_NODE
        configured = true
      } else {
        pipeline.error("Couldn't configure multiple agents")
      }
    }
    
    class Node implements Serializable {
      private p
      private pipeline
      public label = ""
      public customWorkspace = false
      Node(pipeline) {
        this.p        = this.pipeline
        this.pipeline = pipeline
      }
      
      def label(String l) {
        label = l
      }
      
      def customWorkspace(String ws) {
        customWorkspace = ws
      }
    }
  }
}