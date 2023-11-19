-- granular delayParticle = include 'lib/Particle';ParticleSystem = include 'lib/ParticleSystem';

engine.name = 'Granular'
a = arc.connect()

local FRAMES_PER_SECOND = 60

local pre_init_monitor_level;


function init()
  -- pre_init_monitor_level = params:get('monitor_level')   -- capture 'monitor' level before we change it
  -- params:set('monitor_level', -inf)

  local function strip_trailing_zeroes(s)
    return string.format('%.2f', s):gsub("%.?0+$", "")
  end

  system = ParticleSystem:new()
  -- Generate particles regularly


  particle_timer = metro.init(
    function()
      system:addParticle(64, 32)
      redraw()
      screen_dirty = false
    end, 1 / FRAMES_PER_SECOND)
    particle_timer:start()

  init_params()
  screen_dirty = true
  redraw_timer = metro.init(
    function() -- what to perform at every tick
      if screen_dirty == true then
        redraw()
        screen_dirty = false
      end
    end,
    1 / 15 -- how often (15 fps)
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

  params:add_separator('header', 'Vintage Sampler Emulator')

  add_param('bitDepth', 'Bit Depth', 8, 24, 24)                   -- Adjust the range if needed
  add_param('sampleRate', 'Sample Rate', 5000, 48000, 48000)      -- Adjust the range if needed
  add_param('drive', 'Drive', 0.02, 1, 1)                         -- Adjust the range if needed
  add_param('cutoffFreq', 'Cutoff Frequency', 1, 20000, 20000, 1) -- Adjust the range if needed
  add_param('vmeMix', 'VME Mix', 0, 1, 0)

  params:default() -- Set each parameter to its default value
end


function redraw()
  screen.clear()
  system:update()
  system:draw()
  screen.update()
  -- screen.text_center('amp: ' .. params:string('amp'))
end

function enc(n, d)
  -- params:delta('eng_amp', d)
  -- screen_dirty = true
  print("enc")
end

function cleanup()
  params:set('monitor_level', pre_init_monitor_level) -- restore 'monitor' level
  engine.free();
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
  screen.clear()
  for _, p in ipairs(self.particles) do
    p:draw()
  end
  screen.update()
end
