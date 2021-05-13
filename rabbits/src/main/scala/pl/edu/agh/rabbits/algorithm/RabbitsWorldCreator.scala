package pl.edu.agh.rabbits.algorithm

import pl.edu.agh.rabbits.config.RabbitsConfig
import pl.edu.agh.rabbits.model.{Lettuce, Rabbit}
import pl.edu.agh.xinuk.algorithm.WorldCreator
import pl.edu.agh.xinuk.model.{CellContents, CellState, Empty, WorldBuilder}
import pl.edu.agh.xinuk.model.grid.{GridCellId, GridWorldBuilder}

object RabbitsWorldCreator extends WorldCreator[RabbitsConfig] {
  override def prepareWorld(initialPositions: Array[Array[String]])(implicit config: RabbitsConfig): WorldBuilder = {
    val worldBuilder = GridWorldBuilder().withGridConnections()
    if (initialPositions(0).length == 0) {
      for {
        x <- 0 until config.worldWidth
        y <- 0 until config.worldHeight
        if config.random.nextDouble() < config.spawnChance
      } {
        val contents: CellContents = if (config.random.nextDouble() < config.rabbitSpawnChance) {
          Rabbit(config.rabbitStartEnergy, 0)
        }
        else {
          Lettuce(0)
        }
        worldBuilder(GridCellId(x, y)) = CellState(contents)
      }
    } else {
      for {
        x <- initialPositions(0).indices
        y <- initialPositions.indices
      } {
        if (initialPositions(y)(x) != null) {
          val actualValue = initialPositions(y)(x).toLowerCase
          actualValue match {
            case "rabbit" => worldBuilder(GridCellId(x, y)) = CellState(Rabbit(config.rabbitStartEnergy, 0))
            case "lettuce" => worldBuilder(GridCellId(x, y)) = CellState(Lettuce(0))
            case _ =>
          }
        }
      }
    }
    worldBuilder
  }
}
