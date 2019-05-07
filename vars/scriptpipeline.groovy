import org.ybatsianouski.scriptpipeline.Scriptpipeline

def call(Closure body) {
  def sp = new Scriptpipeline(this, body)
  sp.run()
}
