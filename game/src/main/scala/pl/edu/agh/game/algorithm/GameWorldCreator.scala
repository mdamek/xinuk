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
    val availableTypes: List[CellContents] = List(Life)
    if (initialPositions.isEmpty) {
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
    } else {
      for {
        x <- initialPositions.indices
        y <- initialPositions(0).indices
      } {
        val contents: Option[CellContents] = ConvertStringToType(initialPositions(x)(y), availableTypes)
        contents.foreach(c => worldBuilder(GridCellId(x, y)) = CellState(c))
      }
    }
    worldBuilder
  }
}
