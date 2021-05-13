package pl.edu.agh.game.algorithm

import pl.edu.agh.game.config.GameConfig
import pl.edu.agh.game.model.Life
import pl.edu.agh.xinuk.algorithm.WorldCreator
import pl.edu.agh.xinuk.model.{CellContents, CellState, WorldBuilder}
import pl.edu.agh.xinuk.model.grid.{GridCellId, GridWorldBuilder}
import pl.edu.agh.xinuk.simulation.OutsideInitialPositionProvider

object GameWorldCreator extends WorldCreator[GameConfig] {

  def ConvertStringToType(value: String, types: List[CellContents]): Option[CellContents] = {
    types.foreach(typeValue => {
      if (value != null && typeValue.getClass.getSimpleName.toLowerCase.contains(value.toLowerCase)) {
        Option(typeValue)
      }
    })
    None
  }

  override def prepareWorld(initialPositions: Array[Array[String]])(implicit config: GameConfig): WorldBuilder = {
    val worldBuilder = GridWorldBuilder().withGridConnections()
    if (initialPositions(0).length == 0) {
      for {
        x <- 0 until config.worldWidth
        y <- 0 until config.worldHeight
      } {
        if (config.random.nextDouble() < config.lifeSpawnChance) {
          worldBuilder(GridCellId(x, y)) = CellState(Life)
        }
      }
    }
    else {
      for {
        x <- initialPositions(0).indices
        y <- initialPositions.indices
      } {
        if (initialPositions(y)(x) != null) {
          worldBuilder(GridCellId(x, y)) = CellState(Life)
        }
      }
    }
    worldBuilder
  }
}
