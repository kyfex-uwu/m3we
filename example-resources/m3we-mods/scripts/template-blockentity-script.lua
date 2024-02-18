--[[
notes:
 - the variable self is set to this block entity
 - you can call .getEnv() on this to get all available scripts and variables. this can be useful
    when running code from the block (for example, to open a GUI with this block entity's inventory
    when the block is clicked)
]]--

function tick() -- runs every tick!
    -- this function takes no parameters, but you can access the blockState, world, and blockPos 
    -- of this block with __context.blockState, __context.world, and __context.blockPos
end

-- "listening" events
function shouldListenImmediately() return true end -- no idea what this does lol but make this return true
function getListeningRange() return 0 end -- the radius in blocks of the listening sphere
function listen(event) return true end -- when this blockentity hears something (return true if it did something with the event, false if not)

-- nbt stuff
function saveToNBT(nbt)  end -- do all your saving here, write to the nbt argument
function readFromNBT(nbt)  end -- do all your loading here, load from the nbt argument
