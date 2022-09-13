--declare variables above all the functions i think

-- change this function to do different things when a block is replaced!
-- return a table of blockstates to change the default blockstate
function getStateOnPlace(context)
    return {--needs to return a table!
        {"int_prop",2},
        {"direc_prop","north"}
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
        -- make sure you launch minecraft with the console
        -- enabled, so you can see prints (this will be fixed later)
    end
end

function onBroken(world,pos,state)

end
function onSteppedOn(world,pos,state,entity)

end
function onPlaced(world,pos,state,placer,itemStack)

end
function randomTick(state,world,pos,random)

end
function neighborUpdate(state,world,pos,sourceBlock,sourcePos,notify)

end
function scheduledTick(state,world,pos,random)

end