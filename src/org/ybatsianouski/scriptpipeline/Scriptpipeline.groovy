package org.ybatsianouski.scriptpipeline

import Agent

class Scriptpipeline implements Serializable {
  private body
  private p
  private pipeline
  public config = {}
  Scriptpipeline(pipeline, Closure body) {
    this.body     = body
    this.config   = new Config(pipeline)
    this.p        = this.pipeline
    this.pipeline = pipeline
  }
  
  def run() {
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = this.config
    body.call()
    
    config.steps.call()
  }
  
  class Config implements Serializable {
    private agent_configured = false
    private p
    private pipeline
    public agent = false
    public steps = { it -> false }
    Config(pipeline) {
      this.pipeline = pipeline
      this.p        = this.pipeline
      this.agent    = new Agent(this.pipeline) { none }
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
    
    def agent(Closure body) {
      if(agent_configured == false) {
        agent = new Agent(this.pipeline, body)
      } else {
        throw new RuntimeException("ERROR: Multiple occurrences of the steps section")
      }
    }
  }
}