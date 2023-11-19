--declare variables above all the functions i think

-- change this function to do different things when a block is replaced!
-- return a table of blockstates to change the default blockstate
function getStateOnPlace(context)
    return {--needs to return a table!
        int_prop=2,
        direc_prop="north"
    }
end

--this is specified in template-block.json, make sure it returns a table of tables!
function blockShapeFunction(state, world, pos, context)
    return {
        {0,0,0,1,0.5,1},
        {0.375,0.5,0.375,0.625,0.75,0.625}
    }
end

----
-- you can change the functions below or you can delete them,
-- they are executed in the game whenever their event happens
-- (for example, onBroken is executed when the block is broken)

function onUse(state,world,pos,player,hand,hit)
    for key in pairs(state) do
        print(key)
    end
end
function onStateReplaced(state,world,pos,newState,wasMoved) end
function onSteppedOn(state,world,pos,entity) end
function onPlaced(state,world,pos,placer,itemStack) end

function neighborUpdate(state,world,pos,sourceBlock,sourcePos,notify) end
function scheduledTick(state,world,pos,random) end --not the same as a random tick, these have to be scheduled
function randomTick(state,world,pos,random) end --for server
function randomDisplayTick(state,world,pos,random) end --for client

function getStrongRedstonePower(state,world,pos,direction) end
function getWeakRedstonePower(state,world,pos,direction) end

function getDrops(paramsTable) end