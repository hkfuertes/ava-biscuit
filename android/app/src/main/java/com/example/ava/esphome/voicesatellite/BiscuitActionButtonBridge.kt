package com.example.ava.esphome.voicesatellite

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class BiscuitActionButtonBridge(
    var onLocalPress: () -> Unit = {}
) {
    private val _independent = MutableStateFlow(false)
    val independent = _independent.asStateFlow()

    private val _pressed = MutableStateFlow(false)
    val pressed = _pressed.asStateFlow()

    fun setIndependent(value: Boolean) {
        _independent.value = value
    }

    fun onPhysicalButton(pressed: Boolean) {
        if (_pressed.value == pressed) return
        _pressed.value = pressed
        if (pressed && !_independent.value) onLocalPress()
    }

    companion object {
        val shared = BiscuitActionButtonBridge()
    }
}
