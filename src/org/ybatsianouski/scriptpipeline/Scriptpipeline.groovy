package org.ybatsianouski.scriptpipeline

class Scriptpipeline implements Serializable {
  private delegate
  private p
  private pipeline
  public config = {}
  Scriptpipeline(pipeline, Closure body) {
    this.pipeline = pipeline
    this.p        = this.pipeline
    this.config   = new Config(pipeline)
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = this.config
    body()
  }
  
  def run() {
    config.steps.call()
  }
  
  class Config implements Serializable {
    private p
    private pipeline
    public steps = false
    Config(pipeline) {
      this.pipeline = pipeline
      this.p        = this.pipeline
    }
    
    def steps(Closure body) {
      if(steps == false) {
        steps = body
        steps.resolveStrategy = Closure.DELEGATE_FIRST
        steps.delegate        = pipeline
      } else {
        throw new RuntimeException("ERROR: Multiple occurrences of the steps section")
      }
    }
  }
}
