package com.squareup.benchmarks.performance.poetry

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.squareup.sample.container.SampleContainers
import com.squareup.sample.poetry.PoemsBrowserWorkflow
import com.squareup.sample.poetry.model.Poem
import com.squareup.workflow1.ui.WorkflowLayout
import com.squareup.workflow1.ui.WorkflowUiExperimentalApi
import com.squareup.workflow1.ui.backstack.BackStackContainer
import com.squareup.workflow1.ui.plus
import com.squareup.workflow1.ui.renderWorkflowIn
import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber

@OptIn(WorkflowUiExperimentalApi::class)
private val viewRegistry = SampleContainers + BackStackContainer

public class PerformancePoetryActivity : AppCompatActivity() {
  @OptIn(WorkflowUiExperimentalApi::class)
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val model: PoetryModel by viewModels()
    setContentView(
      WorkflowLayout(this).apply { start(lifecycle, model.renderings, viewRegistry) }
    )
  }

  public companion object {
    init {
      Timber.plant(Timber.DebugTree())
    }
  }
}

public class PoetryModel(savedState: SavedStateHandle) : ViewModel() {
  @OptIn(WorkflowUiExperimentalApi::class)
  public val renderings: StateFlow<Any> by lazy {
    renderWorkflowIn(
      workflow = PoemsBrowserWorkflow,
      scope = viewModelScope,
      prop = Poem.allPoems,
      savedStateHandle = savedState
    )
  }
}
