package org.ybatsianouski.scriptpipeline

class Agent implements Serializable {
  private body
  private delegate
  private p
  private pipeline
  public config = {}
  Agent(pipeline, Closure body) {
    this.config   = new Config(pipeline)
    this.p        = this.pipeline
    this.pipeline = pipeline
    this.body     = body
  }
  
  def run(Closure steps) {
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = this.config
    body.call()
    
    steps.resolveStrategy = Closure.DELEGATE_FIRST
    steps.delegate = this.pipeline
    
    switch(config.type) {
      case TYPE_LABEL:
        pipeline.node(config) {
          steps()
        }
        break
      case TYPE_POD_TEMPLATE:
        parent.podTemplate(config) {
          parent.node(config['label']) {
            steps()
          }
        }
        break
      case TYPE_NONE:
        body()
        break
    }
  }
  
  class Config implements Serializable {
    static final String TYPE_LABEL        = 'label'
    static final String TYPE_NONE         = 'none'
    static final String TYPE_POD_TEMPLATE = 'podTemplate'
    
    private p
    private pipeline
    public config     = false
    public configured = false
    public type       = TYPE_NONE
    Config(pipeline) {
      this.p        = this.pipeline
      this.pipeline = pipeline
    }
    
    def label(String label) {
      if (configured == false) {
        config = label
        type   = Agent.TYPE_LABEL
      } else {
        throw new RuntimeException("ERROR: Couldn't configure multiple agents")
      }
    }
    
    def podTemplate(Map conf) {
      if (configured == false) {
        config = conf
        type   = Agent.TYPE_POD_TEMPLATE
      } else {
        throw new RuntimeException("ERROR: Couldn't configure multiple agents")
      }
    }
    
    def none() {
      if (configured == false) {
        config = {}
        type   = Agent.TYPE_NONE
      } else {
        throw new RuntimeException("ERROR: Couldn't configure multiple agents")
      }
    }
  }
}