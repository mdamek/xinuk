package pl.edu.agh.game.algorithm

import pl.edu.agh.xinuk.algorithm.Metrics

case class GameMetrics() extends Metrics {
  override def log: String = {""}

  override def series: Vector[(String, Double)] = Vector()

  override def +(other: Metrics): GameMetrics = {
    other match {
      case GameMetrics.Empty => this

      case _ => throw new UnsupportedOperationException(s"Cannot add non-GameMetrics to GameMetrics")
    }
  }
}

object GameMetrics {
  private val Empty = GameMetrics()
  def empty: GameMetrics = Empty
}
