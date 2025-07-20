package com.nak.engine.input;

public enum InputAction {
    // Movement
    MOVE_FORWARD,
    MOVE_BACKWARD,
    MOVE_LEFT,
    MOVE_RIGHT,
    MOVE_UP,
    MOVE_DOWN,
    MOVE_FORWARD_SLOW,
    MOVE_BACKWARD_SLOW,
    MOVE_LEFT_SLOW,
    MOVE_RIGHT_SLOW,

    // Movement modifiers
    WALK,
    CROUCH,

    // Camera
    RESET_CAMERA,
    TELEPORT_RANDOM,
    TOGGLE_GRAVITY,
    TOGGLE_CAMERA_MODE,
    CAMERA_SHAKE_TEST,

    // Preset positions
    PRESET_POSITION_1,
    PRESET_POSITION_2,
    PRESET_POSITION_3,
    PRESET_POSITION_4,
    PRESET_POSITION_5,

    // View adjustments
    INCREASE_FOV,
    DECREASE_FOV,
    INCREASE_SENSITIVITY,
    DECREASE_SENSITIVITY,

    // Debug and display
    TOGGLE_DEBUG,
    TOGGLE_WIREFRAME,
    TOGGLE_FULLSCREEN,

    // System
    TOGGLE_MOUSE_LOCK,
    EXIT_APPLICATION
}