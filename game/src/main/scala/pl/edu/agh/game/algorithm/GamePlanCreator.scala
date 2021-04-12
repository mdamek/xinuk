package pl.edu.agh.game.algorithm

import pl.edu.agh.game.algorithm.GameUpdate.{CreateLife, RemoveLife}
import pl.edu.agh.game.config.GameConfig
import pl.edu.agh.game.model.Life
import pl.edu.agh.xinuk.algorithm.{Metrics, Plan, PlanCreator, Plans}
import pl.edu.agh.xinuk.model.{CellContents, CellId, CellState, Direction}

case class GamePlanCreator() extends PlanCreator[GameConfig] {
  override def createPlans(iteration: Long, cellId: CellId, cellState: CellState, neighbourContents: Map[Direction, CellContents])(implicit config: GameConfig): (Plans, Metrics) = {
    cellState.contents match {
      case Life => (developingDeath(neighbourContents), GameMetrics.empty)
      case _ => (developingLife(neighbourContents), GameMetrics.empty)
    }
  }

  private def developingDeath(neighbourContents: Map[Direction, CellContents]) = {
    if (neighbourContents.count(_._2 == Life) == 2 || neighbourContents.count(_._2 == Life) == 3) {
      Plans.empty
    } else {
      Plans(None -> Plan(RemoveLife))
    }
  }

  private def developingLife(neighbourContents: Map[Direction, CellContents]) = {
    if (neighbourContents.count(_._2 == Life) == 3) {
      Plans(None -> Plan(CreateLife))
    } else {
      Plans.empty
    }
  }
}
