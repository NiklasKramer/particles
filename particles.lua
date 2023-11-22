-- P/A/R/T/I/C/L/E/S

engine.name = 'Granular'
a = arc.connect()

local FRAMES_PER_SECOND = 120
local pre_init_monitor_level;
local total_screens = 4



function init()
  pre_init_monitor_level = params:get('monitor_level') -- store 'monitor' level
  params:set('monitor_level', 0) -- mute 'monitor' level

  init_params()
  init_timers()
  update_arc_display()
  screen_dirty = true
end

-- TIMERS --

function init_timers()
  system = ParticleSystem:new()
  particle_timer = metro.init(
    function()
      system:addParticle(64, 32)
      screen_dirty = true
    end, 1 / (FRAMES_PER_SECOND/ 4))
  particle_timer:start()

  redraw_timer = metro.init(
    function() -- what to perform at every tick
      if screen_dirty == true then
        redraw()
        screen_dirty = false
      end
    end,
    1 / FRAMES_PER_SECOND -- how often (15 fps)
  -- the above will repeat forever by default
  )
  redraw_timer:start()

  arc_timer = metro.init(
    function()
      update_arc_display()
    end,
    1 / (FRAMES_PER_SECOND/4)
  )
  arc_timer:start()
end

-- PARAMS --

function init_params()
  params:add_separator('header', 'engine controls')

  add_param('amp', 'Amplitude', 0, 12, 4)
  add_param('buffer', 'Buffer', 0, 5, 1, 1)
  add_param('pos', 'Position', 0, 1, 0.5)
  add_param('dur', 'Duration', 0.05, 3, 0.4)
  add_param('dens', 'Density', 0, 20, 10, 0.1)
  add_param('jitter', 'Jitter', 0, 1, 1)
  add_param('triggerType', 'Trigger Type', 0, 1, 0, 1)
  add_param('rate', 'Rate', 0, 2, 1, 0.5)

  params:add_separator('header', 'wobble')

  add_param('depth', 'Depth', 0, 1, 0.02)
  add_param('mix', 'Mix', 0, 1, 1)

  params:add_separator('header', 'filter')
  add_param('cutoffFreq', 'Cutoff Frequency', 100, 20000, 20000, 0.5, 'exp')

  params:add_separator('header', 'vintage sampler')

  add_param('bitDepth', 'Bit Depth', 8, 24, 24)              -- Adjust the range if needed
  add_param('sampleRate', 'Sample Rate', 5000, 48000, 48000,1, 'exp') -- Adjust the range if needed
  add_param('drive', 'Drive', 0.02, 1, 1)                    -- Adjust the range if needed
  add_param('vmeMix', 'VME Mix', 0, 1, 0)

  params:add_separator('header', 'ARC + General')
 
  params:add_control("arc_sens_1", "Arc Sensitivity 1", controlspec.new(0.01, 2, 'lin', 0.01, 0.2))
  params:add_control("arc_sens_2", "Arc Sensitivity 2", controlspec.new(0.01, 2, 'lin', 0.01, 0.2))
  params:add_control("arc_sens_3", "Arc Sensitivity 3", controlspec.new(0.01, 2, 'lin', 0.01, 0.2))
  params:add_control("arc_sens_4", "Arc Sensitivity 4", controlspec.new(0.01, 2, 'lin', 0.01, 0.2))

  params:add_control('selected_screen', 'Selected Screen', controlspec.new(1, total_screens, 'lin', 1, 1))


  params:default() -- Set each parameter to its default value
end

function add_param(id, name, min, max, default, quant, warp)
  warp = warp or 'lin'  -- Default to linear if warp is not specified
  quant = quant or 0.01  -- Use the provided quant value or default to 0.01
  params:add_control(
    id,                 -- ID
    name,               -- display name
    controlspec.new(
      min,              -- min
      max,              -- max
      warp,             -- warp type, now can be 'lin' or 'exp'
      quant,            -- output quantization
      default           -- default value
    )
  )
  params:set_action(id, function(x) engine[id](x) end)
end

-- BUTTONS & ENCODER --

function key(n, z)
  if z == 1 then -- Check if the button is pressed (not released)
    button_pressed(n)
  end
end

function enc(n, d)
  local selected_screen = params:get("selected_screen")

  if selected_screen == 1 then
    -- Mapping for screen 1
    if n == 1 then
      params:delta('dens', d)
    elseif n == 2 then
      params:delta('dur', d)
    elseif n == 3 then
      params:delta('buffer', d)
    end
  elseif selected_screen == 2 then
    -- Mapping for screen 2
    if n == 1 then
      params:delta('amp', d)
    elseif n == 2 then
      params:delta('rate', d) -- Update 'rate' parameter instead of 'pos'
    elseif n == 3 then
      params:delta('pos', d)
    end
  elseif selected_screen == 3 then
    -- Mapping for screen 3
    if n == 1 then
      params:delta('depth', d) -- Update 'depth' parameter instead of 'rate'
    elseif n == 2 then
      params:delta('mix', d)
    elseif n == 3 then
      params:delta('cutoffFreq', d) -- Update 'cutoffFreq' parameter instead of 'jitter'
    end
  elseif selected_screen == 4 then
    -- Mapping for screen 4
    if n == 1 then
      params:delta('bitDepth', d)
    elseif n == 2 then
      params:delta('sampleRate', d)
    elseif n == 3 then
      params:delta('vmeMix', d)
    end
  end

  screen_dirty = true -- Flag to redraw the screen
end

function button_pressed(button)
  if button == 3 then
    -- Increment the screen
    local new_screen = (params:get("selected_screen") % total_screens) + 1
    params:set("selected_screen", new_screen)
  elseif button == 2 then
    -- Decrement the screen
    local new_screen = (params:get("selected_screen") - 2) % total_screens + 1
    params:set("selected_screen", new_screen)
  end
end

-- SCREEN --
function redraw()
  screen.clear()
  system:update()
  system:draw()
  drawParamValues()
  screen.update()
end

function drawParamValues()
  local selected_screen = params:get("selected_screen")
  all_screen_params = {
    {
      {key = 'dens', label = "DENSITY", value = params:get('dens'), format = "%.0f", y = 25},
      {key = 'dur', label = "DURATION", value = params:get('dur'), format = "%.2f", y = 35},
      {key = 'buffer', label = "BUFFER", value = params:get('buffer'), format = "%.0f", y = 45}
    },
    {
      {key = 'amp', label = "AMP", value = params:get('amp'), format = "%.0f", y = 25},
      {key = 'rate', label = "RATE", value = params:get('rate'), format = "%.2f", y = 35},
      {key = 'pos', label = "POSITION", value = params:get('pos'), format = "%.2f", y = 45}
    },
    {
      {key = 'depth', label = "WOBBLE", value = params:get('depth'), format = "%.2f", y = 25},
      {key = 'mix', label = "MIX", value = params:get('mix'), format = "%.2f", y = 35},
      {key = 'cutoffFreq', label = "CUTOFF", value = params:get('cutoffFreq'), format = "%.2f", y = 45}
    },
    {
      {key = 'bitDepth', label = "BIT DEPTH", value = params:get('bitDepth'), format = "%.0f", y = 25},
      {key = 'sampleRate', label = "SAMPLE RATE", value = params:get('sampleRate'), format = "%.0f", y = 35},
      {key = 'vmeMix', label = "VME MIX", value = params:get('vmeMix'), format = "%.2f", y = 45}
    }
  }
  
  screenParams = all_screen_params[selected_screen]

  drawScreenIndicator(selected_screen, total_screens)
  drawBackground(screenParams)
  drawParams(screenParams)
end

function drawParams(params)
  local screenWidth = 128
  local yPositions = { 25, 35, 45 } -- Y positions for each parameter

  for i, param in ipairs(params) do
    local formattedValue = string.format(param.format, param.value)
    local paramText = param.label .. ": " .. formattedValue
    local textWidth = screen.text_extents(paramText) -- Get text width
    local xPosition = (screenWidth - textWidth) / 2  -- Center horizontally

    drawParamText(xPosition, yPositions[i], paramText)
  end
end

function drawBackground(params)
  local bgBrightness = 0 -- Grey level
  local padding = 2      -- Padding around text for the background
  local rectHeight = 10  -- Height of the background rectangle

  screen.level(bgBrightness)
  for i, param in ipairs(params) do
    local formattedValue = string.format(param.format, param.value)
    local paramText = param.label .. ": " .. formattedValue
    local textWidth = screen.text_extents(paramText)
    local xPosition = (128 - textWidth) / 2 - padding -- Centering the background
    local yPosition = param.y - rectHeight / 2        -- Aligning vertically with the text

    screen.rect(xPosition, yPosition, textWidth + 2 * padding, rectHeight)
    screen.fill()
  end
end

function drawParamText(x, y, text)
  local textBrightness = 15
  screen.font_face(2)
  screen.level(textBrightness)
  screen.move(x, y)
  screen.text(text)
end

function drawScreenIndicator(selected, total)
  local squareWidth = 4
  local spacing = 6
  local totalWidth = (squareWidth * total) + (spacing * (total -spacing/2))
  local xBase = (128 - totalWidth) / 2  -- Center the entire group of squares

  local yPosition = 58  -- Position below the parameter text

  for i = 1, total do
    screen.level(i == selected and 15 or 1)  -- Highlight the selected screen
    local xPosition = xBase + (i - 1) * spacing
    screen.rect(xPosition, yPosition, squareWidth, squareWidth)
    screen.fill()
  end
end

function normalize_param_value(value, min, max)
  local range = max - min
  return math.floor(((value - min) / range) * 64)
end

function cleanup()
  params:set('monitor_level', pre_init_monitor_level) -- restore 'monitor' level
end


-- ARC
function a.delta(n, d)
  -- Retrieve the sensitivity setting and scale it down
  local sens = params:get("arc_sens_" .. n)
  d = d * sens

  local current_screen = params:get("selected_screen")

  -- Handle the fourth encoder for screen selection
  if n == 4 then
    params:delta("selected_screen", d)
    return
  end

  if current_screen == 1 then
    -- Screen 1: Map arc encoders to specific parameters
    if n == 1 then
      params:delta('dens', d)
    elseif n == 2 then
      params:delta('dur', d)
    elseif n == 3 then
      params:delta('buffer', d)
    end
  elseif current_screen == 2 then
    -- Screen 2
    if n == 1 then
      params:delta('amp', d)
    elseif n == 2 then
      params:delta('rate', d)
    elseif n == 3 then
      params:delta('pos', d)
    end
  elseif current_screen == 3 then
    -- Screen 3
    if n == 1 then
      params:delta('depth', d)
    elseif n == 2 then
      params:delta('mix', d)
    elseif n == 3 then
      params:delta('cutoffFreq', d)
    end
  elseif current_screen == 4 then
    -- Screen 4
    if n == 1 then
      params:delta('bitDepth', d)
    elseif n == 2 then
      params:delta('sampleRate', d)
    elseif n == 3 then
      params:delta('vmeMix', d)
    end
  end

  -- Update display and arc LEDs
  screen_dirty = true
end

function update_arc_display()
  -- Clear all LEDs on the arc
  a:all(0)
  local selected_screen = params:get("selected_screen")
  -- Example implementations for different screens
  if selected_screen == 1 then
      -- Progress bar for linear parameters (e.g., 'dens', 'dur', 'buffer')
      display_progress_bar(1, params:get('dens'), 0, 20)
      display_progress_bar(2, params:get('dur'), 0.05, 3)
      display_stepped_pattern(3, params:get('buffer'), 0, 5, 6)  -- 6 steps from 0 to 5
  elseif selected_screen == 2 then
      -- Rotating pattern for cyclic parameters (e.g., 'amp', 'rate')
      display_rotating_pattern(1, params:get('amp'), 0, 12)
      display_rotating_pattern(2, params:get('rate'), 0, 2)
      -- Blinking LED for boolean parameter (e.g., 'pos' as on/off)
      display_progress_bar(3, params:get('pos'), 0, 1)
  elseif selected_screen == 3 then
      -- Random pattern for 'depth'
      display_random_pattern(1, params:get('depth'), 0, 1)
      -- Progress bar for 'mix'
      display_progress_bar(2, params:get('mix'), 0, 1)
      -- Rotating pattern for 'cutoffFreq'
      display_exponential_pattern(3, params:get('cutoffFreq'), 100, 20000)
    elseif selected_screen == 4 then
      -- Screen 4: Vintage Sampler Parameters
      -- Example: Use progress bars to represent 'bitDepth' and 'sampleRate'
      display_progress_bar(1, params:get('bitDepth'), 8, 24)
      display_exponential_pattern(2, params:get('sampleRate'), 5000, 48000)
  
      -- Use a different pattern (e.g., blinking LED) for 'vmeMix'
      display_progress_bar(3, params:get('vmeMix'), 0, 1)
  end

  -- Update the fourth encoder for screen selection
  for i = 1, total_screens do
      local led_start = (i - 1) * 16 + 1
      local led_end = i * 16
      local brightness = i == selected_screen and 12 or 3
      for led = led_start, led_end do
          a:led(4, led, brightness)
      end
  end

  a:refresh()
end

function display_progress_bar(encoder, value, min, max)
  local normalized = normalize_param_value(value, min, max)
  local brightness_max = 12 -- Maximum brightness
  local gradient_factor = 1  -- Adjust this value to control the gradient's steepness

  for led = 1, 64 do
    if led <= normalized then
      -- Calculate brightness based on distance from the actual value
      local distance = math.abs(normalized - led) -- Adjust this value to control the gradient's steepness
      local brightness = math.max(1, brightness_max - (distance * gradient_factor)) 
      a:led(encoder, led, brightness)
    else
      a:led(encoder, led, 0) -- LEDs beyond the value are off
    end
  end
end

function display_rotating_pattern(encoder, value, min, max)
  local normalized = normalize_param_value(value, min, max)
  local start_led = (normalized % 64) + 1
  local pattern_width = 2 -- Width of the pattern on each side of the center LED
  local max_brightness = 10 -- Maximum brightness

  for i = -pattern_width, pattern_width do
    local led = (start_led + i - 1) % 64 + 1
    local brightness = max_brightness - math.abs(i) * 3
    brightness = math.max(brightness, 1) -- Ensure brightness is at least 1
    a:led(encoder, led, brightness)
  end
end

function display_stepped_pattern(encoder, value, min, max, steps)
  local leds_per_step = 64 / steps
  local step = math.floor((value - min) / (max - min) * (steps - 1)) + 1

  -- Add initial border if it starts from the first LED
  if leds_per_step % 1 ~= 0 then
      a:led(encoder, 1, 8)  -- Border at the beginning
  end

  -- Light up LEDs for each step
  for s = 1, steps do
      local start_led = math.floor((s - 1) * leds_per_step) + 1
      local end_led = math.floor(s * leds_per_step)

      -- Set LEDs for the current step
      for led = start_led, end_led do
          if s == step then
              a:led(encoder, led, 12)  -- Active step
          else
              a:led(encoder, led, 2)  -- Dim LEDs for inactive steps
          end
      end

      -- Add borders for each step
      if start_led > 1 then
          a:led(encoder, start_led - 1, 8)  -- Border at the start of each step
      end
      if end_led < 64 then
          a:led(encoder, end_led + 1, 8)  -- Border at the end of each step
      end
  end

  -- Add final border if it ends before the last LED
  if leds_per_step % 1 ~= 0 or steps * leds_per_step < 64 then
      a:led(encoder, 64, 8)  -- Border at the end
  end
end

function display_random_pattern(encoder, value, min, max)
    -- Normalize the value to a 0-1 range
    local normalized_value = (value - min) / (max - min)
    -- Invert the normalized value so that a lower value results in fewer LEDs
    local chance = 1 - normalized_value 

    for led = 1, 64 do
        if math.random() > chance then
            a:led(encoder, led, math.random(5, 12))
        else
            a:led(encoder, led, 0)  -- Turn off the LED
        end
    end
end

function display_exponential_pattern(encoder, value, min, max)
  -- Calculate the position of the value in an exponential manner
  local normalized = (math.log(value) - math.log(min)) / (math.log(max) - math.log(min))
  local led_position = math.floor(normalized * 64)

  -- Set the LEDs
  for led = 1, 64 do
      if led == led_position then
          a:led(encoder, led, 15)  -- Brightest LED for the actual value
      elseif led < led_position then
          a:led(encoder, led, 3)   -- Active but dimmer LEDs for values less than the current value
      else
          a:led(encoder, led, 0)   -- Inactive LEDs
      end
  end
end


-- PARTICLE --
Particle = {}
Particle.__index = Particle

function Particle:new(x, y)
  local baseDurationInSeconds = params:get('dur') -- Get the base duration in seconds
  local baseLifespanInFrames = baseDurationInSeconds * FRAMES_PER_SECOND

  -- Random offset in frames. Assuming a maximum of 1 second of randomness:
  local randomOffsetInSeconds = math.random(-1, 1) -- Random offset in seconds
  local randomOffsetInFrames = randomOffsetInSeconds * FRAMES_PER_SECOND

  local instance = {
    x = x,
    y = y,
    vx = math.random(-1, 1),
    vy = math.random(-1, 1),
    lifespan = baseLifespanInFrames + randomOffsetInFrames,
    size = math.random(1, 2)
  }
  setmetatable(instance, Particle)
  return instance
end

function Particle:update()
  -- Randomly adjust velocities with floating-point numbers
  self.vx = self.vx + (math.random(-25, 25) / 100) -- Smaller range
  self.vy = self.vy + (math.random(-25, 25) / 100) -- Smaller range

  -- Update position
  self.x = self.x + self.vx * 0.3
  self.y = self.y + self.vy * 0.3

  -- Update lifespan
  self.lifespan = self.lifespan - 1
end

function Particle:isDead()
  return self.lifespan <= 0
end

function Particle:draw()
  -- Calculate brightness based on lifespan
  local maxLifespan = 60 -- Adjust this to the maximum possible lifespan
  local brightness = math.max(1, math.floor((self.lifespan / maxLifespan) * 15))

  -- Reduce overall brightness if needed
  local brightnessReductionFactor = 1 -- Reduce brightness by 50%
  brightness = math.max(1, math.floor(brightness * brightnessReductionFactor))

  screen.level(brightness)

  -- Draw the particle
  screen.circle(self.x, self.y, self.size)
  screen.fill()
end

-- PARTICLE SYSTEM --
ParticleSystem = {}

ParticleSystem.__index = ParticleSystem

function ParticleSystem:new()
  local instance = {
    particles = {}
  }
  setmetatable(instance, ParticleSystem)
  return instance
end

function ParticleSystem:addParticle(x, y)
  local density = params:get('dens')
  if #self.particles < density then -- MAX_PARTICLES is a constant you define
    local randomX = x + math.random(-5, 5)
    local randomY = y + math.random(-5, 5)
    table.insert(self.particles, Particle:new(randomX, randomY))
  end
end

function ParticleSystem:update()
  for i = #self.particles, 1, -1 do
    local p = self.particles[i]
    p:update()
    if p:isDead() then
      table.remove(self.particles, i)
    end
  end
end

function ParticleSystem:draw()
  -- screen.clear()
  for _, p in ipairs(self.particles) do
    p:draw()
  end
  -- screen.update()
end
