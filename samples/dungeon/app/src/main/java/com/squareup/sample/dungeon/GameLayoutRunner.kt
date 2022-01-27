package com.squareup.sample.dungeon

import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_MASK
import android.view.MotionEvent.ACTION_UP
import android.view.View
import com.squareup.sample.dungeon.Direction.DOWN
import com.squareup.sample.dungeon.Direction.LEFT
import com.squareup.sample.dungeon.Direction.RIGHT
import com.squareup.sample.dungeon.Direction.UP
import com.squareup.sample.dungeon.GameWorkflow.GameRendering
import com.squareup.workflow1.ui.LayoutRunner
import com.squareup.workflow1.ui.LayoutRunner.Companion.bind
import com.squareup.workflow1.ui.ViewEnvironment
import com.squareup.workflow1.ui.ViewFactory
import com.squareup.workflow1.ui.WorkflowUiExperimentalApi
import com.squareup.workflow1.ui.WorkflowViewStub

/**
 * Renders a live game, including the board with player and actors, and the buttons to control
 * the player.
 */
@OptIn(WorkflowUiExperimentalApi::class)
class GameLayoutRunner(view: View) : LayoutRunner<GameRendering> {

  private val boardView: WorkflowViewStub = view.findViewById(R.id.board_stub)
  private val moveLeft: View = view.findViewById(R.id.move_left)
  private val moveRight: View = view.findViewById(R.id.move_right)
  private val moveUp: View = view.findViewById(R.id.move_up)
  private val moveDown: View = view.findViewById(R.id.move_down)

  private lateinit var rendering: GameRendering

  init {
    moveLeft.registerPlayerEventHandlers(LEFT)
    moveRight.registerPlayerEventHandlers(RIGHT)
    moveUp.registerPlayerEventHandlers(UP)
    moveDown.registerPlayerEventHandlers(DOWN)
  }

  override fun showRendering(
    rendering: GameRendering,
    viewEnvironment: ViewEnvironment
  ) {
    boardView.update(rendering.board, viewEnvironment)
    this.rendering = rendering

    // Disable the views if we don't have an event handler, e.g. when the game has finished.
    val controlsEnabled = !rendering.gameOver
    moveLeft.isEnabled = controlsEnabled
    moveRight.isEnabled = controlsEnabled
    moveUp.isEnabled = controlsEnabled
    moveDown.isEnabled = controlsEnabled
  }

  private fun View.registerPlayerEventHandlers(direction: Direction) {
    setOnTouchListener { _, motionEvent ->
      when (motionEvent.action and ACTION_MASK) {
        ACTION_DOWN -> rendering.onStartMoving(direction)
        ACTION_UP -> rendering.onStopMoving(direction)
      }
      // Always return false, so the button ripples and animates correctly.
      return@setOnTouchListener false
    }
  }

  companion object : ViewFactory<GameRendering> by bind(
    R.layout.game_layout, ::GameLayoutRunner
  )
}
