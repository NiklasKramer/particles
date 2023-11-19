-- P/A/R/T/I/C/L/E/S

engine.name = 'Granular'
a = arc.connect()

local FRAMES_PER_SECOND = 30
local pre_init_monitor_level;

local total_screens = 3

local selected_screen = 1

function init()
  pre_init_monitor_level = params:get('monitor_level') -- store 'monitor' level
  params:set('monitor_level', 0) -- mute 'monitor' level

  init_params()
  init_timers()
  screen_dirty = true  
end

function init_timers()
  system = ParticleSystem:new()
  particle_timer = metro.init(
    function()
      system:addParticle(64, 32)
      redraw()
      screen_dirty = false
    end, 1 / FRAMES_PER_SECOND)
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
end

function init_params()
  params:add_separator('header', 'engine controls')

  add_param('amp', 'Amplitude', 0, 12, 4)
  add_param('buffer', 'Buffer', 0, 5, 1, 1)
  add_param('pos', 'Position', 0, 1, 0.5)
  add_param('dur', 'Duration', 0.05, 5, 0.4)
  add_param('dens', 'Density', 0, 30, 10, 0.2)
  add_param('jitter', 'Jitter', 0, 1, 1)
  add_param('triggerType', 'Trigger Type', 0, 1, 0, 1)
  add_param('rate', 'Rate', 0, 2, 1, 1 / 12)

  params:add_separator('header', 'wobble')

  add_param('depth', 'Depth', 0, 0.5, 0.02)
  add_param('mix', 'Mix', 0, 1, 1)

  params:add_separator('header', 'filter')
  add_param('cutoffFreq', 'Cutoff Frequency', 1, 20000, 20000, 1) -- Adjust the range if needed

  params:add_separator('header', 'vintage sampler')

  add_param('bitDepth', 'Bit Depth', 8, 24, 24)                   -- Adjust the range if needed
  add_param('sampleRate', 'Sample Rate', 5000, 48000, 48000)      -- Adjust the range if needed
  add_param('drive', 'Drive', 0.02, 1, 1)                         -- Adjust the range if needed
  add_param('vmeMix', 'VME Mix', 0, 1, 0)

  params:default() -- Set each parameter to its default value
end

function add_param(id, name, min, max, default, quant)
  quant = quant or 0.01 -- Use the provided quant value or default to 0.01
  params:add_control(
    id,                 -- ID
    name,               -- display name
    controlspec.new(
      min,              -- min
      max,              -- max
      'lin',            -- warp
      quant,            -- output quantization
      default           -- default value
    )
  )
  params:set_action(id, function(x) engine[id](x) end)
end

function redraw()
  screen.clear()
  system:update()
  system:draw()
  drawParamValues()
  screen.update()
end

function key(n, z)
  if z == 1 then -- Check if the button is pressed (not released)
    button_pressed(n)
  end
end

function enc(n, d)
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
      params:delta('rate', d)  -- Update 'rate' parameter instead of 'pos'
    elseif n == 3 then
      params:delta('pos', d)
    end
  elseif selected_screen == 3 then
    -- Mapping for screen 3
    if n == 1 then
      params:delta('depth', d)  -- Update 'depth' parameter instead of 'rate'
    elseif n == 2 then
      params:delta('mix', d)
    elseif n == 3 then
      params:delta('cutoffFreq', d)  -- Update 'cutoffFreq' parameter instead of 'jitter'
    end
  end

  screen_dirty = true -- Flag to redraw the screen
  drawParamValues()    -- Call drawParamValues to update the display with the new parameter values
end


function button_pressed(button)
  if button == 3 then -- Button to go to the next screen
    selected_screen = (selected_screen % total_screens) + 1
    screen_dirty = true
  elseif button == 2 then -- Button to go to the previous screen
    selected_screen = (selected_screen - 2) % total_screens + 1
    screen_dirty = true
  end
  redraw() -- Redraw the screen with the new selected screen
end

function drawParamValues()
  local screenParams
  if selected_screen == 1 then
    screenParams = {
      {label = "DENSITY", value = params:get('dens'), format = "%.0f", y = 25},
      {label = "DURATION", value = params:get('dur'), format = "%.2f", y = 35},
      {label = "BUFFER", value = params:get('buffer'), format = "%.0f", y = 45}
    }
  elseif selected_screen == 2 then
    screenParams = {
      {label = "AMP", value = params:get('amp'), format = "%.0f", y = 25},
      {label = "RATE", value = params:get('rate'), format = "%.2f", y = 35},
      {label = "POSITION", value = params:get('pos'), format = "%.2f", y = 45}
    }
  elseif selected_screen == 3 then
    screenParams = {
      {label = "WOBBLE", value = params:get('depth'), format = "%.2f", y = 25},
      {label = "MIX", value = params:get('mix'), format = "%.2f", y = 35},
      {label = "CUTOFF", value = params:get('cutoffFreq'), format = "%.2f", y = 45}
    }
  end

  drawScreenIndicator(selected_screen, total_screens)
  drawBackground(screenParams)
  drawParams(screenParams)
end



function drawParams(params)
  local screenWidth = 128
  local yPositions = {25, 35, 45} -- Y positions for each parameter

  for i, param in ipairs(params) do
    local formattedValue = string.format(param.format, param.value)
    local paramText = param.label .. ": " .. formattedValue
    local textWidth = screen.text_extents(paramText) -- Get text width
    local xPosition = (screenWidth - textWidth) / 2 -- Center horizontally

    drawParamText(xPosition, yPositions[i], paramText)
  end
end

function drawBackground(params)
  local bgBrightness = 0 -- Grey level
  local padding = 2 -- Padding around text for the background
  local rectHeight = 10 -- Height of the background rectangle

  screen.level(bgBrightness)
  for i, param in ipairs(params) do
    local formattedValue = string.format(param.format, param.value)
    local paramText = param.label .. ": " .. formattedValue
    local textWidth = screen.text_extents(paramText)
    local xPosition = (128 - textWidth) / 2 - padding -- Centering the background
    local yPosition = param.y - rectHeight / 2 -- Aligning vertically with the text

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
  local xBase = 64 - (total * 3) -- Adjust position based on total screens
  local yPosition = 58 -- Position below the parameter text

  for i = 1, total do
    screen.level(i == selected and 15 or 1) -- Highlight the selected screen
    screen.rect(xBase + (i * 6), yPosition, 4, 4) -- Draw square
    screen.fill()
  end
end



function a.delta(n, d)
  -- params:delta('eng_amp', d)
  -- screen_dirty = true
  print("delta")
end

function cleanup()
  params:set('monitor_level', pre_init_monitor_level) -- restore 'monitor' level
  engine.free();
end




-- PARTICLE --
Particle = {}
Particle.__index = Particle

function Particle:new(x, y)
  local baseDurationInSeconds = params:get('dur')  -- Get the base duration in seconds
  local baseLifespanInFrames = baseDurationInSeconds * FRAMES_PER_SECOND

  -- Random offset in frames. Assuming a maximum of 1 second of randomness:
  local randomOffsetInSeconds = math.random(-1, 1)  -- Random offset in seconds
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
  self.vx = self.vx + (math.random(-25, 25) / 100)  -- Smaller range
  self.vy = self.vy + (math.random(-25, 25) / 100)  -- Smaller range

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
  local maxLifespan = 60  -- Adjust this to the maximum possible lifespan
  local brightness = math.max(1, math.floor((self.lifespan / maxLifespan) * 15))

  -- Reduce overall brightness if needed
  local brightnessReductionFactor = 1  -- Reduce brightness by 50%
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
  if #self.particles < density then  -- MAX_PARTICLES is a constant you define
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
