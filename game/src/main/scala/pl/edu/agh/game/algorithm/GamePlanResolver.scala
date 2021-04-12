package pl.edu.agh.game.algorithm

import pl.edu.agh.game.algorithm.GameUpdate.{CreateLife, RemoveLife}
import pl.edu.agh.game.config.GameConfig
import pl.edu.agh.game.model.Life
import pl.edu.agh.xinuk.algorithm.{Metrics, PlanResolver, Update}
import pl.edu.agh.xinuk.model.{CellContents, Empty}

case class GamePlanResolver() extends PlanResolver[GameConfig] {
  override def isUpdateValid(iteration: Long, contents: CellContents, update: Update)(implicit config: GameConfig): Boolean = (contents, update) match {
    case (Life, RemoveLife) => true
    case (Empty, CreateLife) => true
    case _ => false
  }

  override def applyUpdate(iteration: Long, contents: CellContents, update: Update)(implicit config: GameConfig): (CellContents, Metrics) = {
    val (newContents: CellContents, metrics: GameMetrics) = (contents, update) match {
      case (Life, RemoveLife) =>
        (Empty, GameMetrics.empty)
      case (Empty, CreateLife) =>
        (Life, GameMetrics.empty)

      case _ => throw new IllegalArgumentException(s"Illegal update applied: state = $contents, update = $update")
    }

    (newContents, metrics)
  }
}