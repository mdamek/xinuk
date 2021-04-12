package pl.edu.agh.game

import java.awt.Color

import pl.edu.agh.game.model.Life
import pl.edu.agh.xinuk.Simulation
import pl.edu.agh.xinuk.model.grid.GridSignalPropagation
import com.typesafe.scalalogging.LazyLogging
import pl.edu.agh.game.algorithm.{GameMetrics, GamePlanCreator, GamePlanResolver, GameWorldCreator}

object GameMain extends LazyLogging {
  private val configPrefix = "game"
  private val metricHeaders = Vector(
  )

  def main(args: Array[String]): Unit = {
    import pl.edu.agh.xinuk.config.ValueReaders._
    new Simulation(
      configPrefix,
      metricHeaders,
      GameWorldCreator,
      GamePlanCreator,
      GamePlanResolver,
      GameMetrics.empty,
      GridSignalPropagation.Standard,
      {
        case cellState =>
          cellState.contents match {
            case Life => Color.RED
            case _ => Color.BLACK
          }
      }).start()
  }
}
