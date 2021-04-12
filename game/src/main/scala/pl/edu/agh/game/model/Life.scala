package pl.edu.agh.game.model

import pl.edu.agh.xinuk.config.XinukConfig
import pl.edu.agh.game.config.GameConfig
import pl.edu.agh.xinuk.model.{CellContents, Signal}

case object Life extends CellContents {
  override def generateSignal(iteration: Long)(implicit config: XinukConfig): Signal =
    config.asInstanceOf[GameConfig].lifeInitialSignal
}