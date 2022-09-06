--declare variables above all the functions i think

function getStateOnPlace(context)
    return {--needs to return a table!
        int_prop=2,
        direc_prop="north"
    }
end

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
function scheduledTick(state,world,pos,random)

end