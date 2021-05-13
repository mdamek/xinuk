package pl.edu.agh.rabbits.algorithm

import pl.edu.agh.rabbits.config.RabbitsConfig
import pl.edu.agh.rabbits.model.{Lettuce, Rabbit}
import pl.edu.agh.xinuk.algorithm.WorldCreator
import pl.edu.agh.xinuk.model.{CellContents, CellState, Empty, WorldBuilder}
import pl.edu.agh.xinuk.model.grid.{GridCellId, GridWorldBuilder}

object RabbitsWorldCreator extends WorldCreator[RabbitsConfig] {

  def ConvertStringToType(value: String, types: List[CellContents]): CellContents = {
    types.foreach(typeValue => {
      if (value != null && typeValue.getClass.getSimpleName.toLowerCase.contains(value.toLowerCase)) {
        typeValue
      }
    })
    Empty
  }

  override def prepareWorld(initialPositions: Array[Array[String]])(implicit config: RabbitsConfig): WorldBuilder = {
    val worldBuilder = GridWorldBuilder().withGridConnections()
    val availableTypes: List[CellContents] = List(Lettuce(0), Rabbit(config.rabbitStartEnergy, 0))

    if (initialPositions.isEmpty) {
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
        x <- initialPositions.indices
        y <- initialPositions(0).indices
      } {
        val contents: CellContents = ConvertStringToType(initialPositions(x)(y), availableTypes)
        worldBuilder(GridCellId(x, y)) = CellState(contents)
      }
    }


    worldBuilder
  }
}
