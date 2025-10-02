function getBlockShape(state, world, pos, context)
    local toReturn = {0,0,0,1,1,1}
    local direc = Properties.get(state, "direction")
    if direc == "north" then toReturn= {0,0,0,1,1,0.5}
    elseif direc == "east" then toReturn= {0.5,0,0,1,1,1}
    elseif direc == "west" then toReturn= {0,0,0,0.5,1,1}
    elseif direc == "south" then toReturn= {0,0,0.5,1,1,1} end

    return toReturn
end

function getStateOnPlace(context)
    return {
        direction=context.getPlayerFacing().asString()
    }
end