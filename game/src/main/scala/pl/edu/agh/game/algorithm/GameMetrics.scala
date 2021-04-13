package pl.edu.agh.game.algorithm

import pl.edu.agh.xinuk.algorithm.Metrics

case class GameMetrics(lifeCount: Long,
                       deathCount: Long) extends Metrics {
  override def log: String = {
    s"$lifeCount;$deathCount;"
  }

  override def series: Vector[(String, Double)] = Vector(
    "Life" -> lifeCount.toDouble,
    "Death" -> deathCount.toDouble
  )

  override def +(other: Metrics): GameMetrics = {
    other match {
      case GameMetrics.Empty => this
      case GameMetrics(otherLifeCount, otherDeathCount) =>
        GameMetrics(
          lifeCount + otherLifeCount,
          deathCount + otherDeathCount)
      case _ => throw new UnsupportedOperationException(s"Cannot add non-GameMetrics to GameMetrics")
    }
  }
}

object GameMetrics {
  val MetricHeaders = Vector(
    "lifeCount",
    "deathCount"
  )
  private val Empty = GameMetrics(0, 0)
  private val Life = GameMetrics(1, 0)
  private val Death = GameMetrics(0, 1)

  def empty: GameMetrics = Empty
  def life: GameMetrics = Life
  def death: GameMetrics = Death
}
