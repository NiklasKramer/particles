-- granular delay

engine.name = 'Granular'
local pre_init_monitor_level;

function init()
  -- pre_init_monitor_level = params:get('monitor_level')   -- capture 'monitor' level before we change it
  -- params:set('monitor_level', -inf)

  local function strip_trailing_zeroes(s)
    return string.format('%.2f', s):gsub("%.?0+$", "")
  end

  params:add_separator('header', 'engine controls')


  local function add_param(id, name, min, max, default, quant)
    quant = quant or 0.01   -- Use the provided quant value or default to 0.01
    params:add_control(
      id,                   -- ID
      name,                 -- display name
      controlspec.new(
        min,                -- min
        max,                -- max
        'lin',              -- warp
        quant,              -- output quantization
        default             -- default value
      )
    )
    params:set_action(id, function(x) engine[id](x) end)
  end
  add_param('amp', 'Amplitude', 0, 8, 4)
  add_param('pos', 'Position', 0, 1, 0.5)
  add_param('dur', 'Duration', 0.05, 1, 0.4)
  add_param('dens', 'Density', 0, 20, 10, 0.2)
  add_param('jitter', 'Jitter', 0, 1, 1)
  add_param('triggerType', 'Trigger Type', 0, 1, 0, 1)
  add_param('rate', 'Rate', 0, 2, 1, 1 / 12)

  params:add_separator('header', 'wobble')

  add_param('depth', 'Depth', 0, 0.5, 0.02)
  add_param('mix', 'Mix', 0, 1, 1)

  params:add_separator('header', 'Vintage Sampler Emulator')
  -- Add the rest of the parameters after the 'wobble' section
  -- add_param('release', 'Release', 0.01, 10, 0.5)    -- Just an example, adjust min/max/default as needed
  -- add_param('maxDelay', 'Max Delay', 0.01, 10, 1)   -- Just an example, adjust min/max/default as needed
  -- add_param('minDelay', 'Min Delay', 0.01, 10, 0.1) -- Just an example, adjust min/max/default as needed

  -- Add VME related parameters
  add_param('bitDepth', 'Bit Depth', 8, 24, 16)               -- Adjust the range if needed
  add_param('sampleRate', 'Sample Rate', 5000, 48000, 44100)  -- Adjust the range if needed
  add_param('drive', 'Drive', 0.1, 1, 0.01)                     -- Adjust the range if needed
  add_param('cutoffFreq', 'Cutoff Frequency', 1, 20000, 20000,1) -- Adjust the range if needed
  add_param('vmeMix', 'VME Mix', 0, 1, 1)


  params:default()   -- Set each parameter to its default value





  screen_dirty = true
  redraw_timer = metro.init(
    function()   -- what to perform at every tick
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

function redraw()
  screen.clear()
  screen.move(64, 32)
  screen.level(15)
  screen.font_size(17)
  screen.text_center('GRANULAR')

  -- screen.text_center('amp: ' .. params:string('amp'))
  screen.update()
end

function enc(n, d)
-- params:delta('eng_amp', d)
  screen_dirty = true
end

function cleanup()
  params:set('monitor_level', pre_init_monitor_level)   -- restore 'monitor' level
  engine.free();
end
