package com.example.roadbook.ui

import android.view.KeyEvent
import com.example.roadbook.model.ScrollDirection
import com.example.roadbook.viewmodel.RoadbookViewModel

class ControllerManager(
    private val viewModel: RoadbookViewModel,
    private val onScrollRequest: (ScrollDirection) -> Unit,
    private val onActionHub: () -> Unit
) {

    fun handleKeyEvent(event: KeyEvent): Boolean {
        val keyCode = event.keyCode
        val isKeyDown = event.action == KeyEvent.ACTION_DOWN

        if (!isKeyDown) return false

        val isControllerKey = keyCode in listOf(
            KeyEvent.KEYCODE_MEDIA_PREVIOUS, KeyEvent.KEYCODE_MEDIA_NEXT,
            KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
            KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_MINUS, KeyEvent.KEYCODE_PLUS, KeyEvent.KEYCODE_EQUALS, KeyEvent.KEYCODE_C
        )

        if (isControllerKey) {
            if (viewModel.bluetoothDeviceName.value == "Nie wykryto") {
                viewModel.bluetoothDeviceName.value = "PGA Rally Controller"
            }

            when (keyCode) {
                KeyEvent.KEYCODE_MEDIA_PREVIOUS, KeyEvent.KEYCODE_MEDIA_NEXT,
                KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                    viewModel.controllerProfile.value = "Roadbook"
                }
                KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_RIGHT,
                KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_MINUS, KeyEvent.KEYCODE_PLUS, KeyEvent.KEYCODE_EQUALS, KeyEvent.KEYCODE_C -> {
                    viewModel.controllerProfile.value = "Navigation"
                }
            }

            if (viewModel.showStartupDialog.value) {
                if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE || keyCode == KeyEvent.KEYCODE_C) {
                    onActionHub()
                }
                return true
            }

            val repeatCount = event.repeatCount
            val progressiveKeyStep = when {
                repeatCount <= 4 -> 10.0
                repeatCount <= 12 -> 100.0
                repeatCount <= 20 -> 500.0
                else -> 1000.0
            }

            when (keyCode) {
                KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                    onScrollRequest(ScrollDirection.UP)
                    return true
                }
                KeyEvent.KEYCODE_MEDIA_NEXT -> {
                    onScrollRequest(ScrollDirection.DOWN)
                    return true
                }
                KeyEvent.KEYCODE_VOLUME_UP -> {
                    viewModel.totalDistanceMeters.value += progressiveKeyStep
                    viewModel.recalculateRoadbookRowOnlyPublic()
                    return true
                }
                KeyEvent.KEYCODE_VOLUME_DOWN -> {
                    if (viewModel.totalDistanceMeters.value >= progressiveKeyStep) {
                        viewModel.totalDistanceMeters.value -= progressiveKeyStep
                    } else {
                        viewModel.totalDistanceMeters.value = 0.0
                    }
                    viewModel.recalculateRoadbookRowOnlyPublic()
                    return true
                }
                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                    onActionHub()
                    return true
                }

                KeyEvent.KEYCODE_DPAD_DOWN -> {
                    onScrollRequest(ScrollDirection.UP)
                    return true
                }
                KeyEvent.KEYCODE_DPAD_UP -> {
                    onScrollRequest(ScrollDirection.DOWN)
                    return true
                }
                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    viewModel.tripDistanceMeters.value = 0.0
                    return true
                }
                KeyEvent.KEYCODE_DPAD_LEFT -> {
                    viewModel.isAutoScrollEnabled.value = !viewModel.isAutoScrollEnabled.value
                    return true
                }
                KeyEvent.KEYCODE_MINUS -> {
                    if (viewModel.totalDistanceMeters.value >= progressiveKeyStep) {
                        viewModel.totalDistanceMeters.value -= progressiveKeyStep
                    } else {
                        viewModel.totalDistanceMeters.value = 0.0
                    }
                    viewModel.recalculateRoadbookRowOnlyPublic()
                    return true
                }
                KeyEvent.KEYCODE_PLUS, KeyEvent.KEYCODE_EQUALS -> {
                    viewModel.totalDistanceMeters.value += progressiveKeyStep
                    viewModel.recalculateRoadbookRowOnlyPublic()
                    return true
                }
                KeyEvent.KEYCODE_C -> {
                    onActionHub()
                    return true
                }
            }
        }

        return false
    }
}