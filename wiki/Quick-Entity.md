# Quick Entity

The Quick Entities panel is a Stream Deck style entity quick control panel. It shows 6 slots on the Ava device, and Home Assistant can change what appears in each slot at any time.

This means the same Ava screen can become a camera panel, light panel, media panel, security panel, or guest panel depending on what is happening at home.

---

## Enable Quick Entities

On the Ava device, open:

`Settings -> Interface & Shortcuts -> Quick Entity Control`

Enable:

`Quick Entity Control`

Then enable:

`HA Slot Configuration`

After that, open the Ava device page in Home Assistant. You should see entities similar to:

```text
switch.xxx_quick_entity_display
text.xxx_quick_entity_slot_1
text.xxx_quick_entity_slot_2
text.xxx_quick_entity_slot_3
text.xxx_quick_entity_slot_4
text.xxx_quick_entity_slot_5
text.xxx_quick_entity_slot_6
```

The `xxx` part depends on your Ava device name. Please copy the real entity IDs from your Home Assistant device page.

`switch.xxx_quick_entity_display` shows or hides the Quick Entities panel.

`text.xxx_quick_entity_slot_1` to `text.xxx_quick_entity_slot_6` decide what appears in each slot.

---

## Preview slots first

Before creating automations, I recommend testing the slots manually.

Open one of the slot text entities in Home Assistant, for example:

`text.xxx_quick_entity_slot_1`

Type a Home Assistant entity ID into it and press Enter.

Example values:

```text
light.living_room
switch.tv_power
scene.movie_mode
script.good_night
camera.front_door
```

Ava will update the panel in real time. This is the easiest way to preview different entities and choose the ones you like before writing an automation.

For example, you can type `camera.front_door` into slot 1 and immediately check how the camera tile looks on Ava. Then you can try `light.front_door`, `scene.movie_mode`, or any other entity ID.

---

## Tap behavior

When you tap a slot, Ava automatically calls the correct Home Assistant service for supported entity types:

```text
switch.*         switch.toggle
light.*          light.toggle
fan.*            fan.toggle
cover.*          cover.toggle
input_boolean.*  input_boolean.toggle
automation.*     automation.toggle
button.*         button.press
script.*         script.turn_on
scene.*          scene.turn_on
timer.*          timer.start or timer.pause
```

Camera entities are supported as display tiles. If you put a `camera.*` entity into a slot, Ava can show it directly in the Quick Entities panel.

For camera-related actions, place the action entities in nearby slots. For example:

```text
slot 1: camera.front_door
slot 2: light.front_door
slot 3: lock.front_door
slot 4: script.unlock_front_door_for_guest
```

---

## Show / Hide the panel

```yaml
# Show
service: switch.turn_on
target:
  entity_id: switch.xxx_quick_entity_display

# Hide
service: switch.turn_off
target:
  entity_id: switch.xxx_quick_entity_display
```

---

## Example 1: Doorbell panel

When someone rings the doorbell, Ava switches the panel to a useful front-door control screen. It shows the camera, entrance light, door lock, and a guest unlock script.

```yaml
alias: Ava doorbell panel
mode: restart
trigger:
  - platform: state
    entity_id: binary_sensor.doorbell
    to: "on"

action:
  - service: text.set_value
    target:
      entity_id: text.ava_quick_entity_slot_1
    data:
      value: camera.front_door

  - service: text.set_value
    target:
      entity_id: text.ava_quick_entity_slot_2
    data:
      value: light.front_door

  - service: text.set_value
    target:
      entity_id: text.ava_quick_entity_slot_3
    data:
      value: lock.front_door

  - service: text.set_value
    target:
      entity_id: text.ava_quick_entity_slot_4
    data:
      value: script.unlock_front_door_for_guest

  - service: text.set_value
    target:
      entity_id: text.ava_quick_entity_slot_5
    data:
      value: script.say_leave_at_door

  - service: switch.turn_on
    target:
      entity_id: switch.ava_quick_entity_display
```

---

## Example 2: Room-aware panel

This example changes the Ava panel based on where activity is detected. If motion is detected in the living room, it shows living room controls. If motion is detected near the entrance, it shows entrance controls.

```yaml
alias: Ava room aware quick panel
mode: restart
trigger:
  - platform: state
    entity_id: binary_sensor.living_room_motion
    to: "on"
    id: living_room

  - platform: state
    entity_id: binary_sensor.entry_motion
    to: "on"
    id: entry

action:
  - choose:
      - conditions:
          - condition: trigger
            id: living_room
        sequence:
          - service: text.set_value
            target:
              entity_id: text.ava_quick_entity_slot_1
            data:
              value: light.living_room

          - service: text.set_value
            target:
              entity_id: text.ava_quick_entity_slot_2
            data:
              value: media_player.living_room_tv

          - service: text.set_value
            target:
              entity_id: text.ava_quick_entity_slot_3
            data:
              value: cover.living_room_curtain

          - service: text.set_value
            target:
              entity_id: text.ava_quick_entity_slot_4
            data:
              value: scene.movie_mode

      - conditions:
          - condition: trigger
            id: entry
        sequence:
          - service: text.set_value
            target:
              entity_id: text.ava_quick_entity_slot_1
            data:
              value: camera.front_door

          - service: text.set_value
            target:
              entity_id: text.ava_quick_entity_slot_2
            data:
              value: light.entry

          - service: text.set_value
            target:
              entity_id: text.ava_quick_entity_slot_3
            data:
              value: lock.front_door

          - service: text.set_value
            target:
              entity_id: text.ava_quick_entity_slot_4
            data:
              value: alarm_control_panel.home_alarm

  - service: switch.turn_on
    target:
      entity_id: switch.ava_quick_entity_display
```

---

## Example 3: Security panel when leaving home

When nobody is home, Ava can switch to a security-focused panel. This is useful for wall tablets or Portal-style devices near the entrance.

```yaml
alias: Ava away security panel
mode: single
trigger:
  - platform: state
    entity_id: group.family
    to: "not_home"

action:
  - service: text.set_value
    target:
      entity_id: text.ava_quick_entity_slot_1
    data:
      value: camera.front_door

  - service: text.set_value
    target:
      entity_id: text.ava_quick_entity_slot_2
    data:
      value: camera.back_yard

  - service: text.set_value
    target:
      entity_id: text.ava_quick_entity_slot_3
    data:
      value: alarm_control_panel.home_alarm

  - service: text.set_value
    target:
      entity_id: text.ava_quick_entity_slot_4
    data:
      value: lock.front_door

  - service: text.set_value
    target:
      entity_id: text.ava_quick_entity_slot_5
    data:
      value: light.outdoor_lights

  - service: text.set_value
    target:
      entity_id: text.ava_quick_entity_slot_6
    data:
      value: script.all_lights_off

  - service: switch.turn_on
    target:
      entity_id: switch.ava_quick_entity_display
```

This keeps the Ava UI simple, but allows Home Assistant to replace the six visible slots in real time. For small wall tablets and Portal-style devices, this works better than showing too many buttons at once.

---

## FAQ

### Quick Entity panel not controlling entities?

1. Check if entity IDs are correct
2. Check if entities exist in Home Assistant
3. Check if the entity type is supported (see Tap behavior table above)
4. Test the entity in HA Developer Tools

### Panel not showing?

1. Check if Quick Entity Control is enabled
2. Check overlay permission
3. Try restarting service

### How to hide the panel?

1. Long press the panel for 5 seconds
2. Or use the HA switch entity `switch.xxx_quick_entity_display`
3. Or toggle from the sidebar

### Slot content not updating?

1. Check if HA Slot Configuration is enabled
2. Verify the entity ID is valid
3. Check network connection between Ava and HA

---

*Back to [Home](Home)*
