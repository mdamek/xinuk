package pl.edu.agh.mock.algorithm

import pl.edu.agh.mock.config.MockConfig
import pl.edu.agh.mock.model.Mock
import pl.edu.agh.xinuk.algorithm.WorldCreator
import pl.edu.agh.xinuk.model._
import pl.edu.agh.xinuk.model.grid.{GridCellId, GridWorldBuilder}
import pl.edu.agh.xinuk.simulation.OutsideInitialPositionProvider

object MockWorldCreator extends WorldCreator[MockConfig] {

  def ConvertStringToType(value: String, types: List[CellContents]): Option[CellContents] = {
    types.foreach(typeValue => {
      if (value != null && typeValue.getClass.getSimpleName.toLowerCase.contains(value.toLowerCase)) {
        Option(typeValue)
      }
    })
    None
  }

  override def prepareWorld(initialPositions: Array[Array[String]])(implicit config: MockConfig): GridWorldBuilder = {
    val worldBuilder: GridWorldBuilder = GridWorldBuilder().withGridConnections()
    val availableTypes: List[CellContents] = List(Mock)

    if (initialPositions.isEmpty) {
      makeGrid(3, worldBuilder)
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

  private def makeGrid(n: Int, worldBuilder: GridWorldBuilder)(implicit config: MockConfig): Unit = {
    val xUnit = config.worldWidth / (n + 1)
    val yUnit = config.worldHeight / (n + 1)
    for {
      xIdx <- 1 to n
      yIdx <- 1 to n
    } {
      worldBuilder(GridCellId(xIdx * xUnit, yIdx * yUnit)) = CellState(Mock)
    }
  }
}
