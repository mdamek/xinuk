package pl.edu.agh.game.algorithm

import pl.edu.agh.game.config.GameConfig
import pl.edu.agh.game.model.Life
import pl.edu.agh.xinuk.algorithm.WorldCreator
import pl.edu.agh.xinuk.model.{CellContents, CellState, WorldBuilder}
import pl.edu.agh.xinuk.model.grid.{GridCellId, GridWorldBuilder}

object GameWorldCreator extends WorldCreator[GameConfig] {
  override def prepareWorld()(implicit config: GameConfig): WorldBuilder = {
    val worldBuilder = GridWorldBuilder().withGridConnections()
    val outsidePositions = Array.ofDim[Int](config.worldWidth, config.worldHeight)

    if (config.loadFromOutside) {
      for {
        x <- 0 until config.worldWidth
        y <- 0 until config.worldHeight
      } {
        val contents: Option[CellContents] = if (outsidePositions(x)(y) > 0) {
          Some(Life)
        } else {
          None
        }
        contents.foreach(c => worldBuilder(GridCellId(x, y)) = CellState(c))
      }
    } else {
      for {
        x <- 0 until config.worldWidth
        y <- 0 until config.worldHeight
      } {
        val contents: Option[CellContents] = if (config.random.nextDouble() < config.lifeSpawnChance) {
          Some(Life)
        }
        else {
          None
        }
        contents.foreach(c => worldBuilder(GridCellId(x, y)) = CellState(c))
      }
    }
    worldBuilder
  }
}
